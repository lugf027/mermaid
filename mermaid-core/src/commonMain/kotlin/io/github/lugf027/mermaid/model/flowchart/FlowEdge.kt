/**
 * FlowEdge - Represents an edge/link in a flowchart.
 */
package io.github.lugf027.mermaid.model.flowchart

import io.github.lugf027.mermaid.model.Edge

/**
 * An edge (link) connecting two vertices in a flowchart.
 */
public data class FlowEdge(
    /**
     * Source vertex ID.
     */
    override val sourceId: String,

    /**
     * Target vertex ID.
     */
    override val targetId: String,

    /**
     * Text label on the edge.
     */
    override val label: String? = null,

    /**
     * Type of link (arrow style).
     */
    val linkType: LinkType = LinkType.DEFAULT,

    /**
     * Arrow head at the start (source end).
     */
    val startArrow: ArrowHead = ArrowHead.NONE,

    /**
     * Arrow head at the end (target end).
     */
    val endArrow: ArrowHead = if (linkType.hasArrow) ArrowHead.ARROW else ArrowHead.NONE,

    /**
     * Additional length for longer links.
     */
    val length: Int = 1,

    /**
     * CSS classes applied to this edge.
     */
    val cssClasses: List<String> = emptyList(),

    /**
     * Points along the edge path after layout.
     */
    var points: List<Point> = emptyList()
) : Edge

/**
 * A 2D point.
 */
public data class Point(
    val x: Float,
    val y: Float
)
