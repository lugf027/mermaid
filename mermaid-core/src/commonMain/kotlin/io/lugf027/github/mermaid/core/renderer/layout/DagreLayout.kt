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
        // 缓存邻接表供 assignCoordinates 使用
        adjForLayout = adjacency
        reverseAdjForLayout = reverseAdj
        assignCoordinates(layers, nodeMap, nodeSep, rankSep, isHorizontal)

        // 构建渲染结果
        val resultNodes = nodeMap.values.toList()
        val resultEdges = data.edges.map { edge ->
            val startNode = nodeMap[edge.start]
            val endNode = nodeMap[edge.end]
            if (startNode != null && endNode != null) {
                val points = computeEdgePoints(startNode, endNode, isHorizontal, rankSep)
                val labelPos = if (edge.label.isNotEmpty()) {
                    // 标签放在路径的中间点
                    if (points.size >= 2) {
                        val midIdx = points.size / 2
                        if (points.size % 2 == 0) {
                            val a = points[midIdx - 1]
                            val b = points[midIdx]
                            Point((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                        } else points[midIdx]
                    } else points.first()
                } else null
                edge.copy(points = points, labelPos = labelPos)
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
     * 三阶段：
     * 1) 初始居中布局（每层节点居中对齐）
     * 2) 向下扫描：将节点对齐到其父节点的中心
     * 3) 向上扫描：将节点对齐到其子节点的中心
     * 匹配 dagre/Sugiyama 的 Brandes-Köpf 坐标分配思路。
     */
    private fun assignCoordinates(
        layers: List<List<String>>,
        nodeMap: MutableMap<String, Node>,
        nodeSep: Float,
        rankSep: Float,
        isHorizontal: Boolean,
    ) {
        // mermaid-js dagre 使用 marginx=8, marginy=8
        val padding = 8f

        // 帮助函数：获取节点在 pos 方向的尺寸
        fun nodeSize(nodeId: String): Float {
            val node = nodeMap[nodeId] ?: return 0f
            return if (isHorizontal) node.height else node.width
        }

        // 计算每层的 maxCross（rank 方向最大尺寸）
        val layerMaxCross = layers.map { layer ->
            layer.maxOfOrNull { id ->
                val node = nodeMap[id] ?: return@maxOfOrNull 0f
                if (isHorizontal) node.width else node.height
            } ?: 0f
        }

        // 阶段 1：初始布局 — 每层节点从左对齐开始
        // 先计算每层的总宽度
        val layerWidths = layers.map { layer ->
            var total = 0f
            for (id in layer) total += nodeSize(id) + nodeSep
            if (layer.isNotEmpty()) total -= nodeSep
            total
        }
        val maxLayerWidth = layerWidths.maxOrNull() ?: 0f

        // 记录每个节点的 pos（横向坐标）
        val posMap = mutableMapOf<String, Float>()

        // 初始化：每层居中排列
        for ((layerIndex, layer) in layers.withIndex()) {
            val centeringOffset = (maxLayerWidth - layerWidths[layerIndex]) / 2f
            var offset = centeringOffset
            for (nodeId in layer) {
                val ns = nodeSize(nodeId)
                posMap[nodeId] = offset + ns / 2f
                offset += ns + nodeSep
            }
        }

        // 阶段 2：向下扫描 — 子节点对齐到父节点中心（平均值）
        for (i in 1 until layers.size) {
            val layer = layers[i]
            for (nodeId in layer) {
                val predecessors = reverseAdjForLayout[nodeId] ?: continue
                if (predecessors.isEmpty()) continue
                val parentPositions = predecessors.mapNotNull { posMap[it] }
                if (parentPositions.isNotEmpty()) {
                    val avg = parentPositions.average().toFloat()
                    posMap[nodeId] = avg
                }
            }
            // 解决重叠：确保同层节点不重叠
            resolveOverlaps(layer, posMap, nodeMap, nodeSep, isHorizontal)
        }

        // 阶段 3：向上扫描 — 微调，将父节点对齐到子节点中心（平均值）
        for (i in layers.size - 2 downTo 0) {
            val layer = layers[i]
            for (nodeId in layer) {
                val successors = adjForLayout[nodeId] ?: continue
                if (successors.isEmpty()) continue
                val childPositions = successors.mapNotNull { posMap[it] }
                if (childPositions.isNotEmpty()) {
                    val avg = childPositions.average().toFloat()
                    posMap[nodeId] = avg
                }
            }
            resolveOverlaps(layer, posMap, nodeMap, nodeSep, isHorizontal)
        }

        // 最终分配坐标到 Node 对象
        var rankOffset = 0f
        for ((layerIndex, layer) in layers.withIndex()) {
            val maxCross = layerMaxCross[layerIndex]
            for (nodeId in layer) {
                val node = nodeMap[nodeId] ?: continue
                val p = posMap[nodeId] ?: continue

                if (isHorizontal) {
                    node.x = rankOffset + padding + maxCross / 2f
                    node.y = p + padding
                } else {
                    node.x = p + padding
                    node.y = rankOffset + padding + maxCross / 2f
                }
                nodeMap[nodeId] = node
            }
            rankOffset += maxCross + rankSep
        }
    }

    /**
     * 解决同层节点重叠，然后重新居中。
     * 1. 按当前 pos 排序，确保相邻节点之间至少有 nodeSep 间距
     * 2. 将整层节点平移回中心位置，避免单方向推导致偏移
     */
    private fun resolveOverlaps(
        layer: List<String>,
        posMap: MutableMap<String, Float>,
        nodeMap: Map<String, Node>,
        nodeSep: Float,
        isHorizontal: Boolean,
    ) {
        if (layer.size <= 1) return

        val sorted = layer.sortedBy { posMap[it] ?: 0f }

        // 记录重叠解决前的中心
        val oldCenter = sorted.mapNotNull { posMap[it] }.average().toFloat()

        // 向右推解决重叠
        for (i in 1 until sorted.size) {
            val prev = sorted[i - 1]
            val curr = sorted[i]
            val prevNode = nodeMap[prev] ?: continue
            val currNode = nodeMap[curr] ?: continue
            val prevSize = if (isHorizontal) prevNode.height else prevNode.width
            val currSize = if (isHorizontal) currNode.height else currNode.width
            val prevPos = posMap[prev] ?: continue
            val currPos = posMap[curr] ?: continue
            val minDist = prevSize / 2f + currSize / 2f + nodeSep
            if (currPos - prevPos < minDist) {
                posMap[curr] = prevPos + minDist
            }
        }

        // 重叠解决后，将整层平移回原来的中心
        val newCenter = sorted.mapNotNull { posMap[it] }.average().toFloat()
        val shift = oldCenter - newCenter
        for (nodeId in sorted) {
            posMap[nodeId] = (posMap[nodeId] ?: 0f) + shift
        }
    }

    /**
     * 计算边的路径点（模拟 dagre 行为）。
     *
     * dagre 通过 makeSpaceForEdgeLabels 将 minlen*2，使所有边都会创建
     * 虚拟节点（dummy node）。虚拟节点的坐标成为边的中间控制点。
     *
     * 对于 TB/BT 布局：
     * - 同列节点（|dx| < 1）：3 个点（src → midPoint → tgt），midPoint 在正中间
     * - 不同列节点：3 个点，midPoint 在两层中间，x = 目标节点的 x
     *
     * mermaid-js 的 edges.js 去掉首尾后保留 [midPoint]，然后加 intersect 交点。
     * generateRoundedPath(radius=5) 在 midPoint 处生成圆角弯曲。
     */
    private fun computeEdgePoints(
        startNode: Node,
        endNode: Node,
        isHorizontal: Boolean,
        rankSep: Float,
    ): List<Point> {
        val sx = startNode.x
        val sy = startNode.y
        val ex = endNode.x
        val ey = endNode.y

        if (isHorizontal) {
            val dy = kotlin.math.abs(ey - sy)
            // 水平布局中间点
            val midX = (sx + ex) / 2f
            val midY = if (dy < 1f) sy else (sy + ey) / 2f
            return listOf(
                Point(sx, sy),
                Point(midX, midY),
                Point(ex, ey),
            )
        } else {
            // 垂直布局（TB/BT）
            // midY 在源节点底边和目标节点顶边的中间
            val srcBottom = sy + startNode.height / 2f
            val tgtTop = ey - endNode.height / 2f
            val midY = (srcBottom + tgtTop) / 2f

            val dx = kotlin.math.abs(ex - sx)
            if (dx < 1f) {
                // 同列：中间点在正中间（垂直直线经过一个中间控制点）
                return listOf(
                    Point(sx, sy),
                    Point(sx, midY),
                    Point(ex, ey),
                )
            }

            // 不同列：虚拟节点 x 坐标靠近目标节点
            // dagre 的 ordering + position 会让虚拟节点在目标节点附近
            // 这样 intersect 计算出的交点更匹配 mermaid-js 的视觉效果
            val midX = ex
            return listOf(
                Point(sx, sy),
                Point(midX, midY),
                Point(ex, ey),
            )
        }
    }

    // 在 layout() 中缓存的邻接表引用，供 assignCoordinates 使用
    private var adjForLayout: Map<String, List<String>> = emptyMap()
    private var reverseAdjForLayout: Map<String, List<String>> = emptyMap()

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
