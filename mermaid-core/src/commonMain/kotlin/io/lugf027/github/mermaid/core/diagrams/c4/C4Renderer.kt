package io.lugf027.github.mermaid.core.diagrams.c4

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

class C4Renderer : DiagramRenderer {
    companion object {
        private const val BOX_W = 180f
        private const val BOX_H = 100f
        private const val GAP = 40f
        private const val COLS = 3
    }

    override fun draw(drawScope: DrawScope, db: DiagramDB, config: MermaidConfig, theme: ThemeVariables, textMeasurer: TextMeasurer, size: Size) {
        with(drawScope) {
        val c4Db = db as? C4Db ?: return
        val shapes = c4Db.getShapes()
        val rels = c4Db.getRels()

        val personColor = Color(0xFF08427B)
        val systemColor = theme.primaryColor.toComposeColor()
        val containerColor = Color(0xFF438DD5)
        val componentColor = Color(0xFF85BBF0)
        val extColor = Color(0xFF999999)
        val textColor = Color.White
        val nameStyle = TextStyle(fontSize = 13.sp, color = textColor)
        val descStyle = TextStyle(fontSize = 10.sp, color = textColor.copy(alpha = 0.8f))
        val relStyle = TextStyle(fontSize = 10.sp, color = theme.primaryTextColor.toComposeColor())

        val positions = mutableMapOf<String, Offset>()

        for ((idx, shape) in shapes.withIndex()) {
            val col = idx % COLS
            val row = idx / COLS
            val x = GAP + col * (BOX_W + GAP)
            val y = GAP + row * (BOX_H + GAP)
            positions[shape.alias] = Offset(x + BOX_W / 2, y + BOX_H / 2)

            val bgColor = when (shape.type) {
                C4ShapeType.PERSON, C4ShapeType.PERSON_EXT -> if (shape.type == C4ShapeType.PERSON_EXT) extColor else personColor
                C4ShapeType.SYSTEM, C4ShapeType.SYSTEM_EXT, C4ShapeType.SYSTEM_DB, C4ShapeType.SYSTEM_QUEUE -> if (shape.type == C4ShapeType.SYSTEM_EXT) extColor else systemColor
                C4ShapeType.CONTAINER, C4ShapeType.CONTAINER_DB, C4ShapeType.CONTAINER_QUEUE -> containerColor
                C4ShapeType.COMPONENT -> componentColor
            }

            drawRoundRect(bgColor, Offset(x, y), Size(BOX_W, BOX_H), CornerRadius(4f))

            if (shape.type == C4ShapeType.PERSON || shape.type == C4ShapeType.PERSON_EXT) {
                drawCircle(bgColor, 14f, Offset(x + BOX_W / 2, y - 10f))
            }

            val nResult = textMeasurer.measure(shape.label, nameStyle)
            drawText(nResult, topLeft = Offset(x + (BOX_W - nResult.size.width) / 2, y + 12f))

            if (shape.description.isNotEmpty()) {
                val dResult = textMeasurer.measure(shape.description, descStyle)
                drawText(dResult, topLeft = Offset(x + (BOX_W - dResult.size.width) / 2, y + 36f))
            }
            if (shape.technology.isNotEmpty()) {
                val tResult = textMeasurer.measure("[${shape.technology}]", descStyle)
                drawText(tResult, topLeft = Offset(x + (BOX_W - tResult.size.width) / 2, y + 56f))
            }
        }

        // 绘制关系线
        for (rel in rels) {
            val from = positions[rel.from] ?: continue
            val to = positions[rel.to] ?: continue
            drawLine(theme.lineColor.toComposeColor(), from, to, strokeWidth = 1.5f)
            if (rel.label.isNotEmpty()) {
                val mid = Offset((from.x + to.x) / 2, (from.y + to.y) / 2)
                val lResult = textMeasurer.measure(rel.label, relStyle)
                drawText(lResult, topLeft = Offset(mid.x - lResult.size.width / 2, mid.y - lResult.size.height - 4f))
            }
        }
        }
    }
}
