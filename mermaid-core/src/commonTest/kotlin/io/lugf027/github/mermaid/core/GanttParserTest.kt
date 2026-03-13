package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.gantt.GanttDb
import io.lugf027.github.mermaid.core.diagram.gantt.GanttParser
import kotlin.test.*

class GanttParserTest {

    private lateinit var db: GanttDb
    private lateinit var parser: GanttParser

    @BeforeTest
    fun setup() {
        db = GanttDb()
        parser = GanttParser()
    }

    @Test
    fun testSimpleGantt() {
        val text = """
            gantt
            title Simple Gantt
            dateFormat YYYY-MM-DD
            section Design
            Task A :a1, 2024-01-01, 2024-01-10
        """.trimIndent()

        parser.parse(text, db)

        assertEquals("Simple Gantt", db.getDiagramTitle())
        assertEquals("YYYY-MM-DD", db.getDateFormat())
        assertEquals(listOf("Design"), db.getSections())

        val tasks = db.getTasks()
        assertEquals(1, tasks.size)
        assertEquals("a1", tasks[0].id)
        assertEquals("Task A", tasks[0].description)
        assertEquals("Design", tasks[0].section)
    }

    @Test
    fun testMultipleSections() {
        val text = """
            gantt
            dateFormat YYYY-MM-DD
            section Design
            Task A :a1, 2024-01-01, 2024-01-10
            section Development
            Task B :b1, 2024-01-11, 2024-01-20
            section Testing
            Task C :c1, 2024-01-21, 2024-01-30
        """.trimIndent()

        parser.parse(text, db)

        assertEquals(listOf("Design", "Development", "Testing"), db.getSections())
        val tasks = db.getTasks()
        assertEquals(3, tasks.size)
        assertEquals("Design", tasks[0].section)
        assertEquals("Development", tasks[1].section)
        assertEquals("Testing", tasks[2].section)
    }

    @Test
    fun testTaskStatusTags() {
        val text = """
            gantt
            dateFormat YYYY-MM-DD
            section Tasks
            Done task     :done, d1, 2024-01-01, 2024-01-05
            Active task   :active, a1, 2024-01-06, 2024-01-10
            Critical task :crit, c1, 2024-01-11, 2024-01-15
            Milestone     :milestone, m1, 2024-01-16, 0d
            Combo         :done, crit, x1, 2024-01-17, 2024-01-20
        """.trimIndent()

        parser.parse(text, db)

        val tasks = db.getTasks()
        assertEquals(5, tasks.size)

        assertTrue(tasks[0].done)
        assertFalse(tasks[0].active)
        assertEquals("d1", tasks[0].id)

        assertTrue(tasks[1].active)
        assertFalse(tasks[1].done)
        assertEquals("a1", tasks[1].id)

        assertTrue(tasks[2].crit)
        assertEquals("c1", tasks[2].id)

        assertTrue(tasks[3].milestone)
        assertEquals("m1", tasks[3].id)

        assertTrue(tasks[4].done)
        assertTrue(tasks[4].crit)
        assertEquals("x1", tasks[4].id)
    }

    @Test
    fun testDurationFormat() {
        val text = """
            gantt
            dateFormat YYYY-MM-DD
            section Tasks
            Task with duration :t1, 2024-01-01, 5d
        """.trimIndent()

        parser.parse(text, db)

        val tasks = db.getTasks()
        assertEquals(1, tasks.size)
        assertEquals("t1", tasks[0].id)

        // 5 天的持续时间
        val expectedDuration = 5 * GanttDb.DAY_MS
        assertEquals(expectedDuration, tasks[0].endTime - tasks[0].startTime)
    }

    @Test
    fun testAfterDependency() {
        val text = """
            gantt
            dateFormat YYYY-MM-DD
            section Tasks
            Task A :a1, 2024-01-01, 2024-01-10
            Task B :b1, after a1, 5d
        """.trimIndent()

        parser.parse(text, db)

        val tasks = db.getTasks()
        assertEquals(2, tasks.size)

        // Task B 的开始时间应该等于 Task A 的结束时间
        assertEquals(tasks[0].endTime, tasks[1].startTime)
    }

    @Test
    fun testAxisFormat() {
        val text = """
            gantt
            dateFormat YYYY-MM-DD
            axisFormat %m/%d
            section Tasks
            Task A :a1, 2024-01-01, 2024-01-10
        """.trimIndent()

        parser.parse(text, db)
        assertEquals("%m/%d", db.getAxisFormat())
    }

    @Test
    fun testExcludes() {
        val text = """
            gantt
            dateFormat YYYY-MM-DD
            excludes weekends, 2024-01-01
            section Tasks
            Task A :a1, 2024-01-02, 2024-01-10
        """.trimIndent()

        parser.parse(text, db)

        val excludes = db.getExcludes()
        assertEquals(2, excludes.size)
        assertTrue("weekends" in excludes)
        assertTrue("2024-01-01" in excludes)
    }

    @Test
    fun testIncludes() {
        val text = """
            gantt
            dateFormat YYYY-MM-DD
            excludes weekends
            includes 2024-01-06
            section Tasks
            Task A :a1, 2024-01-01, 2024-01-10
        """.trimIndent()

        parser.parse(text, db)

        val includes = db.getIncludes()
        assertEquals(1, includes.size)
        assertEquals("2024-01-06", includes[0])
    }

    @Test
    fun testTodayMarker() {
        val text = """
            gantt
            todayMarker off
            section Tasks
            Task A :a1, 2024-01-01, 2024-01-10
        """.trimIndent()

        parser.parse(text, db)
        assertEquals("off", db.getTodayMarker())
    }

    @Test
    fun testTickInterval() {
        val text = """
            gantt
            tickInterval 1week
            section Tasks
            Task A :a1, 2024-01-01, 2024-01-10
        """.trimIndent()

        parser.parse(text, db)
        assertEquals("1week", db.getTickInterval())
    }

    @Test
    fun testInclusiveEndDates() {
        val text = """
            gantt
            dateFormat YYYY-MM-DD
            inclusiveEndDates
            section Tasks
            Task A :a1, 2024-01-01, 2024-01-10
        """.trimIndent()

        parser.parse(text, db)
        assertTrue(db.isInclusiveEndDates())
    }

    @Test
    fun testTopAxis() {
        val text = """
            gantt
            topAxis
            section Tasks
            Task A :a1, 2024-01-01, 2024-01-10
        """.trimIndent()

        parser.parse(text, db)
        assertTrue(db.isTopAxis())
    }

    @Test
    fun testWeekday() {
        val text = """
            gantt
            weekday monday
            section Tasks
            Task A :a1, 2024-01-01, 2024-01-10
        """.trimIndent()

        parser.parse(text, db)
        assertEquals("monday", db.getWeekday())
    }

    @Test
    fun testAutoId() {
        val text = """
            gantt
            dateFormat YYYY-MM-DD
            section Tasks
            First task  :2024-01-01, 5d
            Second task :2024-01-06, 3d
        """.trimIndent()

        parser.parse(text, db)

        val tasks = db.getTasks()
        assertEquals(2, tasks.size)
        assertEquals("task1", tasks[0].id)
        assertEquals("task2", tasks[1].id)
    }

    @Test
    fun testComments() {
        val text = """
            gantt
            %% This is a comment
            dateFormat YYYY-MM-DD
            section Tasks
            %% Another comment
            Task A :a1, 2024-01-01, 2024-01-10
        """.trimIndent()

        parser.parse(text, db)

        val tasks = db.getTasks()
        assertEquals(1, tasks.size)
        assertEquals("a1", tasks[0].id)
    }

    @Test
    fun testAccTitle() {
        val text = """
            gantt
            accTitle: My Gantt Chart
            section Tasks
            Task A :a1, 2024-01-01, 2024-01-10
        """.trimIndent()

        parser.parse(text, db)
        assertEquals("My Gantt Chart", db.getAccTitle())
    }

    @Test
    fun testParseDuration() {
        assertEquals(GanttDb.DAY_MS, GanttDb.parseDuration("1d"))
        assertEquals(5 * GanttDb.DAY_MS, GanttDb.parseDuration("5d"))
        assertEquals(GanttDb.WEEK_MS, GanttDb.parseDuration("1w"))
        assertEquals(2 * GanttDb.HOUR_MS, GanttDb.parseDuration("2h"))
        assertEquals(30 * GanttDb.MINUTE_MS, GanttDb.parseDuration("30m"))
        assertNull(GanttDb.parseDuration("invalid"))
    }

    @Test
    fun testParseDate() {
        assertNotNull(GanttDb.parseDate("2024-01-15"))
        assertNotNull(GanttDb.parseDate("2023-12-31"))
        assertNull(GanttDb.parseDate("not-a-date"))
        assertNull(GanttDb.parseDate("2024-13-01")) // invalid month
    }

    @Test
    fun testTaskOrder() {
        val text = """
            gantt
            dateFormat YYYY-MM-DD
            section A
            Task 1 :t1, 2024-01-01, 5d
            Task 2 :t2, 2024-01-06, 3d
            section B
            Task 3 :t3, 2024-01-09, 5d
        """.trimIndent()

        parser.parse(text, db)

        val tasks = db.getTasks()
        assertEquals(3, tasks.size)
        assertEquals(0, tasks[0].order)
        assertEquals(1, tasks[1].order)
        assertEquals(2, tasks[2].order)
    }
}
