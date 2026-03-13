package io.lugf027.github.mermaid.core.diagram.kanban

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.rendering.svg.buildSvg
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.TextUtils
import kotlin.math.max

/**
 * 看板图渲染器 - 对标 mermaid-js kanbanRenderer.ts
 *
 * 列式布局：每个 section 是一列，列内 item 竖直堆叠。
 */
class KanbanRenderer : DiagramRenderer {

    companion object {
        const val COLUMN_WIDTH = 200.0
        const val COLUMN_PADDING = 10.0
        const val ITEM_HEIGHT = 40.0
        const val ITEM_MARGIN = 8.0
        const val HEADER_HEIGHT = 40.0
        const val HEADER_FONT_SIZE = 14
        const val ITEM_FONT_SIZE = 12
        const val MARGIN = 20.0
        const val CORNER_RADIUS = 5.0
    }

    private val columnColors = listOf(
        "#e8f4fd", "#fdf2e8", "#e8fde8", "#fde8f4",
        "#f4e8fd", "#fdfde8", "#e8fdfd", "#fde8e8"
    )

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val kdb = db as KanbanDb
        val sections = kdb.getSections()
        val title = kdb.getDiagramTitle()

        val kanbanConfig = config.kanban
        val colWidth = (kanbanConfig?.sectionWidth ?: 200).toDouble()
        val padding = (kanbanConfig?.padding ?: 8).toDouble()

        val titleH = if (title.isNotEmpty()) 40.0 else 0.0

        // 计算每列高度
        val columnHeights = mutableListOf<Double>()
        for (section in sections) {
            val items = kdb.getItemsBySection(section.id)
            val h = HEADER_HEIGHT + items.size * (ITEM_HEIGHT + ITEM_MARGIN) + padding * 2
            columnHeights.add(h)
        }

        val maxHeight = columnHeights.maxOrNull() ?: 100.0
        val svgW = MARGIN * 2 + sections.size * (colWidth + COLUMN_PADDING) - COLUMN_PADDING
        val svgH = MARGIN + titleH + maxHeight + MARGIN

        return buildSvg {
            attr("id", diagramId)
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("xmlns:xlink", "http://www.w3.org/1999/xlink")

            group {
                addClass("kanban")

                // 标题
                if (title.isNotEmpty()) {
                    text(title, svgW / 2, MARGIN + 20) {
                        attr("text-anchor", "middle")
                        attr("font-size", "16")
                        attr("font-weight", "bold")
                        attr("fill", themeVariables.textColor)
                    }
                }

                // 列
                for ((colIdx, section) in sections.withIndex()) {
                    val x = MARGIN + colIdx * (colWidth + COLUMN_PADDING)
                    val y = MARGIN + titleH
                    val items = kdb.getItemsBySection(section.id)
                    val colH = HEADER_HEIGHT + items.size * (ITEM_HEIGHT + ITEM_MARGIN) + padding * 2
                    val bgColor = columnColors[colIdx % columnColors.size]

                    group {
                        addClass("kanban-column")

                        // 列背景
                        rect(x, y, colWidth, colH) {
                            attr("fill", bgColor)
                            attr("stroke", themeVariables.lineColor)
                            attr("stroke-width", "1")
                            attr("rx", "5")
                            attr("ry", "5")
                        }

                        // 列标题
                        text(section.label, x + colWidth / 2, y + HEADER_HEIGHT / 2 + 5) {
                            addClass("kanban-section-title")
                            attr("text-anchor", "middle")
                            attr("font-size", "$HEADER_FONT_SIZE")
                            attr("font-weight", "bold")
                            attr("fill", themeVariables.textColor)
                        }

                        // 标题下划线
                        line(x + padding, y + HEADER_HEIGHT, x + colWidth - padding, y + HEADER_HEIGHT) {
                            attr("stroke", themeVariables.lineColor)
                            attr("stroke-width", "0.5")
                        }

                        // 卡片
                        for ((itemIdx, item) in items.withIndex()) {
                            val cardX = x + padding
                            val cardY = y + HEADER_HEIGHT + ITEM_MARGIN + itemIdx * (ITEM_HEIGHT + ITEM_MARGIN)
                            val cardW = colWidth - padding * 2

                            group {
                                addClass("kanban-item")

                                // 卡片背景
                                rect(cardX, cardY, cardW, ITEM_HEIGHT) {
                                    attr("fill", "#ffffff")
                                    attr("stroke", themeVariables.lineColor)
                                    attr("stroke-width", "0.5")
                                    attr("rx", "$CORNER_RADIUS")
                                    attr("ry", "$CORNER_RADIUS")
                                }

                                // 优先级标记
                                if (item.priority.isNotEmpty()) {
                                    val color = when (item.priority.lowercase()) {
                                        "high" -> "#ff4444"
                                        "medium" -> "#ffaa44"
                                        "low" -> "#44aa44"
                                        else -> themeVariables.lineColor
                                    }
                                    rect(cardX, cardY, 3.0, ITEM_HEIGHT) {
                                        attr("fill", color)
                                        attr("rx", "$CORNER_RADIUS")
                                        attr("ry", "$CORNER_RADIUS")
                                    }
                                }

                                // 卡片文字
                                text(item.label, cardX + padding + 3, cardY + ITEM_HEIGHT / 2 + 4) {
                                    addClass("kanban-item-label")
                                    attr("font-size", "$ITEM_FONT_SIZE")
                                    attr("fill", themeVariables.textColor)
                                }

                                // 分配人
                                if (item.assigned.isNotEmpty()) {
                                    text(item.assigned, cardX + cardW - padding, cardY + ITEM_HEIGHT - 8) {
                                        attr("text-anchor", "end")
                                        attr("font-size", "10")
                                        attr("fill", themeVariables.textColor)
                                        attr("opacity", "0.6")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            viewBox(0.0, 0.0, svgW, svgH)
            attr("width", "100%")
            attr("style", "max-width: ${svgW.toInt()}px;")
        }
    }
}
