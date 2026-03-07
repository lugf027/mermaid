package io.lugf027.github.mermaid.core.diagrams.block

import androidx.compose.ui.geometry.CornerRadius
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

class BlockRenderer : DiagramRenderer {
    override fun draw(drawScope: DrawScope, db: DiagramDB, config: MermaidConfig, theme: ThemeVariables, textMeasurer: TextMeasurer, size: Size) {
        with(drawScope) {
        val bDb = db as? BlockDb ?: return
        val blocks = bDb.getBlocks(); val edges = bDb.getEdges()
        val cols = if (bDb.columns > 0) bDb.columns else 3
        val margin = 40f; val gap = 20f; val boxW = (size.width - margin * 2 - gap * (cols - 1)) / cols; val boxH = 50f
        val textColor = theme.primaryTextColor.toComposeColor()
        val style = TextStyle(fontSize = 12.sp, color = textColor)
        val positions = mutableMapOf<String, Offset>()

        for ((idx, block) in blocks.withIndex()) {
            val col = idx % cols; val row = idx / cols
            val x = margin + col * (boxW + gap); val y = margin + row * (boxH + gap)
            drawRoundRect(theme.primaryColor.toComposeColor(), Offset(x, y), Size(boxW, boxH), CornerRadius(4f))
            drawRoundRect(theme.primaryBorderColor.toComposeColor(), Offset(x, y), Size(boxW, boxH), CornerRadius(4f), style = Stroke(1.5f))
            val r = textMeasurer.measure(block.label, style)
            drawText(r, topLeft = Offset(x + (boxW - r.size.width) / 2, y + (boxH - r.size.height) / 2))
            positions[block.id] = Offset(x + boxW / 2, y + boxH / 2)
        }

        for (edge in edges) {
            val from = positions[edge.start] ?: continue; val to = positions[edge.end] ?: continue
            drawLine(theme.lineColor.toComposeColor(), from, to, 1.5f)
        }
        }
    }
}
