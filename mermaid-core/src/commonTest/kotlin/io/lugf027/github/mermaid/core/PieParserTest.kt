package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.pie.PieDb
import io.lugf027.github.mermaid.core.diagram.pie.PieParser
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 饼图解析器单元测试 - 对标 mermaid-js pie.spec.ts
 */
class PieParserTest {

    private lateinit var parser: PieParser
    private lateinit var db: PieDb

    @BeforeTest
    fun setup() {
        parser = PieParser()
        db = PieDb()
    }

    @Test
    fun testBasicPie() {
        val input = """
            pie
                "Dogs" : 30
                "Cats" : 70
        """.trimIndent()
        parser.parse(input, db)

        val sections = db.getSections()
        assertEquals(2, sections.size)
        assertEquals(30.0, sections["Dogs"])
        assertEquals(70.0, sections["Cats"])
    }

    @Test
    fun testPieWithShowData() {
        val input = """
            pie showData
                "Dogs" : 30
                "Cats" : 70
        """.trimIndent()
        parser.parse(input, db)

        assertTrue(db.getShowData())
        assertEquals(2, db.getSections().size)
    }

    @Test
    fun testPieWithTitle() {
        val input = """
            pie
                title My Pets
                "Dogs" : 30
                "Cats" : 70
        """.trimIndent()
        parser.parse(input, db)

        assertEquals("My Pets", db.getDiagramTitle())
        assertEquals(2, db.getSections().size)
    }

    @Test
    fun testPieWithFloatValues() {
        val input = """
            pie
                "A" : 30.5
                "B" : 69.5
        """.trimIndent()
        parser.parse(input, db)

        assertEquals(30.5, db.getSections()["A"])
        assertEquals(69.5, db.getSections()["B"])
    }

    @Test
    fun testPieWithSingleQuotes() {
        val input = """
            pie
                'Dogs' : 30
                'Cats' : 70
        """.trimIndent()
        parser.parse(input, db)

        assertEquals(2, db.getSections().size)
        assertEquals(30.0, db.getSections()["Dogs"])
    }

    @Test
    fun testNegativeValueThrows() {
        val input = """
            pie
                "Dogs" : -30
        """.trimIndent()
        assertFailsWith<IllegalArgumentException> {
            parser.parse(input, db)
        }
    }

    @Test
    fun testDuplicateLabelsIgnored() {
        val input = """
            pie
                "Dogs" : 30
                "Dogs" : 50
                "Cats" : 70
        """.trimIndent()
        parser.parse(input, db)

        val sections = db.getSections()
        assertEquals(2, sections.size) // "Dogs" only once
        assertEquals(30.0, sections["Dogs"]) // First value kept
    }

    @Test
    fun testPieWithAccTitle() {
        val input = """
            pie
                accTitle: Accessible Title
                "A" : 50
                "B" : 50
        """.trimIndent()
        parser.parse(input, db)

        assertEquals("Accessible Title", db.getAccTitle())
    }

    @Test
    fun testPieWithAccDescr() {
        val input = """
            pie
                accDescr: A description
                "A" : 50
                "B" : 50
        """.trimIndent()
        parser.parse(input, db)

        assertEquals("A description", db.getAccDescription())
    }

    @Test
    fun testEmptyPie() {
        val input = "pie"
        parser.parse(input, db)

        assertTrue(db.getSections().isEmpty())
        assertFalse(db.getShowData())
    }

    @Test
    fun testPieWithComments() {
        val input = """
            pie
                %% This is a comment
                "A" : 50
                "B" : 50
        """.trimIndent()
        parser.parse(input, db)

        assertEquals(2, db.getSections().size)
    }

    @Test
    fun testPieWithMultipleSections() {
        val input = """
            pie
                "Very Long Label One" : 10
                "Label Two" : 20
                "Label Three" : 30
                "Label Four" : 40
        """.trimIndent()
        parser.parse(input, db)

        assertEquals(4, db.getSections().size)
        assertEquals(10.0, db.getSections()["Very Long Label One"])
        assertEquals(40.0, db.getSections()["Label Four"])
    }
}
