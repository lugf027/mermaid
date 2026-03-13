package io.lugf027.github.mermaid.core.diagram.requirement

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.rendering.svg.buildSvg
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.TextUtils
import kotlin.math.max

/**
 * 需求图渲染器 - 对标 mermaid-js requirementRenderer.ts
 *
 * 自定义渲染：将需求和元素渲染为矩形节点，
 * 关系渲染为带标签的连接线。
 * 使用简单的网格布局（按行排列节点）。
 */
class RequirementRenderer : DiagramRenderer {

    companion object {
        const val NODE_WIDTH = 200.0
        const val NODE_MIN_HEIGHT = 60.0
        const val NODE_PADDING = 10.0
        const val LINE_HEIGHT = 20.0
        const val SPACING_X = 50.0
        const val SPACING_Y = 60.0
        const val MARGIN = 30.0
        const val TYPE_FONT_SIZE = 12
        const val NAME_FONT_SIZE = 14
        const val DETAIL_FONT_SIZE = 11
        const val LABEL_FONT_SIZE = 11
        const val NODES_PER_ROW = 3
    }

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val rdb = db as RequirementDb
        val requirements = rdb.getRequirements()
        val elements = rdb.getElements()
        val relations = rdb.getRelations()
        val title = rdb.getDiagramTitle()

        val reqConfig = config.requirement
        val nodeW = (reqConfig?.rectMinWidth ?: 200).toDouble()
        val fillColor = reqConfig?.rectFill ?: "#f9f9f9"
        val borderColor = reqConfig?.rectBorderColor ?: "#bbb"

        // 收集所有节点（需求 + 元素）
        data class NodeInfo(
            val id: String,
            val label: String,
            val typeLabel: String,
            val lines: List<String>,
            var x: Double = 0.0,
            var y: Double = 0.0,
            var w: Double = nodeW,
            var h: Double = NODE_MIN_HEIGHT
        )

        val allNodes = mutableListOf<NodeInfo>()

        for ((name, req) in requirements) {
            val lines = mutableListOf<String>()
            if (req.requirementId.isNotEmpty()) lines.add("Id: ${req.requirementId}")
            if (req.text.isNotEmpty()) lines.add("Text: ${req.text}")
            if (req.risk != null) lines.add("Risk: ${req.risk!!.name.lowercase()}")
            if (req.verifyMethod != null) lines.add("Verify: ${req.verifyMethod!!.name.lowercase()}")
            val h = max(NODE_MIN_HEIGHT, (lines.size + 2) * LINE_HEIGHT + NODE_PADDING * 2)
            allNodes.add(NodeInfo(name, name, "«${req.type.displayName}»", lines, h = h))
        }

        for ((name, elem) in elements) {
            val lines = mutableListOf<String>()
            if (elem.type.isNotEmpty()) lines.add("Type: ${elem.type}")
            if (elem.docRef.isNotEmpty()) lines.add("DocRef: ${elem.docRef}")
            val h = max(NODE_MIN_HEIGHT, (lines.size + 2) * LINE_HEIGHT + NODE_PADDING * 2)
            allNodes.add(NodeInfo(name, name, "«Element»", lines, h = h))
        }

        // 简单网格布局
        val titleH = if (title.isNotEmpty()) 40.0 else 0.0
        for ((idx, node) in allNodes.withIndex()) {
            val col = idx % NODES_PER_ROW
            val row = idx / NODES_PER_ROW
            node.x = MARGIN + col * (nodeW + SPACING_X)
            node.y = MARGIN + titleH + row * (NODE_MIN_HEIGHT + SPACING_Y + 40)
        }

        val nodeMap = allNodes.associateBy { it.id }

        // 计算视图大小
        val maxX = allNodes.maxOfOrNull { it.x + it.w } ?: 400.0
        val maxY = allNodes.maxOfOrNull { it.y + it.h } ?: 300.0
        val svgW = maxX + MARGIN
        val svgH = maxY + MARGIN

        return buildSvg {
            attr("id", diagramId)
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("xmlns:xlink", "http://www.w3.org/1999/xlink")

            // 箭头标记
            defs {
                marker {
                    attr("id", "arrowhead")
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
                addClass("requirement-diagram")

                // 标题
                if (title.isNotEmpty()) {
                    text(title, svgW / 2, MARGIN + 16) {
                        attr("text-anchor", "middle")
                        attr("font-size", "16")
                        attr("font-weight", "bold")
                        attr("fill", themeVariables.textColor)
                    }
                }

                // 节点
                group {
                    addClass("nodes")
                    for (node in allNodes) {
                        group {
                            addClass("requirement-node")
                            // 背景矩形
                            rect(node.x, node.y, node.w, node.h) {
                                attr("fill", fillColor)
                                attr("stroke", borderColor)
                                attr("stroke-width", "1")
                                attr("rx", "3")
                                attr("ry", "3")
                            }

                            // 类型标签（斜体）
                            text(node.typeLabel, node.x + node.w / 2, node.y + NODE_PADDING + TYPE_FONT_SIZE) {
                                attr("text-anchor", "middle")
                                attr("font-size", "$TYPE_FONT_SIZE")
                                attr("font-style", "italic")
                                attr("fill", themeVariables.textColor)
                            }

                            // 分隔线
                            val sepY = node.y + NODE_PADDING + TYPE_FONT_SIZE + 6
                            line(node.x, sepY, node.x + node.w, sepY) {
                                attr("stroke", borderColor)
                                attr("stroke-width", "0.5")
                            }

                            // 名称
                            text(node.label, node.x + node.w / 2, sepY + NAME_FONT_SIZE + 4) {
                                attr("text-anchor", "middle")
                                attr("font-size", "$NAME_FONT_SIZE")
                                attr("font-weight", "bold")
                                attr("fill", themeVariables.textColor)
                            }

                            // 详细信息
                            var detailY = sepY + NAME_FONT_SIZE + 8 + LINE_HEIGHT
                            for (detail in node.lines) {
                                text(detail, node.x + NODE_PADDING, detailY) {
                                    attr("font-size", "$DETAIL_FONT_SIZE")
                                    attr("fill", themeVariables.textColor)
                                }
                                detailY += LINE_HEIGHT
                            }
                        }
                    }
                }

                // 关系线
                group {
                    addClass("relations")
                    for (rel in relations) {
                        val srcNode = nodeMap[rel.src] ?: continue
                        val dstNode = nodeMap[rel.dst] ?: continue

                        val srcCX = srcNode.x + srcNode.w / 2
                        val srcCY = srcNode.y + srcNode.h / 2
                        val dstCX = dstNode.x + dstNode.w / 2
                        val dstCY = dstNode.y + dstNode.h / 2

                        // 从边缘出发
                        val (sx, sy) = getEdgePoint(srcNode.x, srcNode.y, srcNode.w, srcNode.h, dstCX, dstCY)
                        val (ex, ey) = getEdgePoint(dstNode.x, dstNode.y, dstNode.w, dstNode.h, srcCX, srcCY)

                        group {
                            addClass("relation")
                            // 连接线
                            line(sx, sy, ex, ey) {
                                attr("stroke", themeVariables.lineColor)
                                attr("stroke-width", "1.5")
                                if (rel.type != RequirementDb.RelationType.CONTAINS) {
                                    attr("stroke-dasharray", "10,7")
                                }
                                attr("marker-end", "url(#arrowhead)")
                            }
                            // 关系标签
                            val midX = (sx + ex) / 2
                            val midY = (sy + ey) / 2
                            text("«${rel.type.label}»", midX, midY - 5) {
                                attr("text-anchor", "middle")
                                attr("font-size", "$LABEL_FONT_SIZE")
                                attr("font-style", "italic")
                                attr("fill", themeVariables.textColor)
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

    /** 计算从节点矩形边缘出发的连接点 */
    // 简单的边缘点计算
    private fun getEdgePoint(
        x: Double, y: Double, w: Double, h: Double,
        targetX: Double, targetY: Double
    ): Pair<Double, Double> {
        val cx = x + w / 2
        val cy = y + h / 2
        val dx = targetX - cx
        val dy = targetY - cy

        return if (kotlin.math.abs(dx) * h > kotlin.math.abs(dy) * w) {
            // 从左或右边出发
            if (dx > 0) Pair(x + w, cy + dy * (w / 2) / dx.coerceAtLeast(0.001))
            else Pair(x, cy - dy * (w / 2) / (-dx).coerceAtLeast(0.001))
        } else {
            // 从上或下边出发
            if (dy > 0) Pair(cx + dx * (h / 2) / dy.coerceAtLeast(0.001), y + h)
            else Pair(cx - dx * (h / 2) / (-dy).coerceAtLeast(0.001), y)
        }
    }
}
