/**
 * MermaidDiagram Composable - Main entry point for rendering Mermaid diagrams.
 */
package io.github.lugf027.mermaid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import io.github.lugf027.mermaid.layout.LayoutConfig
import io.github.lugf027.mermaid.layout.TextMeasureProvider
import io.github.lugf027.mermaid.layout.TextSize
import io.github.lugf027.mermaid.model.DiagramType
import io.github.lugf027.mermaid.model.flowchart.FlowchartData
import io.github.lugf027.mermaid.render.flowchart.FlowchartRenderer
import io.github.lugf027.mermaid.theme.MermaidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.TextMeasurer as ComposeTextMeasurer

/**
 * Compose-based TextMeasureProvider implementation.
 * 
 * Uses Compose's TextMeasurer to accurately measure text dimensions
 * for precise node sizing in diagrams.
 */
public class ComposeTextMeasureProvider(
    private val textMeasurer: ComposeTextMeasurer
) : TextMeasureProvider {
    
    override fun measureText(text: String, fontSize: Float): TextSize {
        val textLayoutResult = textMeasurer.measure(
            text = text,
            style = TextStyle(fontSize = fontSize.sp)
        )
        return TextSize(
            width = textLayoutResult.size.width.toFloat(),
            height = textLayoutResult.size.height.toFloat()
        )
    }
}

/**
 * A Composable that renders a Mermaid diagram.
 * 
 * @param composition The parsed Mermaid composition to render
 * @param modifier Modifier for the composable
 * @param theme Theme configuration for rendering
 * @param enableZoom Whether to enable zoom and pan gestures
 * @param contentDescription Accessibility description
 */
@Composable
public fun MermaidDiagram(
    composition: MermaidComposition?,
    modifier: Modifier = Modifier,
    theme: MermaidTheme = MermaidTheme.Default,
    enableZoom: Boolean = true,
    contentDescription: String? = null,
) {
    if (composition == null) {
        // Show placeholder when no composition
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            // Empty state
        }
        return
    }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val textMeasurer = rememberTextMeasurer()

    val gestureModifier = if (enableZoom) {
        Modifier.pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                scale = (scale * zoom).coerceIn(0.5f, 3f)
                offset = offset + pan
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(gestureModifier)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            when (composition.diagramType) {
                DiagramType.FLOWCHART -> {
                    val data = composition.diagramData as FlowchartData
                    with(FlowchartRenderer) {
                        renderWithTextMeasurer(data, theme, textMeasurer)
                    }
                }
                else -> {
                    // Unsupported diagram type
                }
            }
        }
    }
}

/**
 * MermaidDiagram that parses text with precise text measurement.
 * 
 * This version uses Compose's TextMeasurer during the layout phase
 * to ensure accurate node sizing that properly fits text content.
 * 
 * @param text The Mermaid diagram text
 * @param modifier Modifier for the composable
 * @param theme Theme configuration for rendering
 * @param layoutConfig Layout configuration
 * @param enableZoom Whether to enable zoom and pan gestures
 */
@Composable
public fun MermaidDiagram(
    text: String,
    modifier: Modifier = Modifier,
    theme: MermaidTheme = MermaidTheme.Default,
    layoutConfig: LayoutConfig = LayoutConfig(),
    enableZoom: Boolean = true,
) {
    val textMeasurer = rememberTextMeasurer()
    val fontSize = theme.fontSize
    
    // Create TextMeasureProvider from Compose TextMeasurer
    val textMeasureProvider = remember(textMeasurer) {
        ComposeTextMeasureProvider(textMeasurer)
    }
    
    // Parse and layout with precise text measurement
    val compositionResult = rememberMermaidCompositionWithMeasurer(
        text = text,
        layoutConfig = layoutConfig,
        textMeasureProvider = textMeasureProvider,
        fontSize = fontSize
    )

    when (val result = compositionResult) {
        is MermaidCompositionResult.Loading -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                // Loading state
            }
        }
        is MermaidCompositionResult.Success -> {
            MermaidDiagram(
                composition = result.composition,
                modifier = modifier,
                theme = theme,
                enableZoom = enableZoom
            )
        }
        is MermaidCompositionResult.Error -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                // Error state - could show error message
            }
        }
    }
}

/**
 * Remember and load a Mermaid composition with precise text measurement.
 * 
 * @param text The Mermaid diagram text
 * @param layoutConfig Layout configuration
 * @param textMeasureProvider Provider for precise text measurement
 * @param fontSize Font size for text measurement
 * @return The composition result
 */
@Composable
internal fun rememberMermaidCompositionWithMeasurer(
    text: String,
    layoutConfig: LayoutConfig,
    textMeasureProvider: TextMeasureProvider,
    fontSize: Float
): MermaidCompositionResult {
    var result by remember { mutableStateOf<MermaidCompositionResult>(MermaidCompositionResult.Loading) }
    
    // Use text, layoutConfig, fontSize as keys to trigger recomposition
    LaunchedEffect(text, layoutConfig, fontSize) {
        result = MermaidCompositionResult.Loading
        result = try {
            // Parse on default dispatcher but measurement happens on main thread
            // since textMeasurer needs main thread access
            val composition = MermaidComposition.parse(
                text = text,
                layoutConfig = layoutConfig,
                textMeasureProvider = textMeasureProvider,
                fontSize = fontSize
            )
            MermaidCompositionResult.Success(composition)
        } catch (e: Exception) {
            MermaidCompositionResult.Error(e)
        }
    }
    
    return result
}
