package io.lugf027.github.mermaid.core.diagrams.kanban

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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

class KanbanRenderer : DiagramRenderer {
    override fun draw(drawScope: DrawScope, db: DiagramDB, config: MermaidConfig, theme: ThemeVariables, textMeasurer: TextMeasurer, size: Size) {
        with(drawScope) {
        val kDb = db as? KanbanDb ?: return
        val columns = kDb.getColumns()
        if (columns.isEmpty()) return

        val textColor = theme.primaryTextColor.toComposeColor()
        val borderColor = theme.primaryBorderColor.toComposeColor()
        val headerColor = theme.primaryColor.toComposeColor()
        val bgColor = theme.background.toComposeColor()
        val colWidth = (size.width - 40f) / columns.size
        val margin = 20f
        val headerH = 36f
        val itemH = 32f
        val gap = 8f
        val headerStyle = TextStyle(fontSize = 14.sp, color = Color.White)
        val itemStyle = TextStyle(fontSize = 12.sp, color = textColor)

        for ((idx, col) in columns.withIndex()) {
            val x = margin + idx * colWidth
            val colHeight = headerH + col.items.size * (itemH + gap) + gap
            // Column bg
            drawRoundRect(bgColor, Offset(x, margin), Size(colWidth - gap, colHeight), CornerRadius(6f))
            drawRoundRect(borderColor, Offset(x, margin), Size(colWidth - gap, colHeight), CornerRadius(6f), style = Stroke(1f))
            // Header
            drawRoundRect(headerColor, Offset(x, margin), Size(colWidth - gap, headerH), CornerRadius(6f, 6f))
            val hResult = textMeasurer.measure(col.label, headerStyle)
            drawText(hResult, topLeft = Offset(x + (colWidth - gap - hResult.size.width) / 2, margin + (headerH - hResult.size.height) / 2))
            // Items
            var iy = margin + headerH + gap
            for (item in col.items) {
                drawRoundRect(Color(0xFFF5F5F5), Offset(x + gap, iy), Size(colWidth - gap * 3, itemH), CornerRadius(4f))
                val iResult = textMeasurer.measure(item.label, itemStyle)
                drawText(iResult, topLeft = Offset(x + gap + 8f, iy + (itemH - iResult.size.height) / 2))
                iy += itemH + gap
            }
        }
        }
    }
}
