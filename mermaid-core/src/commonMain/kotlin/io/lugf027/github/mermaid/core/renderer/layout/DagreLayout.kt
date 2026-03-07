package io.lugf027.github.mermaid.core.renderer.layout

import io.lugf027.github.mermaid.core.types.*
import kotlin.math.max

/**
 * Dagre 层次布局算法的纯 Kotlin 实现。
 * 基于 Sugiyama 框架：
 * 1. Rank Assignment（层级分配）- 最长路径法
 * 2. Ordering（层内排序）- 重心法减少边交叉
 * 3. Coordinate Assignment（坐标分配）- 简化的 Brandes-Köpf 算法
 *
 * 对应 mermaid-js 使用的 dagre 布局库。
 */
class DagreLayout : LayoutEngine {
    override val name: String = "dagre"

    companion object {
        private const val DEFAULT_NODE_SEP = 50f
        private const val DEFAULT_RANK_SEP = 50f
        private const val DEFAULT_EDGE_SEP = 10f
        private const val DEFAULT_NODE_WIDTH = 100f
        private const val DEFAULT_NODE_HEIGHT = 40f
    }

    override fun layout(data: LayoutData): RenderData {
        if (data.nodes.isEmpty()) {
            return RenderData(emptyList(), emptyList(), emptyList(), Bounds())
        }

        val nodeSep = data.nodeSep.coerceAtLeast(10f)
        val rankSep = data.rankSep.coerceAtLeast(10f)
        val isHorizontal = data.direction == Direction.LR || data.direction == Direction.RL
        val isReversed = data.direction == Direction.BT || data.direction == Direction.RL

        // 构建图
        val nodeMap = mutableMapOf<String, Node>()
        data.nodes.forEach { node ->
            val n = node.copy(
                width = if (node.width > 0) node.width else DEFAULT_NODE_WIDTH,
                height = if (node.height > 0) node.height else DEFAULT_NODE_HEIGHT,
            )
            nodeMap[n.id] = n
        }

        val adjacency = mutableMapOf<String, MutableList<String>>()
        val reverseAdj = mutableMapOf<String, MutableList<String>>()
        data.edges.forEach { edge ->
            adjacency.getOrPut(edge.start) { mutableListOf() }.add(edge.end)
            reverseAdj.getOrPut(edge.end) { mutableListOf() }.add(edge.start)
        }

        // ─── 1. Rank Assignment (最长路径法) ─────────────────────
        val ranks = assignRanks(nodeMap.keys.toList(), adjacency, reverseAdj)
        if (isReversed) {
            val maxRank = ranks.values.maxOrNull() ?: 0
            ranks.forEach { (k, v) -> ranks[k] = maxRank - v }
        }

        // ─── 2. Ordering (层内排序 - 重心法) ────────────────────
        val layers = buildLayers(ranks)
        orderLayers(layers, adjacency, reverseAdj)

        // ─── 3. Coordinate Assignment ──────────────────────────
        assignCoordinates(layers, nodeMap, nodeSep, rankSep, isHorizontal)

        // 构建渲染结果
        val resultNodes = nodeMap.values.toList()
        val resultEdges = data.edges.map { edge ->
            val startNode = nodeMap[edge.start]
            val endNode = nodeMap[edge.end]
            if (startNode != null && endNode != null) {
                edge.copy(
                    points = listOf(
                        Point(startNode.x, startNode.y),
                        Point(endNode.x, endNode.y),
                    ),
                    labelPos = if (edge.label.isNotEmpty()) {
                        Point(
                            (startNode.x + endNode.x) / 2f,
                            (startNode.y + endNode.y) / 2f,
                        )
                    } else null,
                )
            } else edge
        }

        // 计算总边界
        val bounds = calculateBounds(resultNodes)

        return RenderData(
            nodes = resultNodes,
            edges = resultEdges,
            clusters = data.clusters,
            bounds = bounds,
            direction = data.direction,
        )
    }

    /**
     * 最长路径法分配层级。
     */
    private fun assignRanks(
        nodeIds: List<String>,
        adjacency: Map<String, List<String>>,
        reverseAdj: Map<String, List<String>>,
    ): MutableMap<String, Int> {
        val ranks = mutableMapOf<String, Int>()
        val visited = mutableSetOf<String>()

        fun dfs(nodeId: String): Int {
            if (nodeId in ranks) return ranks[nodeId]!!
            if (nodeId in visited) return 0 // 避免环
            visited.add(nodeId)

            val predecessors = reverseAdj[nodeId] ?: emptyList()
            val rank = if (predecessors.isEmpty()) 0
            else predecessors.maxOf { dfs(it) } + 1

            ranks[nodeId] = rank
            return rank
        }

        nodeIds.forEach { dfs(it) }

        // 确保所有节点都有 rank
        nodeIds.forEach { id ->
            if (id !in ranks) ranks[id] = 0
        }

        return ranks
    }

    /**
     * 根据 rank 将节点分层。
     */
    private fun buildLayers(ranks: Map<String, Int>): MutableList<MutableList<String>> {
        val maxRank = ranks.values.maxOrNull() ?: 0
        val layers = MutableList(maxRank + 1) { mutableListOf<String>() }
        ranks.forEach { (nodeId, rank) ->
            layers[rank].add(nodeId)
        }
        return layers
    }

    /**
     * 重心法减少层间边交叉。
     * 对每一层的节点按照其相邻层中连接节点的平均位置排序。
     */
    private fun orderLayers(
        layers: MutableList<MutableList<String>>,
        adjacency: Map<String, List<String>>,
        reverseAdj: Map<String, List<String>>,
    ) {
        // 向下扫描
        for (i in 1 until layers.size) {
            val prevLayer = layers[i - 1]
            val posMap = mutableMapOf<String, Int>()
            prevLayer.forEachIndexed { index, nodeId -> posMap[nodeId] = index }

            layers[i].sortBy { nodeId ->
                val predecessors = reverseAdj[nodeId] ?: emptyList()
                if (predecessors.isEmpty()) 0.0
                else predecessors.mapNotNull { posMap[it]?.toDouble() }.average()
            }
        }

        // 向上扫描
        for (i in layers.size - 2 downTo 0) {
            val nextLayer = layers[i + 1]
            val posMap = mutableMapOf<String, Int>()
            nextLayer.forEachIndexed { index, nodeId -> posMap[nodeId] = index }

            layers[i].sortBy { nodeId ->
                val successors = adjacency[nodeId] ?: emptyList()
                if (successors.isEmpty()) 0.0
                else successors.mapNotNull { posMap[it]?.toDouble() }.average()
            }
        }
    }

    /**
     * 分配节点坐标。
     * 两阶段：1) 初始左对齐布局 2) 基于连接关系居中调整。
     */
    private fun assignCoordinates(
        layers: List<List<String>>,
        nodeMap: MutableMap<String, Node>,
        nodeSep: Float,
        rankSep: Float,
        isHorizontal: Boolean,
    ) {
        val padding = 40f

        // 阶段 1：计算每层的宽度/高度，分配 rank 方向坐标
        data class LayerInfo(val totalSize: Float, val maxCross: Float)
        val layerInfos = mutableListOf<LayerInfo>()

        for (layer in layers) {
            var totalSize = 0f
            var maxCross = 0f
            for (nodeId in layer) {
                val node = nodeMap[nodeId] ?: continue
                if (isHorizontal) {
                    totalSize += node.height + nodeSep
                    maxCross = max(maxCross, node.width)
                } else {
                    totalSize += node.width + nodeSep
                    maxCross = max(maxCross, node.height)
                }
            }
            if (layer.isNotEmpty()) totalSize -= nodeSep // 最后一个节点后不需要间距
            layerInfos.add(LayerInfo(totalSize, maxCross))
        }

        // 计算所有层中最大的宽度（用于居中）
        val maxLayerSize = layerInfos.maxOfOrNull { it.totalSize } ?: 0f

        // 分配坐标
        var rankOffset = 0f
        for ((layerIndex, layer) in layers.withIndex()) {
            val info = layerInfos[layerIndex]
            // 居中偏移：让较窄的层在宽层中居中
            val centeringOffset = (maxLayerSize - info.totalSize) / 2f
            var posOffset = centeringOffset

            for (nodeId in layer) {
                val node = nodeMap[nodeId] ?: continue

                if (isHorizontal) {
                    node.x = rankOffset + padding + info.maxCross / 2f
                    node.y = posOffset + padding + node.height / 2f
                    posOffset += node.height + nodeSep
                } else {
                    node.x = posOffset + padding + node.width / 2f
                    node.y = rankOffset + padding + info.maxCross / 2f
                    posOffset += node.width + nodeSep
                }

                nodeMap[nodeId] = node
            }

            rankOffset += info.maxCross + rankSep
        }
    }

    /**
     * 计算所有节点的总边界框。
     */
    private fun calculateBounds(nodes: List<Node>): Bounds {
        if (nodes.isEmpty()) return Bounds()

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        nodes.forEach { node ->
            minX = kotlin.math.min(minX, node.x - node.width / 2f)
            minY = kotlin.math.min(minY, node.y - node.height / 2f)
            maxX = kotlin.math.max(maxX, node.x + node.width / 2f)
            maxY = kotlin.math.max(maxY, node.y + node.height / 2f)
        }

        return Bounds(
            x = minX,
            y = minY,
            width = maxX - minX,
            height = maxY - minY,
        )
    }
}
