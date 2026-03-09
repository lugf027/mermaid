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

    // 存储数据
    private val _vertices = mutableMapOf<String, Vertex>()
    private val _edges = mutableListOf<Edge>()
    private val _subGraphs = mutableListOf<SubGraph>()
    private val _classDefs = mutableMapOf<String, DiagramStyleClassDef>()
    private var _direction: String = "TB"

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

    /** 添加顶点 */
    fun addVertex(id: String, text: String? = null, type: String = "squareRect") {
        if (!_vertices.containsKey(id)) {
            _vertices[id] = Vertex(id = id, text = text ?: id, type = type)
        } else {
            val v = _vertices[id]!!
            if (text != null) v.text = text
            if (type != "squareRect") v.type = type
        }
    }

    /** 添加边 */
    fun addEdge(start: String, end: String, type: String = "arrow_point", text: String? = null, stroke: String = "normal", length: Int = 1) {
        _edges.add(Edge(start = start, end = end, type = type, text = text, stroke = stroke, length = length))
        // 确保端点顶点存在
        addVertex(start)
        addVertex(end)
    }

    /** 添加子图 */
    fun addSubGraph(id: String, title: String?, nodes: List<String>, dir: String? = null) {
        _subGraphs.add(SubGraph(id = id, title = title, nodes = nodes.toMutableList(), dir = dir))
    }

    /** 添加样式类 */
    fun addClass(id: String, styles: List<String>) {
        _classDefs[id] = DiagramStyleClassDef(id = id, styles = styles)
    }

    /**
     * 获取布局数据 - 对标 flowDb.ts 的 getData()
     */
    fun getData(config: MermaidConfig): LayoutData {
        val flowConfig = config.flowchart

        val layoutNodes = _vertices.map { (id, vertex) ->
            LayoutNode(
                id = id,
                label = vertex.text ?: id,
                shape = vertex.type,
                cssStyles = vertex.styles,
                cssClasses = vertex.classes.joinToString(" "),
                link = vertex.link,
                linkTarget = vertex.linkTarget,
                tooltip = vertex.tooltip,
                domId = "flowchart-${id}",
                padding = (flowConfig?.padding ?: 15).toDouble()
            )
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
