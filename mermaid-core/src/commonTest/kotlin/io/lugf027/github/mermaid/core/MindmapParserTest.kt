package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.mindmap.MindmapDb
import io.lugf027.github.mermaid.core.diagram.mindmap.MindmapNodeType
import io.lugf027.github.mermaid.core.diagram.mindmap.MindmapParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MindmapParserTest {
    private val parser = MindmapParser()

    @Test
    fun testSimpleMindmap() {
        val db = MindmapDb()
        val text = """
            mindmap
              root
                child1
                child2
        """.trimIndent()

        parser.parse(text, db)
        val root = db.getRootNode()
        assertNotNull(root)
        assertEquals("root", root.descr)
        assertEquals(2, root.children.size)
    }

    @Test
    fun testNestedMindmap() {
        val db = MindmapDb()
        val text = """
            mindmap
              root
                child1
                  grandchild1
                  grandchild2
                child2
        """.trimIndent()

        parser.parse(text, db)
        val root = db.getRootNode()
        assertNotNull(root)
        assertEquals(2, root.children.size)
        assertEquals(2, root.children[0].children.size)
        assertEquals("grandchild1", root.children[0].children[0].descr)
    }

    @Test
    fun testRectShape() {
        val db = MindmapDb()
        val text = """
            mindmap
              [Root]
                [Child]
        """.trimIndent()

        parser.parse(text, db)
        val root = db.getRootNode()
        assertNotNull(root)
        assertEquals(MindmapNodeType.RECT, root.type)
    }

    @Test
    fun testRoundedRectShape() {
        val db = MindmapDb()
        val text = """
            mindmap
              (Root)
                (Child)
        """.trimIndent()

        parser.parse(text, db)
        val root = db.getRootNode()
        assertNotNull(root)
        assertEquals(MindmapNodeType.ROUNDED_RECT, root.type)
    }

    @Test
    fun testCircleShape() {
        val db = MindmapDb()
        val text = """
            mindmap
              ((Root))
                ((Child))
        """.trimIndent()

        parser.parse(text, db)
        val root = db.getRootNode()
        assertNotNull(root)
        assertEquals(MindmapNodeType.CIRCLE, root.type)
    }

    @Test
    fun testCloudShape() {
        val db = MindmapDb()
        val text = """
            mindmap
              )Root(
        """.trimIndent()

        parser.parse(text, db)
        val root = db.getRootNode()
        assertNotNull(root)
        assertEquals(MindmapNodeType.CLOUD, root.type)
    }

    @Test
    fun testBangShape() {
        val db = MindmapDb()
        val text = """
            mindmap
              ))Root((
        """.trimIndent()

        parser.parse(text, db)
        val root = db.getRootNode()
        assertNotNull(root)
        assertEquals(MindmapNodeType.BANG, root.type)
    }

    @Test
    fun testHexagonShape() {
        val db = MindmapDb()
        val text = """
            mindmap
              {{Root}}
        """.trimIndent()

        parser.parse(text, db)
        val root = db.getRootNode()
        assertNotNull(root)
        assertEquals(MindmapNodeType.HEXAGON, root.type)
    }

    @Test
    fun testDefaultShape() {
        val db = MindmapDb()
        val text = """
            mindmap
              Root
        """.trimIndent()

        parser.parse(text, db)
        val root = db.getRootNode()
        assertNotNull(root)
        assertEquals(MindmapNodeType.DEFAULT, root.type)
    }

    @Test
    fun testSectionAssignment() {
        val db = MindmapDb()
        val text = """
            mindmap
              root
                A
                B
                C
        """.trimIndent()

        parser.parse(text, db)
        val root = db.getRootNode()
        assertNotNull(root)
        assertEquals(-1, root.section) // 根节点 section = -1
        assertEquals(0, root.children[0].section) // 第一个子节点 section = 0
        assertEquals(1, root.children[1].section) // 第二个子节点 section = 1
        assertEquals(2, root.children[2].section) // 第三个子节点 section = 2
    }

    @Test
    fun testDeepSectionInheritance() {
        val db = MindmapDb()
        val text = """
            mindmap
              root
                A
                  A1
                  A2
                B
                  B1
        """.trimIndent()

        parser.parse(text, db)
        val root = db.getRootNode()
        assertNotNull(root)
        val a = root.children[0]
        val b = root.children[1]
        assertEquals(a.section, a.children[0].section) // 子节点继承父节点 section
        assertEquals(b.section, b.children[0].section)
    }

    @Test
    fun testCommentsIgnored() {
        val db = MindmapDb()
        val text = """
            mindmap
              %% This is a comment
              root
                child1
        """.trimIndent()

        parser.parse(text, db)
        val root = db.getRootNode()
        assertNotNull(root)
        assertEquals("root", root.descr)
    }

    @Test
    fun testMixedShapes() {
        val db = MindmapDb()
        val text = """
            mindmap
              root
                [Rect]
                (Rounded)
                ((Circle))
        """.trimIndent()

        parser.parse(text, db)
        val root = db.getRootNode()
        assertNotNull(root)
        assertEquals(3, root.children.size)
        assertEquals(MindmapNodeType.RECT, root.children[0].type)
        assertEquals(MindmapNodeType.ROUNDED_RECT, root.children[1].type)
        assertEquals(MindmapNodeType.CIRCLE, root.children[2].type)
    }
}
