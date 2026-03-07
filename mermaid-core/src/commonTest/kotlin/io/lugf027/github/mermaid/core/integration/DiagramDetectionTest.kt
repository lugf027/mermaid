package io.lugf027.github.mermaid.core.integration

import io.lugf027.github.mermaid.core.core.MermaidKMP
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith

class DiagramDetectionTest {

    @Test
    fun detectsFlowchart() {
        val diagram = MermaidKMP.parse("flowchart TD\n  A --> B")
        assertNotNull(diagram)
    }

    @Test
    fun detectsGraph() {
        val diagram = MermaidKMP.parse("graph LR\n  A --> B")
        assertNotNull(diagram)
    }

    @Test
    fun detectsSequenceDiagram() {
        val diagram = MermaidKMP.parse("sequenceDiagram\n  Alice->>Bob: Hello")
        assertNotNull(diagram)
    }

    @Test
    fun detectsClassDiagram() {
        val diagram = MermaidKMP.parse("classDiagram\n  Animal <|-- Duck")
        assertNotNull(diagram)
    }

    @Test
    fun detectsStateDiagram() {
        val diagram = MermaidKMP.parse("stateDiagram-v2\n  [*] --> Still")
        assertNotNull(diagram)
    }

    @Test
    fun detectsErDiagram() {
        val diagram = MermaidKMP.parse("erDiagram\n  A ||--o{ B : has")
        assertNotNull(diagram)
    }

    @Test
    fun detectsGantt() {
        val diagram = MermaidKMP.parse("gantt\n  title Test\n  section S\n  A task :a1, 2024-01-01, 30d")
        assertNotNull(diagram)
    }

    @Test
    fun detectsPie() {
        val diagram = MermaidKMP.parse("pie\n  \"A\" : 10")
        assertNotNull(diagram)
    }

    @Test
    fun detectsGitGraph() {
        val diagram = MermaidKMP.parse("gitGraph\n  commit")
        assertNotNull(diagram)
    }

    @Test
    fun detectsMindmap() {
        val diagram = MermaidKMP.parse("mindmap\n  root((R))\n    A")
        assertNotNull(diagram)
    }

    @Test
    fun detectsTimeline() {
        val diagram = MermaidKMP.parse("timeline\n  2020 : Event A")
        assertNotNull(diagram)
    }

    @Test
    fun detectsJourney() {
        val diagram = MermaidKMP.parse("journey\n  title Test\n  section S\n  Do thing: 5: Me")
        assertNotNull(diagram)
    }

    @Test
    fun detectsInfo() {
        val diagram = MermaidKMP.parse("info")
        assertNotNull(diagram)
    }

    @Test
    fun rejectsInvalidInput() {
        val result = MermaidKMP.tryParse("this is not a valid diagram")
        assertNotNull(result)
    }

    @Test
    fun handlesEmptyInput() {
        assertFailsWith<Exception> {
            MermaidKMP.parse("")
        }
    }

    @Test
    fun handlesFrontmatter() {
        val diagram = MermaidKMP.parse("""
            ---
            title: My Diagram
            ---
            pie
                "A" : 10
        """.trimIndent())
        assertNotNull(diagram)
    }
}
