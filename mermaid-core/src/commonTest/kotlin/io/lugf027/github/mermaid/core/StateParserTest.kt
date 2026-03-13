package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.stateDiagram.StateDb
import io.lugf027.github.mermaid.core.diagram.stateDiagram.StateParser
import io.lugf027.github.mermaid.core.diagram.stateDiagram.StateType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 状态图解析器单元测试
 */
class StateParserTest {

    private val parser = StateParser()
    private fun parse(text: String): StateDb {
        val db = StateDb()
        parser.parse(text, db)
        return db
    }

    @Test
    fun testSimpleTransition() {
        val db = parse("""
            stateDiagram-v2
            Idle --> Moving
        """.trimIndent())
        val transitions = db.getTransitions()
        assertEquals(1, transitions.size)
        assertEquals("Idle", transitions[0].from)
        assertEquals("Moving", transitions[0].to)
    }

    @Test
    fun testTransitionWithLabel() {
        val db = parse("""
            stateDiagram-v2
            Idle --> Moving : start moving
        """.trimIndent())
        val transitions = db.getTransitions()
        assertEquals(1, transitions.size)
        assertEquals("start moving", transitions[0].label)
    }

    @Test
    fun testStartState() {
        val db = parse("""
            stateDiagram-v2
            [*] --> Idle
        """.trimIndent())
        val transitions = db.getTransitions()
        assertEquals(1, transitions.size)
        val startState = db.getStates()[transitions[0].from]
        assertTrue(startState != null)
        assertEquals(StateType.START, startState.type)
    }

    @Test
    fun testEndState() {
        val db = parse("""
            stateDiagram-v2
            Idle --> [*]
        """.trimIndent())
        val transitions = db.getTransitions()
        assertEquals(1, transitions.size)
        val endState = db.getStates()[transitions[0].to]
        assertTrue(endState != null)
        assertEquals(StateType.END, endState.type)
    }

    @Test
    fun testStateWithDescription() {
        val db = parse("""
            stateDiagram-v2
            Idle : Waiting for input
        """.trimIndent())
        val states = db.getStates()
        assertTrue(states.containsKey("Idle"))
        assertEquals("Waiting for input", states["Idle"]!!.descriptions[0])
    }

    @Test
    fun testStateAlias() {
        val db = parse("""
            stateDiagram-v2
            state "Ready State" as Ready
        """.trimIndent())
        val states = db.getStates()
        assertTrue(states.containsKey("Ready"))
        assertEquals("Ready State", states["Ready"]!!.alias)
    }

    @Test
    fun testForkState() {
        val db = parse("""
            stateDiagram-v2
            state forkPoint <<fork>>
        """.trimIndent())
        val states = db.getStates()
        assertTrue(states.containsKey("forkPoint"))
        assertEquals(StateType.FORK, states["forkPoint"]!!.type)
    }

    @Test
    fun testJoinState() {
        val db = parse("""
            stateDiagram-v2
            state joinPoint <<join>>
        """.trimIndent())
        val states = db.getStates()
        assertTrue(states.containsKey("joinPoint"))
        assertEquals(StateType.JOIN, states["joinPoint"]!!.type)
    }

    @Test
    fun testChoiceState() {
        val db = parse("""
            stateDiagram-v2
            state choicePoint <<choice>>
        """.trimIndent())
        val states = db.getStates()
        assertTrue(states.containsKey("choicePoint"))
        assertEquals(StateType.CHOICE, states["choicePoint"]!!.type)
    }

    @Test
    fun testBracketNotation() {
        val db = parse("""
            stateDiagram-v2
            state forkPoint [[fork]]
        """.trimIndent())
        val states = db.getStates()
        assertTrue(states.containsKey("forkPoint"))
        assertEquals(StateType.FORK, states["forkPoint"]!!.type)
    }

    @Test
    fun testCompositeState() {
        val db = parse("""
            stateDiagram-v2
            state MyComposite {
                InnerA --> InnerB
            }
        """.trimIndent())
        val states = db.getStates()
        assertTrue(states.containsKey("MyComposite"))
        assertTrue(states["MyComposite"]!!.isComposite)
    }

    @Test
    fun testNoteRightOf() {
        val db = parse("""
            stateDiagram-v2
            Idle
            note right of Idle : This is a note
        """.trimIndent())
        val states = db.getStates()
        assertTrue(states["Idle"]!!.note != null)
        assertEquals("This is a note", states["Idle"]!!.note!!.text)
        assertEquals("right of", states["Idle"]!!.note!!.position)
    }

    @Test
    fun testNoteLeftOf() {
        val db = parse("""
            stateDiagram-v2
            Idle
            note left of Idle : Left note
        """.trimIndent())
        val states = db.getStates()
        assertEquals("left of", states["Idle"]!!.note!!.position)
    }

    @Test
    fun testDirection() {
        val db = parse("""
            stateDiagram-v2
            direction LR
            Idle --> Moving
        """.trimIndent())
        assertEquals("LR", db.getDirection())
    }

    @Test
    fun testMultipleTransitions() {
        val db = parse("""
            stateDiagram-v2
            [*] --> Idle
            Idle --> Moving : start
            Moving --> Idle : stop
            Moving --> [*]
        """.trimIndent())
        assertEquals(4, db.getTransitions().size)
    }

    @Test
    fun testCompositeWithAlias() {
        val db = parse("""
            stateDiagram-v2
            state "Moving State" as Moving {
                Walk --> Run
            }
        """.trimIndent())
        val states = db.getStates()
        assertTrue(states.containsKey("Moving"))
        assertEquals("Moving State", states["Moving"]!!.alias)
        assertTrue(states["Moving"]!!.isComposite)
    }

    @Test
    fun testClassDef() {
        val db = parse("""
            stateDiagram-v2
            classDef highlight fill:#f00,stroke:#333
            Idle
        """.trimIndent())
        // ClassDef should parse without error
        assertTrue(db.getStates().containsKey("Idle"))
    }

    @Test
    fun testHideEmptyDescription() {
        val db = parse("""
            stateDiagram-v2
            hide empty description
            Idle --> Moving
        """.trimIndent())
        assertEquals(1, db.getTransitions().size)
    }

    @Test
    fun testTitle() {
        val db = parse("""
            stateDiagram-v2
            title My State Diagram
            Idle --> Moving
        """.trimIndent())
        assertEquals("My State Diagram", db.getDiagramTitle())
    }
}
