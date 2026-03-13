package io.lugf027.github.mermaid.core.diagram.quadrantChart

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.rendering.svg.buildSvg
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.TextUtils
import kotlin.math.max

/**
 * 象限图渲染器 - 对标 mermaid-js quadrantRenderer.ts
 *
 * 绘制四象限区域 + 十字分割线 + 数据点 + 轴标签 + 标题
 */
class QuadrantRenderer : DiagramRenderer {

    companion object {
        const val CHART_WIDTH = 500.0
        const val CHART_HEIGHT = 500.0
        const val TITLE_FONT_SIZE = 20
        const val TITLE_PADDING = 10.0
        const val QUADRANT_PADDING = 5.0
        const val AXIS_LABEL_FONT_SIZE = 16
        const val AXIS_LABEL_PADDING = 5.0
        const val QUADRANT_LABEL_FONT_SIZE = 16
        const val POINT_RADIUS = 5.0
        const val POINT_LABEL_FONT_SIZE = 12
        const val POINT_TEXT_PADDING = 5.0
    }

    // 象限颜色
    private val quadrantFills = listOf("#f0f0ff", "#e8ffe8", "#ffe8e8", "#fff0e8")

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val qdb = db as QuadrantDb
        val title = qdb.getDiagramTitle()
        val points = qdb.getPoints()

        val chartW = config.quadrantChart?.chartWidth?.toDouble() ?: CHART_WIDTH
        val chartH = config.quadrantChart?.chartHeight?.toDouble() ?: CHART_HEIGHT

        // 计算空间分配
        val titleH = if (title.isNotEmpty()) TITLE_FONT_SIZE + TITLE_PADDING * 2 else 0.0
        val xAxisH = if (qdb.xAxisLeftText.isNotEmpty() || qdb.xAxisRightText.isNotEmpty())
            AXIS_LABEL_FONT_SIZE + AXIS_LABEL_PADDING * 2 else 0.0
        val yAxisW = if (qdb.yAxisBottomText.isNotEmpty() || qdb.yAxisTopText.isNotEmpty())
            AXIS_LABEL_FONT_SIZE + AXIS_LABEL_PADDING * 2 else 0.0

        val quadrantTop = titleH
        val quadrantLeft = yAxisW
        val quadrantW = chartW - yAxisW
        val quadrantH = chartH - titleH - xAxisH
        val halfW = quadrantW / 2
        val halfH = quadrantH / 2

        return buildSvg {
            attr("id", diagramId)
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("xmlns:xlink", "http://www.w3.org/1999/xlink")

            group {
                addClass("main")

                // 1. 四个象限背景
                group {
                    addClass("quadrants")
                    // Q2（左上）
                    rect(quadrantLeft, quadrantTop, halfW, halfH) {
                        attr("fill", quadrantFills[1])
                        attr("stroke", "none")
                    }
                    // Q1（右上）
                    rect(quadrantLeft + halfW, quadrantTop, halfW, halfH) {
                        attr("fill", quadrantFills[0])
                        attr("stroke", "none")
                    }
                    // Q3（左下）
                    rect(quadrantLeft, quadrantTop + halfH, halfW, halfH) {
                        attr("fill", quadrantFills[2])
                        attr("stroke", "none")
                    }
                    // Q4（右下）
                    rect(quadrantLeft + halfW, quadrantTop + halfH, halfW, halfH) {
                        attr("fill", quadrantFills[3])
                        attr("stroke", "none")
                    }

                    // 象限文字标签
                    if (qdb.quadrant2Text.isNotEmpty()) {
                        text(qdb.quadrant2Text, quadrantLeft + halfW / 2, quadrantTop + QUADRANT_LABEL_FONT_SIZE + QUADRANT_PADDING) {
                            attr("text-anchor", "middle")
                            attr("font-size", "$QUADRANT_LABEL_FONT_SIZE")
                            attr("fill", themeVariables.textColor)
                        }
                    }
                    if (qdb.quadrant1Text.isNotEmpty()) {
                        text(qdb.quadrant1Text, quadrantLeft + halfW + halfW / 2, quadrantTop + QUADRANT_LABEL_FONT_SIZE + QUADRANT_PADDING) {
                            attr("text-anchor", "middle")
                            attr("font-size", "$QUADRANT_LABEL_FONT_SIZE")
                            attr("fill", themeVariables.textColor)
                        }
                    }
                    if (qdb.quadrant3Text.isNotEmpty()) {
                        text(qdb.quadrant3Text, quadrantLeft + halfW / 2, quadrantTop + halfH + QUADRANT_LABEL_FONT_SIZE + QUADRANT_PADDING) {
                            attr("text-anchor", "middle")
                            attr("font-size", "$QUADRANT_LABEL_FONT_SIZE")
                            attr("fill", themeVariables.textColor)
                        }
                    }
                    if (qdb.quadrant4Text.isNotEmpty()) {
                        text(qdb.quadrant4Text, quadrantLeft + halfW + halfW / 2, quadrantTop + halfH + QUADRANT_LABEL_FONT_SIZE + QUADRANT_PADDING) {
                            attr("text-anchor", "middle")
                            attr("font-size", "$QUADRANT_LABEL_FONT_SIZE")
                            attr("fill", themeVariables.textColor)
                        }
                    }
                }

                // 2. 边框和十字线
                group {
                    addClass("border")
                    val strokeColor = themeVariables.lineColor
                    // 外框
                    rect(quadrantLeft, quadrantTop, quadrantW, quadrantH) {
                        attr("fill", "none")
                        attr("stroke", strokeColor)
                        attr("stroke-width", "2")
                    }
                    // 水平中线
                    line(quadrantLeft, quadrantTop + halfH, quadrantLeft + quadrantW, quadrantTop + halfH) {
                        attr("stroke", strokeColor)
                        attr("stroke-width", "1")
                    }
                    // 垂直中线
                    line(quadrantLeft + halfW, quadrantTop, quadrantLeft + halfW, quadrantTop + quadrantH) {
                        attr("stroke", strokeColor)
                        attr("stroke-width", "1")
                    }
                }

                // 3. 数据点
                if (points.isNotEmpty()) {
                    group {
                        addClass("data-points")
                        for (pt in points) {
                            val px = quadrantLeft + pt.x * quadrantW
                            val py = quadrantTop + (1.0 - pt.y) * quadrantH  // Y 轴反转
                            val r = pt.radius
                            val fill = pt.color.ifEmpty { themeVariables.primaryColor }

                            circle(px, py, r) {
                                addClass("point")
                                attr("fill", fill)
                                if (pt.strokeColor.isNotEmpty()) attr("stroke", pt.strokeColor)
                                if (pt.strokeWidth.isNotEmpty()) attr("stroke-width", pt.strokeWidth)
                            }
                            text(pt.text, px, py - r - POINT_TEXT_PADDING) {
                                addClass("point-label")
                                attr("text-anchor", "middle")
                                attr("font-size", "$POINT_LABEL_FONT_SIZE")
                                attr("fill", themeVariables.textColor)
                            }
                        }
                    }
                }

                // 4. 轴标签
                group {
                    addClass("labels")
                    if (qdb.xAxisLeftText.isNotEmpty()) {
                        text(qdb.xAxisLeftText, quadrantLeft, chartH - AXIS_LABEL_PADDING) {
                            attr("text-anchor", "start")
                            attr("font-size", "$AXIS_LABEL_FONT_SIZE")
                            attr("fill", themeVariables.textColor)
                        }
                    }
                    if (qdb.xAxisRightText.isNotEmpty()) {
                        text(qdb.xAxisRightText, chartW, chartH - AXIS_LABEL_PADDING) {
                            attr("text-anchor", "end")
                            attr("font-size", "$AXIS_LABEL_FONT_SIZE")
                            attr("fill", themeVariables.textColor)
                        }
                    }
                    if (qdb.yAxisBottomText.isNotEmpty()) {
                        text(qdb.yAxisBottomText, AXIS_LABEL_PADDING, quadrantTop + quadrantH) {
                            attr("text-anchor", "start")
                            attr("font-size", "$AXIS_LABEL_FONT_SIZE")
                            attr("fill", themeVariables.textColor)
                            attr("transform", "rotate(-90, ${AXIS_LABEL_PADDING}, ${quadrantTop + quadrantH})")
                        }
                    }
                    if (qdb.yAxisTopText.isNotEmpty()) {
                        text(qdb.yAxisTopText, AXIS_LABEL_PADDING, quadrantTop) {
                            attr("text-anchor", "start")
                            attr("font-size", "$AXIS_LABEL_FONT_SIZE")
                            attr("fill", themeVariables.textColor)
                            attr("transform", "rotate(-90, ${AXIS_LABEL_PADDING}, $quadrantTop)")
                        }
                    }
                }

                // 5. 标题
                if (title.isNotEmpty()) {
                    group {
                        addClass("title")
                        text(title, chartW / 2, TITLE_PADDING + TITLE_FONT_SIZE) {
                            attr("text-anchor", "middle")
                            attr("font-size", "$TITLE_FONT_SIZE")
                            attr("fill", themeVariables.textColor)
                        }
                    }
                }
            }

            viewBox(0.0, 0.0, chartW, chartH)
            attr("width", "100%")
            attr("style", "max-width: ${chartW.toInt()}px;")
        }
    }
}
