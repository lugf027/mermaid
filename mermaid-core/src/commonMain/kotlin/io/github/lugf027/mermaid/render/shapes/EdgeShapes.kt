/**
 * EdgeShapes - Drawing utilities for edges and arrows.
 */
package io.github.lugf027.mermaid.render.shapes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.lugf027.mermaid.model.flowchart.ArrowHead
import io.github.lugf027.mermaid.model.flowchart.FlowEdge
import io.github.lugf027.mermaid.model.flowchart.LinkType
import io.github.lugf027.mermaid.model.flowchart.Point
import io.github.lugf027.mermaid.theme.MermaidTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Utility object for drawing edges and arrows.
 */
public object EdgeShapes {

    private const val ARROW_SIZE = 10f
    private const val CIRCLE_RADIUS = 5f

    /**
     * Draw an edge with its line and arrow heads.
     */
    public fun DrawScope.drawEdge(
        edge: FlowEdge,
        theme: MermaidTheme
    ) {
        if (edge.points.size < 2) return

        val strokeWidth = when {
            edge.linkType.isThick -> theme.edgeStrokeWidth * 2
            else -> theme.edgeStrokeWidth
        }

        val pathEffect = when {
            edge.linkType.isDotted -> PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
            edge.linkType == LinkType.INVISIBLE -> return // Don't draw invisible links
            else -> null
        }

        // Draw the line
        drawEdgeLine(edge.points, theme.edgeStrokeColor, strokeWidth, pathEffect)

        // Draw start arrow
        if (edge.startArrow != ArrowHead.NONE && edge.points.size >= 2) {
            val start = edge.points[0]
            val next = edge.points[1]
            drawArrowHead(
                point = start,
                angle = atan2((start.y - next.y).toDouble(), (start.x - next.x).toDouble()).toFloat(),
                arrowHead = edge.startArrow,
                color = theme.edgeStrokeColor,
                strokeWidth = strokeWidth
            )
        }

        // Draw end arrow
        if (edge.endArrow != ArrowHead.NONE && edge.points.size >= 2) {
            val end = edge.points.last()
            val prev = edge.points[edge.points.size - 2]
            drawArrowHead(
                point = end,
                angle = atan2((end.y - prev.y).toDouble(), (end.x - prev.x).toDouble()).toFloat(),
                arrowHead = edge.endArrow,
                color = theme.edgeStrokeColor,
                strokeWidth = strokeWidth
            )
        }

        // Draw label if present
        edge.label?.let { label ->
            if (label.isNotEmpty() && edge.points.size >= 2) {
                drawEdgeLabel(edge.points, label, theme)
            }
        }
    }

    private fun DrawScope.drawEdgeLine(
        points: List<Point>,
        color: Color,
        strokeWidth: Float,
        pathEffect: PathEffect?
    ) {
        if (points.size < 2) return

        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = pathEffect
            )
        )
    }

    private fun DrawScope.drawArrowHead(
        point: Point,
        angle: Float,
        arrowHead: ArrowHead,
        color: Color,
        strokeWidth: Float
    ) {
        when (arrowHead) {
            ArrowHead.ARROW -> drawArrow(point, angle, color, strokeWidth)
            ArrowHead.CIRCLE -> drawCircleHead(point, color, strokeWidth)
            ArrowHead.CROSS -> drawCross(point, angle, color, strokeWidth)
            ArrowHead.NONE -> { /* Do nothing */ }
        }
    }

    private fun DrawScope.drawArrow(
        point: Point,
        angle: Float,
        color: Color,
        strokeWidth: Float
    ) {
        val arrowAngle = PI.toFloat() / 6 // 30 degrees

        val x1 = point.x - ARROW_SIZE * cos(angle - arrowAngle)
        val y1 = point.y - ARROW_SIZE * sin(angle - arrowAngle)
        val x2 = point.x - ARROW_SIZE * cos(angle + arrowAngle)
        val y2 = point.y - ARROW_SIZE * sin(angle + arrowAngle)

        val path = Path().apply {
            moveTo(point.x, point.y)
            lineTo(x1, y1)
            moveTo(point.x, point.y)
            lineTo(x2, y2)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }

    private fun DrawScope.drawCircleHead(
        point: Point,
        color: Color,
        strokeWidth: Float
    ) {
        drawCircle(
            color = color,
            radius = CIRCLE_RADIUS,
            center = Offset(point.x, point.y),
            style = Stroke(width = strokeWidth)
        )
    }

    private fun DrawScope.drawCross(
        point: Point,
        angle: Float,
        color: Color,
        strokeWidth: Float
    ) {
        val size = ARROW_SIZE * 0.7f
        val crossAngle = PI.toFloat() / 4 // 45 degrees

        // First line of X
        drawLine(
            color = color,
            start = Offset(
                point.x - size * cos(angle + crossAngle),
                point.y - size * sin(angle + crossAngle)
            ),
            end = Offset(
                point.x + size * cos(angle + crossAngle),
                point.y + size * sin(angle + crossAngle)
            ),
            strokeWidth = strokeWidth
        )

        // Second line of X
        drawLine(
            color = color,
            start = Offset(
                point.x - size * cos(angle - crossAngle),
                point.y - size * sin(angle - crossAngle)
            ),
            end = Offset(
                point.x + size * cos(angle - crossAngle),
                point.y + size * sin(angle - crossAngle)
            ),
            strokeWidth = strokeWidth
        )
    }

    private fun DrawScope.drawEdgeLabel(
        points: List<Point>,
        label: String,
        theme: MermaidTheme
    ) {
        // Find midpoint of the edge
        val midIndex = points.size / 2
        val midPoint = if (points.size % 2 == 0 && midIndex > 0) {
            Point(
                (points[midIndex - 1].x + points[midIndex].x) / 2,
                (points[midIndex - 1].y + points[midIndex].y) / 2
            )
        } else {
            points[midIndex]
        }

        // Estimate label size
        val labelWidth = label.length * 8f + 16f
        val labelHeight = 20f

        // Draw background
        drawRoundRect(
            color = theme.edgeLabelBackground,
            topLeft = Offset(midPoint.x - labelWidth / 2, midPoint.y - labelHeight / 2),
            size = androidx.compose.ui.geometry.Size(labelWidth, labelHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
        )

        // Note: Actual text drawing would be done in the renderer with TextMeasurer
    }

    /**
     * Calculate a bezier curve through points for smoother edges.
     */
    public fun calculateBezierPath(points: List<Point>): Path {
        val path = Path()
        if (points.isEmpty()) return path

        path.moveTo(points[0].x, points[0].y)

        if (points.size == 2) {
            path.lineTo(points[1].x, points[1].y)
        } else {
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val current = points[i]
                val midX = (prev.x + current.x) / 2
                val midY = (prev.y + current.y) / 2

                if (i == 1) {
                    path.lineTo(midX, midY)
                } else {
                    path.quadraticBezierTo(prev.x, prev.y, midX, midY)
                }

                if (i == points.size - 1) {
                    path.lineTo(current.x, current.y)
                }
            }
        }

        return path
    }
}
