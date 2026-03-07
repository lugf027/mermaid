package io.lugf027.github.mermaid.core.parser

import io.lugf027.github.mermaid.core.core.MermaidKMP
import io.lugf027.github.mermaid.core.diagrams.flowchart.FlowDb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FlowchartParserTest {

    @Test
    fun parseBasicFlowchart() {
        val diagram = MermaidKMP.parse("""
            flowchart TD
                A --> B
        """.trimIndent())

        assertNotNull(diagram)
        val db = diagram.db as FlowDb
        val vertices = db.getVertices()
        assertTrue(vertices.containsKey("A"))
        assertTrue(vertices.containsKey("B"))
        assertEquals(1, db.getEdges().size)
    }

    @Test
    fun parseFlowchartWithLabels() {
        val diagram = MermaidKMP.parse("""
            flowchart LR
                A[Start] --> B{Decision}
                B -->|Yes| C[End]
                B -->|No| D[Retry]
        """.trimIndent())

        val db = diagram.db as FlowDb
        val vertices = db.getVertices()
        assertEquals(4, vertices.size, "Should have 4 vertices (A, B, C, D)")
        assertEquals(3, db.getEdges().size, "Should have 3 edges")

        // 验证节点标签
        assertEquals("Start", vertices["A"]?.text)
        assertEquals("Decision", vertices["B"]?.text)
        assertEquals("End", vertices["C"]?.text)
        assertEquals("Retry", vertices["D"]?.text)

        // 验证边文本
        val edges = db.getEdges()
        val bToC = edges.find { it.start == "B" && it.end == "C" }
        assertNotNull(bToC, "Edge B->C should exist")
        assertEquals("Yes", bToC.text)

        val bToD = edges.find { it.start == "B" && it.end == "D" }
        assertNotNull(bToD, "Edge B->D should exist")
        assertEquals("No", bToD.text)
    }

    @Test
    fun parseCompleteFlowchart() {
        val diagram = MermaidKMP.parse("""
            flowchart TD
                A[Start] --> B{Is it sunny?}
                B -->|Yes| C[Go to the park]
                B -->|No| D[Stay home]
                C --> E[Have fun!]
                D --> E
                E --> F[End]
        """.trimIndent())

        val db = diagram.db as FlowDb
        val vertices = db.getVertices()
        assertEquals(6, vertices.size, "Should have 6 vertices (A-F)")

        // 验证所有节点标签
        assertEquals("Start", vertices["A"]?.text)
        assertEquals("Is it sunny?", vertices["B"]?.text)
        assertEquals("Go to the park", vertices["C"]?.text)
        assertEquals("Stay home", vertices["D"]?.text)
        assertEquals("Have fun!", vertices["E"]?.text)
        assertEquals("End", vertices["F"]?.text)

        // 验证边数量
        assertEquals(6, db.getEdges().size, "Should have 6 edges")

        // 验证带文本的边
        val edges = db.getEdges()
        val bToC = edges.find { it.start == "B" && it.end == "C" }
        assertNotNull(bToC, "Edge B->C should exist")
        assertEquals("Yes", bToC.text)

        val bToD = edges.find { it.start == "B" && it.end == "D" }
        assertNotNull(bToD, "Edge B->D should exist")
        assertEquals("No", bToD.text)
    }

    @Test
    fun parseChristmasFlowchart() {
        val diagram = MermaidKMP.parse("""
            flowchart TD
                A[Christmas] -->|Get money| B(Go shopping)
                B --> C{Let me think}
                C -->|One| D[Laptop]
                C -->|Two| E[iPhone]
                C -->|Three| F[fa:fa-car Car]
        """.trimIndent())

        val db = diagram.db as FlowDb
        val vertices = db.getVertices()
        assertEquals(6, vertices.size, "Should have 6 vertices (A-F)")

        // 验证节点标签
        assertEquals("Christmas", vertices["A"]?.text)
        assertEquals("Go shopping", vertices["B"]?.text)
        assertEquals("Let me think", vertices["C"]?.text)
        assertEquals("Laptop", vertices["D"]?.text)
        assertEquals("iPhone", vertices["E"]?.text)
        assertEquals("fa:fa-car Car", vertices["F"]?.text)

        // 验证边数量
        assertEquals(5, db.getEdges().size, "Should have 5 edges")

        // 验证带文本的边
        val edges = db.getEdges()
        val aToB = edges.find { it.start == "A" && it.end == "B" }
        assertNotNull(aToB, "Edge A->B should exist")
        assertEquals("Get money", aToB.text)

        val cToD = edges.find { it.start == "C" && it.end == "D" }
        assertNotNull(cToD, "Edge C->D should exist")
        assertEquals("One", cToD.text)

        val cToE = edges.find { it.start == "C" && it.end == "E" }
        assertNotNull(cToE, "Edge C->E should exist")
        assertEquals("Two", cToE.text)

        val cToF = edges.find { it.start == "C" && it.end == "F" }
        assertNotNull(cToF, "Edge C->F should exist")
        assertEquals("Three", cToF.text)

        // 验证形状类型
        assertNotNull(vertices["B"]?.type, "B should have ROUND type")
        assertNotNull(vertices["C"]?.type, "C should have DIAMOND type")
    }

    @Test
    fun parseFlowchartGraph() {
        val diagram = MermaidKMP.parse("""
            graph TD
                A --> B
                B --> C
        """.trimIndent())

        assertNotNull(diagram)
        val db = diagram.db as FlowDb
        assertEquals(3, db.getVertices().size)
        assertEquals(2, db.getEdges().size)
    }
}
