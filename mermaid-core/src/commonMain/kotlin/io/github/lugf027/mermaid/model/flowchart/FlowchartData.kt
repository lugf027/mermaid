/**
 * FlowchartData - Complete data model for a flowchart diagram.
 */
package io.github.lugf027.mermaid.model.flowchart

import io.github.lugf027.mermaid.model.Bounds
import io.github.lugf027.mermaid.model.DiagramData
import io.github.lugf027.mermaid.model.DiagramType
import io.github.lugf027.mermaid.model.Direction

/**
 * Complete data for a flowchart diagram.
 */
public data class FlowchartData(
    /**
     * Layout direction.
     */
    override var direction: Direction = Direction.DEFAULT,

    /**
     * Diagram title (from %%title directive).
     */
    override val title: String? = null,

    /**
     * All vertices in the flowchart.
     */
    val vertices: MutableMap<String, FlowVertex> = mutableMapOf(),

    /**
     * All edges in the flowchart.
     */
    val edges: MutableList<FlowEdge> = mutableListOf(),

    /**
     * All subgraphs in the flowchart.
     */
    val subgraphs: MutableMap<String, FlowSubgraph> = mutableMapOf(),

    /**
     * Root-level vertex IDs (not in any subgraph).
     */
    val rootVertexIds: MutableList<String> = mutableListOf(),

    /**
     * Root-level subgraph IDs.
     */
    val rootSubgraphIds: MutableList<String> = mutableListOf(),

    /**
     * Class definitions (name -> style).
     */
    val classDefs: MutableMap<String, String> = mutableMapOf(),

    /**
     * Click handlers (vertexId -> handler definition).
     */
    val clickHandlers: MutableMap<String, ClickHandler> = mutableMapOf(),

    /**
     * Overall bounds after layout.
     */
    override var bounds: Bounds = Bounds.EMPTY
) : DiagramData {
    
    override val type: DiagramType = DiagramType.FLOWCHART

    /**
     * Get a vertex by ID.
     */
    public fun getVertex(id: String): FlowVertex? = vertices[id]

    /**
     * Get a subgraph by ID.
     */
    public fun getSubgraph(id: String): FlowSubgraph? = subgraphs[id]

    /**
     * Get all edges from a specific vertex.
     */
    public fun getEdgesFrom(vertexId: String): List<FlowEdge> =
        edges.filter { it.sourceId == vertexId }

    /**
     * Get all edges to a specific vertex.
     */
    public fun getEdgesTo(vertexId: String): List<FlowEdge> =
        edges.filter { it.targetId == vertexId }

    /**
     * Get all vertex IDs.
     */
    public fun getAllVertexIds(): Set<String> = vertices.keys

    /**
     * Add a vertex to the flowchart.
     */
    public fun addVertex(vertex: FlowVertex) {
        vertices[vertex.id] = vertex
    }

    /**
     * Add an edge to the flowchart.
     */
    public fun addEdge(edge: FlowEdge) {
        edges.add(edge)
    }

    /**
     * Add a subgraph to the flowchart.
     */
    public fun addSubgraph(subgraph: FlowSubgraph) {
        subgraphs[subgraph.id] = subgraph
    }
}

/**
 * Click handler definition.
 */
public data class ClickHandler(
    /**
     * Target vertex ID.
     */
    val vertexId: String,

    /**
     * Callback function name or URL.
     */
    val callback: String,

    /**
     * Tooltip text.
     */
    val tooltip: String? = null
)
