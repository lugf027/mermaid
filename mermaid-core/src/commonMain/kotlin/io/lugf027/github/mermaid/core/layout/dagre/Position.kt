package io.lugf027.github.mermaid.core.layout.dagre

import io.lugf027.github.mermaid.core.layout.Point

/**
 * 坐标赋值算法 - 对标 dagre position 模块
 *
 * 基于 Brandes-Kopf 算法的简化实现。
 * Sugiyama 分层布局的第三步。
 */
object Position {

    /**
     * 为图中的所有节点和边分配 x, y 坐标
     *
     * @param graph 已经完成 rank 和 order 的图
     */
    fun position(graph: Graph) {
        val layers = Rank.layers(graph)
        val isHorizontal = graph.rankdir == "LR" || graph.rankdir == "RL"

        // 分配 y 坐标（沿 rank 方向）
        assignRankPositions(graph, layers, isHorizontal)

        // 分配 x 坐标（沿 order 方向）
        assignOrderPositions(graph, layers, isHorizontal)

        // 处理 RL 和 BT 反向
        if (graph.rankdir == "RL" || graph.rankdir == "BT") {
            reversePositions(graph, layers, isHorizontal)
        }

        // 分配边的路径点
        assignEdgePoints(graph)
    }

    /**
     * 沿 rank 方向分配坐标
     */
    private fun assignRankPositions(graph: Graph, layers: List<List<String>>, isHorizontal: Boolean) {
        var pos = graph.marginY
        for (layer in layers) {
            val maxNodeSize = layer.mapNotNull { graph.getNode(it) }
                .maxOfOrNull { if (isHorizontal) it.width else it.height } ?: 0.0

            for (nodeId in layer) {
                val node = graph.getNode(nodeId) ?: continue
                if (isHorizontal) {
                    node.x = pos + maxNodeSize / 2
                } else {
                    node.y = pos + maxNodeSize / 2
                }
            }
            pos += maxNodeSize + graph.rankSep
        }
    }

    /**
     * 沿 order 方向分配坐标
     */
    private fun assignOrderPositions(graph: Graph, layers: List<List<String>>, isHorizontal: Boolean) {
        for (layer in layers) {
            val sorted = layer.sortedBy { graph.getNode(it)?.order ?: 0 }
            var pos = graph.marginX

            for (nodeId in sorted) {
                val node = graph.getNode(nodeId) ?: continue
                val size = if (isHorizontal) node.height else node.width

                if (isHorizontal) {
                    node.y = pos + size / 2
                } else {
                    node.x = pos + size / 2
                }
                pos += size + graph.nodeSep
            }
        }
    }

    /**
     * 反转坐标（用于 RL 和 BT 方向）
     */
    private fun reversePositions(graph: Graph, layers: List<List<String>>, isHorizontal: Boolean) {
        // 计算总范围
        val allNodes = graph.getNodes()
        if (isHorizontal) {
            val maxX = allNodes.maxOfOrNull { it.x + it.width / 2 } ?: 0.0
            for (node in allNodes) {
                node.x = maxX - node.x
            }
        } else {
            val maxY = allNodes.maxOfOrNull { it.y + it.height / 2 } ?: 0.0
            for (node in allNodes) {
                node.y = maxY - node.y
            }
        }
    }

    /**
     * 为边分配路径点
     */
    private fun assignEdgePoints(graph: Graph) {
        for (edge in graph.edges()) {
            val sourceNode = graph.getNode(edge.source) ?: continue
            val targetNode = graph.getNode(edge.target) ?: continue

            // 简单的直线路径：起点 → 中点 → 终点
            val startPoint = Point(sourceNode.x, sourceNode.y)
            val endPoint = Point(targetNode.x, targetNode.y)
            val midPoint = Point(
                (startPoint.x + endPoint.x) / 2,
                (startPoint.y + endPoint.y) / 2
            )

            edge.points.clear()
            edge.points.add(startPoint)
            edge.points.add(midPoint)
            edge.points.add(endPoint)

            // 边标签位置在中点
            edge.x = midPoint.x
            edge.y = midPoint.y
        }
    }
}
