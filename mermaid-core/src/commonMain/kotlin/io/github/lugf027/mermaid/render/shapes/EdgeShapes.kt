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
    public fun calculateSmoothBezierPath(points: List<Point>, cornerRadius: Float = 8f): Path {
        val path = Path()
        if (points.isEmpty()) return path

        // For orthogonal paths (paths with right angles), use rounded corners
        if (hasOrthogonalSegments(points)) {
            return generateRoundedPath(points, cornerRadius)
        }

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
     * Check if the path has orthogonal (right angle) segments.
     * This is typical for back edges and routed paths.
     */
    private fun hasOrthogonalSegments(points: List<Point>): Boolean {
        if (points.size < 3) return false
        
        for (i in 1 until points.size - 1) {
            val prev = points[i - 1]
            val curr = points[i]
            val next = points[i + 1]
            
            // Check if this is a corner (approximately 90 degrees)
            val dx1 = curr.x - prev.x
            val dy1 = curr.y - prev.y
            val dx2 = next.x - curr.x
            val dy2 = next.y - curr.y
            
            // Orthogonal if one segment is mostly horizontal and the other is mostly vertical
            val isHorizontal1 = kotlin.math.abs(dy1) < kotlin.math.abs(dx1) * 0.1f
            val isVertical1 = kotlin.math.abs(dx1) < kotlin.math.abs(dy1) * 0.1f
            val isHorizontal2 = kotlin.math.abs(dy2) < kotlin.math.abs(dx2) * 0.1f
            val isVertical2 = kotlin.math.abs(dx2) < kotlin.math.abs(dy2) * 0.1f
            
            if ((isHorizontal1 && isVertical2) || (isVertical1 && isHorizontal2)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Generate a path with rounded corners at each turn.
     * Based on mermaid-js generateRoundedPath function.
     * 
     * @param points Array of points defining the path
     * @param radius Corner radius for the rounded corners
     * @return Path with rounded corners using quadratic bezier curves
     */
    public fun generateRoundedPath(points: List<Point>, radius: Float = 8f): Path {
        val path = Path()
        if (points.isEmpty()) return path
        if (points.size == 1) {
            path.moveTo(points[0].x, points[0].y)
            return path
        }
        if (points.size == 2) {
            path.moveTo(points[0].x, points[0].y)
            path.lineTo(points[1].x, points[1].y)
            return path
        }
        
        path.moveTo(points[0].x, points[0].y)
        
        for (i in 1 until points.size - 1) {
            val prev = points[i - 1]
            val curr = points[i]
            val next = points[i + 1]
            
            // Calculate vectors from current point to neighbors
            val dx1 = curr.x - prev.x
            val dy1 = curr.y - prev.y
            val dx2 = next.x - curr.x
            val dy2 = next.y - curr.y
            
            // Calculate lengths
            val len1 = sqrt((dx1 * dx1 + dy1 * dy1).toDouble()).toFloat()
            val len2 = sqrt((dx2 * dx2 + dy2 * dy2).toDouble()).toFloat()
            
            if (len1 == 0f || len2 == 0f) {
                path.lineTo(curr.x, curr.y)
                continue
            }
            
            // Calculate the angle between the two segments
            val dotProduct = dx1 * dx2 + dy1 * dy2
            val crossProduct = dx1 * dy2 - dy1 * dx2
            val angle = kotlin.math.abs(atan2(crossProduct.toDouble(), dotProduct.toDouble())).toFloat()
            
            // If the angle is very small (nearly straight), just draw a line
            if (angle < 0.1f) {
                path.lineTo(curr.x, curr.y)
                continue
            }
            
            // Calculate the cut length for the corner
            // The cut length is limited by the available segment length and the desired radius
            val halfAngle = angle / 2
            val tanHalfAngle = kotlin.math.tan(halfAngle.toDouble()).toFloat()
            val idealCutLen = if (tanHalfAngle != 0f) radius / tanHalfAngle else radius
            val cutLen = minOf(idealCutLen, len1 / 2, len2 / 2)
            
            // Calculate the start point of the curve (on the incoming segment)
            val startX = curr.x - (dx1 / len1) * cutLen
            val startY = curr.y - (dy1 / len1) * cutLen
            
            // Calculate the end point of the curve (on the outgoing segment)
            val endX = curr.x + (dx2 / len2) * cutLen
            val endY = curr.y + (dy2 / len2) * cutLen
            
            // Draw line to the start of the curve
            path.lineTo(startX, startY)
            
            // Draw the rounded corner using a quadratic bezier curve
            // The control point is the original corner point
            path.quadraticBezierTo(curr.x, curr.y, endX, endY)
        }
        
        // Draw line to the last point
        path.lineTo(points.last().x, points.last().y)
        
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
