package io.lugf027.github.mermaid.core.types

/**
 * 解析选项。
 */
data class ParseOptions(
    /** 是否抑制错误（不抛出异常） */
    val suppressErrors: Boolean = false
)

/**
 * 解析结果。
 */
data class ParseResult(
    /** 检测到的图表类型 */
    val diagramType: String,
    /** 是否解析成功 */
    val success: Boolean = true,
    /** 错误信息（如果失败） */
    val error: MermaidError? = null
)

/**
 * Mermaid 错误类型。
 */
data class MermaidError(
    /** 错误消息 */
    val message: String,
    /** 错误发生的行号 */
    val line: Int = -1,
    /** 错误发生的列号 */
    val column: Int = -1,
    /** 错误类型 */
    val type: ErrorType = ErrorType.PARSE_ERROR,
    /** 详细信息 */
    val details: String = ""
) {
    override fun toString(): String {
        return if (line >= 0) {
            "[$type] Line $line:$column - $message"
        } else {
            "[$type] $message"
        }
    }
}

/**
 * 错误类型。
 */
enum class ErrorType {
    /** 解析错误 */
    PARSE_ERROR,
    /** 类型检测错误 */
    DETECTION_ERROR,
    /** 渲染错误 */
    RENDER_ERROR,
    /** 配置错误 */
    CONFIG_ERROR,
    /** 未知错误 */
    UNKNOWN
}

/**
 * 图表类型标识。
 * 对应 mermaid-js 的 diagram 类型字符串。
 */
object DiagramTypeId {
    const val FLOWCHART = "flowchart"
    const val FLOWCHART_V2 = "flowchart-v2"
    const val SEQUENCE = "sequence"
    const val CLASS = "classDiagram"
    const val CLASS_V2 = "classDiagram-v2"
    const val STATE = "stateDiagram"
    const val STATE_V2 = "stateDiagram-v2"
    const val ER = "er"
    const val GANTT = "gantt"
    const val GIT = "gitGraph"
    const val PIE = "pie"
    const val MINDMAP = "mindmap"
    const val TIMELINE = "timeline"
    const val KANBAN = "kanban"
    const val C4 = "c4"
    const val QUADRANT = "quadrantChart"
    const val XY_CHART = "xychart-beta"
    const val REQUIREMENT = "requirement"
    const val JOURNEY = "journey"
    const val SANKEY = "sankey"
    const val BLOCK = "block-beta"
    const val PACKET = "packet-beta"
    const val ARCHITECTURE = "architecture"
    const val INFO = "info"
    const val RADAR = "radar"
    const val ISHIKAWA = "ishikawa"
    const val VENN = "venn"
    const val TREEMAP = "treemap"
    const val ERROR = "error"
}

/**
 * Frontmatter 数据。
 * 从 Mermaid 文本头部的 YAML frontmatter 中提取。
 */
data class FrontmatterData(
    val title: String? = null,
    val displayMode: String? = null,
    val config: Map<String, Any?> = emptyMap()
)

/**
 * Directive 数据。
 * 从 Mermaid 文本中的 %%{...}%% 指令中提取。
 */
data class DirectiveData(
    val type: String,
    val args: Map<String, Any?> = emptyMap()
)
