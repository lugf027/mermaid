package io.lugf027.github.mermaid.core.diagram.architecture

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.rendering.svg.buildSvg
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.TextUtils
import kotlin.math.max

/**
 * 架构图渲染器 - 对标 mermaid-js architectureRenderer.ts
 *
 * 简化版本：使用网格布局代替 Cytoscape+fcose。
 * 服务节点渲染为带图标占位符和标题的矩形，
 * Group 渲染为带标题的虚线边界框。
 */
class ArchitectureRenderer : DiagramRenderer {

    companion object {
        const val NODE_SIZE = 80.0
        const val NODE_PADDING = 10.0
        const val NODE_SPACING = 40.0
        const val GROUP_PADDING = 20.0
        const val TITLE_FONT_SIZE = 12
        const val MARGIN = 30.0
        const val ICON_SIZE = 48.0
        const val NODES_PER_ROW = 4
    }

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val adb = db as ArchitectureDb
        val services = adb.getServices()
        val junctions = adb.getJunctions()
        val groups = adb.getGroups()
        val edges = adb.getEdges()
        val title = adb.getDiagramTitle()

        val archConfig = config.architecture
        val iconSize = (archConfig?.iconSize ?: 80).toDouble()
        val nodeSize = iconSize

        // 简单网格布局
        data class NodeLayout(
            val id: String,
            val label: String,
            val icon: String,
            var x: Double = 0.0,
            var y: Double = 0.0,
            val size: Double = nodeSize,
            val isJunction: Boolean = false
        )

        val nodeLayouts = mutableMapOf<String, NodeLayout>()
        val allNodes = mutableListOf<NodeLayout>()

        // 先布局服务
        for ((id, svc) in services) {
            val layout = NodeLayout(id, svc.title.ifEmpty { id }, svc.icon)
            allNodes.add(layout)
            nodeLayouts[id] = layout
        }

        // junction
        for ((id, _) in junctions) {
            val layout = NodeLayout(id, "", "", isJunction = true)
            allNodes.add(layout)
            nodeLayouts[id] = layout
        }

        // 网格布局
        val titleH = if (title.isNotEmpty()) 40.0 else 0.0
        for ((idx, node) in allNodes.withIndex()) {
            val col = idx % NODES_PER_ROW
            val row = idx / NODES_PER_ROW
            node.x = MARGIN + col * (nodeSize + NODE_SPACING)
            node.y = MARGIN + titleH + row * (nodeSize + TITLE_FONT_SIZE + NODE_SPACING + 10)
        }

        val maxX = allNodes.maxOfOrNull { it.x + it.size } ?: 400.0
        val maxY = allNodes.maxOfOrNull { it.y + it.size + TITLE_FONT_SIZE + 10 } ?: 300.0
        val svgW = maxX + MARGIN
        val svgH = maxY + MARGIN

        return buildSvg {
            attr("id", diagramId)
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("xmlns:xlink", "http://www.w3.org/1999/xlink")

            // 箭头标记
            defs {
                marker {
                    attr("id", "arch-arrow")
                    attr("markerWidth", "10")
                    attr("markerHeight", "10")
                    attr("refX", "10")
                    attr("refY", "5")
                    attr("orient", "auto")
                    path("M 0 0 L 10 5 L 0 10 Z") {
                        attr("fill", themeVariables.lineColor)
                    }
                }
            }

            group {
                addClass("architecture")

                // 标题
                if (title.isNotEmpty()) {
                    text(title, svgW / 2, MARGIN + 16) {
                        attr("text-anchor", "middle")
                        attr("font-size", "16")
                        attr("font-weight", "bold")
                        attr("fill", themeVariables.textColor)
                    }
                }

                // Group 背景
                for ((gId, grp) in groups) {
                    val children = adb.getGroupChildren(gId)
                    if (children.isEmpty()) continue

                    val childLayouts = children.mapNotNull { nodeLayouts[it] }
                    if (childLayouts.isEmpty()) continue

                    val gx = childLayouts.minOf { it.x } - GROUP_PADDING
                    val gy = childLayouts.minOf { it.y } - GROUP_PADDING - 20
                    val gw = childLayouts.maxOf { it.x + it.size } - gx + GROUP_PADDING
                    val gh = childLayouts.maxOf { it.y + it.size + TITLE_FONT_SIZE + 10 } - gy + GROUP_PADDING

                    group {
                        addClass("architecture-group")
                        rect(gx, gy, gw, gh) {
                            attr("fill", "none")
                            attr("stroke", themeVariables.lineColor)
                            attr("stroke-width", "1")
                            attr("stroke-dasharray", "5,5")
                            attr("rx", "8")
                            attr("ry", "8")
                        }
                        if (grp.title.isNotEmpty()) {
                            text(grp.title, gx + 10, gy + 16) {
                                attr("font-size", "$TITLE_FONT_SIZE")
                                attr("font-weight", "bold")
                                attr("fill", themeVariables.textColor)
                            }
                        }
                    }
                }

                // 服务节点
                group {
                    addClass("services")
                    for (node in allNodes) {
                        if (node.isJunction) continue

                        group {
                            addClass("service")
                            // 图标区域（简化为圆角矩形）
                            rect(node.x, node.y, node.size, node.size) {
                                attr("fill", themeVariables.mainBkg)
                                attr("stroke", themeVariables.lineColor)
                                attr("stroke-width", "1")
                                attr("rx", "8")
                                attr("ry", "8")
                            }

                            // 图标占位
                            if (node.icon.isNotEmpty()) {
                                text(node.icon, node.x + node.size / 2, node.y + node.size / 2 + 4) {
                                    attr("text-anchor", "middle")
                                    attr("font-size", "24")
                                    attr("fill", themeVariables.textColor)
                                }
                            }

                            // 标题
                            if (node.label.isNotEmpty()) {
                                text(node.label, node.x + node.size / 2, node.y + node.size + TITLE_FONT_SIZE + 4) {
                                    attr("text-anchor", "middle")
                                    attr("font-size", "$TITLE_FONT_SIZE")
                                    attr("fill", themeVariables.textColor)
                                }
                            }
                        }
                    }
                }

                // 边
                group {
                    addClass("edges")
                    for (edge in edges) {
                        val src = nodeLayouts[edge.lhsId] ?: continue
                        val dst = nodeLayouts[edge.rhsId] ?: continue

                        val (sx, sy) = getEdgePoint(src.x, src.y, src.size, edge.lhsDir)
                        val (ex, ey) = getEdgePoint(dst.x, dst.y, dst.size, edge.rhsDir)

                        group {
                            addClass("edge")
                            line(sx, sy, ex, ey) {
                                attr("stroke", themeVariables.lineColor)
                                attr("stroke-width", "1.5")
                                if (edge.rhsInto || edge.lhsInto) {
                                    attr("marker-end", "url(#arch-arrow)")
                                }
                            }
                            if (edge.title.isNotEmpty()) {
                                val mx = (sx + ex) / 2
                                val my = (sy + ey) / 2
                                text(edge.title, mx, my - 5) {
                                    attr("text-anchor", "middle")
                                    attr("font-size", "11")
                                    attr("fill", themeVariables.textColor)
                                }
                            }
                        }
                    }
                }
            }

            viewBox(0.0, 0.0, svgW, svgH)
            attr("width", "100%")
            attr("style", "max-width: ${svgW.toInt()}px;")
        }
    }

    /** 根据方向获取节点边缘的连接点 */
    private fun getEdgePoint(x: Double, y: Double, size: Double, dir: ArchitectureDb.Direction): Pair<Double, Double> {
        val cx = x + size / 2
        val cy = y + size / 2
        return when (dir) {
            ArchitectureDb.Direction.L -> Pair(x, cy)
            ArchitectureDb.Direction.R -> Pair(x + size, cy)
            ArchitectureDb.Direction.T -> Pair(cx, y)
            ArchitectureDb.Direction.B -> Pair(cx, y + size)
        }
    }
}
