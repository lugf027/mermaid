package io.lugf027.github.mermaid.core.diagrams.state

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
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
 * 状态图 Compose Canvas 渲染器。
 * 使用 Dagre 布局引擎。
 */
class StateRenderer : DiagramRenderer {

    companion object {
        private const val FONT_SIZE = 14f
        private const val PADDING = 12f
        private const val STROKE_WIDTH = 2f
        private const val MARGIN = 20f
        private const val START_END_RADIUS = 8f
    }

    override fun draw(
        drawScope: DrawScope,
        db: DiagramDB,
        config: MermaidConfig,
        theme: ThemeVariables,
        textMeasurer: TextMeasurer,
        size: Size,
    ) {
        val stateDb = db as? StateDb ?: return
        val layoutData = stateDb.getData()
        if (layoutData.nodes.isEmpty()) return

        val textStyle = TextStyle(fontSize = FONT_SIZE.sp)

        // 测量节点尺寸
        val updatedNodes = layoutData.nodes.map { node ->
            if (node.isGroup) return@map node
            when (node.shape) {
                ShapeId.STATE_START, ShapeId.STATE_END -> node.copy(width = START_END_RADIUS * 4, height = START_END_RADIUS * 4)
                ShapeId.FORK_JOIN -> node.copy(width = 60f, height = 6f)
                ShapeId.CHOICE -> node.copy(width = 30f, height = 30f)
                else -> {
                    val m = textMeasurer.measure(node.label, textStyle)
                    node.copy(width = m.size.width.toFloat() + PADDING * 3, height = m.size.height.toFloat() + PADDING * 2)
                }
            }
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
        val stateBg = theme.stateBkg.toComposeColor()
        val stateBorder = theme.compositeBorder.toComposeColor()
        val txtColor = theme.textColor.toComposeColor()
        val lineColor = theme.transitionColor.toComposeColor()
        val specialColor = theme.specialStateColor.toComposeColor()

        with(drawScope) {
            // 绘制边
            for (edge in renderData.edges) {
                val s = nodeMap[edge.start] ?: continue
                val t = nodeMap[edge.end] ?: continue
                val sx = s.x * scale + offsetX; val sy = s.y * scale + offsetY
                val tx = t.x * scale + offsetX; val ty = t.y * scale + offsetY
                drawLine(lineColor, Offset(sx, sy), Offset(tx, ty), STROKE_WIDTH * scale)

                // 简单箭头
                val dx = tx - sx; val dy = ty - sy
                val len = kotlin.math.sqrt(dx * dx + dy * dy)
                if (len > 0) {
                    val ux = dx / len; val uy = dy / len
                    val arrSize = 8f * scale
                    drawLine(lineColor, Offset(tx, ty), Offset(tx - arrSize * ux + arrSize * 0.4f * uy, ty - arrSize * uy - arrSize * 0.4f * ux), STROKE_WIDTH * scale)
                    drawLine(lineColor, Offset(tx, ty), Offset(tx - arrSize * ux - arrSize * 0.4f * uy, ty - arrSize * uy + arrSize * 0.4f * ux), STROKE_WIDTH * scale)
                }

                // 边标签
                if (edge.label.isNotEmpty()) {
                    val labelStyle = TextStyle(fontSize = (FONT_SIZE * 0.85f * scale).sp, color = theme.transitionLabelColor.toComposeColor())
                    val m = textMeasurer.measure(edge.label, labelStyle)
                    drawText(m, topLeft = Offset((sx + tx) / 2f - m.size.width / 2f, (sy + ty) / 2f - m.size.height))
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

                when (node.shape) {
                    ShapeId.STATE_START -> {
                        drawCircle(specialColor, START_END_RADIUS * scale, Offset(cx, cy), style = Fill)
                    }
                    ShapeId.STATE_END -> {
                        drawCircle(specialColor, START_END_RADIUS * scale, Offset(cx, cy), style = Fill)
                        drawCircle(Color.White, START_END_RADIUS * scale * 0.6f, Offset(cx, cy), style = Fill)
                        drawCircle(specialColor, START_END_RADIUS * scale * 0.6f, Offset(cx, cy), style = Stroke(STROKE_WIDTH * scale))
                    }
                    ShapeId.FORK_JOIN -> {
                        drawRect(specialColor, Offset(cx - w / 2f, cy - h / 2f), Size(w, h))
                    }
                    ShapeId.CHOICE -> {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(cx, cy - h / 2f)
                            lineTo(cx + w / 2f, cy)
                            lineTo(cx, cy + h / 2f)
                            lineTo(cx - w / 2f, cy)
                            close()
                        }
                        drawPath(path, stateBg, style = Fill)
                        drawPath(path, stateBorder, style = Stroke(STROKE_WIDTH * scale))
                    }
                    else -> {
                        val cr = androidx.compose.ui.geometry.CornerRadius(5f * scale)
                        drawRoundRect(stateBg, Offset(cx - w / 2f, cy - h / 2f), Size(w, h), cr)
                        drawRoundRect(stateBorder, Offset(cx - w / 2f, cy - h / 2f), Size(w, h), cr, style = Stroke(STROKE_WIDTH * scale))

                        val m = textMeasurer.measure(node.label, scaledTextStyle)
                        drawText(m, topLeft = Offset(cx - m.size.width / 2f, cy - m.size.height / 2f))
                    }
                }
            }
        }
    }
}
