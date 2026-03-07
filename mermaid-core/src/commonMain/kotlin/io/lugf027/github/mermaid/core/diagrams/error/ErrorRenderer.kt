package io.lugf027.github.mermaid.core.diagrams.error

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.types.DiagramDB
import io.lugf027.github.mermaid.core.types.DiagramRenderer

class ErrorRenderer : DiagramRenderer {
    override fun draw(drawScope: DrawScope, db: DiagramDB, config: MermaidConfig, theme: ThemeVariables, textMeasurer: TextMeasurer, size: Size) {
        with(drawScope) {
        val eDb = db as? ErrorDb ?: return
        val style = TextStyle(fontSize = 16.sp, color = Color.Red)
        val r = textMeasurer.measure(eDb.errorMessage, style)
        drawText(r, topLeft = Offset((size.width - r.size.width) / 2, (size.height - r.size.height) / 2))
        }
    }
}
