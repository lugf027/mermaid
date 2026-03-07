package io.lugf027.github.mermaid.core.types

import kotlinx.serialization.Serializable

/**
 * 二维坐标点。
 */
@Serializable
data class Point(
    val x: Float = 0f,
    val y: Float = 0f
)

/**
 * 矩形边界。
 */
@Serializable
data class Bounds(
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f
) {
    val left: Float get() = x
    val top: Float get() = y
    val right: Float get() = x + width
    val bottom: Float get() = y + height
    val centerX: Float get() = x + width / 2f
    val centerY: Float get() = y + height / 2f
}

/**
 * 文本尺寸信息。
 */
data class TextDimensions(
    val width: Float = 0f,
    val height: Float = 0f,
    val lineHeight: Float = 0f
)

/**
 * 边（连线）的类型。
 * 对应 mermaid-js 中的箭头/线条类型。
 */
enum class EdgeType {
    /** 无箭头 */
    ARROW_NONE,
    /** 普通箭头 */
    ARROW_POINT,
    /** 圆形 */
    ARROW_CIRCLE,
    /** 叉形 */
    ARROW_CROSS,
    /** 开放箭头 */
    ARROW_OPEN
}

/**
 * 线条样式。
 */
enum class StrokeType {
    /** 实线 */
    NORMAL,
    /** 虚线 */
    DOTTED,
    /** 粗线 */
    THICK
}

/**
 * 图表中的节点（顶点）。
 * 对应 mermaid-js 的 Node 类型。
 */
data class Node(
    /** 节点唯一标识 */
    val id: String,
    /** 显示文本/标签 */
    val label: String = "",
    /** 节点形状 */
    val shape: ShapeId = ShapeId.RECT,
    /** 布局后的坐标 (中心点) */
    var x: Float = 0f,
    var y: Float = 0f,
    /** 节点尺寸 */
    var width: Float = 0f,
    var height: Float = 0f,
    /** 内边距 */
    val padding: Float = 8f,
    /** CSS 类名列表 */
    val cssClasses: MutableList<String> = mutableListOf(),
    /** 自定义样式 */
    val style: MutableMap<String, String> = mutableMapOf(),
    /** 是否为子图/集群节点 */
    val isGroup: Boolean = false,
    /** 父节点 ID（如果在子图内） */
    var parentId: String? = null,
    /** 所属分区/泳道 */
    var partition: String? = null,
    /** 链接 URL */
    var link: String? = null,
    /** 提示文本 */
    var tooltip: String? = null,
    /** 图标名称 */
    var icon: String? = null,
    /** 是否需要 Markdown 渲染 */
    var useHtmlLabels: Boolean = false,
    /** 节点层级（用于子图嵌套） */
    var level: Int = 0,
    /** 额外属性 */
    val extra: MutableMap<String, Any?> = mutableMapOf()
)

/**
 * 图表中的边（连线）。
 * 对应 mermaid-js 的 Edge 类型。
 */
data class Edge(
    /** 起始节点 ID */
    val start: String,
    /** 结束节点 ID */
    val end: String,
    /** 边标签文本 */
    val label: String = "",
    /** 边类型（箭头方向） */
    val type: EdgeType = EdgeType.ARROW_POINT,
    /** 线条样式 */
    val stroke: StrokeType = StrokeType.NORMAL,
    /** 起始端箭头类型 */
    val arrowTypeStart: EdgeType = EdgeType.ARROW_NONE,
    /** 结束端箭头类型 */
    val arrowTypeEnd: EdgeType = EdgeType.ARROW_POINT,
    /** CSS 类名列表 */
    val cssClasses: MutableList<String> = mutableListOf(),
    /** 自定义样式 */
    val style: MutableMap<String, String> = mutableMapOf(),
    /** 布局后的路径点 */
    var points: List<Point> = emptyList(),
    /** 标签位置 */
    var labelPos: Point? = null,
    /** 边的最小长度 */
    var minLen: Int = 1,
    /** 额外属性 */
    val extra: MutableMap<String, Any?> = mutableMapOf()
)

/**
 * 子图/集群定义。
 */
data class Cluster(
    /** 子图唯一标识 */
    val id: String,
    /** 显示标题 */
    val title: String = "",
    /** 包含的节点 ID */
    val nodeIds: MutableList<String> = mutableListOf(),
    /** 布局后的边界 */
    var bounds: Bounds = Bounds(),
    /** 样式 */
    val style: MutableMap<String, String> = mutableMapOf(),
    /** 父子图 ID */
    var parentId: String? = null
)

/**
 * 布局数据 - 传递给布局引擎的完整数据结构。
 * 对应 mermaid-js 的 LayoutData。
 */
data class LayoutData(
    /** 所有节点 */
    val nodes: List<Node> = emptyList(),
    /** 所有边 */
    val edges: List<Edge> = emptyList(),
    /** 所有子图/集群 */
    val clusters: List<Cluster> = emptyList(),
    /** 图的方向 */
    val direction: Direction = Direction.TB,
    /** 节点间距 */
    val nodeSep: Float = 50f,
    /** 层级间距 */
    val rankSep: Float = 50f,
    /** 边间距 */
    val edgeSep: Float = 10f,
    /** 排名方向 */
    val rankDir: Direction = Direction.TB
)

/**
 * 图的方向。
 */
enum class Direction {
    /** 从上到下 */
    TB,
    /** 从下到上 */
    BT,
    /** 从左到右 */
    LR,
    /** 从右到左 */
    RL
}

/**
 * 渲染后的完整数据（布局计算完成后的最终数据）。
 */
data class RenderData(
    /** 布局后的节点 */
    val nodes: List<Node>,
    /** 布局后的边（包含路径点） */
    val edges: List<Edge>,
    /** 布局后的子图 */
    val clusters: List<Cluster>,
    /** 图表总边界 */
    val bounds: Bounds,
    /** 图表标题 */
    val title: String = "",
    /** 方向 */
    val direction: Direction = Direction.TB
)
