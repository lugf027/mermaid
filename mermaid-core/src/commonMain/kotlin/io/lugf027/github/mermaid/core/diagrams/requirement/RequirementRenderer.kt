package io.lugf027.github.mermaid.core.diagrams.requirement

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

class RequirementRenderer : DiagramRenderer {
    override fun draw(drawScope: DrawScope, db: DiagramDB, config: MermaidConfig, theme: ThemeVariables, textMeasurer: TextMeasurer, size: Size) {
        with(drawScope) {
        val rDb = db as? RequirementDb ?: return
        val reqs = rDb.getRequirements()
        val elems = rDb.getElements()
        val rels = rDb.getRelations()
        val textColor = theme.primaryTextColor.toComposeColor()
        val style = TextStyle(fontSize = 12.sp, color = textColor)
        val positions = mutableMapOf<String, Offset>()
        val boxW = 180f; val boxH = 80f; val gap = 40f
        var x = 40f; var y = 40f; var col = 0

        for ((name, req) in reqs) {
            drawRect(theme.primaryColor.toComposeColor(), Offset(x, y), Size(boxW, boxH))
            drawRect(theme.primaryBorderColor.toComposeColor(), Offset(x, y), Size(boxW, boxH), style = Stroke(1.5f))
            val nr = textMeasurer.measure("<<${req.type}>>", TextStyle(fontSize = 10.sp, color = Color.White))
            drawText(nr, topLeft = Offset(x + (boxW - nr.size.width) / 2, y + 4f))
            val tr = textMeasurer.measure(name, TextStyle(fontSize = 13.sp, color = Color.White))
            drawText(tr, topLeft = Offset(x + (boxW - tr.size.width) / 2, y + 24f))
            if (req.text.isNotEmpty()) { val dr = textMeasurer.measure(req.text, TextStyle(fontSize = 10.sp, color = Color.White)); drawText(dr, topLeft = Offset(x + (boxW - dr.size.width) / 2, y + 46f)) }
            positions[name] = Offset(x + boxW / 2, y + boxH / 2)
            col++; x += boxW + gap; if (col >= 3) { col = 0; x = 40f; y += boxH + gap }
        }

        for ((name, elem) in elems) {
            drawRect(theme.secondaryColor.toComposeColor(), Offset(x, y), Size(boxW, boxH))
            drawRect(theme.secondaryBorderColor.toComposeColor(), Offset(x, y), Size(boxW, boxH), style = Stroke(1.5f))
            val tr = textMeasurer.measure(name, TextStyle(fontSize = 13.sp, color = textColor))
            drawText(tr, topLeft = Offset(x + (boxW - tr.size.width) / 2, y + 20f))
            if (elem.type.isNotEmpty()) { val er = textMeasurer.measure(elem.type, style); drawText(er, topLeft = Offset(x + (boxW - er.size.width) / 2, y + 44f)) }
            positions[name] = Offset(x + boxW / 2, y + boxH / 2)
            col++; x += boxW + gap; if (col >= 3) { col = 0; x = 40f; y += boxH + gap }
        }

        for (rel in rels) {
            val from = positions[rel.src] ?: continue; val to = positions[rel.dst] ?: continue
            drawLine(theme.lineColor.toComposeColor(), from, to, 1.5f)
            val mid = Offset((from.x + to.x) / 2, (from.y + to.y) / 2)
            val lr = textMeasurer.measure(rel.type, TextStyle(fontSize = 10.sp, color = textColor))
            drawText(lr, topLeft = Offset(mid.x - lr.size.width / 2, mid.y - lr.size.height - 4f))
        }
        }
    }
}
