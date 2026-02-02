/**
 * DiagramDetector - Interface for detecting diagram types.
 */
package io.github.lugf027.mermaid.parser

import io.github.lugf027.mermaid.model.DiagramType

/**
 * Interface for diagram type detectors.
 */
public interface DiagramDetector {
    /**
     * The diagram type this detector handles.
     */
    val diagramType: DiagramType

    /**
     * Check if this detector can handle the given text.
     */
    fun detect(text: String): Boolean
}

/**
 * Registry for diagram detectors.
 */
public object DiagramDetectorRegistry {
    private val detectors: MutableList<DiagramDetector> = mutableListOf()

    init {
        // Register built-in detectors
        register(FlowchartDetector)
    }

    /**
     * Register a detector.
     */
    public fun register(detector: DiagramDetector) {
        detectors.add(detector)
    }

    /**
     * Detect the diagram type from text.
     */
    public fun detect(text: String): DiagramType {
        for (detector in detectors) {
            if (detector.detect(text)) {
                return detector.diagramType
            }
        }
        return DiagramType.UNKNOWN
    }
}
