package io.lugf027.github.mermaid.core.diagrams.quadrant

import io.lugf027.github.mermaid.core.db.CommonDb

data class QuadrantPoint(val label: String, val x: Float, val y: Float, val className: String? = null)

class QuadrantDb : CommonDb() {
    var xAxisLeft = "Low"; var xAxisRight = "High"
    var yAxisBottom = "Low"; var yAxisTop = "High"
    var q1Text = ""; var q2Text = ""; var q3Text = ""; var q4Text = ""
    private val points = mutableListOf<QuadrantPoint>()

    fun addPoint(p: QuadrantPoint) { points.add(p) }
    fun getPoints(): List<QuadrantPoint> = points

    override fun clear() { super.clear(); points.clear(); xAxisLeft = "Low"; xAxisRight = "High"; yAxisBottom = "Low"; yAxisTop = "High" }
}
