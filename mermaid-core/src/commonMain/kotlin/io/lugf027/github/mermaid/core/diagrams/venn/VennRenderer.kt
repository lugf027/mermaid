package io.lugf027.github.mermaid.core.diagrams.venn

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.renderer.compose.toComposeColor
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.types.DiagramDB
import io.lugf027.github.mermaid.core.types.DiagramRenderer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class VennRenderer : DiagramRenderer {
    override fun draw(drawScope: DrawScope, db: DiagramDB, config: MermaidConfig, theme: ThemeVariables, textMeasurer: TextMeasurer, size: Size) {
        with(drawScope) {
        val vDb = db as? VennDb ?: return
        val sets = vDb.getSets()
        if (sets.isEmpty()) return

        val cx = size.width / 2; val cy = size.height / 2
        val radius = minOf(size.width, size.height) / 4
        val offset = radius * 0.5f
        val colors = listOf(theme.primaryColor.toComposeColor(), theme.secondaryColor.toComposeColor(), theme.tertiaryColor.toComposeColor())
        val style = TextStyle(fontSize = 13.sp, color = theme.primaryTextColor.toComposeColor())

        for ((idx, set) in sets.withIndex()) {
            val angle = 2 * PI.toFloat() / sets.size * idx - PI.toFloat() / 2
            val ox = cx + offset * cos(angle); val oy = cy + offset * sin(angle)
            val color = colors[idx % colors.size]
            drawCircle(color.copy(alpha = 0.3f), radius, Offset(ox, oy))
            drawCircle(color, radius, Offset(ox, oy), style = Stroke(2f))
            val r = textMeasurer.measure(set.label, style)
            drawText(r, topLeft = Offset(ox - r.size.width / 2, oy - r.size.height / 2))
        }

        val title = vDb.getDiagramTitle()
        if (title.isNotEmpty()) {
            val tr = textMeasurer.measure(title, TextStyle(fontSize = 16.sp, color = theme.primaryTextColor.toComposeColor()))
            drawText(tr, topLeft = Offset((size.width - tr.size.width) / 2, 8f))
        }
        }
    }
}
