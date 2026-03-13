package io.lugf027.github.mermaid.core.diagram

import io.lugf027.github.mermaid.core.detect.DetectorRegistry
import io.lugf027.github.mermaid.core.diagram.classDiagram.ClassDiagram
import io.lugf027.github.mermaid.core.diagram.er.ErDiagram
import io.lugf027.github.mermaid.core.diagram.error.ErrorDiagram
import io.lugf027.github.mermaid.core.diagram.flowchart.FlowchartDiagram
import io.lugf027.github.mermaid.core.diagram.gantt.GanttDiagram
import io.lugf027.github.mermaid.core.diagram.info.InfoDiagram
import io.lugf027.github.mermaid.core.diagram.journey.JourneyDiagram
import io.lugf027.github.mermaid.core.diagram.mindmap.MindmapDiagram
import io.lugf027.github.mermaid.core.diagram.pie.PieDiagram
import io.lugf027.github.mermaid.core.diagram.sequence.SequenceDiagram
import io.lugf027.github.mermaid.core.diagram.stateDiagram.StateDiagram
import io.lugf027.github.mermaid.core.diagram.timeline.TimelineDiagram
import io.lugf027.github.mermaid.core.diagram.gitGraph.GitGraphDiagram
import io.lugf027.github.mermaid.core.diagram.c4.C4Diagram
import io.lugf027.github.mermaid.core.diagram.block.BlockDiagram
import io.lugf027.github.mermaid.core.diagram.quadrantChart.QuadrantDiagram
import io.lugf027.github.mermaid.core.diagram.xychart.XYChartDiagram
import io.lugf027.github.mermaid.core.diagram.sankey.SankeyDiagram
import io.lugf027.github.mermaid.core.diagram.radar.RadarDiagram
import io.lugf027.github.mermaid.core.diagram.requirement.RequirementDiagram
import io.lugf027.github.mermaid.core.diagram.packet.PacketDiagram
import io.lugf027.github.mermaid.core.diagram.kanban.KanbanDiagram
import io.lugf027.github.mermaid.core.diagram.architecture.ArchitectureDiagram
import io.lugf027.github.mermaid.core.util.Logger

/**
 * 图表注册编排 - 对标 mermaid-js diagram-orchestration.ts
 *
 * 在应用启动时调用，注册所有图表类型的检测器和定义。
 * 注册顺序决定检测优先级。
 */
object DiagramOrchestration {

    private val log = Logger("DiagramOrchestration")

    /** 是否已初始化 */
    private var initialized = false

    /**
     * 注册所有内置图表
     *
     * 包含检测器注册和图表定义注册两个步骤。
     * 幂等操作，重复调用不会重复注册。
     */
    fun registerAll() {
        if (initialized) {
            log.debug("Already initialized, skipping")
            return
        }

        log.info("Registering all built-in diagrams")

        // 1. 注册检测器（决定文本匹配的优先级）
        DetectorRegistry.registerBuiltinDetectors()

        // 2. 注册图表定义
        registerDiagramDefinitions()

        initialized = true
        log.info("Registered ${DiagramRegistry.getRegisteredIds().size} diagram types")
    }

    /**
     * 重置所有注册（用于测试）
     */
    fun reset() {
        DetectorRegistry.clear()
        DiagramRegistry.clear()
        initialized = false
    }

    /**
     * 注册所有图表定义
     */
    private fun registerDiagramDefinitions() {
        // --- 第一批：核心图表 ---

        // Flowchart (v2 - 默认)
        val flowchartDef = FlowchartDiagram.definition()
        DiagramRegistry.register(flowchartDef)

        // Flowchart (legacy)
        val flowchartLegacy = FlowchartDiagram.legacyDefinition()
        DiagramRegistry.register(flowchartLegacy)

        // Pie
        val pieDef = PieDiagram.definition()
        DiagramRegistry.register(pieDef)

        // Error (fallback)
        val errorDef = ErrorDiagram.definition()
        DiagramRegistry.register(errorDef)

        // --- 第二批 ---

        // Sequence
        val sequenceDef = SequenceDiagram.definition()
        DiagramRegistry.register(sequenceDef)

        // ClassDiagram
        val classDef = ClassDiagram.definition()
        DiagramRegistry.register(classDef)

        // StateDiagram
        val stateDef = StateDiagram.definition()
        DiagramRegistry.register(stateDef)

        // ER Diagram
        val erDef = ErDiagram.definition()
        DiagramRegistry.register(erDef)

        // Gantt
        val ganttDef = GanttDiagram.definition()
        DiagramRegistry.register(ganttDef)

        // --- 第三批 3a ---

        // Info
        val infoDef = InfoDiagram.definition()
        DiagramRegistry.register(infoDef)

        // Journey
        val journeyDef = JourneyDiagram.definition()
        DiagramRegistry.register(journeyDef)

        // Mindmap
        val mindmapDef = MindmapDiagram.definition()
        DiagramRegistry.register(mindmapDef)

        // Timeline
        val timelineDef = TimelineDiagram.definition()
        DiagramRegistry.register(timelineDef)

        // --- 第三批 3b ---

        // GitGraph
        val gitGraphDef = GitGraphDiagram.definition()
        DiagramRegistry.register(gitGraphDef)

        // C4
        val c4Def = C4Diagram.definition()
        DiagramRegistry.register(c4Def)

        // Block
        val blockDef = BlockDiagram.definition()
        DiagramRegistry.register(blockDef)

        // --- 第三批 3c ---

        // QuadrantChart
        val quadrantDef = QuadrantDiagram.definition()
        DiagramRegistry.register(quadrantDef)

        // XYChart
        val xyChartDef = XYChartDiagram.definition()
        DiagramRegistry.register(xyChartDef)

        // Sankey
        val sankeyDef = SankeyDiagram.definition()
        DiagramRegistry.register(sankeyDef)

        // Radar
        val radarDef = RadarDiagram.definition()
        DiagramRegistry.register(radarDef)

        // --- 第三批 3d ---

        // Requirement
        val requirementDef = RequirementDiagram.definition()
        DiagramRegistry.register(requirementDef)

        // Packet
        val packetDef = PacketDiagram.definition()
        DiagramRegistry.register(packetDef)

        // Kanban
        val kanbanDef = KanbanDiagram.definition()
        DiagramRegistry.register(kanbanDef)

        // Architecture
        val archDef = ArchitectureDiagram.definition()
        DiagramRegistry.register(archDef)

        // --- 第三批 3e（待实现）---
        // ishikawa, venn, treemap, flowchart-elk
    }
}
