package io.lugf027.github.mermaid.core.diagrams.gantt

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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

/**
 * 甘特图渲染器 — 自定义时间轴布局。
 */
class GanttRenderer : DiagramRenderer {

    companion object {
        private const val MARGIN_LEFT = 150f
        private const val MARGIN_TOP = 60f
        private const val BAR_HEIGHT = 24f
        private const val BAR_GAP = 8f
        private const val SECTION_GAP = 16f
        private const val BAR_AREA_WIDTH = 500f
        private const val FONT_SIZE = 12f
    }

    override fun draw(
        drawScope: DrawScope,
        db: DiagramDB,
        config: MermaidConfig,
        theme: ThemeVariables,
        textMeasurer: TextMeasurer,
        size: Size,
    ) {
        with(drawScope) {
        val ganttDb = db as? GanttDb ?: return
        val sections = ganttDb.getSections()
        val tasks = ganttDb.getTasks()

        val textColor = theme.primaryTextColor.toComposeColor()
        val lineColor = theme.lineColor.toComposeColor()
        val taskStyle = TextStyle(fontSize = FONT_SIZE.sp, color = textColor)
        val sectionStyle = TextStyle(fontSize = (FONT_SIZE + 1).sp, color = textColor)

        // 标题
        val title = ganttDb.getDiagramTitle()
        if (title.isNotEmpty()) {
            val titleResult = textMeasurer.measure(title, TextStyle(fontSize = 18.sp, color = textColor))
            drawText(titleResult, topLeft = Offset((size.width - titleResult.size.width) / 2, 8f))
        }

        // 计算任务总数
        val totalTasks = tasks.size
        if (totalTasks == 0) return

        // 简化：把所有任务平均分配条宽
        val barWidth = BAR_AREA_WIDTH / maxOf(totalTasks, 1)

        var y = MARGIN_TOP
        val colors = listOf(
            theme.primaryColor.toComposeColor(),
            theme.secondaryColor.toComposeColor(),
            theme.tertiaryColor.toComposeColor(),
        )
        var taskIdx = 0

        for ((sIdx, section) in sections.withIndex()) {
            // Section 名
            val secResult = textMeasurer.measure(section.name, sectionStyle)
            drawText(secResult, topLeft = Offset(8f, y + 2f))

            // Section 背景条纹
            val sectionHeight = section.tasks.size * (BAR_HEIGHT + BAR_GAP)
            if (sIdx % 2 == 0) {
                drawRect(
                    color = Color(0x0A000000),
                    topLeft = Offset(MARGIN_LEFT, y),
                    size = Size(BAR_AREA_WIDTH, sectionHeight),
                )
            }

            for (task in section.tasks) {
                val barColor = when {
                    task.isCrit -> Color(0xFFFF3333)
                    task.isDone -> Color(0xFFAAAAAA)
                    task.isActive -> colors[sIdx % colors.size]
                    else -> colors[sIdx % colors.size].copy(alpha = 0.7f)
                }

                // 简化布局：每个任务占一行，条的宽度按索引递增
                val barX = MARGIN_LEFT + taskIdx * barWidth * 0.3f
                val bw = barWidth * 2f

                if (task.isMilestone) {
                    // 菱形标记
                    val cx = barX + bw / 2
                    val cy = y + BAR_HEIGHT / 2
                    val r = BAR_HEIGHT / 2.5f
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(cx, cy - r)
                        lineTo(cx + r, cy)
                        lineTo(cx, cy + r)
                        lineTo(cx - r, cy)
                        close()
                    }
                    drawPath(path, barColor)
                } else {
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(barX, y),
                        size = Size(bw, BAR_HEIGHT),
                        cornerRadius = CornerRadius(3f, 3f),
                    )
                }

                // 任务名
                val taskResult = textMeasurer.measure(task.task, taskStyle)
                drawText(
                    taskResult,
                    topLeft = Offset(barX + bw + 8f, y + (BAR_HEIGHT - taskResult.size.height) / 2),
                )

                y += BAR_HEIGHT + BAR_GAP
                taskIdx++
            }

            y += SECTION_GAP
        }

        // 如果没有 sections，按 tasks 平铺
        if (sections.isEmpty()) {
            for (task in tasks) {
                val barColor = colors[taskIdx % colors.size]
                val barX = MARGIN_LEFT
                val bw = BAR_AREA_WIDTH * 0.6f

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(barX, y),
                    size = Size(bw, BAR_HEIGHT),
                    cornerRadius = CornerRadius(3f, 3f),
                )
                val taskResult = textMeasurer.measure(task.task, taskStyle)
                drawText(
                    taskResult,
                    topLeft = Offset(8f, y + (BAR_HEIGHT - taskResult.size.height) / 2),
                )
                y += BAR_HEIGHT + BAR_GAP
                taskIdx++
            }
        }
        }
    }
}
