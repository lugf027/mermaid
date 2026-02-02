/**
 * rememberMermaidComposition - Composable function to load and remember a Mermaid composition.
 */
package io.github.lugf027.mermaid

import androidx.compose.runtime.*
import io.github.lugf027.mermaid.layout.LayoutConfig
import io.github.lugf027.mermaid.layout.TextMeasureProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Remember and load a Mermaid composition.
 * 
 * @param keys Keys that trigger recomposition when changed
 * @param spec Lambda that returns the composition spec
 * @return The composition result (Loading, Success, or Error)
 */
@Composable
public fun rememberMermaidComposition(
    vararg keys: Any?,
    spec: suspend () -> MermaidCompositionSpec,
): MermaidCompositionResult {
    var result by remember { mutableStateOf<MermaidCompositionResult>(MermaidCompositionResult.Loading) }

    LaunchedEffect(keys = keys) {
        result = MermaidCompositionResult.Loading
        result = try {
            val compositionSpec = spec()
            val composition = withContext(Dispatchers.Default) {
                compositionSpec.load()
            }
            MermaidCompositionResult.Success(composition)
        } catch (e: Exception) {
            MermaidCompositionResult.Error(e)
        }
    }

    return result
}

/**
 * Remember and load a Mermaid composition with precise text measurement.
 * 
 * @param keys Keys that trigger recomposition when changed
 * @param textMeasureProvider Provider for precise text measurement
 * @param fontSize Font size for text measurement
 * @param spec Lambda that returns the composition spec
 * @return The composition result (Loading, Success, or Error)
 */
@Composable
public fun rememberMermaidComposition(
    vararg keys: Any?,
    textMeasureProvider: TextMeasureProvider?,
    fontSize: Float,
    spec: suspend () -> MermaidCompositionSpec,
): MermaidCompositionResult {
    var result by remember { mutableStateOf<MermaidCompositionResult>(MermaidCompositionResult.Loading) }

    LaunchedEffect(keys = keys) {
        result = MermaidCompositionResult.Loading
        result = try {
            val compositionSpec = spec()
            // Use the version with text measurement provider
            val composition = if (textMeasureProvider != null) {
                compositionSpec.load(textMeasureProvider, fontSize)
            } else {
                withContext(Dispatchers.Default) {
                    compositionSpec.load()
                }
            }
            MermaidCompositionResult.Success(composition)
        } catch (e: Exception) {
            MermaidCompositionResult.Error(e)
        }
    }

    return result
}

/**
 * Remember and load a Mermaid composition from text.
 * 
 * @param text The Mermaid diagram text
 * @return The composition result
 */
@Composable
public fun rememberMermaidComposition(
    text: String
): MermaidCompositionResult {
    return rememberMermaidComposition(text) {
        MermaidCompositionSpec.String(text)
    }
}

/**
 * Remember and load a Mermaid composition from text with configuration.
 * 
 * @param text The Mermaid diagram text
 * @param layoutConfig Layout configuration
 * @return The composition result
 */
@Composable
public fun rememberMermaidComposition(
    text: String,
    layoutConfig: LayoutConfig
): MermaidCompositionResult {
    return rememberMermaidComposition(text, layoutConfig) {
        MermaidCompositionSpec.String(text, layoutConfig = layoutConfig)
    }
}

/**
 * State holder for Mermaid diagram interaction.
 */
@Stable
public class MermaidState internal constructor() {
    /**
     * Current zoom scale.
     */
    public var scale: Float by mutableStateOf(1f)

    /**
     * Current pan offset X.
     */
    public var offsetX: Float by mutableStateOf(0f)

    /**
     * Current pan offset Y.
     */
    public var offsetY: Float by mutableStateOf(0f)

    /**
     * Currently selected node ID.
     */
    public var selectedNodeId: String? by mutableStateOf(null)

    /**
     * Reset zoom and pan to default.
     */
    public fun reset() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    /**
     * Zoom in by a factor.
     */
    public fun zoomIn(factor: Float = 1.2f) {
        scale = (scale * factor).coerceAtMost(3f)
    }

    /**
     * Zoom out by a factor.
     */
    public fun zoomOut(factor: Float = 1.2f) {
        scale = (scale / factor).coerceAtLeast(0.5f)
    }
}

/**
 * Remember a MermaidState instance.
 */
@Composable
public fun rememberMermaidState(): MermaidState {
    return remember { MermaidState() }
}
