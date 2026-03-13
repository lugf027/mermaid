package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.timeline.TimelineDb
import io.lugf027.github.mermaid.core.diagram.timeline.TimelineParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimelineParserTest {
    private val parser = TimelineParser()

    @Test
    fun testSimpleTimeline() {
        val db = TimelineDb()
        val text = """
            timeline
            title My Timeline
            2023-Q1 : Event 1
        """.trimIndent()

        parser.parse(text, db)
        assertEquals("My Timeline", db.getDiagramTitle())
        assertEquals(1, db.getTasks().size)
    }

    @Test
    fun testMultiplePeriodsAndEvents() {
        val db = TimelineDb()
        val text = """
            timeline
            2023-Q1 : Event A
                    : Event B
            2023-Q2 : Event C
        """.trimIndent()

        parser.parse(text, db)
        assertEquals(2, db.getTasks().size)
        val firstTask = db.getTasks()[0]
        assertEquals("2023-Q1", firstTask.task)
        assertEquals(2, firstTask.events.size)
        assertEquals("Event A", firstTask.events[0])
        assertEquals("Event B", firstTask.events[1])
    }

    @Test
    fun testSections() {
        val db = TimelineDb()
        val text = """
            timeline
            title History
            section Ancient
            3000 BC : Pyramids built
            section Modern
            2023 : AI revolution
        """.trimIndent()

        parser.parse(text, db)
        assertEquals(listOf("Ancient", "Modern"), db.getSections())
        assertEquals(2, db.getTasks().size)
        assertEquals("Ancient", db.getTasks()[0].section)
        assertEquals("Modern", db.getTasks()[1].section)
    }

    @Test
    fun testPeriodWithoutEvents() {
        val db = TimelineDb()
        val text = """
            timeline
            2023-Q1
            2023-Q2
        """.trimIndent()

        parser.parse(text, db)
        assertEquals(2, db.getTasks().size)
        assertTrue(db.getTasks()[0].events.isEmpty())
        assertTrue(db.getTasks()[1].events.isEmpty())
    }

    @Test
    fun testCommentsIgnored() {
        val db = TimelineDb()
        val text = """
            timeline
            %% This is a comment
            # This is also a comment
            title My Timeline
            2023 : Event
        """.trimIndent()

        parser.parse(text, db)
        assertEquals("My Timeline", db.getDiagramTitle())
        assertEquals(1, db.getTasks().size)
    }

    @Test
    fun testAccTitleAndDescr() {
        val db = TimelineDb()
        val text = """
            timeline
            accTitle: Timeline accessibility title
            accDescr: Timeline accessibility description
            2023 : Event
        """.trimIndent()

        parser.parse(text, db)
        assertEquals("Timeline accessibility title", db.getAccTitle())
        assertEquals("Timeline accessibility description", db.getAccDescription())
    }

    @Test
    fun testMultipleEventsPerPeriod() {
        val db = TimelineDb()
        val text = """
            timeline
            section Q1 2023
            January : New Year
                    : Winter Sale
                    : Team kickoff
            February : Valentine's Day
        """.trimIndent()

        parser.parse(text, db)
        assertEquals(2, db.getTasks().size)
        assertEquals(3, db.getTasks()[0].events.size)
        assertEquals(1, db.getTasks()[1].events.size)
    }

    @Test
    fun testGetTasksForSection() {
        val db = TimelineDb()
        val text = """
            timeline
            section Phase 1
            2023-Q1 : Event A
            2023-Q2 : Event B
            section Phase 2
            2023-Q3 : Event C
        """.trimIndent()

        parser.parse(text, db)
        assertEquals(2, db.getTasksForSection("Phase 1").size)
        assertEquals(1, db.getTasksForSection("Phase 2").size)
    }

    @Test
    fun testNoSectionDefaultsEmpty() {
        val db = TimelineDb()
        val text = """
            timeline
            2023 : Event
        """.trimIndent()

        parser.parse(text, db)
        assertEquals("", db.getTasks().first().section)
    }

    @Test
    fun testTitleParsing() {
        val db = TimelineDb()
        val text = """
            timeline
            title Project Milestones 2023
        """.trimIndent()

        parser.parse(text, db)
        assertEquals("Project Milestones 2023", db.getDiagramTitle())
    }
}
