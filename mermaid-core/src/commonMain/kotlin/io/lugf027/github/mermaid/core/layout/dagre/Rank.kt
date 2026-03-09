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
     * 规范化排名：使最小 rank 为 0
     */
    private fun normalizeRanks(graph: Graph) {
        val minRank = graph.getNodes().minOfOrNull { it.rank } ?: return
        if (minRank != 0) {
            for (node in graph.getNodes()) {
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
