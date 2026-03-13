package io.lugf027.github.mermaid.core.diagram.xychart

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.rendering.svg.buildSvg
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.TextUtils
import kotlin.math.max
import kotlin.math.min

/**
 * XY 图表渲染器 - 对标 mermaid-js xychartRenderer.ts
 *
 * 自定义渲染：标题 + X/Y轴 + bar/line 图表
 */
class XYChartRenderer : DiagramRenderer {

    companion object {
        const val DEFAULT_WIDTH = 700.0
        const val DEFAULT_HEIGHT = 500.0
        const val MARGIN_TOP = 60.0
        const val MARGIN_BOTTOM = 60.0
        const val MARGIN_LEFT = 60.0
        const val MARGIN_RIGHT = 20.0
        const val TICK_LENGTH = 5.0
        const val AXIS_LINE_WIDTH = 2.0
    }

    private val plotColors = listOf(
        "#4285F4", "#EA4335", "#FBBC04", "#34A853",
        "#FF6D01", "#46BDC6", "#7B61FF", "#F538A0"
    )

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val xyDb = db as XYChartDb
        val title = xyDb.getDiagramTitle()
        val plots = xyDb.getPlots()
        val xAxis = xyDb.xAxis
        val yAxis = xyDb.getEffectiveYAxis()

        val width = config.xyChart?.width?.toDouble() ?: DEFAULT_WIDTH
        val height = config.xyChart?.height?.toDouble() ?: DEFAULT_HEIGHT

        val plotLeft = MARGIN_LEFT
        val plotTop = MARGIN_TOP
        val plotWidth = width - MARGIN_LEFT - MARGIN_RIGHT
        val plotHeight = height - MARGIN_TOP - MARGIN_BOTTOM
        val plotBottom = plotTop + plotHeight

        // 确定数据点数量
        val numPoints = when (xAxis) {
            is XYChartDb.AxisData.BandAxis -> xAxis.categories.size
            else -> plots.flatMap { when (it) {
                is XYChartDb.PlotData.BarPlot -> it.data
                is XYChartDb.PlotData.LinePlot -> it.data
            }}.size.let { max(it / plots.size.coerceAtLeast(1), 1) }
        }

        return buildSvg {
            attr("id", diagramId)
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("xmlns:xlink", "http://www.w3.org/1999/xlink")

            group {
                addClass("main")

                // 背景
                rect(0.0, 0.0, width, height) {
                    attr("fill", themeVariables.mainBkg)
                    addClass("background")
                }

                // 标题
                if (title.isNotEmpty()) {
                    group {
                        addClass("chart-title")
                        text(title, width / 2, 30.0) {
                            attr("text-anchor", "middle")
                            attr("font-size", "16")
                            attr("fill", themeVariables.textColor)
                            attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")
                        }
                    }
                }

                // X 轴
                group {
                    addClass("bottom-axis")
                    // 轴线
                    line(plotLeft, plotBottom, plotLeft + plotWidth, plotBottom) {
                        attr("stroke", themeVariables.lineColor)
                        attr("stroke-width", "$AXIS_LINE_WIDTH")
                    }

                    // 刻度和标签
                    when (xAxis) {
                        is XYChartDb.AxisData.BandAxis -> {
                            val bandW = plotWidth / xAxis.categories.size
                            for ((i, cat) in xAxis.categories.withIndex()) {
                                val x = plotLeft + bandW * i + bandW / 2
                                line(x, plotBottom, x, plotBottom + TICK_LENGTH) {
                                    attr("stroke", themeVariables.lineColor)
                                    attr("stroke-width", "1")
                                }
                                text(cat, x, plotBottom + TICK_LENGTH + 14) {
                                    attr("text-anchor", "middle")
                                    attr("font-size", "12")
                                    attr("fill", themeVariables.textColor)
                                }
                            }
                            if (xAxis.title.isNotEmpty()) {
                                text(xAxis.title, plotLeft + plotWidth / 2, plotBottom + 40) {
                                    attr("text-anchor", "middle")
                                    attr("font-size", "14")
                                    attr("fill", themeVariables.textColor)
                                }
                            }
                        }
                        else -> {}
                    }
                }

                // Y 轴
                group {
                    addClass("left-axis")
                    line(plotLeft, plotTop, plotLeft, plotBottom) {
                        attr("stroke", themeVariables.lineColor)
                        attr("stroke-width", "$AXIS_LINE_WIDTH")
                    }

                    // Y 轴刻度
                    val ticks = 5
                    val yRange = yAxis.max - yAxis.min
                    for (i in 0..ticks) {
                        val v = yAxis.min + yRange * i / ticks
                        val y = plotBottom - (v - yAxis.min) / yRange * plotHeight
                        line(plotLeft - TICK_LENGTH, y, plotLeft, y) {
                            attr("stroke", themeVariables.lineColor)
                            attr("stroke-width", "1")
                        }
                        // 网格线
                        line(plotLeft, y, plotLeft + plotWidth, y) {
                            attr("stroke", themeVariables.lineColor)
                            attr("stroke-width", "0.5")
                            attr("stroke-dasharray", "3")
                            attr("opacity", "0.3")
                        }
                        text(v.toInt().toString(), plotLeft - TICK_LENGTH - 5, y + 4) {
                            attr("text-anchor", "end")
                            attr("font-size", "11")
                            attr("fill", themeVariables.textColor)
                        }
                    }

                    if (yAxis.title.isNotEmpty()) {
                        text(yAxis.title, 15.0, plotTop + plotHeight / 2) {
                            attr("text-anchor", "middle")
                            attr("font-size", "14")
                            attr("fill", themeVariables.textColor)
                            attr("transform", "rotate(-90, 15, ${plotTop + plotHeight / 2})")
                        }
                    }
                }

                // 图表数据
                group {
                    addClass("plot")
                    var barPlotIdx = 0
                    val barPlotCount = plots.count { it is XYChartDb.PlotData.BarPlot }

                    for ((plotIdx, plot) in plots.withIndex()) {
                        val color = plotColors[plotIdx % plotColors.size]

                        when (plot) {
                            is XYChartDb.PlotData.BarPlot -> {
                                group {
                                    addClass("bar-plot-$plotIdx")
                                    val bandW = if (numPoints > 0) plotWidth / numPoints else plotWidth
                                    val barW = bandW * 0.8 / barPlotCount.coerceAtLeast(1)
                                    val barOffset = barW * barPlotIdx

                                    for ((i, v) in plot.data.withIndex()) {
                                        val x = plotLeft + bandW * i + bandW * 0.1 + barOffset
                                        val barH = (v - yAxis.min) / (yAxis.max - yAxis.min) * plotHeight
                                        val y = plotBottom - barH

                                        rect(x, y, barW, barH) {
                                            attr("fill", color)
                                            attr("stroke", "none")
                                        }
                                    }
                                }
                                barPlotIdx++
                            }
                            is XYChartDb.PlotData.LinePlot -> {
                                group {
                                    addClass("line-plot-$plotIdx")
                                    val bandW = if (numPoints > 0) plotWidth / numPoints else plotWidth

                                    val pathD = buildString {
                                        for ((i, v) in plot.data.withIndex()) {
                                            val x = plotLeft + bandW * i + bandW / 2
                                            val y = plotBottom - (v - yAxis.min) / (yAxis.max - yAxis.min) * plotHeight
                                            if (i == 0) append("M $x $y") else append(" L $x $y")
                                        }
                                    }
                                    path(pathD) {
                                        attr("stroke", color)
                                        attr("stroke-width", "2")
                                        attr("fill", "none")
                                    }

                                    // 数据点
                                    for ((i, v) in plot.data.withIndex()) {
                                        val x = plotLeft + bandW * i + bandW / 2
                                        val y = plotBottom - (v - yAxis.min) / (yAxis.max - yAxis.min) * plotHeight
                                        circle(x, y, 3.0) {
                                            attr("fill", color)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            viewBox(0.0, 0.0, width, height)
            attr("width", "100%")
            attr("style", "max-width: ${width.toInt()}px;")
        }
    }
}
