package io.lugf027.github.mermaid.core.diagram.xychart

import io.lugf027.github.mermaid.core.diagram.DiagramDB

/**
 * XY 图表数据库 - 对标 mermaid-js xychartDb.ts
 *
 * 支持 bar 和 line 两种 plot 类型，X/Y 轴支持 band 和 linear 两种模式。
 */
class XYChartDb : DiagramDB {

    /** 轴类型 */
    sealed class AxisData {
        abstract val title: String

        data class BandAxis(
            override val title: String,
            val categories: List<String>
        ) : AxisData()

        data class LinearAxis(
            override val title: String,
            val min: Double,
            val max: Double
        ) : AxisData()
    }

    /** 图表数据 */
    sealed class PlotData {
        abstract val title: String

        data class BarPlot(
            override val title: String = "",
            val data: List<Double>
        ) : PlotData()

        data class LinePlot(
            override val title: String = "",
            val data: List<Double>
        ) : PlotData()
    }

    // --- 内部状态 ---
    var xAxis: AxisData? = null
    var yAxis: AxisData? = null
    private val plots = mutableListOf<PlotData>()
    private var orientation = "vertical" // vertical | horizontal

    // --- DiagramDB ---
    private var diagramTitle = ""
    private var accTitle = ""
    private var accDescription = ""

    override fun clear() {
        xAxis = null
        yAxis = null
        plots.clear()
        orientation = "vertical"
        diagramTitle = ""
        accTitle = ""
        accDescription = ""
    }

    override fun setDiagramTitle(title: String) { diagramTitle = title }
    override fun getDiagramTitle(): String = diagramTitle
    override fun setAccTitle(title: String) { accTitle = title }
    override fun getAccTitle(): String = accTitle
    override fun setAccDescription(desc: String) { accDescription = desc }
    override fun getAccDescription(): String = accDescription

    // --- 操作 ---

    fun setOrientation(o: String) { orientation = o.lowercase() }
    fun getOrientation(): String = orientation

    fun setXAxisBand(title: String, categories: List<String>) {
        xAxis = AxisData.BandAxis(title, categories)
    }

    fun setXAxisLinear(title: String, min: Double, max: Double) {
        xAxis = AxisData.LinearAxis(title, min, max)
    }

    fun setYAxisLinear(title: String, min: Double, max: Double) {
        yAxis = AxisData.LinearAxis(title, min, max)
    }

    fun addBarPlot(title: String = "", data: List<Double>) {
        plots.add(PlotData.BarPlot(title, data))
    }

    fun addLinePlot(title: String = "", data: List<Double>) {
        plots.add(PlotData.LinePlot(title, data))
    }

    // --- 查询 ---

    fun getPlots(): List<PlotData> = plots.toList()

    /**
     * 自动推断 Y 轴范围
     */
    fun getEffectiveYAxis(): AxisData.LinearAxis {
        if (yAxis is AxisData.LinearAxis) return yAxis as AxisData.LinearAxis
        val allValues = plots.flatMap { when (it) {
            is PlotData.BarPlot -> it.data
            is PlotData.LinePlot -> it.data
        }}
        val min = allValues.minOrNull() ?: 0.0
        val max = allValues.maxOrNull() ?: 100.0
        return AxisData.LinearAxis("", kotlin.math.floor(min), kotlin.math.ceil(max))
    }
}
