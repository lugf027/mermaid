package io.lugf027.github.mermaid.core.diagrams.classdiagram

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
import io.lugf027.github.mermaid.core.renderer.layout.DagreLayout
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.types.*
import kotlin.math.min

/**
 * 类图 Compose Canvas 渲染器。
 * 使用 Dagre 布局引擎计算坐标，绘制类框（含成员/方法分区）和关系线。
 */
class ClassRenderer : DiagramRenderer {

    companion object {
        private const val FONT_SIZE = 12f
        private const val TITLE_FONT_SIZE = 14f
        private const val PADDING = 10f
        private const val STROKE_WIDTH = 2f
        private const val MARGIN = 20f
    }

    override fun draw(
        drawScope: DrawScope,
        db: DiagramDB,
        config: MermaidConfig,
        theme: ThemeVariables,
        textMeasurer: TextMeasurer,
        size: Size,
    ) {
        val classDb = db as? ClassDb ?: return
        val layoutData = classDb.getData()
        if (layoutData.nodes.isEmpty()) return

        val textStyle = TextStyle(fontSize = FONT_SIZE.sp)

        // 测量节点文本尺寸
        val updatedNodes = layoutData.nodes.map { node ->
            if (node.isGroup) return@map node
            val lines = node.label.lines()
            var maxW = 0f
            var totalH = 0f
            for (line in lines) {
                val m = textMeasurer.measure(line, textStyle)
                if (m.size.width > maxW) maxW = m.size.width.toFloat()
                totalH += m.size.height.toFloat()
            }
            node.copy(width = maxW + PADDING * 4, height = totalH + PADDING * 3)
        }

        val dagre = DagreLayout()
        val renderData = dagre.layout(layoutData.copy(nodes = updatedNodes))

        val bounds = renderData.bounds
        val scale = min(
            (size.width - MARGIN * 2) / bounds.width.coerceAtLeast(1f),
            (size.height - MARGIN * 2) / bounds.height.coerceAtLeast(1f),
        ).coerceIn(0.3f, 2f)

        val offsetX = (size.width - bounds.width * scale) / 2f - bounds.x * scale
        val offsetY = MARGIN - bounds.y * scale

        val nodeMap = renderData.nodes.associateBy { it.id }
        val bgColor = theme.mainBkg.toComposeColor()
        val borderColor = theme.border1.toComposeColor()
        val txtColor = theme.textColor.toComposeColor()

        with(drawScope) {
            // 绘制边
            for (edge in renderData.edges) {
                val s = nodeMap[edge.start] ?: continue
                val t = nodeMap[edge.end] ?: continue
                val sx = s.x * scale + offsetX
                val sy = s.y * scale + offsetY
                val tx = t.x * scale + offsetX
                val ty = t.y * scale + offsetY
                val lineColor = theme.lineColor.toComposeColor()
                val sw = if (edge.stroke == StrokeType.DOTTED) {
                    drawLine(lineColor, Offset(sx, sy), Offset(tx, ty), strokeWidth = STROKE_WIDTH * scale,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f * scale, 4f * scale)))
                    STROKE_WIDTH * scale
                } else {
                    drawLine(lineColor, Offset(sx, sy), Offset(tx, ty), strokeWidth = STROKE_WIDTH * scale)
                    STROKE_WIDTH * scale
                }
            }

            // 绘制节点
            val scaledTextStyle = TextStyle(fontSize = (FONT_SIZE * scale).sp, color = txtColor)
            for (node in renderData.nodes) {
                if (node.isGroup) continue
                val cx = node.x * scale + offsetX
                val cy = node.y * scale + offsetY
                val w = node.width * scale
                val h = node.height * scale
                val left = cx - w / 2f
                val top = cy - h / 2f

                // 背景
                drawRect(bgColor, Offset(left, top), Size(w, h))
                drawRect(borderColor, Offset(left, top), Size(w, h), style = Stroke(STROKE_WIDTH * scale))

                // 文本（按行绘制）
                val lines = node.label.lines()
                var textY = top + PADDING * scale
                for (line in lines) {
                    if (line == "---") {
                        // 分隔线
                        drawLine(borderColor, Offset(left, textY), Offset(left + w, textY), STROKE_WIDTH * 0.5f * scale)
                        textY += 4f * scale
                        continue
                    }
                    val m = textMeasurer.measure(line, scaledTextStyle)
                    drawText(m, topLeft = Offset(left + PADDING * scale, textY))
                    textY += m.size.height.toFloat()
                }
            }
        }
    }
}
