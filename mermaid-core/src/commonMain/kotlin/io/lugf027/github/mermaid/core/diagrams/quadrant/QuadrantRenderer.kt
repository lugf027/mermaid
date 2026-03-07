package io.lugf027.github.mermaid.core.diagrams.quadrant

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

class QuadrantRenderer : DiagramRenderer {
    override fun draw(drawScope: DrawScope, db: DiagramDB, config: MermaidConfig, theme: ThemeVariables, textMeasurer: TextMeasurer, size: Size) {
        with(drawScope) {
        val qDb = db as? QuadrantDb ?: return
        val margin = 60f
        val chartW = size.width - margin * 2
        val chartH = size.height - margin * 2
        val midX = margin + chartW / 2
        val midY = margin + chartH / 2
        val textColor = theme.primaryTextColor.toComposeColor()
        val lineColor = theme.lineColor.toComposeColor()
        val style = TextStyle(fontSize = 12.sp, color = textColor)
        val colors = listOf(Color(0x330066FF), Color(0x3300CC66), Color(0x33FF9900), Color(0x33FF3333))

        // 四象限背景
        drawRect(colors[0], Offset(midX, margin), Size(chartW / 2, chartH / 2))     // Q1 top-right
        drawRect(colors[1], Offset(margin, margin), Size(chartW / 2, chartH / 2))   // Q2 top-left
        drawRect(colors[2], Offset(margin, midY), Size(chartW / 2, chartH / 2))     // Q3 bottom-left
        drawRect(colors[3], Offset(midX, midY), Size(chartW / 2, chartH / 2))       // Q4 bottom-right

        // 轴线
        drawLine(lineColor, Offset(margin, midY), Offset(margin + chartW, midY), 1.5f)
        drawLine(lineColor, Offset(midX, margin), Offset(midX, margin + chartH), 1.5f)

        // 象限标签
        val q1R = textMeasurer.measure(qDb.q1Text, style); drawText(q1R, topLeft = Offset(midX + chartW / 4 - q1R.size.width / 2, margin + chartH / 4 - q1R.size.height / 2))
        val q2R = textMeasurer.measure(qDb.q2Text, style); drawText(q2R, topLeft = Offset(margin + chartW / 4 - q2R.size.width / 2, margin + chartH / 4 - q2R.size.height / 2))
        val q3R = textMeasurer.measure(qDb.q3Text, style); drawText(q3R, topLeft = Offset(margin + chartW / 4 - q3R.size.width / 2, midY + chartH / 4 - q3R.size.height / 2))
        val q4R = textMeasurer.measure(qDb.q4Text, style); drawText(q4R, topLeft = Offset(midX + chartW / 4 - q4R.size.width / 2, midY + chartH / 4 - q4R.size.height / 2))

        // 轴标签
        val xlR = textMeasurer.measure(qDb.xAxisLeft, style); drawText(xlR, topLeft = Offset(margin, margin + chartH + 8f))
        val xrR = textMeasurer.measure(qDb.xAxisRight, style); drawText(xrR, topLeft = Offset(margin + chartW - xrR.size.width, margin + chartH + 8f))
        val ybR = textMeasurer.measure(qDb.yAxisBottom, style); drawText(ybR, topLeft = Offset(margin - ybR.size.width - 8f, margin + chartH - ybR.size.height))
        val ytR = textMeasurer.measure(qDb.yAxisTop, style); drawText(ytR, topLeft = Offset(margin - ytR.size.width - 8f, margin))

        // 数据点
        for (p in qDb.getPoints()) {
            val px = margin + p.x * chartW
            val py = margin + (1 - p.y) * chartH
            drawCircle(theme.primaryColor.toComposeColor(), 6f, Offset(px, py))
            val lr = textMeasurer.measure(p.label, style)
            drawText(lr, topLeft = Offset(px - lr.size.width / 2, py + 10f))
        }

        val title = qDb.getDiagramTitle()
        if (title.isNotEmpty()) {
            val tr = textMeasurer.measure(title, TextStyle(fontSize = 16.sp, color = textColor))
            drawText(tr, topLeft = Offset((size.width - tr.size.width) / 2, 8f))
        }
        }
    }
}
