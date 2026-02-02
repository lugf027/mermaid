/**
 * DiagramType - Enumeration of supported Mermaid diagram types.
 */
package io.github.lugf027.mermaid.model

/**
 * Represents the type of Mermaid diagram.
 */
public enum class DiagramType(
    /**
     * Keywords that identify this diagram type.
     */
    public val keywords: List<String>
) {
    /**
     * Flowchart diagram (graph/flowchart).
     */
    FLOWCHART(listOf("flowchart", "graph")),

    /**
     * Sequence diagram.
     */
    SEQUENCE(listOf("sequenceDiagram")),

    /**
     * Class diagram.
     */
    CLASS(listOf("classDiagram", "classDiagram-v2")),

    /**
     * State diagram.
     */
    STATE(listOf("stateDiagram", "stateDiagram-v2")),

    /**
     * Entity Relationship diagram.
     */
    ER(listOf("erDiagram")),

    /**
     * Gantt chart.
     */
    GANTT(listOf("gantt")),

    /**
     * Pie chart.
     */
    PIE(listOf("pie")),

    /**
     * Git graph.
     */
    GIT_GRAPH(listOf("gitGraph")),

    /**
     * Mindmap diagram.
     */
    MINDMAP(listOf("mindmap")),

    /**
     * Timeline diagram.
     */
    TIMELINE(listOf("timeline")),

    /**
     * C4 diagram.
     */
    C4(listOf("C4Context", "C4Container", "C4Component", "C4Dynamic", "C4Deployment")),

    /**
     * User journey diagram.
     */
    JOURNEY(listOf("journey")),

    /**
     * Quadrant chart.
     */
    QUADRANT(listOf("quadrantChart")),

    /**
     * Unknown/unsupported diagram type.
     */
    UNKNOWN(emptyList());

    public companion object {
        /**
         * Detect diagram type from the first line of Mermaid text.
         */
        public fun detect(text: String): DiagramType {
            val firstLine = text.trimStart().lines().firstOrNull()?.trim() ?: return UNKNOWN
            val firstWord = firstLine.split(Regex("\\s+")).firstOrNull() ?: return UNKNOWN
            
            return entries.find { type ->
                type.keywords.any { keyword ->
                    firstWord.equals(keyword, ignoreCase = true)
                }
            } ?: UNKNOWN
        }
    }
}
