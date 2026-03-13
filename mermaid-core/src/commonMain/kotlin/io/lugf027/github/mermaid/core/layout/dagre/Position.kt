package io.lugf027.github.mermaid.core.layout.dagre

import io.lugf027.github.mermaid.core.layout.Point
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 坐标赋值算法 - 精确对标 dagre position 模块
 *
 * 完整实现 Brandes-Köpf 快速水平坐标分配算法：
 * - 4 方向垂直对齐（上左、上右、下左、下右）
 * - 水平压缩（block graph 构建 + 两遍扫描）
 * - 对齐最小宽度方案
 * - 取中位数平衡
 *
 * 精确复刻 dagre-d3-es/src/dagre/position/bk.js 和 position/index.js
 */
object Position {

    /**
     * 为图中的所有节点分配 x, y 坐标 — 对标 dagre position/index.js
     *
     * 注意：dagre 内部始终以 TB 方向处理（LR/RL 通过 coordinateSystem 转换），
     * 所以这里的 y 是 rank 方向，x 是 order 方向。
     *
     * @param graph 已经完成 rank 和 order 的图（包含 dummy 节点）
     */
    fun position(graph: Graph) {
        val layers = buildLayerMatrix(graph)

        // 分配 y 坐标（沿 rank 方向）— 对标 positionY
        positionY(graph, layers)

        // 分配 x 坐标（沿 order 方向）— 完整 Brandes-Köpf positionX
        val xs = positionX(graph, layers)
        for ((nodeId, x) in xs) {
            val node = graph.getNode(nodeId) ?: continue
            node.x = x
        }
    }

    /**
     * 沿 rank 方向分配 y 坐标 — 精确对标 dagre position/index.js positionY
     */
    private fun positionY(graph: Graph, layers: List<List<String>>) {
        val rankSep = graph.rankSep
        var prevY = 0.0

        for (layer in layers) {
            val maxHeight = layer.mapNotNull { graph.getNode(it) }
                .maxOfOrNull { it.height } ?: 0.0

            for (nodeId in layer) {
                val node = graph.getNode(nodeId) ?: continue
                node.y = prevY + maxHeight / 2
            }

            prevY += maxHeight + rankSep
        }
    }

    // ========================================================================
    // Brandes-Köpf positionX — 精确对标 dagre position/bk.js
    // ========================================================================

    /**
     * 主入口 — 对标 bk.js positionX(g)
     *
     * 执行 4 种对齐方式（ul, ur, dl, dr），找到最小宽度对齐并取平衡值。
     *
     * @return Map<nodeId, x坐标>
     */
    private fun positionX(graph: Graph, layers: List<List<String>>): Map<String, Double> {
        val conflicts = mutableMapOf<String, MutableSet<String>>()
        findType1Conflicts(graph, layers, conflicts)
        findType2Conflicts(graph, layers, conflicts)

        val xss = mutableMapOf<String, MutableMap<String, Double>>()

        for (vert in listOf("u", "d")) {
            var adjustedLayering: List<List<String>> = if (vert == "u") {
                layers
            } else {
                layers.reversed()
            }

            for (horiz in listOf("l", "r")) {
                if (horiz == "r") {
                    adjustedLayering = adjustedLayering.map { it.reversed() }
                }

                val neighborFn: (String) -> List<String> = if (vert == "u") {
                    { v -> graph.predecessors(v) }
                } else {
                    { v -> graph.successors(v) }
                }

                val alignment = verticalAlignment(graph, adjustedLayering, conflicts, neighborFn)
                var xs = horizontalCompaction(
                    graph, adjustedLayering, alignment.first, alignment.second, horiz == "r"
                )

                if (horiz == "r") {
                    xs = xs.mapValues { (_, x) -> -x }.toMutableMap()
                }

                xss[vert + horiz] = xs
            }
        }

        val smallestWidth = findSmallestWidthAlignment(graph, xss)
        alignCoordinates(xss, smallestWidth)
        return balance(xss)
    }

    /**
     * Type-1 冲突检测 — 对标 bk.js findType1Conflicts
     *
     * Type-1 冲突：非内部线段与内部线段交叉。
     * 内部线段是两端都是 dummy 节点的边。
     */
    private fun findType1Conflicts(
        graph: Graph,
        layering: List<List<String>>,
        conflicts: MutableMap<String, MutableSet<String>>
    ) {
        if (layering.size < 2) return

        for (layerIdx in 1 until layering.size) {
            val prevLayer = layering[layerIdx - 1]
            val layer = layering[layerIdx]
            var k0 = 0
            var scanPos = 0
            val prevLayerLength = prevLayer.size
            val lastNode = layer.lastOrNull()

            for ((i, v) in layer.withIndex()) {
                val w = findOtherInnerSegmentNode(graph, v)
                val k1 = if (w != null) graph.getNode(w)?.order ?: prevLayerLength else prevLayerLength

                if (w != null || v == lastNode) {
                    for (scanIdx in scanPos..i) {
                        val scanNode = layer[scanIdx]
                        for (u in graph.predecessors(scanNode)) {
                            val uLabel = graph.getNode(u) ?: continue
                            val uPos = uLabel.order
                            if ((uPos < k0 || k1 < uPos) &&
                                !(uLabel.dummy != null && graph.getNode(scanNode)?.dummy != null)
                            ) {
                                addConflict(conflicts, u, scanNode)
                            }
                        }
                    }
                    scanPos = i + 1
                    k0 = k1
                }
            }
        }
    }

    /**
     * Type-2 冲突检测 — 对标 bk.js findType2Conflicts
     */
    private fun findType2Conflicts(
        graph: Graph,
        layering: List<List<String>>,
        conflicts: MutableMap<String, MutableSet<String>>
    ) {
        if (layering.size < 2) return

        for (layerIdx in 1 until layering.size) {
            val north = layering[layerIdx - 1]
            val south = layering[layerIdx]
            var prevNorthPos = -1
            var nextNorthPos = -1
            var southPos = 0

            for ((southLookahead, v) in south.withIndex()) {
                if (graph.getNode(v)?.dummy == "border") {
                    val predecessors = graph.predecessors(v)
                    if (predecessors.isNotEmpty()) {
                        nextNorthPos = graph.getNode(predecessors[0])?.order ?: -1
                        scanType2(graph, south, southPos, southLookahead, prevNorthPos, nextNorthPos, conflicts)
                        southPos = southLookahead
                        prevNorthPos = nextNorthPos
                    }
                }
                scanType2(graph, south, southPos, south.size, nextNorthPos, north.size, conflicts)
            }
        }
    }

    private fun scanType2(
        graph: Graph,
        south: List<String>,
        southPos: Int,
        southEnd: Int,
        prevNorthBorder: Int,
        nextNorthBorder: Int,
        conflicts: MutableMap<String, MutableSet<String>>
    ) {
        for (i in southPos until southEnd) {
            val v = south[i]
            if (graph.getNode(v)?.dummy != null) {
                for (u in graph.predecessors(v)) {
                    val uNode = graph.getNode(u) ?: continue
                    if (uNode.dummy != null &&
                        (uNode.order < prevNorthBorder || uNode.order > nextNorthBorder)
                    ) {
                        addConflict(conflicts, u, v)
                    }
                }
            }
        }
    }

    /**
     * 查找内部线段的另一端 — 对标 bk.js findOtherInnerSegmentNode
     */
    private fun findOtherInnerSegmentNode(graph: Graph, v: String): String? {
        if (graph.getNode(v)?.dummy != null) {
            return graph.predecessors(v).firstOrNull { graph.getNode(it)?.dummy != null }
        }
        return null
    }

    /**
     * 添加冲突记录 — 对标 bk.js addConflict
     */
    private fun addConflict(conflicts: MutableMap<String, MutableSet<String>>, v: String, w: String) {
        var a = v
        var b = w
        if (a > b) { val tmp = a; a = b; b = tmp }
        conflicts.getOrPut(a) { mutableSetOf() }.add(b)
    }

    /**
     * 检查是否有冲突 — 对标 bk.js hasConflict
     */
    private fun hasConflict(conflicts: Map<String, Set<String>>, v: String, w: String): Boolean {
        var a = v
        var b = w
        if (a > b) { val tmp = a; a = b; b = tmp }
        return conflicts[a]?.contains(b) == true
    }

    /**
     * 垂直对齐 — 对标 bk.js verticalAlignment
     *
     * 尝试将节点对齐到其中位邻居形成垂直"块"。
     *
     * @return Pair(root, align)
     */
    private fun verticalAlignment(
        graph: Graph,
        layering: List<List<String>>,
        conflicts: Map<String, Set<String>>,
        neighborFn: (String) -> List<String>
    ): Pair<MutableMap<String, String>, MutableMap<String, String>> {
        val root = mutableMapOf<String, String>()
        val align = mutableMapOf<String, String>()
        val pos = mutableMapOf<String, Int>()

        // 缓存位置信息
        for (layer in layering) {
            for ((order, v) in layer.withIndex()) {
                root[v] = v
                align[v] = v
                pos[v] = order
            }
        }

        for (layer in layering) {
            var prevIdx = -1
            for (v in layer) {
                var ws = neighborFn(v)
                if (ws.isNotEmpty()) {
                    ws = ws.sortedBy { pos[it] ?: 0 }
                    val mp = (ws.size - 1).toDouble() / 2.0
                    val iStart = floor(mp).toInt()
                    val iEnd = ceil(mp).toInt()
                    for (i in iStart..iEnd) {
                        val w = ws[i]
                        if (align[v] == v && prevIdx < (pos[w] ?: 0) &&
                            !hasConflict(conflicts, v, w)
                        ) {
                            align[w] = v
                            align[v] = root[w] ?: w
                            root[v] = root[w] ?: w
                            prevIdx = pos[w] ?: 0
                        }
                    }
                }
            }
        }

        return Pair(root, align)
    }

    /**
     * 水平压缩 — 对标 bk.js horizontalCompaction
     *
     * 构建 block graph，两遍扫描分配坐标。
     */
    private fun horizontalCompaction(
        graph: Graph,
        layering: List<List<String>>,
        root: Map<String, String>,
        align: Map<String, String>,
        reverseSep: Boolean
    ): MutableMap<String, Double> {
        val xs = mutableMapOf<String, Double>()

        // 构建 block graph
        val blockGraph = buildBlockGraph(graph, layering, root, reverseSep)
        val borderType = if (reverseSep) "borderLeft" else "borderRight"

        // 迭代函数 — 对标 bk.js iterate
        fun iterate(
            setXsFunc: (String) -> Unit,
            nextNodesFunc: (String) -> List<String>
        ) {
            val stack = blockGraph.nodeIds().toMutableList()
            val visited = mutableSetOf<String>()
            var elem = stack.removeLastOrNull()
            while (elem != null) {
                if (elem in visited) {
                    setXsFunc(elem)
                } else {
                    visited.add(elem)
                    stack.add(elem)
                    stack.addAll(nextNodesFunc(elem))
                }
                elem = stack.removeLastOrNull()
            }
        }

        // 第一遍：分配最小坐标
        iterate(
            setXsFunc = { elem ->
                val inEdges = blockGraph.inEdgesOf(elem)
                xs[elem] = inEdges.fold(0.0) { acc, e ->
                    val edgeWeight = blockGraph.getEdge(e.source, e.target)?.weight ?: 0.0
                    max(acc, (xs[e.source] ?: 0.0) + edgeWeight)
                }
            },
            nextNodesFunc = { elem -> blockGraph.predecessors(elem) }
        )

        // 第二遍：分配最大坐标
        iterate(
            setXsFunc = { elem ->
                val outEdges = blockGraph.outEdgesOf(elem)
                var minVal = Double.POSITIVE_INFINITY
                for (e in outEdges) {
                    val edgeWeight = blockGraph.getEdge(e.source, e.target)?.weight ?: 0.0
                    minVal = min(minVal, (xs[e.target] ?: 0.0) - edgeWeight)
                }
                val node = graph.getNode(elem)
                if (minVal != Double.POSITIVE_INFINITY && node?.extra?.get("borderType") != borderType) {
                    xs[elem] = max(xs[elem] ?: 0.0, minVal)
                }
            },
            nextNodesFunc = { elem -> blockGraph.successors(elem) }
        )

        // 将所有节点的坐标设为其 root 的坐标
        for ((v, _) in align) {
            xs[v] = xs[root[v] ?: v] ?: 0.0
        }

        return xs
    }

    /**
     * 构建 block graph — 对标 bk.js buildBlockGraph
     */
    private fun buildBlockGraph(
        graph: Graph,
        layering: List<List<String>>,
        root: Map<String, String>,
        reverseSep: Boolean
    ): Graph {
        val blockGraph = Graph(isDirected = true, isMultigraph = false, isCompound = false)

        for (layer in layering) {
            var u: String? = null
            for (v in layer) {
                val vRoot = root[v] ?: v
                blockGraph.setNode(vRoot)
                if (u != null) {
                    val uRoot = root[u] ?: u
                    val prevEdge = blockGraph.getEdge(uRoot, vRoot)
                    val prevMax = prevEdge?.weight ?: 0.0
                    val sepVal = sep(graph, v, u, reverseSep)
                    blockGraph.setEdge(uRoot, vRoot, Graph.EdgeData(
                        source = uRoot,
                        target = vRoot,
                        weight = max(sepVal, prevMax)
                    ))
                }
                u = v
            }
        }

        return blockGraph
    }

    /**
     * sep 函数 — 精确对标 bk.js sep(nodeSep, edgeSep, reverseSep)
     *
     * 计算两个相邻节点之间所需的最小间距。
     */
    private fun sep(graph: Graph, v: String, w: String, reverseSep: Boolean): Double {
        val nodeSep = graph.nodeSep
        val edgeSep = graph.edgeSep
        val vLabel = graph.getNode(v) ?: return nodeSep
        val wLabel = graph.getNode(w) ?: return nodeSep

        var sum = 0.0
        var delta = 0.0

        sum += vLabel.width / 2
        val vLabelpos = vLabel.labelpos
        if (vLabelpos != null) {
            when (vLabelpos.lowercase()) {
                "l" -> delta = -vLabel.width / 2
                "r" -> delta = vLabel.width / 2
            }
        }
        if (delta != 0.0) {
            sum += if (reverseSep) delta else -delta
        }
        delta = 0.0

        sum += (if (vLabel.dummy != null) edgeSep else nodeSep) / 2
        sum += (if (wLabel.dummy != null) edgeSep else nodeSep) / 2

        sum += wLabel.width / 2
        val wLabelpos = wLabel.labelpos
        if (wLabelpos != null) {
            when (wLabelpos.lowercase()) {
                "l" -> delta = wLabel.width / 2
                "r" -> delta = -wLabel.width / 2
            }
        }
        if (delta != 0.0) {
            sum += if (reverseSep) delta else -delta
        }

        return sum
    }

    /**
     * 找到最小宽度的对齐方案 — 对标 bk.js findSmallestWidthAlignment
     */
    private fun findSmallestWidthAlignment(
        graph: Graph,
        xss: Map<String, Map<String, Double>>
    ): Map<String, Double> {
        var bestXs: Map<String, Double>? = null
        var bestWidth = Double.POSITIVE_INFINITY

        for ((_, xs) in xss) {
            var maxVal = Double.NEGATIVE_INFINITY
            var minVal = Double.POSITIVE_INFINITY

            for ((v, x) in xs) {
                val halfWidth = (graph.getNode(v)?.width ?: 0.0) / 2
                maxVal = max(x + halfWidth, maxVal)
                minVal = min(x - halfWidth, minVal)
            }

            val width = maxVal - minVal
            if (width < bestWidth) {
                bestWidth = width
                bestXs = xs
            }
        }

        return bestXs ?: emptyMap()
    }

    /**
     * 对齐各方案坐标 — 对标 bk.js alignCoordinates
     */
    private fun alignCoordinates(
        xss: MutableMap<String, MutableMap<String, Double>>,
        alignTo: Map<String, Double>
    ) {
        val alignToVals = alignTo.values
        if (alignToVals.isEmpty()) return
        val alignToMin = alignToVals.min()
        val alignToMax = alignToVals.max()

        for (vert in listOf("u", "d")) {
            for (horiz in listOf("l", "r")) {
                val alignment = vert + horiz
                val xs = xss[alignment] ?: continue
                if (xs === alignTo) continue

                val xsVals = xs.values
                if (xsVals.isEmpty()) continue

                val delta = if (horiz == "l") {
                    alignToMin - xsVals.min()
                } else {
                    alignToMax - xsVals.max()
                }

                if (delta != 0.0) {
                    xss[alignment] = xs.mapValues { (_, x) -> x + delta }.toMutableMap()
                }
            }
        }
    }

    /**
     * 取平衡值 — 对标 bk.js balance
     *
     * 对每个节点，取 4 种对齐中间两个值的平均值。
     */
    private fun balance(xss: Map<String, Map<String, Double>>): Map<String, Double> {
        val ul = xss["ul"] ?: return emptyMap()
        return ul.mapValues { (v, _) ->
            val values = listOf(
                xss["ul"]?.get(v) ?: 0.0,
                xss["ur"]?.get(v) ?: 0.0,
                xss["dl"]?.get(v) ?: 0.0,
                xss["dr"]?.get(v) ?: 0.0
            ).sorted()
            (values[1] + values[2]) / 2
        }
    }

    /**
     * 构建层矩阵 — 对标 dagre util.js buildLayerMatrix
     *
     * 按 rank 分组，每组按 order 排列
     */
    private fun buildLayerMatrix(graph: Graph): List<List<String>> {
        val maxRank = graph.getNodes().maxOfOrNull { it.rank } ?: 0
        val layers = (0..maxRank).map { rank ->
            graph.getNodes()
                .filter { it.rank == rank }
                .sortedBy { it.order }
                .map { it.id }
        }
        return layers
    }

    // ========================================================================
    // 交点计算算法 — 精确复刻 mermaid-js intersect 模块
    // ========================================================================

    /**
     * 根据节点形状分派交点计算 — 对标 mermaid-js node.intersect(point)
     *
     * 在 mermaid-js 中，每个形状渲染时在 node 对象上绑定 intersect 方法：
     * - rect/squareRect/roundedRect: intersect.rect(node, point)
     * - diamond/question: intersect.polygon(node, diamondPoints, point)，结果 -0.5
     * - circle: intersect.circle(node, radius, point)
     */
    fun intersectNode(node: Graph.NodeData, point: Point): Point {
        return when (node.shape) {
            "diamond" -> intersectDiamond(node, point)
            "circle", "doubleCircle" -> intersectCircle(node, point)
            else -> intersectRect(node, point)
        }
    }

    /**
     * 矩形交点 — 精确复刻 intersect-rect.js
     *
     * 算法来源: https://math.stackexchange.com/questions/108113/find-edge-between-two-boxes
     */
    fun intersectRect(node: Graph.NodeData, point: Point): Point {
        val x = node.x
        val y = node.y
        val dx = point.x - x
        val dy = point.y - y
        val w = node.width / 2
        val h = node.height / 2

        val sx: Double
        val sy: Double

        if (abs(dy) * w > abs(dx) * h) {
            // Intersection is top or bottom of rect
            val hh = if (dy < 0) -h else h
            sx = if (dy == 0.0) 0.0 else (hh * dx) / dy
            sy = hh
        } else {
            // Intersection is left or right of rect
            val ww = if (dx < 0) -w else w
            sx = ww
            sy = if (dx == 0.0) 0.0 else (ww * dy) / dx
        }

        return Point(x + sx, y + sy)
    }

    /**
     * 菱形交点 — 精确复刻 mermaid-js question.ts 的 calcIntersect
     *
     * question.ts 中:
     *   const s = bounds.width;  // diamond 的 width == height == s
     *   const points = [
     *     { x: s/2, y: 0 },       // top
     *     { x: s,   y: -s/2 },    // right
     *     { x: s/2, y: -s },      // bottom
     *     { x: 0,   y: -s/2 },    // left
     *   ];
     *   const res = intersect.polygon(bounds, points, point);
     *   return { x: res.x - 0.5, y: res.y - 0.5 };  // 微调 0.5px
     */
    fun intersectDiamond(node: Graph.NodeData, point: Point): Point {
        val s = node.width  // diamond: width == height == s
        val polyPoints = listOf(
            Point(s / 2, 0.0),       // top
            Point(s, -s / 2),        // right
            Point(s / 2, -s),        // bottom
            Point(0.0, -s / 2)       // left
        )
        val res = intersectPolygon(node, polyPoints, point)
        return Point(res.x - 0.5, res.y - 0.5)
    }

    /**
     * 多边形交点 — 精确复刻 intersect-polygon.js
     *
     * 遍历多边形每条边，用 intersectLine 测试射线与每条边的交点，
     * 取距离 point 最近的交点。
     */
    fun intersectPolygon(node: Graph.NodeData, polyPoints: List<Point>, point: Point): Point {
        val x1 = node.x
        val y1 = node.y

        val intersections = mutableListOf<Point>()

        // 计算 polyPoints 的最小 x/y
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        for (p in polyPoints) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
        }

        // 计算偏移：将 polyPoints 相对于节点中心定位
        val left = x1 - node.width / 2 - minX
        val top = y1 - node.height / 2 - minY

        for (i in polyPoints.indices) {
            val p1 = polyPoints[i]
            val p2 = polyPoints[if (i < polyPoints.size - 1) i + 1 else 0]

            val intersect = intersectLine(
                // p1, p2 参数：从 node 中心到 point 的射线
                Point(x1, y1), point,
                // q1, q2 参数：多边形的一条边
                Point(left + p1.x, top + p1.y),
                Point(left + p2.x, top + p2.y)
            )
            if (intersect != null) {
                intersections.add(intersect)
            }
        }

        if (intersections.isEmpty()) {
            return Point(node.x, node.y)
        }

        if (intersections.size > 1) {
            // 多个交点，取距离 point 最近的
            intersections.sortBy { p ->
                val pdx = p.x - point.x
                val pdy = p.y - point.y
                sqrt(pdx * pdx + pdy * pdy)
            }
        }

        return intersections[0]
    }

    /**
     * 两条线段交点 — 精确复刻 intersect-line.js
     *
     * Algorithm from J. Avro, (ed.) Graphics Gems, No 2, Morgan Kaufmann, 1994, p7 and p473.
     *
     * 关键特性：包含 rounding offset (denom/2)，这会导致与纯数学公式有微小差异。
     *
     * @param p1, p2 第一条线段（从节点中心到 point 的射线）
     * @param q1, q2 第二条线段（多边形的一条边）
     * @return 交点，如果不相交返回 null
     */
    fun intersectLine(p1: Point, p2: Point, q1: Point, q2: Point): Point? {
        // Compute a1, b1, c1, where line joining p1 and p2 is F(x,y) = a1*x + b1*y + c1 = 0
        val a1 = p2.y - p1.y
        val b1 = p1.x - p2.x
        val c1 = p2.x * p1.y - p1.x * p2.y

        // Compute r3 and r4
        val r3 = a1 * q1.x + b1 * q1.y + c1
        val r4 = a1 * q2.x + b1 * q2.y + c1

        // Check signs of r3 and r4. If both lie on same side of line 1,
        // the line segments do not intersect.
        if (r3 != 0.0 && r4 != 0.0 && sameSign(r3, r4)) {
            return null // DON'T_INTERSECT
        }

        // Compute a2, b2, c2 where line joining q1 and q2 is G(x,y) = a2*x + b2*y + c2 = 0
        val a2 = q2.y - q1.y
        val b2 = q1.x - q2.x
        val c2 = q2.x * q1.y - q1.x * q2.y

        // Compute r1 and r2
        val r1 = a2 * p1.x + b2 * p1.y + c2
        val r2 = a2 * p2.x + b2 * p2.y + c2

        val epsilon = 1e-6

        // Check signs of r1 and r2
        if (abs(r1) < epsilon && abs(r2) < epsilon && sameSign(r1, r2)) {
            return null // DON'T_INTERSECT
        }

        // Line segments intersect: compute intersection point
        val denom = a1 * b2 - a2 * b1
        if (denom == 0.0) {
            return null // COLLINEAR
        }

        // The denom/2 is to get rounding instead of truncating.
        // It is added or subtracted to the numerator, depending upon the
        // sign of the numerator.
        val offset = abs(denom / 2)

        var num = b1 * c2 - b2 * c1
        val x = if (num < 0) (num - offset) / denom else (num + offset) / denom

        num = a2 * c1 - a1 * c2
        val y = if (num < 0) (num - offset) / denom else (num + offset) / denom

        return Point(x, y)
    }

    private fun sameSign(r1: Double, r2: Double): Boolean {
        return r1 * r2 > 0
    }

    /**
     * 圆形交点 — 精确复刻 intersect-circle.js → intersect-ellipse.js
     */
    fun intersectCircle(node: Graph.NodeData, point: Point): Point {
        val r = maxOf(node.width, node.height) / 2
        val dx = point.x - node.x
        val dy = point.y - node.y
        val dist = sqrt(dx * dx + dy * dy)

        if (dist == 0.0) return Point(node.x + r, node.y)

        return Point(
            node.x + dx * r / dist,
            node.y + dy * r / dist
        )
    }
}
