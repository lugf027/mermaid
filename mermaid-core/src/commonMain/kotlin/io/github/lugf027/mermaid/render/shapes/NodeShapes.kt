/**
 * NodeShapes - Drawing utilities for different node shapes.
 */
package io.github.lugf027.mermaid.render.shapes

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.lugf027.mermaid.model.Bounds
import io.github.lugf027.mermaid.model.flowchart.NodeShape
import io.github.lugf027.mermaid.theme.MermaidTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Utility object for drawing various node shapes.
 */
public object NodeShapes {

    /**
     * Draw a node shape.
     */
    public fun DrawScope.drawNodeShape(
        shape: NodeShape,
        bounds: Bounds,
        theme: MermaidTheme
    ) {
        when (shape) {
            NodeShape.RECTANGLE -> drawRectangle(bounds, theme)
            NodeShape.ROUNDED -> drawRoundedRectangle(bounds, theme)
            NodeShape.STADIUM -> drawStadium(bounds, theme)
            NodeShape.SUBROUTINE -> drawSubroutine(bounds, theme)
            NodeShape.CYLINDER -> drawCylinder(bounds, theme)
            NodeShape.CIRCLE -> drawCircle(bounds, theme)
            NodeShape.DOUBLE_CIRCLE -> drawDoubleCircle(bounds, theme)
            NodeShape.ASYMMETRIC -> drawAsymmetric(bounds, theme)
            NodeShape.DIAMOND -> drawDiamond(bounds, theme)
            NodeShape.HEXAGON -> drawHexagon(bounds, theme)
            NodeShape.PARALLELOGRAM -> drawParallelogram(bounds, theme, slantRight = true)
            NodeShape.PARALLELOGRAM_ALT -> drawParallelogram(bounds, theme, slantRight = false)
            NodeShape.TRAPEZOID -> drawTrapezoid(bounds, theme, wideTop = true)
            NodeShape.TRAPEZOID_ALT -> drawTrapezoid(bounds, theme, wideTop = false)
        }
    }

    private fun DrawScope.drawRectangle(bounds: Bounds, theme: MermaidTheme) {
        val rect = Rect(bounds.x, bounds.y, bounds.right, bounds.bottom)
        
        // Fill
        drawRect(
            color = theme.nodeFillColor,
            topLeft = Offset(bounds.x, bounds.y),
            size = Size(bounds.width, bounds.height)
        )
        
        // Stroke
        drawRect(
            color = theme.nodeStrokeColor,
            topLeft = Offset(bounds.x, bounds.y),
            size = Size(bounds.width, bounds.height),
            style = Stroke(width = theme.nodeStrokeWidth)
        )
    }

    private fun DrawScope.drawRoundedRectangle(bounds: Bounds, theme: MermaidTheme) {
        val cornerRadius = CornerRadius(theme.cornerRadius)
        
        // Fill
        drawRoundRect(
            color = theme.nodeFillColor,
            topLeft = Offset(bounds.x, bounds.y),
            size = Size(bounds.width, bounds.height),
            cornerRadius = cornerRadius
        )
        
        // Stroke
        drawRoundRect(
            color = theme.nodeStrokeColor,
            topLeft = Offset(bounds.x, bounds.y),
            size = Size(bounds.width, bounds.height),
            cornerRadius = cornerRadius,
            style = Stroke(width = theme.nodeStrokeWidth)
        )
    }

    private fun DrawScope.drawStadium(bounds: Bounds, theme: MermaidTheme) {
        val cornerRadius = CornerRadius(bounds.height / 2)
        
        // Fill
        drawRoundRect(
            color = theme.nodeFillColor,
            topLeft = Offset(bounds.x, bounds.y),
            size = Size(bounds.width, bounds.height),
            cornerRadius = cornerRadius
        )
        
        // Stroke
        drawRoundRect(
            color = theme.nodeStrokeColor,
            topLeft = Offset(bounds.x, bounds.y),
            size = Size(bounds.width, bounds.height),
            cornerRadius = cornerRadius,
            style = Stroke(width = theme.nodeStrokeWidth)
        )
    }

    private fun DrawScope.drawSubroutine(bounds: Bounds, theme: MermaidTheme) {
        val inset = 8f
        
        // Outer rectangle - Fill
        drawRect(
            color = theme.nodeFillColor,
            topLeft = Offset(bounds.x, bounds.y),
            size = Size(bounds.width, bounds.height)
        )
        
        // Outer rectangle - Stroke
        drawRect(
            color = theme.nodeStrokeColor,
            topLeft = Offset(bounds.x, bounds.y),
            size = Size(bounds.width, bounds.height),
            style = Stroke(width = theme.nodeStrokeWidth)
        )
        
        // Left vertical line
        drawLine(
            color = theme.nodeStrokeColor,
            start = Offset(bounds.x + inset, bounds.y),
            end = Offset(bounds.x + inset, bounds.bottom),
            strokeWidth = theme.nodeStrokeWidth
        )
        
        // Right vertical line
        drawLine(
            color = theme.nodeStrokeColor,
            start = Offset(bounds.right - inset, bounds.y),
            end = Offset(bounds.right - inset, bounds.bottom),
            strokeWidth = theme.nodeStrokeWidth
        )
    }

    private fun DrawScope.drawCylinder(bounds: Bounds, theme: MermaidTheme) {
        val ellipseHeight = bounds.height * 0.2f
        
        val path = Path().apply {
            // Top ellipse
            moveTo(bounds.x, bounds.y + ellipseHeight / 2)
            cubicTo(
                bounds.x, bounds.y - ellipseHeight / 2,
                bounds.right, bounds.y - ellipseHeight / 2,
                bounds.right, bounds.y + ellipseHeight / 2
            )
            
            // Right side
            lineTo(bounds.right, bounds.bottom - ellipseHeight / 2)
            
            // Bottom ellipse
            cubicTo(
                bounds.right, bounds.bottom + ellipseHeight / 2,
                bounds.x, bounds.bottom + ellipseHeight / 2,
                bounds.x, bounds.bottom - ellipseHeight / 2
            )
            
            // Left side
            close()
        }
        
        drawPath(path, color = theme.nodeFillColor, style = Fill)
        drawPath(path, color = theme.nodeStrokeColor, style = Stroke(width = theme.nodeStrokeWidth))
        
        // Inner ellipse at top
        val topEllipsePath = Path().apply {
            moveTo(bounds.x, bounds.y + ellipseHeight / 2)
            cubicTo(
                bounds.x, bounds.y + ellipseHeight * 1.5f,
                bounds.right, bounds.y + ellipseHeight * 1.5f,
                bounds.right, bounds.y + ellipseHeight / 2
            )
        }
        drawPath(topEllipsePath, color = theme.nodeStrokeColor, style = Stroke(width = theme.nodeStrokeWidth))
    }

    private fun DrawScope.drawCircle(bounds: Bounds, theme: MermaidTheme) {
        val radius = minOf(bounds.width, bounds.height) / 2
        val center = Offset(bounds.centerX, bounds.centerY)
        
        drawCircle(
            color = theme.nodeFillColor,
            radius = radius,
            center = center
        )
        
        drawCircle(
            color = theme.nodeStrokeColor,
            radius = radius,
            center = center,
            style = Stroke(width = theme.nodeStrokeWidth)
        )
    }

    private fun DrawScope.drawDoubleCircle(bounds: Bounds, theme: MermaidTheme) {
        val outerRadius = minOf(bounds.width, bounds.height) / 2
        val innerRadius = outerRadius - 6f
        val center = Offset(bounds.centerX, bounds.centerY)
        
        drawCircle(
            color = theme.nodeFillColor,
            radius = outerRadius,
            center = center
        )
        
        drawCircle(
            color = theme.nodeStrokeColor,
            radius = outerRadius,
            center = center,
            style = Stroke(width = theme.nodeStrokeWidth)
        )
        
        drawCircle(
            color = theme.nodeStrokeColor,
            radius = innerRadius,
            center = center,
            style = Stroke(width = theme.nodeStrokeWidth)
        )
    }

    private fun DrawScope.drawAsymmetric(bounds: Bounds, theme: MermaidTheme) {
        val flagWidth = bounds.height * 0.3f
        
        val path = Path().apply {
            moveTo(bounds.x, bounds.y)
            lineTo(bounds.right - flagWidth, bounds.y)
            lineTo(bounds.right, bounds.centerY)
            lineTo(bounds.right - flagWidth, bounds.bottom)
            lineTo(bounds.x, bounds.bottom)
            close()
        }
        
        drawPath(path, color = theme.nodeFillColor, style = Fill)
        drawPath(path, color = theme.nodeStrokeColor, style = Stroke(width = theme.nodeStrokeWidth))
    }

    private fun DrawScope.drawDiamond(bounds: Bounds, theme: MermaidTheme) {
        val path = Path().apply {
            moveTo(bounds.centerX, bounds.y)
            lineTo(bounds.right, bounds.centerY)
            lineTo(bounds.centerX, bounds.bottom)
            lineTo(bounds.x, bounds.centerY)
            close()
        }
        
        drawPath(path, color = theme.nodeFillColor, style = Fill)
        drawPath(path, color = theme.nodeStrokeColor, style = Stroke(width = theme.nodeStrokeWidth))
    }

    private fun DrawScope.drawHexagon(bounds: Bounds, theme: MermaidTheme) {
        val inset = bounds.height * 0.25f
        
        val path = Path().apply {
            moveTo(bounds.x + inset, bounds.y)
            lineTo(bounds.right - inset, bounds.y)
            lineTo(bounds.right, bounds.centerY)
            lineTo(bounds.right - inset, bounds.bottom)
            lineTo(bounds.x + inset, bounds.bottom)
            lineTo(bounds.x, bounds.centerY)
            close()
        }
        
        drawPath(path, color = theme.nodeFillColor, style = Fill)
        drawPath(path, color = theme.nodeStrokeColor, style = Stroke(width = theme.nodeStrokeWidth))
    }

    private fun DrawScope.drawParallelogram(bounds: Bounds, theme: MermaidTheme, slantRight: Boolean) {
        val slant = bounds.height * 0.3f
        
        val path = Path().apply {
            if (slantRight) {
                moveTo(bounds.x + slant, bounds.y)
                lineTo(bounds.right, bounds.y)
                lineTo(bounds.right - slant, bounds.bottom)
                lineTo(bounds.x, bounds.bottom)
            } else {
                moveTo(bounds.x, bounds.y)
                lineTo(bounds.right - slant, bounds.y)
                lineTo(bounds.right, bounds.bottom)
                lineTo(bounds.x + slant, bounds.bottom)
            }
            close()
        }
        
        drawPath(path, color = theme.nodeFillColor, style = Fill)
        drawPath(path, color = theme.nodeStrokeColor, style = Stroke(width = theme.nodeStrokeWidth))
    }

    private fun DrawScope.drawTrapezoid(bounds: Bounds, theme: MermaidTheme, wideTop: Boolean) {
        val inset = bounds.width * 0.15f
        
        val path = Path().apply {
            if (wideTop) {
                moveTo(bounds.x, bounds.y)
                lineTo(bounds.right, bounds.y)
                lineTo(bounds.right - inset, bounds.bottom)
                lineTo(bounds.x + inset, bounds.bottom)
            } else {
                moveTo(bounds.x + inset, bounds.y)
                lineTo(bounds.right - inset, bounds.y)
                lineTo(bounds.right, bounds.bottom)
                lineTo(bounds.x, bounds.bottom)
            }
            close()
        }
        
        drawPath(path, color = theme.nodeFillColor, style = Fill)
        drawPath(path, color = theme.nodeStrokeColor, style = Stroke(width = theme.nodeStrokeWidth))
    }
}
