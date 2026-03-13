package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.treemap.TreemapDb
import io.lugf027.github.mermaid.core.diagram.treemap.TreemapParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TreemapParserTest {

    private fun parse(text: String): TreemapDb {
        val db = TreemapDb()
        TreemapParser().parse(text, db)
        return db
    }

    @Test
    fun testBasicTreemap() {
        val db = parse("""
            treemap-beta
              "Root"
                "A" : 100
                "B" : 200
        """.trimIndent())

        val root = db.getRootNode()
        assertNotNull(root)
        assertEquals("Root", root.name)
        assertEquals(TreemapDb.NodeType.SECTION, root.type)
        assertEquals(2, root.children.size)
    }

    @Test
    fun testLeafValues() {
        val db = parse("""
            treemap-beta
              "Company"
                "Product A" : 500
                "Product B" : 300
                "Product C" : 200
        """.trimIndent())

        val root = db.getRootNode()
        assertNotNull(root)
        val children = root.children
        assertEquals(3, children.size)
        assertEquals(500.0, children[0].value)
        assertEquals(300.0, children[1].value)
        assertEquals(200.0, children[2].value)
        assertEquals(1000.0, root.totalValue())
    }

    @Test
    fun testNestedSections() {
        val db = parse("""
            treemap-beta
              "Company"
                "Division A"
                  "Team 1" : 100
                  "Team 2" : 200
                "Division B"
                  "Team 3" : 150
        """.trimIndent())

        val root = db.getRootNode()
        assertNotNull(root)
        assertEquals(2, root.children.size)
        assertEquals("Division A", root.children[0].name)
        assertEquals(TreemapDb.NodeType.SECTION, root.children[0].type)
        assertEquals(2, root.children[0].children.size)
        assertEquals(1, root.children[1].children.size)
    }

    @Test
    fun testWithoutBeta() {
        val db = parse("""
            treemap
              "Root"
                "A" : 50
        """.trimIndent())

        val root = db.getRootNode()
        assertNotNull(root)
    }

    @Test
    fun testAccTitle() {
        val db = parse("""
            treemap-beta
              accTitle: Sales Distribution
              "Root"
                "A" : 100
        """.trimIndent())

        assertEquals("Sales Distribution", db.getAccTitle())
    }

    @Test
    fun testCommentsIgnored() {
        val db = parse("""
            treemap-beta
              %% Company structure
              "Company"
                %% Main products
                "Product" : 100
        """.trimIndent())

        val root = db.getRootNode()
        assertNotNull(root)
    }

    @Test
    fun testDecimalValues() {
        val db = parse("""
            treemap-beta
              "Data"
                "X" : 10.5
                "Y" : 20.7
        """.trimIndent())

        val root = db.getRootNode()
        assertNotNull(root)
        assertEquals(10.5, root.children[0].value)
        assertEquals(20.7, root.children[1].value)
    }

    @Test
    fun testTotalValue() {
        val db = parse("""
            treemap-beta
              "Root"
                "Group"
                  "A" : 10
                  "B" : 20
                "C" : 30
        """.trimIndent())

        val root = db.getRootNode()
        assertNotNull(root)
        assertEquals(60.0, root.totalValue())
    }

    @Test
    fun testEmptyDiagram() {
        val db = parse("""
            treemap-beta
        """.trimIndent())

        val root = db.getRootNode()
        assertNull(root)
    }
}
