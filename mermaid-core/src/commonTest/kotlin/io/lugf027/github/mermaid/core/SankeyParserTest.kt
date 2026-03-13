package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.sankey.SankeyDb
import io.lugf027.github.mermaid.core.diagram.sankey.SankeyParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SankeyParserTest {

    private fun parse(text: String): SankeyDb {
        val db = SankeyDb()
        SankeyParser().parse(text, db)
        return db
    }

    @Test
    fun testBasicSankeyDiagram() {
        val db = parse("""
            sankey-beta

            A,B,10
            B,C,5
            B,D,5
        """.trimIndent())

        val nodes = db.getNodes()
        val links = db.getLinks()
        assertEquals(4, nodes.size)
        assertEquals(3, links.size)
        assertEquals("A", links[0].source)
        assertEquals("B", links[0].target)
        assertEquals(10.0, links[0].value, 0.001)
    }

    @Test
    fun testQuotedNodeNames() {
        val db = parse("""
            sankey-beta

            "Source Node","Target Node",100
        """.trimIndent())

        val links = db.getLinks()
        assertEquals(1, links.size)
        assertEquals("Source Node", links[0].source)
        assertEquals("Target Node", links[0].target)
        assertEquals(100.0, links[0].value, 0.001)
    }

    @Test
    fun testMultipleLinks() {
        val db = parse("""
            sankey-beta

            Electricity,Heat,50
            Electricity,Light,30
            Gas,Heat,20
            Gas,Transport,40
        """.trimIndent())

        // Electricity, Heat, Light, Gas, Transport = 5 unique nodes
        assertEquals(5, db.getNodes().size)
        assertEquals(4, db.getLinks().size)
    }

    @Test
    fun testNodeValues() {
        val db = parse("""
            sankey-beta

            A,B,30
            A,C,20
            B,D,15
            C,D,10
        """.trimIndent())

        assertEquals(50.0, db.getNodeOutputValue("A"), 0.001) // 30 + 20
        assertEquals(30.0, db.getNodeInputValue("B"), 0.001)
        assertEquals(25.0, db.getNodeInputValue("D"), 0.001) // 15 + 10
    }

    @Test
    fun testNodeColumns() {
        val db = parse("""
            sankey-beta

            A,B,10
            A,C,20
            B,D,5
            C,D,15
        """.trimIndent())

        val columns = db.getNodeColumns()
        assertTrue(columns.isNotEmpty())
        assertTrue(columns[0].contains("A"))
        assertTrue(columns.last().contains("D"))
    }

    @Test
    fun testCommentsSkipped() {
        val db = parse("""
            sankey-beta
            %% This is a comment
            A,B,10
            %% Another comment
            B,C,5
        """.trimIndent())

        assertEquals(3, db.getNodes().size)
        assertEquals(2, db.getLinks().size)
    }

    @Test
    fun testDecimalValues() {
        val db = parse("""
            sankey-beta

            Source,Target,25.5
        """.trimIndent())

        assertEquals(25.5, db.getLinks()[0].value, 0.001)
    }

    @Test
    fun testAccTitleAndDescr() {
        val db = parse("""
            sankey-beta
            accTitle: Energy Flow
            accDescr: Shows energy distribution

            A,B,100
        """.trimIndent())

        assertEquals("Energy Flow", db.getAccTitle())
        assertEquals("Shows energy distribution", db.getAccDescription())
    }
}
