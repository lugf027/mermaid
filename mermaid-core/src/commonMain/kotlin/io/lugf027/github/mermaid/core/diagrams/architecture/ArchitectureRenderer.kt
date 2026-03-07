package io.lugf027.github.mermaid.core.diagrams.architecture

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

class ArchitectureRenderer : DiagramRenderer {
    override fun draw(drawScope: DrawScope, db: DiagramDB, config: MermaidConfig, theme: ThemeVariables, textMeasurer: TextMeasurer, size: Size) {
        with(drawScope) {
        val aDb = db as? ArchitectureDb ?: return
        val nodes = aDb.getNodes(); val edges = aDb.getEdges()
        val textColor = theme.primaryTextColor.toComposeColor()
        val style = TextStyle(fontSize = 12.sp, color = textColor)
        val boxW = 120f; val boxH = 70f; val gap = 40f; val margin = 40f; val cols = 3
        val positions = mutableMapOf<String, Offset>()

        for ((idx, node) in nodes.withIndex()) {
            val col = idx % cols; val row = idx / cols
            val x = margin + col * (boxW + gap); val y = margin + row * (boxH + gap)
            val color = if (node.type == ArchNodeType.SERVICE) theme.primaryColor.toComposeColor() else theme.secondaryColor.toComposeColor()
            drawRoundRect(color, Offset(x, y), Size(boxW, boxH), CornerRadius(8f))
            drawRoundRect(theme.primaryBorderColor.toComposeColor(), Offset(x, y), Size(boxW, boxH), CornerRadius(8f), style = Stroke(1.5f))
            val r = textMeasurer.measure(node.title.ifEmpty { node.id }, style)
            drawText(r, topLeft = Offset(x + (boxW - r.size.width) / 2, y + (boxH - r.size.height) / 2))
            positions[node.id] = Offset(x + boxW / 2, y + boxH / 2)
        }

        for (edge in edges) {
            val from = positions[edge.lhsId] ?: continue; val to = positions[edge.rhsId] ?: continue
            drawLine(theme.lineColor.toComposeColor(), from, to, 1.5f)
        }
        }
    }
}
