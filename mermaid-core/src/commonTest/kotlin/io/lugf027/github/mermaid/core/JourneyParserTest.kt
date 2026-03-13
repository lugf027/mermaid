package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.journey.JourneyDb
import io.lugf027.github.mermaid.core.diagram.journey.JourneyParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JourneyParserTest {
    private val parser = JourneyParser()

    @Test
    fun testSimpleJourney() {
        val db = JourneyDb()
        val text = """
            journey
            title My Journey
            section Go to work
            Make tea: 5: Me
            Go upstairs: 3: Me, Cat
        """.trimIndent()

        parser.parse(text, db)
        assertEquals("My Journey", db.getDiagramTitle())
        assertEquals(listOf("Go to work"), db.getSections())
        assertEquals(2, db.getTasks().size)
    }

    @Test
    fun testTaskParsing() {
        val db = JourneyDb()
        val text = """
            journey
            section Shopping
            Go shopping: 5: Mum, Dad
        """.trimIndent()

        parser.parse(text, db)
        val task = db.getTasks().first()
        assertEquals("Go shopping", task.task)
        assertEquals(5, task.score)
        assertEquals(listOf("Mum", "Dad"), task.people)
        assertEquals("Shopping", task.section)
    }

    @Test
    fun testTaskWithoutActors() {
        val db = JourneyDb()
        val text = """
            journey
            Do something: 3
        """.trimIndent()

        parser.parse(text, db)
        val task = db.getTasks().first()
        assertEquals("Do something", task.task)
        assertEquals(3, task.score)
        assertTrue(task.people.isEmpty())
    }

    @Test
    fun testMultipleSections() {
        val db = JourneyDb()
        val text = """
            journey
            title Daily Routine
            section Morning
            Wake up: 1: Me
            Eat breakfast: 3: Me
            section Work
            Code: 5: Me
            Meeting: 2: Me, Boss
        """.trimIndent()

        parser.parse(text, db)
        assertEquals(listOf("Morning", "Work"), db.getSections())
        assertEquals(4, db.getTasks().size)
        assertEquals("Morning", db.getTasks()[0].section)
        assertEquals("Work", db.getTasks()[2].section)
    }

    @Test
    fun testActorsExtraction() {
        val db = JourneyDb()
        val text = """
            journey
            Task1: 5: Alice, Bob
            Task2: 3: Bob, Charlie
            Task3: 4: Alice
        """.trimIndent()

        parser.parse(text, db)
        val actors = db.getActors()
        assertEquals(listOf("Alice", "Bob", "Charlie"), actors) // 排序后
    }

    @Test
    fun testCommentsIgnored() {
        val db = JourneyDb()
        val text = """
            journey
            %% This is a comment
            # This is also a comment
            title My Journey
            Make tea: 5: Me
        """.trimIndent()

        parser.parse(text, db)
        assertEquals("My Journey", db.getDiagramTitle())
        assertEquals(1, db.getTasks().size)
    }

    @Test
    fun testAccTitleAndDescr() {
        val db = JourneyDb()
        val text = """
            journey
            accTitle: My accessible title
            accDescr: My accessible description
            Make tea: 5: Me
        """.trimIndent()

        parser.parse(text, db)
        assertEquals("My accessible title", db.getAccTitle())
        assertEquals("My accessible description", db.getAccDescription())
    }

    @Test
    fun testNoSectionDefaultsEmpty() {
        val db = JourneyDb()
        val text = """
            journey
            Task1: 5: Alice
        """.trimIndent()

        parser.parse(text, db)
        assertEquals("", db.getTasks().first().section)
    }
}
