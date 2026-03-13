package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.venn.VennDb
import io.lugf027.github.mermaid.core.diagram.venn.VennParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VennParserTest {

    private fun parse(text: String): VennDb {
        val db = VennDb()
        VennParser().parse(text, db)
        return db
    }

    @Test
    fun testBasicTwoSets() {
        val db = parse("""
            venn-beta
            set A [Set A] : 10
            set B [Set B] : 8
        """.trimIndent())

        val sets = db.getSets()
        assertEquals(2, sets.size)
        assertEquals("Set A", sets[0].label)
        assertEquals(10.0, sets[0].size)
        assertEquals("Set B", sets[1].label)
        assertEquals(8.0, sets[1].size)
    }

    @Test
    fun testWithUnion() {
        val db = parse("""
            venn-beta
            set A [Set A] : 10
            set B [Set B] : 10
            union A, B [Overlap] : 2.5
        """.trimIndent())

        val unions = db.getUnions()
        assertEquals(1, unions.size)
        assertEquals("Overlap", unions[0].label)
        assertEquals(2.5, unions[0].size)
        assertEquals(listOf("A", "B"), unions[0].sets)
    }

    @Test
    fun testThreeSets() {
        val db = parse("""
            venn-beta
            set X [Apples] : 15
            set Y [Bananas] : 12
            set Z [Cherries] : 8
        """.trimIndent())

        val sets = db.getSets()
        assertEquals(3, sets.size)
        val knownSets = db.getKnownSets()
        assertTrue(knownSets.containsAll(listOf("X", "Y", "Z")))
    }

    @Test
    fun testSetWithoutLabel() {
        val db = parse("""
            venn-beta
            set A : 10
        """.trimIndent())

        val sets = db.getSets()
        assertEquals(1, sets.size)
        assertEquals("A", sets[0].label) // no label bracket → use id
    }

    @Test
    fun testWithTitle() {
        val db = parse("""
            venn-beta
            title Fruit Overlap
            set A [Apples] : 10
            set B [Bananas] : 10
        """.trimIndent())

        assertEquals("Fruit Overlap", db.getDiagramTitle())
    }

    @Test
    fun testWithStyle() {
        val db = parse("""
            venn-beta
            set A [X] : 10
            style A fill: #ff0000, stroke: blue
        """.trimIndent())

        val styles = db.getStyleEntries()
        assertEquals(1, styles.size)
        assertEquals(listOf("A"), styles[0].targets)
        assertTrue(styles[0].styles.containsKey("fill"))
    }

    @Test
    fun testAccTitle() {
        val db = parse("""
            venn-beta
            accTitle: My Venn Diagram
            set A : 10
        """.trimIndent())

        assertEquals("My Venn Diagram", db.getAccTitle())
    }

    @Test
    fun testWithoutBeta() {
        val db = parse("""
            venn
            set A [Cats] : 10
            set B [Dogs] : 8
        """.trimIndent())

        val sets = db.getSets()
        assertEquals(2, sets.size)
    }

    @Test
    fun testCommentsIgnored() {
        val db = parse("""
            venn-beta
            %% This is a comment
            set A [X] : 10
            %% Another comment
            set B [Y] : 8
        """.trimIndent())

        assertEquals(2, db.getSets().size)
    }
}
