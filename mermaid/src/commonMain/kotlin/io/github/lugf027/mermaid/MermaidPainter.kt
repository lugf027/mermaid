/**
 * MermaidPainter - Painter implementation for Mermaid diagrams.
 */
package io.github.lugf027.mermaid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import io.github.lugf027.mermaid.model.DiagramType
import io.github.lugf027.mermaid.model.flowchart.FlowchartData
import io.github.lugf027.mermaid.render.flowchart.FlowchartRenderer
import io.github.lugf027.mermaid.theme.MermaidTheme

/**
 * A Painter that renders a Mermaid diagram.
 * 
 * This can be used with Image() or any other composable that accepts a Painter.
 */
public class MermaidPainter(
    private val composition: MermaidComposition,
    private val theme: MermaidTheme = MermaidTheme.Default
) : Painter() {

    override val intrinsicSize: Size
        get() = Size(composition.width, composition.height)

    override fun DrawScope.onDraw() {
        when (composition.diagramType) {
            DiagramType.FLOWCHART -> {
                val data = composition.diagramData as FlowchartData
                with(FlowchartRenderer) {
                    render(data, theme)
                }
            }
            else -> {
                // Unsupported diagram type - draw nothing
            }
        }
    }
}

/**
 * Remember a MermaidPainter for the given composition.
 * 
 * @param composition The Mermaid composition to render
 * @param theme The theme to use for rendering
 * @return A Painter that renders the diagram
 */
@Composable
public fun rememberMermaidPainter(
    composition: MermaidComposition?,
    theme: MermaidTheme = MermaidTheme.Default
): Painter? {
    return remember(composition, theme) {
        composition?.let { MermaidPainter(it, theme) }
    }
}
