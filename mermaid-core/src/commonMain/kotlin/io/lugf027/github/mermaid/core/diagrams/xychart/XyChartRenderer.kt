package io.lugf027.github.mermaid.core.diagrams.xychart

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

class XyChartRenderer : DiagramRenderer {
    override fun draw(drawScope: DrawScope, db: DiagramDB, config: MermaidConfig, theme: ThemeVariables, textMeasurer: TextMeasurer, size: Size) {
        with(drawScope) {
        val xyDb = db as? XyChartDb ?: return
        val margin = 60f
        val chartW = size.width - margin * 2
        val chartH = size.height - margin * 2 - 40f
        val textColor = theme.primaryTextColor.toComposeColor()
        val lineColor = theme.lineColor.toComposeColor()
        val style = TextStyle(fontSize = 11.sp, color = textColor)

        // 轴线
        drawLine(lineColor, Offset(margin, margin + chartH), Offset(margin + chartW, margin + chartH), 1.5f)
        drawLine(lineColor, Offset(margin, margin), Offset(margin, margin + chartH), 1.5f)

        // 计算数据范围
        val allData = xyDb.plots.flatMap { it.data }
        val maxVal = xyDb.yAxis.max ?: (allData.maxOrNull() ?: 100f)
        val minVal = xyDb.yAxis.min ?: 0f
        val range = maxVal - minVal
        val cats = xyDb.xAxis.categories ?: (1..maxOf(allData.size, 1)).map { it.toString() }
        val barW = chartW / maxOf(cats.size, 1)

        // X 轴标签
        for ((idx, cat) in cats.withIndex()) {
            val r = textMeasurer.measure(cat, style)
            drawText(r, topLeft = Offset(margin + idx * barW + barW / 2 - r.size.width / 2, margin + chartH + 8f))
        }

        // 绘制 plots
        val colors = listOf(theme.primaryColor.toComposeColor(), theme.secondaryColor.toComposeColor(), theme.tertiaryColor.toComposeColor())
        for ((pIdx, plot) in xyDb.plots.withIndex()) {
            val color = colors[pIdx % colors.size]
            when (plot.type) {
                PlotType.BAR -> {
                    val bw = barW * 0.6f / maxOf(xyDb.plots.count { it.type == PlotType.BAR }, 1)
                    for ((idx, v) in plot.data.withIndex()) {
                        val h = if (range > 0) (v - minVal) / range * chartH else 0f
                        val x = margin + idx * barW + barW * 0.2f + pIdx * bw
                        drawRoundRect(color, Offset(x, margin + chartH - h), Size(bw, h), CornerRadius(2f))
                    }
                }
                PlotType.LINE -> {
                    for (idx in 0 until plot.data.size - 1) {
                        val v1 = plot.data[idx]; val v2 = plot.data[idx + 1]
                        val h1 = if (range > 0) (v1 - minVal) / range * chartH else 0f
                        val h2 = if (range > 0) (v2 - minVal) / range * chartH else 0f
                        val x1 = margin + idx * barW + barW / 2
                        val x2 = margin + (idx + 1) * barW + barW / 2
                        drawLine(color, Offset(x1, margin + chartH - h1), Offset(x2, margin + chartH - h2), 2f)
                    }
                    for ((idx, v) in plot.data.withIndex()) {
                        val h = if (range > 0) (v - minVal) / range * chartH else 0f
                        drawCircle(color, 4f, Offset(margin + idx * barW + barW / 2, margin + chartH - h))
                    }
                }
            }
        }

        val title = xyDb.getDiagramTitle()
        if (title.isNotEmpty()) {
            val tr = textMeasurer.measure(title, TextStyle(fontSize = 16.sp, color = textColor))
            drawText(tr, topLeft = Offset((size.width - tr.size.width) / 2, 8f))
        }
        }
    }
}
