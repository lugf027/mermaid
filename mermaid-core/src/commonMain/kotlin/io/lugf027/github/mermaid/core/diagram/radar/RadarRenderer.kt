package io.lugf027.github.mermaid.core.diagram.radar

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.rendering.svg.buildSvg
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.TextUtils
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.max

/**
 * 雷达图渲染器 - 对标 mermaid-js radarRenderer.ts
 *
 * 绘制：
 * 1. 同心多边形网格（levels）
 * 2. 从中心向外的轴线
 * 3. 每个数据集的填充多边形
 * 4. 轴标签
 * 5. 图例
 */
class RadarRenderer : DiagramRenderer {

    companion object {
        const val LEVELS = 5  // 网格层数
        const val TITLE_FONT_SIZE = 18
        const val LABEL_FONT_SIZE = 12
        const val LEGEND_FONT_SIZE = 12
        const val LEGEND_RECT_SIZE = 12.0
        const val LEGEND_SPACING = 20.0
        const val DOT_RADIUS = 4.0
    }

    private val datasetColors = listOf(
        "#4e79a7", "#f28e2c", "#e15759", "#76b7b2",
        "#59a14f", "#edc949", "#af7aa1", "#ff9da7"
    )

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val rdb = db as RadarDb
        val axes = rdb.getAxes()
        val datasets = rdb.getDatasets()
        val title = rdb.getDiagramTitle()
        val maxValue = rdb.getEffectiveMaxValue()
        val showLegend = rdb.getShowLegend()

        val radarConfig = config.radar
        val chartWidth = (radarConfig?.width ?: 600).toDouble()
        val chartHeight = (radarConfig?.height ?: 600).toDouble()
        val marginTop = (radarConfig?.marginTop ?: 50).toDouble()
        val marginRight = (radarConfig?.marginRight ?: 50).toDouble()
        val marginBottom = (radarConfig?.marginBottom ?: 50).toDouble()
        val marginLeft = (radarConfig?.marginLeft ?: 50).toDouble()
        val axisLabelFactor = radarConfig?.axisLabelFactor ?: 1.05

        val numAxes = axes.size
        if (numAxes < 3) {
            return buildEmptySvg(diagramId, chartWidth, chartHeight)
        }

        // 计算中心和半径
        val plotWidth = chartWidth - marginLeft - marginRight
        val plotHeight = chartHeight - marginTop - marginBottom - (if (title.isNotEmpty()) 30 else 0)
        val radius = minOf(plotWidth, plotHeight) / 2
        val cx = chartWidth / 2
        val titleOffset = if (title.isNotEmpty()) 30.0 else 0.0
        val cy = marginTop + titleOffset + plotHeight / 2

        val angleSlice = 2 * PI / numAxes

        return buildSvg {
            attr("id", diagramId)
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("xmlns:xlink", "http://www.w3.org/1999/xlink")

            group {
                addClass("radar-chart")

                // 标题
                if (title.isNotEmpty()) {
                    text(title, cx, marginTop + TITLE_FONT_SIZE) {
                        attr("text-anchor", "middle")
                        attr("font-size", "$TITLE_FONT_SIZE")
                        attr("font-weight", "bold")
                        attr("fill", themeVariables.textColor)
                    }
                }

                // 1. 同心网格
                group {
                    addClass("grid")
                    for (level in 1..LEVELS) {
                        val r = radius * level / LEVELS
                        val pathD = buildPolygonPath(cx, cy, r, numAxes, angleSlice)
                        path(pathD) {
                            attr("fill", "none")
                            attr("stroke", themeVariables.lineColor)
                            attr("stroke-width", "0.5")
                            attr("stroke-opacity", "0.5")
                        }

                        // 刻度值标签
                        val labelVal = maxValue * level / LEVELS
                        text(formatValue(labelVal), cx + 5, cy - r + 12) {
                            attr("font-size", "10")
                            attr("fill", themeVariables.textColor)
                            attr("opacity", "0.6")
                        }
                    }
                }

                // 2. 轴线 + 轴标签
                group {
                    addClass("axes")
                    for ((i, axisLabel) in axes.withIndex()) {
                        val angle = angleSlice * i - PI / 2
                        val lineX = cx + radius * cos(angle)
                        val lineY = cy + radius * sin(angle)

                        // 轴线
                        line(cx, cy, lineX, lineY) {
                            attr("stroke", themeVariables.lineColor)
                            attr("stroke-width", "1")
                            attr("stroke-opacity", "0.5")
                        }

                        // 轴标签
                        val labelX = cx + radius * axisLabelFactor * cos(angle)
                        val labelY = cy + radius * axisLabelFactor * sin(angle)
                        val anchor = when {
                            cos(angle) > 0.1 -> "start"
                            cos(angle) < -0.1 -> "end"
                            else -> "middle"
                        }
                        text(axisLabel, labelX, labelY + 4) {
                            attr("text-anchor", anchor)
                            attr("font-size", "$LABEL_FONT_SIZE")
                            attr("fill", themeVariables.textColor)
                        }
                    }
                }

                // 3. 数据多边形
                group {
                    addClass("datasets")
                    for ((dsIdx, dataset) in datasets.withIndex()) {
                        val color = datasetColors[dsIdx % datasetColors.size]
                        val values = dataset.values

                        group {
                            addClass("dataset-$dsIdx")

                            // 填充多边形
                            val pathD = buildString {
                                for ((i, v) in values.withIndex()) {
                                    if (i >= numAxes) break
                                    val angle = angleSlice * i - PI / 2
                                    val r = (v / maxValue) * radius
                                    val px = cx + r * cos(angle)
                                    val py = cy + r * sin(angle)
                                    if (i == 0) append("M $px $py") else append(" L $px $py")
                                }
                                append(" Z")
                            }
                            path(pathD) {
                                attr("fill", color)
                                attr("fill-opacity", "0.2")
                                attr("stroke", color)
                                attr("stroke-width", "2")
                            }

                            // 数据点
                            for ((i, v) in values.withIndex()) {
                                if (i >= numAxes) break
                                val angle = angleSlice * i - PI / 2
                                val r = (v / maxValue) * radius
                                val px = cx + r * cos(angle)
                                val py = cy + r * sin(angle)
                                circle(px, py, DOT_RADIUS) {
                                    attr("fill", color)
                                    attr("stroke", "#fff")
                                    attr("stroke-width", "1")
                                }
                            }
                        }
                    }
                }

                // 4. 图例
                if (showLegend && datasets.isNotEmpty()) {
                    group {
                        addClass("legend")
                        val legendX = chartWidth - marginRight - 120
                        var legendY = marginTop + titleOffset + 10

                        for ((dsIdx, dataset) in datasets.withIndex()) {
                            val color = datasetColors[dsIdx % datasetColors.size]
                            rect(legendX, legendY, LEGEND_RECT_SIZE, LEGEND_RECT_SIZE) {
                                attr("fill", color)
                                attr("fill-opacity", "0.6")
                                attr("stroke", color)
                                attr("stroke-width", "1")
                            }
                            text(dataset.name, legendX + LEGEND_RECT_SIZE + 5, legendY + LEGEND_RECT_SIZE - 2) {
                                attr("font-size", "$LEGEND_FONT_SIZE")
                                attr("fill", themeVariables.textColor)
                            }
                            legendY += LEGEND_SPACING
                        }
                    }
                }
            }

            viewBox(0.0, 0.0, chartWidth, chartHeight)
            attr("width", "100%")
            attr("style", "max-width: ${chartWidth.toInt()}px;")
        }
    }

    /**
     * 构建正多边形路径
     */
    private fun buildPolygonPath(cx: Double, cy: Double, r: Double, n: Int, angleSlice: Double): String {
        return buildString {
            for (i in 0 until n) {
                val angle = angleSlice * i - PI / 2
                val x = cx + r * cos(angle)
                val y = cy + r * sin(angle)
                if (i == 0) append("M $x $y") else append(" L $x $y")
            }
            append(" Z")
        }
    }

    private fun buildEmptySvg(id: String, w: Double, h: Double): SvgRoot {
        return buildSvg {
            attr("id", id)
            attr("xmlns", "http://www.w3.org/2000/svg")
            viewBox(0.0, 0.0, w, h)
        }
    }

    private fun formatValue(v: Double): String {
        return if (v == v.toLong().toDouble()) v.toLong().toString()
        else {
            val rounded = (v * 10).toLong() / 10.0
            if (rounded == rounded.toLong().toDouble()) "${rounded.toLong()}.0"
            else rounded.toString()
        }
    }
}
