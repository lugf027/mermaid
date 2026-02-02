/**
 * FlowVertex - Represents a node in a flowchart.
 */
package io.github.lugf027.mermaid.model.flowchart

import io.github.lugf027.mermaid.model.Bounds
import io.github.lugf027.mermaid.model.Node

/**
 * A vertex (node) in a flowchart diagram.
 */
public data class FlowVertex(
    /**
     * Unique identifier for this vertex.
     */
    override val id: String,

    /**
     * Display text for this vertex.
     */
    override val label: String,

    /**
     * Shape of this vertex.
     */
    val shape: NodeShape = NodeShape.DEFAULT,

    /**
     * CSS classes applied to this vertex.
     */
    val cssClasses: List<String> = emptyList(),

    /**
     * Link URL if this vertex is clickable.
     */
    val link: String? = null,

    /**
     * Tooltip text.
     */
    val tooltip: String? = null,

    /**
     * Position and size after layout (mutable during layout phase).
     */
    override var bounds: Bounds = Bounds.EMPTY
) : Node {

    /**
     * X position (center) after layout.
     */
    val x: Float get() = bounds.centerX

    /**
     * Y position (center) after layout.
     */
    val y: Float get() = bounds.centerY

    /**
     * Width after layout.
     */
    val width: Float get() = bounds.width

    /**
     * Height after layout.
     */
    val height: Float get() = bounds.height
}
