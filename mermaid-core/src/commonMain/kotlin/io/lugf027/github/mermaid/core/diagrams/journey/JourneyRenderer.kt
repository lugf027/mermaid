package io.lugf027.github.mermaid.core.diagrams.journey

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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

class JourneyRenderer : DiagramRenderer {
    override fun draw(drawScope: DrawScope, db: DiagramDB, config: MermaidConfig, theme: ThemeVariables, textMeasurer: TextMeasurer, size: Size) {
        with(drawScope) {
        val jDb = db as? JourneyDb ?: return
        val tasks = jDb.getTasks()
        if (tasks.isEmpty()) return

        val textColor = theme.primaryTextColor.toComposeColor()
        val style = TextStyle(fontSize = 12.sp, color = textColor)
        val cardW = 150f; val cardH = 60f; val gap = 16f; val margin = 40f
        var x = margin; var y = margin + 30f

        val title = jDb.getDiagramTitle()
        if (title.isNotEmpty()) {
            val tr = textMeasurer.measure(title, TextStyle(fontSize = 16.sp, color = textColor))
            drawText(tr, topLeft = Offset((size.width - tr.size.width) / 2, 8f))
        }

        var lastSection = ""
        for (task in tasks) {
            if (task.section != lastSection) {
                lastSection = task.section
                val sr = textMeasurer.measure(task.section, TextStyle(fontSize = 14.sp, color = textColor))
                drawText(sr, topLeft = Offset(margin, y)); y += sr.size.height + 8f
                drawLine(theme.lineColor.toComposeColor(), Offset(margin, y), Offset(size.width - margin, y), 1f); y += 8f
            }

            val scoreColor = when {
                task.score >= 4 -> Color(0xFF4CAF50)
                task.score >= 3 -> Color(0xFFFFC107)
                else -> Color(0xFFF44336)
            }
            drawRoundRect(scoreColor.copy(alpha = 0.2f), Offset(x, y), Size(cardW, cardH), CornerRadius(6f))
            val tr = textMeasurer.measure(task.task, style)
            drawText(tr, topLeft = Offset(x + 8f, y + 8f))
            val sr = textMeasurer.measure("Score: ${task.score}", TextStyle(fontSize = 11.sp, color = scoreColor))
            drawText(sr, topLeft = Offset(x + 8f, y + 28f))
            if (task.people.isNotEmpty()) {
                val pr = textMeasurer.measure(task.people.joinToString(", "), TextStyle(fontSize = 10.sp, color = textColor.copy(alpha = 0.7f)))
                drawText(pr, topLeft = Offset(x + 8f, y + 44f))
            }
            x += cardW + gap
            if (x + cardW > size.width - margin) { x = margin; y += cardH + gap }
        }
        }
    }
}
