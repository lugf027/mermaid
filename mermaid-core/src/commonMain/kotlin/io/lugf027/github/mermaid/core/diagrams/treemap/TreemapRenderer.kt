package io.lugf027.github.mermaid.core.diagrams.treemap

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

class TreemapRenderer : DiagramRenderer {
    override fun draw(drawScope: DrawScope, db: DiagramDB, config: MermaidConfig, theme: ThemeVariables, textMeasurer: TextMeasurer, size: Size) {
        val tDb = db as? TreemapDb ?: return
        val root = tDb.root ?: return
        val colors = listOf(theme.primaryColor.toComposeColor(), theme.secondaryColor.toComposeColor(), theme.tertiaryColor.toComposeColor())
        val textColor = theme.primaryTextColor.toComposeColor()
        val style = TextStyle(fontSize = 11.sp, color = textColor)
        val margin = 20f

        with(drawScope) {
            drawTreemap(root.children.ifEmpty { listOf(root) }, margin, margin, size.width - margin * 2, size.height - margin * 2, true, 0, colors, textMeasurer, style, textColor)
        }
    }

    private fun DrawScope.drawTreemap(
        nodes: List<TreemapNode>, x: Float, y: Float, w: Float, h: Float,
        horizontal: Boolean, depth: Int, colors: List<Color>,
        textMeasurer: TextMeasurer, style: TextStyle, textColor: Color,
    ) {
        val total = nodes.sumOf { it.value.toDouble() }.toFloat()
        if (total <= 0 || nodes.isEmpty()) return
        var offset = 0f

        for ((idx, node) in nodes.withIndex()) {
            val ratio = node.value / total
            val nw = if (horizontal) w * ratio else w
            val nh = if (horizontal) h else h * ratio
            val nx = if (horizontal) x + offset else x
            val ny = if (horizontal) y else y + offset
            if (horizontal) offset += nw else offset += nh

            val color = colors[(depth + idx) % colors.size]
            drawRect(color.copy(alpha = 0.3f), Offset(nx, ny), Size(nw, nh))
            drawRect(color, Offset(nx, ny), Size(nw, nh), style = Stroke(1f))

            if (nw > 20f && nh > 15f) {
                val r = textMeasurer.measure(node.label, style)
                if (r.size.width < nw && r.size.height < nh) {
                    drawText(r, topLeft = Offset(nx + (nw - r.size.width) / 2, ny + (nh - r.size.height) / 2))
                }
            }

            if (node.children.isNotEmpty()) {
                drawTreemap(node.children, nx + 2, ny + 2, nw - 4, nh - 4, !horizontal, depth + 1, colors, textMeasurer, style, textColor)
            }
        }
    }
}
