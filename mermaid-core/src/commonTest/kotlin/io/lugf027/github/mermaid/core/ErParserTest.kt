package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.er.ErDb
import io.lugf027.github.mermaid.core.diagram.er.ErParser
import io.lugf027.github.mermaid.core.diagram.er.Cardinality
import io.lugf027.github.mermaid.core.diagram.er.Identification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ER 图解析器单元测试
 */
class ErParserTest {

    private val parser = ErParser()
    private fun parse(text: String): ErDb {
        val db = ErDb()
        parser.parse(text, db)
        return db
    }

    @Test
    fun testSimpleRelationship() {
        val db = parse("""
            erDiagram
            CUSTOMER ||--o{ ORDER : places
        """.trimIndent())
        assertEquals(2, db.getEntities().size)
        assertTrue(db.getEntities().containsKey("CUSTOMER"))
        assertTrue(db.getEntities().containsKey("ORDER"))
        assertEquals(1, db.getRelationships().size)
        assertEquals("places", db.getRelationships()[0].roleLabel)
    }

    @Test
    fun testMultipleRelationships() {
        val db = parse("""
            erDiagram
            CUSTOMER ||--o{ ORDER : places
            ORDER ||--|{ LINE-ITEM : contains
            PRODUCT ||--o{ LINE-ITEM : "ordered in"
        """.trimIndent())
        assertEquals(4, db.getEntities().size)
        assertEquals(3, db.getRelationships().size)
    }

    @Test
    fun testEntityWithAttributes() {
        val db = parse("""
            erDiagram
            CUSTOMER {
                string name
                int age
                string email PK
            }
        """.trimIndent())
        val customer = db.getEntities()["CUSTOMER"]!!
        assertEquals(3, customer.attributes.size)
        assertEquals("name", customer.attributes[0].name)
        assertEquals("string", customer.attributes[0].type)
        assertEquals("email", customer.attributes[2].name)
        assertTrue(customer.attributes[2].keys.contains("PK"))
    }

    @Test
    fun testEntityWithComment() {
        val db = parse("""
            erDiagram
            CUSTOMER {
                string name "The customer name"
            }
        """.trimIndent())
        val customer = db.getEntities()["CUSTOMER"]!!
        assertEquals("The customer name", customer.attributes[0].comment)
    }

    @Test
    fun testEntityWithFK() {
        val db = parse("""
            erDiagram
            ORDER {
                int id PK
                int customerId FK
                string status
            }
        """.trimIndent())
        val order = db.getEntities()["ORDER"]!!
        assertTrue(order.attributes[0].keys.contains("PK"))
        assertTrue(order.attributes[1].keys.contains("FK"))
    }

    @Test
    fun testIdentifyingRelationship() {
        val db = parse("""
            erDiagram
            PARENT ||--|{ CHILD : has
        """.trimIndent())
        val rel = db.getRelationships()[0]
        assertEquals(Identification.IDENTIFYING, rel.relSpec.relType)
    }

    @Test
    fun testNonIdentifyingRelationship() {
        val db = parse("""
            erDiagram
            PARENT ||..o{ CHILD : has
        """.trimIndent())
        val rel = db.getRelationships()[0]
        assertEquals(Identification.NON_IDENTIFYING, rel.relSpec.relType)
    }

    @Test
    fun testCardinalityOnlyOne() {
        val db = parse("""
            erDiagram
            A ||--|| B : rel
        """.trimIndent())
        val rel = db.getRelationships()[0]
        assertEquals(Cardinality.ONLY_ONE, rel.relSpec.cardA)
        assertEquals(Cardinality.ONLY_ONE, rel.relSpec.cardB)
    }

    @Test
    fun testCardinalityZeroOrMore() {
        val db = parse("""
            erDiagram
            A ||--o{ B : rel
        """.trimIndent())
        val rel = db.getRelationships()[0]
        assertEquals(Cardinality.ONLY_ONE, rel.relSpec.cardA)
        assertEquals(Cardinality.ZERO_OR_MORE, rel.relSpec.cardB)
    }

    @Test
    fun testEntityAlias() {
        val db = parse("""
            erDiagram
            CUST["Customer Entity"]
        """.trimIndent())
        val cust = db.getEntities()["CUST"]!!
        assertEquals("Customer Entity", cust.alias)
    }

    @Test
    fun testDirection() {
        val db = parse("""
            erDiagram
            direction LR
            A ||--|| B : rel
        """.trimIndent())
        assertEquals("LR", db.getDirection())
    }

    @Test
    fun testTitle() {
        val db = parse("""
            erDiagram
            title My ER Diagram
            A ||--|| B : rel
        """.trimIndent())
        assertEquals("My ER Diagram", db.getDiagramTitle())
    }

    @Test
    fun testSimpleEntityDeclaration() {
        val db = parse("""
            erDiagram
            CUSTOMER
            ORDER
        """.trimIndent())
        assertEquals(2, db.getEntities().size)
    }

    @Test
    fun testMixedDeclarations() {
        val db = parse("""
            erDiagram
            CUSTOMER {
                string name PK
            }
            CUSTOMER ||--o{ ORDER : places
            ORDER {
                int id PK
                int customerId FK
            }
        """.trimIndent())
        assertEquals(2, db.getEntities().size)
        assertEquals(1, db.getRelationships().size)
        assertEquals(1, db.getEntities()["CUSTOMER"]!!.attributes.size)
        assertEquals(2, db.getEntities()["ORDER"]!!.attributes.size)
    }

    @Test
    fun testComments() {
        val db = parse("""
            erDiagram
            %% This is a comment
            CUSTOMER ||--o{ ORDER : places
        """.trimIndent())
        assertEquals(1, db.getRelationships().size)
    }
}
