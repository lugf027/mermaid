package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.c4.C4Db
import io.lugf027.github.mermaid.core.diagram.c4.C4Parser
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * C4 图解析器单元测试 - 对标 mermaid-js c4.spec.ts
 */
class C4ParserTest {

    private lateinit var parser: C4Parser
    private lateinit var db: C4Db

    @BeforeTest
    fun setup() {
        parser = C4Parser()
        db = C4Db()
    }

    @Test
    fun testC4ContextType() {
        val input = """
            C4Context
        """.trimIndent()
        parser.parse(input, db)

        assertEquals("C4Context", db.getC4Type())
    }

    @Test
    fun testC4ContainerType() {
        val input = """
            C4Container
        """.trimIndent()
        parser.parse(input, db)

        assertEquals("C4Container", db.getC4Type())
    }

    @Test
    fun testPersonElement() {
        val input = """
            C4Context
            Person(user, "User", "A person")
        """.trimIndent()
        parser.parse(input, db)

        val shapes = db.getC4ShapeArray()
        assertEquals(1, shapes.size)
        assertEquals("user", shapes[0].alias)
        assertEquals("User", shapes[0].label)
        assertEquals("A person", shapes[0].descr)
        assertEquals("person", shapes[0].typeC4Shape)
    }

    @Test
    fun testPersonExtElement() {
        val input = """
            C4Context
            Person_Ext(user, "External User", "External person")
        """.trimIndent()
        parser.parse(input, db)

        val shapes = db.getC4ShapeArray()
        assertEquals(1, shapes.size)
        assertEquals("external_person", shapes[0].typeC4Shape)
    }

    @Test
    fun testSystemElement() {
        val input = """
            C4Context
            System(sys1, "System 1", "My system")
        """.trimIndent()
        parser.parse(input, db)

        val shapes = db.getC4ShapeArray()
        assertEquals(1, shapes.size)
        assertEquals("system", shapes[0].typeC4Shape)
        assertEquals("sys1", shapes[0].alias)
        assertEquals("System 1", shapes[0].label)
    }

    @Test
    fun testSystemExtElement() {
        val input = """
            C4Context
            System_Ext(ext, "External System", "An external system")
        """.trimIndent()
        parser.parse(input, db)

        val shapes = db.getC4ShapeArray()
        assertEquals("external_system", shapes[0].typeC4Shape)
    }

    @Test
    fun testContainerElement() {
        val input = """
            C4Container
            Container(web, "Web App", "Java", "Web application")
        """.trimIndent()
        parser.parse(input, db)

        val shapes = db.getC4ShapeArray()
        assertEquals(1, shapes.size)
        assertEquals("container", shapes[0].typeC4Shape)
        assertEquals("Web App", shapes[0].label)
        assertEquals("Java", shapes[0].techn)
        assertEquals("Web application", shapes[0].descr)
    }

    @Test
    fun testContainerDbElement() {
        val input = """
            C4Container
            ContainerDb(db, "Database", "PostgreSQL", "Stores data")
        """.trimIndent()
        parser.parse(input, db)

        val shapes = db.getC4ShapeArray()
        assertEquals("container_db", shapes[0].typeC4Shape)
    }

    @Test
    fun testComponentElement() {
        val input = """
            C4Component
            Component(comp1, "Component", "Spring MVC", "Handles requests")
        """.trimIndent()
        parser.parse(input, db)

        val shapes = db.getC4ShapeArray()
        assertEquals(1, shapes.size)
        assertEquals("component", shapes[0].typeC4Shape)
    }

    @Test
    fun testRelationship() {
        val input = """
            C4Context
            Person(user, "User", "A person")
            System(sys, "System", "My system")
            Rel(user, sys, "Uses", "HTTPS")
        """.trimIndent()
        parser.parse(input, db)

        val rels = db.getRels()
        assertEquals(1, rels.size)
        assertEquals("rel", rels[0].type)
        assertEquals("user", rels[0].from)
        assertEquals("sys", rels[0].to)
        assertEquals("Uses", rels[0].label)
        assertEquals("HTTPS", rels[0].techn)
    }

    @Test
    fun testBiRelationship() {
        val input = """
            C4Context
            Person(user, "User", "")
            System(sys, "System", "")
            BiRel(user, sys, "Communicates")
        """.trimIndent()
        parser.parse(input, db)

        val rels = db.getRels()
        assertEquals(1, rels.size)
        assertEquals("birel", rels[0].type)
    }

    @Test
    fun testDirectionalRelationships() {
        val input = """
            C4Context
            Person(a, "A", "")
            System(b, "B", "")
            Rel_Up(a, b, "Up")
            Rel_Down(a, b, "Down")
            Rel_Left(a, b, "Left")
            Rel_Right(a, b, "Right")
        """.trimIndent()
        parser.parse(input, db)

        val rels = db.getRels()
        assertEquals(4, rels.size)
        assertEquals("rel_u", rels[0].type)
        assertEquals("rel_d", rels[1].type)
        assertEquals("rel_l", rels[2].type)
        assertEquals("rel_r", rels[3].type)
    }

    @Test
    fun testBoundary() {
        val input = """
            C4Context
            Enterprise_Boundary(b0, "Enterprise") {
                Person(user, "User", "")
                System(sys, "System", "")
            }
        """.trimIndent()
        parser.parse(input, db)

        val shapes = db.getC4ShapeArray()
        assertEquals(2, shapes.size)
        // 元素在 boundary 内
        assertEquals("b0", shapes[0].parentBoundary)
        assertEquals("b0", shapes[1].parentBoundary)

        // boundary
        val boundaries = db.getBoundaries()
        assertTrue(boundaries.any { it.alias == "b0" && it.label == "Enterprise" })
    }

    @Test
    fun testNestedBoundary() {
        val input = """
            C4Context
            Enterprise_Boundary(outer, "Outer") {
                System_Boundary(inner, "Inner") {
                    System(sys, "System", "")
                }
            }
        """.trimIndent()
        parser.parse(input, db)

        val shapes = db.getC4ShapeArray()
        assertEquals(1, shapes.size)
        assertEquals("inner", shapes[0].parentBoundary)

        val boundaries = db.getBoundaries()
        val innerB = boundaries.find { it.alias == "inner" }
        assertTrue(innerB != null)
        assertEquals("outer", innerB!!.parentBoundary)
    }

    @Test
    fun testUpdateElementStyle() {
        val input = """
            C4Context
            Person(user, "User", "")
            UpdateElementStyle(user, ${"\""}#ff0000${"\""}, ${"\""}#ffffff${"\""}, ${"\""}#000000${"\""})
        """.trimIndent()
        parser.parse(input, db)

        val shape = db.getC4Shape("user")
        assertTrue(shape != null)
        assertEquals("#ff0000", shape!!.bgColor)
        assertEquals("#ffffff", shape.fontColor)
        assertEquals("#000000", shape.borderColor)
    }

    @Test
    fun testUpdateRelStyle() {
        val input = """
            C4Context
            Person(a, "A", "")
            System(b, "B", "")
            Rel(a, b, "Uses")
            UpdateRelStyle(a, b, ${"\""}#ff0000${"\""}, ${"\""}#0000ff${"\""})
        """.trimIndent()
        parser.parse(input, db)

        val rels = db.getRels()
        assertEquals(1, rels.size)
        assertEquals("#ff0000", rels[0].textColor)
        assertEquals("#0000ff", rels[0].lineColor)
    }

    @Test
    fun testUpdateLayoutConfig() {
        val input = """
            C4Context
            UpdateLayoutConfig(3, 1)
        """.trimIndent()
        parser.parse(input, db)

        assertEquals(3, db.getC4ShapeInRow())
        assertEquals(1, db.getC4BoundaryInRow())
    }

    @Test
    fun testTitle() {
        val input = """
            C4Context
            title My C4 Diagram
        """.trimIndent()
        parser.parse(input, db)

        assertEquals("My C4 Diagram", db.getDiagramTitle())
    }

    @Test
    fun testClearOnParse() {
        val input1 = """
            C4Context
            Person(a, "A", "")
        """.trimIndent()
        parser.parse(input1, db)
        assertEquals(1, db.getC4ShapeArray().size)

        val input2 = """
            C4Container
        """.trimIndent()
        parser.parse(input2, db)
        assertEquals(0, db.getC4ShapeArray().size)
        assertEquals("C4Container", db.getC4Type())
    }

    @Test
    fun testDeploymentNode() {
        val input = """
            C4Deployment
            Deployment_Node(cloud, "AWS", "Cloud") {
                Container(web, "Web App", "Java", "")
            }
        """.trimIndent()
        parser.parse(input, db)

        val shapes = db.getC4ShapeArray()
        assertEquals(1, shapes.size)
        assertEquals("cloud", shapes[0].parentBoundary)

        val boundaries = db.getBoundaries()
        assertTrue(boundaries.any { it.alias == "cloud" })
    }

    @Test
    fun testMultipleElements() {
        val input = """
            C4Context
            Person(user, "User", "Uses the system")
            System(webApp, "Web Application", "Delivers content")
            System(api, "API Service", "REST API")
            SystemDb(db, "Database", "Stores data")
            Rel(user, webApp, "Visits", "HTTPS")
            Rel(webApp, api, "Calls", "JSON/HTTPS")
            Rel(api, db, "Reads/Writes", "JDBC")
        """.trimIndent()
        parser.parse(input, db)

        assertEquals(4, db.getC4ShapeArray().size)
        assertEquals(3, db.getRels().size)
    }

    @Test
    fun testCommentsSkipped() {
        val input = """
            C4Context
            %% This is a comment
            Person(user, "User", "")
            %% Another comment
        """.trimIndent()
        parser.parse(input, db)

        assertEquals(1, db.getC4ShapeArray().size)
    }
}
