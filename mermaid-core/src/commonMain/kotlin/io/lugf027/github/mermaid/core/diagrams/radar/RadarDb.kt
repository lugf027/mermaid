package io.lugf027.github.mermaid.core.diagrams.radar

import io.lugf027.github.mermaid.core.db.CommonDb

data class RadarAxis(val name: String, val label: String = name)
data class RadarCurve(val name: String, val label: String = name, val entries: List<Float>)

class RadarDb : CommonDb() {
    private val axes = mutableListOf<RadarAxis>()
    private val curves = mutableListOf<RadarCurve>()
    var showLegend = true; var ticks = 5; var maxVal: Float? = null; var graticule = "circle"

    fun addAxis(axis: RadarAxis) { axes.add(axis) }
    fun addCurve(curve: RadarCurve) { curves.add(curve) }
    fun getAxes() = axes; fun getCurves() = curves

    override fun clear() { super.clear(); axes.clear(); curves.clear(); showLegend = true; ticks = 5; maxVal = null }
}
