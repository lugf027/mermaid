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
        val extra: MutableMap<String, Any> = mutableMapOf()
    )

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
        val extra: MutableMap<String, Any> = mutableMapOf()
    )

    /** 图级属性 */
    var rankdir: String = "TB"
    var rankSep: Double = 50.0
    var nodeSep: Double = 50.0
    var edgeSep: Double = 10.0
    var marginX: Double = 0.0
    var marginY: Double = 0.0

    // 内部数据
    private val nodes = mutableMapOf<String, NodeData>()
    private val inEdges = mutableMapOf<String, MutableList<EdgeData>>()
    private val outEdges = mutableMapOf<String, MutableList<EdgeData>>()
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
        nodes.remove(id)
        inEdges.remove(id)
        outEdges.remove(id)
        parent.remove(id)
        childrenMap.remove(id)
    }

    fun nodeIds(): Set<String> = nodes.keys.toSet()

    fun nodeCount(): Int = nodes.size

    fun getNodes(): List<NodeData> = nodes.values.toList()

    // ====== 边操作 ======

    fun setEdge(source: String, target: String, data: EdgeData = EdgeData(source, target)) {
        outEdges.getOrPut(source) { mutableListOf() }.add(data)
        inEdges.getOrPut(target) { mutableListOf() }.add(data)
    }

    fun getEdge(source: String, target: String): EdgeData? {
        return outEdges[source]?.find { it.target == target }
    }

    fun hasEdge(source: String, target: String): Boolean {
        return outEdges[source]?.any { it.target == target } == true
    }

    fun edges(): List<EdgeData> {
        return outEdges.values.flatten()
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
}
