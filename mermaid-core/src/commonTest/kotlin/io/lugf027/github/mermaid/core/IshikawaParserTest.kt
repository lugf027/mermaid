package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.ishikawa.IshikawaDb
import io.lugf027.github.mermaid.core.diagram.ishikawa.IshikawaParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IshikawaParserTest {

    private fun parse(text: String): IshikawaDb {
        val db = IshikawaDb()
        IshikawaParser().parse(text, db)
        return db
    }

    @Test
    fun testBasicIshikawa() {
        val db = parse("""
            ishikawa-beta
              Quality Issue
                People
                  Lack of training
                Methods
                  No standard process
        """.trimIndent())

        val root = db.getRootNode()
        assertNotNull(root)
        assertEquals("Quality Issue", root.text)
        assertEquals(2, root.children.size)
        assertEquals("People", root.children[0].text)
        assertEquals("Methods", root.children[1].text)
    }

    @Test
    fun testCategoryWithMultipleCauses() {
        val db = parse("""
            ishikawa-beta
              Defect
                Materials
                  Low quality
                  Wrong specification
                  Expired stock
        """.trimIndent())

        val root = db.getRootNode()
        assertNotNull(root)
        val categories = db.getCategories()
        assertEquals(1, categories.size)
        assertEquals("Materials", categories[0].text)
        assertEquals(3, categories[0].children.size)
        assertEquals("Low quality", categories[0].children[0].text)
        assertEquals("Wrong specification", categories[0].children[1].text)
        assertEquals("Expired stock", categories[0].children[2].text)
    }

    @Test
    fun testMultipleCategories() {
        val db = parse("""
            ishikawa-beta
              Effect
                Cat1
                  Cause A
                Cat2
                  Cause B
                Cat3
                  Cause C
                Cat4
                  Cause D
        """.trimIndent())

        val categories = db.getCategories()
        assertEquals(4, categories.size)
        assertEquals("Cat1", categories[0].text)
        assertEquals("Cat2", categories[1].text)
        assertEquals("Cat3", categories[2].text)
        assertEquals("Cat4", categories[3].text)
    }

    @Test
    fun testWithoutBeta() {
        val db = parse("""
            ishikawa
              Problem
                Category
                  Root Cause
        """.trimIndent())

        val root = db.getRootNode()
        assertNotNull(root)
        assertEquals("Problem", root.text)
    }

    @Test
    fun testCommentsIgnored() {
        val db = parse("""
            ishikawa-beta
              %% This is a comment
              Effect
                Category
                  Cause
        """.trimIndent())

        val root = db.getRootNode()
        assertNotNull(root)
    }

    @Test
    fun testAccTitle() {
        val db = parse("""
            ishikawa-beta
              accTitle: Root Cause Analysis
              Problem
                Category
                  Cause
        """.trimIndent())

        assertEquals("Root Cause Analysis", db.getAccTitle())
    }

    @Test
    fun testEmptyDiagram() {
        val db = parse("""
            ishikawa-beta
        """.trimIndent())

        val root = db.getRootNode()
        assertNull(root)
    }

    @Test
    fun testEffectOnly() {
        val db = parse("""
            ishikawa-beta
              Problem Statement
        """.trimIndent())

        val root = db.getRootNode()
        assertNotNull(root)
        assertEquals("Problem Statement", root.text)
        assertTrue(root.children.isEmpty())
    }
}
