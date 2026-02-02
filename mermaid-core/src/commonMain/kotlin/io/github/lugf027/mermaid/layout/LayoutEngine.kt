/**
 * LayoutEngine - Interface for diagram layout algorithms.
 */
package io.github.lugf027.mermaid.layout

import io.github.lugf027.mermaid.model.Bounds
import io.github.lugf027.mermaid.model.DiagramData

/**
 * Configuration for layout algorithms.
 */
public data class LayoutConfig(
    /**
     * Minimum width for nodes.
     */
    val nodeMinWidth: Float = 100f,

    /**
     * Minimum height for nodes.
     */
    val nodeMinHeight: Float = 40f,

    /**
     * Horizontal spacing between nodes.
     */
    val nodeSpacingX: Float = 50f,

    /**
     * Vertical spacing between nodes.
     */
    val nodeSpacingY: Float = 60f,

    /**
     * Spacing between different ranks/layers in the layout.
     * This controls the distance between nodes in different hierarchical levels.
     */
    val rankSpacing: Float = 80f,

    /**
     * Padding inside subgraphs.
     */
    val subgraphPadding: Float = 20f,

    /**
     * Padding around the entire diagram.
     */
    val diagramPadding: Float = 30f,

    /**
     * Estimated character width for text measurement.
     * Increased from 8f to 10f for better text fitting.
     */
    val charWidth: Float = 10f,

    /**
     * Estimated line height for text measurement.
     */
    val lineHeight: Float = 20f,

    /**
     * Padding inside nodes around text.
     * Increased from 12f to 16f for better text spacing.
     */
    val textPadding: Float = 16f,

    /**
     * Top margin for subgraph title.
     * Space between subgraph border and title text.
     */
    val subgraphTitleTopMargin: Float = 8f,

    /**
     * Bottom margin for subgraph title.
     * Space between title text and subgraph content.
     */
    val subgraphTitleBottomMargin: Float = 16f,

    /**
     * Estimated height for subgraph title text.
     */
    val subgraphTitleHeight: Float = 24f,

    /**
     * Corner radius for rounded path edges.
     * Used for generating smoother curved arrows.
     */
    val edgeCornerRadius: Float = 8f
)

/**
 * Interface for layout engines.
 */
public interface LayoutEngine<T : DiagramData> {
    /**
     * Calculate layout for the given diagram data.
     * 
     * @param data The diagram data to layout
     * @param config Layout configuration
     * @return The updated diagram data with positions calculated
     */
    fun layout(data: T, config: LayoutConfig = LayoutConfig()): T
}

/**
 * Result of text measurement.
 */
public data class TextSize(
    val width: Float,
    val height: Float
)

/**
 * Simple text measurement utility.
 */
public object TextMeasurer {
    /**
     * Estimate the size of text.
     */
    public fun measure(text: String, config: LayoutConfig): TextSize {
        val lines = text.split('\n')
        val maxLineLength = lines.maxOfOrNull { it.length } ?: 0
        val width = maxLineLength * config.charWidth + config.textPadding * 2
        val height = lines.size * config.lineHeight + config.textPadding * 2
        return TextSize(width.coerceAtLeast(config.nodeMinWidth), height.coerceAtLeast(config.nodeMinHeight))
    }
}
