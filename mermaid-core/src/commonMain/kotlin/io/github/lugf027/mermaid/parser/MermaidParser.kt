/**
 * MermaidParser - Main entry point for parsing Mermaid diagrams.
 */
package io.github.lugf027.mermaid.parser

import io.github.lugf027.mermaid.MermaidParseException
import io.github.lugf027.mermaid.UnsupportedDiagramException
import io.github.lugf027.mermaid.model.DiagramData
import io.github.lugf027.mermaid.model.DiagramType
import io.github.lugf027.mermaid.parser.flowchart.FlowchartParser

/**
 * Main parser that delegates to specific diagram parsers.
 */
public object MermaidParser {

    /**
     * Parse Mermaid text and return the diagram data.
     * 
     * @param text The Mermaid diagram text to parse
     * @return The parsed diagram data
     * @throws MermaidParseException if parsing fails
     * @throws UnsupportedDiagramException if the diagram type is not supported
     */
    public fun parse(text: String): DiagramData {
        val trimmedText = text.trim()
        
        if (trimmedText.isEmpty()) {
            throw MermaidParseException("Empty diagram text")
        }

        val diagramType = DiagramDetectorRegistry.detect(trimmedText)
        
        return when (diagramType) {
            DiagramType.FLOWCHART -> FlowchartParser.parse(trimmedText)
            DiagramType.UNKNOWN -> throw UnsupportedDiagramException("unknown")
            else -> throw UnsupportedDiagramException(diagramType.name)
        }
    }

    /**
     * Detect the diagram type without parsing.
     */
    public fun detectType(text: String): DiagramType {
        return DiagramDetectorRegistry.detect(text.trim())
    }

    /**
     * Check if a diagram type is supported.
     */
    public fun isSupported(type: DiagramType): Boolean {
        return type == DiagramType.FLOWCHART
    }
}
