/**
 * DiagramData - Base interface for all diagram data types.
 */
package io.github.lugf027.mermaid.model

/**
 * Base interface for diagram data.
 * 
 * Each diagram type has its own implementation with specific data structures.
 */
public interface DiagramData {
    /**
     * The type of this diagram.
     */
    val type: DiagramType

    /**
     * The layout direction.
     */
    var direction: Direction

    /**
     * The bounding box of the diagram after layout.
     */
    var bounds: Bounds

    /**
     * Title of the diagram (if specified).
     */
    val title: String?
}

/**
 * Common node interface for all diagram types.
 */
public interface Node {
    /**
     * Unique identifier for this node.
     */
    val id: String

    /**
     * Display label for this node.
     */
    val label: String

    /**
     * Position and size after layout.
     */
    val bounds: Bounds
}

/**
 * Common edge interface for all diagram types.
 */
public interface Edge {
    /**
     * Source node ID.
     */
    val sourceId: String

    /**
     * Target node ID.
     */
    val targetId: String

    /**
     * Edge label (if any).
     */
    val label: String?
}
