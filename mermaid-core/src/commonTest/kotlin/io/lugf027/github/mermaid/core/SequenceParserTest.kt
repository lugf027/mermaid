package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.sequence.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 时序图解析器单元测试 - 对标 mermaid-js sequenceDiagram.spec.ts
 */
class SequenceParserTest {

    private lateinit var parser: SequenceParser
    private lateinit var db: SequenceDb

    @BeforeTest
    fun setup() {
        parser = SequenceParser()
        db = SequenceDb()
    }

    @Test
    fun testBasicSequence() {
        val input = """
            sequenceDiagram
                Alice->>Bob: Hello Bob
                Bob-->>Alice: Hi Alice
        """.trimIndent()
        parser.parse(input, db)

        val actors = db.getActorKeys()
        assertEquals(2, actors.size)
        assertEquals("Alice", actors[0])
        assertEquals("Bob", actors[1])

        val messages = db.getMessages().filter { LineType.isMessage(it.type) }
        assertEquals(2, messages.size)
        assertEquals("Hello Bob", messages[0].message)
        assertEquals(LineType.SOLID, messages[0].type)
        assertEquals("Hi Alice", messages[1].message)
        assertEquals(LineType.DOTTED, messages[1].type)
    }

    @Test
    fun testParticipantDeclaration() {
        val input = """
            sequenceDiagram
                participant A as Alice
                participant B as Bob
                A->>B: Hello
        """.trimIndent()
        parser.parse(input, db)

        val actors = db.getActors()
        assertEquals(2, actors.size)
        assertEquals("Alice", actors["A"]?.description)
        assertEquals("Bob", actors["B"]?.description)
    }

    @Test
    fun testActorTypes() {
        val input = """
            sequenceDiagram
                actor A as Alice
                participant B as Bob
                database D as DB
                A->>B: Hello
                B->>D: Query
        """.trimIndent()
        parser.parse(input, db)

        val actors = db.getActors()
        assertEquals(ParticipantType.ACTOR, actors["A"]?.type)
        assertEquals(ParticipantType.PARTICIPANT, actors["B"]?.type)
        assertEquals(ParticipantType.DATABASE, actors["D"]?.type)
    }

    @Test
    fun testArrowTypes() {
        val input = """
            sequenceDiagram
                A->>B: Solid with arrowhead
                A-->>B: Dotted with arrowhead
                A-xB: Solid with cross
                A--xB: Dotted with cross
                A-)B: Solid with dot
                A--)B: Dotted with dot
                A->B: Solid open
                A-->B: Dotted open
        """.trimIndent()
        parser.parse(input, db)

        val messages = db.getMessages().filter { LineType.isMessage(it.type) }
        assertEquals(8, messages.size)
        assertEquals(LineType.SOLID, messages[0].type)
        assertEquals(LineType.DOTTED, messages[1].type)
        assertEquals(LineType.SOLID_CROSS, messages[2].type)
        assertEquals(LineType.DOTTED_CROSS, messages[3].type)
        assertEquals(LineType.SOLID_POINT, messages[4].type)
        assertEquals(LineType.DOTTED_POINT, messages[5].type)
        assertEquals(LineType.SOLID_OPEN, messages[6].type)
        assertEquals(LineType.DOTTED_OPEN, messages[7].type)
    }

    @Test
    fun testNoteLeftOf() {
        val input = """
            sequenceDiagram
                Alice->>Bob: Hello
                note left of Alice: Alice thinks
        """.trimIndent()
        parser.parse(input, db)

        val notes = db.getNotes()
        assertEquals(1, notes.size)
        assertEquals("Alice", notes[0].actor)
        assertEquals(Placement.LEFTOF, notes[0].placement)
        assertEquals("Alice thinks", notes[0].message)
    }

    @Test
    fun testNoteRightOf() {
        val input = """
            sequenceDiagram
                Alice->>Bob: Hello
                note right of Bob: Bob receives
        """.trimIndent()
        parser.parse(input, db)

        val notes = db.getNotes()
        assertEquals(1, notes.size)
        assertEquals("Bob", notes[0].actor)
        assertEquals(Placement.RIGHTOF, notes[0].placement)
        assertEquals("Bob receives", notes[0].message)
    }

    @Test
    fun testNoteOver() {
        val input = """
            sequenceDiagram
                Alice->>Bob: Hello
                note over Alice,Bob: Shared note
        """.trimIndent()
        parser.parse(input, db)

        val notes = db.getNotes()
        assertEquals(1, notes.size)
        assertEquals(Placement.OVER, notes[0].placement)
        assertEquals("Shared note", notes[0].message)
    }

    @Test
    fun testLoop() {
        val input = """
            sequenceDiagram
                Alice->>Bob: Hello
                loop Every minute
                    Bob->>Alice: Ping
                end
        """.trimIndent()
        parser.parse(input, db)

        val messages = db.getMessages()
        val loopStarts = messages.filter { it.type == LineType.LOOP_START }
        val loopEnds = messages.filter { it.type == LineType.LOOP_END }
        assertEquals(1, loopStarts.size)
        assertEquals("Every minute", loopStarts[0].message)
        assertEquals(1, loopEnds.size)
    }

    @Test
    fun testAltElse() {
        val input = """
            sequenceDiagram
                Alice->>Bob: Hello
                alt Is busy
                    Bob->>Alice: Sorry, busy
                else Is free
                    Bob->>Alice: Hi there!
                end
        """.trimIndent()
        parser.parse(input, db)

        val messages = db.getMessages()
        val altStarts = messages.filter { it.type == LineType.ALT_START }
        val altElses = messages.filter { it.type == LineType.ALT_ELSE }
        val altEnds = messages.filter { it.type == LineType.ALT_END }
        assertEquals(1, altStarts.size)
        assertEquals("Is busy", altStarts[0].message)
        assertEquals(1, altElses.size)
        assertEquals("Is free", altElses[0].message)
        assertEquals(1, altEnds.size)
    }

    @Test
    fun testOpt() {
        val input = """
            sequenceDiagram
                Alice->>Bob: Hello
                opt Extra response
                    Bob->>Alice: Thanks!
                end
        """.trimIndent()
        parser.parse(input, db)

        val messages = db.getMessages()
        val optStarts = messages.filter { it.type == LineType.OPT_START }
        assertEquals(1, optStarts.size)
        assertEquals("Extra response", optStarts[0].message)
    }

    @Test
    fun testPar() {
        val input = """
            sequenceDiagram
                par Alice to Bob
                    Alice->>Bob: Hello
                and Alice to John
                    Alice->>John: Hello
                end
        """.trimIndent()
        parser.parse(input, db)

        val messages = db.getMessages()
        val parStarts = messages.filter { it.type == LineType.PAR_START }
        val parAnds = messages.filter { it.type == LineType.PAR_AND }
        assertEquals(1, parStarts.size)
        assertEquals(1, parAnds.size)
    }

    @Test
    fun testCritical() {
        val input = """
            sequenceDiagram
                critical Establish connection
                    Alice->>Bob: Connect
                option Timeout
                    Alice->>Alice: Retry
                end
        """.trimIndent()
        parser.parse(input, db)

        val messages = db.getMessages()
        val critStarts = messages.filter { it.type == LineType.CRITICAL_START }
        val critOptions = messages.filter { it.type == LineType.CRITICAL_OPTION }
        assertEquals(1, critStarts.size)
        assertEquals(1, critOptions.size)
    }

    @Test
    fun testActivateDeactivate() {
        val input = """
            sequenceDiagram
                Alice->>Bob: Hello
                activate Bob
                Bob->>Alice: Hi
                deactivate Bob
        """.trimIndent()
        parser.parse(input, db)

        val messages = db.getMessages()
        val activeStarts = messages.filter { it.type == LineType.ACTIVE_START }
        val activeEnds = messages.filter { it.type == LineType.ACTIVE_END }
        assertEquals(1, activeStarts.size)
        assertEquals(1, activeEnds.size)
    }

    @Test
    fun testAutoNumber() {
        val input = """
            sequenceDiagram
                autonumber
                Alice->>Bob: Hello
                Bob->>Alice: Hi
        """.trimIndent()
        parser.parse(input, db)

        assertTrue(db.isSequenceNumbersEnabled())
        assertEquals(1, db.getAutoNumberStart())
        assertEquals(1, db.getAutoNumberStep())
    }

    @Test
    fun testAutoNumberWithParams() {
        val input = """
            sequenceDiagram
                autonumber 10 5
                Alice->>Bob: Hello
        """.trimIndent()
        parser.parse(input, db)

        assertTrue(db.isSequenceNumbersEnabled())
        assertEquals(10, db.getAutoNumberStart())
        assertEquals(5, db.getAutoNumberStep())
    }

    @Test
    fun testTitle() {
        val input = """
            sequenceDiagram
                title My Sequence Diagram
                Alice->>Bob: Hello
        """.trimIndent()
        parser.parse(input, db)

        assertEquals("My Sequence Diagram", db.getDiagramTitle())
    }

    @Test
    fun testAccTitle() {
        val input = """
            sequenceDiagram
                accTitle: Accessible Title
                Alice->>Bob: Hello
        """.trimIndent()
        parser.parse(input, db)

        assertEquals("Accessible Title", db.getAccTitle())
    }

    @Test
    fun testAccDescr() {
        val input = """
            sequenceDiagram
                accDescr: A description
                Alice->>Bob: Hello
        """.trimIndent()
        parser.parse(input, db)

        assertEquals("A description", db.getAccDescription())
    }

    @Test
    fun testComments() {
        val input = """
            sequenceDiagram
                %% This is a comment
                Alice->>Bob: Hello
                %% Another comment
                Bob->>Alice: Hi
        """.trimIndent()
        parser.parse(input, db)

        val messages = db.getMessages().filter { LineType.isMessage(it.type) }
        assertEquals(2, messages.size)
    }

    @Test
    fun testSelfMessage() {
        val input = """
            sequenceDiagram
                Alice->>Alice: Thinking...
        """.trimIndent()
        parser.parse(input, db)

        val messages = db.getMessages().filter { LineType.isMessage(it.type) }
        assertEquals(1, messages.size)
        assertEquals("Alice", messages[0].from)
        assertEquals("Alice", messages[0].to)
        assertEquals("Thinking...", messages[0].message)
    }

    @Test
    fun testEmptySequence() {
        val input = "sequenceDiagram"
        parser.parse(input, db)

        assertTrue(db.getActorKeys().isEmpty())
        assertTrue(db.getMessages().isEmpty())
    }

    @Test
    fun testImplicitActorCreation() {
        val input = """
            sequenceDiagram
                Alice->>Bob: Hello
                Bob->>Charlie: Forward
        """.trimIndent()
        parser.parse(input, db)

        val actors = db.getActorKeys()
        assertEquals(3, actors.size)
        assertEquals("Alice", actors[0])
        assertEquals("Bob", actors[1])
        assertEquals("Charlie", actors[2])
    }

    @Test
    fun testInvalidHeader() {
        val input = """
            notASequenceDiagram
                Alice->>Bob: Hello
        """.trimIndent()
        assertFailsWith<IllegalArgumentException> {
            parser.parse(input, db)
        }
    }

    @Test
    fun testBreak() {
        val input = """
            sequenceDiagram
                Alice->>Bob: Hello
                break When error
                    Bob->>Alice: Error!
                end
        """.trimIndent()
        parser.parse(input, db)

        val messages = db.getMessages()
        val breakStarts = messages.filter { it.type == LineType.BREAK_START }
        assertEquals(1, breakStarts.size)
        assertEquals("When error", breakStarts[0].message)
    }

    @Test
    fun testBox() {
        val input = """
            sequenceDiagram
                box Green Internal
                    participant A
                    participant B
                end
                A->>B: Hello
        """.trimIndent()
        parser.parse(input, db)

        val boxes = db.getBoxes()
        assertEquals(1, boxes.size)
    }

    @Test
    fun testDbClear() {
        val input = """
            sequenceDiagram
                Alice->>Bob: Hello
        """.trimIndent()
        parser.parse(input, db)
        assertEquals(2, db.getActorKeys().size)

        // 再次解析应清除之前的状态
        db.clear()
        assertEquals(0, db.getActorKeys().size)
        assertEquals(0, db.getMessages().size)
    }
}
