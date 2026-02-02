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
import kotlin.math.sqrt

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
        theme: MermaidTheme,
        useBezierCurve: Boolean = true
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

        // Draw the line using bezier curve or straight lines
        if (useBezierCurve && edge.points.size >= 2) {
            drawBezierEdgeLine(edge.points, theme.edgeStrokeColor, strokeWidth, pathEffect)
        } else {
            drawEdgeLine(edge.points, theme.edgeStrokeColor, strokeWidth, pathEffect)
        }

        // Draw start arrow
        if (edge.startArrow != ArrowHead.NONE && edge.points.size >= 2) {
            val start = edge.points[0]
            val next = if (edge.points.size > 2) edge.points[1] else edge.points[1]
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

    /**
     * Draw edge line using bezier curves for smooth appearance.
     */
    private fun DrawScope.drawBezierEdgeLine(
        points: List<Point>,
        color: Color,
        strokeWidth: Float,
        pathEffect: PathEffect?
    ) {
        if (points.size < 2) return

        val path = calculateSmoothBezierPath(points)

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
            ArrowHead.ARROW -> drawFilledArrow(point, angle, color)
            ArrowHead.CIRCLE -> drawCircleHead(point, color, strokeWidth)
            ArrowHead.CROSS -> drawCross(point, angle, color, strokeWidth)
            ArrowHead.NONE -> { /* Do nothing */ }
        }
    }

    /**
     * Draw a filled arrow head for better visibility.
     */
    private fun DrawScope.drawFilledArrow(
        point: Point,
        angle: Float,
        color: Color
    ) {
        val arrowAngle = PI.toFloat() / 6 // 30 degrees
        val size = ARROW_SIZE

        val x1 = point.x - size * cos(angle - arrowAngle)
        val y1 = point.y - size * sin(angle - arrowAngle)
        val x2 = point.x - size * cos(angle + arrowAngle)
        val y2 = point.y - size * sin(angle + arrowAngle)

        val path = Path().apply {
            moveTo(point.x, point.y)
            lineTo(x1, y1)
            lineTo(x2, y2)
            close()
        }

        drawPath(
            path = path,
            color = color,
            style = Fill
        )
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
     * Calculate a smooth bezier curve through control points.
     * Uses cubic bezier curves for 3+ points, quadratic for simpler cases.
     */
    public fun calculateSmoothBezierPath(points: List<Point>): Path {
        val path = Path()
        if (points.isEmpty()) return path

        path.moveTo(points[0].x, points[0].y)

        when (points.size) {
            1 -> { /* Just a point, nothing to draw */ }
            2 -> {
                // Simple straight line
                path.lineTo(points[1].x, points[1].y)
            }
            3 -> {
                // Use the middle point as a control point for quadratic bezier
                path.quadraticBezierTo(
                    points[1].x, points[1].y,
                    points[2].x, points[2].y
                )
            }
            4 -> {
                // For 4 points, use two middle points as control points for cubic bezier
                path.cubicTo(
                    points[1].x, points[1].y,
                    points[2].x, points[2].y,
                    points[3].x, points[3].y
                )
            }
            else -> {
                // For more points, use a series of cubic bezier curves
                // Using Catmull-Rom to Bezier conversion for smooth curves
                for (i in 0 until points.size - 1) {
                    val p0 = if (i > 0) points[i - 1] else points[i]
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val p3 = if (i + 2 < points.size) points[i + 2] else points[i + 1]
                    
                    // Convert Catmull-Rom to Bezier control points
                    val cp1x = p1.x + (p2.x - p0.x) / 6
                    val cp1y = p1.y + (p2.y - p0.y) / 6
                    val cp2x = p2.x - (p3.x - p1.x) / 6
                    val cp2y = p2.y - (p3.y - p1.y) / 6
                    
                    path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
                }
            }
        }

        return path
    }

    /**
     * Calculate a bezier curve through points for smoother edges.
     * Kept for backward compatibility.
     */
    public fun calculateBezierPath(points: List<Point>): Path {
        return calculateSmoothBezierPath(points)
    }
}
