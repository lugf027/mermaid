package io.lugf027.github.mermaid.core.layout.dagre

import kotlin.math.max
import kotlin.math.min

/**
 * 层内排序算法 - 精确对标 dagre order 模块
 *
 * 完整实现 dagre 的 ordering 算法：
 * 1. initOrder: DFS-based 初始排序
 * 2. buildLayerGraph: 为每层构建排序子图
 * 3. sortSubgraph: 递归子图排序（barycenter + resolveConflicts + sort）
 * 4. crossCount: Barth 双层交叉计数
 * 5. 迭代优化: 交替上下扫描，保留最优方案
 */
object Order {

    /**
     * 主入口 — 对标 dagre order/index.js order(g)
     *
     * 对各层节点进行排序，最小化边交叉。
     * 使用迭代优化：交替从上到下和从下到上扫描，保留交叉数最少的方案。
     */
    fun order(graph: Graph) {
        val maxRank = Rank.maxRank(graph)

        // 构建每层的 layer graph — 对标 buildLayerGraphs
        val downLayerGraphs = (1..maxRank).map { rank ->
            buildLayerGraph(graph, rank, "inEdges")
        }
        val upLayerGraphs = (maxRank - 1 downTo 0).map { rank ->
            buildLayerGraph(graph, rank, "outEdges")
        }

        // 初始排序 — 对标 initOrder
        val layering = initOrder(graph, maxRank)
        assignOrder(graph, layering)

        var bestCC = Double.POSITIVE_INFINITY
        var best: List<List<String>>? = null

        // 迭代优化 — 对标 order/index.js 的主循环
        var i = 0
        var lastBest = 0
        while (lastBest < 4) {
            val layerGraphs = if (i % 2 == 1) downLayerGraphs else upLayerGraphs
            val biasRight = i % 4 >= 2
            sweepLayerGraphs(graph, layerGraphs, biasRight)

            val currentLayering = buildLayerMatrix(graph, maxRank)
            val cc = crossCount(graph, currentLayering)

            if (cc < bestCC) {
                lastBest = 0
                best = currentLayering.map { it.toList() }
                bestCC = cc
            } else {
                lastBest++
            }

            i++
            // 安全限制，防止无限循环
            if (i > 100) break
        }

        if (best != null) {
            assignOrder(graph, best)
        }
    }

    // ========================================================================
    // initOrder — 对标 dagre order/init-order.js
    // ========================================================================

    /**
     * 初始排序 — 对标 initOrder(g)
     *
     * 使用 DFS 从 rank 最小的节点开始遍历，按首次访问顺序分配初始 order。
     * 这来自 Gansner 等人的论文 "A Technique for Drawing Directed Graphs"。
     */
    private fun initOrder(graph: Graph, maxRank: Int): List<List<String>> {
        val visited = mutableSetOf<String>()
        val layers = (0..maxRank).map { mutableListOf<String>() }

        // 获取所有简单节点（没有子节点的节点，对标 !g.children(v).length）
        val simpleNodes = graph.nodeIds().filter { graph.children(it).isEmpty() }

        // 按 rank 排序
        val orderedVs = simpleNodes.sortedBy { graph.getNode(it)?.rank ?: 0 }

        fun dfs(v: String) {
            if (v in visited) return
            visited.add(v)
            val node = graph.getNode(v) ?: return
            val rank = node.rank
            if (rank in layers.indices) {
                layers[rank].add(v)
            }
            for (successor in graph.successors(v)) {
                dfs(successor)
            }
        }

        for (v in orderedVs) {
            dfs(v)
        }

        return layers
    }

    // ========================================================================
    // buildLayerGraph — 对标 dagre order/build-layer-graph.js
    // ========================================================================

    /**
     * 层图数据 — 用于排序的子图
     *
     * 简化版：因为 mermaid flowchart 不使用复合图（subgraph 在 dagre 层面没有嵌套），
     * 所有节点都直接挂在 root 下。
     */
    data class LayerGraph(
        val root: String,
        val graph: Graph,
        val originalGraph: Graph,
        val rank: Int
    )

    /**
     * 构建层图 — 对标 buildLayerGraph(g, rank, relationship)
     *
     * 为指定 rank 层构建排序子图。
     *
     * 关键：dagre JS 中 graphlib 节点是引用语义，layer graph 和原始图共享同一个
     * 节点对象。当 sweepLayerGraphs 更新一个 layer 的 order 时，其他 layer graph
     * 中引用该节点的 order 也会自动更新。Kotlin 中通过直接传递 NodeData 对象
     * （而非 copy）来实现相同效果。
     */
    private fun buildLayerGraph(g: Graph, rank: Int, relationship: String): LayerGraph {
        val root = "_root_${rank}_${relationship}"
        val result = Graph(isDirected = true, isMultigraph = false, isCompound = true)
        result.setNode(root)

        for (v in g.nodeIds()) {
            val node = g.getNode(v) ?: continue
            if (node.rank == rank) {
                // 直接使用原始图的 NodeData 引用（不复制），对标 JS graphlib 的引用语义
                result.setNode(v, node)
                result.setParent(v, root)

                // 获取相关边
                val edges = if (relationship == "inEdges") {
                    g.inEdgesOf(v)
                } else {
                    g.outEdgesOf(v)
                }

                for (e in edges) {
                    val u = if (e.source == v) e.target else e.source
                    // 确保 u 节点在 result 中存在 — 同样使用原始引用
                    if (!result.hasNode(u)) {
                        val uNode = g.getNode(u)
                        if (uNode != null) {
                            result.setNode(u, uNode)
                        }
                    }

                    // 设置边（聚合权重）
                    val existingEdge = result.getEdge(u, v)
                    val existingWeight = existingEdge?.weight ?: 0.0
                    val edgeWeight = e.weight
                    result.setEdge(u, v, Graph.EdgeData(
                        source = u,
                        target = v,
                        weight = edgeWeight + existingWeight
                    ))
                }
            }
        }

        return LayerGraph(root, result, g, rank)
    }

    // ========================================================================
    // sweepLayerGraphs — 对标 dagre order/index.js sweepLayerGraphs
    // ========================================================================

    /**
     * 扫描层图 — 对标 sweepLayerGraphs(layerGraphs, biasRight)
     */
    private fun sweepLayerGraphs(originalGraph: Graph, layerGraphs: List<LayerGraph>, biasRight: Boolean) {
        val cg = Graph(isDirected = true, isMultigraph = false, isCompound = false)

        for (lg in layerGraphs) {
            val sorted = sortSubgraph(lg.graph, lg.root, cg, biasRight)
            for ((i, v) in sorted.withIndex()) {
                lg.graph.getNode(v)?.order = i
                // 同步回原始图
                originalGraph.getNode(v)?.order = i
            }
            addSubgraphConstraints(lg.graph, cg, sorted)
        }
    }

    // ========================================================================
    // sortSubgraph — 对标 dagre order/sort-subgraph.js
    // ========================================================================

    /**
     * 子图排序 — 对标 sortSubgraph(g, v, cg, biasRight)
     *
     * 对子图 v 下的所有子节点进行排序。
     *
     * @return 排序后的节点列表
     */
    private fun sortSubgraph(g: Graph, v: String, cg: Graph, biasRight: Boolean): List<String> {
        val movable = g.children(v).toMutableList()
        val node = g.getNode(v)
        val bl = node?.extra?.get("borderLeft") as? String
        val br = node?.extra?.get("borderRight") as? String

        if (bl != null) {
            movable.removeAll { it == bl || it == br }
        }

        // 计算重心 — 对标 barycenter(g, movable)
        val barycenters = barycenter(g, movable)
        val subgraphs = mutableMapOf<String, List<String>>()

        for (entry in barycenters) {
            val children = g.children(entry.v)
            if (children.isNotEmpty()) {
                val subgraphResult = sortSubgraph(g, entry.v, cg, biasRight)
                subgraphs[entry.v] = subgraphResult
                // 注意：完整 dagre 在此处会合并 barycenter。
                // 简化处理：子图的重心不再递归合并，因为 mermaid flowchart 不嵌套子图
            }
        }

        // 解决冲突 — 对标 resolveConflicts(barycenters, cg)
        val entries = resolveConflicts(barycenters, cg)

        // 展开子图 — 对标 expandSubgraphs(entries, subgraphs)
        for (entry in entries) {
            entry.vs = entry.vs.flatMap { nodeId ->
                subgraphs[nodeId] ?: listOf(nodeId)
            }.toMutableList()
        }

        // 排序 — 对标 sort(entries, biasRight)
        val result = sort(entries, biasRight)

        // 处理边界节点
        if (bl != null && br != null) {
            val vs = mutableListOf<String>()
            vs.add(bl)
            vs.addAll(result)
            vs.add(br)
            return vs
        }

        return result
    }

    // ========================================================================
    // barycenter — 对标 dagre order/barycenter.js
    // ========================================================================

    /**
     * 重心结果
     */
    data class BarycenterEntry(
        val v: String,
        var barycenter: Double? = null,  // null 表示没有入边
        var weight: Double? = null
    )

    /**
     * 计算重心 — 对标 barycenter(g, movable)
     *
     * 对每个可移动节点，根据入边的权重和源节点的 order 值计算加权重心。
     */
    private fun barycenter(g: Graph, movable: Collection<String>): List<BarycenterEntry> {
        return movable.map { v ->
            val inEdges = g.inEdgesOf(v)
            if (inEdges.isEmpty()) {
                BarycenterEntry(v)
            } else {
                var sum = 0.0
                var weight = 0.0
                for (e in inEdges) {
                    val edgeData = g.getEdge(e.source, e.target)
                    val edgeWeight = edgeData?.weight ?: 1.0
                    val nodeU = g.getNode(e.source)
                    val uOrder = nodeU?.order ?: 0
                    sum += edgeWeight * uOrder
                    weight += edgeWeight
                }
                BarycenterEntry(v, barycenter = sum / weight, weight = weight)
            }
        }
    }

    // ========================================================================
    // resolveConflicts — 对标 dagre order/resolve-conflicts.js
    // ========================================================================

    /**
     * 排序条目
     */
    data class SortEntry(
        var vs: MutableList<String>,
        var i: Int,
        var barycenter: Double? = null,
        var weight: Double? = null,
        var merged: Boolean = false,
        val inEntries: MutableList<SortEntry> = mutableListOf(),
        val outEntries: MutableList<SortEntry> = mutableListOf(),
        var indegree: Int = 0
    )

    /**
     * 解决冲突 — 对标 resolveConflicts(entries, cg)
     *
     * 基于 Forster 的论文 "A Fast and Simple Heuristic for Constrained
     * Two-Level Crossing Reduction"。
     */
    private fun resolveConflicts(entries: List<BarycenterEntry>, cg: Graph): MutableList<SortEntry> {
        val mappedEntries = mutableMapOf<String, SortEntry>()

        for ((i, entry) in entries.withIndex()) {
            val sortEntry = SortEntry(
                vs = mutableListOf(entry.v),
                i = i,
                barycenter = entry.barycenter,
                weight = entry.weight
            )
            mappedEntries[entry.v] = sortEntry
        }

        // 处理约束图的边
        for (edge in cg.edges()) {
            val entryV = mappedEntries[edge.source]
            val entryW = mappedEntries[edge.target]
            if (entryV != null && entryW != null) {
                entryW.indegree++
                entryV.outEntries.add(entryW)
            }
        }

        // 找到所有没有入度的条目
        val sourceSet = mappedEntries.values.filter { it.indegree == 0 }.toMutableList()

        return doResolveConflicts(sourceSet)
    }

    /**
     * 执行冲突解决 — 对标 doResolveConflicts(sourceSet)
     */
    private fun doResolveConflicts(sourceSet: MutableList<SortEntry>): MutableList<SortEntry> {
        val result = mutableListOf<SortEntry>()

        while (sourceSet.isNotEmpty()) {
            val entry = sourceSet.removeLast()
            result.add(entry)

            // handleIn: 处理入边 — 对标 handleIn(vEntry)
            for (uEntry in entry.inEntries.reversed()) {
                if (uEntry.merged) continue
                if (uEntry.barycenter == null || entry.barycenter == null ||
                    uEntry.barycenter!! >= entry.barycenter!!) {
                    mergeEntries(entry, uEntry)
                }
            }

            // handleOut: 处理出边 — 对标 handleOut(vEntry)
            for (wEntry in entry.outEntries) {
                wEntry.inEntries.add(entry)
                wEntry.indegree--
                if (wEntry.indegree == 0) {
                    sourceSet.add(wEntry)
                }
            }
        }

        return result.filter { !it.merged }.toMutableList()
    }

    /**
     * 合并条目 — 对标 mergeEntries(target, source)
     */
    private fun mergeEntries(target: SortEntry, source: SortEntry) {
        var sum = 0.0
        var weight = 0.0

        if (target.weight != null && target.weight!! > 0) {
            sum += target.barycenter!! * target.weight!!
            weight += target.weight!!
        }
        if (source.weight != null && source.weight!! > 0) {
            sum += source.barycenter!! * source.weight!!
            weight += source.weight!!
        }

        // 注意：dagre 是 source.vs.concat(target.vs)，source 在前
        target.vs = (source.vs + target.vs).toMutableList()
        if (weight > 0) {
            target.barycenter = sum / weight
            target.weight = weight
        }
        target.i = min(source.i, target.i)
        source.merged = true
    }

    // ========================================================================
    // sort — 对标 dagre order/sort.js
    // ========================================================================

    /**
     * 排序 — 对标 sort(entries, biasRight)
     *
     * @return 排序后的节点列表
     */
    private fun sort(entries: List<SortEntry>, biasRight: Boolean): List<String> {
        // 分为有重心值和无重心值两组
        val sortable = mutableListOf<SortEntry>()
        val unsortable = mutableListOf<SortEntry>()

        for (entry in entries) {
            if (entry.barycenter != null) {
                sortable.add(entry)
            } else {
                unsortable.add(entry)
            }
        }

        // 按 -i 排序 unsortable（即 i 从大到小）
        unsortable.sortByDescending { it.i }

        // sortable 按 barycenter 排序，相等时按 bias
        sortable.sortWith(Comparator { a, b ->
            val ba = a.barycenter!!
            val bb = b.barycenter!!
            if (ba < bb) return@Comparator -1
            if (ba > bb) return@Comparator 1
            // 相等时根据 bias
            if (!biasRight) a.i - b.i else b.i - a.i
        })

        // 穿插排列
        val vs = mutableListOf<List<String>>()
        var vsIndex = 0

        // 消费 unsortable（开头）
        vsIndex = consumeUnsortable(vs, unsortable, vsIndex)

        for (entry in sortable) {
            vsIndex += entry.vs.size
            vs.add(entry.vs)
            vsIndex = consumeUnsortable(vs, unsortable, vsIndex)
        }

        return vs.flatten()
    }

    /**
     * 消费 unsortable 条目 — 对标 consumeUnsortable(vs, unsortable, index)
     */
    private fun consumeUnsortable(
        vs: MutableList<List<String>>,
        unsortable: MutableList<SortEntry>,
        index: Int
    ): Int {
        var idx = index
        while (unsortable.isNotEmpty() && unsortable.last().i <= idx) {
            val last = unsortable.removeLast()
            vs.add(last.vs)
            idx++
        }
        return idx
    }

    // ========================================================================
    // addSubgraphConstraints — 对标 dagre order/add-subgraph-constraints.js
    // ========================================================================

    /**
     * 添加子图约束 — 对标 addSubgraphConstraints(g, cg, vs)
     *
     * 在排序后为约束图添加子图之间的顺序约束。
     */
    private fun addSubgraphConstraints(g: Graph, cg: Graph, vs: List<String>) {
        val prev = mutableMapOf<String?, String>()
        var rootPrev: String? = null

        for (v in vs) {
            var child: String? = g.getParent(v)
            var parent: String?
            var prevChild: String?

            while (child != null) {
                parent = g.getParent(child)
                if (parent != null) {
                    prevChild = prev[parent]
                    prev[parent] = child
                } else {
                    prevChild = rootPrev
                    rootPrev = child
                }

                if (prevChild != null && prevChild != child) {
                    if (!cg.hasNode(prevChild)) cg.setNode(prevChild)
                    if (!cg.hasNode(child)) cg.setNode(child)
                    cg.setEdge(prevChild, child)
                    return
                }

                child = parent
            }
        }
    }

    // ========================================================================
    // crossCount — 对标 dagre order/cross-count.js
    // ========================================================================

    /**
     * 交叉计数 — 对标 crossCount(g, layering)
     *
     * Barth 等人的 "Bilayer Cross Counting" 算法。
     * 使用累积树（accumulator tree）高效计算加权交叉数。
     */
    private fun crossCount(g: Graph, layering: List<List<String>>): Double {
        var cc = 0.0
        for (i in 1 until layering.size) {
            cc += twoLayerCrossCount(g, layering[i - 1], layering[i])
        }
        return cc
    }

    /**
     * 两层交叉计数 — 对标 twoLayerCrossCount(g, northLayer, southLayer)
     */
    private fun twoLayerCrossCount(g: Graph, northLayer: List<String>, southLayer: List<String>): Double {
        // 南层节点的位置映射
        val southPos = mutableMapOf<String, Int>()
        for ((i, v) in southLayer.withIndex()) {
            southPos[v] = i
        }

        // 收集所有从北到南的边，按南层位置排序
        data class SouthEntry(val pos: Int, val weight: Double)

        val southEntries = mutableListOf<SouthEntry>()
        for (v in northLayer) {
            val entries = g.outEdgesOf(v).mapNotNull { e ->
                val pos = southPos[e.target] ?: return@mapNotNull null
                val weight = g.getEdge(e.source, e.target)?.weight ?: 1.0
                SouthEntry(pos, weight)
            }.sortedBy { it.pos }
            southEntries.addAll(entries)
        }

        // 构建累积树
        var firstIndex = 1
        while (firstIndex < southLayer.size) firstIndex = firstIndex shl 1
        val treeSize = 2 * firstIndex - 1
        firstIndex -= 1
        val tree = DoubleArray(treeSize)

        // 计算加权交叉数
        var cc = 0.0
        for (entry in southEntries) {
            var index = entry.pos + firstIndex
            tree[index] += entry.weight
            var weightSum = 0.0
            while (index > 0) {
                if (index % 2 != 0) {
                    weightSum += tree[index + 1]
                }
                index = (index - 1) shr 1
                tree[index] += entry.weight
            }
            cc += entry.weight * weightSum
        }

        return cc
    }

    // ========================================================================
    // 辅助函数
    // ========================================================================

    /**
     * 构建层矩阵 — 对标 dagre util.js buildLayerMatrix
     *
     * 使用 order 属性将节点放入对应位置。
     * 注意：dagre 的 buildLayerMatrix 使用 layering[rank][order] = v 赋值方式，
     * 这意味着如果两个节点有相同的 rank 和 order，后者会覆盖前者。
     */
    private fun buildLayerMatrix(graph: Graph, maxRank: Int): List<List<String>> {
        // 对标 dagre util.js buildLayerMatrix:
        //   layering[rank][node.order] = v
        // 使用 sparse array 方式
        val layering = (0..maxRank).map { mutableMapOf<Int, String>() }

        for (v in graph.nodeIds()) {
            val node = graph.getNode(v) ?: continue
            val rank = node.rank
            if (rank in layering.indices) {
                layering[rank][node.order] = v
            }
        }

        // 转换为紧凑列表（过滤空位）
        return layering.map { orderMap ->
            orderMap.entries.sortedBy { it.key }.map { it.value }
        }
    }

    /**
     * 分配 order — 对标 assignOrder(g, layering)
     */
    private fun assignOrder(graph: Graph, layering: List<List<String>>) {
        for (layer in layering) {
            for ((i, v) in layer.withIndex()) {
                graph.getNode(v)?.order = i
            }
        }
    }
}
