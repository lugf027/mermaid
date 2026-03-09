package io.lugf027.github.mermaid.core.rendering.edges

import io.lugf027.github.mermaid.core.layout.LayoutEdge
import io.lugf027.github.mermaid.core.layout.Point
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.themes.ThemeVariables

/**
 * 边渲染器 - 对标 mermaid-js edges.js
 *
 * 根据 Edge 数据生成 SVG 路径（直线、折线、曲线），处理箭头标记引用和标签定位。
 */
object EdgeRenderer {

    /**
     * 渲染一条边
     *
     * @param edge 布局后的边数据（包含路径点）
     * @param diagramId 图表 ID（用于标记引用）
     * @param themeVariables 主题变量
     * @return SVG 组元素
     */
    fun render(edge: LayoutEdge, diagramId: String, themeVariables: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("edgePath")
        g.attr("id", "edge-${edge.id}")

        // 渲染路径
        val pathData = buildPathData(edge)
        val pathGroup = g.group {
            addClass("path")
        }

        pathGroup.path(pathData) {
            attr("fill", "none")
            attr("stroke", getStrokeColor(edge, themeVariables))
            attr("stroke-width", getStrokeWidth(edge))

            // 虚线样式
            if (edge.stroke == "dotted" || edge.thickness == "dotted") {
                attr("stroke-dasharray", "3")
            }

            // 箭头标记
            if (edge.arrowTypeEnd != null) {
                attr("marker-end", "url(#arrowhead-${diagramId})")
            }
        }

        // 渲染标签
        if (!edge.label.isNullOrEmpty()) {
            renderLabel(g, edge, themeVariables)
        }

        return g
    }

    /**
     * 构建路径数据
     */
    private fun buildPathData(edge: LayoutEdge): String {
        val points = edge.points
        if (points.isEmpty()) return ""

        val builder = SvgPathBuilder()
        builder.moveTo(points[0].x, points[0].y)

        if (points.size <= 2) {
            // 直线
            for (i in 1 until points.size) {
                builder.lineTo(points[i].x, points[i].y)
            }
        } else {
            // 使用贝塞尔曲线平滑路径
            for (i in 1 until points.size - 1 step 2) {
                val cp = points[i]
                val end = if (i + 1 < points.size) points[i + 1] else points.last()
                builder.quadTo(cp.x, cp.y, end.x, end.y)
            }

            // 如果剩余点未处理，用直线连接
            if (points.size % 2 == 0) {
                builder.lineTo(points.last().x, points.last().y)
            }
        }

        return builder.build()
    }

    /**
     * 渲染边标签
     */
    private fun renderLabel(g: SvgGroup, edge: LayoutEdge, tv: ThemeVariables) {
        val labelGroup = g.group {
            addClass("edgeLabel")
            translate(edge.x, edge.y)
        }

        // 背景矩形
        val label = edge.label!!
        val textWidth = label.length * 8.0 + 10
        val textHeight = 20.0

        labelGroup.rect(-textWidth / 2, -textHeight / 2, textWidth, textHeight) {
            attr("fill", tv.edgeLabelBackground)
            attr("stroke", "none")
            rounded(3.0)
        }

        labelGroup.text(label, 0.0, 5.0) {
            attr("text-anchor", "middle")
            attr("dominant-baseline", "middle")
            attr("fill", tv.textColor)
            attr("font-size", "12")
        }
    }

    private fun getStrokeColor(edge: LayoutEdge, tv: ThemeVariables): String {
        return when (edge.stroke) {
            "normal" -> tv.lineColor
            "thick" -> tv.lineColor
            else -> tv.lineColor
        }
    }

    private fun getStrokeWidth(edge: LayoutEdge): String {
        return when (edge.thickness) {
            "normal" -> "2"
            "thick" -> "3.5"
            "invisible" -> "0"
            else -> "2"
        }
    }
}
