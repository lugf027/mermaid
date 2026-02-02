/**
 * MermaidComposition - Holds the parsed diagram data.
 */
package io.github.lugf027.mermaid

import androidx.compose.runtime.Stable
import io.github.lugf027.mermaid.layout.LayoutConfig
import io.github.lugf027.mermaid.layout.TextMeasureProvider
import io.github.lugf027.mermaid.layout.flowchart.FlowchartLayout
import io.github.lugf027.mermaid.model.DiagramData
import io.github.lugf027.mermaid.model.DiagramType
import io.github.lugf027.mermaid.model.flowchart.FlowchartData
import io.github.lugf027.mermaid.parser.MermaidParser

/**
 * Represents a parsed Mermaid diagram composition.
 * 
 * This class holds the parsed diagram data and provides methods
 * for accessing diagram properties.
 */
@Stable
public class MermaidComposition internal constructor(
    /**
     * The parsed and laid out diagram data.
     */
    public val diagramData: DiagramData,
    
    /**
     * The type of diagram.
     */
    public val diagramType: DiagramType,
) {
    /**
     * The calculated width of the diagram.
     */
    public val width: Float
        get() = diagramData.bounds.width

    /**
     * The calculated height of the diagram.
     */
    public val height: Float
        get() = diagramData.bounds.height

    public companion object {
        /**
         * Parse Mermaid text and create a composition.
         * 
         * @param text The Mermaid diagram text
         * @param layoutConfig Optional layout configuration
         * @return The parsed and laid out composition
         */
        public fun parse(
            text: String,
            layoutConfig: LayoutConfig = LayoutConfig()
        ): MermaidComposition {
            return parse(text, layoutConfig, null, 14f)
        }
        
        /**
         * Parse Mermaid text and create a composition with precise text measurement.
         * 
         * @param text The Mermaid diagram text
         * @param layoutConfig Layout configuration
         * @param textMeasureProvider Provider for precise text measurement (e.g., Compose TextMeasurer wrapper)
         * @param fontSize Font size for text measurement (in sp)
         * @return The parsed and laid out composition
         */
        public fun parse(
            text: String,
            layoutConfig: LayoutConfig,
            textMeasureProvider: TextMeasureProvider?,
            fontSize: Float
        ): MermaidComposition {
            // Parse the text
            val diagramData = MermaidParser.parse(text)
            
            // Apply layout with text measurement provider
            val layoutData = when (diagramData) {
                is FlowchartData -> FlowchartLayout.layout(
                    diagramData, 
                    layoutConfig,
                    textMeasureProvider,
                    fontSize
                )
                else -> diagramData
            }
            
            return MermaidComposition(
                diagramData = layoutData,
                diagramType = layoutData.type
            )
        }
    }
}

/**
 * Result of loading a MermaidComposition.
 */
public sealed class MermaidCompositionResult {
    /**
     * Loading is in progress.
     */
    public data object Loading : MermaidCompositionResult()

    /**
     * Composition loaded successfully.
     */
    public data class Success(val composition: MermaidComposition) : MermaidCompositionResult()

    /**
     * Loading failed with an error.
     */
    public data class Error(val exception: Throwable) : MermaidCompositionResult()

    /**
     * Get the composition if loading succeeded, null otherwise.
     */
    public val compositionOrNull: MermaidComposition?
        get() = (this as? Success)?.composition
}
