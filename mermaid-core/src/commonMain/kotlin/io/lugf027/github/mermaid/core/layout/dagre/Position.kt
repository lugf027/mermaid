package io.lugf027.github.mermaid.core.layout.dagre

import io.lugf027.github.mermaid.core.layout.Point
import kotlin.math.abs

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
     * @param edgeLabelWidths 每对相邻 rank 之间的最大边标签宽度，用于增加间距
     */
    fun position(graph: Graph, edgeLabelWidths: Map<Int, Double> = emptyMap()) {
        val layers = Rank.layers(graph)
        val isHorizontal = graph.rankdir == "LR" || graph.rankdir == "RL"

        // 分配 y 坐标（沿 rank 方向），考虑边标签宽度
        assignRankPositions(graph, layers, isHorizontal, edgeLabelWidths)

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
     * 沿 rank 方向分配坐标。
     *
     * 在相邻 rank 之间，如果有边标签跨越该间隙，额外增加标签宽度的间距。
     * 这样节点之间的间隙 = rankSep + edgeLabelWidth（如果有标签的边跨越该间隙）。
     */
    private fun assignRankPositions(
        graph: Graph,
        layers: List<List<String>>,
        isHorizontal: Boolean,
        edgeLabelWidths: Map<Int, Double>
    ) {
        var pos = graph.marginY
        for ((layerIdx, layer) in layers.withIndex()) {
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

            // 计算到下一层的间距：rankSep + 该 rank 间隙的边标签宽度
            val labelWidth = edgeLabelWidths[layerIdx] ?: 0.0
            pos += maxNodeSize + graph.rankSep + labelWidth
        }
    }

    /**
     * 沿 order 方向分配坐标 - 每层节点居中对齐
     */
    private fun assignOrderPositions(graph: Graph, layers: List<List<String>>, isHorizontal: Boolean) {
        // 先计算每层的总尺寸，找出最大层尺寸
        var maxLayerSize = 0.0
        val layerSizes = mutableListOf<Double>()

        for (layer in layers) {
            val sorted = layer.sortedBy { graph.getNode(it)?.order ?: 0 }
            var totalSize = 0.0
            for ((idx, nodeId) in sorted.withIndex()) {
                val node = graph.getNode(nodeId) ?: continue
                val size = if (isHorizontal) node.height else node.width
                totalSize += size
                if (idx > 0) totalSize += graph.nodeSep
            }
            layerSizes.add(totalSize)
            maxLayerSize = maxOf(maxLayerSize, totalSize)
        }

        // 分配坐标，每层居中于最大层尺寸
        for ((layerIdx, layer) in layers.withIndex()) {
            val sorted = layer.sortedBy { graph.getNode(it)?.order ?: 0 }
            val layerSize = layerSizes[layerIdx]
            // 居中偏移
            val offset = graph.marginX + (maxLayerSize - layerSize) / 2
            var pos = offset

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
     * 为边分配路径点 - 路径从节点边缘出发
     */
    private fun assignEdgePoints(graph: Graph) {
        for (edge in graph.edges()) {
            val sourceNode = graph.getNode(edge.source) ?: continue
            val targetNode = graph.getNode(edge.target) ?: continue

            // 计算从 source 边缘到 target 边缘的路径
            val sourceCenter = Point(sourceNode.x, sourceNode.y)
            val targetCenter = Point(targetNode.x, targetNode.y)

            val startPoint = intersectNode(sourceNode, targetCenter)
            val endPoint = intersectNode(targetNode, sourceCenter)
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

    /**
     * 计算从外部点到节点边缘的交点
     *
     * @param node 目标节点
     * @param point 外部参考点（连线方向）
     * @return 节点边缘上的交点
     */
    private fun intersectNode(node: Graph.NodeData, point: Point): Point {
        return when (node.shape) {
            "diamond" -> intersectDiamond(node, point)
            "circle", "doubleCircle" -> intersectCircle(node, point)
            else -> intersectRect(node, point)
        }
    }

    /**
     * 矩形边缘交点计算
     */
    private fun intersectRect(node: Graph.NodeData, point: Point): Point {
        val w = node.width / 2
        val h = node.height / 2
        val dx = point.x - node.x
        val dy = point.y - node.y

        if (dx == 0.0 && dy == 0.0) {
            return Point(node.x, node.y)
        }

        // 计算射线与矩形边的交点
        val sx: Double
        val sy: Double

        if (abs(dy) * w > abs(dx) * h) {
            // 交于上边或下边
            sy = if (dy > 0) h else -h
            sx = if (dy != 0.0) sy * dx / dy else 0.0
        } else {
            // 交于左边或右边
            sx = if (dx > 0) w else -w
            sy = if (dx != 0.0) sx * dy / dx else 0.0
        }

        return Point(node.x + sx, node.y + sy)
    }

    /**
     * 菱形边缘交点计算
     */
    private fun intersectDiamond(node: Graph.NodeData, point: Point): Point {
        val w = node.width / 2
        val h = node.height / 2
        val dx = point.x - node.x
        val dy = point.y - node.y

        if (dx == 0.0 && dy == 0.0) {
            return Point(node.x, node.y)
        }

        // 菱形由四条线段组成，计算与射线的交点
        // 菱形顶点: top(0,-h), right(w,0), bottom(0,h), left(-w,0)
        val absDx = abs(dx)
        val absDy = abs(dy)

        // 菱形边的方程: |x/w| + |y/h| = 1
        // 射线参数: x = t*dx, y = t*dy
        // |t*dx/w| + |t*dy/h| = 1
        // t * (|dx|/w + |dy|/h) = 1
        val t = 1.0 / (absDx / w + absDy / h)

        return Point(node.x + t * dx, node.y + t * dy)
    }

    /**
     * 圆形边缘交点计算
     */
    private fun intersectCircle(node: Graph.NodeData, point: Point): Point {
        val r = maxOf(node.width, node.height) / 2
        val dx = point.x - node.x
        val dy = point.y - node.y
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)

        if (dist == 0.0) return Point(node.x + r, node.y)

        return Point(
            node.x + dx * r / dist,
            node.y + dy * r / dist
        )
    }
}
