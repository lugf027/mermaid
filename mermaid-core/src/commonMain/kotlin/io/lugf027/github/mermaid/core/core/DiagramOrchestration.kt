package io.lugf027.github.mermaid.core.core

import io.lugf027.github.mermaid.core.types.DiagramTypeId

/**
 * 图表编排器。
 * 注册所有内置图表类型的 detector 和 loader。
 * 对应 mermaid-js 的 diagram-orchestration.ts。
 *
 * 每种图表类型通过正则表达式检测，按需懒加载。
 */
object DiagramOrchestration {

    /**
     * 注册所有内置图表类型。
     */
    fun registerAll() {
        // ─── Pie 饼图 ─────────────────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.PIE,
            detector = { text, _ -> text.trimStart().startsWith("pie", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.pie.PieDiagramDefinition()
            }
        )

        // ─── Flowchart 流程图 ─────────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.FLOWCHART,
            detector = { text, _ ->
                val first = text.trimStart().lines().firstOrNull()?.trim() ?: ""
                first.matches(Regex("""^(flowchart|graph)\s.*""", RegexOption.IGNORE_CASE)) ||
                    first.matches(Regex("""^(flowchart|graph)\s*$""", RegexOption.IGNORE_CASE))
            },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.flowchart.FlowchartDiagramDefinition()
            }
        )

        // ─── Sequence 时序图 ──────────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.SEQUENCE,
            detector = { text, _ -> text.trimStart().startsWith("sequenceDiagram", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.sequence.SequenceDiagramDefinition()
            }
        )

        // ─── Class 类图 ──────────────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.CLASS,
            detector = { text, _ -> text.trimStart().startsWith("classDiagram", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.classdiagram.ClassDiagramDefinition()
            }
        )

        // ─── State 状态图 ────────────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.STATE,
            detector = { text, _ -> text.trimStart().startsWith("stateDiagram", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.state.StateDiagramDefinition()
            }
        )

        // ─── ER 实体关系图 ───────────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.ER,
            detector = { text, _ -> text.trimStart().startsWith("erDiagram", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.er.ErDiagramDefinition()
            }
        )

        // ─── Gantt 甘特图 ───────────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.GANTT,
            detector = { text, _ -> text.trimStart().startsWith("gantt", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.gantt.GanttDiagramDefinition()
            }
        )

        // ─── Git Graph ──────────────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.GIT,
            detector = { text, _ -> text.trimStart().startsWith("gitGraph", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.git.GitDiagramDefinition()
            }
        )

        // ─── Mindmap 思维导图 ────────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.MINDMAP,
            detector = { text, _ -> text.trimStart().startsWith("mindmap", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.mindmap.MindmapDiagramDefinition()
            }
        )

        // ─── Timeline 时间线 ────────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.TIMELINE,
            detector = { text, _ -> text.trimStart().startsWith("timeline", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.timeline.TimelineDiagramDefinition()
            }
        )

        // ─── Kanban 看板 ─────────────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.KANBAN,
            detector = { text, _ -> text.trimStart().startsWith("kanban", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.kanban.KanbanDiagramDefinition()
            }
        )

        // ─── C4 架构图 ──────────────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.C4,
            detector = { text, _ ->
                val first = text.trimStart().lines().firstOrNull()?.trim() ?: ""
                first.startsWith("C4Context", ignoreCase = true) ||
                    first.startsWith("C4Container", ignoreCase = true) ||
                    first.startsWith("C4Component", ignoreCase = true) ||
                    first.startsWith("C4Dynamic", ignoreCase = true) ||
                    first.startsWith("C4Deployment", ignoreCase = true)
            },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.c4.C4DiagramDefinition()
            }
        )

        // ─── Quadrant Chart 象限图 ──────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.QUADRANT,
            detector = { text, _ -> text.trimStart().startsWith("quadrantChart", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.quadrant.QuadrantDiagramDefinition()
            }
        )

        // ─── XY Chart ──────────────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.XY_CHART,
            detector = { text, _ -> text.trimStart().startsWith("xychart-beta", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.xychart.XyChartDiagramDefinition()
            }
        )

        // ─── Requirement 需求图 ─────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.REQUIREMENT,
            detector = { text, _ -> text.trimStart().startsWith("requirementDiagram", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.requirement.RequirementDiagramDefinition()
            }
        )

        // ─── Journey 用户旅程图 ─────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.JOURNEY,
            detector = { text, _ -> text.trimStart().startsWith("journey", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.journey.JourneyDiagramDefinition()
            }
        )

        // ─── Sankey 桑基图 ──────────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.SANKEY,
            detector = { text, _ -> text.trimStart().startsWith("sankey", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.sankey.SankeyDiagramDefinition()
            }
        )

        // ─── Block 块状图 ───────────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.BLOCK,
            detector = { text, _ -> text.trimStart().startsWith("block-beta", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.block.BlockDiagramDefinition()
            }
        )

        // ─── Packet ─────────────────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.PACKET,
            detector = { text, _ -> text.trimStart().startsWith("packet-beta", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.packet.PacketDiagramDefinition()
            }
        )

        // ─── Architecture 架构图 ────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.ARCHITECTURE,
            detector = { text, _ -> text.trimStart().startsWith("architecture", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.architecture.ArchitectureDiagramDefinition()
            }
        )

        // ─── Info ───────────────────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.INFO,
            detector = { text, _ -> text.trimStart().startsWith("info", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.info.InfoDiagramDefinition()
            }
        )

        // ─── Radar 雷达图 ───────────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.RADAR,
            detector = { text, _ -> text.trimStart().startsWith("radar", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.radar.RadarDiagramDefinition()
            }
        )

        // ─── Ishikawa 鱼骨图 ────────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.ISHIKAWA,
            detector = { text, _ -> text.trimStart().startsWith("ishikawa", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.ishikawa.IshikawaDiagramDefinition()
            }
        )

        // ─── Venn 韦恩图 ────────────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.VENN,
            detector = { text, _ -> text.trimStart().startsWith("venn", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.venn.VennDiagramDefinition()
            }
        )

        // ─── Treemap 矩形树图 ───────────────────────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.TREEMAP,
            detector = { text, _ -> text.trimStart().startsWith("treemap", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.treemap.TreemapDiagramDefinition()
            }
        )

        // ─── Error（内部错误渲染，始终最后注册） ─────────────
        DiagramRegistry.registerDetector(
            id = DiagramTypeId.ERROR,
            detector = { text, _ -> text.trimStart().startsWith("error", ignoreCase = true) },
            loader = {
                io.lugf027.github.mermaid.core.diagrams.error.ErrorDiagramDefinition()
            }
        )
    }
}
