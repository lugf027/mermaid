package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.radar.RadarDb
import io.lugf027.github.mermaid.core.diagram.radar.RadarParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class RadarParserTest {

    private fun parse(text: String): RadarDb {
        val db = RadarDb()
        RadarParser().parse(text, db)
        return db
    }

    @Test
    fun testBasicRadarChart() {
        val db = parse("""
            radar-beta
            title Skills Assessment
            axis Speed, Reliability, Comfort, Safety, Efficiency
            Developer --> 80, 90, 70, 85, 95
        """.trimIndent())

        assertEquals("Skills Assessment", db.getDiagramTitle())
        assertEquals(5, db.getAxisCount())
        assertEquals(listOf("Speed", "Reliability", "Comfort", "Safety", "Efficiency"), db.getAxes())
        assertEquals(1, db.getDatasets().size)
        assertEquals("Developer", db.getDatasets()[0].name)
        assertEquals(listOf(80.0, 90.0, 70.0, 85.0, 95.0), db.getDatasets()[0].values)
    }

    @Test
    fun testMultipleDatasets() {
        val db = parse("""
            radar-beta
            axis A, B, C, D
            Team1 --> 80, 70, 90, 60
            Team2 --> 60, 90, 70, 80
            Team3 --> 70, 80, 80, 70
        """.trimIndent())

        assertEquals(3, db.getDatasets().size)
        assertEquals("Team1", db.getDatasets()[0].name)
        assertEquals("Team2", db.getDatasets()[1].name)
        assertEquals("Team3", db.getDatasets()[2].name)
    }

    @Test
    fun testQuotedDatasetName() {
        val db = parse("""
            radar-beta
            axis X, Y, Z
            "My Team" --> 50, 60, 70
        """.trimIndent())

        assertEquals(1, db.getDatasets().size)
        assertEquals("My Team", db.getDatasets()[0].name)
    }

    @Test
    fun testMaxValue() {
        val db = parse("""
            radar-beta
            axis A, B, C
            max 200
            Data --> 100, 150, 180
        """.trimIndent())

        assertEquals(200.0, db.getEffectiveMaxValue(), 0.001)
    }

    @Test
    fun testAutoMaxValue() {
        val db = parse("""
            radar-beta
            axis A, B, C
            Data --> 30, 70, 50
        """.trimIndent())

        assertEquals(70.0, db.getEffectiveMaxValue(), 0.001)
    }

    @Test
    fun testShowLegend() {
        val db = parse("""
            radar-beta
            axis A, B, C
            showLegend false
            Data --> 10, 20, 30
        """.trimIndent())

        assertFalse(db.getShowLegend())
    }

    @Test
    fun testDefaultShowLegend() {
        val db = parse("""
            radar-beta
            axis A, B, C
            Data --> 10, 20, 30
        """.trimIndent())

        assertTrue(db.getShowLegend())
    }

    @Test
    fun testCommentsAndEmptyLines() {
        val db = parse("""
            radar-beta
            %% This is a comment
            title Test

            axis A, B, C

            %% Another comment
            Data --> 1, 2, 3
        """.trimIndent())

        assertEquals("Test", db.getDiagramTitle())
        assertEquals(3, db.getAxisCount())
        assertEquals(1, db.getDatasets().size)
    }

    @Test
    fun testRadarWithoutBeta() {
        val db = parse("""
            radar-beta
            axis A, B, C
            D --> 10, 20, 30
        """.trimIndent())

        assertEquals(3, db.getAxisCount())
        assertEquals(1, db.getDatasets().size)
    }

    @Test
    fun testAccTitleAndDescr() {
        val db = parse("""
            radar-beta
            accTitle: Performance Radar
            accDescr: Comparison of team performance
            axis A, B, C
            Data --> 1, 2, 3
        """.trimIndent())

        assertEquals("Performance Radar", db.getAccTitle())
        assertEquals("Comparison of team performance", db.getAccDescription())
    }
}
