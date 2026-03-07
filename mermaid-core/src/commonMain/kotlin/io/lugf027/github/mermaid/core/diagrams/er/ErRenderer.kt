package io.lugf027.github.mermaid.core.diagrams.er

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

/**
 * ER 图 Compose Canvas 渲染器。
 * 使用简化的网格布局排列实体，然后绘制关系连线。
 */
class ErRenderer : DiagramRenderer {

    companion object {
        private const val ENTITY_WIDTH = 200f
        private const val ENTITY_HEADER_HEIGHT = 36f
        private const val ATTR_ROW_HEIGHT = 24f
        private const val ENTITY_PADDING = 8f
        private const val MARGIN = 40f
        private const val ENTITY_GAP_X = 60f
        private const val ENTITY_GAP_Y = 40f
        private const val FONT_SIZE = 13f
        private const val COLS_PER_ROW = 3
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
        val erDb = db as? ErDb ?: return
        val entities = erDb.getEntities()
        val relationships = erDb.getRelationships()

        val bgColor = theme.background.toComposeColor()
        val primaryColor = theme.primaryColor.toComposeColor()
        val borderColor = theme.primaryBorderColor.toComposeColor()
        val textColor = theme.primaryTextColor.toComposeColor()
        val lineColor = theme.lineColor.toComposeColor()

        // 计算每个实体的尺寸和位置（网格布局）
        data class EntityLayout(val id: String, val x: Float, val y: Float, val w: Float, val h: Float)

        val layouts = mutableMapOf<String, EntityLayout>()
        var col = 0
        var row = 0
        var maxRowHeight = 0f

        for ((id, entity) in entities) {
            val attrCount = entity.attributes.size
            val h = ENTITY_HEADER_HEIGHT + attrCount * ATTR_ROW_HEIGHT + ENTITY_PADDING * 2
            val x = MARGIN + col * (ENTITY_WIDTH + ENTITY_GAP_X)
            val y = MARGIN + row * (maxRowHeight + ENTITY_GAP_Y)
            layouts[id] = EntityLayout(id, x, y, ENTITY_WIDTH, h)
            maxRowHeight = maxOf(maxRowHeight, h)
            col++
            if (col >= COLS_PER_ROW) {
                col = 0
                row++
                maxRowHeight = 0f
            }
        }

        // 重新计算 y 坐标（由于 maxRowHeight 不同行可能不一样）
        // 简化处理：使用固定行高
        val maxH = layouts.values.maxOfOrNull { it.h } ?: 100f
        val fixedLayouts = mutableMapOf<String, EntityLayout>()
        for ((id, layout) in layouts) {
            val r = entities.keys.indexOf(id) / COLS_PER_ROW
            val c = entities.keys.indexOf(id) % COLS_PER_ROW
            fixedLayouts[id] = layout.copy(
                x = MARGIN + c * (ENTITY_WIDTH + ENTITY_GAP_X),
                y = MARGIN + r * (maxH + ENTITY_GAP_Y),
            )
        }

        // 绘制实体
        val headerStyle = TextStyle(fontSize = (FONT_SIZE + 1).sp, color = Color.White)
        val attrStyle = TextStyle(fontSize = FONT_SIZE.sp, color = textColor)

        for ((id, entity) in entities) {
            val layout = fixedLayouts[id] ?: continue
            // 背景
            drawRect(
                color = bgColor,
                topLeft = Offset(layout.x, layout.y),
                size = Size(layout.w, layout.h),
            )
            // 边框
            drawRect(
                color = borderColor,
                topLeft = Offset(layout.x, layout.y),
                size = Size(layout.w, layout.h),
                style = Stroke(width = 2f),
            )
            // 头部背景
            drawRect(
                color = primaryColor,
                topLeft = Offset(layout.x, layout.y),
                size = Size(layout.w, ENTITY_HEADER_HEIGHT),
            )
            // 实体名
            val nameResult = textMeasurer.measure(entity.label, headerStyle)
            drawText(
                nameResult,
                topLeft = Offset(
                    layout.x + (layout.w - nameResult.size.width) / 2,
                    layout.y + (ENTITY_HEADER_HEIGHT - nameResult.size.height) / 2,
                ),
            )
            // 属性
            entity.attributes.forEachIndexed { idx, attr ->
                val attrText = buildString {
                    append(attr.type)
                    append(" ")
                    append(attr.name)
                    if (attr.keys.isNotEmpty()) {
                        append(" ")
                        append(attr.keys.joinToString(","))
                    }
                }
                val result = textMeasurer.measure(attrText, attrStyle)
                drawText(
                    result,
                    topLeft = Offset(
                        layout.x + ENTITY_PADDING,
                        layout.y + ENTITY_HEADER_HEIGHT + ENTITY_PADDING + idx * ATTR_ROW_HEIGHT,
                    ),
                )
            }
        }

        // 绘制关系连线
        for (rel in relationships) {
            val layoutA = fixedLayouts[rel.entityA] ?: continue
            val layoutB = fixedLayouts[rel.entityB] ?: continue
            val startX = layoutA.x + layoutA.w / 2
            val startY = layoutA.y + layoutA.h / 2
            val endX = layoutB.x + layoutB.w / 2
            val endY = layoutB.y + layoutB.h / 2

            drawLine(
                color = lineColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = if (rel.identification == Identification.IDENTIFYING) 2f else 1.5f,
            )
            // 关系标签
            if (rel.roleLabel.isNotEmpty()) {
                val midX = (startX + endX) / 2
                val midY = (startY + endY) / 2
                val labelResult = textMeasurer.measure(rel.roleLabel, attrStyle)
                drawText(
                    labelResult,
                    topLeft = Offset(midX - labelResult.size.width / 2, midY - labelResult.size.height - 4f),
                )
            }
        }

        // 标题
        val title = erDb.getDiagramTitle()
        if (title.isNotEmpty()) {
            val titleStyle = TextStyle(fontSize = 18.sp, color = textColor)
            val titleResult = textMeasurer.measure(title, titleStyle)
            drawText(titleResult, topLeft = Offset((size.width - titleResult.size.width) / 2, 8f))
        }
        }
    }
}
