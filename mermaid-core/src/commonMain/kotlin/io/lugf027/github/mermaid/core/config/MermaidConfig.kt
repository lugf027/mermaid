package io.lugf027.github.mermaid.core.config

import kotlinx.serialization.Serializable

/**
 * Mermaid 全局配置 - 对标 mermaid-js MermaidConfig
 *
 * 所有属性均可选（null 表示使用默认值），支持层级合并：
 * directives > siteConfig > defaultConfig
 */
@Serializable
data class MermaidConfig(
    /** 主题名称: "default", "dark", "forest", "neutral", "base" */
    val theme: String? = null,
    /** 自定义主题 CSS */
    val themeCSS: String? = null,
    /** 外观风格: "classic", "handDrawn" */
    val look: String? = null,
    /** 手绘种子值 */
    val handDrawnSeed: Int? = null,
    /** 布局算法: "dagre", "elk" */
    val layout: String? = null,
    /** 最大文本长度 */
    val maxTextSize: Int? = null,
    /** 最大边数 */
    val maxEdges: Int? = null,
    /** 暗色模式 */
    val darkMode: Boolean? = null,
    /** 是否使用 HTML 标签 */
    val htmlLabels: Boolean? = null,
    /** 字体族 */
    val fontFamily: String? = null,
    /** 备选字体族 */
    val altFontFamily: String? = null,
    /** 日志级别 (1=debug, 2=info, 3=warn, 4=error, 5=fatal) */
    val logLevel: Int? = null,
    /** 安全级别: "strict", "loose", "antiscript", "sandbox" */
    val securityLevel: String? = null,
    /** 箭头标记使用绝对路径 */
    val arrowMarkerAbsolute: Boolean? = null,
    /** 确定性 ID */
    val deterministicIds: Boolean? = null,
    /** 确定性 ID 种子 */
    val deterministicIDSeed: String? = null,
    /** 字体大小 */
    val fontSize: Int? = null,
    /** Markdown 自动换行 */
    val markdownAutoWrap: Boolean? = null,
    /** 是否抑制错误渲染 */
    val suppressErrorRendering: Boolean? = null,
    /** 是否换行 */
    val wrap: Boolean? = null,

    // 各图表子配置
    val flowchart: FlowchartDiagramConfig? = null,
    val sequence: SequenceDiagramConfig? = null,
    val gantt: GanttDiagramConfig? = null,
    val journey: JourneyDiagramConfig? = null,
    val timeline: TimelineDiagramConfig? = null,
    @Suppress("PropertyName")
    val `class`: ClassDiagramConfig? = null,
    val state: StateDiagramConfig? = null,
    val er: ErDiagramConfig? = null,
    val pie: PieDiagramConfig? = null,
    val quadrantChart: QuadrantChartConfig? = null,
    val xyChart: XYChartConfig? = null,
    val requirement: RequirementDiagramConfig? = null,
    val architecture: ArchitectureDiagramConfig? = null,
    val mindmap: MindmapDiagramConfig? = null,
    val ishikawa: IshikawaDiagramConfig? = null,
    val kanban: KanbanDiagramConfig? = null,
    val gitGraph: GitGraphDiagramConfig? = null,
    val c4: C4DiagramConfig? = null,
    val sankey: SankeyDiagramConfig? = null,
    val packet: PacketDiagramConfig? = null,
    val block: BlockDiagramConfig? = null,
    val radar: RadarDiagramConfig? = null,
    val venn: VennDiagramConfig? = null,
    /** ELK 布局引擎配置 - 仅当 layout="elk" 或使用 flowchart-elk 时生效 */
    val elk: ElkConfig? = null,
) {
    companion object {
        /** 默认配置 */
        val DEFAULT = MermaidConfig(
            theme = "default",
            look = "classic",
            handDrawnSeed = 0,
            layout = "dagre",
            maxTextSize = 50000,
            maxEdges = 500,
            darkMode = false,
            fontFamily = "\"trebuchet ms\", verdana, arial, sans-serif",
            logLevel = 5,
            securityLevel = "strict",
            arrowMarkerAbsolute = false,
            deterministicIds = false,
            fontSize = 16,
            markdownAutoWrap = true,
            suppressErrorRendering = false,
            flowchart = FlowchartDiagramConfig(),
            sequence = SequenceDiagramConfig(),
            gantt = GanttDiagramConfig(),
            journey = JourneyDiagramConfig(),
            timeline = TimelineDiagramConfig(),
            `class` = ClassDiagramConfig(),
            state = StateDiagramConfig(),
            er = ErDiagramConfig(),
            pie = PieDiagramConfig(),
            quadrantChart = QuadrantChartConfig(),
            xyChart = XYChartConfig(),
            requirement = RequirementDiagramConfig(),
            architecture = ArchitectureDiagramConfig(),
            mindmap = MindmapDiagramConfig(),
            ishikawa = IshikawaDiagramConfig(),
            kanban = KanbanDiagramConfig(),
            gitGraph = GitGraphDiagramConfig(),
            c4 = C4DiagramConfig(),
            sankey = SankeyDiagramConfig(),
            packet = PacketDiagramConfig(),
            block = BlockDiagramConfig(),
            radar = RadarDiagramConfig(),
            venn = VennDiagramConfig(),
            elk = ElkConfig(),
        )
    }
}
