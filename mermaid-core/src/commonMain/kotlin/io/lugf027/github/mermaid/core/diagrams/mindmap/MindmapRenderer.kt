package io.lugf027.github.mermaid.core.diagrams.mindmap

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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 思维导图渲染器 — 放射状树布局。
 */
class MindmapRenderer : DiagramRenderer {

    companion object {
        private const val NODE_PADDING = 12f
        private const val LEVEL_RADIUS = 120f
        private const val FONT_SIZE = 13f
    }

    override fun draw(
        drawScope: DrawScope,
        db: DiagramDB,
        config: MermaidConfig,
        theme: ThemeVariables,
        textMeasurer: TextMeasurer,
        size: Size,
    ) {
        val mmDb = db as? MindmapDb ?: return
        val root = mmDb.getRoot() ?: return

        val textColor = theme.primaryTextColor.toComposeColor()
        val lineColor = theme.lineColor.toComposeColor()
        val colors = listOf(
            theme.primaryColor.toComposeColor(),
            theme.secondaryColor.toComposeColor(),
            theme.tertiaryColor.toComposeColor(),
        )

        val centerX = size.width / 2
        val centerY = size.height / 2

        // 递归绘制
        with(drawScope) {
            drawNode(root, centerX, centerY, 0f, 2 * PI.toFloat(), 0, textMeasurer, textColor, lineColor, colors,
                TextStyle(fontSize = FONT_SIZE.sp, color = textColor))
        }
    }

    private fun DrawScope.drawNode(
        node: MindmapNode,
        cx: Float, cy: Float,
        startAngle: Float, sweep: Float,
        depth: Int,
        textMeasurer: TextMeasurer,
        textColor: Color, lineColor: Color, colors: List<Color>,
        style: TextStyle,
    ) {
        val color = colors[depth % colors.size]
        val nodeRadius = 30f + (4 - minOf(depth, 3)) * 8f

        // 绘制节点
        when (node.type) {
            MindmapNodeType.CIRCLE -> drawCircle(color, nodeRadius, Offset(cx, cy))
            MindmapNodeType.RECT -> {
                val r = textMeasurer.measure(node.text, style)
                val w = r.size.width + NODE_PADDING * 2
                val h = r.size.height + NODE_PADDING * 2
                drawRect(color, Offset(cx - w / 2, cy - h / 2), Size(w, h))
            }
            MindmapNodeType.HEXAGON -> {
                drawCircle(color, nodeRadius, Offset(cx, cy))
                drawCircle(Color.Transparent, nodeRadius, Offset(cx, cy), style = Stroke(2f))
            }
            else -> drawCircle(color, nodeRadius, Offset(cx, cy))
        }

        // 节点文本
        val textResult = textMeasurer.measure(node.text, style)
        drawText(textResult, topLeft = Offset(cx - textResult.size.width / 2f, cy - textResult.size.height / 2f))

        // 子节点
        val childCount = node.children.size
        if (childCount == 0) return

        val childSweep = sweep / childCount
        for ((idx, child) in node.children.withIndex()) {
            val angle = startAngle + childSweep * idx + childSweep / 2
            val childCx = cx + LEVEL_RADIUS * cos(angle)
            val childCy = cy + LEVEL_RADIUS * sin(angle)

            // 连线
            drawLine(lineColor, Offset(cx, cy), Offset(childCx, childCy), strokeWidth = 2f)

            drawNode(child, childCx, childCy, angle - childSweep / 2, childSweep, depth + 1,
                textMeasurer, textColor, lineColor, colors, style)
        }
    }
}
