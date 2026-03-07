package io.lugf027.github.mermaid.core.diagrams.timeline

import androidx.compose.ui.geometry.CornerRadius
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

class TimelineRenderer : DiagramRenderer {
    override fun draw(drawScope: DrawScope, db: DiagramDB, config: MermaidConfig, theme: ThemeVariables, textMeasurer: TextMeasurer, size: Size) {
        with(drawScope) {
        val tlDb = db as? TimelineDb ?: return
        val events = tlDb.getEvents()
        if (events.isEmpty()) return

        val textColor = theme.primaryTextColor.toComposeColor()
        val lineColor = theme.lineColor.toComposeColor()
        val colors = listOf(theme.primaryColor.toComposeColor(), theme.secondaryColor.toComposeColor(), theme.tertiaryColor.toComposeColor())
        val style = TextStyle(fontSize = 13.sp, color = textColor)
        val periodStyle = TextStyle(fontSize = 14.sp, color = textColor)

        val title = tlDb.getDiagramTitle()
        var y = 40f
        if (title.isNotEmpty()) {
            val r = textMeasurer.measure(title, TextStyle(fontSize = 18.sp, color = textColor))
            drawText(r, topLeft = Offset((size.width - r.size.width) / 2, 8f))
            y = 40f
        }

        val centerY = size.height / 2
        val lineY = centerY
        // 时间轴线
        drawLine(lineColor, Offset(40f, lineY), Offset(size.width - 40f, lineY), strokeWidth = 2f)

        val spacing = (size.width - 80f) / maxOf(events.size, 1)

        for ((idx, event) in events.withIndex()) {
            val x = 40f + spacing * idx + spacing / 2
            val color = colors[idx % colors.size]

            // 时间点圆
            drawCircle(color, 8f, Offset(x, lineY))

            // Period 文本（上方）
            val pResult = textMeasurer.measure(event.period, periodStyle)
            drawText(pResult, topLeft = Offset(x - pResult.size.width / 2, lineY - 30f - pResult.size.height))

            // 事件卡片（下方）
            var eventY = lineY + 20f
            for (ev in event.events) {
                val eResult = textMeasurer.measure(ev, style)
                val cardW = eResult.size.width + 16f
                val cardH = eResult.size.height + 12f
                drawRoundRect(color.copy(alpha = 0.15f), Offset(x - cardW / 2, eventY), Size(cardW, cardH), CornerRadius(4f))
                drawText(eResult, topLeft = Offset(x - eResult.size.width / 2, eventY + 6f))
                eventY += cardH + 8f
            }
        }
        }
    }
}
