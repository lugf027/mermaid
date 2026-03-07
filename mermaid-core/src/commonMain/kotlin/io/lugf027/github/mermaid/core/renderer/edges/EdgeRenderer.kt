package io.lugf027.github.mermaid.core.renderer.edges

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import io.lugf027.github.mermaid.core.types.Edge
import io.lugf027.github.mermaid.core.types.EdgeType
import io.lugf027.github.mermaid.core.types.Point
import io.lugf027.github.mermaid.core.types.StrokeType
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * 边渲染器。
 * 根据 Edge 类型绘制不同样式的连线和箭头。
 */
object EdgeRenderer {

    /**
     * 绘制边。
     */
    fun DrawScope.drawEdge(
        edge: Edge,
        color: Color = Color.Black,
        textMeasurer: TextMeasurer,
        textColor: Color = Color.Black,
    ) {
        val points = edge.points
        if (points.size < 2) return

        val strokeWidth = when (edge.stroke) {
            StrokeType.THICK -> 3.5f
            else -> 2f
        }
        val pathEffect = when (edge.stroke) {
            StrokeType.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
            else -> null
        }

        // 绘制路径
        val path = buildPath(points)
        drawPath(path, color, style = Stroke(width = strokeWidth, pathEffect = pathEffect))

        // 绘制起始端箭头
        if (edge.arrowTypeStart != EdgeType.ARROW_NONE && points.size >= 2) {
            val p0 = points[0]
            val p1 = points[1]
            val angle = atan2(p0.y - p1.y, p0.x - p1.x)
            drawArrow(Offset(p0.x, p0.y), angle, edge.arrowTypeStart, color)
        }

        // 绘制结束端箭头
        if (edge.arrowTypeEnd != EdgeType.ARROW_NONE && points.size >= 2) {
            val pLast = points.last()
            val pPrev = points[points.size - 2]
            val angle = atan2(pLast.y - pPrev.y, pLast.x - pPrev.x)
            drawArrow(Offset(pLast.x, pLast.y), angle, edge.arrowTypeEnd, color)
        }

        // 绘制标签
        if (edge.label.isNotEmpty()) {
            val labelPos = edge.labelPos ?: calculateLabelPosition(points)
            val style = TextStyle(fontSize = 12.sp, color = textColor)
            val result = textMeasurer.measure(edge.label, style)
            // 背景
            drawRect(
                Color(0xFFE8E8E8),
                topLeft = Offset(labelPos.x - result.size.width / 2f - 2, labelPos.y - result.size.height / 2f - 1),
                size = androidx.compose.ui.geometry.Size(result.size.width + 4f, result.size.height + 2f),
            )
            drawText(
                textLayoutResult = result,
                topLeft = Offset(labelPos.x - result.size.width / 2f, labelPos.y - result.size.height / 2f),
            )
        }
    }

    /**
     * 绘制箭头。
     */
    private fun DrawScope.drawArrow(
        tip: Offset,
        angle: Float,
        type: EdgeType,
        color: Color,
    ) {
        val arrowSize = 10f
        when (type) {
            EdgeType.ARROW_POINT -> {
                val left = Offset(
                    tip.x - arrowSize * cos(angle - 0.4f),
                    tip.y - arrowSize * sin(angle - 0.4f),
                )
                val right = Offset(
                    tip.x - arrowSize * cos(angle + 0.4f),
                    tip.y - arrowSize * sin(angle + 0.4f),
                )
                val path = Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(left.x, left.y)
                    lineTo(right.x, right.y)
                    close()
                }
                drawPath(path, color)
            }
            EdgeType.ARROW_CIRCLE -> {
                val cx = tip.x - (arrowSize / 2f) * cos(angle)
                val cy = tip.y - (arrowSize / 2f) * sin(angle)
                drawCircle(color, arrowSize / 2f, Offset(cx, cy))
            }
            EdgeType.ARROW_CROSS -> {
                val size = arrowSize * 0.6f
                drawLine(color, Offset(tip.x - size, tip.y - size), Offset(tip.x + size, tip.y + size), 2f)
                drawLine(color, Offset(tip.x - size, tip.y + size), Offset(tip.x + size, tip.y - size), 2f)
            }
            EdgeType.ARROW_OPEN -> {
                val left = Offset(
                    tip.x - arrowSize * cos(angle - 0.4f),
                    tip.y - arrowSize * sin(angle - 0.4f),
                )
                val right = Offset(
                    tip.x - arrowSize * cos(angle + 0.4f),
                    tip.y - arrowSize * sin(angle + 0.4f),
                )
                drawLine(color, left, tip, 2f)
                drawLine(color, right, tip, 2f)
            }
            else -> {}
        }
    }

    /**
     * 构建平滑路径。
     */
    private fun buildPath(points: List<Point>): Path {
        return Path().apply {
            moveTo(points.first().x, points.first().y)
            if (points.size == 2) {
                lineTo(points.last().x, points.last().y)
            } else {
                for (i in 1 until points.size - 1) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    val next = points[i + 1]
                    val cx1 = (prev.x + curr.x) / 2f
                    val cy1 = (prev.y + curr.y) / 2f
                    val cx2 = (curr.x + next.x) / 2f
                    val cy2 = (curr.y + next.y) / 2f
                    quadraticBezierTo(curr.x, curr.y, cx2, cy2)
                }
                val last = points.last()
                lineTo(last.x, last.y)
            }
        }
    }

    /**
     * 计算标签位置（路径中点）。
     */
    private fun calculateLabelPosition(points: List<Point>): Point {
        if (points.size < 2) return points.firstOrNull() ?: Point()
        val mid = points.size / 2
        return if (points.size % 2 == 0) {
            val a = points[mid - 1]
            val b = points[mid]
            Point((a.x + b.x) / 2f, (a.y + b.y) / 2f)
        } else {
            points[mid]
        }
    }
}
