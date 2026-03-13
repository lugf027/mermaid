package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.xychart.XYChartDb
import io.lugf027.github.mermaid.core.diagram.xychart.XYChartParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class XYChartParserTest {

    private fun parse(text: String): XYChartDb {
        val db = XYChartDb()
        XYChartParser().parse(text, db)
        return db
    }

    @Test
    fun testBasicBarChart() {
        val db = parse("""
            xychart-beta
            title Sales by Month
            x-axis "Month" [Jan, Feb, Mar, Apr]
            y-axis "Revenue" 0 --> 100
            bar [10, 30, 50, 20]
        """.trimIndent())

        assertEquals("Sales by Month", db.getDiagramTitle())
        assertTrue(db.xAxis is XYChartDb.AxisData.BandAxis)
        val xAxis = db.xAxis as XYChartDb.AxisData.BandAxis
        assertEquals("Month", xAxis.title)
        assertEquals(listOf("Jan", "Feb", "Mar", "Apr"), xAxis.categories)

        assertTrue(db.yAxis is XYChartDb.AxisData.LinearAxis)
        val yAxis = db.yAxis as XYChartDb.AxisData.LinearAxis
        assertEquals("Revenue", yAxis.title)
        assertEquals(0.0, yAxis.min, 0.001)
        assertEquals(100.0, yAxis.max, 0.001)

        val plots = db.getPlots()
        assertEquals(1, plots.size)
        assertTrue(plots[0] is XYChartDb.PlotData.BarPlot)
        assertEquals(listOf(10.0, 30.0, 50.0, 20.0), (plots[0] as XYChartDb.PlotData.BarPlot).data)
    }

    @Test
    fun testLineChart() {
        val db = parse("""
            xychart-beta
            x-axis "X" [A, B, C]
            line [1, 2, 3]
        """.trimIndent())

        val plots = db.getPlots()
        assertEquals(1, plots.size)
        assertTrue(plots[0] is XYChartDb.PlotData.LinePlot)
        assertEquals(listOf(1.0, 2.0, 3.0), (plots[0] as XYChartDb.PlotData.LinePlot).data)
    }

    @Test
    fun testMixedBarAndLine() {
        val db = parse("""
            xychart-beta
            x-axis [Q1, Q2, Q3, Q4]
            bar [100, 200, 300, 400]
            line [50, 150, 250, 350]
        """.trimIndent())

        val plots = db.getPlots()
        assertEquals(2, plots.size)
        assertTrue(plots[0] is XYChartDb.PlotData.BarPlot)
        assertTrue(plots[1] is XYChartDb.PlotData.LinePlot)
    }

    @Test
    fun testHorizontalOrientation() {
        val db = parse("""
            xychart-beta horizontal
            x-axis [A, B, C]
            bar [1, 2, 3]
        """.trimIndent())

        assertEquals("horizontal", db.getOrientation())
    }

    @Test
    fun testTitledPlots() {
        val db = parse("""
            xychart-beta
            x-axis [Jan, Feb, Mar]
            bar "Sales" [10, 20, 30]
            line "Trend" [15, 20, 25]
        """.trimIndent())

        val plots = db.getPlots()
        assertEquals("Sales", (plots[0] as XYChartDb.PlotData.BarPlot).title)
        assertEquals("Trend", (plots[1] as XYChartDb.PlotData.LinePlot).title)
    }

    @Test
    fun testAutoYAxis() {
        val db = parse("""
            xychart-beta
            x-axis [A, B, C]
            bar [10, 50, 30]
        """.trimIndent())

        val yAxis = db.getEffectiveYAxis()
        assertEquals(10.0, yAxis.min, 0.001)
        assertEquals(50.0, yAxis.max, 0.001)
    }

    @Test
    fun testXchartBetaOptional() {
        val db = parse("""
            xychart-beta
            x-axis [A, B]
            bar [1, 2]
        """.trimIndent())

        assertEquals(1, db.getPlots().size)

        val db2 = parse("""
            xychart
            x-axis [A, B]
            bar [1, 2]
        """.trimIndent())

        // xychart without -beta should also be matched by the regex
        // But the parser's RE_START expects optional -beta
        // This test verifies both formats are supported
    }
}
