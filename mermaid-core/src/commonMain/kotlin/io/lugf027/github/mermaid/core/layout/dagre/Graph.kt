package io.lugf027.github.mermaid.core.layout.dagre

/**
 * 图数据结构（邻接表）- 对标 graphlib
 *
 * 支持有向图的节点/边/子图管理，作为 dagre 布局算法的基础数据结构。
 */
class Graph(
    val isDirected: Boolean = true,
    val isMultigraph: Boolean = false,
    val isCompound: Boolean = true
) {
    /** 节点数据 */
    data class NodeData(
        val id: String,
        var label: String? = null,
        var width: Double = 0.0,
        var height: Double = 0.0,
        var x: Double = 0.0,
        var y: Double = 0.0,
        var rank: Int = -1,
        var order: Int = -1,
        var padding: Double = 0.0,
        var shape: String = "rect",
        /** dummy 节点类型: null=真实节点, "edge"=长边拆分, "edge-label"=边标签 */
        var dummy: String? = null,
        /** 如果是 dummy 节点，指向原始边的 EdgeData */
        var edgeLabel: EdgeData? = null,
        /** 如果是 dummy 节点，记录原始边的 source->target */
        var edgeObj: EdgeKey? = null,
        /** 标签位置（用于带标签的 dummy 节点） */
        var labelpos: String? = null,
        val extra: MutableMap<String, Any> = mutableMapOf()
    )

    /** 边的键 */
    data class EdgeKey(val v: String, val w: String, val name: String? = null)

    /** 边数据 */
    data class EdgeData(
        val source: String,
        val target: String,
        var label: String? = null,
        var weight: Double = 1.0,
        var minLen: Int = 1,
        var width: Double = 0.0,
        var height: Double = 0.0,
        var x: Double = 0.0,
        var y: Double = 0.0,
        var points: MutableList<io.lugf027.github.mermaid.core.layout.Point> = mutableListOf(),
        var labelpos: String = "c",
        var labeloffset: Double = 10.0,
        /** 标签在哪个 rank（由 injectEdgeLabelProxies 设置） */
        var labelRank: Int = -1,
        /** 是否是反转的边 */
        var reversed: Boolean = false,
        /**
         * 标记 edge 是否有标签坐标（x, y 已由 normalize.undo 设置）。
         * 精确对标 JS 的 Object.prototype.hasOwnProperty.call(edge, 'x') 检查。
         * dagre 中只有带标签的边（经过 normalize.undo 后）才有 x/y 属性，
         * 无标签边的 x/y 始终为默认值 0.0，但在 JS 中属性不存在。
         */
        var hasLabelCoords: Boolean = false,
        val extra: MutableMap<String, Any> = mutableMapOf()
    )

    /** 图级属性 */
    var rankdir: String = "TB"
    var rankSep: Double = 50.0
    var nodeSep: Double = 50.0
    var edgeSep: Double = 20.0
    var marginX: Double = 0.0
    var marginY: Double = 0.0
    /** dagre normalize 使用的 dummy chain 首节点列表 */
    val dummyChains: MutableList<String> = mutableListOf()
    /** 唯一 ID 计数器 */
    private var idCounter = 0

    // 内部数据 — 使用 LinkedHashMap 保持插入顺序（对标 JS Map 的插入顺序语义）
    private val nodes = linkedMapOf<String, NodeData>()
    private val inEdges = linkedMapOf<String, MutableList<EdgeData>>()
    private val outEdges = linkedMapOf<String, MutableList<EdgeData>>()
    /** 全局边列表 — 保持边的全局插入顺序，对标 JS graphlib 的 _edgeObjs */
    private val allEdges = linkedMapOf<String, EdgeData>()
    private val parent = mutableMapOf<String, String?>()
    private val childrenMap = mutableMapOf<String, MutableSet<String>>()

    // ====== 节点操作 ======

    fun setNode(id: String, data: NodeData = NodeData(id)) {
        nodes[id] = data
        if (!inEdges.containsKey(id)) inEdges[id] = mutableListOf()
        if (!outEdges.containsKey(id)) outEdges[id] = mutableListOf()
        if (!parent.containsKey(id)) parent[id] = null
        if (!childrenMap.containsKey(id)) childrenMap[id] = mutableSetOf()
    }

    fun getNode(id: String): NodeData? = nodes[id]

    fun hasNode(id: String): Boolean = nodes.containsKey(id)

    fun removeNode(id: String) {
        // 移除与该节点相关的所有边
        val inList = inEdges[id]?.toList() ?: emptyList()
        for (edge in inList) {
            outEdges[edge.source]?.removeAll { it.target == id }
            allEdges.remove("${edge.source}\u0000${id}")
        }
        val outList = outEdges[id]?.toList() ?: emptyList()
        for (edge in outList) {
            inEdges[edge.target]?.removeAll { it.source == id }
            allEdges.remove("${id}\u0000${edge.target}")
        }
        nodes.remove(id)
        inEdges.remove(id)
        outEdges.remove(id)
        parent.remove(id)
        childrenMap.remove(id)
    }

    fun nodeIds(): Set<String> = nodes.keys.toSet()

    fun nodeCount(): Int = nodes.size

    fun getNodes(): List<NodeData> = nodes.values.toList()

    /**
     * 生成唯一 ID — 对标 dagre 的 _.uniqueId
     */
    fun uniqueId(prefix: String): String {
        var v: String
        do {
            v = "$prefix${idCounter++}"
        } while (hasNode(v))
        return v
    }

    // ====== 边操作 ======

    fun setEdge(source: String, target: String, data: EdgeData = EdgeData(source, target)) {
        val edgeKey = "${source}\u0000${target}"
        // 先移除已有的同方向边
        outEdges.getOrPut(source) { mutableListOf() }.removeAll { it.target == target }
        inEdges.getOrPut(target) { mutableListOf() }.removeAll { it.source == source }
        allEdges.remove(edgeKey)
        // 添加新边
        outEdges[source]!!.add(data)
        inEdges[target]!!.add(data)
        allEdges[edgeKey] = data
    }

    fun removeEdge(source: String, target: String) {
        val edgeKey = "${source}\u0000${target}"
        outEdges[source]?.removeAll { it.target == target }
        inEdges[target]?.removeAll { it.source == source }
        allEdges.remove(edgeKey)
    }

    fun getEdge(source: String, target: String): EdgeData? {
        return outEdges[source]?.find { it.target == target }
    }

    fun hasEdge(source: String, target: String): Boolean {
        return outEdges[source]?.any { it.target == target } == true
    }

    /**
     * 返回所有边 — 保持全局插入顺序，对标 JS graphlib g.edges()
     *
     * 注意：不是按 source 分组的顺序，而是按边添加到图中的顺序。
     * 这对 normalize.run 中边的处理顺序至关重要。
     */
    fun edges(): List<EdgeData> {
        return allEdges.values.toList()
    }

    /** 返回所有边的 (source, target) 键对 */
    fun edgeKeys(): List<EdgeKey> {
        return edges().map { EdgeKey(it.source, it.target) }
    }

    fun edgeCount(): Int = edges().size

    fun inEdgesOf(nodeId: String): List<EdgeData> = inEdges[nodeId] ?: emptyList()

    fun outEdgesOf(nodeId: String): List<EdgeData> = outEdges[nodeId] ?: emptyList()

    fun predecessors(nodeId: String): List<String> =
        inEdgesOf(nodeId).map { it.source }.distinct()

    fun successors(nodeId: String): List<String> =
        outEdgesOf(nodeId).map { it.target }.distinct()

    // ====== 复合图（子图）操作 ======

    fun setParent(childId: String, parentId: String?) {
        if (!isCompound) return
        parent[childId] = parentId
        if (parentId != null) {
            childrenMap.getOrPut(parentId) { mutableSetOf() }.add(childId)
        }
    }

    fun getParent(nodeId: String): String? = parent[nodeId]

    fun children(parentId: String? = null): Set<String> {
        return if (parentId == null) {
            nodes.keys.filter { parent[it] == null }.toSet()
        } else {
            childrenMap[parentId] ?: emptySet()
        }
    }

    // ====== 遍历 ======

    /**
     * 拓扑排序
     */
    fun topologicalSort(): List<String> {
        val visited = mutableSetOf<String>()
        val result = mutableListOf<String>()

        fun visit(nodeId: String) {
            if (nodeId in visited) return
            visited.add(nodeId)
            for (edge in outEdgesOf(nodeId)) {
                visit(edge.target)
            }
            result.add(0, nodeId)
        }

        for (id in nodeIds()) {
            visit(id)
        }
        return result
    }

    /**
     * 获取没有入边的节点（根节点）
     */
    fun sources(): List<String> {
        return nodeIds().filter { inEdgesOf(it).isEmpty() }
    }

    /**
     * 获取没有出边的节点（叶节点）
     */
    fun sinks(): List<String> {
        return nodeIds().filter { outEdgesOf(it).isEmpty() }
    }

    /**
     * 返回节点的所有邻居（前驱 + 后继的并集）— 对标 graphlib Graph.neighbors(v)
     *
     * 用于 network simplex 的树遍历（无向图语义）。
     */
    fun neighbors(nodeId: String): List<String> {
        val preds = predecessors(nodeId)
        val sucs = successors(nodeId)
        return (preds + sucs).distinct()
    }

    /**
     * 返回与节点关联的所有边（入边 + 出边）— 对标 graphlib Graph.nodeEdges(v)
     *
     * 返回的 EdgeData 保留原始的 source/target，调用方可以通过比较 source == nodeId
     * 判断是出边还是入边。
     */
    fun nodeEdges(nodeId: String): List<EdgeData> {
        val inList = inEdgesOf(nodeId)
        val outList = outEdgesOf(nodeId)
        return inList + outList
    }
}
