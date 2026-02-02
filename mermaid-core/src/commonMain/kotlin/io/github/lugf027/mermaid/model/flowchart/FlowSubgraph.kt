/**
 * FlowSubgraph - Represents a subgraph/cluster in a flowchart.
 */
package io.github.lugf027.mermaid.model.flowchart

import io.github.lugf027.mermaid.model.Bounds
import io.github.lugf027.mermaid.model.Direction

/**
 * A subgraph (cluster) containing other vertices and subgraphs.
 */
public data class FlowSubgraph(
    /**
     * Unique identifier for this subgraph.
     */
    val id: String,

    /**
     * Display title for this subgraph.
     */
    val title: String?,

    /**
     * Direction override for this subgraph (if different from parent).
     */
    val direction: Direction? = null,

    /**
     * Vertex IDs contained in this subgraph.
     */
    val vertexIds: MutableList<String> = mutableListOf(),

    /**
     * Nested subgraph IDs.
     */
    val subgraphIds: MutableList<String> = mutableListOf(),

    /**
     * CSS classes applied to this subgraph.
     */
    val cssClasses: List<String> = emptyList(),

    /**
     * Position and size after layout.
     */
    var bounds: Bounds = Bounds.EMPTY
)
