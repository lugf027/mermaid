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

        // 3. Position 坐标赋值
        Position.position(graph)

        // 4. 将坐标写回 LayoutData
        return applyLayout(data, graph)
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

            // 估算节点尺寸
            val labelWidth = TextUtils.estimateTextWidth(node.label ?: node.id)
            val labelHeight = TextUtils.estimateTextHeight(node.label ?: node.id)

            val width = if (node.width > 0) node.width else (labelWidth + node.padding * 2).coerceAtLeast(50.0)
            val height = if (node.height > 0) node.height else (labelHeight + node.padding * 2).coerceAtLeast(30.0)

            graph.setNode(node.id, Graph.NodeData(
                id = node.id,
                label = node.label,
                width = width,
                height = height,
                shape = node.shape,
                padding = node.padding
            ))
        }

        // 添加边
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
     * 将 Graph 中的布局结果写回 LayoutData
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
            if (graphEdge != null) {
                edge.copy(
                    points = graphEdge.points.toMutableList(),
                    x = graphEdge.x,
                    y = graphEdge.y
                )
            } else edge
        }

        return data.copy(nodes = updatedNodes, edges = updatedEdges)
    }
}
