package io.lugf027.github.mermaid.core.config

import kotlinx.serialization.Serializable

/**
 * Mermaid 全局配置数据类。
 * 对应 mermaid-js config.type.ts 的 MermaidConfig 接口。
 * 包含全局配置和各图表类型的专用配置。
 */
@Serializable
data class MermaidConfig(
    // ─── 全局配置 ──────────────────────────────────────────────
    /** 主题名称 */
    val theme: ThemeName = ThemeName.DEFAULT,
    /** 主题变量覆盖 */
    val themeVariables: Map<String, String> = emptyMap(),
    /** 最大文本长度 */
    val maxTextSize: Int = 50000,
    /** 最大边数 */
    val maxEdges: Int = 500,
    /** 字体族 */
    val fontFamily: String = "\"trebuchet ms\", verdana, arial, sans-serif",
    /** 日志级别 (1-5) */
    val logLevel: Int = 5,
    /** 安全级别 */
    val securityLevel: SecurityLevel = SecurityLevel.STRICT,
    /** 是否开始时使用 */
    val startOnLoad: Boolean = true,
    /** 箭头标记绝对定位 */
    val arrowMarkerAbsolute: Boolean = false,
    /** 是否使用 HTML 标签 */
    val htmlLabels: Boolean = true,
    /** 是否启用手绘风格 */
    val look: DiagramLook = DiagramLook.CLASSIC,
    /** 布局算法 */
    val layout: String = "dagre",
    /** 全局文本方向 */
    val wrappingWidth: Int = 200,
    /** 是否启用文本换行 */
    val wrap: Boolean = false,
    /** 默认渲染器 */
    val defaultRenderer: String = "dagre-wrapper",

    // ─── 流程图配置 ─────────────────────────────────────────────
    val flowchart: FlowchartConfig = FlowchartConfig(),
    // ─── 时序图配置 ─────────────────────────────────────────────
    val sequence: SequenceConfig = SequenceConfig(),
    // ─── 甘特图配置 ─────────────────────────────────────────────
    val gantt: GanttConfig = GanttConfig(),
    // ─── 类图配置 ──────────────────────────────────────────────
    val classDiagram: ClassDiagramConfig = ClassDiagramConfig(),
    // ─── 状态图配置 ─────────────────────────────────────────────
    val state: StateDiagramConfig = StateDiagramConfig(),
    // ─── ER 图配置 ──────────────────────────────────────────────
    val er: ErDiagramConfig = ErDiagramConfig(),
    // ─── 饼图配置 ──────────────────────────────────────────────
    val pie: PieConfig = PieConfig(),
    // ─── 象限图配置 ──────────────────────────────────────────
    val quadrantChart: QuadrantConfig = QuadrantConfig(),
    // ─── XY 图表配置 ─────────────────────────────────────────
    val xyChart: XyChartConfig = XyChartConfig(),
    // ─── 思维导图配置 ────────────────────────────────────────
    val mindmap: MindmapConfig = MindmapConfig(),
    // ─── 时间线配置 ──────────────────────────────────────────
    val timeline: TimelineConfig = TimelineConfig(),
    // ─── 桑基图配置 ──────────────────────────────────────────
    val sankey: SankeyConfig = SankeyConfig(),
    // ─── 需求图配置 ──────────────────────────────────────────
    val requirement: RequirementConfig = RequirementConfig(),
    // ─── 架构图配置 ──────────────────────────────────────────
    val architecture: ArchitectureConfig = ArchitectureConfig(),
    // ─── 雷达图配置 ──────────────────────────────────────────
    val radar: RadarConfig = RadarConfig(),
    // ─── Packet 配置 ─────────────────────────────────────────
    val packet: PacketConfig = PacketConfig(),
    // ─── 看板配置 ───────────────────────────────────────────
    val kanban: KanbanConfig = KanbanConfig(),
    // ─── Block 配置 ──────────────────────────────────────────
    val block: BlockConfig = BlockConfig(),
)

/**
 * 主题名称枚举。
 */
@Serializable
enum class ThemeName {
    DEFAULT, DARK, FOREST, NEUTRAL, BASE
}

/**
 * 安全级别。
 */
@Serializable
enum class SecurityLevel {
    STRICT, LOOSE, ANTISCRIPT, SANDBOX
}

/**
 * 图表外观风格。
 */
@Serializable
enum class DiagramLook {
    CLASSIC, HAND_DRAWN
}

// ─── 各图表类型的专用配置 ─────────────────────────────────────

@Serializable
data class FlowchartConfig(
    val diagramPadding: Int = 8,
    val htmlLabels: Boolean = true,
    val nodeSpacing: Int = 50,
    val rankSpacing: Int = 50,
    val curve: String = "basis",
    val padding: Int = 15,
    val useMaxWidth: Boolean = true,
    val defaultRenderer: String = "dagre-wrapper",
    val wrappingWidth: Int = 200,
)

@Serializable
data class SequenceConfig(
    val diagramMarginX: Int = 50,
    val diagramMarginY: Int = 10,
    val actorMargin: Int = 50,
    val width: Int = 150,
    val height: Int = 65,
    val boxMargin: Int = 10,
    val boxTextMargin: Int = 5,
    val noteMargin: Int = 10,
    val messageMargin: Int = 35,
    val messageAlign: String = "center",
    val mirrorActors: Boolean = true,
    val forceMenus: Boolean = false,
    val bottomMarginAdj: Int = 1,
    val useMaxWidth: Boolean = true,
    val rightAngles: Boolean = false,
    val showSequenceNumbers: Boolean = false,
    val actorFontSize: Int = 14,
    val actorFontFamily: String = "\"Open Sans\", sans-serif",
    val actorFontWeight: Int = 400,
    val noteFontSize: Int = 14,
    val noteFontFamily: String = "\"trebuchet ms\", verdana, arial, sans-serif",
    val noteFontWeight: Int = 400,
    val noteAlign: String = "center",
    val messageFontSize: Int = 16,
    val messageFontFamily: String = "\"trebuchet ms\", verdana, arial, sans-serif",
    val messageFontWeight: Int = 400,
    val wrap: Boolean = false,
    val wrapPadding: Int = 10,
    val labelBoxWidth: Int = 50,
    val labelBoxHeight: Int = 20,
)

@Serializable
data class GanttConfig(
    val titleTopMargin: Int = 25,
    val barHeight: Int = 20,
    val barGap: Int = 4,
    val topPadding: Int = 50,
    val rightPadding: Int = 75,
    val leftPadding: Int = 75,
    val gridLineStartPadding: Int = 35,
    val fontSize: Int = 11,
    val fontFamily: String = "\"Open Sans\", sans-serif",
    val sectionFontSize: Int = 11,
    val numberSectionStyles: Int = 4,
    val axisFormat: String = "%Y-%m-%d",
    val useMaxWidth: Boolean = true,
    val topAxis: Boolean = false,
    val useWidth: Int = 0,
)

@Serializable
data class ClassDiagramConfig(
    val titleTopMargin: Int = 25,
    val arrowMarkerAbsolute: Boolean = false,
    val diagramPadding: Int = 8,
    val htmlLabels: Boolean = true,
    val nodeSpacing: Int = 50,
    val rankSpacing: Int = 50,
    val curve: String = "basis",
    val padding: Int = 16,
    val useMaxWidth: Boolean = true,
    val defaultRenderer: String = "dagre-wrapper",
)

@Serializable
data class StateDiagramConfig(
    val titleTopMargin: Int = 25,
    val diagramPadding: Int = 8,
    val nodeSpacing: Int = 50,
    val rankSpacing: Int = 50,
    val curve: String = "basis",
    val padding: Int = 8,
    val useMaxWidth: Boolean = true,
    val defaultRenderer: String = "dagre-wrapper",
)

@Serializable
data class ErDiagramConfig(
    val diagramPadding: Int = 20,
    val layoutDirection: String = "TB",
    val minEntityWidth: Int = 100,
    val minEntityHeight: Int = 75,
    val entityPadding: Int = 15,
    val stroke: String = "gray",
    val fill: String = "honeydew",
    val fontSize: Int = 12,
    val useMaxWidth: Boolean = true,
)

@Serializable
data class PieConfig(
    val useMaxWidth: Boolean = true,
    val useWidth: Int = 0,
    val textPosition: Float = 0.75f,
)

@Serializable
data class QuadrantConfig(
    val chartWidth: Int = 500,
    val chartHeight: Int = 500,
    val titleFontSize: Int = 20,
    val titlePadding: Int = 10,
    val quadrantPadding: Int = 5,
    val xAxisLabelFontSize: Int = 16,
    val yAxisLabelFontSize: Int = 16,
    val quadrantLabelFontSize: Int = 16,
    val quadrantTextTopPadding: Int = 5,
    val quadrantExternalBorderStrokeWidth: Int = 2,
    val quadrantInternalBorderStrokeWidth: Int = 1,
    val pointTextPadding: Int = 5,
    val pointLabelFontSize: Int = 12,
    val pointRadius: Int = 5,
    val useMaxWidth: Boolean = true,
)

@Serializable
data class XyChartConfig(
    val width: Int = 700,
    val height: Int = 500,
    val titleFontSize: Int = 20,
    val titlePadding: Int = 10,
    val showTitle: Boolean = true,
    val xAxis: AxisConfig = AxisConfig(),
    val yAxis: AxisConfig = AxisConfig(),
    val chartOrientation: String = "vertical",
    val plotReservedSpacePercent: Int = 50,
)

@Serializable
data class AxisConfig(
    val showLabel: Boolean = true,
    val labelFontSize: Int = 14,
    val labelPadding: Int = 5,
    val showTitle: Boolean = true,
    val titleFontSize: Int = 16,
    val titlePadding: Int = 5,
    val showTick: Boolean = true,
    val tickLength: Int = 5,
    val tickWidth: Int = 2,
    val showAxisLine: Boolean = true,
    val axisLineWidth: Int = 2,
)

@Serializable
data class MindmapConfig(
    val useMaxWidth: Boolean = true,
    val padding: Int = 10,
    val maxNodeWidth: Int = 200,
)

@Serializable
data class TimelineConfig(
    val diagramMarginX: Int = 50,
    val diagramMarginY: Int = 10,
    val leftMargin: Int = 150,
    val width: Int = 150,
    val height: Int = 50,
    val padding: Int = 8,
    val boxMargin: Int = 10,
    val boxTextMargin: Int = 5,
    val noteMargin: Int = 10,
    val messageMargin: Int = 35,
    val messageAlign: String = "center",
    val bottomMarginAdj: Int = 1,
    val useMaxWidth: Boolean = true,
    val sectionFontSize: Int = 24,
    val sectionColor: String = "Black",
)

@Serializable
data class SankeyConfig(
    val useMaxWidth: Boolean = true,
    val width: Int = 600,
    val height: Int = 400,
    val linkColor: String = "gradient",
    val nodeAlignment: String = "justify",
    val showValues: Boolean = true,
    val prefix: String = "",
    val suffix: String = "",
)

@Serializable
data class RequirementConfig(
    val useMaxWidth: Boolean = true,
    val rectFill: String = "#f9f9f9",
    val textColor: String = "#333",
    val reqBackFill: String = "#f9f9f9",
    val reqBorderColor: String = "#bbb",
    val lineColor: String = "#333",
    val fontSize: Int = 14,
)

@Serializable
data class ArchitectureConfig(
    val useMaxWidth: Boolean = true,
    val padding: Int = 40,
    val iconSize: Int = 80,
    val fontSize: Int = 16,
)

@Serializable
data class RadarConfig(
    val useMaxWidth: Boolean = true,
    val width: Int = 600,
    val height: Int = 600,
    val marginTop: Int = 40,
    val marginRight: Int = 40,
    val marginBottom: Int = 40,
    val marginLeft: Int = 40,
    val axisScaleCount: Int = 5,
    val curveTension: Float = 0f,
    val showLegend: Boolean = true,
    val showMarkers: Boolean = true,
    val graticule: String = "circle",
)

@Serializable
data class PacketConfig(
    val useMaxWidth: Boolean = true,
    val rowHeight: Int = 32,
    val bitWidth: Int = 32,
    val bitsPerRow: Int = 32,
    val showBits: Boolean = true,
    val paddingX: Int = 5,
    val paddingY: Int = 5,
)

@Serializable
data class KanbanConfig(
    val useMaxWidth: Boolean = true,
    val padding: Int = 8,
    val itemHeight: Int = 50,
    val sectionWidth: Int = 200,
)

@Serializable
data class BlockConfig(
    val useMaxWidth: Boolean = true,
    val padding: Int = 8,
)
