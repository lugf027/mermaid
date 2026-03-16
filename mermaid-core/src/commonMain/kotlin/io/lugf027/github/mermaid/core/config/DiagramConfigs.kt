package io.lugf027.github.mermaid.core.config

import kotlinx.serialization.Serializable

/**
 * 图表基础配置接口 - 对标 mermaid-js BaseDiagramConfig
 */
interface BaseDiagramConfig {
    val useWidth: Int?
    val useMaxWidth: Boolean?
}

/** 流程图配置 - 对标 FlowchartDiagramConfig */
@Serializable
data class FlowchartDiagramConfig(
    val titleTopMargin: Int? = 25,
    val diagramPadding: Int? = 8,
    override val useMaxWidth: Boolean? = true,
    override val useWidth: Int? = null,
    val htmlLabels: Boolean? = null,
    val nodeSpacing: Int? = 50,
    val rankSpacing: Int? = 50,
    val curve: String? = "rounded",
    val padding: Int? = 15,
    val defaultRenderer: String? = "dagre-wrapper",
    val wrappingWidth: Int? = 200,
    val inheritDir: Boolean? = false,
) : BaseDiagramConfig

/** 时序图配置 - 对标 SequenceDiagramConfig */
@Serializable
data class SequenceDiagramConfig(
    override val useMaxWidth: Boolean? = true,
    override val useWidth: Int? = null,
    val hideUnusedParticipants: Boolean? = false,
    val activationWidth: Int? = 10,
    val diagramMarginX: Int? = 50,
    val diagramMarginY: Int? = 10,
    val actorMargin: Int? = 50,
    val width: Int? = 150,
    val height: Int? = 65,
    val boxMargin: Int? = 10,
    val boxTextMargin: Int? = 5,
    val noteMargin: Int? = 10,
    val messageMargin: Int? = 35,
    val messageAlign: String? = "center",
    val mirrorActors: Boolean? = true,
    val bottomMarginAdj: Int? = 1,
    val rightAngles: Boolean? = false,
    val showSequenceNumbers: Boolean? = false,
    val actorFontSize: Int? = 14,
    val actorFontFamily: String? = "\"Open Sans\", sans-serif",
    val actorFontWeight: Int? = 400,
    val noteFontSize: Int? = 14,
    val noteFontFamily: String? = "\"trebuchet ms\", verdana, arial, sans-serif",
    val noteFontWeight: Int? = 400,
    val noteAlign: String? = "center",
    val messageFontSize: Int? = 16,
    val messageFontFamily: String? = "\"trebuchet ms\", verdana, arial, sans-serif",
    val messageFontWeight: Int? = 400,
    val wrap: Boolean? = false,
    val wrapPadding: Int? = 10,
    val labelBoxWidth: Int? = 50,
    val labelBoxHeight: Int? = 20,
) : BaseDiagramConfig

/** 甘特图配置 - 对标 GanttDiagramConfig */
@Serializable
data class GanttDiagramConfig(
    val titleTopMargin: Int? = 25,
    override val useMaxWidth: Boolean? = true,
    override val useWidth: Int? = null,
    val barHeight: Int? = 20,
    val barGap: Int? = 4,
    val topPadding: Int? = 50,
    val rightPadding: Int? = 75,
    val leftPadding: Int? = 75,
    val gridLineStartPadding: Int? = 35,
    val fontSize: Int? = 11,
    val sectionFontSize: Int? = 11,
    val numberSectionStyles: Int? = 4,
    val axisFormat: String? = "%Y-%m-%d",
    val topAxis: Boolean? = false,
    val displayMode: String? = "",
    val weekday: String? = "sunday",
) : BaseDiagramConfig

/** 用户旅程图配置 - 对标 JourneyDiagramConfig */
@Serializable
data class JourneyDiagramConfig(
    override val useMaxWidth: Boolean? = true,
    override val useWidth: Int? = null,
    val diagramMarginX: Int? = 50,
    val diagramMarginY: Int? = 10,
    val leftMargin: Int? = 150,
    val width: Int? = 150,
    val height: Int? = 50,
    val boxMargin: Int? = 10,
    val boxTextMargin: Int? = 5,
    val noteMargin: Int? = 10,
    val messageMargin: Int? = 35,
    val messageAlign: String? = "center",
    val bottomMarginAdj: Int? = 1,
    val rightAngles: Boolean? = false,
    val taskFontSize: Int? = 14,
    val taskFontFamily: String? = "\"trebuchet ms\", verdana, arial, sans-serif",
    val taskMargin: Int? = 50,
    val activationWidth: Int? = 10,
    val textPlacement: String? = "fo",
) : BaseDiagramConfig

/** 时间线配置 - 对标 TimelineDiagramConfig */
@Serializable
data class TimelineDiagramConfig(
    override val useMaxWidth: Boolean? = true,
    override val useWidth: Int? = null,
    val diagramMarginX: Int? = 50,
    val diagramMarginY: Int? = 10,
    val leftMargin: Int? = 150,
    val width: Int? = 150,
    val height: Int? = 50,
    val boxMargin: Int? = 10,
    val boxTextMargin: Int? = 5,
    val noteMargin: Int? = 10,
    val messageMargin: Int? = 35,
    val messageAlign: String? = "center",
    val bottomMarginAdj: Int? = 1,
    val rightAngles: Boolean? = false,
    val taskFontSize: Int? = 14,
    val taskFontFamily: String? = "\"trebuchet ms\", verdana, arial, sans-serif",
    val padding: Int? = 8,
    val disableMulticolor: Boolean? = false,
) : BaseDiagramConfig

/** 类图配置 - 对标 ClassDiagramConfig */
@Serializable
data class ClassDiagramConfig(
    val titleTopMargin: Int? = 25,
    override val useMaxWidth: Boolean? = true,
    override val useWidth: Int? = null,
    val dividerMargin: Int? = 10,
    val padding: Int? = 5,
    val textHeight: Int? = 10,
    val defaultRenderer: String? = "dagre-wrapper",
    val htmlLabels: Boolean? = false,
    val hideEmptyMembersBox: Boolean? = false,
    val nodeSpacing: Int? = 50,
    val rankSpacing: Int? = 50,
    val diagramPadding: Int? = 8,
) : BaseDiagramConfig

/** 状态图配置 - 对标 StateDiagramConfig */
@Serializable
data class StateDiagramConfig(
    val titleTopMargin: Int? = 25,
    override val useMaxWidth: Boolean? = true,
    override val useWidth: Int? = null,
    val dividerMargin: Int? = 10,
    val sizeUnit: Int? = 5,
    val padding: Int? = 8,
    val textHeight: Int? = 10,
    val titleShift: Int? = -15,
    val noteMargin: Int? = 10,
    val nodeSpacing: Int? = 50,
    val rankSpacing: Int? = 50,
    val forkWidth: Int? = 70,
    val forkHeight: Int? = 7,
    val miniPadding: Int? = 2,
    val fontSizeFactor: Double? = 5.02,
    val fontSize: Int? = 24,
    val labelHeight: Int? = 16,
    val edgeLengthFactor: String? = "20",
    val compositTitleSize: Int? = 35,
    val radius: Int? = 5,
    val defaultRenderer: String? = "dagre-wrapper",
) : BaseDiagramConfig

/** ER 图配置 - 对标 ErDiagramConfig */
@Serializable
data class ErDiagramConfig(
    val titleTopMargin: Int? = 25,
    override val useMaxWidth: Boolean? = true,
    override val useWidth: Int? = null,
    val diagramPadding: Int? = 20,
    val layoutDirection: String? = "TB",
    val minEntityWidth: Int? = 100,
    val minEntityHeight: Int? = 75,
    val entityPadding: Int? = 15,
    val nodeSpacing: Int? = 140,
    val rankSpacing: Int? = 80,
    val stroke: String? = "gray",
    val fill: String? = "honeydew",
    val fontSize: Int? = 12,
) : BaseDiagramConfig

/** 饼图配置 - 对标 PieDiagramConfig */
@Serializable
data class PieDiagramConfig(
    override val useWidth: Int? = 984,
    override val useMaxWidth: Boolean? = true,
    val textPosition: Double? = 0.75,
) : BaseDiagramConfig

/** 象限图配置 - 对标 QuadrantChartConfig */
@Serializable
data class QuadrantChartConfig(
    override val useMaxWidth: Boolean? = true,
    override val useWidth: Int? = null,
    val chartWidth: Int? = 500,
    val chartHeight: Int? = 500,
    val titleFontSize: Int? = 20,
    val titlePadding: Int? = 10,
    val quadrantPadding: Int? = 5,
    val xAxisLabelPadding: Int? = 5,
    val yAxisLabelPadding: Int? = 5,
    val xAxisLabelFontSize: Int? = 16,
    val yAxisLabelFontSize: Int? = 16,
    val quadrantLabelFontSize: Int? = 16,
    val quadrantTextTopPadding: Int? = 5,
    val pointTextPadding: Int? = 5,
    val pointLabelFontSize: Int? = 12,
    val pointRadius: Int? = 5,
    val xAxisPosition: String? = "top",
    val yAxisPosition: String? = "left",
) : BaseDiagramConfig

/** XY 图表配置 - 对标 XYChartConfig */
@Serializable
data class XYChartConfig(
    override val useMaxWidth: Boolean? = true,
    override val useWidth: Int? = null,
    val width: Int? = 700,
    val height: Int? = 500,
    val titleFontSize: Int? = 20,
    val titlePadding: Int? = 10,
    val showTitle: Boolean? = true,
    val showDataLabel: Boolean? = false,
    val chartOrientation: String? = "vertical",
    val plotReservedSpacePercent: Int? = 50,
) : BaseDiagramConfig

/** 需求图配置 - 对标 RequirementDiagramConfig */
@Serializable
data class RequirementDiagramConfig(
    override val useMaxWidth: Boolean? = true,
    override val useWidth: Int? = null,
    val rectFill: String? = "#f9f9f9",
    val textColor: String? = "#333",
    val rectBorderSize: String? = "0.5px",
    val rectBorderColor: String? = "#bbb",
    val rectMinWidth: Int? = 200,
    val rectMinHeight: Int? = 200,
    val fontSize: Int? = 14,
    val rectPadding: Int? = 10,
    val lineHeight: Int? = 20,
) : BaseDiagramConfig

/** 架构图配置 - 对标 ArchitectureDiagramConfig */
@Serializable
data class ArchitectureDiagramConfig(
    override val useMaxWidth: Boolean? = true,
    override val useWidth: Int? = null,
    val padding: Int? = 40,
    val iconSize: Int? = 80,
    val fontSize: Int? = 16,
) : BaseDiagramConfig

/** 思维导图配置 - 对标 MindmapDiagramConfig */
@Serializable
data class MindmapDiagramConfig(
    override val useMaxWidth: Boolean? = true,
    override val useWidth: Int? = null,
    val padding: Int? = 10,
    val maxNodeWidth: Int? = 200,
    val layoutAlgorithm: String? = "cose-bilkent",
) : BaseDiagramConfig

/** 鱼骨图配置 - 对标 IshikawaDiagramConfig */
@Serializable
data class IshikawaDiagramConfig(
    override val useMaxWidth: Boolean? = false,
    override val useWidth: Int? = null,
    val diagramPadding: Int? = 20,
) : BaseDiagramConfig

/** 看板图配置 - 对标 KanbanDiagramConfig */
@Serializable
data class KanbanDiagramConfig(
    override val useMaxWidth: Boolean? = true,
    override val useWidth: Int? = null,
    val padding: Int? = 8,
    val sectionWidth: Int? = 200,
    val ticketBaseUrl: String? = "",
) : BaseDiagramConfig

/** Git 图配置 - 对标 GitGraphDiagramConfig */
@Serializable
data class GitGraphDiagramConfig(
    override val useMaxWidth: Boolean? = true,
    override val useWidth: Int? = null,
    val titleTopMargin: Int? = 25,
    val diagramPadding: Int? = 8,
    val mainBranchName: String? = "main",
    val mainBranchOrder: Int? = 0,
    val showCommitLabel: Boolean? = true,
    val showBranches: Boolean? = true,
    val rotateCommitLabel: Boolean? = true,
    val parallelCommits: Boolean? = false,
    val arrowMarkerAbsolute: Boolean? = false,
) : BaseDiagramConfig

/** C4 图配置 - 对标 C4DiagramConfig (简化版) */
@Serializable
data class C4DiagramConfig(
    override val useMaxWidth: Boolean? = true,
    override val useWidth: Int? = null,
    val diagramMarginX: Int? = 50,
    val diagramMarginY: Int? = 10,
    val c4ShapeMargin: Int? = 50,
    val c4ShapePadding: Int? = 20,
    val width: Int? = 216,
    val height: Int? = 60,
    val boxMargin: Int? = 10,
    val c4ShapeInRow: Int? = 4,
    val c4BoundaryInRow: Int? = 2,
    val personFontSize: Int? = 14,
    val personFontFamily: String? = "\"Open Sans\", sans-serif",
    val personFontWeight: String? = "normal",
    val systemFontSize: Int? = 14,
    val systemFontFamily: String? = "\"Open Sans\", sans-serif",
    val systemFontWeight: String? = "normal",
    val external_personFontSize: Int? = 14,
    val external_personFontFamily: String? = "\"Open Sans\", sans-serif",
    val external_personFontWeight: String? = "normal",
    val external_systemFontSize: Int? = 14,
    val external_systemFontFamily: String? = "\"Open Sans\", sans-serif",
    val external_systemFontWeight: String? = "normal",
) : BaseDiagramConfig

/** 桑基图配置 - 对标 SankeyDiagramConfig */
@Serializable
data class SankeyDiagramConfig(
    override val useMaxWidth: Boolean? = false,
    override val useWidth: Int? = null,
    val width: Int? = 600,
    val height: Int? = 400,
    val linkColor: String? = "gradient",
    val nodeAlignment: String? = "justify",
    val showValues: Boolean? = true,
    val prefix: String? = "",
    val suffix: String? = "",
) : BaseDiagramConfig

/** 数据包图配置 - 对标 PacketDiagramConfig */
@Serializable
data class PacketDiagramConfig(
    override val useMaxWidth: Boolean? = true,
    override val useWidth: Int? = null,
    val rowHeight: Int? = 32,
    val bitWidth: Int? = 32,
    val bitsPerRow: Int? = 32,
    val showBits: Boolean? = true,
    val paddingX: Int? = 5,
    val paddingY: Int? = 5,
) : BaseDiagramConfig

/** 块图配置 - 对标 BlockDiagramConfig */
@Serializable
data class BlockDiagramConfig(
    override val useMaxWidth: Boolean? = true,
    override val useWidth: Int? = null,
    val padding: Int? = 8,
) : BaseDiagramConfig

/** 雷达图配置 - 对标 RadarDiagramConfig */
@Serializable
data class RadarDiagramConfig(
    override val useMaxWidth: Boolean? = true,
    override val useWidth: Int? = null,
    val width: Int? = 600,
    val height: Int? = 600,
    val marginTop: Int? = 50,
    val marginRight: Int? = 50,
    val marginBottom: Int? = 50,
    val marginLeft: Int? = 50,
    val axisScaleFactor: Double? = 1.0,
    val axisLabelFactor: Double? = 1.05,
    val curveTension: Double? = 0.17,
) : BaseDiagramConfig

/** 韦恩图配置 - 对标 VennDiagramConfig */
@Serializable
data class VennDiagramConfig(
    override val useMaxWidth: Boolean? = true,
    override val useWidth: Int? = null,
    val width: Int? = 800,
    val height: Int? = 450,
    val padding: Int? = 8,
    val useDebugLayout: Boolean? = false,
) : BaseDiagramConfig

/**
 * ELK 布局引擎配置 - 对标 mermaid-js config.schema.yaml elk 配置
 *
 * 控制 ELK (Eclipse Layout Kernel) 布局算法的行为参数。
 * 仅当 layout 设为 "elk" 或使用 flowchart-elk 图表类型时生效。
 */
@Serializable
data class ElkConfig(
    /** 是否合并平行边 - 对标 elk.layered.mergeEdges */
    val mergeEdges: Boolean? = false,
    /** 节点放置策略 - 对标 nodePlacement.strategy
     *  可选值: SIMPLE, NETWORK_SIMPLEX, LINEAR_SEGMENTS, BRANDES_KOEPF */
    val nodePlacementStrategy: String? = "BRANDES_KOEPF",
    /** 循环打破策略 - 对标 elk.layered.cycleBreaking.strategy
     *  可选值: GREEDY, DEPTH_FIRST, MODEL_ORDER, GREEDY_MODEL_ORDER */
    val cycleBreakingStrategy: String? = "GREEDY_MODEL_ORDER",
    /** 是否强制保持模型中的节点顺序 - 对标 elk.layered.crossingMinimization.forceNodeModelOrder */
    val forceNodeModelOrder: Boolean? = false,
    /** 保持模型顺序的策略 - 对标 elk.layered.considerModelOrder.strategy
     *  可选值: NONE, NODES_AND_EDGES, PREFER_EDGES, PREFER_NODES */
    val considerModelOrder: String? = "NODES_AND_EDGES",
)
