/**
 * RenderEngine - Base interface for diagram rendering.
 */
package io.github.lugf027.mermaid.render

import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.lugf027.mermaid.model.DiagramData
import io.github.lugf027.mermaid.theme.MermaidTheme

/**
 * Interface for diagram render engines.
 */
public interface RenderEngine<T : DiagramData> {
    /**
     * Render the diagram data.
     * 
     * @param data The diagram data to render
     * @param theme The theme to use for rendering
     */
    fun DrawScope.render(data: T, theme: MermaidTheme)
}
