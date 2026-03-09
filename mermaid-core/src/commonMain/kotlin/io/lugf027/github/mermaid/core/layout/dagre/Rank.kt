package io.lugf027.github.mermaid.core.layout.dagre

/**
 * 分层排名算法 - 对标 dagre rank 模块
 *
 * 使用 longest-path 算法为有向图中的每个节点分配层级（rank）。
 * Sugiyama 分层布局的第一步。
 */
object Rank {

    /**
     * 使用最长路径算法分配排名
     *
     * 确保每条边 e 满足：rank(target) - rank(source) >= minLen(e)
     */
    fun longestPath(graph: Graph) {
        val visited = mutableSetOf<String>()

        fun dfs(nodeId: String): Int {
            if (nodeId in visited) return graph.getNode(nodeId)?.rank ?: 0
            visited.add(nodeId)

            val outEdges = graph.outEdgesOf(nodeId)
            if (outEdges.isEmpty()) {
                graph.getNode(nodeId)?.rank = 0
                return 0
            }

            var maxRank = Int.MIN_VALUE
            for (edge in outEdges) {
                val targetRank = dfs(edge.target)
                maxRank = maxOf(maxRank, targetRank + edge.minLen)
            }

            graph.getNode(nodeId)?.rank = maxRank
            return maxRank
        }

        // 从所有 sink 节点开始反向 DFS
        val sinks = graph.sinks()
        for (sink in sinks) {
            graph.getNode(sink)?.rank = 0
        }

        // 从 source 节点开始 DFS
        for (source in graph.sources()) {
            dfs(source)
        }

        // 规范化：确保最小 rank 为 0
        normalizeRanks(graph)
    }

    /**
     * 规范化排名：反转 rank 使 source 节点 rank 最小，sink 节点 rank 最大，
     * 然后偏移使最小 rank 为 0。
     *
     * longestPath 算法将 sink 设为 rank=0，source 获得最高 rank。
     * 但标准 dagre 的语义是 source 在前（rank 小），target 在后（rank 大），
     * 所以需要反转。
     */
    private fun normalizeRanks(graph: Graph) {
        val nodes = graph.getNodes()
        if (nodes.isEmpty()) return

        // 反转 rank：maxRank - rank，使 source 节点 rank 最小
        val maxRank = nodes.maxOf { it.rank }
        for (node in nodes) {
            node.rank = maxRank - node.rank
        }

        // 确保最小 rank 为 0
        val minRank = nodes.minOf { it.rank }
        if (minRank != 0) {
            for (node in nodes) {
                node.rank -= minRank
            }
        }
    }

    /**
     * 获取最大 rank 值
     */
    fun maxRank(graph: Graph): Int {
        return graph.getNodes().maxOfOrNull { it.rank } ?: 0
    }

    /**
     * 按 rank 分层，返回每层的节点 ID 列表
     */
    fun layers(graph: Graph): List<List<String>> {
        val max = maxRank(graph)
        val result = (0..max).map { rank ->
            graph.getNodes().filter { it.rank == rank }.map { it.id }
        }
        return result
    }
}
