package io.lugf027.github.mermaid.core.diagrams.info

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.renderer.compose.toComposeColor
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.types.DiagramDB
import io.lugf027.github.mermaid.core.types.DiagramRenderer

class InfoRenderer : DiagramRenderer {
    override fun draw(drawScope: DrawScope, db: DiagramDB, config: MermaidConfig, theme: ThemeVariables, textMeasurer: TextMeasurer, size: Size) {
        with(drawScope) {
        val iDb = db as? InfoDb ?: return
        val style = TextStyle(fontSize = 24.sp, color = theme.primaryTextColor.toComposeColor())
        val r = textMeasurer.measure(iDb.version, style)
        drawText(r, topLeft = Offset((size.width - r.size.width) / 2, (size.height - r.size.height) / 2))
        }
    }
}
