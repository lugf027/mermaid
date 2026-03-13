package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.requirement.RequirementDb
import io.lugf027.github.mermaid.core.diagram.requirement.RequirementParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RequirementParserTest {

    private fun parse(text: String): RequirementDb {
        val db = RequirementDb()
        RequirementParser().parse(text, db)
        return db
    }

    @Test
    fun testBasicRequirement() {
        val db = parse("""
            requirementDiagram

            requirement "Login Feature" {
                id: REQ001
                text: Users shall be able to log in
                risk: low
                verifyMethod: test
            }
        """.trimIndent())

        val reqs = db.getRequirements()
        assertEquals(1, reqs.size)
        assertTrue(reqs.containsKey("Login Feature"))
        val req = reqs["Login Feature"]!!
        assertEquals("REQ001", req.requirementId)
        assertEquals("Users shall be able to log in", req.text)
        assertEquals(RequirementDb.RiskLevel.LOW, req.risk)
        assertEquals(RequirementDb.VerifyMethod.TEST, req.verifyMethod)
        assertEquals(RequirementDb.RequirementType.REQUIREMENT, req.type)
    }

    @Test
    fun testFunctionalRequirement() {
        val db = parse("""
            requirementDiagram

            functionalRequirement "Search" {
                id: FR001
                text: System shall support search
                risk: medium
                verifyMethod: demonstration
            }
        """.trimIndent())

        val reqs = db.getRequirements()
        assertEquals(1, reqs.size)
        val req = reqs["Search"]!!
        assertEquals(RequirementDb.RequirementType.FUNCTIONAL, req.type)
        assertEquals(RequirementDb.RiskLevel.MEDIUM, req.risk)
        assertEquals(RequirementDb.VerifyMethod.DEMONSTRATION, req.verifyMethod)
    }

    @Test
    fun testElement() {
        val db = parse("""
            requirementDiagram

            element "Web App" {
                type: application
                docref: https://example.com
            }
        """.trimIndent())

        val elems = db.getElements()
        assertEquals(1, elems.size)
        assertTrue(elems.containsKey("Web App"))
        val elem = elems["Web App"]!!
        assertEquals("application", elem.type)
        assertEquals("https://example.com", elem.docRef)
    }

    @Test
    fun testRelationRightArrow() {
        val db = parse("""
            requirementDiagram

            requirement "Req A" {
                id: R1
            }
            requirement "Req B" {
                id: R2
            }

            Req_A - satisfies -> Req_B
        """.trimIndent())

        // Note: node names in relations are references, not necessarily same as quoted names
        val rels = db.getRelations()
        assertEquals(1, rels.size)
        assertEquals(RequirementDb.RelationType.SATISFIES, rels[0].type)
    }

    @Test
    fun testRelationLeftArrow() {
        val db = parse("""
            requirementDiagram

            requirement "MyReq" {
                id: R1
            }
            element "MyElem" {
                type: component
            }

            MyReq <- verifies - MyElem
        """.trimIndent())

        val rels = db.getRelations()
        assertEquals(1, rels.size)
        assertEquals(RequirementDb.RelationType.VERIFIES, rels[0].type)
        // Left arrow: dst is reversed - src=MyElem, dst=MyReq
        assertEquals("MyElem", rels[0].src)
        assertEquals("MyReq", rels[0].dst)
    }

    @Test
    fun testMultipleRequirementTypes() {
        val db = parse("""
            requirementDiagram

            requirement "R1" {
                id: 1
            }
            interfaceRequirement "R2" {
                id: 2
            }
            performanceRequirement "R3" {
                id: 3
            }
            physicalRequirement "R4" {
                id: 4
            }
            designConstraint "R5" {
                id: 5
            }
        """.trimIndent())

        val reqs = db.getRequirements()
        assertEquals(5, reqs.size)
        assertEquals(RequirementDb.RequirementType.REQUIREMENT, reqs["R1"]!!.type)
        assertEquals(RequirementDb.RequirementType.INTERFACE, reqs["R2"]!!.type)
        assertEquals(RequirementDb.RequirementType.PERFORMANCE, reqs["R3"]!!.type)
        assertEquals(RequirementDb.RequirementType.PHYSICAL, reqs["R4"]!!.type)
        assertEquals(RequirementDb.RequirementType.DESIGN_CONSTRAINT, reqs["R5"]!!.type)
    }

    @Test
    fun testAllRelationTypes() {
        val db = parse("""
            requirementDiagram

            A - contains -> B
            C - copies -> D
            E - derives -> F
            G - satisfies -> H
            I - verifies -> J
            K - refines -> L
            M - traces -> N
        """.trimIndent())

        val rels = db.getRelations()
        assertEquals(7, rels.size)
        assertEquals(RequirementDb.RelationType.CONTAINS, rels[0].type)
        assertEquals(RequirementDb.RelationType.COPIES, rels[1].type)
        assertEquals(RequirementDb.RelationType.DERIVES, rels[2].type)
        assertEquals(RequirementDb.RelationType.SATISFIES, rels[3].type)
        assertEquals(RequirementDb.RelationType.VERIFIES, rels[4].type)
        assertEquals(RequirementDb.RelationType.REFINES, rels[5].type)
        assertEquals(RequirementDb.RelationType.TRACES, rels[6].type)
    }

    @Test
    fun testCommentsAndAccTitle() {
        val db = parse("""
            requirementDiagram
            accTitle: System Requirements
            accDescr: Overview of system requirements

            %% This is a comment
            requirement "Main Req" {
                id: MR1
                text: Main requirement
            }
        """.trimIndent())

        assertEquals("System Requirements", db.getAccTitle())
        assertEquals("Overview of system requirements", db.getAccDescription())
        assertEquals(1, db.getRequirements().size)
    }

    @Test
    fun testDirection() {
        val db = parse("""
            requirementDiagram
            direction LR

            requirement "Test" {
                id: T1
            }
        """.trimIndent())

        assertEquals("LR", db.getDirection())
    }
}
