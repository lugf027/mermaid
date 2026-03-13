package io.lugf027.github.mermaid.core.diagram.sankey

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.rendering.svg.buildSvg
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.TextUtils
import kotlin.math.max

/**
 * 桑基图渲染器 - 对标 mermaid-js sankeyRenderer.ts
 *
 * 布局算法：
 * 1. 拓扑排序确定节点列（column）
 * 2. 每列内节点高度按流量值分配
 * 3. 节点间用 Bézier 曲线连接，链接宽度按流量值比例
 */
class SankeyRenderer : DiagramRenderer {

    companion object {
        const val NODE_WIDTH = 10.0
        const val NODE_PADDING = 10.0  // 列内节点间距
        const val LABEL_PADDING = 6.0
        const val LABEL_FONT_SIZE = 12
        const val MARGIN = 20.0
    }

    private val nodeColors = listOf(
        "#4e79a7", "#f28e2c", "#e15759", "#76b7b2",
        "#59a14f", "#edc949", "#af7aa1", "#ff9da7",
        "#9c755f", "#bab0ab"
    )

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val sdb = db as SankeyDb
        val nodes = sdb.getNodes()
        val links = sdb.getLinks()

        val sankeyConfig = config.sankey
        val chartWidth = (sankeyConfig?.width ?: 600).toDouble()
        val chartHeight = (sankeyConfig?.height ?: 400).toDouble()
        val showValues = sankeyConfig?.showValues ?: true
        val prefix = sankeyConfig?.prefix ?: ""
        val suffix = sankeyConfig?.suffix ?: ""

        val plotLeft = MARGIN
        val plotTop = MARGIN
        val plotWidth = chartWidth - MARGIN * 2
        val plotHeight = chartHeight - MARGIN * 2

        // 1. 拓扑排序获取列
        val columns = sdb.getNodeColumns()
        if (columns.isEmpty()) {
            return buildEmptySvg(diagramId, chartWidth, chartHeight)
        }

        val numCols = columns.size
        val colSpacing = if (numCols > 1) (plotWidth - NODE_WIDTH) / (numCols - 1) else plotWidth

        // 2. 计算每个节点的流量值
        val nodeValues = mutableMapOf<String, Double>()
        for (node in nodes) {
            nodeValues[node.id] = sdb.getNodeValue(node.id)
        }

        // 3. 计算每列的总流量，用于确定缩放比例
        val maxColValue = columns.maxOfOrNull { col ->
            col.sumOf { nodeValues[it] ?: 0.0 } + (col.size - 1) * NODE_PADDING
        } ?: 1.0

        val scale = plotHeight / maxColValue

        // 4. 布局每列的节点
        data class NodeLayout(
            val id: String,
            val x: Double,
            val y: Double,
            val width: Double,
            val height: Double,
            val colorIndex: Int
        )

        val nodeLayouts = mutableMapOf<String, NodeLayout>()
        var colorIdx = 0

        for ((colIdx, col) in columns.withIndex()) {
            val x = plotLeft + colIdx * colSpacing
            val colTotalValue = col.sumOf { nodeValues[it] ?: 0.0 }
            val colTotalPadding = (col.size - 1) * NODE_PADDING
            val colTotalHeight = colTotalValue * scale + colTotalPadding
            var y = plotTop + (plotHeight - colTotalHeight) / 2  // 居中对齐

            for (nodeId in col) {
                val h = (nodeValues[nodeId] ?: 0.0) * scale
                nodeLayouts[nodeId] = NodeLayout(nodeId, x, y, NODE_WIDTH, max(h, 1.0), colorIdx)
                y += h + NODE_PADDING
                colorIdx++
            }
        }

        return buildSvg {
            attr("id", diagramId)
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("xmlns:xlink", "http://www.w3.org/1999/xlink")

            group {
                addClass("sankey")

                // 5. 绘制链接（先画链接，再画节点覆盖上层）
                group {
                    addClass("links")
                    // 追踪每个节点已使用的输出/输入偏移
                    val sourceOffsets = mutableMapOf<String, Double>()
                    val targetOffsets = mutableMapOf<String, Double>()

                    for (link in links) {
                        val srcLayout = nodeLayouts[link.source] ?: continue
                        val tgtLayout = nodeLayouts[link.target] ?: continue

                        val linkHeight = link.value * scale
                        val srcOffset = sourceOffsets[link.source] ?: 0.0
                        val tgtOffset = targetOffsets[link.target] ?: 0.0

                        val sy = srcLayout.y + srcOffset + linkHeight / 2
                        val ty = tgtLayout.y + tgtOffset + linkHeight / 2

                        sourceOffsets[link.source] = srcOffset + linkHeight
                        targetOffsets[link.target] = tgtOffset + linkHeight

                        // Bézier 曲线路径
                        val sx = srcLayout.x + srcLayout.width
                        val tx = tgtLayout.x
                        val cx = (sx + tx) / 2  // 控制点 X

                        val pathD = "M $sx,$sy C $cx,$sy $cx,$ty $tx,$ty"

                        path(pathD) {
                            addClass("link")
                            attr("fill", "none")
                            attr("stroke", nodeColors[srcLayout.colorIndex % nodeColors.size])
                            attr("stroke-opacity", "0.4")
                            attr("stroke-width", "${max(linkHeight, 1.0)}")
                        }
                    }
                }

                // 6. 绘制节点
                group {
                    addClass("nodes")
                    for ((_, layout) in nodeLayouts) {
                        val color = nodeColors[layout.colorIndex % nodeColors.size]

                        group {
                            addClass("node")
                            rect(layout.x, layout.y, layout.width, layout.height) {
                                attr("fill", color)
                                attr("stroke", "#000")
                                attr("stroke-width", "0.5")
                            }

                            // 节点标签
                            val labelText = if (showValues) {
                                val v = nodeValues[layout.id] ?: 0.0
                                "${layout.id} - $prefix${formatValue(v)}$suffix"
                            } else {
                                layout.id
                            }

                            // 标签位置：最后一列在左侧，其他在右侧
                            val isLastCol = columns.last().contains(layout.id)
                            val labelX = if (isLastCol) layout.x - LABEL_PADDING else layout.x + layout.width + LABEL_PADDING
                            val labelAnchor = if (isLastCol) "end" else "start"

                            text(labelText, labelX, layout.y + layout.height / 2 + 4) {
                                attr("text-anchor", labelAnchor)
                                attr("font-size", "$LABEL_FONT_SIZE")
                                attr("fill", themeVariables.textColor)
                            }
                        }
                    }
                }
            }

            viewBox(0.0, 0.0, chartWidth, chartHeight)
            attr("width", "100%")
            attr("style", "max-width: ${chartWidth.toInt()}px;")
        }
    }

    private fun buildEmptySvg(id: String, w: Double, h: Double): SvgRoot {
        return buildSvg {
            attr("id", id)
            attr("xmlns", "http://www.w3.org/2000/svg")
            viewBox(0.0, 0.0, w, h)
        }
    }

    private fun formatValue(v: Double): String {
        return if (v == v.toLong().toDouble()) v.toLong().toString()
        else {
            val rounded = (v * 10).toLong() / 10.0
            if (rounded == rounded.toLong().toDouble()) "${rounded.toLong()}.0"
            else rounded.toString()
        }
    }
}
