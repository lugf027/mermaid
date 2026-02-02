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
import androidx.compose.ui.text.rememberTextMeasurer
import io.github.lugf027.mermaid.model.DiagramType
import io.github.lugf027.mermaid.model.flowchart.FlowchartData
import io.github.lugf027.mermaid.render.flowchart.FlowchartRenderer
import io.github.lugf027.mermaid.theme.MermaidTheme

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
 * Simplified MermaidDiagram that parses text directly.
 * 
 * @param text The Mermaid diagram text
 * @param modifier Modifier for the composable
 * @param theme Theme configuration for rendering
 * @param enableZoom Whether to enable zoom and pan gestures
 */
@Composable
public fun MermaidDiagram(
    text: String,
    modifier: Modifier = Modifier,
    theme: MermaidTheme = MermaidTheme.Default,
    enableZoom: Boolean = true,
) {
    val compositionResult = rememberMermaidComposition(text) {
        MermaidCompositionSpec.String(text)
    }

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
