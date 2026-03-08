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

    /**
     * 虚拟节点信息，用于 normalize/denormalize 边。
     * dagre 的 normalize.js 会为跨越 >1 层的边插入虚拟节点。
     */
    private data class DummyNode(
        val id: String,
        var rank: Int,
        var x: Float = 0f,
        var y: Float = 0f,
        val width: Float = 0f,
        val height: Float = 0f,
    )

    /**
     * 记录一条原始边被拆分后的虚拟节点链。
     */
    private data class DummyChain(
        val originalEdge: Edge,
        val dummyIds: List<String>,
    )

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

        // ─── 1.5 makeSpaceForEdgeLabels ─────────────────────────
        // dagre layout.js: graph.ranksep /= 2; edge.minlen *= 2
        // 将 ranksep 减半，同时所有 rank 翻倍。这样每条边至少跨 2 层，
        // 中间层用于放虚拟节点（为 edge label 留空间），但总间距保持不变。
        val effectiveRankSep = rankSep / 2f
        ranks.forEach { (k, v) -> ranks[k] = v * 2 }

        // ─── 1.6 Normalize: 插入虚拟节点 ────────────────────────
        // dagre normalize.js: 对跨越 >1 层的边，在中间每层插入虚拟节点。
        // 翻倍 rank 后，原来相邻的节点现在相隔 2 层，中间空出 1 层放虚拟节点。
        val dummyNodes = mutableMapOf<String, DummyNode>()
        val dummyChains = mutableListOf<DummyChain>()
        var dummyCounter = 0
        // 扩展邻接表以包含虚拟节点
        val extAdj = mutableMapOf<String, MutableList<String>>()
        val extRevAdj = mutableMapOf<String, MutableList<String>>()
        adjacency.forEach { (k, v) -> extAdj[k] = v.toMutableList() }
        reverseAdj.forEach { (k, v) -> extRevAdj[k] = v.toMutableList() }

        for (edge in data.edges) {
            val srcRank = ranks[edge.start] ?: continue
            val tgtRank = ranks[edge.end] ?: continue
            val span = tgtRank - srcRank
            if (span <= 1) continue  // 相邻层不需要虚拟节点

            // 从原始邻接表中移除直接连接
            extAdj[edge.start]?.remove(edge.end)
            extRevAdj[edge.end]?.remove(edge.start)

            val dummyIds = mutableListOf<String>()
            var prevId = edge.start
            for (r in srcRank + 1 until tgtRank) {
                val dId = "_dummy_${dummyCounter++}"
                dummyNodes[dId] = DummyNode(id = dId, rank = r)
                dummyIds.add(dId)
                ranks[dId] = r
                // 连接 prev → dummy
                extAdj.getOrPut(prevId) { mutableListOf() }.add(dId)
                extRevAdj.getOrPut(dId) { mutableListOf() }.add(prevId)
                prevId = dId
            }
            // 连接最后一个 dummy → target
            extAdj.getOrPut(prevId) { mutableListOf() }.add(edge.end)
            extRevAdj.getOrPut(edge.end) { mutableListOf() }.add(prevId)

            dummyChains.add(DummyChain(edge, dummyIds))
        }

        // ─── 2. Ordering (层内排序 - 重心法) ────────────────────
        val layers = buildLayers(ranks)
        orderLayers(layers, extAdj, extRevAdj)

        // ─── 3. Coordinate Assignment ──────────────────────────
        adjForLayout = extAdj
        reverseAdjForLayout = extRevAdj
        assignCoordinatesWithDummies(layers, nodeMap, dummyNodes, nodeSep, effectiveRankSep, isHorizontal)

        // ─── 4. Denormalize: 收集虚拟节点坐标为 edge.points ───
        // 记录哪些边有虚拟链
        val chainMap = mutableMapOf<String, DummyChain>()  // key = "start->end"
        for (chain in dummyChains) {
            chainMap["${chain.originalEdge.start}->${chain.originalEdge.end}"] = chain
        }

        val resultNodes = nodeMap.values.toList()
        val resultEdges = data.edges.map { edge ->
            val startNode = nodeMap[edge.start]
            val endNode = nodeMap[edge.end]
            if (startNode != null && endNode != null) {
                val chain = chainMap["${edge.start}->${edge.end}"]
                val points = if (chain != null) {
                    // 有虚拟节点链 → 收集虚拟节点坐标
                    // dagre: edge.points = [srcCenter, ...dummyPositions, tgtCenter]
                    val pts = mutableListOf(Point(startNode.x, startNode.y))
                    for (dId in chain.dummyIds) {
                        val dn = dummyNodes[dId] ?: continue
                        pts.add(Point(dn.x, dn.y))
                    }
                    pts.add(Point(endNode.x, endNode.y))
                    pts
                } else {
                    // 没有虚拟节点（相邻层）→ 直接 [src, tgt]
                    // dagre 的 assignNodeIntersects 会处理
                    listOf(Point(startNode.x, startNode.y), Point(endNode.x, endNode.y))
                }

                val labelPos = if (edge.label.isNotEmpty() && points.size >= 2) {
                    val midIdx = points.size / 2
                    if (points.size % 2 == 0) {
                        val a = points[midIdx - 1]
                        val b = points[midIdx]
                        Point((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                    } else points[midIdx]
                } else null
                edge.copy(points = points, labelPos = labelPos)
            } else edge
        }

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
     * 分配节点坐标（包含虚拟节点）。
     * 三阶段：
     * 1) 初始居中布局（每层节点居中对齐）
     * 2) 向下扫描：将节点对齐到其父节点的中心
     * 3) 向上扫描：将节点对齐到其子节点的中心
     * 匹配 dagre/Sugiyama 的 Brandes-Köpf 坐标分配思路。
     *
     * 虚拟节点宽度/高度为 0，参与横向位置计算但不影响层高。
     */
    private fun assignCoordinatesWithDummies(
        layers: List<List<String>>,
        nodeMap: MutableMap<String, Node>,
        dummyNodes: MutableMap<String, DummyNode>,
        nodeSep: Float,
        rankSep: Float,
        isHorizontal: Boolean,
    ) {
        val padding = 8f
        // dagre 的 edgeSep 默认为 10，虚拟节点之间或虚拟与真实节点之间使用 edgeSep
        val edgeSep = DEFAULT_EDGE_SEP

        // 帮助函数：获取节点在 pos 方向的尺寸
        fun nodeSize(nodeId: String): Float {
            if (nodeId.startsWith("_dummy_")) return 0f
            val node = nodeMap[nodeId] ?: return 0f
            return if (isHorizontal) node.height else node.width
        }

        // 判断节点是否是虚拟节点
        fun isDummy(nodeId: String) = nodeId.startsWith("_dummy_")

        // 获取两个相邻节点之间的间距（dagre: 虚拟节点间用 edgeSep，其他用 nodeSep）
        fun sepBetween(a: String, b: String): Float {
            return if (isDummy(a) || isDummy(b)) edgeSep else nodeSep
        }

        // 计算每层的 maxCross（rank 方向最大尺寸，虚拟节点层高度为 0）
        val layerMaxCross = layers.map { layer ->
            layer.maxOfOrNull { id ->
                if (isDummy(id)) return@maxOfOrNull 0f
                val node = nodeMap[id] ?: return@maxOfOrNull 0f
                if (isHorizontal) node.width else node.height
            } ?: 0f
        }

        // 阶段 1：初始布局 — 每层节点居中排列
        val layerWidths = layers.map { layer ->
            var total = 0f
            for (i in layer.indices) {
                total += nodeSize(layer[i])
                if (i < layer.size - 1) total += sepBetween(layer[i], layer[i + 1])
            }
            total
        }
        val maxLayerWidth = layerWidths.maxOrNull() ?: 0f

        val posMap = mutableMapOf<String, Float>()

        for ((layerIndex, layer) in layers.withIndex()) {
            val centeringOffset = (maxLayerWidth - layerWidths[layerIndex]) / 2f
            var offset = centeringOffset
            for (i in layer.indices) {
                val nodeId = layer[i]
                val ns = nodeSize(nodeId)
                posMap[nodeId] = offset + ns / 2f
                offset += ns
                if (i < layer.size - 1) offset += sepBetween(nodeId, layer[i + 1])
            }
        }

        // 阶段 2：向下扫描 — 子节点对齐到父节点中心
        for (i in 1 until layers.size) {
            val layer = layers[i]
            for (nodeId in layer) {
                val predecessors = reverseAdjForLayout[nodeId] ?: continue
                if (predecessors.isEmpty()) continue
                val parentPositions = predecessors.mapNotNull { posMap[it] }
                if (parentPositions.isNotEmpty()) {
                    posMap[nodeId] = parentPositions.average().toFloat()
                }
            }
            resolveOverlapsWithDummies(layer, posMap, nodeMap, dummyNodes, nodeSep, edgeSep, isHorizontal)
        }

        // 阶段 3：向上扫描 — 微调
        for (i in layers.size - 2 downTo 0) {
            val layer = layers[i]
            for (nodeId in layer) {
                val successors = adjForLayout[nodeId] ?: continue
                if (successors.isEmpty()) continue
                val childPositions = successors.mapNotNull { posMap[it] }
                if (childPositions.isNotEmpty()) {
                    posMap[nodeId] = childPositions.average().toFloat()
                }
            }
            resolveOverlapsWithDummies(layer, posMap, nodeMap, dummyNodes, nodeSep, edgeSep, isHorizontal)
        }

        // 最终分配坐标到 Node 对象和虚拟节点
        var rankOffset = 0f
        for ((layerIndex, layer) in layers.withIndex()) {
            val maxCross = layerMaxCross[layerIndex]
            for (nodeId in layer) {
                val p = posMap[nodeId] ?: continue
                if (isDummy(nodeId)) {
                    val dn = dummyNodes[nodeId] ?: continue
                    if (isHorizontal) {
                        dn.x = rankOffset + padding + maxCross / 2f
                        dn.y = p + padding
                    } else {
                        dn.x = p + padding
                        dn.y = rankOffset + padding + maxCross / 2f
                    }
                } else {
                    val node = nodeMap[nodeId] ?: continue
                    if (isHorizontal) {
                        node.x = rankOffset + padding + maxCross / 2f
                        node.y = p + padding
                    } else {
                        node.x = p + padding
                        node.y = rankOffset + padding + maxCross / 2f
                    }
                    nodeMap[nodeId] = node
                }
            }
            rankOffset += maxCross + rankSep
        }
    }

    /**
     * 解决同层节点重叠（支持虚拟节点）。
     * 虚拟节点宽度为 0，与其他节点之间使用 edgeSep。
     */
    private fun resolveOverlapsWithDummies(
        layer: List<String>,
        posMap: MutableMap<String, Float>,
        nodeMap: Map<String, Node>,
        dummyNodes: Map<String, DummyNode>,
        nodeSep: Float,
        edgeSep: Float,
        isHorizontal: Boolean,
    ) {
        if (layer.size <= 1) return

        fun isDummy(id: String) = id.startsWith("_dummy_")
        fun sizeOf(id: String): Float {
            if (isDummy(id)) return 0f
            val node = nodeMap[id] ?: return 0f
            return if (isHorizontal) node.height else node.width
        }

        val sorted = layer.sortedBy { posMap[it] ?: 0f }
        val oldCenter = sorted.mapNotNull { posMap[it] }.average().toFloat()

        for (i in 1 until sorted.size) {
            val prev = sorted[i - 1]
            val curr = sorted[i]
            val prevSize = sizeOf(prev)
            val currSize = sizeOf(curr)
            val sep = if (isDummy(prev) || isDummy(curr)) edgeSep else nodeSep
            val prevPos = posMap[prev] ?: continue
            val currPos = posMap[curr] ?: continue
            val minDist = prevSize / 2f + currSize / 2f + sep
            if (currPos - prevPos < minDist) {
                posMap[curr] = prevPos + minDist
            }
        }

        val newCenter = sorted.mapNotNull { posMap[it] }.average().toFloat()
        val shift = oldCenter - newCenter
        for (nodeId in sorted) {
            posMap[nodeId] = (posMap[nodeId] ?: 0f) + shift
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
