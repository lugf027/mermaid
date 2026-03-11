package io.lugf027.github.mermaid.core.layout.dagre

import io.lugf027.github.mermaid.core.layout.*
import io.lugf027.github.mermaid.core.util.TextUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Dagre 布局主入口 - 精确对标 dagre-d3-es/src/dagre/layout.js
 *
 * 完整实现 dagre 的 Sugiyama 分层有向图布局流程：
 *
 * 1. makeSpaceForEdgeLabels: ranksep /= 2, minlen *= 2
 * 2. rank: 使用 longest-path 为节点分配层级
 * 3. normalize.run: 将长边拆分为 dummy 节点
 * 4. order: 层内排序最小化边交叉
 * 5. coordinateSystem.adjust: LR/RL 时交换 width/height
 * 6. position: 为所有节点（含 dummy）分配坐标
 * 7. coordinateSystem.undo: 恢复坐标系
 * 8. normalize.undo: 收集 dummy 节点坐标为 edge.points
 * 9. assignNodeIntersects: 在首尾添加矩形交点
 * 10. translateGraph: 平移使坐标非负并加 margin
 * 11. applyLayout: 将结果写回 LayoutData，执行 insertEdge 逻辑
 */
class DagreLayout : LayoutAlgorithm {

    override fun layout(data: LayoutData): LayoutData {
        val graph = buildGraph(data)

        // 如果没有节点，直接返回
        if (graph.nodeCount() == 0) return data

        // ====== 完整的 dagre runLayout 流程 (精确对标 layout.js runLayout) ======

        // 1. makeSpaceForEdgeLabels: ranksep /= 2, minlen *= 2
        makeSpaceForEdgeLabels(graph)

        // 2. removeSelfEdges: 移除自环边（暂存到节点上）
        removeSelfEdges(graph)

        // 3. acyclic.run: 反转反向边使图变成 DAG
        Acyclic.run(graph)

        // 4. rank: 分层
        Rank.longestPath(graph)

        // 5. injectEdgeLabelProxies: 为带标签的边创建临时代理节点
        injectEdgeLabelProxies(graph)

        // 6. normalizeRanks: 确保最小 rank 为 0
        normalizeRanks(graph)

        // 7. removeEdgeLabelProxies: 将代理节点的 rank 记录到边上
        removeEdgeLabelProxies(graph)

        // 8. normalize.run: 将长边拆分为 dummy 节点
        Normalize.run(graph)

        // 9. order: 层内排序（包括 dummy 节点）
        Order.order(graph)

        // 10. coordinateSystem.adjust: LR/RL 时交换 width/height
        adjustCoordinateSystem(graph)

        // 11. position: 为所有节点（含 dummy）分配坐标
        Position.position(graph)

        // 12. normalize.undo: 收集 dummy 节点坐标为 edge.points
        Normalize.undo(graph)

        // 13. fixupEdgeLabelCoords
        fixupEdgeLabelCoords(graph)

        // 14. coordinateSystem.undo: 恢复坐标系
        // dagre 原始流程: coordinateSystem.undo 在 normalize.undo + fixup 之后
        undoCoordinateSystem(graph)

        // 15. translateGraph: 平移使坐标非负并加 margin
        translateGraph(graph)

        // 17. assignNodeIntersects: 在首尾添加矩形交点
        assignNodeIntersects(graph)

        // 18. reversePointsForReversedEdges: 反转被反转边的 points
        Acyclic.reversePointsForReversedEdges(graph)

        // 19. acyclic.undo: 恢复反向边
        Acyclic.undo(graph)

        // 20. 将坐标写回 LayoutData
        return applyLayout(data, graph)
    }

    // ========================================================================
    // dagre 内部步骤
    // ========================================================================

    /**
     * 对标 dagre layout.js makeSpaceForEdgeLabels
     *
     * 通过加倍 minlen 和减半 ranksep 来为边标签腾出空间。
     * 这样每条边至少跨 2 个 rank，中间的 dummy 节点可以放置标签。
     */
    private fun makeSpaceForEdgeLabels(graph: Graph) {
        graph.rankSep /= 2

        for (edge in graph.edges()) {
            edge.minLen *= 2
            if (edge.labelpos.lowercase() != "c") {
                val rankdir = graph.rankdir.uppercase()
                if (rankdir == "TB" || rankdir == "TD" || rankdir == "BT") {
                    edge.width += edge.labeloffset
                } else {
                    edge.height += edge.labeloffset
                }
            }
        }
    }

    /**
     * 对标 dagre layout.js injectEdgeLabelProxies
     *
     * 为带标签的边创建临时代理节点。
     * 这些节点在 rank 分配后用于确定标签的 rank 位置。
     */
    private fun injectEdgeLabelProxies(graph: Graph) {
        for (edge in graph.edges().toList()) {
            if (edge.width > 0 && edge.height > 0) {
                val v = graph.getNode(edge.source) ?: continue
                val w = graph.getNode(edge.target) ?: continue
                val proxyRank = (w.rank - v.rank) / 2 + v.rank
                val proxyId = graph.uniqueId("_ep")
                graph.setNode(proxyId, Graph.NodeData(
                    id = proxyId,
                    width = 0.0,
                    height = 0.0,
                    dummy = "edge-proxy",
                    rank = proxyRank,
                    edgeObj = Graph.EdgeKey(edge.source, edge.target)
                ))
            }
        }
    }

    /**
     * 对标 dagre util.js normalizeRanks
     *
     * 确保最小 rank 为 0。
     */
    private fun normalizeRanks(graph: Graph) {
        val minRank = graph.getNodes().filter { it.rank >= 0 }.minOfOrNull { it.rank } ?: 0
        if (minRank != 0) {
            for (node in graph.getNodes()) {
                if (node.rank >= 0) {
                    node.rank -= minRank
                }
            }
        }
    }

    /**
     * 对标 dagre layout.js removeEdgeLabelProxies
     *
     * 将代理节点的 rank 记录到对应边的 labelRank 属性上，
     * 然后移除代理节点。
     */
    private fun removeEdgeLabelProxies(graph: Graph) {
        val proxies = graph.getNodes().filter { it.dummy == "edge-proxy" }
        for (proxy in proxies) {
            val edgeObj = proxy.edgeObj ?: continue
            val edge = graph.getEdge(edgeObj.v, edgeObj.w) ?: continue
            edge.labelRank = proxy.rank
            graph.removeNode(proxy.id)
        }
    }

    /**
     * 对标 dagre layout.js removeSelfEdges
     *
     * 移除自环边（source == target 的边），因为 dagre 无法处理自环。
     * dagre 原始实现会将自环存储到节点上并在后续恢复，
     * 但为简化实现，这里直接跳过自环边。
     */
    private fun removeSelfEdges(graph: Graph) {
        val selfEdges = graph.edges().filter { it.source == it.target }
        for (edge in selfEdges) {
            graph.removeEdge(edge.source, edge.target)
        }
    }

    /**
     * 对标 dagre coordinate-system.js adjust
     *
     * LR/RL 方向时，交换所有节点和边的 width/height，
     * 使内部始终以 TB 方向处理。
     */
    private fun adjustCoordinateSystem(graph: Graph) {
        val rankdir = graph.rankdir.lowercase()
        if (rankdir == "lr" || rankdir == "rl") {
            for (node in graph.getNodes()) {
                val w = node.width
                node.width = node.height
                node.height = w
            }
            for (edge in graph.edges()) {
                val w = edge.width
                edge.width = edge.height
                edge.height = w
            }
        }
    }

    /**
     * 对标 dagre coordinate-system.js undo
     *
     * 恢复坐标系：
     * - BT/RL: 反转 y
     * - LR/RL: 交换 x/y 和 width/height
     */
    private fun undoCoordinateSystem(graph: Graph) {
        val rankdir = graph.rankdir.lowercase()

        if (rankdir == "bt" || rankdir == "rl") {
            // reverseY
            for (node in graph.getNodes()) {
                node.y = -node.y
            }
            for (edge in graph.edges()) {
                for (i in edge.points.indices) {
                    edge.points[i] = Point(edge.points[i].x, -edge.points[i].y)
                }
                edge.y = -edge.y
            }
        }

        if (rankdir == "lr" || rankdir == "rl") {
            // swapXY for nodes
            for (node in graph.getNodes()) {
                val x = node.x
                node.x = node.y
                node.y = x
            }
            // swapXY for edges
            for (edge in graph.edges()) {
                for (i in edge.points.indices) {
                    edge.points[i] = Point(edge.points[i].y, edge.points[i].x)
                }
                val ex = edge.x
                edge.x = edge.y
                edge.y = ex
            }
            // swapWidthHeight
            for (node in graph.getNodes()) {
                val w = node.width
                node.width = node.height
                node.height = w
            }
            for (edge in graph.edges()) {
                val w = edge.width
                edge.width = edge.height
                edge.height = w
            }
        }
    }

    /**
     * 对标 dagre layout.js fixupEdgeLabelCoords
     */
    private fun fixupEdgeLabelCoords(graph: Graph) {
        for (edge in graph.edges()) {
            if (edge.x != 0.0 || edge.y != 0.0) {
                val lp = edge.labelpos.lowercase()
                if (lp == "l" || lp == "r") {
                    edge.width -= edge.labeloffset
                }
                when (lp) {
                    "l" -> edge.x -= edge.width / 2 + edge.labeloffset
                    "r" -> edge.x += edge.width / 2 + edge.labeloffset
                }
            }
        }
    }

    /**
     * 对标 dagre layout.js assignNodeIntersects
     *
     * dagre 内部使用 intersectRect（所有节点当做矩形）来计算
     * 边的首尾交点。这些交点后续会被 mermaid-js 的 insertEdge 替换。
     */
    private fun assignNodeIntersects(graph: Graph) {
        for (edge in graph.edges()) {
            val nodeV = graph.getNode(edge.source) ?: continue
            val nodeW = graph.getNode(edge.target) ?: continue

            val p1: Point
            val p2: Point

            if (edge.points.isEmpty()) {
                p1 = Point(nodeW.x, nodeW.y)
                p2 = Point(nodeV.x, nodeV.y)
            } else {
                p1 = edge.points.first()
                p2 = edge.points.last()
            }

            // dagre 使用 util.intersectRect（矩形交点）
            edge.points.add(0, intersectRect(nodeV, p1))
            edge.points.add(intersectRect(nodeW, p2))
        }
    }

    /**
     * 矩形交点 — 对标 dagre util.js intersectRect
     *
     * 注意：这是 dagre 内部的矩形交点，与 mermaid-js 的形状 intersect 不同。
     * dagre 对所有节点都用矩形交点，而 mermaid-js 会根据形状使用不同的交点算法。
     */
    private fun intersectRect(rect: Graph.NodeData, point: Point): Point {
        val x = rect.x
        val y = rect.y
        val dx = point.x - x
        val dy = point.y - y
        val w = rect.width / 2
        val h = rect.height / 2

        if (dx == 0.0 && dy == 0.0) {
            // dagre throws error here, but we return center as fallback
            return Point(x, y)
        }

        val sx: Double
        val sy: Double

        if (abs(dy) * w > abs(dx) * h) {
            val hh = if (dy < 0) -h else h
            sx = (hh * dx) / dy
            sy = hh
        } else {
            val ww = if (dx < 0) -w else w
            sx = ww
            sy = (ww * dy) / dx
        }

        return Point(x + sx, y + sy)
    }

    /**
     * 对标 dagre layout.js translateGraph
     *
     * 平移所有坐标使最小值 + margin。
     */
    private fun translateGraph(graph: Graph) {
        var minX = Double.MAX_VALUE
        var maxX = 0.0  // 精确对标 JS: var maxX = 0;
        var minY = Double.MAX_VALUE
        var maxY = 0.0  // 精确对标 JS: var maxY = 0;

        fun getExtremes(x: Double, y: Double, w: Double, h: Double) {
            minX = min(minX, x - w / 2)
            maxX = max(maxX, x + w / 2)
            minY = min(minY, y - h / 2)
            maxY = max(maxY, y + h / 2)
        }

        for (node in graph.getNodes()) {
            getExtremes(node.x, node.y, node.width, node.height)
        }

        for (edge in graph.edges()) {
            if (edge.x != 0.0 || edge.y != 0.0) {
                getExtremes(edge.x, edge.y, edge.width, edge.height)
            }
        }

        minX -= graph.marginX
        minY -= graph.marginY

        for (node in graph.getNodes()) {
            node.x -= minX
            node.y -= minY
        }

        for (edge in graph.edges()) {
            for (i in edge.points.indices) {
                edge.points[i] = Point(edge.points[i].x - minX, edge.points[i].y - minY)
            }
            if (edge.x != 0.0 || edge.y != 0.0) {
                edge.x -= minX
                edge.y -= minY
            }
        }
    }

    // ========================================================================
    // 图构建
    // ========================================================================

    /**
     * 从 LayoutData 构建 Graph
     */
    private fun buildGraph(data: LayoutData): Graph {
        val graph = Graph()

        // 设置图属性
        // dagre 将 TD 视为 TB 的别名
        val direction = if (data.direction.uppercase() == "TD") "TB" else data.direction.uppercase()
        graph.rankdir = direction
        graph.rankSep = data.rankSpacing.toDouble()
        graph.nodeSep = data.nodeSpacing.toDouble()
        graph.marginX = data.diagramPadding.toDouble()
        graph.marginY = data.diagramPadding.toDouble()

        // 添加节点
        for (node in data.nodes) {
            if (node.isGroup) continue // 跳过组节点

            // 估算节点尺寸 - 对齐 mermaid-js 的尺寸计算
            val labelText = node.label ?: node.id
            val labelWidth = TextUtils.estimateDomTextWidth(labelText)
            val labelHeight = 24.0  // mermaid-js 单行文本 foreignObject 高度固定 24px

            // mermaid-js 矩形节点: padding 每边 30，高度 = labelHeight + 30 = 54
            val horizontalPadding = 30.0
            val verticalPadding = 15.0  // 上下各 15

            val width: Double
            val height: Double

            when (node.shape) {
                "diamond" -> {
                    // mermaid-js 菱形: 全对角线 = textWidth + nodeHeight(54)
                    // nodeHeight = labelHeight + verticalPadding * 2 = 24 + 30 = 54
                    val nodeHeight = labelHeight + verticalPadding * 2
                    val fullDiag = labelWidth + nodeHeight
                    width = fullDiag
                    height = fullDiag
                }
                "circle", "doubleCircle" -> {
                    // 圆形: 直径 = max(textWidth, textHeight) + padding * 2
                    val maxDim = maxOf(labelWidth, labelHeight)
                    width = maxDim + horizontalPadding * 2
                    height = width
                }
                else -> {
                    // 矩形类形状（squareRect, roundedRect, stadium 等）
                    width = if (node.width > 0) node.width else (labelWidth + horizontalPadding * 2)
                    height = if (node.height > 0) node.height else (labelHeight + verticalPadding * 2)
                }
            }

            graph.setNode(node.id, Graph.NodeData(
                id = node.id,
                label = node.label,
                width = width,
                height = height,
                shape = node.shape,
                padding = node.padding
            ))
        }

        // 添加边 — 设置标签尺寸供 dagre 使用
        for (edge in data.edges) {
            if (!graph.hasNode(edge.start) || !graph.hasNode(edge.end)) continue

            val labelWidth: Double
            val labelHeight: Double
            if (!edge.label.isNullOrEmpty()) {
                labelWidth = TextUtils.estimateDomTextWidth(edge.label!!, 16.0)
                labelHeight = 24.0
            } else {
                labelWidth = 0.0
                labelHeight = 0.0
            }

            graph.setEdge(edge.start, edge.end, Graph.EdgeData(
                source = edge.start,
                target = edge.end,
                label = edge.label,
                minLen = edge.minLen,
                width = labelWidth,
                height = labelHeight
            ))
        }

        return graph
    }

    // ========================================================================
    // 结果应用
    // ========================================================================

    /**
     * 将 Graph 中的布局结果写回 LayoutData。
     *
     * 精确复刻 mermaid-js 的边点处理流程（insertEdge）：
     *
     * dagre 布局后 edge.points 已包含完整的路由信息：
     *   [startRectIntersect, ...dummyNodePositions..., endRectIntersect]
     *
     * mermaid-js insertEdge() 的逻辑：
     *   - points = points.slice(1, edge.points.length - 1)  // 去掉 dagre 的矩形交点
     *   - points.unshift(tail.intersect(points[0]))  // 用形状特定交点替换首点
     *   - points.push(head.intersect(points[points.length-1]))  // 用形状特定交点替换尾点
     */
    private fun applyLayout(data: LayoutData, graph: Graph): LayoutData {
        val updatedNodes = data.nodes.map { node ->
            val graphNode = graph.getNode(node.id)
            if (graphNode != null) {
                node.copy(
                    x = graphNode.x,
                    y = graphNode.y,
                    width = graphNode.width,
                    height = graphNode.height
                )
            } else node
        }

        val updatedEdges = data.edges.map { edge ->
            val graphEdge = graph.getEdge(edge.start, edge.end)
            val sourceNode = graph.getNode(edge.start)
            val targetNode = graph.getNode(edge.end)

            if (graphEdge != null && sourceNode != null && targetNode != null) {
                // 对标 mermaid-js insertEdge (edges.js line 563-576):
                //   points = points.slice(1, edge.points.length - 1);  // 去掉首尾
                //   points.unshift(tail.intersect(points[0]));
                //   points.push(head.intersect(points[points.length - 1]));
                val dagrePoints = graphEdge.points.toList()

                val finalPoints: MutableList<Point>

                if (dagrePoints.size > 2) {
                    // 去掉 dagre 添加的首尾矩形交点，保留中间 dummy 节点坐标
                    val middlePoints = dagrePoints.subList(1, dagrePoints.size - 1).toMutableList()

                    // 用形状特定的 intersect 替换首尾
                    val startPoint = Position.intersectNode(sourceNode, middlePoints.first())
                    val endPoint = Position.intersectNode(targetNode, middlePoints.last())

                    finalPoints = mutableListOf<Point>()
                    finalPoints.add(startPoint)
                    finalPoints.addAll(middlePoints)
                    finalPoints.add(endPoint)
                } else {
                    // 边界情况：只有 2 个点
                    finalPoints = dagrePoints.toMutableList()
                }

                // 标签位置
                val labelX = graphEdge.x
                val labelY = graphEdge.y

                edge.copy(
                    points = finalPoints,
                    x = labelX,
                    y = labelY
                )
            } else edge
        }

        return data.copy(nodes = updatedNodes, edges = updatedEdges)
    }
}
