package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.detect.DetectorRegistry
import io.lugf027.github.mermaid.core.detect.DiagramTypeDetector
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 图表类型检测单元测试 - 测试 30 种图表类型的正则匹配
 */
class DetectorTest {

    @BeforeTest
    fun setup() {
        DetectorRegistry.registerBuiltinDetectors()
    }

    @Test
    fun testFlowchartDetection() {
        assertEquals("flowchart-v2", DiagramTypeDetector.detect("flowchart LR\n  A --> B"))
        assertEquals("flowchart-v2", DiagramTypeDetector.detect("flowchart TD\n  A --> B"))
    }

    @Test
    fun testGraphDetection() {
        // "graph" 默认也映射到 flowchart-v2（当 renderer = dagre-wrapper）
        val result = DiagramTypeDetector.detect("graph LR\n  A --> B")
        assertEquals("flowchart-v2", result)
    }

    @Test
    fun testPieDetection() {
        assertEquals("pie", DiagramTypeDetector.detect("pie\n  \"Dogs\" : 30"))
        assertEquals("pie", DiagramTypeDetector.detect("  pie\n  \"Cats\" : 70"))
        assertEquals("pie", DiagramTypeDetector.detect("pie showData\n  \"Dogs\" : 30"))
    }

    @Test
    fun testSequenceDetection() {
        assertEquals("sequence", DiagramTypeDetector.detect("sequenceDiagram\n  Alice->>Bob: Hello"))
    }

    @Test
    fun testClassDiagramDetection() {
        assertEquals("classDiagram", DiagramTypeDetector.detect("classDiagram\n  Animal <|-- Dog"))
    }

    @Test
    fun testStateDiagramDetection() {
        assertEquals("stateDiagram", DiagramTypeDetector.detect("stateDiagram-v2\n  [*] --> Active"))
    }

    @Test
    fun testErDiagramDetection() {
        assertEquals("er", DiagramTypeDetector.detect("erDiagram\n  CUSTOMER ||--o{ ORDER : places"))
    }

    @Test
    fun testGanttDetection() {
        assertEquals("gantt", DiagramTypeDetector.detect("gantt\n  title A Gantt Chart"))
    }

    @Test
    fun testGitGraphDetection() {
        assertEquals("gitGraph", DiagramTypeDetector.detect("gitGraph\n  commit"))
    }

    @Test
    fun testJourneyDetection() {
        assertEquals("journey", DiagramTypeDetector.detect("journey\n  title My Day"))
    }

    @Test
    fun testMindmapDetection() {
        assertEquals("mindmap", DiagramTypeDetector.detect("mindmap\n  Root"))
    }

    @Test
    fun testTimelineDetection() {
        assertEquals("timeline", DiagramTypeDetector.detect("timeline\n  2023 : Event"))
    }

    @Test
    fun testC4Detection() {
        assertEquals("c4", DiagramTypeDetector.detect("C4Context\n  Person(user, \"User\")"))
        assertEquals("c4", DiagramTypeDetector.detect("C4Container\n  System(sys, \"System\")"))
    }

    @Test
    fun testSankeyDetection() {
        assertEquals("sankey", DiagramTypeDetector.detect("sankey-beta\n  source,target,value"))
    }

    @Test
    fun testQuadrantChartDetection() {
        assertEquals("quadrantChart", DiagramTypeDetector.detect("quadrantChart\n  x-axis Low --> High"))
    }

    @Test
    fun testKanbanDetection() {
        assertEquals("kanban", DiagramTypeDetector.detect("kanban\n  column1"))
    }

    @Test
    fun testArchitectureDetection() {
        assertEquals("architecture", DiagramTypeDetector.detect("architecture\n  service api"))
    }

    @Test
    fun testUnknownDiagram() {
        assertEquals("error", DiagramTypeDetector.detect("unknown diagram type"))
    }

    @Test
    fun testEmptyInput() {
        assertEquals("error", DiagramTypeDetector.detect(""))
    }
}
