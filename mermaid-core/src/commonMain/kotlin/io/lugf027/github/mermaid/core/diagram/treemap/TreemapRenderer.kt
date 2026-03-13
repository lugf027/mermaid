package io.lugf027.github.mermaid.core.diagram.treemap

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.rendering.svg.buildSvg
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.TextUtils
import kotlin.math.max
import kotlin.math.min

/**
 * 树形图渲染器 - 对标 mermaid-js treemap/renderer.ts
 *
 * 使用简化的 squarify 布局算法替代 d3.treemap。
 * Section 节点有标题区域，Leaf 节点显示名称和值。
 */
class TreemapRenderer : DiagramRenderer {

    companion object {
        const val WIDTH = 600.0
        const val HEIGHT = 400.0
        const val PADDING = 4.0
        const val HEADER_HEIGHT = 25.0
        const val FONT_SIZE = 12
        const val VALUE_FONT_SIZE = 10
        val COLORS = listOf(
            "#4e79a7", "#f28e2b", "#e15759", "#76b7b2",
            "#59a14f", "#edc948", "#b07aa1", "#ff9da7",
            "#9c755f", "#bab0ac", "#d37295", "#fabfd2"
        )
    }

    /** 布局矩形 */
    private data class Rect(val x: Double, val y: Double, val w: Double, val h: Double)

    /** 布局结果 */
    private data class NodeLayout(
        val node: TreemapDb.TreemapNode,
        val rect: Rect,
        val colorIndex: Int
    )

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val tdb = db as TreemapDb
        val root = tdb.getRootNode()
        val title = tdb.getDiagramTitle()
        val titleH = if (title.isNotEmpty()) 30.0 else 0.0

        val layouts = mutableListOf<NodeLayout>()

        if (root != null) {
            val rootRect = Rect(PADDING, PADDING + titleH, WIDTH - PADDING * 2, HEIGHT - PADDING * 2)
            layoutNode(root, rootRect, 0, layouts)
        }

        val svgH = HEIGHT + titleH

        return buildSvg {
            attr("id", diagramId)
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("xmlns:xlink", "http://www.w3.org/1999/xlink")

            group {
                addClass("treemap")

                // 标题
                if (title.isNotEmpty()) {
                    text(title, WIDTH / 2, 22.0) {
                        attr("text-anchor", "middle")
                        attr("font-size", "16")
                        attr("font-weight", "bold")
                        attr("fill", themeVariables.textColor)
                    }
                }

                // 渲染节点
                for (layout in layouts) {
                    val r = layout.rect
                    val n = layout.node
                    val color = COLORS[layout.colorIndex % COLORS.size]

                    group {
                        addClass("treemap-node")

                        if (n.type == TreemapDb.NodeType.SECTION && n.children.isNotEmpty()) {
                            // Section: 只渲染标题区域
                            rect(r.x, r.y, r.w, HEADER_HEIGHT) {
                                attr("fill", color)
                                attr("fill-opacity", "0.3")
                                attr("stroke", color)
                                attr("stroke-width", "1")
                            }
                            if (r.w > 30) {
                                text(n.name, r.x + 5, r.y + HEADER_HEIGHT - 8) {
                                    attr("font-size", "$FONT_SIZE")
                                    attr("font-weight", "bold")
                                    attr("fill", themeVariables.textColor)
                                }
                            }
                        } else {
                            // Leaf: 填充矩形 + 标签 + 值
                            rect(r.x, r.y, r.w, r.h) {
                                attr("fill", color)
                                attr("fill-opacity", "0.7")
                                attr("stroke", "#fff")
                                attr("stroke-width", "1")
                            }
                            // 标签（如果空间够）
                            if (r.w > 20 && r.h > 20) {
                                val textW = TextUtils.estimateTextWidth(n.name, FONT_SIZE.toDouble())
                                val displayName = if (textW > r.w - 8) {
                                    n.name.take(((r.w - 16) / 7).toInt().coerceAtLeast(1)) + "…"
                                } else {
                                    n.name
                                }
                                text(displayName, r.x + r.w / 2, r.y + r.h / 2) {
                                    attr("text-anchor", "middle")
                                    attr("font-size", "$FONT_SIZE")
                                    attr("fill", "#fff")
                                }
                                if (n.value > 0 && r.h > 35) {
                                    val valueStr = if (n.value == n.value.toLong().toDouble()) {
                                        n.value.toLong().toString()
                                    } else {
                                        n.value.toString()
                                    }
                                    text(valueStr, r.x + r.w / 2, r.y + r.h / 2 + VALUE_FONT_SIZE + 4) {
                                        attr("text-anchor", "middle")
                                        attr("font-size", "$VALUE_FONT_SIZE")
                                        attr("fill", "#fff")
                                        attr("opacity", "0.8")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            viewBox(0.0, 0.0, WIDTH, svgH)
            attr("width", "100%")
            attr("style", "max-width: ${WIDTH.toInt()}px;")
        }
    }

    /**
     * 简化的 squarify 布局：按比例分配矩形面积
     */
    private fun layoutNode(
        node: TreemapDb.TreemapNode,
        rect: Rect,
        colorIndex: Int,
        results: MutableList<NodeLayout>
    ) {
        results.add(NodeLayout(node, rect, colorIndex))

        if (node.children.isEmpty()) return

        // 子节点可用区域（section 减去头部）
        val childRect = if (node.type == TreemapDb.NodeType.SECTION) {
            Rect(rect.x + PADDING, rect.y + HEADER_HEIGHT + PADDING,
                 rect.w - PADDING * 2, rect.h - HEADER_HEIGHT - PADDING * 2)
        } else {
            rect
        }

        if (childRect.w <= 0 || childRect.h <= 0) return

        val totalValue = node.children.sumOf { it.totalValue() }
        if (totalValue <= 0) return

        // 简化 squarify: 沿短边切割
        squarify(node.children, childRect, totalValue, colorIndex, results)
    }

    private fun squarify(
        children: List<TreemapDb.TreemapNode>,
        rect: Rect,
        totalValue: Double,
        baseColorIndex: Int,
        results: MutableList<NodeLayout>
    ) {
        if (children.isEmpty() || rect.w <= 0 || rect.h <= 0) return

        if (children.size == 1) {
            layoutNode(children[0], rect, baseColorIndex, results)
            return
        }

        // 沿较长边分割
        val isHorizontal = rect.w >= rect.h
        var consumed = 0.0
        var offset = if (isHorizontal) rect.x else rect.y

        for ((idx, child) in children.withIndex()) {
            val ratio = child.totalValue() / totalValue
            val childColorIdx = baseColorIndex + idx

            if (isHorizontal) {
                val w = if (idx == children.size - 1) {
                    rect.x + rect.w - offset  // 最后一个取剩余
                } else {
                    rect.w * ratio
                }
                val childRect = Rect(offset, rect.y, max(w, 1.0), rect.h)
                layoutNode(child, childRect, childColorIdx, results)
                offset += w
            } else {
                val h = if (idx == children.size - 1) {
                    rect.y + rect.h - offset
                } else {
                    rect.h * ratio
                }
                val childRect = Rect(rect.x, offset, rect.w, max(h, 1.0))
                layoutNode(child, childRect, childColorIdx, results)
                offset += h
            }
        }
    }
}
