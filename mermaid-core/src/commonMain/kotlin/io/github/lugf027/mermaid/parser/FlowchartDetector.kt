/**
 * FlowchartDetector - Detects flowchart/graph diagrams.
 */
package io.github.lugf027.mermaid.parser

import io.github.lugf027.mermaid.model.DiagramType

/**
 * Detector for flowchart diagrams.
 * 
 * Detects diagrams starting with:
 * - flowchart
 * - graph
 */
public object FlowchartDetector : DiagramDetector {
    
    override val diagramType: DiagramType = DiagramType.FLOWCHART

    private val FLOWCHART_REGEX = Regex(
        """^\s*(?:%%[^\n]*\n)*\s*(flowchart|graph)\s*(TB|TD|BT|LR|RL)?\s*""",
        RegexOption.IGNORE_CASE
    )

    override fun detect(text: String): Boolean {
        return FLOWCHART_REGEX.containsMatchIn(text)
    }

    /**
     * Extract the direction from the flowchart declaration.
     */
    public fun extractDirection(text: String): String? {
        val match = FLOWCHART_REGEX.find(text)
        return match?.groupValues?.getOrNull(2)?.uppercase()
    }

    /**
     * Check if this is a 'graph' style declaration (older syntax).
     */
    public fun isGraphStyle(text: String): Boolean {
        val match = FLOWCHART_REGEX.find(text)
        return match?.groupValues?.getOrNull(1)?.lowercase() == "graph"
    }
}
