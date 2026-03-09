package io.lugf027.github.mermaid.core.rendering.edges

import io.lugf027.github.mermaid.core.layout.LayoutEdge
import io.lugf027.github.mermaid.core.layout.Point
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.TextUtils

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
     * 构建路径数据 - 使用直线段连接路径点
     *
     * mermaid-js 的简单流程图边使用 d3.line() 生成的直线路径
     */
    private fun buildPathData(edge: LayoutEdge): String {
        val points = edge.points
        if (points.isEmpty()) return ""

        val builder = SvgPathBuilder()
        builder.moveTo(points[0].x, points[0].y)

        // 使用直线段连接所有路径点
        for (i in 1 until points.size) {
            builder.lineTo(points[i].x, points[i].y)
        }

        return builder.build()
    }

    /**
     * 渲染边标签 - 对齐 mermaid-js 的 edgeLabel 样式
     *
     * mermaid-js 中边标签继承 SVG 根元素的 16px 字号，
     * rect 尺寸 = 文本宽度 × 24px（foreignObject 高度），
     * 居中于 edgeLabel 的 translate 位置。
     */
    private fun renderLabel(g: SvgGroup, edge: LayoutEdge, tv: ThemeVariables) {
        val labelGroup = g.group {
            addClass("edgeLabel")
            translate(edge.x, edge.y)
        }

        val label = edge.label!!
        // mermaid-js 使用 16px 继承字号测量标签宽度
        val textWidth = TextUtils.estimateTextWidth(label, 16.0)
        val textHeight = 24.0  // mermaid-js foreignObject 高度

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
