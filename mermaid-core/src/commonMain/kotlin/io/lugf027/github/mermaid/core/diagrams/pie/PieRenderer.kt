package io.lugf027.github.mermaid.core.diagrams.pie

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import io.lugf027.github.mermaid.core.themes.getPieColors
import io.lugf027.github.mermaid.core.types.DiagramDB
import io.lugf027.github.mermaid.core.types.DiagramRenderer
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 饼图 Compose Canvas 渲染器。
 * 像素级还原 mermaid-js 的 pie 图绘制效果。
 *
 * 布局参数（与 mermaid-js 一致）：
 * - 画布高度: 450
 * - 饼图区域: 450 × 450
 * - 边距: 40
 * - 半径: 185 (= 450/2 - 40)
 * - 标签位置: 半径 × textPosition (默认 0.75)
 * - 图例矩形: 18×18, 间距 4
 * - 图例水平偏移: 12 × 18 = 216 (圆心右侧)
 */
class PieRenderer : DiagramRenderer {

    companion object {
        private const val MARGIN = 40f
        private const val PIE_HEIGHT = 450f
        private const val LEGEND_RECT_SIZE = 18f
        private const val LEGEND_SPACING = 4f
        private const val TWO_PI = (2 * kotlin.math.PI).toFloat()
        /** 角度 → 弧度 */
        private fun toRadians(deg: Double): Double = deg * kotlin.math.PI / 180.0
    }

    override fun draw(
        drawScope: DrawScope,
        db: DiagramDB,
        config: MermaidConfig,
        theme: ThemeVariables,
        textMeasurer: TextMeasurer,
        size: Size,
    ) {
        val pieDb = db as? PieDb ?: return
        val sections = pieDb.getSections()
        if (sections.isEmpty()) return

        val sum = sections.values.sum()
        if (sum <= 0) return

        // 获取主题颜色
        val pieColors = theme.getPieColors().map { it.toComposeColor() }
        val strokeColor = theme.pieStrokeColor.toComposeColor()
        val strokeWidth = 2f
        val opacity = 0.7f
        val textPosition = pieDb.textPosition

        // 计算尺寸（适应画布）
        val scale = min(size.width / 700f, size.height / PIE_HEIGHT).coerceAtLeast(0.5f)
        val pieSize = PIE_HEIGHT * scale
        val radius = (pieSize / 2f - MARGIN * scale)
        val centerX = pieSize / 2f
        val centerY = pieSize / 2f

        with(drawScope) {
            // ─── 1. 绘制标题 ────────────────────────────────
            val title = pieDb.getDiagramTitle()
            if (title.isNotEmpty()) {
                val titleStyle = TextStyle(
                    fontSize = (25f * scale).sp,
                    color = theme.pieTitleTextColor.toComposeColor(),
                )
                val titleResult = textMeasurer.measure(title, titleStyle)
                drawText(
                    textLayoutResult = titleResult,
                    topLeft = Offset(
                        centerX - titleResult.size.width / 2f,
                        centerY - (PIE_HEIGHT * scale - 50f * scale) / 2f - titleResult.size.height,
                    ),
                )
            }

            // ─── 2. 绘制外圆环 ──────────────────────────────
            drawCircle(
                color = theme.pieOuterStrokeColor.toComposeColor(),
                radius = radius + strokeWidth / 2f,
                center = Offset(centerX, centerY),
                style = Stroke(width = strokeWidth),
            )

            // ─── 3. 过滤数据（< 1% 的不绘制） ──────────────
            val allEntries = sections.entries.toList()
            val colorMap = mutableMapOf<String, Color>()
            allEntries.forEachIndexed { index, entry ->
                colorMap[entry.key] = pieColors[index % pieColors.size]
            }

            val filteredEntries = allEntries.filter { entry ->
                val percentage = (entry.value / sum * 100)
                percentage.roundToInt() > 0
            }

            // ─── 4. 绘制扇形 ────────────────────────────────
            var startAngle = -90f // 从顶部开始（-90° = 12点钟方向）
            val arcData = filteredEntries.map { entry ->
                val sweepAngle = (entry.value / sum * 360f).toFloat()
                val data = ArcData(entry.key, entry.value, startAngle, sweepAngle)
                startAngle += sweepAngle
                data
            }

            for (arc in arcData) {
                val color = colorMap[arc.label] ?: pieColors[0]
                val arcColor = color.copy(alpha = opacity)

                // 绘制扇形
                val path = createArcPath(centerX, centerY, radius, arc.startAngle, arc.sweepAngle)
                drawPath(path, arcColor, style = Fill)
                drawPath(path, strokeColor, style = Stroke(width = strokeWidth))
            }

            // ─── 5. 绘制百分比标签 ──────────────────────────
            val sectionTextStyle = TextStyle(
                fontSize = (17f * scale).sp,
                color = theme.pieSectionTextColor.toComposeColor(),
            )

            for (arc in arcData) {
                val percentage = (arc.value / sum * 100).roundToInt()
                if (percentage == 0) continue

                val labelText = "$percentage%"
                val midAngle = arc.startAngle + arc.sweepAngle / 2f
                val labelRadius = radius * textPosition
                val labelX = centerX + labelRadius * cos(toRadians(midAngle.toDouble())).toFloat()
                val labelY = centerY + labelRadius * sin(toRadians(midAngle.toDouble())).toFloat()

                val result = textMeasurer.measure(labelText, sectionTextStyle)
                drawText(
                    textLayoutResult = result,
                    topLeft = Offset(
                        labelX - result.size.width / 2f,
                        labelY - result.size.height / 2f,
                    ),
                )
            }

            // ─── 6. 绘制图例 ────────────────────────────────
            val legendX = centerX + 216f * scale // 12 × LEGEND_RECT_SIZE
            val legendItemHeight = (LEGEND_RECT_SIZE + LEGEND_SPACING) * scale
            val totalLegendHeight = legendItemHeight * allEntries.size
            val legendStartY = centerY - totalLegendHeight / 2f

            val legendTextStyle = TextStyle(
                fontSize = (17f * scale).sp,
                color = theme.pieLegendTextColor.toComposeColor(),
            )

            allEntries.forEachIndexed { index, entry ->
                val itemY = legendStartY + index * legendItemHeight
                val rectSize = LEGEND_RECT_SIZE * scale
                val color = colorMap[entry.key] ?: pieColors[0]

                // 色块
                drawRect(
                    color = color,
                    topLeft = Offset(legendX, itemY),
                    size = Size(rectSize, rectSize),
                )
                drawRect(
                    color = color,
                    topLeft = Offset(legendX, itemY),
                    size = Size(rectSize, rectSize),
                    style = Stroke(width = 1f),
                )

                // 标签文字
                val labelText = if (pieDb.getShowData()) {
                    "${entry.key} [${entry.value}]"
                } else {
                    entry.key
                }

                val textResult = textMeasurer.measure(labelText, legendTextStyle)
                drawText(
                    textLayoutResult = textResult,
                    topLeft = Offset(
                        legendX + rectSize + LEGEND_SPACING * scale,
                        itemY + (rectSize - textResult.size.height) / 2f,
                    ),
                )
            }
        }
    }

    /**
     * 创建扇形 Path。
     */
    private fun createArcPath(
        cx: Float, cy: Float, radius: Float,
        startAngleDeg: Float, sweepAngleDeg: Float,
    ): Path {
        return Path().apply {
            moveTo(cx, cy)
            val startRad = toRadians(startAngleDeg.toDouble())
            val x1 = cx + radius * cos(startRad).toFloat()
            val y1 = cy + radius * sin(startRad).toFloat()
            lineTo(x1, y1)

            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    cx - radius, cy - radius,
                    cx + radius, cy + radius,
                ),
                startAngleDegrees = startAngleDeg,
                sweepAngleDegrees = sweepAngleDeg,
                forceMoveTo = false,
            )
            close()
        }
    }

    private data class ArcData(
        val label: String,
        val value: Double,
        val startAngle: Float,
        val sweepAngle: Float,
    )
}
