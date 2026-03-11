package io.lugf027.github.mermaid.core.layout.dagre

/**
 * 分层排名算法 - 对标 dagre rank 模块
 *
 * 支持两种排名策略：
 * - longestPath: 简单快速但排名不够紧凑（将叶节点推到最深层）
 * - networkSimplex: dagre 默认策略，通过迭代优化最小化边长度总和
 */
object Rank {

    // ====================================================================
    // Network Simplex 算法 - 精确对标 dagre rank/network-simplex.js
    // ====================================================================

    /**
     * Network Simplex 排名算法 - dagre 默认排名策略
     *
     * 步骤：
     * 1. 使用 longestPath 初始化排名
     * 2. 构建 feasible tight tree（紧边生成树）
     * 3. 迭代寻找负 cut value 的边并替换，直到所有 cut value >= 0
     *
     * 前置条件：图必须是 DAG，所有边有 minLen 和 weight
     * 后置条件：所有节点有最优化的 rank 属性，从 0 开始
     */
    fun networkSimplex(graph: Graph) {
        // 1. 简化图（合并多重边）
        val g = simplify(graph)

        // 2. 初始排名
        longestPathInternal(g)

        // 3. 构建 feasible tree
        val t = feasibleTree(g)

        // 4. 初始化 low/lim 值（DFS 序）
        initLowLimValues(t)

        // 5. 初始化 cut values
        initCutValues(t, g)

        // 6. 迭代优化
        var e = leaveEdge(t)
        while (e != null) {
            val f = enterEdge(t, g, e)
            if (f != null) {
                exchangeEdges(t, g, e, f)
            }
            e = leaveEdge(t)
        }

        // 7. 将排名写回原始图（simplify 共享节点引用，rank 已在 g 上更新）
        // 但以防万一，显式同步
        for (nodeId in graph.nodeIds()) {
            val gNode = g.getNode(nodeId)
            val origNode = graph.getNode(nodeId)
            if (gNode != null && origNode != null) {
                origNode.rank = gNode.rank
            }
        }

        // 8. 规范化 rank
        normalizeRanks(graph)
    }

    /**
     * 简化图 — 对标 dagre util.js simplify()
     *
     * 合并多重边：weight 相加，minLen 取最大值
     */
    private fun simplify(g: Graph): Graph {
        val simplified = Graph(isDirected = true, isMultigraph = false, isCompound = false)
        simplified.rankdir = g.rankdir
        simplified.rankSep = g.rankSep
        simplified.nodeSep = g.nodeSep
        simplified.edgeSep = g.edgeSep
        simplified.marginX = g.marginX
        simplified.marginY = g.marginY

        for (node in g.getNodes()) {
            simplified.setNode(node.id, node) // 共享同一个 NodeData 引用
        }
        for (edge in g.edges()) {
            val existing = simplified.getEdge(edge.source, edge.target)
            if (existing != null) {
                existing.weight += edge.weight
                existing.minLen = maxOf(existing.minLen, edge.minLen)
            } else {
                simplified.setEdge(edge.source, edge.target,
                    Graph.EdgeData(
                        source = edge.source,
                        target = edge.target,
                        weight = edge.weight,
                        minLen = edge.minLen
                    )
                )
            }
        }
        return simplified
    }

    // ====================================================================
    // Feasible Tree — 对标 dagre rank/feasible-tree.js
    // ====================================================================

    /**
     * 树中节点/边的附加数据
     */
    private class TreeNodeData(
        val id: String,
        var low: Int = 0,
        var lim: Int = 0,
        var parent: String? = null
    )

    private class TreeEdgeData(
        val v: String,
        val w: String,
        var cutvalue: Double = 0.0
    )

    /**
     * 简单的无向树数据结构 — 对标 JS 中 new Graph({ directed: false })
     */
    private class Tree {
        private val nodes = linkedMapOf<String, TreeNodeData>()
        private val edges = linkedMapOf<String, TreeEdgeData>()
        private val adj = linkedMapOf<String, MutableSet<String>>()

        fun setNode(id: String, data: TreeNodeData = TreeNodeData(id)) {
            nodes[id] = data
            adj.getOrPut(id) { mutableSetOf() }
        }

        fun getNode(id: String): TreeNodeData? = nodes[id]
        fun hasNode(id: String): Boolean = nodes.containsKey(id)
        fun nodeCount(): Int = nodes.size
        fun nodeIds(): Set<String> = nodes.keys.toSet()

        fun setEdge(v: String, w: String, data: TreeEdgeData = TreeEdgeData(v, w)) {
            // 无向图：两个方向都存储
            val key1 = "$v\u0000$w"
            val key2 = "$w\u0000$v"
            edges[key1] = data
            edges[key2] = data
            adj.getOrPut(v) { mutableSetOf() }.add(w)
            adj.getOrPut(w) { mutableSetOf() }.add(v)
        }

        fun getEdge(v: String, w: String): TreeEdgeData? {
            return edges["$v\u0000$w"]
        }

        fun hasEdge(v: String, w: String): Boolean {
            return edges.containsKey("$v\u0000$w")
        }

        fun removeEdge(v: String, w: String) {
            edges.remove("$v\u0000$w")
            edges.remove("$w\u0000$v")
            adj[v]?.remove(w)
            adj[w]?.remove(v)
        }

        fun neighbors(v: String): List<String> = adj[v]?.toList() ?: emptyList()

        /**
         * 返回所有唯一的边（无向图中 v->w 和 w->v 只返回一次）
         */
        fun edgeKeys(): List<Pair<String, String>> {
            val seen = mutableSetOf<String>()
            val result = mutableListOf<Pair<String, String>>()
            for ((key, data) in edges) {
                val canonical = if (data.v <= data.w) "${data.v}\u0000${data.w}" else "${data.w}\u0000${data.v}"
                if (canonical !in seen) {
                    seen.add(canonical)
                    result.add(Pair(data.v, data.w))
                }
            }
            return result
        }
    }

    /**
     * 构建 feasible tight tree — 对标 feasible-tree.js feasibleTree()
     *
     * 紧边：rank(target) - rank(source) == minLen（即 slack == 0）
     */
    private fun feasibleTree(g: Graph): Tree {
        val t = Tree()

        // 选择第一个节点作为起始
        val start = g.nodeIds().firstOrNull() ?: return t
        val size = g.nodeCount()
        t.setNode(start)

        while (tightTree(t, g) < size) {
            val edge = findMinSlackEdge(t, g)
            if (edge != null) {
                val delta = if (t.hasNode(edge.source)) slack(g, edge) else -slack(g, edge)
                shiftRanks(t, g, delta)
            } else {
                break // 不应该发生（图是连通的）
            }
        }

        return t
    }

    /**
     * 构建紧边的最大树 — 对标 feasible-tree.js tightTree()
     */
    private fun tightTree(t: Tree, g: Graph): Int {
        val currentNodes = t.nodeIds().toList()
        for (v in currentNodes) {
            for (edge in g.nodeEdges(v)) {
                val edgeV = edge.source
                val w = if (v == edgeV) edge.target else edgeV
                if (!t.hasNode(w) && slack(g, edge) == 0) {
                    t.setNode(w)
                    t.setEdge(v, w)
                    tightTree(t, g) // 递归继续扩展
                    return t.nodeCount()
                }
            }
        }
        return t.nodeCount()
    }

    /**
     * 查找连接树内外节点的最小 slack 边 — 对标 feasible-tree.js findMinSlackEdge()
     */
    private fun findMinSlackEdge(t: Tree, g: Graph): Graph.EdgeData? {
        var bestEdge: Graph.EdgeData? = null
        var bestSlack = Int.MAX_VALUE
        for (edge in g.edges()) {
            if (t.hasNode(edge.source) != t.hasNode(edge.target)) {
                val s = slack(g, edge)
                if (s < bestSlack) {
                    bestSlack = s
                    bestEdge = edge
                }
            }
        }
        return bestEdge
    }

    /**
     * 移动树中所有节点的 rank — 对标 feasible-tree.js shiftRanks()
     */
    private fun shiftRanks(t: Tree, g: Graph, delta: Int) {
        for (nodeId in t.nodeIds()) {
            val node = g.getNode(nodeId)
            if (node != null) {
                node.rank += delta
            }
        }
    }

    /**
     * 计算边的 slack — 对标 rank/util.js slack()
     * slack = rank(target) - rank(source) - minLen
     */
    private fun slack(g: Graph, edge: Graph.EdgeData): Int {
        val sourceRank = g.getNode(edge.source)?.rank ?: 0
        val targetRank = g.getNode(edge.target)?.rank ?: 0
        return targetRank - sourceRank - edge.minLen
    }

    // ====================================================================
    // Network Simplex 核心 — 对标 network-simplex.js
    // ====================================================================

    /**
     * 初始化 low/lim 值 — 对标 initLowLimValues()
     *
     * 通过 DFS 遍历树，给每个节点分配 low/lim 值和 parent 指针。
     * 这些值用于快速判断两个节点的祖先关系（isDescendant）。
     */
    private fun initLowLimValues(tree: Tree, root: String? = null) {
        val r = root ?: tree.nodeIds().firstOrNull() ?: return
        dfsAssignLowLim(tree, mutableSetOf(), 1, r, null)
    }

    private fun dfsAssignLowLim(tree: Tree, visited: MutableSet<String>, nextLim: Int, v: String, parent: String?): Int {
        var lim = nextLim
        val low = nextLim
        val label = tree.getNode(v) ?: return lim

        visited.add(v)
        for (w in tree.neighbors(v)) {
            if (w !in visited) {
                lim = dfsAssignLowLim(tree, visited, lim, w, v)
            }
        }

        label.low = low
        label.lim = lim++
        label.parent = parent

        return lim
    }

    /**
     * 初始化 cut values — 对标 initCutValues()
     *
     * 对树进行后序遍历，为每条树边计算 cut value。
     */
    private fun initCutValues(t: Tree, g: Graph) {
        val vs = postorder(t, t.nodeIds().toList())
        // 跳过最后一个节点（根节点没有 parent 边）
        for (i in 0 until vs.size - 1) {
            assignCutValue(t, g, vs[i])
        }
    }

    private fun assignCutValue(t: Tree, g: Graph, child: String) {
        val childLab = t.getNode(child) ?: return
        val parent = childLab.parent ?: return
        val treeEdge = t.getEdge(child, parent) ?: return
        treeEdge.cutvalue = calcCutValue(t, g, child)
    }

    /**
     * 计算 cut value — 对标 calcCutValue()
     */
    private fun calcCutValue(t: Tree, g: Graph, child: String): Double {
        val childLab = t.getNode(child) ?: return 0.0
        val parent = childLab.parent ?: return 0.0

        // True if the child is on the tail end of the edge in the directed graph
        var childIsTail = true
        // The graph's view of the tree edge we're inspecting
        var graphEdge = g.getEdge(child, parent)

        if (graphEdge == null) {
            childIsTail = false
            graphEdge = g.getEdge(parent, child) ?: return 0.0
        }

        var cutValue = graphEdge.weight

        for (e in g.nodeEdges(child)) {
            val isOutEdge = e.source == child
            val other = if (isOutEdge) e.target else e.source

            if (other != parent) {
                val pointsToHead = isOutEdge == childIsTail
                val otherWeight = e.weight

                cutValue += if (pointsToHead) otherWeight else -otherWeight
                if (isTreeEdge(t, child, other)) {
                    val otherCutValue = t.getEdge(child, other)?.cutvalue ?: 0.0
                    cutValue += if (pointsToHead) -otherCutValue else otherCutValue
                }
            }
        }

        return cutValue
    }

    /**
     * 查找负 cut value 的离开边 — 对标 leaveEdge()
     */
    private fun leaveEdge(tree: Tree): Pair<String, String>? {
        for ((v, w) in tree.edgeKeys()) {
            val edge = tree.getEdge(v, w) ?: continue
            if (edge.cutvalue < 0) {
                return Pair(v, w)
            }
        }
        return null
    }

    /**
     * 查找进入边 — 对标 enterEdge()
     *
     * 在所有非树边中，找到一条能替换离开边的最优边。
     */
    private fun enterEdge(t: Tree, g: Graph, edge: Pair<String, String>): Pair<String, String>? {
        var v = edge.first
        var w = edge.second

        // 确保 v 是 tail，w 是 head（有向图语义）
        if (!g.hasEdge(v, w)) {
            v = edge.second
            w = edge.first
        }

        val vLabel = t.getNode(v) ?: return null
        val wLabel = t.getNode(w) ?: return null
        val tailLabel: TreeNodeData
        var flip = false

        if (vLabel.lim > wLabel.lim) {
            tailLabel = wLabel
            flip = true
        } else {
            tailLabel = vLabel
        }

        // 在所有图边中寻找候选边
        var bestEdge: Pair<String, String>? = null
        var bestSlack = Int.MAX_VALUE

        for (e in g.edges()) {
            val eVLabel = t.getNode(e.source)
            val eWLabel = t.getNode(e.target)
            if (eVLabel != null && eWLabel != null) {
                val vIsDesc = flip == isDescendant(t, eVLabel, tailLabel)
                val wIsDesc = flip != isDescendant(t, eWLabel, tailLabel)
                if (vIsDesc && wIsDesc) {
                    val s = slack(g, e)
                    if (s < bestSlack) {
                        bestSlack = s
                        bestEdge = Pair(e.source, e.target)
                    }
                }
            }
        }

        return bestEdge
    }

    /**
     * 交换边 — 对标 exchangeEdges()
     */
    private fun exchangeEdges(t: Tree, g: Graph, e: Pair<String, String>, f: Pair<String, String>) {
        t.removeEdge(e.first, e.second)
        t.setEdge(f.first, f.second)
        initLowLimValues(t)
        initCutValues(t, g)
        updateRanks(t, g)
    }

    /**
     * 更新排名 — 对标 updateRanks()
     */
    private fun updateRanks(t: Tree, g: Graph) {
        val root = t.nodeIds().firstOrNull { t.getNode(it)?.parent == null } ?: return
        val vs = preorder(t, listOf(root))
        // 跳过根节点
        for (i in 1 until vs.size) {
            val v = vs[i]
            val nodeData = t.getNode(v) ?: continue
            val parent = nodeData.parent ?: continue

            var edge = g.getEdge(v, parent)
            var flipped = false
            if (edge == null) {
                edge = g.getEdge(parent, v) ?: continue
                flipped = true
            }

            val parentRank = g.getNode(parent)?.rank ?: 0
            g.getNode(v)?.rank = parentRank + if (flipped) edge.minLen else -edge.minLen
        }
    }

    /**
     * 检查是否是树边 — 对标 isTreeEdge()
     */
    private fun isTreeEdge(tree: Tree, u: String, v: String): Boolean {
        return tree.hasEdge(u, v)
    }

    /**
     * 检查 vLabel 是否是 rootLabel 的后代 — 对标 isDescendant()
     */
    private fun isDescendant(tree: Tree, vLabel: TreeNodeData, rootLabel: TreeNodeData): Boolean {
        return rootLabel.low <= vLabel.lim && vLabel.lim <= rootLabel.lim
    }

    // ====================================================================
    // 树遍历 — 对标 graphlib alg/dfs.js
    // ====================================================================

    /**
     * 后序遍历 — 对标 alg.postorder(t, t.nodes())
     */
    private fun postorder(t: Tree, roots: List<String>): List<String> {
        return dfs(t, roots, isPostOrder = true)
    }

    /**
     * 前序遍历 — 对标 alg.preorder(t, root)
     */
    private fun preorder(t: Tree, roots: List<String>): List<String> {
        return dfs(t, roots, isPostOrder = false)
    }

    /**
     * DFS 遍历（前序/后序）— 对标 graphlib alg/dfs.js doDfs()
     *
     * 对无向图（Tree），使用 neighbors 导航。
     */
    private fun dfs(t: Tree, roots: List<String>, isPostOrder: Boolean): List<String> {
        val acc = mutableListOf<String>()
        val visited = mutableSetOf<String>()

        for (root in roots) {
            doDfs(t, root, isPostOrder, visited, acc)
        }

        return acc
    }

    private fun doDfs(t: Tree, v: String, isPostOrder: Boolean, visited: MutableSet<String>, acc: MutableList<String>) {
        if (v in visited) return
        visited.add(v)

        if (!isPostOrder) {
            acc.add(v)
        }

        for (w in t.neighbors(v)) {
            doDfs(t, w, isPostOrder, visited, acc)
        }

        if (isPostOrder) {
            acc.add(v)
        }
    }

    // ====================================================================
    // Longest Path 算法 — 原有实现
    // ====================================================================

    /**
     * longestPath 内部实现 — 对标 JS dagre rank/util.js longestPath()
     *
     * 注意：JS 版本直接在源节点上调 DFS，sink 节点 rank = 0，
     * source 节点 rank 为负数。这是 network simplex 所需的初始排名语义。
     */
    private fun longestPathInternal(graph: Graph) {
        val visited = mutableSetOf<String>()

        fun dfs(v: String): Int {
            if (v in visited) return graph.getNode(v)?.rank ?: 0
            visited.add(v)

            val outEdges = graph.outEdgesOf(v)
            if (outEdges.isEmpty()) {
                graph.getNode(v)?.rank = 0
                return 0
            }

            // JS 用 _.min(_.map(g.outEdges(v), e => dfs(e.w) - g.edge(e).minlen))
            var minRank = Int.MAX_VALUE
            for (edge in outEdges) {
                val targetRank = dfs(edge.target)
                minRank = minOf(minRank, targetRank - edge.minLen)
            }

            if (minRank == Int.MAX_VALUE) minRank = 0
            graph.getNode(v)?.rank = minRank
            return minRank
        }

        for (source in graph.sources()) {
            dfs(source)
        }
    }

    /**
     * 使用最长路径算法分配排名（公开接口，兼容旧调用）
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

    // ====================================================================
    // 工具方法
    // ====================================================================

    /**
     * 规范化排名：使最小 rank 为 0
     */
    private fun normalizeRanks(graph: Graph) {
        val nodes = graph.getNodes()
        if (nodes.isEmpty()) return

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
