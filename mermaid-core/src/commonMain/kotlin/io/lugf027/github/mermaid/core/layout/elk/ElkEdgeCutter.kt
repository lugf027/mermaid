package io.lugf027.github.mermaid.core.layout.elk

import io.lugf027.github.mermaid.core.layout.Point
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * ELK 边裁切模块 - 精确对标 mermaid-js render.ts 的 cutter2() 及相关辅助函数
 *
 * 将 ELK 输出的边路径裁切到节点/子图的边框交点。
 *
 * 核心流程（对标 render.ts 第 993-1071 行）：
 * 1. 检测端点节点是否为子图(isGroup)
 * 2. 如果端点在子图边框上(onBorder)，跳过该端的 cutter2
 * 3. 否则执行完整的 cutter2 裁切
 * 4. 去除重复点和过近的尾部点
 */
object ElkEdgeCutter {

    /**
     * 处理一条边的端点裁切 - 对标 render.ts 第 1002-1041 行的完整逻辑
     *
     * @param points 边的点序列（已包含首尾的节点中心点）
     * @param startNode 起始节点信息
     * @param endNode 终止节点信息
     * @return 裁切后的点序列
     */
    fun processEdge(
        points: MutableList<Point>,
        startNode: NodeInfo,
        endNode: NodeInfo,
    ): List<Point> {
        if (points.isEmpty()) return emptyList()

        val startBounds = boundsFor(startNode)
        val endBounds = boundsFor(endNode)

        val prevPoints = points.toMutableList()

        // 获取候选边框点
        val (startCandidate, startCenterApprox) = getCandidateBorderPoint(
            prevPoints, startNode.centerX, startNode.centerY, "start"
        )
        val (endCandidate, endCenterApprox) = getCandidateBorderPoint(
            prevPoints, endNode.centerX, endNode.centerY, "end"
        )

        val skipStart = startNode.isGroup && ElkGeometry.onBorder(startBounds, startCandidate)
        val skipEnd = endNode.isGroup && ElkGeometry.onBorder(endBounds, endCandidate)

        // 移除自动添加的中心点（如果端点在边框上）
        dropAutoCenterPoint(prevPoints, "start", skipStart && startCenterApprox)
        dropAutoCenterPoint(prevPoints, "end", skipEnd && endCenterApprox)

        val resultPoints: MutableList<Point>

        if (skipStart || skipEnd) {
            // 部分跳过逻辑
            if (!skipStart) {
                applyStartIntersectionIfNeeded(prevPoints, startNode, startBounds)
            }
            if (!skipEnd) {
                applyEndIntersectionIfNeeded(prevPoints, endNode, endBounds)
            }
            resultPoints = prevPoints
        } else {
            // 完整 cutter2
            resultPoints = cutter2(startNode, endNode, prevPoints)
        }

        // 验证和清理
        val hasNaN = resultPoints.any { !it.x.isFinite() || !it.y.isFinite() }
        val cleaned = if (!resultPoints.isNotEmpty() || resultPoints.size < 2 || hasNaN) {
            val fallback = prevPoints.filter { it.x.isFinite() && it.y.isFinite() }
            if (fallback.size >= 2) fallback.toMutableList() else prevPoints
        } else {
            resultPoints
        }

        // 去除连续重复点（距离 < 1e-6）
        val deduped = cleaned.filterIndexed { i, p ->
            if (i == 0) true
            else {
                val prev = cleaned[i - 1]
                abs(p.x - prev.x) > 1e-6 || abs(p.y - prev.y) > 1e-6
            }
        }

        return deduped
    }

    /**
     * 完整的 cutter2 实现 - 精确对标 render.ts 第 615-707 行
     */
    private fun cutter2(
        startNode: NodeInfo,
        endNode: NodeInfo,
        _points: MutableList<Point>,
    ): MutableList<Point> {
        val startBounds = boundsFor(startNode)
        val endBounds = boundsFor(endNode)

        if (_points.isEmpty()) return mutableListOf()

        val points = _points.toMutableList()

        val startCenter = points[0]
        val endCenter = points[points.size - 1]

        // 找到第一个在起始节点外部的点
        var firstOutsideStartIndex = -1
        for (i in points.indices) {
            if (ElkGeometry.outsideNode(startBounds, points[i])) {
                firstOutsideStartIndex = i
                break
            }
        }

        // 计算与起始节点的交点
        if (firstOutsideStartIndex != -1) {
            val outsidePointForStart = points[firstOutsideStartIndex]
            val startIntersection = ElkGeometry.computeNodeIntersection(
                startBounds, outsidePointForStart, startCenter
            )
            ElkGeometry.replaceEndpoint(points, "start", startIntersection)
        }

        // 计算与终止节点的交点
        var outsidePointForEnd: Point? = null
        var outsideIndexForEnd = -1

        for (i in points.indices.reversed()) {
            if (ElkGeometry.outsideNode(endBounds, points[i])) {
                outsidePointForEnd = points[i]
                outsideIndexForEnd = i
                break
            }
        }

        if (outsidePointForEnd == null && points.size > 1) {
            outsidePointForEnd = points[points.size - 2]
            outsideIndexForEnd = points.size - 2
        }

        if (outsidePointForEnd != null) {
            val endIntersection = ElkGeometry.computeNodeIntersection(
                endBounds, outsidePointForEnd, endCenter
            )
            ElkGeometry.replaceEndpoint(points, "end", endIntersection)
        }

        // 尾部清理：最后两个点距离 < 2px 则移除最后一个
        if (points.size > 1) {
            val lastPoint = points[points.size - 1]
            val secondLastPoint = points[points.size - 2]
            val distance = sqrt(
                (lastPoint.x - secondLastPoint.x) * (lastPoint.x - secondLastPoint.x) +
                (lastPoint.y - secondLastPoint.y) * (lastPoint.y - secondLastPoint.y)
            )
            if (distance < 2) {
                points.removeAt(points.size - 1)
            }
        }

        return points
    }

    /**
     * 获取候选边框点 - 对标 render.ts getCandidateBorderPoint
     */
    private fun getCandidateBorderPoint(
        points: List<Point>,
        nodeX: Double,
        nodeY: Double,
        side: String,
    ): Pair<Point, Boolean> {
        if (points.isEmpty()) {
            return Pair(Point(nodeX, nodeY), true)
        }

        if (side == "start") {
            val first = points[0]
            val centerApprox = ElkGeometry.isCenterApprox(first, nodeX, nodeY)
            val candidate = if (centerApprox && points.size > 1) points[1] else first
            return Pair(candidate, centerApprox)
        } else {
            val last = points[points.size - 1]
            val centerApprox = ElkGeometry.isCenterApprox(last, nodeX, nodeY)
            val candidate = if (centerApprox && points.size > 1) points[points.size - 2] else last
            return Pair(candidate, centerApprox)
        }
    }

    /**
     * 移除自动添加的中心点 - 对标 render.ts dropAutoCenterPoint
     */
    private fun dropAutoCenterPoint(points: MutableList<Point>, side: String, doDrop: Boolean) {
        if (!doDrop) return

        if (side == "start") {
            if (points.isNotEmpty()) points.removeAt(0)
        } else {
            if (points.isNotEmpty()) points.removeAt(points.size - 1)
        }
    }

    /**
     * 仅对起点应用交点 - 对标 render.ts applyStartIntersectionIfNeeded
     */
    private fun applyStartIntersectionIfNeeded(
        points: MutableList<Point>,
        startNode: NodeInfo,
        startBounds: ElkGeometry.RectBounds,
    ) {
        var firstOutsideStartIndex = -1
        for (i in points.indices) {
            if (ElkGeometry.outsideNode(startBounds, points[i])) {
                firstOutsideStartIndex = i
                break
            }
        }
        if (firstOutsideStartIndex != -1) {
            val outsidePointForStart = points[firstOutsideStartIndex]
            val startCenter = points[0]
            val startIntersection = ElkGeometry.computeNodeIntersection(
                startBounds, outsidePointForStart, startCenter
            )
            ElkGeometry.replaceEndpoint(points, "start", startIntersection)
        }
    }

    /**
     * 仅对终点应用交点 - 对标 render.ts applyEndIntersectionIfNeeded
     */
    private fun applyEndIntersectionIfNeeded(
        points: MutableList<Point>,
        endNode: NodeInfo,
        endBounds: ElkGeometry.RectBounds,
    ) {
        var outsideIndexForEnd = -1
        for (i in points.indices.reversed()) {
            if (ElkGeometry.outsideNode(endBounds, points[i])) {
                outsideIndexForEnd = i
                break
            }
        }
        if (outsideIndexForEnd != -1) {
            val outsidePointForEnd = points[outsideIndexForEnd]
            val endCenter = points[points.size - 1]
            val endIntersection = ElkGeometry.computeNodeIntersection(
                endBounds, outsidePointForEnd, endCenter
            )
            ElkGeometry.replaceEndpoint(points, "end", endIntersection)
        }
    }

    /**
     * 构建节点边界 - 对标 render.ts boundsFor
     */
    private fun boundsFor(node: NodeInfo): ElkGeometry.RectBounds {
        val width = if (node.isGroup) {
            max(node.width, (node.labelWidth ?: 0.0) + node.padding)
        } else {
            node.width
        }
        return ElkGeometry.RectBounds(
            x = node.centerX,
            y = node.centerY,
            width = width,
            height = node.height,
            padding = node.padding,
        )
    }

    /**
     * 节点信息数据类 - 包含 cutter2 所需的所有节点信息
     */
    data class NodeInfo(
        val id: String,
        /** 节点中心的绝对 X 坐标 */
        val centerX: Double,
        /** 节点中心的绝对 Y 坐标 */
        val centerY: Double,
        val width: Double,
        val height: Double,
        val padding: Double = 0.0,
        val isGroup: Boolean = false,
        /** 子图的标签宽度 */
        val labelWidth: Double? = null,
    )
}
