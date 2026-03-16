package io.lugf027.github.mermaid.core.layout.elk

import io.lugf027.github.mermaid.core.layout.Point

/**
 * ELK 层次图数据结构 - 对标 elkjs 的 ElkNode/ElkEdge/ElkLabel
 *
 * ELK 使用嵌套的层次图结构（children 递归包含子节点），
 * 不同于 dagre 的扁平图。子图(subgraph)作为容器节点参与布局。
 */

/**
 * ELK 节点 - 可以包含子节点（层次图）
 */
data class ElkNode(
    val id: String,
    var x: Double = 0.0,
    var y: Double = 0.0,
    var width: Double = 0.0,
    var height: Double = 0.0,
    var isGroup: Boolean = false,
    var children: MutableList<ElkNode> = mutableListOf(),
    var edges: MutableList<ElkEdge> = mutableListOf(),
    var labels: MutableList<ElkLabel> = mutableListOf(),
    var layoutOptions: MutableMap<String, String> = mutableMapOf(),
    var parentId: String? = null,
    var padding: Double = 0.0,
    var shape: String = "squareRect",
    /** 布局后的绝对偏移信息 */
    var offset: ElkOffset? = null,
    /** CSS 类名 */
    var cssClasses: String = "",
    /** CSS 样式 */
    var cssStyles: List<String> = emptyList(),
    /** 方向（子图可有独立方向） */
    var dir: String? = null,
    /** 标签文本 */
    var label: String? = null,
    /** 标签数据（子图标签尺寸） */
    var labelData: ElkLabelData? = null,
)

/**
 * ELK 边
 */
data class ElkEdge(
    val id: String,
    var sources: List<String> = emptyList(),
    var targets: List<String> = emptyList(),
    var sections: MutableList<ElkSection> = mutableListOf(),
    var labels: MutableList<ElkLabel> = mutableListOf(),
    /** 原始 edge 数据（start/end ID） */
    var start: String = "",
    var end: String = "",
    /** 布局后的最终点序列 */
    var points: MutableList<Point> = mutableListOf(),
    /** 标签中心坐标 */
    var labelX: Double = 0.0,
    var labelY: Double = 0.0,
    /** 边样式相关 */
    var arrowTypeStart: String? = null,
    var arrowTypeEnd: String? = "arrow_point",
    var stroke: String = "normal",
    var thickness: String = "normal",
    var pattern: String? = null,
    var edgeLabel: String? = null,
    var type: String? = null,
)

/**
 * ELK 边的路由段 - 对标 elkjs sections
 */
data class ElkSection(
    var startPoint: Point,
    var endPoint: Point,
    var bendPoints: MutableList<Point> = mutableListOf(),
)

/**
 * ELK 标签
 */
data class ElkLabel(
    var text: String = "",
    var width: Double = 0.0,
    var height: Double = 0.0,
    var x: Double = 0.0,
    var y: Double = 0.0,
    var layoutOptions: MutableMap<String, String> = mutableMapOf(),
)

/**
 * ELK 标签数据（子图标签尺寸信息）
 */
data class ElkLabelData(
    var width: Double = 0.0,
    var height: Double = 0.0,
    var wrappingWidth: Int? = null,
)

/**
 * 节点的绝对偏移信息 - 对标 mermaid-js render.ts drawNodes 中的 offset
 */
data class ElkOffset(
    /** 节点左上角的绝对 X 坐标 */
    var posX: Double = 0.0,
    /** 节点左上角的绝对 Y 坐标 */
    var posY: Double = 0.0,
    /** 父容器的相对 X */
    var x: Double = 0.0,
    /** 父容器的相对 Y */
    var y: Double = 0.0,
    /** 嵌套深度 */
    var depth: Int = 0,
    /** 节点宽度（考虑标签宽度） */
    var width: Double = 0.0,
    /** 节点高度 */
    var height: Double = 0.0,
)
