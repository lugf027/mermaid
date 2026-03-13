package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.kanban.KanbanDb
import io.lugf027.github.mermaid.core.diagram.kanban.KanbanParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KanbanParserTest {

    private fun parse(text: String): KanbanDb {
        val db = KanbanDb()
        KanbanParser().parse(text, db)
        return db
    }

    @Test
    fun testBasicKanban() {
        val db = parse("""
kanban
Todo
  task1[Create feature]
  task2[Write tests]
Done
  task3[Review PR]
        """.trimIndent())

        val nodes = db.getNodes()
        assertTrue(nodes.size >= 5) // 2 sections + 3 items
        val sections = db.getSections()
        assertTrue(sections.size >= 2)
    }

    @Test
    fun testSections() {
        val db = parse("""
kanban
Backlog
Todo
In Progress
Done
        """.trimIndent())

        val sections = db.getSections()
        assertEquals(4, sections.size)
        assertEquals("Backlog", sections[0].label)
        assertEquals("Todo", sections[1].label)
        assertEquals("In Progress", sections[2].label)
        assertEquals("Done", sections[3].label)
    }

    @Test
    fun testItemsInSection() {
        val db = parse("""
kanban
Todo
  item1[First task]
  item2[Second task]
Done
  item3[Completed task]
        """.trimIndent())

        val sections = db.getSections()
        assertTrue(sections.isNotEmpty())
        val todoSection = sections.firstOrNull { it.label == "Todo" }
        assertTrue(todoSection != null)

        val todoItems = db.getItemsBySection(todoSection!!.id)
        assertEquals(2, todoItems.size)
    }

    @Test
    fun testNodeWithIdAndLabel() {
        val db = parse("""
kanban
Column1
  myTask[Important task]
        """.trimIndent())

        val nodes = db.getNodes()
        val item = nodes.find { it.label == "Important task" }
        assertTrue(item != null)
        assertEquals("myTask", item!!.id)
        assertEquals(1, item.level)
    }

    @Test
    fun testPureTextNodes() {
        val db = parse("""
kanban
Simple Column
  Simple item
        """.trimIndent())

        val nodes = db.getNodes()
        assertTrue(nodes.size >= 2)
        val section = nodes.first { it.isSection }
        assertEquals("Simple Column", section.label)
    }

    @Test
    fun testCommentsSkipped() {
        val db = parse("""
kanban
%% This is a comment
Todo
  %% Another comment
  task1[Do something]
        """.trimIndent())

        val sections = db.getSections()
        assertEquals(1, sections.size)
        assertEquals("Todo", sections[0].label)
    }

    @Test
    fun testAccTitleAndDescr() {
        val db = parse("""
kanban
accTitle: Project Board
accDescr: Shows project tasks

Todo
  t1[Task one]
        """.trimIndent())

        assertEquals("Project Board", db.getAccTitle())
        assertEquals("Shows project tasks", db.getAccDescription())
    }

    @Test
    fun testMultipleColumnsWithItems() {
        val db = parse("""
kanban
Backlog
  b1[Research]
  b2[Design]
In Progress
  p1[Implement]
Review
  r1[Code Review]
Done
  d1[Deploy]
  d2[Monitor]
        """.trimIndent())

        val sections = db.getSections()
        assertEquals(4, sections.size)

        val allNodes = db.getNodes()
        val items = allNodes.filter { !it.isSection }
        assertEquals(6, items.size)
    }
}
