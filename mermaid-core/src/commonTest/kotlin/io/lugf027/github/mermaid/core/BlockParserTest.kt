package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.block.BlockDb
import io.lugf027.github.mermaid.core.diagram.block.BlockParser
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 块图解析器单元测试 - 对标 mermaid-js block.spec.ts
 */
class BlockParserTest {

    private lateinit var parser: BlockParser
    private lateinit var db: BlockDb

    @BeforeTest
    fun setup() {
        parser = BlockParser()
        db = BlockDb()
    }

    @Test
    fun testBasicBlockDiagram() {
        val input = """
            block-beta
            A["Block A"]
            B["Block B"]
        """.trimIndent()
        parser.parse(input, db)

        val children = db.getChildren("root")
        assertEquals(2, children.size)
        assertEquals("A", children[0].id)
        assertEquals("Block A", children[0].label)
        assertEquals("B", children[1].id)
        assertEquals("Block B", children[1].label)
    }

    @Test
    fun testColumns() {
        val input = """
            block-beta
            columns 3
            A["A"]
            B["B"]
            C["C"]
        """.trimIndent()
        parser.parse(input, db)

        assertEquals(3, db.getColumns())
        assertEquals(3, db.getChildren("root").size)
    }

    @Test
    fun testNodeWithSpan() {
        val input = """
            block-beta
            columns 3
            A["Wide Block"]:2
            B["Normal"]
        """.trimIndent()
        parser.parse(input, db)

        val children = db.getChildren("root")
        assertEquals(2, children.size)
        assertEquals(2, children[0].widthInColumns)
        assertEquals(1, children[1].widthInColumns)
    }

    @Test
    fun testSpace() {
        val input = """
            block-beta
            columns 3
            A["A"]
            space
            B["B"]
        """.trimIndent()
        parser.parse(input, db)

        val children = db.getChildren("root")
        assertEquals(3, children.size)
        assertEquals(BlockDb.BlockType.SPACE, children[1].type)
    }

    @Test
    fun testSpaceWithSpan() {
        val input = """
            block-beta
            columns 4
            A["A"]
            space:2
            B["B"]
        """.trimIndent()
        parser.parse(input, db)

        val children = db.getChildren("root")
        assertEquals(3, children.size)
        assertEquals(2, children[1].widthInColumns)
    }

    @Test
    fun testCompositeBlock() {
        val input = """
            block-beta
            group block:2
               A["Inner A"]
               B["Inner B"]
            end
            C["Outer"]
        """.trimIndent()
        parser.parse(input, db)

        val rootChildren = db.getChildren("root")
        assertEquals(2, rootChildren.size)
        assertEquals(BlockDb.BlockType.COMPOSITE, rootChildren[0].type)
        assertEquals("C", rootChildren[1].id)

        val innerChildren = db.getChildren("group")
        assertEquals(2, innerChildren.size)
        assertEquals("A", innerChildren[0].id)
    }

    @Test
    fun testEdge() {
        val input = """
            block-beta
            A["A"]
            B["B"]
            A --> B
        """.trimIndent()
        parser.parse(input, db)

        val edges = db.getEdges()
        assertEquals(1, edges.size)
        assertEquals("A", edges[0].source)
        assertEquals("B", edges[0].target)
        assertEquals("arrow_point", edges[0].arrowTypeEnd)
    }

    @Test
    fun testEdgeWithLabel() {
        val input = """
            block-beta
            A["A"]
            B["B"]
            A -- "label" --> B
        """.trimIndent()
        parser.parse(input, db)

        val edges = db.getEdges()
        assertEquals(1, edges.size)
        assertEquals("label", edges[0].label)
    }

    @Test
    fun testDottedEdge() {
        val input = """
            block-beta
            A["A"]
            B["B"]
            A -.-> B
        """.trimIndent()
        parser.parse(input, db)

        val edges = db.getEdges()
        assertEquals(1, edges.size)
        assertEquals("dotted", edges[0].lineType)
    }

    @Test
    fun testThickEdge() {
        val input = """
            block-beta
            A["A"]
            B["B"]
            A ==> B
        """.trimIndent()
        parser.parse(input, db)

        val edges = db.getEdges()
        assertEquals(1, edges.size)
        assertEquals("thick", edges[0].lineType)
    }

    @Test
    fun testClassDef() {
        val input = """
            block-beta
            classDef highlight fill:#f96,stroke:#333
            A["A"]
            class A highlight
        """.trimIndent()
        parser.parse(input, db)

        val classDefs = db.getClassDefs()
        assertTrue(classDefs.containsKey("highlight"))
        assertEquals("#f96", classDefs["highlight"]?.get("fill"))
    }

    @Test
    fun testRoundShape() {
        val input = """
            block-beta
            A("Round Block")
        """.trimIndent()
        parser.parse(input, db)

        val children = db.getChildren("root")
        assertEquals(1, children.size)
        assertEquals("stadium", children[0].shape)
    }

    @Test
    fun testCircleShape() {
        val input = """
            block-beta
            A(("Circle"))
        """.trimIndent()
        parser.parse(input, db)

        val children = db.getChildren("root")
        assertEquals(1, children.size)
        assertEquals("circle", children[0].shape)
    }

    @Test
    fun testDiamondShape() {
        val input = """
            block-beta
            A{"Diamond"}
        """.trimIndent()
        parser.parse(input, db)

        val children = db.getChildren("root")
        assertEquals(1, children.size)
        assertEquals("diamond", children[0].shape)
    }

    @Test
    fun testHexagonShape() {
        val input = """
            block-beta
            A{{"Hexagon"}}
        """.trimIndent()
        parser.parse(input, db)

        val children = db.getChildren("root")
        assertEquals(1, children.size)
        assertEquals("hexagon", children[0].shape)
    }

    @Test
    fun testBareId() {
        val input = """
            block-beta
            myBlock
        """.trimIndent()
        parser.parse(input, db)

        val children = db.getChildren("root")
        assertEquals(1, children.size)
        assertEquals("myBlock", children[0].id)
        assertEquals("myBlock", children[0].label)
    }

    @Test
    fun testCommentsSkipped() {
        val input = """
            block-beta
            %% comment
            A["A"]
            %% another comment
            B["B"]
        """.trimIndent()
        parser.parse(input, db)

        assertEquals(2, db.getChildren("root").size)
    }

    @Test
    fun testClearOnParse() {
        val input1 = """
            block-beta
            A["A"]
            B["B"]
        """.trimIndent()
        parser.parse(input1, db)
        assertEquals(2, db.getChildren("root").size)

        val input2 = """
            block-beta
            C["C"]
        """.trimIndent()
        parser.parse(input2, db)
        assertEquals(1, db.getChildren("root").size)
    }

    @Test
    fun testBlockBeta() {
        val input = """
            block-beta
            A["A"]
        """.trimIndent()
        parser.parse(input, db)

        assertEquals(1, db.getChildren("root").size)
    }

    @Test
    fun testEmptyBlock() {
        val input = """
            block-beta
        """.trimIndent()
        parser.parse(input, db)

        assertEquals(0, db.getChildren("root").size)
    }
}
