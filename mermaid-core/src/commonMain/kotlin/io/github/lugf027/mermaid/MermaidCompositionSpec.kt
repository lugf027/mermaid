/**
 * MermaidCompositionSpec - Specification for loading Mermaid compositions.
 */
package io.github.lugf027.mermaid

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

    public companion object {
        /**
         * Create a spec from a Mermaid string.
         */
        public fun String(
            mermaidText: String,
            key: String? = null
        ): MermaidCompositionSpec = StringSpec(mermaidText, key)
    }
}

/**
 * Specification for loading from a string.
 */
internal class StringSpec(
    private val mermaidText: String,
    override val key: String?
) : MermaidCompositionSpec {
    override suspend fun load(): MermaidComposition {
        return MermaidComposition.parse(mermaidText)
    }
}
