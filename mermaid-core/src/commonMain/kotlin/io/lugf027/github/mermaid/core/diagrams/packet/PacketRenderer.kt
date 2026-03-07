package io.lugf027.github.mermaid.core.diagrams.packet

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

class PacketRenderer : DiagramRenderer {
    override fun draw(drawScope: DrawScope, db: DiagramDB, config: MermaidConfig, theme: ThemeVariables, textMeasurer: TextMeasurer, size: Size) {
        with(drawScope) {
        val pDb = db as? PacketDb ?: return
        val blocks = pDb.getBlocks()
        if (blocks.isEmpty()) return

        val margin = 30f; val rowH = 40f; val bitsPerRow = 32
        val bitW = (size.width - margin * 2) / bitsPerRow
        val textColor = theme.primaryTextColor.toComposeColor()
        val borderColor = theme.primaryBorderColor.toComposeColor()
        val bgColor = theme.primaryColor.toComposeColor()
        val style = TextStyle(fontSize = 11.sp, color = textColor)

        for (block in blocks) {
            val row = block.start / bitsPerRow
            val colStart = block.start % bitsPerRow
            val colEnd = block.end % bitsPerRow
            val x = margin + colStart * bitW
            val y = margin + row * rowH
            val w = (colEnd - colStart + 1) * bitW

            drawRect(bgColor.copy(alpha = 0.1f), Offset(x, y), Size(w, rowH))
            drawRect(borderColor, Offset(x, y), Size(w, rowH), style = Stroke(1f))

            val r = textMeasurer.measure(block.label, style)
            drawText(r, topLeft = Offset(x + (w - r.size.width) / 2, y + (rowH - r.size.height) / 2))
        }

        // Bit numbers at top
        val numStyle = TextStyle(fontSize = 9.sp, color = textColor.copy(alpha = 0.6f))
        for (bit in 0 until bitsPerRow step 4) {
            val r = textMeasurer.measure(bit.toString(), numStyle)
            drawText(r, topLeft = Offset(margin + bit * bitW + bitW / 2 - r.size.width / 2, margin - 14f))
        }
        }
    }
}
