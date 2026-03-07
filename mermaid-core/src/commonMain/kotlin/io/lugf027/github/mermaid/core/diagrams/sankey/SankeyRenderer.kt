package io.lugf027.github.mermaid.core.diagrams.sankey

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

class SankeyRenderer : DiagramRenderer {
    override fun draw(drawScope: DrawScope, db: DiagramDB, config: MermaidConfig, theme: ThemeVariables, textMeasurer: TextMeasurer, size: Size) {
        with(drawScope) {
        val sDb = db as? SankeyDb ?: return
        val nodes = sDb.getNodes(); val links = sDb.getLinks()
        if (nodes.isEmpty()) return

        val textColor = theme.primaryTextColor.toComposeColor()
        val style = TextStyle(fontSize = 12.sp, color = textColor)
        val colors = listOf(theme.primaryColor.toComposeColor(), theme.secondaryColor.toComposeColor(), theme.tertiaryColor.toComposeColor())
        val margin = 40f; val nodeW = 20f
        val chartH = size.height - margin * 2

        // Simplified sankey: left nodes → right nodes
        val sources = links.map { it.source }.distinct()
        val targets = links.map { it.target }.distinct().filter { it !in sources }
        val leftNodes = sources; val rightNodes = if (targets.isEmpty()) nodes.drop(leftNodes.size) else targets

        val totalValue = links.sumOf { it.value.toDouble() }.toFloat()
        // Left column
        var ly = margin
        val leftPositions = mutableMapOf<String, Pair<Float, Float>>() // node -> (y, height)
        for (node in leftNodes) {
            val nodeValue = links.filter { it.source == node }.sumOf { it.value.toDouble() }.toFloat()
            val h = if (totalValue > 0) (nodeValue / totalValue) * chartH else chartH / leftNodes.size
            drawRect(colors[leftNodes.indexOf(node) % colors.size], Offset(margin, ly), Size(nodeW, h))
            val r = textMeasurer.measure(node, style)
            drawText(r, topLeft = Offset(margin - r.size.width - 8f, ly + h / 2 - r.size.height / 2))
            leftPositions[node] = ly to h
            ly += h + 4f
        }
        // Right column
        var ry = margin
        val rightPositions = mutableMapOf<String, Pair<Float, Float>>()
        for (node in rightNodes) {
            val nodeValue = links.filter { it.target == node }.sumOf { it.value.toDouble() }.toFloat()
            val h = if (totalValue > 0) (nodeValue / totalValue) * chartH else chartH / maxOf(rightNodes.size, 1)
            val rx = size.width - margin - nodeW
            drawRect(colors[rightNodes.indexOf(node) % colors.size], Offset(rx, ry), Size(nodeW, h))
            val r = textMeasurer.measure(node, style)
            drawText(r, topLeft = Offset(rx + nodeW + 8f, ry + h / 2 - r.size.height / 2))
            rightPositions[node] = ry to h
            ry += h + 4f
        }
        // Links
        for (link in links) {
            val (ly2, lh) = leftPositions[link.source] ?: continue
            val (ry2, rh) = rightPositions[link.target] ?: continue
            val color = colors[0].copy(alpha = 0.3f)
            drawLine(color, Offset(margin + nodeW, ly2 + lh / 2), Offset(size.width - margin - nodeW, ry2 + rh / 2), strokeWidth = maxOf(link.value / totalValue * chartH * 0.5f, 2f))
        }
        }
    }
}
