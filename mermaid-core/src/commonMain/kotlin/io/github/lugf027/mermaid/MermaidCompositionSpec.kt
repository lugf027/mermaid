/**
 * MermaidCompositionSpec - Specification for loading Mermaid compositions.
 */
package io.github.lugf027.mermaid

import io.github.lugf027.mermaid.layout.LayoutConfig
import io.github.lugf027.mermaid.layout.TextMeasureProvider

/**
 * Specification for loading a Mermaid composition.
 */
public sealed interface MermaidCompositionSpec {
    /**
     * Unique key for caching purposes.
     */
    public val key: String?

    /**
     * Load the Mermaid composition.
     */
    public suspend fun load(): MermaidComposition
    
    /**
     * Load the Mermaid composition with precise text measurement.
     * 
     * @param textMeasureProvider Provider for precise text measurement
     * @param fontSize Font size for text measurement (in sp)
     */
    public suspend fun load(
        textMeasureProvider: TextMeasureProvider?,
        fontSize: Float
    ): MermaidComposition

    public companion object {
        /**
         * Create a spec from a Mermaid string.
         */
        public fun String(
            mermaidText: String,
            key: String? = null,
            layoutConfig: LayoutConfig = LayoutConfig()
        ): MermaidCompositionSpec = StringSpec(mermaidText, key, layoutConfig)
    }
}

/**
 * Specification for loading from a string.
 */
internal class StringSpec(
    private val mermaidText: String,
    override val key: String?,
    private val layoutConfig: LayoutConfig = LayoutConfig()
) : MermaidCompositionSpec {
    override suspend fun load(): MermaidComposition {
        return MermaidComposition.parse(mermaidText, layoutConfig)
    }
    
    override suspend fun load(
        textMeasureProvider: TextMeasureProvider?,
        fontSize: Float
    ): MermaidComposition {
        return MermaidComposition.parse(mermaidText, layoutConfig, textMeasureProvider, fontSize)
    }
}
