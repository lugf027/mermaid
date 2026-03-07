package io.lugf027.github.mermaid.core.renderer.layout

/**
 * 图数据结构。
 * 提供有向图的节点/边管理、拓扑排序等基础操作。
 * 对应 mermaid-js 使用的 graphlib 库。
 */
class GraphLib<N, E> {
    /** 节点数据 */
    private val nodeData = mutableMapOf<String, N>()

    /** 邻接表（出边） */
    private val outEdges = mutableMapOf<String, MutableList<GraphEdge<E>>>()

    /** 邻接表（入边） */
    private val inEdges = mutableMapOf<String, MutableList<GraphEdge<E>>>()

    /** 所有边列表 */
    private val edgeList = mutableListOf<GraphEdge<E>>()

    /** 节点顺序 */
    private val nodeOrder = mutableListOf<String>()

    data class GraphEdge<E>(
        val source: String,
        val target: String,
        val data: E,
        val name: String = "",
    )

    /** 添加节点 */
    fun setNode(id: String, data: N) {
        if (id !in nodeData) {
            nodeOrder.add(id)
            outEdges[id] = mutableListOf()
            inEdges[id] = mutableListOf()
        }
        nodeData[id] = data
    }

    /** 获取节点数据 */
    fun getNode(id: String): N? = nodeData[id]

    /** 移除节点 */
    fun removeNode(id: String) {
        nodeData.remove(id)
        nodeOrder.remove(id)
        outEdges.remove(id)
        inEdges.remove(id)
        edgeList.removeAll { it.source == id || it.target == id }
    }

    /** 检查节点是否存在 */
    fun hasNode(id: String): Boolean = id in nodeData

    /** 获取所有节点 ID */
    fun nodes(): List<String> = nodeOrder.toList()

    /** 获取节点数量 */
    fun nodeCount(): Int = nodeData.size

    /** 添加边 */
    fun setEdge(source: String, target: String, data: E, name: String = "") {
        val edge = GraphEdge(source, target, data, name)
        outEdges.getOrPut(source) { mutableListOf() }.add(edge)
        inEdges.getOrPut(target) { mutableListOf() }.add(edge)
        edgeList.add(edge)
    }

    /** 获取边数据 */
    fun getEdge(source: String, target: String): E? {
        return outEdges[source]?.find { it.target == target }?.data
    }

    /** 检查边是否存在 */
    fun hasEdge(source: String, target: String): Boolean {
        return outEdges[source]?.any { it.target == target } == true
    }

    /** 获取所有边 */
    fun edges(): List<GraphEdge<E>> = edgeList.toList()

    /** 获取边数量 */
    fun edgeCount(): Int = edgeList.size

    /** 获取节点的后继（出边目标） */
    fun successors(id: String): List<String> {
        return outEdges[id]?.map { it.target } ?: emptyList()
    }

    /** 获取节点的前驱（入边来源） */
    fun predecessors(id: String): List<String> {
        return inEdges[id]?.map { it.source } ?: emptyList()
    }

    /** 获取节点的出边 */
    fun outEdgesOf(id: String): List<GraphEdge<E>> {
        return outEdges[id]?.toList() ?: emptyList()
    }

    /** 获取节点的入边 */
    fun inEdgesOf(id: String): List<GraphEdge<E>> {
        return inEdges[id]?.toList() ?: emptyList()
    }

    /** 获取节点的度（入度 + 出度） */
    fun degree(id: String): Int {
        return (outEdges[id]?.size ?: 0) + (inEdges[id]?.size ?: 0)
    }

    /** 获取节点的入度 */
    fun inDegree(id: String): Int = inEdges[id]?.size ?: 0

    /** 获取节点的出度 */
    fun outDegree(id: String): Int = outEdges[id]?.size ?: 0

    /** 获取源节点（入度为0） */
    fun sources(): List<String> {
        return nodeOrder.filter { inDegree(it) == 0 }
    }

    /** 获取汇节点（出度为0） */
    fun sinks(): List<String> {
        return nodeOrder.filter { outDegree(it) == 0 }
    }

    /**
     * 拓扑排序（Kahn 算法）。
     * @return 排序后的节点 ID 列表，如果有环则返回 null
     */
    fun topologicalSort(): List<String>? {
        val inDegrees = mutableMapOf<String, Int>()
        nodeOrder.forEach { inDegrees[it] = inDegree(it) }

        val queue = ArrayDeque<String>()
        inDegrees.filter { it.value == 0 }.forEach { queue.add(it.key) }

        val result = mutableListOf<String>()
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            result.add(node)
            for (succ in successors(node)) {
                val newDeg = (inDegrees[succ] ?: 0) - 1
                inDegrees[succ] = newDeg
                if (newDeg == 0) queue.add(succ)
            }
        }
        return if (result.size == nodeCount()) result else null
    }

    /** 深拷贝 */
    fun copy(): GraphLib<N, E> {
        val g = GraphLib<N, E>()
        for (id in nodeOrder) {
            nodeData[id]?.let { g.setNode(id, it) }
        }
        for (edge in edgeList) {
            g.setEdge(edge.source, edge.target, edge.data, edge.name)
        }
        return g
    }
}
