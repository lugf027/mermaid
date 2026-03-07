package io.lugf027.github.mermaid.core.diagrams.sequence

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
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
import kotlin.math.max

/**
 * 时序图 Compose Canvas 渲染器。
 * 不使用 Dagre 布局，自行计算参与者位置、消息线、生命线。
 *
 * 渲染流程：
 * 1. 在顶部绘制参与者（Actor 方框）
 * 2. 逐行向下绘制消息箭头线
 * 3. 绘制 loop/alt/opt 等结构块的背景框
 * 4. 在底部再画一次参与者
 */
class SequenceRenderer : DiagramRenderer {

    companion object {
        private const val ACTOR_WIDTH = 120f
        private const val ACTOR_HEIGHT = 40f
        private const val ACTOR_MARGIN = 30f
        private const val MESSAGE_HEIGHT = 50f
        private const val TOP_MARGIN = 20f
        private const val FONT_SIZE = 14f
        private const val ARROW_SIZE = 8f
        private const val BLOCK_PADDING = 10f
        private const val NOTE_WIDTH = 100f
        private const val NOTE_HEIGHT = 30f
    }

    override fun draw(
        drawScope: DrawScope,
        db: DiagramDB,
        config: MermaidConfig,
        theme: ThemeVariables,
        textMeasurer: TextMeasurer,
        size: Size,
    ) {
        val seqDb = db as? SequenceDb ?: return
        val actorKeys = seqDb.getActorKeys()
        val actors = seqDb.getActors()
        val messages = seqDb.getMessages()

        if (actorKeys.isEmpty()) return

        // 计算缩放
        val totalWidth = actorKeys.size * (ACTOR_WIDTH + ACTOR_MARGIN) - ACTOR_MARGIN
        val totalMessages = messages.count { it.type.value < 10 } // 只计信号类消息
        val totalHeight = TOP_MARGIN * 2 + ACTOR_HEIGHT * 2 + max(totalMessages, 1) * MESSAGE_HEIGHT + 40f
        val scaleX = (size.width * 0.9f) / totalWidth.coerceAtLeast(1f)
        val scaleY = (size.height * 0.9f) / totalHeight.coerceAtLeast(1f)
        val scale = minOf(scaleX, scaleY, 2f).coerceAtLeast(0.3f)

        // 计算偏移（居中）
        val offsetX = (size.width - totalWidth * scale) / 2f
        val offsetY = TOP_MARGIN * scale

        // 参与者位置表
        val actorX = mutableMapOf<String, Float>()
        actorKeys.forEachIndexed { idx, key ->
            actorX[key] = idx * (ACTOR_WIDTH + ACTOR_MARGIN) * scale + ACTOR_WIDTH * scale / 2f + offsetX
        }

        val actorBoxColor = theme.actorBkg.toComposeColor()
        val actorBorderColor = theme.actorBorder.toComposeColor()
        val actorTextColor = theme.actorTextColor.toComposeColor()
        val signalColor = theme.signalColor.toComposeColor()
        val noteBgColor = theme.noteBkgColor.toComposeColor()
        val noteBorderColor = theme.noteBorderColor.toComposeColor()
        val noteTextColor = theme.noteTextColor.toComposeColor()

        val textStyle = TextStyle(fontSize = (FONT_SIZE * scale).sp, color = actorTextColor)
        val signalTextStyle = TextStyle(fontSize = (FONT_SIZE * scale).sp, color = signalColor)
        val noteTextStyle = TextStyle(fontSize = ((FONT_SIZE - 2) * scale).sp, color = noteTextColor)

        with(drawScope) {
            // 1. 绘制标题
            val title = seqDb.getDiagramTitle()
            if (title.isNotEmpty()) {
                val titleStyle = TextStyle(fontSize = (18f * scale).sp, color = actorTextColor)
                val titleResult = textMeasurer.measure(title, titleStyle)
                drawText(titleResult, topLeft = Offset((size.width - titleResult.size.width) / 2f, 4f * scale))
            }

            // 2. 绘制顶部参与者
            val topY = offsetY + 20f * scale
            drawActors(actorKeys, actors, actorX, topY, scale, actorBoxColor, actorBorderColor, textMeasurer, textStyle)

            // 3. 绘制生命线（竖线从参与者底部到消息区域底部）
            var currentY = topY + ACTOR_HEIGHT * scale + 10f * scale
            val lifelines = actorKeys.associateWith { currentY }

            // 4. 逐条绘制消息
            val blockStarts = mutableListOf<Float>() // 记录块起始 Y
            var msgIndex = 0
            for (msg in messages) {
                when {
                    msg.type.value in 0..6 || msg.type == LineType.SOLID_POINT || msg.type == LineType.DOTTED_POINT -> {
                        // 信号消息
                        val fromX = actorX[msg.from] ?: continue
                        val toX = actorX[msg.to] ?: continue
                        val isDotted = msg.type == LineType.DOTTED || msg.type == LineType.DOTTED_OPEN ||
                            msg.type == LineType.DOTTED_CROSS || msg.type == LineType.DOTTED_POINT
                        val isCross = msg.type == LineType.SOLID_CROSS || msg.type == LineType.DOTTED_CROSS
                        val isOpen = msg.type == LineType.SOLID_OPEN || msg.type == LineType.DOTTED_OPEN

                        drawSignalLine(fromX, toX, currentY, isDotted, isCross, isOpen, signalColor, ARROW_SIZE * scale, scale)

                        // 消息文本
                        if (msg.message.isNotEmpty()) {
                            val labelText = if (seqDb.isAutoNumber()) "${++msgIndex}. ${msg.message}" else msg.message
                            val labelResult = textMeasurer.measure(labelText, signalTextStyle)
                            val labelX = (fromX + toX) / 2f - labelResult.size.width / 2f
                            drawText(labelResult, topLeft = Offset(labelX, currentY - labelResult.size.height - 2f * scale))
                        }
                        currentY += MESSAGE_HEIGHT * scale
                    }
                    msg.type == LineType.NOTE -> {
                        val x = actorX[msg.from] ?: continue
                        val noteW = NOTE_WIDTH * scale
                        val noteH = NOTE_HEIGHT * scale
                        drawRoundRect(noteBgColor, Offset(x - noteW / 2f, currentY - noteH / 2f), Size(noteW, noteH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * scale))
                        drawRoundRect(noteBorderColor, Offset(x - noteW / 2f, currentY - noteH / 2f), Size(noteW, noteH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * scale), style = Stroke(1f * scale))
                        val noteResult = textMeasurer.measure(msg.message, noteTextStyle)
                        drawText(noteResult, topLeft = Offset(x - noteResult.size.width / 2f, currentY - noteResult.size.height / 2f))
                        currentY += MESSAGE_HEIGHT * scale
                    }
                    msg.type == LineType.LOOP_START || msg.type == LineType.ALT_START ||
                        msg.type == LineType.OPT_START || msg.type == LineType.PAR_START ||
                        msg.type == LineType.CRITICAL_START || msg.type == LineType.BREAK_START ||
                        msg.type == LineType.RECT_START -> {
                        blockStarts.add(currentY)
                        // 绘制块标签
                        if (msg.message.isNotEmpty()) {
                            val label = "${getBlockTypeName(msg.type)} [${msg.message}]"
                            val labelResult = textMeasurer.measure(label, signalTextStyle)
                            drawText(labelResult, topLeft = Offset(offsetX + 5f * scale, currentY))
                        }
                        currentY += 15f * scale
                    }
                    msg.type == LineType.LOOP_END || msg.type == LineType.ALT_END ||
                        msg.type == LineType.OPT_END || msg.type == LineType.PAR_END ||
                        msg.type == LineType.CRITICAL_END || msg.type == LineType.BREAK_END ||
                        msg.type == LineType.RECT_END -> {
                        if (blockStarts.isNotEmpty()) {
                            val startY = blockStarts.removeLast()
                            val blockWidth = totalWidth * scale
                            drawRoundRect(
                                Color.Gray.copy(alpha = 0.05f),
                                Offset(offsetX, startY - 5f * scale),
                                Size(blockWidth, currentY - startY + 10f * scale),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * scale),
                            )
                            drawRoundRect(
                                Color.Gray.copy(alpha = 0.3f),
                                Offset(offsetX, startY - 5f * scale),
                                Size(blockWidth, currentY - startY + 10f * scale),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * scale),
                                style = Stroke(1f * scale),
                            )
                        }
                    }
                    msg.type == LineType.ALT_ELSE || msg.type == LineType.PAR_AND ||
                        msg.type == LineType.CRITICAL_OPTION -> {
                        // 绘制虚线分隔
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4f * scale, 4f * scale))
                        drawLine(Color.Gray, Offset(offsetX, currentY), Offset(offsetX + totalWidth * scale, currentY),
                            strokeWidth = 1f * scale, pathEffect = dashEffect)
                        if (msg.message.isNotEmpty()) {
                            val label = "[${msg.message}]"
                            val labelResult = textMeasurer.measure(label, signalTextStyle)
                            drawText(labelResult, topLeft = Offset(offsetX + 5f * scale, currentY + 2f * scale))
                        }
                        currentY += 15f * scale
                    }
                    else -> {}
                }
            }

            // 5. 绘制生命线（竖虚线）
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4f * scale, 4f * scale))
            for (key in actorKeys) {
                val x = actorX[key] ?: continue
                drawLine(
                    Color.Gray,
                    Offset(x, topY + ACTOR_HEIGHT * scale),
                    Offset(x, currentY),
                    strokeWidth = 1f * scale,
                    pathEffect = dashEffect,
                )
            }

            // 6. 绘制底部参与者
            drawActors(actorKeys, actors, actorX, currentY + 10f * scale, scale, actorBoxColor, actorBorderColor, textMeasurer, textStyle)
        }
    }

    private fun DrawScope.drawActors(
        keys: List<String>,
        actors: Map<String, SequenceActor>,
        actorX: Map<String, Float>,
        y: Float,
        scale: Float,
        bgColor: Color,
        borderColor: Color,
        textMeasurer: TextMeasurer,
        textStyle: TextStyle,
    ) {
        val w = ACTOR_WIDTH * scale
        val h = ACTOR_HEIGHT * scale
        for (key in keys) {
            val x = actorX[key] ?: continue
            val actor = actors[key] ?: continue
            val left = x - w / 2f

            if (actor.type == ActorType.ACTOR) {
                // 人形图标
                val headRadius = 10f * scale
                drawCircle(borderColor, headRadius, Offset(x, y + headRadius), style = Stroke(2f * scale))
                drawLine(borderColor, Offset(x, y + headRadius * 2), Offset(x, y + h * 0.65f), 2f * scale)
                drawLine(borderColor, Offset(x - 15f * scale, y + h * 0.35f), Offset(x + 15f * scale, y + h * 0.35f), 2f * scale)
                drawLine(borderColor, Offset(x, y + h * 0.65f), Offset(x - 10f * scale, y + h), 2f * scale)
                drawLine(borderColor, Offset(x, y + h * 0.65f), Offset(x + 10f * scale, y + h), 2f * scale)
            } else {
                // 方框
                drawRoundRect(bgColor, Offset(left, y), Size(w, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * scale))
                drawRoundRect(borderColor, Offset(left, y), Size(w, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * scale),
                    style = Stroke(2f * scale))
            }

            // 名字
            val nameResult = textMeasurer.measure(actor.description, textStyle)
            drawText(nameResult, topLeft = Offset(
                x - nameResult.size.width / 2f,
                if (actor.type == ActorType.ACTOR) y + h + 2f * scale else y + (h - nameResult.size.height) / 2f
            ))
        }
    }

    private fun DrawScope.drawSignalLine(
        fromX: Float, toX: Float, y: Float,
        isDotted: Boolean, isCross: Boolean, isOpen: Boolean,
        color: Color, arrowSize: Float, scale: Float,
    ) {
        val strokeWidth = 2f * scale
        if (isDotted) {
            val pe = PathEffect.dashPathEffect(floatArrayOf(6f * scale, 4f * scale))
            drawLine(color, Offset(fromX, y), Offset(toX, y), strokeWidth = strokeWidth, pathEffect = pe)
        } else {
            drawLine(color, Offset(fromX, y), Offset(toX, y), strokeWidth = strokeWidth)
        }

        // 箭头
        val direction = if (toX > fromX) 1f else -1f
        if (isCross) {
            // 叉号
            val cx = toX
            drawLine(color, Offset(cx - arrowSize * 0.5f, y - arrowSize * 0.5f), Offset(cx + arrowSize * 0.5f, y + arrowSize * 0.5f), strokeWidth)
            drawLine(color, Offset(cx - arrowSize * 0.5f, y + arrowSize * 0.5f), Offset(cx + arrowSize * 0.5f, y - arrowSize * 0.5f), strokeWidth)
        } else if (isOpen) {
            // 开放箭头
            drawLine(color, Offset(toX, y), Offset(toX - direction * arrowSize, y - arrowSize * 0.6f), strokeWidth)
            drawLine(color, Offset(toX, y), Offset(toX - direction * arrowSize, y + arrowSize * 0.6f), strokeWidth)
        } else {
            // 实心箭头
            val path = Path().apply {
                moveTo(toX, y)
                lineTo(toX - direction * arrowSize, y - arrowSize * 0.6f)
                lineTo(toX - direction * arrowSize, y + arrowSize * 0.6f)
                close()
            }
            drawPath(path, color, style = Fill)
        }
    }

    private fun getBlockTypeName(type: LineType): String = when (type) {
        LineType.LOOP_START -> "loop"
        LineType.ALT_START -> "alt"
        LineType.OPT_START -> "opt"
        LineType.PAR_START -> "par"
        LineType.CRITICAL_START -> "critical"
        LineType.BREAK_START -> "break"
        LineType.RECT_START -> "rect"
        else -> ""
    }
}
