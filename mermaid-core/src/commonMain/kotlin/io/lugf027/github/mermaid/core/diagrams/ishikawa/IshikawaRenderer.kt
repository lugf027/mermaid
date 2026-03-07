package io.lugf027.github.mermaid.core.diagrams.ishikawa

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
import kotlin.math.cos
import kotlin.math.sin

class IshikawaRenderer : DiagramRenderer {
    override fun draw(drawScope: DrawScope, db: DiagramDB, config: MermaidConfig, theme: ThemeVariables, textMeasurer: TextMeasurer, size: Size) {
        with(drawScope) {
        val iDb = db as? IshikawaDb ?: return
        val root = iDb.root ?: return

        val textColor = theme.primaryTextColor.toComposeColor()
        val lineColor = theme.lineColor.toComposeColor()
        val style = TextStyle(fontSize = 12.sp, color = textColor)
        val headX = size.width - 80f; val spineY = size.height / 2
        val spineStartX = 60f

        // Main spine (horizontal)
        drawLine(lineColor, Offset(spineStartX, spineY), Offset(headX, spineY), 3f)

        // Effect (head) box
        val headR = textMeasurer.measure(root.text, TextStyle(fontSize = 14.sp, color = textColor))
        drawRect(theme.primaryColor.toComposeColor(), Offset(headX, spineY - headR.size.height / 2 - 8f), Size(headR.size.width.toFloat() + 16f, headR.size.height.toFloat() + 16f))
        drawText(headR, topLeft = Offset(headX + 8f, spineY - headR.size.height / 2))

        // Cause bones
        val causes = root.children
        val boneAngle = 0.5f // ~30 degrees
        val boneLength = 120f
        val spacing = (headX - spineStartX) / maxOf(causes.size + 1, 1)

        for ((idx, cause) in causes.withIndex()) {
            val attachX = spineStartX + spacing * (idx + 1)
            val isTop = idx % 2 == 0
            val dir = if (isTop) -1f else 1f
            val endX = attachX - boneLength * cos(boneAngle)
            val endY = spineY + dir * boneLength * sin(boneAngle)

            drawLine(lineColor, Offset(attachX, spineY), Offset(endX, endY), 2f)

            val cr = textMeasurer.measure(cause.text, style)
            drawText(cr, topLeft = Offset(endX - cr.size.width / 2, endY + if (isTop) -cr.size.height - 4f else 4f))

            // Sub-causes
            for ((sIdx, sub) in cause.children.withIndex()) {
                val subOffset = (sIdx + 1) * 25f
                val sx = endX + subOffset * cos(boneAngle) * 0.6f
                val sy = endY - dir * subOffset * sin(boneAngle) * 0.3f
                val subEndX = sx - 40f
                drawLine(lineColor.copy(alpha = 0.6f), Offset(sx, sy), Offset(subEndX, sy), 1f)
                val sr = textMeasurer.measure(sub.text, TextStyle(fontSize = 10.sp, color = textColor))
                drawText(sr, topLeft = Offset(subEndX - sr.size.width - 4f, sy - sr.size.height / 2))
            }
        }
        }
    }
}
