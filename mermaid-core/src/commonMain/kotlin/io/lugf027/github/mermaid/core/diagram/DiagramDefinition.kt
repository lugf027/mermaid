package io.lugf027.github.mermaid.core.diagram

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.rendering.svg.SvgRoot
import io.lugf027.github.mermaid.core.themes.ThemeVariables

/**
 * 图表数据库接口 - 对标 mermaid-js DiagramDB
 *
 * 每种图表类型实现此接口，用于存储解析结果。
 * 解析器将 Mermaid 文本解析后的数据写入 DB，渲染器从 DB 读取数据生成 SVG。
 */
interface DiagramDB {
    /** 清空所有已存储的数据 */
    fun clear()

    /** 设置图表标题 */
    fun setDiagramTitle(title: String)

    /** 获取图表标题 */
    fun getDiagramTitle(): String

    /** 设置无障碍标题 */
    fun setAccTitle(title: String)

    /** 获取无障碍标题 */
    fun getAccTitle(): String

    /** 设置无障碍描述 */
    fun setAccDescription(desc: String)

    /** 获取无障碍描述 */
    fun getAccDescription(): String

    /** 获取图表方向 (TB/LR/BT/RL) */
    fun getDirection(): String = "TB"

    /** 设置图表方向 */
    fun setDirection(direction: String) {}

    /** 获取配置 */
    fun getConfig(): MermaidConfig? = null
}

/**
 * 图表渲染器接口 - 负责将 DB 中的数据渲染为 SVG IR
 *
 * 两种渲染模式：
 * 1. 统一渲染器：使用 LayoutData + 布局算法（如 dagre），适用于 flowchart/class/state 等
 * 2. 自定义渲染器：图表独立的 SVG 生成逻辑，适用于 pie/gantt/sequence 等
 */
interface DiagramRenderer {
    /**
     * 将图表数据渲染为 SVG IR
     *
     * @param db 图表数据库
     * @param config 合并后的配置
     * @param themeVariables 主题变量
     * @param diagramId 图表唯一标识
     * @return SVG 根节点
     */
    fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot
}

/**
 * 图表解析器接口 - 负责将 Mermaid 文本解析并填充 DB
 */
interface DiagramParser {
    /**
     * 解析 Mermaid 文本，将结果写入 DB
     *
     * @param text 清理后的 Mermaid 文本（已移除 frontmatter/指令/注释）
     * @param db 目标数据库
     */
    fun parse(text: String, db: DiagramDB)
}

/**
 * 图表类型检测器
 */
typealias DiagramDetector = (text: String) -> Boolean

/**
 * 图表样式类定义 - 对标 mermaid-js DiagramStyleClassDef
 */
data class DiagramStyleClassDef(
    val id: String,
    val styles: List<String> = emptyList(),
    val textStyles: List<String> = emptyList()
)

/**
 * 图表定义 - 组合 parser + db + renderer，对标 mermaid-js DiagramDefinition
 *
 * 每种图表类型注册一个 DiagramDefinition 实例到 DiagramRegistry。
 */
data class DiagramDefinition(
    /** 图表类型 ID，如 "flowchart-v2"、"pie" 等 */
    val id: String,
    /** 类型检测函数 */
    val detector: DiagramDetector,
    /** DB 工厂函数，每次解析创建新实例 */
    val dbFactory: () -> DiagramDB,
    /** 解析器 */
    val parser: DiagramParser,
    /** 渲染器 */
    val renderer: DiagramRenderer,
    /** 样式生成函数 */
    val styles: ((ThemeVariables) -> String)? = null,
    /** 初始化回调 */
    val init: ((MermaidConfig) -> Unit)? = null
)
