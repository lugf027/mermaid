package io.lugf027.github.mermaid.core.diagrams.radar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
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

class RadarRenderer : DiagramRenderer {
    override fun draw(drawScope: DrawScope, db: DiagramDB, config: MermaidConfig, theme: ThemeVariables, textMeasurer: TextMeasurer, size: Size) {
        with(drawScope) {
        val rDb = db as? RadarDb ?: return
        val axes = rDb.getAxes(); val curves = rDb.getCurves()
        if (axes.isEmpty()) return

        val cx = size.width / 2; val cy = size.height / 2
        val radius = minOf(size.width, size.height) / 2 - 60f
        val n = axes.size
        val angleStep = 2 * PI.toFloat() / n
        val textColor = theme.primaryTextColor.toComposeColor()
        val lineColor = theme.lineColor.toComposeColor()
        val style = TextStyle(fontSize = 11.sp, color = textColor)
        val colors = listOf(theme.primaryColor.toComposeColor(), theme.secondaryColor.toComposeColor(), theme.tertiaryColor.toComposeColor())

        // Graticule (grid)
        for (t in 1..rDb.ticks) {
            val r = radius * t / rDb.ticks
            if (rDb.graticule == "polygon") {
                val path = Path()
                for (j in 0 until n) {
                    val angle = -PI.toFloat() / 2 + j * angleStep
                    val px = cx + r * cos(angle); val py = cy + r * sin(angle)
                    if (j == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawPath(path, lineColor.copy(alpha = 0.2f), style = Stroke(1f))
            } else {
                drawCircle(lineColor.copy(alpha = 0.2f), r, Offset(cx, cy), style = Stroke(1f))
            }
        }

        // Axes
        for (j in 0 until n) {
            val angle = -PI.toFloat() / 2 + j * angleStep
            val px = cx + radius * cos(angle); val py = cy + radius * sin(angle)
            drawLine(lineColor.copy(alpha = 0.4f), Offset(cx, cy), Offset(px, py), 1f)
            val lr = textMeasurer.measure(axes[j].label, style)
            drawText(lr, topLeft = Offset(px - lr.size.width / 2, py + if (py < cy) -lr.size.height - 4f else 4f))
        }

        // Curves
        val maxVal = rDb.maxVal ?: curves.flatMap { it.entries }.maxOrNull() ?: 100f
        for ((cIdx, curve) in curves.withIndex()) {
            val color = colors[cIdx % colors.size]
            val path = Path()
            for (j in curve.entries.indices) {
                val v = curve.entries[j] / maxVal
                val angle = -PI.toFloat() / 2 + j * angleStep
                val px = cx + radius * v * cos(angle); val py = cy + radius * v * sin(angle)
                if (j == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            drawPath(path, color.copy(alpha = 0.2f))
            drawPath(path, color, style = Stroke(2f))

            for (j in curve.entries.indices) {
                val v = curve.entries[j] / maxVal
                val angle = -PI.toFloat() / 2 + j * angleStep
                drawCircle(color, 4f, Offset(cx + radius * v * cos(angle), cy + radius * v * sin(angle)))
            }
        }

        val title = rDb.getDiagramTitle()
        if (title.isNotEmpty()) {
            val tr = textMeasurer.measure(title, TextStyle(fontSize = 16.sp, color = textColor))
            drawText(tr, topLeft = Offset((size.width - tr.size.width) / 2, 8f))
        }
        }
    }
}
