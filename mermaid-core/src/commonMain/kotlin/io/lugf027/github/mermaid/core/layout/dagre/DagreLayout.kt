package io.lugf027.github.mermaid.core.layout.dagre

import io.lugf027.github.mermaid.core.layout.*
import io.lugf027.github.mermaid.core.util.TextUtils

/**
 * Dagre 布局主入口 - 对标 mermaid-js layout-algorithms/dagre
 *
 * 实现基于 Sugiyama 的分层有向图布局：
 * 1. Rank：将节点分配到不同层级
 * 2. Order：在同层内排序以最小化边交叉
 * 3. Position：分配具体的 x, y 坐标
 */
class DagreLayout : LayoutAlgorithm {

    override fun layout(data: LayoutData): LayoutData {
        val graph = buildGraph(data)

        // 如果没有节点，直接返回
        if (graph.nodeCount() == 0) return data

        // 1. Rank 分层
        Rank.longestPath(graph)

        // 2. Order 排序
        Order.order(graph)

        // 3. 计算每对相邻 rank 之间的边标签宽度，用于增加间距
        val edgeLabelWidths = computeEdgeLabelWidths(data, graph)

        // 4. Position 坐标赋值（传入边标签宽度信息）
        Position.position(graph, edgeLabelWidths)

        // 5. 将坐标写回 LayoutData
        return applyLayout(data, graph)
    }

    /**
     * 计算每对相邻 rank 之间跨越的最大边标签宽度。
     *
     * mermaid-js 中，带标签的边在相邻 rank 之间增加间距 = 标签文本宽度，
     * 标签位置在间隙中心。
     *
     * @return Map: rank -> 该 rank 到下一 rank 之间的最大边标签宽度
     */
    private fun computeEdgeLabelWidths(data: LayoutData, graph: Graph): Map<Int, Double> {
        val result = mutableMapOf<Int, Double>()
        val isHorizontal = graph.rankdir == "LR" || graph.rankdir == "RL"

        for (edge in data.edges) {
            if (edge.label.isNullOrEmpty()) continue
            val sourceNode = graph.getNode(edge.start) ?: continue
            val targetNode = graph.getNode(edge.end) ?: continue

            // 边标签宽度 = 文本宽度（mermaid-js 在 16px 字体下测量标签宽度）
            val labelWidth = TextUtils.estimateTextWidth(edge.label!!, 16.0)

            // 确定源和目标的 rank（取较小的 rank 作为间隙起始）
            val minRank = minOf(sourceNode.rank, targetNode.rank)

            // 记录该 rank 间隙的最大标签宽度
            val current = result[minRank] ?: 0.0
            if (labelWidth > current) {
                result[minRank] = labelWidth
            }
        }

        return result
    }

    /**
     * 从 LayoutData 构建 Graph
     */
    private fun buildGraph(data: LayoutData): Graph {
        val graph = Graph()

        // 设置图属性
        graph.rankdir = data.direction
        graph.rankSep = data.rankSpacing.toDouble()
        graph.nodeSep = data.nodeSpacing.toDouble()
        graph.marginX = data.diagramPadding.toDouble()
        graph.marginY = data.diagramPadding.toDouble()

        // 添加节点
        for (node in data.nodes) {
            if (node.isGroup) continue // 跳过组节点

            // 估算节点尺寸 - 对齐 mermaid-js 的尺寸计算
            val labelText = node.label ?: node.id
            val labelWidth = TextUtils.estimateTextWidth(labelText)
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

        // 添加边（不再插入虚拟标签节点，标签位置在布局后计算）
        for (edge in data.edges) {
            if (!graph.hasNode(edge.start) || !graph.hasNode(edge.end)) continue

            graph.setEdge(edge.start, edge.end, Graph.EdgeData(
                source = edge.start,
                target = edge.end,
                label = edge.label,
                minLen = edge.minLen
            ))
        }

        return graph
    }

    /**
     * 将 Graph 中的布局结果写回 LayoutData。
     *
     * 对于有标签的边，标签位置 = 源节点边缘和目标节点边缘之间的中点。
     * 边路径从节点边缘出发，经过标签位置，到达目标节点边缘。
     */
    private fun applyLayout(data: LayoutData, graph: Graph): LayoutData {
        val isHorizontal = graph.rankdir == "LR" || graph.rankdir == "RL"

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
                if (!edge.label.isNullOrEmpty()) {
                    // 有标签的边：
                    // 1) 计算标签位置（源节点边缘和目标节点边缘之间的中点）
                    val labelX: Double
                    val labelY: Double
                    if (isHorizontal) {
                        val sourceRight = sourceNode.x + sourceNode.width / 2
                        val targetLeft = targetNode.x - targetNode.width / 2
                        labelX = (sourceRight + targetLeft) / 2
                        labelY = targetNode.y
                    } else {
                        val sourceBottom = sourceNode.y + sourceNode.height / 2
                        val targetTop = targetNode.y - targetNode.height / 2
                        labelX = targetNode.x
                        labelY = (sourceBottom + targetTop) / 2
                    }

                    // 2) 使用 graphEdge.points 中的交点（起点和终点），中间插入标签位置
                    val points = if (graphEdge.points.size >= 2) {
                        mutableListOf(
                            graphEdge.points.first(),
                            Point(labelX, labelY),
                            graphEdge.points.last()
                        )
                    } else {
                        mutableListOf(
                            Point(sourceNode.x, sourceNode.y),
                            Point(labelX, labelY),
                            Point(targetNode.x, targetNode.y)
                        )
                    }

                    edge.copy(
                        points = points,
                        x = labelX,
                        y = labelY
                    )
                } else {
                    // 无标签的边：直接使用 Position.assignEdgePoints 计算的路径
                    edge.copy(
                        points = graphEdge.points.toMutableList(),
                        x = graphEdge.x,
                        y = graphEdge.y
                    )
                }
            } else edge
        }

        return data.copy(nodes = updatedNodes, edges = updatedEdges)
    }
}
