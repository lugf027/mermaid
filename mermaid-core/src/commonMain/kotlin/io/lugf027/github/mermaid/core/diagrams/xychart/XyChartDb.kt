package io.lugf027.github.mermaid.core.diagrams.xychart

import io.lugf027.github.mermaid.core.db.CommonDb

enum class PlotType { LINE, BAR }
data class PlotData(val type: PlotType, val data: List<Float>)
data class AxisData(val categories: List<String>? = null, val min: Float? = null, val max: Float? = null)

class XyChartDb : CommonDb() {
    var xAxis = AxisData(); var yAxis = AxisData()
    val plots = mutableListOf<PlotData>()

    fun setXAxisCategories(cats: List<String>) { xAxis = xAxis.copy(categories = cats) }
    fun setYAxisRange(label: String, min: Float, max: Float) { yAxis = yAxis.copy(min = min, max = max) }
    fun addPlot(plot: PlotData) { plots.add(plot) }

    override fun clear() { super.clear(); xAxis = AxisData(); yAxis = AxisData(); plots.clear() }
}
