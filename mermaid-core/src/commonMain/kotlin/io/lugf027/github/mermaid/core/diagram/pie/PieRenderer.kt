package io.lugf027.github.mermaid.core.diagram.pie

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.Logger
import kotlin.math.*

/**
 * 饼图渲染器（自定义渲染模式） - 对标 mermaid-js pieRenderer.ts
 *
 * 使用 d3.pie / d3.arc 的 Kotlin 等价实现计算弧形路径。
 * 渲染元素：外圈、扇区路径、百分比标签、标题、图例。
 */
class PieRenderer : DiagramRenderer {

    private val log = Logger("PieRenderer")

    companion object {
        /** 外边距 */
        private const val MARGIN = 40

        /** 图例色块大小 */
        private const val LEGEND_RECT_SIZE = 18

        /** 图例间距 */
        private const val LEGEND_SPACING = 4

        /** SVG 高度 */
        private const val HEIGHT = 450

        /** 饼图宽度（= 高度） */
        private const val PIE_WIDTH = 450
    }

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val pieDb = db as? PieDb ?: throw IllegalArgumentException("Expected PieDb")
        val sections = pieDb.getSections()
        val showData = pieDb.getShowData()
        val title = pieDb.getDiagramTitle()
        val textPosition = config.pie?.textPosition ?: 0.75

        // 计算总和
        val sum = sections.values.sum()
        if (sum <= 0) {
            log.warn("Pie chart has no data or sum is zero")
            return buildEmptySvg(diagramId, title)
        }

        // 过滤小于 1% 的扇区（用于弧计算）
        val filteredSections = sections.entries
            .map { PieSection(it.key, it.value) }
            .filter { (it.value / sum) * 100 >= 1 }

        // 计算弧形数据（类似 d3.pie）
        val arcs = computePieArcs(filteredSections)

        // 半径
        val radius = (minOf(PIE_WIDTH, HEIGHT) / 2.0) - MARGIN  // = 185

        // 颜色分配：所有原始 section key（包括被过滤的），确保颜色一致性
        val allKeys = sections.keys.toList()

        // 计算图例所需宽度
        val longestLabel = sections.keys.maxOfOrNull { labelText(it, sections[it]!!, showData).length } ?: 0
        val legendTextWidth = longestLabel * 9.0  // 估算字符宽度

        return buildSvg {
            attr("id", diagramId)
            addClass("pieDiagram")
            attr("xmlns", "http://www.w3.org/2000/svg")

            // 添加无障碍信息
            if (pieDb.getAccTitle().isNotEmpty()) {
                title(pieDb.getAccTitle())
            }
            if (pieDb.getAccDescription().isNotEmpty()) {
                desc(pieDb.getAccDescription())
            }

            // defs + style
            defs {
                style(generatePieStyles(themeVariables))
            }

            // 主绘制组 - 平移到饼图中心
            group {
                attr("transform", "translate(${PIE_WIDTH / 2},${ HEIGHT / 2})")

                // 外圈
                circle(0.0, 0.0, radius + parseStrokeWidth(themeVariables.pieOuterStrokeWidth) / 2.0) {
                    addClass("pieOuterCircle")
                }

                // 绘制扇区
                val filteredArcs = arcs.filter { arc ->
                    val percentage = ((arc.endAngle - arc.startAngle) / (2 * PI)) * 100
                    percentage.roundToInt() > 0
                }

                for (arc in filteredArcs) {
                    val colorIndex = allKeys.indexOf(arc.label)
                    val fillColor = themeVariables.getPieColor(if (colorIndex >= 0) colorIndex else 0)

                    // 扇区路径
                    val pathData = SvgPathBuilder.arc(
                        innerRadius = 0.0,
                        outerRadius = radius,
                        startAngle = arc.startAngle,
                        endAngle = arc.endAngle
                    )
                    path(pathData) {
                        addClass("pieCircle")
                        attr("fill", fillColor)
                    }
                }

                // 百分比标签
                for (arc in filteredArcs) {
                    val percentage = ((arc.endAngle - arc.startAngle) / (2 * PI)) * 100
                    val percentText = "${percentage.roundToInt()}%"

                    // 标签位置 = labelArc 的 centroid
                    val midAngle = (arc.startAngle + arc.endAngle) / 2.0 - PI / 2.0
                    val labelRadius = radius * textPosition
                    val labelX = labelRadius * cos(midAngle)
                    val labelY = labelRadius * sin(midAngle)

                    text(percentText, labelX, labelY) {
                        addClass("slice")
                        attr("text-anchor", "middle")
                        attr("dominant-baseline", "central")
                    }
                }
            }

            // 标题
            if (title.isNotEmpty()) {
                text(title, (PIE_WIDTH / 2.0), 25.0) {
                    addClass("pieTitleText")
                    attr("text-anchor", "middle")
                }
            }

            // 图例
            val legendX = PIE_WIDTH + 12.0 * LEGEND_RECT_SIZE / 2.0  // 右侧偏移
            val legendStartY = (HEIGHT - allKeys.size * (LEGEND_RECT_SIZE + LEGEND_SPACING)) / 2.0

            group {
                addClass("legend")
                attr("transform", "translate($legendX,${legendStartY})")

                for ((index, key) in allKeys.withIndex()) {
                    val yOffset = index * (LEGEND_RECT_SIZE + LEGEND_SPACING)
                    val fillColor = themeVariables.getPieColor(index)

                    group {
                        attr("transform", "translate(0,$yOffset)")

                        // 色块矩形
                        rect(0.0, 0.0, LEGEND_RECT_SIZE.toDouble(), LEGEND_RECT_SIZE.toDouble()) {
                            attr("fill", fillColor)
                            attr("stroke", fillColor)
                        }

                        // 标签文本
                        val labelStr = labelText(key, sections[key]!!, showData)
                        text(labelStr, (LEGEND_RECT_SIZE + LEGEND_SPACING).toDouble(), (LEGEND_RECT_SIZE - LEGEND_SPACING).toDouble()) {
                            addClass("legend")
                            attr("text-anchor", "start")
                        }
                    }
                }
            }

            // 设置 viewBox
            val totalWidth = legendX + legendTextWidth + MARGIN
            viewBox(0.0, 0.0, totalWidth, HEIGHT.toDouble())
            attr("width", "${totalWidth}")
            attr("height", "${HEIGHT}")
        }
    }

    /**
     * 计算饼图弧形数据 - 等效于 d3.pie()
     *
     * 返回每个扇区的起始/结束角度（弧度），不排序保持原始顺序。
     * d3 约定：12 点方向为 0，顺时针方向。
     */
    private fun computePieArcs(sections: List<PieSection>): List<PieArc> {
        val total = sections.sumOf { it.value }
        if (total <= 0) return emptyList()

        val arcs = mutableListOf<PieArc>()
        var currentAngle = 0.0

        for (section in sections) {
            val startAngle = currentAngle
            val endAngle = currentAngle + (section.value / total) * 2 * PI
            arcs.add(PieArc(section.label, section.value, startAngle, endAngle))
            currentAngle = endAngle
        }

        return arcs
    }

    /** 生成图例标签文本 */
    private fun labelText(label: String, value: Double, showData: Boolean): String {
        return if (showData) {
            val valueStr = if (value == value.toLong().toDouble()) {
                value.toLong().toString()
            } else {
                value.toString()
            }
            "$label [$valueStr]"
        } else {
            label
        }
    }

    /** 生成饼图 CSS 样式 - 对标 pieStyles.ts */
    private fun generatePieStyles(tv: ThemeVariables): String {
        return """
.pieCircle {
  stroke: ${tv.pieStrokeColor};
  stroke-width: ${tv.pieStrokeWidth};
  opacity: ${tv.pieOpacity};
}
.pieOuterCircle {
  stroke: ${tv.pieOuterStrokeColor};
  stroke-width: ${tv.pieOuterStrokeWidth};
  fill: none;
}
.pieTitleText {
  text-anchor: middle;
  font-size: ${tv.pieTitleTextSize};
  fill: ${tv.pieTitleTextColor};
  font-family: ${tv.fontFamily};
}
.slice {
  font-family: ${tv.fontFamily};
  fill: ${tv.pieSectionTextColor};
  font-size: ${tv.pieSectionTextSize};
}
.legend text {
  fill: ${tv.pieLegendTextColor};
  font-family: ${tv.fontFamily};
  font-size: ${tv.pieLegendTextSize};
}
""".trimIndent()
    }

    /** 解析 stroke-width 字符串为 Double */
    private fun parseStrokeWidth(width: String): Double {
        return width.replace("px", "").toDoubleOrNull() ?: 2.0
    }

    /** 构建空饼图 SVG */
    private fun buildEmptySvg(diagramId: String, title: String): SvgRoot {
        return buildSvg {
            attr("id", diagramId)
            addClass("pieDiagram")
            attr("xmlns", "http://www.w3.org/2000/svg")
            viewBox(0.0, 0.0, PIE_WIDTH.toDouble(), HEIGHT.toDouble())
            attr("width", "$PIE_WIDTH")
            attr("height", "$HEIGHT")

            if (title.isNotEmpty()) {
                text(title, (PIE_WIDTH / 2.0), (HEIGHT / 2.0)) {
                    addClass("pieTitleText")
                    attr("text-anchor", "middle")
                }
            }
        }
    }
}

/** 饼图扇区数据 */
internal data class PieSection(
    val label: String,
    val value: Double
)

/** 饼图弧形数据 - 对标 d3.PieArcDatum */
internal data class PieArc(
    val label: String,
    val value: Double,
    val startAngle: Double,
    val endAngle: Double
)
