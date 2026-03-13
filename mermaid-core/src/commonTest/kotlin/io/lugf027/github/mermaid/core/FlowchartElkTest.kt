package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.flowchartElk.FlowchartElkDiagram
import io.lugf027.github.mermaid.core.diagram.flowchart.FlowchartDb
import io.lugf027.github.mermaid.core.diagram.flowchart.FlowchartParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class FlowchartElkTest {

    @Test
    fun testElkKeywordDetection() {
        val def = FlowchartElkDiagram.definition()
        assertTrue(def.detector("flowchart-elk LR\n  A --> B"))
    }

    @Test
    fun testElkDirectiveDetection() {
        val def = FlowchartElkDiagram.definition()
        val text = """
            %%{init: {'flowchart': {'defaultRenderer': 'elk'}}}%%
            flowchart LR
              A --> B
        """.trimIndent()
        assertTrue(def.detector(text))
    }

    @Test
    fun testNonElkFlowchartNotDetected() {
        val def = FlowchartElkDiagram.definition()
        assertFalse(def.detector("flowchart LR\n  A --> B"))
    }

    @Test
    fun testElkReusesFlowchartParsing() {
        val db = FlowchartDb()
        FlowchartParser().parse("""
            flowchart-elk LR
              A[Node A] --> B[Node B]
              B --> C[Node C]
        """.trimIndent(), db)

        val vertices = db.getVertices()
        assertTrue(vertices.isNotEmpty())
    }

    @Test
    fun testDefinitionId() {
        val def = FlowchartElkDiagram.definition()
        assertEquals("flowchart-elk", def.id)
    }
}
