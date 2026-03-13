package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.classDiagram.ClassDb
import io.lugf027.github.mermaid.core.diagram.classDiagram.ClassParser
import io.lugf027.github.mermaid.core.diagram.classDiagram.RelationType
import io.lugf027.github.mermaid.core.diagram.classDiagram.LineType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 类图解析器单元测试
 */
class ClassParserTest {

    private val parser = ClassParser()
    private fun parse(text: String): ClassDb {
        val db = ClassDb()
        parser.parse(text, db)
        return db
    }

    @Test
    fun testSimpleClassDeclaration() {
        val db = parse("""
            classDiagram
            class Animal
        """.trimIndent())
        assertTrue(db.getClasses().containsKey("Animal"))
    }

    @Test
    fun testMultipleClasses() {
        val db = parse("""
            classDiagram
            class Animal
            class Dog
            class Cat
        """.trimIndent())
        assertEquals(3, db.getClasses().size)
        assertTrue(db.getClasses().containsKey("Animal"))
        assertTrue(db.getClasses().containsKey("Dog"))
        assertTrue(db.getClasses().containsKey("Cat"))
    }

    @Test
    fun testClassWithMembers() {
        val db = parse("""
            classDiagram
            class Animal {
                +String name
                +int age
                +makeSound()
            }
        """.trimIndent())
        val animal = db.getClasses()["Animal"]!!
        assertEquals(2, animal.members.size)
        assertEquals(1, animal.methods.size)
        assertEquals("name", animal.members[0].id)
        assertEquals("+", animal.members[0].visibility)
        assertEquals("makeSound", animal.methods[0].id)
    }

    @Test
    fun testAnnotation() {
        val db = parse("""
            classDiagram
            class Shape
            <<interface>> Shape
        """.trimIndent())
        val shape = db.getClasses()["Shape"]!!
        assertTrue(shape.annotations.contains("interface"))
    }

    @Test
    fun testAnnotationInBody() {
        val db = parse("""
            classDiagram
            class Shape {
                <<interface>>
                +draw()
            }
        """.trimIndent())
        val shape = db.getClasses()["Shape"]!!
        assertTrue(shape.annotations.contains("interface"))
        assertEquals(1, shape.methods.size)
    }

    @Test
    fun testInheritanceRelation() {
        val db = parse("""
            classDiagram
            Animal <|-- Dog
        """.trimIndent())
        val relations = db.getRelations()
        assertEquals(1, relations.size)
        assertEquals("Animal", relations[0].id1)
        assertEquals("Dog", relations[0].id2)
        assertEquals(RelationType.EXTENSION, relations[0].relation.type1)
        assertEquals(LineType.LINE, relations[0].relation.lineType)
    }

    @Test
    fun testCompositionRelation() {
        val db = parse("""
            classDiagram
            Company *-- Employee
        """.trimIndent())
        val relations = db.getRelations()
        assertEquals(1, relations.size)
        assertEquals(RelationType.COMPOSITION, relations[0].relation.type1)
    }

    @Test
    fun testAggregationRelation() {
        val db = parse("""
            classDiagram
            Department o-- Employee
        """.trimIndent())
        val relations = db.getRelations()
        assertEquals(1, relations.size)
        assertEquals(RelationType.AGGREGATION, relations[0].relation.type1)
    }

    @Test
    fun testRelationWithLabel() {
        val db = parse("""
            classDiagram
            Customer --> Order : places
        """.trimIndent())
        val relations = db.getRelations()
        assertEquals(1, relations.size)
        assertEquals("places", relations[0].title)
    }

    @Test
    fun testDottedLineRelation() {
        val db = parse("""
            classDiagram
            Class1 ..> Class2 : depends on
        """.trimIndent())
        val relations = db.getRelations()
        assertEquals(1, relations.size)
        assertEquals(LineType.DOTTED_LINE, relations[0].relation.lineType)
        assertEquals("depends on", relations[0].title)
    }

    @Test
    fun testDirection() {
        val db = parse("""
            classDiagram
            direction LR
            class A
        """.trimIndent())
        assertEquals("LR", db.getDirection())
    }

    @Test
    fun testNoteFor() {
        val db = parse("""
            classDiagram
            class Animal
            note for Animal "This is a note"
        """.trimIndent())
        val notes = db.getNotes()
        assertEquals(1, notes.size)
        assertEquals("This is a note", notes[0].text)
        assertEquals("Animal", notes[0].forClass)
    }

    @Test
    fun testVisibilityMarkers() {
        val db = parse("""
            classDiagram
            class MyClass {
                +publicMethod()
                -privateAttr
                #protectedMethod()
                ~packageMethod()
            }
        """.trimIndent())
        val cls = db.getClasses()["MyClass"]!!
        assertEquals("+", cls.methods[0].visibility)
        assertEquals("-", cls.members[0].visibility)
        assertEquals("#", cls.methods[1].visibility)
        assertEquals("~", cls.methods[2].visibility)
    }

    @Test
    fun testMethodWithReturnType() {
        val db = parse("""
            classDiagram
            class MyClass {
                +getAge() int
            }
        """.trimIndent())
        val cls = db.getClasses()["MyClass"]!!
        assertEquals(1, cls.methods.size)
        assertEquals("getAge", cls.methods[0].id)
        assertEquals("int", cls.methods[0].returnType)
    }

    @Test
    fun testStaticAndAbstractClassifiers() {
        val db = parse("""
            classDiagram
            class MyClass {
                +staticMethod()$
                +abstractMethod()*
            }
        """.trimIndent())
        val cls = db.getClasses()["MyClass"]!!
        assertEquals(2, cls.methods.size)
    }

    @Test
    fun testTitle() {
        val db = parse("""
            classDiagram
            title My Class Diagram
            class A
        """.trimIndent())
        assertEquals("My Class Diagram", db.getDiagramTitle())
    }
}
