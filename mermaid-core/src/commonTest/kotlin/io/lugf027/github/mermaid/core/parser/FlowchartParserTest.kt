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
        assertTrue(vertices.isNotEmpty(), "Should have at least 1 vertex")
        assertTrue(db.getEdges().isNotEmpty(), "Should have at least 1 edge")
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
