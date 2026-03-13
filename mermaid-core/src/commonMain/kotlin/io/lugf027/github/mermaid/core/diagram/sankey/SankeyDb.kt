package io.lugf027.github.mermaid.core.diagram.sankey

import io.lugf027.github.mermaid.core.diagram.DiagramDB

/**
 * 桑基图数据库 - 对标 mermaid-js sankeyDb.ts
 *
 * 存储节点（SankeyNode）和链接（SankeyLink），CSV 格式数据。
 * 桑基图本质是带权重的有向无环图，用于展示流量/能量/资源在节点间的流动。
 */
class SankeyDb : DiagramDB {

    /** 桑基图节点 */
    data class SankeyNode(
        val id: String,
        // 布局计算后填充
        var x: Double = 0.0,
        var y: Double = 0.0,
        var width: Double = 0.0,
        var height: Double = 0.0
    )

    /** 桑基图链接 */
    data class SankeyLink(
        val source: String,
        val target: String,
        val value: Double
    )

    // --- 内部状态 ---
    private val nodes = mutableMapOf<String, SankeyNode>()
    private val links = mutableListOf<SankeyLink>()

    // --- DiagramDB ---
    private var diagramTitle = ""
    private var accTitle = ""
    private var accDescription = ""

    override fun clear() {
        nodes.clear()
        links.clear()
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

    // --- 操作 ---

    fun addLink(source: String, target: String, value: Double) {
        if (!nodes.containsKey(source)) {
            nodes[source] = SankeyNode(id = source)
        }
        if (!nodes.containsKey(target)) {
            nodes[target] = SankeyNode(id = target)
        }
        links.add(SankeyLink(source, target, value))
    }

    // --- 查询 ---

    fun getNodes(): List<SankeyNode> = nodes.values.toList()
    fun getLinks(): List<SankeyLink> = links.toList()
    fun getNodeById(id: String): SankeyNode? = nodes[id]

    /**
     * 获取节点的输入流量总和
     */
    fun getNodeInputValue(nodeId: String): Double =
        links.filter { it.target == nodeId }.sumOf { it.value }

    /**
     * 获取节点的输出流量总和
     */
    fun getNodeOutputValue(nodeId: String): Double =
        links.filter { it.source == nodeId }.sumOf { it.value }

    /**
     * 获取节点的最大流量（输入或输出中较大者）
     */
    fun getNodeValue(nodeId: String): Double =
        maxOf(getNodeInputValue(nodeId), getNodeOutputValue(nodeId))

    /**
     * 按拓扑排序获取节点层级
     */
    fun getNodeColumns(): List<List<String>> {
        val inDegree = mutableMapOf<String, Int>()
        for (node in nodes.keys) inDegree[node] = 0
        for (link in links) {
            inDegree[link.target] = (inDegree[link.target] ?: 0) + 1
        }

        val columns = mutableListOf<List<String>>()
        val remaining = inDegree.toMutableMap()

        while (remaining.isNotEmpty()) {
            val col = remaining.filter { it.value == 0 }.keys.toList()
            if (col.isEmpty()) break // 有环，退出避免无限循环
            columns.add(col)
            for (n in col) {
                remaining.remove(n)
                for (link in links) {
                    if (link.source == n && remaining.containsKey(link.target)) {
                        remaining[link.target] = (remaining[link.target] ?: 1) - 1
                    }
                }
            }
        }

        return columns
    }
}
