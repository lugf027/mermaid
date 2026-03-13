package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.architecture.ArchitectureDb
import io.lugf027.github.mermaid.core.diagram.architecture.ArchitectureParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArchitectureParserTest {

    private fun parse(text: String): ArchitectureDb {
        val db = ArchitectureDb()
        ArchitectureParser().parse(text, db)
        return db
    }

    @Test
    fun testBasicArchitecture() {
        val db = parse("""
            architecture-beta
            service api(server)[API Server]
            service db(database)[Database]
            api:R --> L:db
        """.trimIndent())

        val services = db.getServices()
        assertEquals(2, services.size)
        assertTrue(services.containsKey("api"))
        assertTrue(services.containsKey("db"))
        assertEquals("API Server", services["api"]!!.title)
        assertEquals("Database", services["db"]!!.title)

        val edges = db.getEdges()
        assertEquals(1, edges.size)
        assertEquals("api", edges[0].lhsId)
        assertEquals("db", edges[0].rhsId)
    }

    @Test
    fun testServiceWithIcon() {
        val db = parse("""
            architecture-beta
            service web(cloud)[Web Server]
        """.trimIndent())

        val services = db.getServices()
        assertEquals(1, services.size)
        assertEquals("cloud", services["web"]!!.icon)
        assertEquals("Web Server", services["web"]!!.title)
    }

    @Test
    fun testGroup() {
        val db = parse("""
            architecture-beta
            group cloud[Cloud Infrastructure]
            service api(server)[API] in cloud
            service db(database)[DB] in cloud
        """.trimIndent())

        val groups = db.getGroups()
        assertEquals(1, groups.size)
        assertEquals("Cloud Infrastructure", groups["cloud"]!!.title)

        val services = db.getServices()
        assertEquals("cloud", services["api"]!!.inGroup)
        assertEquals("cloud", services["db"]!!.inGroup)

        val children = db.getGroupChildren("cloud")
        assertEquals(2, children.size)
        assertTrue(children.contains("api"))
        assertTrue(children.contains("db"))
    }

    @Test
    fun testJunction() {
        val db = parse("""
            architecture-beta
            service a(server)[Service A]
            junction jn
            service b(server)[Service B]
            a:R --> L:jn
            jn:R --> L:b
        """.trimIndent())

        val junctions = db.getJunctions()
        assertEquals(1, junctions.size)
        assertTrue(junctions.containsKey("jn"))

        val edges = db.getEdges()
        assertEquals(2, edges.size)
    }

    @Test
    fun testEdgeDirections() {
        val db = parse("""
            architecture-beta
            service a(x)[A]
            service b(x)[B]
            a:T --> B:b
        """.trimIndent())

        val edges = db.getEdges()
        assertEquals(1, edges.size)
        assertEquals(ArchitectureDb.Direction.T, edges[0].lhsDir)
        assertEquals(ArchitectureDb.Direction.B, edges[0].rhsDir)
    }

    @Test
    fun testEdgeWithTitle() {
        val db = parse("""
            architecture-beta
            service a(x)[A]
            service b(x)[B]
            a:R - [HTTP] -> L:b
        """.trimIndent())

        val edges = db.getEdges()
        assertEquals(1, edges.size)
        assertEquals("HTTP", edges[0].title)
        assertTrue(edges[0].rhsInto) // > means arrow
    }

    @Test
    fun testBidirectionalEdge() {
        val db = parse("""
            architecture-beta
            service a(x)[A]
            service b(x)[B]
            a:R <--> L:b
        """.trimIndent())

        val edges = db.getEdges()
        assertEquals(1, edges.size)
        assertTrue(edges[0].lhsInto)  // < means arrow on left
        assertTrue(edges[0].rhsInto)  // > means arrow on right
    }

    @Test
    fun testNoArrowEdge() {
        val db = parse("""
            architecture-beta
            service a(x)[A]
            service b(x)[B]
            a:R -- L:b
        """.trimIndent())

        val edges = db.getEdges()
        assertEquals(1, edges.size)
        assertTrue(!edges[0].lhsInto)
        assertTrue(!edges[0].rhsInto)
    }

    @Test
    fun testCommentsSkipped() {
        val db = parse("""
            architecture-beta
            %% comment
            service s1(x)[Svc 1]
            %% another comment
            service s2(x)[Svc 2]
        """.trimIndent())

        assertEquals(2, db.getServices().size)
    }

    @Test
    fun testAccTitleAndDescr() {
        val db = parse("""
            architecture-beta
            accTitle: System Architecture
            accDescr: Overview of system components

            service api(server)[API]
        """.trimIndent())

        assertEquals("System Architecture", db.getAccTitle())
        assertEquals("Overview of system components", db.getAccDescription())
    }
}
