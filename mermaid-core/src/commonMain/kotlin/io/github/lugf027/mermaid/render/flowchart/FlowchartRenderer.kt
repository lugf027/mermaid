/**
 * FlowchartRenderer - Render engine for flowchart diagrams.
 */
package io.github.lugf027.mermaid.render.flowchart

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.lugf027.mermaid.layout.LayoutConfig
import io.github.lugf027.mermaid.model.flowchart.FlowchartData
import io.github.lugf027.mermaid.model.flowchart.FlowEdge
import io.github.lugf027.mermaid.model.flowchart.FlowSubgraph
import io.github.lugf027.mermaid.model.flowchart.FlowVertex
import io.github.lugf027.mermaid.render.RenderEngine
import io.github.lugf027.mermaid.render.shapes.EdgeShapes
import io.github.lugf027.mermaid.render.shapes.EdgeShapes.drawEdge
import io.github.lugf027.mermaid.render.shapes.NodeShapes
import io.github.lugf027.mermaid.render.shapes.NodeShapes.drawNodeShape
import io.github.lugf027.mermaid.theme.MermaidTheme

/**
 * Render engine for flowchart diagrams.
 */
public object FlowchartRenderer : RenderEngine<FlowchartData> {
    
    // Default layout config for title margins (can be overridden)
    private val defaultLayoutConfig = LayoutConfig()

    override fun DrawScope.render(data: FlowchartData, theme: MermaidTheme) {
        // Draw background
        drawRect(
            color = theme.backgroundColor,
            topLeft = Offset.Zero,
            size = size
        )

        // Draw subgraphs (back to front)
        drawSubgraphs(data, theme)

        // Draw edges
        drawEdges(data, theme)

        // Draw nodes
        drawNodes(data, theme)
    }

    /**
     * Render with text measurer for proper text layout.
     */
    public fun DrawScope.renderWithTextMeasurer(
        data: FlowchartData,
        theme: MermaidTheme,
        textMeasurer: TextMeasurer,
        layoutConfig: LayoutConfig = defaultLayoutConfig
    ) {
        // Draw background
        drawRect(
            color = theme.backgroundColor,
            topLeft = Offset.Zero,
            size = size
        )

        // Draw subgraphs (back to front)
        drawSubgraphsWithText(data, theme, textMeasurer, layoutConfig)

        // Draw edges
        drawEdgesWithLabels(data, theme, textMeasurer)

        // Draw nodes with text
        drawNodesWithText(data, theme, textMeasurer)
    }

    private fun DrawScope.drawSubgraphs(data: FlowchartData, theme: MermaidTheme) {
        // Draw subgraphs in order (parents before children)
        for (subgraphId in data.rootSubgraphIds) {
            drawSubgraphRecursive(data, subgraphId, theme)
        }
    }

    private fun DrawScope.drawSubgraphRecursive(
        data: FlowchartData,
        subgraphId: String,
        theme: MermaidTheme
    ) {
        val subgraph = data.subgraphs[subgraphId] ?: return
        val bounds = subgraph.bounds

        if (bounds.width > 0 && bounds.height > 0) {
            // Draw subgraph background
            drawRoundRect(
                color = theme.subgraphFillColor,
                topLeft = Offset(bounds.x, bounds.y),
                size = Size(bounds.width, bounds.height),
                cornerRadius = CornerRadius(theme.cornerRadius)
            )

            // Draw subgraph border
            drawRoundRect(
                color = theme.subgraphStrokeColor,
                topLeft = Offset(bounds.x, bounds.y),
                size = Size(bounds.width, bounds.height),
                cornerRadius = CornerRadius(theme.cornerRadius),
                style = Stroke(width = theme.nodeStrokeWidth)
            )
        }

        // Draw nested subgraphs
        for (nestedId in subgraph.subgraphIds) {
            drawSubgraphRecursive(data, nestedId, theme)
        }
    }

    private fun DrawScope.drawSubgraphsWithText(
        data: FlowchartData,
        theme: MermaidTheme,
        textMeasurer: TextMeasurer,
        layoutConfig: LayoutConfig
    ) {
        for (subgraphId in data.rootSubgraphIds) {
            drawSubgraphWithTextRecursive(data, subgraphId, theme, textMeasurer, layoutConfig)
        }
    }

    private fun DrawScope.drawSubgraphWithTextRecursive(
        data: FlowchartData,
        subgraphId: String,
        theme: MermaidTheme,
        textMeasurer: TextMeasurer,
        layoutConfig: LayoutConfig
    ) {
        val subgraph = data.subgraphs[subgraphId] ?: return
        val bounds = subgraph.bounds

        if (bounds.width > 0 && bounds.height > 0) {
            // Draw subgraph background
            drawRoundRect(
                color = theme.subgraphFillColor,
                topLeft = Offset(bounds.x, bounds.y),
                size = Size(bounds.width, bounds.height),
                cornerRadius = CornerRadius(theme.cornerRadius)
            )

            // Draw subgraph border
            drawRoundRect(
                color = theme.subgraphStrokeColor,
                topLeft = Offset(bounds.x, bounds.y),
                size = Size(bounds.width, bounds.height),
                cornerRadius = CornerRadius(theme.cornerRadius),
                style = Stroke(width = theme.nodeStrokeWidth)
            )

            // Draw title with proper margins
            subgraph.title?.let { title ->
                val textLayoutResult = textMeasurer.measure(
                    text = title,
                    style = TextStyle(
                        fontSize = theme.fontSize.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.textColor
                    )
                )
                
                // Position title using configured margins
                // Title is positioned at top-left of subgraph with proper margins
                val titleX = bounds.x + layoutConfig.subgraphPadding
                val titleY = bounds.y + layoutConfig.subgraphTitleTopMargin
                
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(titleX, titleY)
                )
            }
        }

        // Draw nested subgraphs
        for (nestedId in subgraph.subgraphIds) {
            drawSubgraphWithTextRecursive(data, nestedId, theme, textMeasurer, layoutConfig)
        }
    }

    private fun DrawScope.drawEdges(data: FlowchartData, theme: MermaidTheme) {
        for (edge in data.edges) {
            drawEdge(edge, theme)
        }
    }

    private fun DrawScope.drawEdgesWithLabels(
        data: FlowchartData,
        theme: MermaidTheme,
        textMeasurer: TextMeasurer
    ) {
        for (edge in data.edges) {
            drawEdge(edge, theme)

            // Draw edge label
            edge.label?.let { label ->
                if (label.isNotEmpty() && edge.points.size >= 2) {
                    drawEdgeLabelText(edge, label, theme, textMeasurer)
                }
            }
        }
    }

    private fun DrawScope.drawEdgeLabelText(
        edge: FlowEdge,
        label: String,
        theme: MermaidTheme,
        textMeasurer: TextMeasurer
    ) {
        val points = edge.points
        if (points.size < 2) return

        // Find midpoint
        val midIndex = points.size / 2
        val midPoint = if (points.size % 2 == 0 && midIndex > 0) {
            Offset(
                (points[midIndex - 1].x + points[midIndex].x) / 2,
                (points[midIndex - 1].y + points[midIndex].y) / 2
            )
        } else {
            Offset(points[midIndex].x, points[midIndex].y)
        }

        val textLayoutResult = textMeasurer.measure(
            text = label,
            style = TextStyle(
                fontSize = theme.edgeLabelFontSize.sp,
                color = theme.textColor
            )
        )

        val textWidth = textLayoutResult.size.width.toFloat()
        val textHeight = textLayoutResult.size.height.toFloat()
        val padding = 4f

        // Draw background
        drawRoundRect(
            color = theme.edgeLabelBackground,
            topLeft = Offset(
                midPoint.x - textWidth / 2 - padding,
                midPoint.y - textHeight / 2 - padding
            ),
            size = Size(textWidth + padding * 2, textHeight + padding * 2),
            cornerRadius = CornerRadius(4f)
        )

        // Draw text
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                midPoint.x - textWidth / 2,
                midPoint.y - textHeight / 2
            )
        )
    }

    private fun DrawScope.drawNodes(data: FlowchartData, theme: MermaidTheme) {
        for (vertex in data.vertices.values) {
            // Draw node shape
            drawNodeShape(vertex.shape, vertex.bounds, theme)
        }
    }

    private fun DrawScope.drawNodesWithText(
        data: FlowchartData,
        theme: MermaidTheme,
        textMeasurer: TextMeasurer
    ) {
        for (vertex in data.vertices.values) {
            // Draw node shape
            drawNodeShape(vertex.shape, vertex.bounds, theme)

            // Draw label
            val textLayoutResult = textMeasurer.measure(
                text = vertex.label,
                style = TextStyle(
                    fontSize = theme.fontSize.sp,
                    color = theme.textColor
                )
            )

            val textWidth = textLayoutResult.size.width.toFloat()
            val textHeight = textLayoutResult.size.height.toFloat()

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    vertex.bounds.centerX - textWidth / 2,
                    vertex.bounds.centerY - textHeight / 2
                )
            )
        }
    }
}
