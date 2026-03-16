package io.lugf027.github.mermaid.core.diagram.flowchart

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.*
import io.lugf027.github.mermaid.core.layout.*

/**
 * 流程图数据库 - 对标 mermaid-js flowDb.ts
 *
 * 存储流程图的顶点（vertices）、边（edges）、子图（subGraphs）、样式类定义等。
 */
class FlowchartDb : DiagramDB {
    /** 顶点定义 */
    data class Vertex(
        val id: String,
        var text: String? = null,
        var type: String = "squareRect",  // 形状类型
        var classes: MutableList<String> = mutableListOf(),
        var styles: MutableList<String> = mutableListOf(),
        var link: String? = null,
        var linkTarget: String? = null,
        var tooltip: String? = null,
        var domId: String = "",  // mermaid-js 的 domId
    )

    /** 边定义 */
    data class Edge(
        val start: String,
        val end: String,
        var type: String = "arrow_point",  // 箭头类型
        var text: String? = null,
        var stroke: String = "normal",  // normal, thick, dotted
        var length: Int = 1,  // 最小长度
    )

    /** 子图定义 */
    data class SubGraph(
        val id: String,
        var title: String? = null,
        var nodes: MutableList<String> = mutableListOf(),
        var dir: String? = null,
    )

    // 存储数据 — 使用 LinkedHashMap 保持插入顺序（对标 JS Map 的插入顺序语义）
    private val _vertices = linkedMapOf<String, Vertex>()
    private val _edges = mutableListOf<Edge>()
    private val _subGraphs = mutableListOf<SubGraph>()
    private val _classDefs = mutableMapOf<String, DiagramStyleClassDef>()
    private var _direction: String = "TB"

    /**
     * 全局顶点计数器 — 对齐 mermaid-js flowDb.ts 的 vertexCounter
     *
     * mermaid-js 中 vertexCounter 在每次 addVertex 调用时都递增（不管节点是否已存在），
     * 但 domId 只在首次创建时设置（使用当时的 counter 值）。
     */
    private var vertexCounter: Int = 0

    private var diagramTitle: String = ""
    private var accTitle: String = ""
    private var accDescription: String = ""

    override fun clear() {
        _vertices.clear()
        _edges.clear()
        _subGraphs.clear()
        _classDefs.clear()
        _direction = "TB"
        diagramTitle = ""
        accTitle = ""
        accDescription = ""
        // mermaid-js 的 clear() 不重置 vertexCounter
        // 但 FlowchartDb 每次 new 出来时 vertexCounter 从 0 开始
        vertexCounter = 0
    }

    override fun setDiagramTitle(title: String) { diagramTitle = title }
    override fun getDiagramTitle(): String = diagramTitle
    override fun setAccTitle(title: String) { accTitle = title }
    override fun getAccTitle(): String = accTitle
    override fun setAccDescription(desc: String) { accDescription = desc }
    override fun getAccDescription(): String = accDescription
    override fun getDirection(): String = _direction
    override fun setDirection(direction: String) { _direction = direction }

    fun getVertices(): Map<String, Vertex> = _vertices
    fun getEdges(): List<Edge> = _edges
    fun getSubGraphs(): List<SubGraph> = _subGraphs

    /** 添加顶点 — 对齐 mermaid-js flowDb.ts addVertex 行为 */
    fun addVertex(id: String, text: String? = null, type: String = "squareRect") {
        if (!_vertices.containsKey(id)) {
            _vertices[id] = Vertex(
                id = id,
                text = text ?: id,
                type = type,
                domId = "flowchart-${id}-${vertexCounter}"
            )
        } else {
            val v = _vertices[id]!!
            if (text != null) v.text = text
            if (type != "squareRect") v.type = type
        }
        // mermaid-js: vertexCounter++ 在 if 块外部，每次 addVertex 调用都递增
        vertexCounter++
    }

    /** 添加边 - 对标 mermaid-js addLink（不调用 addVertex，避免多余的 counter 递增） */
    fun addEdge(start: String, end: String, type: String = "arrow_point", text: String? = null, stroke: String = "normal", length: Int = 1) {
        _edges.add(Edge(start = start, end = end, type = type, text = text, stroke = stroke, length = length))
        // 注意：mermaid-js 的 addLink 不会调用 addVertex
        // 端点顶点在解析阶段通过 parseNodeDefinition 已经添加过了
    }

    /** 添加子图 */
    fun addSubGraph(id: String, title: String?, nodes: List<String>, dir: String? = null) {
        _subGraphs.add(SubGraph(id = id, title = title, nodes = nodes.toMutableList(), dir = dir))
    }

    /**
     * 更新子图的节点列表 — 在 subgraph...end 块解析完成后由解析器调用
     *
     * 对标 mermaid-js flowDb.ts addSubGraph() 中的 makeUniq() 逻辑：
     * 如果一个节点已经属于某个更内层的子图，则不会被添加到外层子图。
     * 这保证了每个节点只属于最内层的子图。
     */
    fun updateSubGraphNodes(sgId: String, nodeIds: List<String>) {
        val sg = _subGraphs.find { it.id == sgId } ?: return
        // makeUniq: 排除已经存在于其他（更内层）子图中的节点
        val existingNodes = mutableSetOf<String>()
        for (otherSg in _subGraphs) {
            if (otherSg.id != sgId) {
                existingNodes.addAll(otherSg.nodes)
            }
        }
        val uniqueNodes = nodeIds.filter { it !in existingNodes && it.isNotEmpty() }
        sg.nodes = uniqueNodes.toMutableList()
    }

    /** 添加样式类 */
    fun addClass(id: String, styles: List<String>) {
        _classDefs[id] = DiagramStyleClassDef(id = id, styles = styles)
    }

    /**
     * 获取布局数据 - 对标 flowDb.ts 的 getData()
     *
     * 将子图作为 isGroup=true 的 LayoutNode 包含在内，
     * 并为子图内的节点设置 parentId，使 ELK 布局能正确处理层次图结构。
     */
    fun getData(config: MermaidConfig): LayoutData {
        val flowConfig = config.flowchart

        // 构建节点 → 子图 的映射：nodeId → subgraphId
        val nodeParentMap = mutableMapOf<String, String>()
        for (sg in _subGraphs) {
            for (nodeId in sg.nodes) {
                nodeParentMap[nodeId] = sg.id
            }
        }

        // 1. 普通节点
        val layoutNodes = _vertices.entries.map { (id, vertex) ->
            LayoutNode(
                id = id,
                label = vertex.text ?: id,
                shape = vertex.type,
                cssStyles = vertex.styles,
                cssClasses = vertex.classes.joinToString(" "),
                link = vertex.link,
                linkTarget = vertex.linkTarget,
                tooltip = vertex.tooltip,
                domId = vertex.domId,
                padding = (flowConfig?.padding ?: 15).toDouble(),
                parentId = nodeParentMap[id],
            )
        }.toMutableList()

        // 2. 子图作为 isGroup 节点
        // 子图也可以嵌套在其他子图中（对标 mermaid-js getData 中的 parentDB.get(subGraph.id)）
        for (sg in _subGraphs) {
            layoutNodes.add(LayoutNode(
                id = sg.id,
                label = sg.title,
                isGroup = true,
                dir = sg.dir,
                children = sg.nodes.toList(),
                padding = (flowConfig?.padding ?: 15).toDouble(),
                parentId = nodeParentMap[sg.id],  // 子图嵌套关系
            ))
        }

        val layoutEdges = _edges.mapIndexed { idx, edge ->
            LayoutEdge(
                id = "e${idx}-${edge.start}-${edge.end}",
                start = edge.start,
                end = edge.end,
                label = edge.text,
                arrowTypeEnd = edge.type,
                stroke = edge.stroke,
                minLen = edge.length,
            )
        }

        return LayoutData(
            nodes = layoutNodes,
            edges = layoutEdges,
            config = config,
            direction = _direction,
            nodeSpacing = flowConfig?.nodeSpacing ?: 50,
            rankSpacing = flowConfig?.rankSpacing ?: 50,
            diagramPadding = flowConfig?.diagramPadding ?: 8,
        )
    }
}
