package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.quadrantChart.QuadrantDb
import io.lugf027.github.mermaid.core.diagram.quadrantChart.QuadrantParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuadrantParserTest {

    private fun parse(text: String): QuadrantDb {
        val db = QuadrantDb()
        QuadrantParser().parse(text, db)
        return db
    }

    @Test
    fun testBasicQuadrantChart() {
        val db = parse("""
            quadrantChart
            title My Chart
            x-axis Low --> High
            y-axis Small --> Large
            quadrant-1 Q1
            quadrant-2 Q2
            quadrant-3 Q3
            quadrant-4 Q4
        """.trimIndent())

        assertEquals("My Chart", db.getDiagramTitle())
        assertEquals("Low", db.xAxisLeftText)
        assertEquals("High", db.xAxisRightText)
        assertEquals("Small", db.yAxisBottomText)
        assertEquals("Large", db.yAxisTopText)
        assertEquals("Q1", db.quadrant1Text)
        assertEquals("Q2", db.quadrant2Text)
        assertEquals("Q3", db.quadrant3Text)
        assertEquals("Q4", db.quadrant4Text)
    }

    @Test
    fun testPointParsing() {
        val db = parse("""
            quadrantChart
            Point A: [0.3, 0.7]
            Point B: [0.8, 0.2]
        """.trimIndent())

        val points = db.getPoints()
        assertEquals(2, points.size)
        assertEquals("Point A", points[0].text)
        assertEquals(0.3, points[0].x, 0.001)
        assertEquals(0.7, points[0].y, 0.001)
        assertEquals("Point B", points[1].text)
        assertEquals(0.8, points[1].x, 0.001)
        assertEquals(0.2, points[1].y, 0.001)
    }

    @Test
    fun testPointWithClassName() {
        val db = parse("""
            quadrantChart
            classDef important color:#ff0000, radius:8
            My Point:::important: [0.5, 0.5]
        """.trimIndent())

        val points = db.getPoints()
        assertEquals(1, points.size)
        assertEquals("My Point", points[0].text)
        assertEquals("important", points[0].className)
        assertEquals(8.0, points[0].radius, 0.001)
        assertEquals("#ff0000", points[0].color)
    }

    @Test
    fun testXAxisWithoutArrow() {
        val db = parse("""
            quadrantChart
            x-axis Performance
            y-axis Usability
        """.trimIndent())

        assertEquals("Performance", db.xAxisLeftText)
        assertEquals("", db.xAxisRightText)
        assertEquals("Usability", db.yAxisBottomText)
        assertEquals("", db.yAxisTopText)
    }

    @Test
    fun testCommentsAndEmptyLines() {
        val db = parse("""
            quadrantChart
            %% This is a comment
            title Test
            
            Point A: [0.1, 0.9]
        """.trimIndent())

        assertEquals("Test", db.getDiagramTitle())
        assertEquals(1, db.getPoints().size)
    }

    @Test
    fun testAccTitleAndDescr() {
        val db = parse("""
            quadrantChart
            accTitle: My accessible title
            accDescr: My description
        """.trimIndent())

        assertEquals("My accessible title", db.getAccTitle())
        assertEquals("My description", db.getAccDescription())
    }
}
