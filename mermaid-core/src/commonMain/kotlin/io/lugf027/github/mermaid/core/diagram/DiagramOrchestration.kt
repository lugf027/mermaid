package io.lugf027.github.mermaid.core.diagram

import io.lugf027.github.mermaid.core.detect.DetectorRegistry
import io.lugf027.github.mermaid.core.diagram.error.ErrorDiagram
import io.lugf027.github.mermaid.core.diagram.flowchart.FlowchartDiagram
import io.lugf027.github.mermaid.core.diagram.pie.PieDiagram
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

        // --- 第二批（待实现）---
        // sequence, class, state, er, gantt

        // --- 第三批（待实现）---
        // gitGraph, info, journey, c4, mindmap, timeline, sankey,
        // quadrantChart, xychart, requirement, block, packet,
        // kanban, architecture, radar, ishikawa, venn, treemap
    }
}
