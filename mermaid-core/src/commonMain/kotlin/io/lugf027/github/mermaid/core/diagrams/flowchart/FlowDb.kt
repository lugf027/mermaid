package io.lugf027.github.mermaid.core.diagrams.flowchart

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.db.CommonDb
import io.lugf027.github.mermaid.core.types.*
import io.lugf027.github.mermaid.core.utils.Logger

/**
 * 流程图数据存储层。
 * 管理 vertices、edges、subGraphs、classes 等数据。
 * 对应 mermaid-js flowDb.ts 的 FlowDB 类。
 */
class FlowDb : CommonDb() {

    private val tag = "FlowDb"

    /** 节点计数器（用于生成 domId） */
    private var vertexCounter = 0

    /** 所有节点：id → FlowVertex */
    private val vertices = mutableMapOf<String, FlowVertex>()

    /** 所有边 */
    private val edges = mutableListOf<FlowEdge>()

    /** 最大边数限制 */
    private val maxEdges = 500

    /** 样式类定义 */
    private val classes = mutableMapOf<String, FlowClass>()

    /** 子图列表（保持添加顺序） */
    private val subGraphs = mutableListOf<FlowSubGraph>()

    /** 子图快查 */
    private val subGraphLookup = mutableMapOf<String, FlowSubGraph>()

    /** 子图计数（用于生成匿名子图 ID） */
    private var subCount = 0

    /** 图方向 */
    private var direction: String = "TB"

    /** 提示文本 */
    private val tooltips = mutableMapOf<String, String>()

    /** 默认边插值算法 */
    var defaultInterpolate: String? = null

    /** 默认边样式 */
    var defaultStyle: List<String>? = null

    // ─── 节点操作 ──────────────────────────────────

    /**
     * 添加节点。如果 id 已存在，更新其文本和类型。
     * 对应 mermaid-js addVertex()。
     */
    fun addVertex(
        id: String,
        text: String? = null,
        type: FlowVertexType? = null,
        style: List<String>? = null,
        classList: List<String>? = null,
        dir: String? = null,
    ) {
        val existing = vertices[id]
        if (existing != null) {
            // 更新已有节点
            if (!text.isNullOrBlank()) {
                existing.text = text
                existing.labelType = detectLabelType(text)
            }
            if (type != null) existing.type = type
            if (!style.isNullOrEmpty()) existing.styles.addAll(style)
            if (!classList.isNullOrEmpty()) existing.classes.addAll(classList)
            if (dir != null) existing.dir = dir
        } else {
            val vertex = FlowVertex(
                id = id,
                text = text ?: id,
                labelType = detectLabelType(text ?: id),
                type = type,
                dir = dir,
            )
            if (!style.isNullOrEmpty()) vertex.styles.addAll(style)
            if (!classList.isNullOrEmpty()) vertex.classes.addAll(classList)
            vertices[id] = vertex
            vertexCounter++
        }
    }

    /**
     * 获取所有节点。
     */
    fun getVertices(): Map<String, FlowVertex> = vertices.toMap()

    // ─── 边操作 ────────────────────────────────────

    /**
     * 批量添加边（支持 A & B --> C & D 语法）。
     * 对应 mermaid-js addLink()。
     */
    fun addLink(startIds: List<String>, endIds: List<String>, linkData: LinkInfo, text: String = "") {
        for (start in startIds) {
            for (end in endIds) {
                addSingleLink(start, end, linkData, text)
            }
        }
    }

    /**
     * 添加单条边。
     */
    fun addSingleLink(start: String, end: String, linkData: LinkInfo, text: String = "") {
        if (edges.size >= maxEdges) {
            Logger.warn(tag, "Max edges ($maxEdges) reached, ignoring: $start -> $end")
            return
        }

        // 确保端点节点存在
        addVertex(start)
        addVertex(end)

        val edge = FlowEdge(
            start = start,
            end = end,
            type = linkData.type,
            stroke = linkData.stroke,
            text = text,
            labelType = detectLabelType(text),
            length = linkData.length,
        )

        // 应用默认样式和插值
        defaultInterpolate?.let { edge.interpolate = it }
        defaultStyle?.let { edge.style.addAll(it) }

        edges.add(edge)
    }

    /**
     * 获取所有边。
     */
    fun getEdges(): List<FlowEdge> = edges.toList()

    // ─── 子图操作 ──────────────────────────────────

    /**
     * 添加子图。
     * 对应 mermaid-js addSubGraph()。
     */
    fun addSubGraph(id: String?, nodeList: List<String>, title: String?) {
        val sgId = id ?: "subGraph${subCount++}"
        val cleanTitle = title ?: sgId

        // 去重节点（排除嵌套子图中的节点）
        val nodes = nodeList.distinct().toMutableList()

        val sg = FlowSubGraph(
            id = sgId,
            nodes = nodes,
            title = cleanTitle,
            labelType = detectLabelType(cleanTitle),
        )

        subGraphs.add(sg)
        subGraphLookup[sgId] = sg
    }

    /**
     * 获取所有子图。
     */
    fun getSubGraphs(): List<FlowSubGraph> = subGraphs.toList()

    // ─── 样式类操作 ────────────────────────────────

    /**
     * 定义样式类。
     * classDef className fill:#f9f,stroke:#333
     */
    fun addClass(ids: String, style: String) {
        val styleList = style.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        ids.split(",").map { it.trim() }.forEach { id ->
            val cls = classes.getOrPut(id) { FlowClass(id) }
            cls.styles.addAll(styleList)
        }
    }

    /**
     * 将已定义的类分配给节点。
     * class nodeId1,nodeId2 className
     */
    fun setClass(ids: String, className: String) {
        ids.split(",").map { it.trim() }.forEach { id ->
            val vertex = vertices[id]
            if (vertex != null) {
                vertex.classes.add(className)
            }
            val sg = subGraphLookup[id]
            if (sg != null) {
                sg.classes.add(className)
            }
        }
    }

    /**
     * 获取所有样式类。
     */
    fun getClasses(): Map<String, FlowClass> = classes.toMap()

    // ─── 方向操作 ──────────────────────────────────

    /**
     * 设置图方向。
     * 对应 mermaid-js setDirection()。
     * 支持 TD→TB, <>^v 等别名。
     */
    fun setDirection(dir: String) {
        direction = when (dir.uppercase()) {
            "TD" -> "TB"
            "<" -> "RL"
            "^" -> "BT"
            ">" -> "LR"
            "V" -> "TB"
            else -> dir.uppercase()
        }
    }

    /**
     * 获取图方向。
     */
    fun getDirection(): String = direction

    // ─── 链接与交互 ────────────────────────────────

    /**
     * 设置节点超链接。
     */
    fun setLink(ids: String, linkStr: String, target: String? = null) {
        ids.split(",").map { it.trim() }.forEach { id ->
            vertices[id]?.let {
                it.link = linkStr
                it.linkTarget = target ?: "_blank"
            }
        }
    }

    /**
     * 设置节点提示文本。
     */
    fun setTooltip(ids: String, tooltip: String) {
        ids.split(",").map { it.trim() }.forEach { id ->
            tooltips[id] = tooltip
        }
    }

    /**
     * 为指定位置的边更新样式。
     */
    fun updateLinkStyle(positions: List<Int>, style: List<String>) {
        for (pos in positions) {
            if (pos == -1) {
                // default: 设置默认边样式
                defaultStyle = style
            } else if (pos in edges.indices) {
                edges[pos].style.addAll(style)
            }
        }
    }

    /**
     * 为指定位置的边更新插值算法。
     */
    fun updateLinkInterpolate(positions: List<Int>, interpolate: String) {
        for (pos in positions) {
            if (pos == -1) {
                defaultInterpolate = interpolate
            } else if (pos in edges.indices) {
                edges[pos].interpolate = interpolate
            }
        }
    }

    // ─── getData: 转换为通用 LayoutData ────────────

    /**
     * 核心数据转换方法。
     * 将 FlowDB 内部数据转换为通用 LayoutData 供渲染器使用。
     * 对应 mermaid-js flowDb.ts getData()。
     */
    fun getData(): LayoutData {
        val nodes = mutableListOf<Node>()
        val layoutEdges = mutableListOf<Edge>()

        // 1. 构建父子关系
        val parentMap = mutableMapOf<String, String>() // nodeId → subGraphId
        for (sg in subGraphs) {
            for (nodeId in sg.nodes) {
                parentMap[nodeId] = sg.id
            }
        }

        // 2. 添加子图节点（作为 group/cluster）
        for (sg in subGraphs) {
            val node = Node(
                id = sg.id,
                label = sg.title,
                shape = ShapeId.ROUNDED_RECT,
                isGroup = true,
                parentId = parentMap[sg.id],
                cssClasses = sg.classes.toMutableList(),
            )
            nodes.add(node)
        }

        // 3. 添加普通节点
        for ((_, vertex) in vertices) {
            // 跳过已作为子图添加的节点
            if (subGraphLookup.containsKey(vertex.id)) continue

            val shapeId = FlowVertexType.toShapeId(vertex.type)
            val styleMap = mutableMapOf<String, String>()
            vertex.styles.forEach { s ->
                val parts = s.split(":")
                if (parts.size == 2) styleMap[parts[0].trim()] = parts[1].trim()
            }
            val node = Node(
                id = vertex.id,
                label = vertex.text,
                shape = shapeId,
                parentId = parentMap[vertex.id],
                cssClasses = vertex.classes.toMutableList(),
                style = styleMap,
            )
            nodes.add(node)
        }

        // 4. 添加边
        for (edge in edges) {
            val (arrowTypeStart, arrowTypeEnd) = resolveArrowTypes(edge.type)
            val strokeType = when (edge.stroke) {
                StrokeStyle.NORMAL -> io.lugf027.github.mermaid.core.types.StrokeType.NORMAL
                StrokeStyle.THICK -> io.lugf027.github.mermaid.core.types.StrokeType.THICK
                StrokeStyle.DOTTED -> io.lugf027.github.mermaid.core.types.StrokeType.DOTTED
                StrokeStyle.INVISIBLE -> io.lugf027.github.mermaid.core.types.StrokeType.NORMAL
            }
            val layoutEdge = Edge(
                start = edge.start,
                end = edge.end,
                label = edge.text,
                type = io.lugf027.github.mermaid.core.types.EdgeType.ARROW_POINT,
                stroke = strokeType,
                arrowTypeStart = arrowTypeStart,
                arrowTypeEnd = arrowTypeEnd,
            )
            layoutEdges.add(layoutEdge)
        }

        return LayoutData(
            nodes = nodes,
            edges = layoutEdges,
        )
    }

    // ─── 内部方法 ──────────────────────────────────

    /**
     * 解析箭头类型为起止两端的 EdgeType。
     */
    private fun resolveArrowTypes(type: ArrowType): Pair<EdgeType, EdgeType> {
        return when (type) {
            ArrowType.ARROW_POINT -> EdgeType.ARROW_NONE to EdgeType.ARROW_POINT
            ArrowType.ARROW_CIRCLE -> EdgeType.ARROW_NONE to EdgeType.ARROW_CIRCLE
            ArrowType.ARROW_CROSS -> EdgeType.ARROW_NONE to EdgeType.ARROW_CROSS
            ArrowType.ARROW_OPEN -> EdgeType.ARROW_NONE to EdgeType.ARROW_NONE
            ArrowType.DOUBLE_ARROW_POINT -> EdgeType.ARROW_POINT to EdgeType.ARROW_POINT
            ArrowType.DOUBLE_ARROW_CIRCLE -> EdgeType.ARROW_CIRCLE to EdgeType.ARROW_CIRCLE
            ArrowType.DOUBLE_ARROW_CROSS -> EdgeType.ARROW_CROSS to EdgeType.ARROW_CROSS
        }
    }

    private fun detectLabelType(text: String): LabelType {
        return when {
            text.startsWith("`") && text.endsWith("`") -> LabelType.MARKDOWN
            text.startsWith("\"") && text.endsWith("\"") -> LabelType.STRING
            else -> LabelType.TEXT
        }
    }

    override fun clear() {
        super.clear()
        vertexCounter = 0
        vertices.clear()
        edges.clear()
        classes.clear()
        subGraphs.clear()
        subGraphLookup.clear()
        tooltips.clear()
        subCount = 0
        direction = "TB"
        defaultInterpolate = null
        defaultStyle = null
    }
}
