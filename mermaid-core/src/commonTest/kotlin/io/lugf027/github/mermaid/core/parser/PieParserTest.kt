package io.lugf027.github.mermaid.core.parser

import io.lugf027.github.mermaid.core.core.MermaidKMP
import io.lugf027.github.mermaid.core.diagrams.pie.PieDb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PieParserTest {

    @Test
    fun parsePieChart() {
        val diagram = MermaidKMP.parse("""
            pie title Pets
                "Dogs" : 386
                "Cats" : 85
                "Rats" : 15
        """.trimIndent())

        assertNotNull(diagram)
        val db = diagram.db as PieDb
        assertEquals("Pets", db.getDiagramTitle())
        val sections = db.getSections()
        assertEquals(3, sections.size)
        assertEquals(386.0, sections["Dogs"])
        assertEquals(85.0, sections["Cats"])
        assertEquals(15.0, sections["Rats"])
    }

    @Test
    fun parsePieChartWithShowData() {
        val diagram = MermaidKMP.parse("""
            pie showData
                "A" : 10
                "B" : 20
        """.trimIndent())

        val db = diagram.db as PieDb
        assertTrue(db.getShowData())
        assertEquals(2, db.getSections().size)
    }

    @Test
    fun parsePieChartEmpty() {
        val diagram = MermaidKMP.parse("pie")
        val db = diagram.db as PieDb
        assertEquals(0, db.getSections().size)
    }
}
