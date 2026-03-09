package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.flowchart.FlowchartDb
import io.lugf027.github.mermaid.core.diagram.flowchart.FlowchartParser
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 流程图解析器单元测试 - 测试各种流程图语法
 */
class FlowchartParserTest {

    private lateinit var parser: FlowchartParser
    private lateinit var db: FlowchartDb

    @BeforeTest
    fun setup() {
        parser = FlowchartParser()
        db = FlowchartDb()
    }

    @Test
    fun testBasicFlowchart() {
        val input = """
            flowchart LR
                A --> B
        """.trimIndent()
        parser.parse(input, db)

        assertTrue(db.getVertices().containsKey("A"))
        assertTrue(db.getVertices().containsKey("B"))
        assertEquals(1, db.getEdges().size)
    }

    @Test
    fun testFlowchartWithLabels() {
        val input = """
            flowchart TD
                A[Start] --> B[End]
        """.trimIndent()
        parser.parse(input, db)

        val a = db.getVertices()["A"]
        val b = db.getVertices()["B"]
        assertEquals("Start", a?.text)
        assertEquals("End", b?.text)
    }

    @Test
    fun testGraphKeyword() {
        val input = """
            graph LR
                A --> B
        """.trimIndent()
        parser.parse(input, db)

        assertTrue(db.getVertices().isNotEmpty())
        assertEquals("LR", db.getDirection())
    }

    @Test
    fun testNodeShapes() {
        val input = """
            flowchart LR
                A[Square]
                B(Rounded)
                C((Circle))
                D{Diamond}
                E>Asymmetric]
        """.trimIndent()
        parser.parse(input, db)

        assertEquals(5, db.getVertices().size)
    }

    @Test
    fun testMultipleEdges() {
        val input = """
            flowchart TD
                A --> B
                B --> C
                C --> D
        """.trimIndent()
        parser.parse(input, db)

        assertEquals(4, db.getVertices().size)
        assertEquals(3, db.getEdges().size)
    }

    @Test
    fun testDirections() {
        for (dir in listOf("TB", "TD", "BT", "RL", "LR")) {
            val localDb = FlowchartDb()
            parser.parse("flowchart $dir\n    A --> B", localDb)
            assertEquals(dir, localDb.getDirection())
        }
    }

    @Test
    fun testSubgraph() {
        val input = """
            flowchart LR
                subgraph sub1[Subgraph Title]
                    A --> B
                end
        """.trimIndent()
        parser.parse(input, db)

        assertTrue(db.getSubGraphs().isNotEmpty())
    }

    @Test
    fun testEmptyFlowchart() {
        val input = "flowchart LR"
        parser.parse(input, db)
        assertTrue(db.getVertices().isEmpty())
        assertEquals("LR", db.getDirection())
    }

    @Test
    fun testEdgeWithLabel() {
        val input = """
            flowchart LR
                A -->|text| B
        """.trimIndent()
        parser.parse(input, db)

        val edge = db.getEdges().firstOrNull()
        assertTrue(edge != null)
    }
}
