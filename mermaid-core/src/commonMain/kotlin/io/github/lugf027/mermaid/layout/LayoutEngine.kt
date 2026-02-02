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
     * Padding inside subgraphs.
     */
    val subgraphPadding: Float = 20f,

    /**
     * Padding around the entire diagram.
     */
    val diagramPadding: Float = 30f,

    /**
     * Estimated character width for text measurement.
     */
    val charWidth: Float = 8f,

    /**
     * Estimated line height for text measurement.
     */
    val lineHeight: Float = 20f,

    /**
     * Padding inside nodes around text.
     */
    val textPadding: Float = 12f
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
