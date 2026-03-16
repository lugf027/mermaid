package io.lugf027.github.mermaid.core.layout.elk

import io.lugf027.github.mermaid.core.layout.Point
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * ELK 几何计算工具 - 精确对标 mermaid-js packages/mermaid-layout-elk/src/geometry.ts
 *
 * 提供矩形交点、边界检测、点裁切等几何计算函数。
 */
object ElkGeometry {

    /** 容差常量 - 对标 geometry.ts EPS = 1 */
    const val EPS = 1.0

    /** 推出距离 - 对标 geometry.ts PUSH_OUT = 10 */
    const val PUSH_OUT = 10.0

    /**
     * 矩形边界数据
     * @param x 中心 x
     * @param y 中心 y
     * @param width 宽度
     * @param height 高度
     * @param padding 内边距
     */
    data class RectBounds(
        val x: Double,
        val y: Double,
        val width: Double,
        val height: Double,
        val padding: Double = 0.0,
    )

    /**
     * 检测点是否在矩形边框上 - 对标 geometry.ts onBorder
     *
     * @param bounds 矩形边界
     * @param p 待检测的点
     * @param tol 容差 (默认 0.5)
     * @return 点是否在边框上
     */
    fun onBorder(bounds: RectBounds, p: Point, tol: Double = 0.5): Boolean {
        val halfW = bounds.width / 2
        val halfH = bounds.height / 2
        val left = bounds.x - halfW
        val right = bounds.x + halfW
        val top = bounds.y - halfH
        val bottom = bounds.y + halfH

        val onLeft = abs(p.x - left) <= tol && p.y >= top - tol && p.y <= bottom + tol
        val onRight = abs(p.x - right) <= tol && p.y >= top - tol && p.y <= bottom + tol
        val onTop = abs(p.y - top) <= tol && p.x >= left - tol && p.x <= right + tol
        val onBottom = abs(p.y - bottom) <= tol && p.x >= left - tol && p.x <= right + tol
        return onLeft || onRight || onTop || onBottom
    }

    /**
     * 计算矩形与线段的交点 - 精确对标 geometry.ts intersection
     *
     * 计算从 insidePoint 到 outsidePoint 的线段与矩形边框的交点。
     *
     * @param node 矩形边界（中心 x/y, width/height）
     * @param outsidePoint 矩形外部的点
     * @param insidePoint 矩形内部的点
     * @return 交点坐标
     */
    fun intersection(node: RectBounds, outsidePoint: Point, insidePoint: Point): Point {
        val x = node.x
        val y = node.y

        val dx = abs(x - insidePoint.x)
        val w = node.width / 2
        var r = if (insidePoint.x < outsidePoint.x) w - dx else w + dx
        val h = node.height / 2

        val Q = abs(outsidePoint.y - insidePoint.y)
        val R = abs(outsidePoint.x - insidePoint.x)

        if (abs(y - outsidePoint.y) * w > abs(x - outsidePoint.x) * h) {
            // 交点在矩形的上边或下边
            val q = if (insidePoint.y < outsidePoint.y) outsidePoint.y - h - y else y - h - outsidePoint.y
            r = (R * q) / Q
            var resX = if (insidePoint.x < outsidePoint.x) insidePoint.x + r else insidePoint.x - R + r
            var resY = if (insidePoint.y < outsidePoint.y) insidePoint.y + Q - q else insidePoint.y - Q + q

            // 仅处理轴对齐特殊情况
            if (R == 0.0) resX = outsidePoint.x
            if (Q == 0.0) resY = outsidePoint.y

            return Point(resX, resY)
        } else {
            // 交点在矩形的左边或右边
            r = if (insidePoint.x < outsidePoint.x) outsidePoint.x - w - x else x - w - outsidePoint.x
            val q = (Q * r) / R
            var _x = if (insidePoint.x < outsidePoint.x) insidePoint.x + R - r else insidePoint.x - R + r
            var _y = if (insidePoint.y < outsidePoint.y) insidePoint.y + q else insidePoint.y - q

            // 仅处理轴对齐特殊情况
            if (R == 0.0) _x = outsidePoint.x
            if (Q == 0.0) _y = outsidePoint.y

            return Point(_x, _y)
        }
    }

    /**
     * 检测点是否在节点外部 - 对标 geometry.ts outsideNode
     */
    fun outsideNode(node: RectBounds, point: Point): Boolean {
        val dx = abs(point.x - node.x)
        val dy = abs(point.y - node.y)
        val w = node.width / 2
        val h = node.height / 2
        return dx >= w || dy >= h
    }

    /**
     * 确保点确实在节点外部 - 对标 geometry.ts ensureTrulyOutside
     *
     * 如果点恰好在边框上（容差范围内），将其向外推出。
     */
    fun ensureTrulyOutside(bounds: RectBounds, p: Point, push: Double = PUSH_OUT): Point {
        val dx = abs(p.x - bounds.x)
        val dy = abs(p.y - bounds.y)
        val w = bounds.width / 2
        val h = bounds.height / 2
        if (abs(dx - w) < EPS || abs(dy - h) < EPS) {
            val dirX = p.x - bounds.x
            val dirY = p.y - bounds.y
            val len = sqrt(dirX * dirX + dirY * dirY)
            if (len > 0) {
                return Point(
                    bounds.x + (dirX / len) * (len + push),
                    bounds.y + (dirY / len) * (len + push)
                )
            }
        }
        return p
    }

    /**
     * 创建内部点 - 对标 geometry.ts makeInsidePoint
     */
    fun makeInsidePoint(bounds: RectBounds, outside: Point, center: Point): Point {
        val isVertical = abs(outside.x - bounds.x) < EPS
        val isHorizontal = abs(outside.y - bounds.y) < EPS
        return Point(
            x = when {
                isVertical -> outside.x
                outside.x < bounds.x -> bounds.x - bounds.width / 4
                else -> bounds.x + bounds.width / 4
            },
            y = if (isHorizontal) outside.y else center.y
        )
    }

    /**
     * 回退交点计算 - 对标 geometry.ts fallbackIntersection
     */
    fun fallbackIntersection(bounds: RectBounds, outside: Point, center: Point): Point {
        val inside = makeInsidePoint(bounds, outside, center)
        return intersection(bounds, outside, inside)
    }

    /**
     * 计算节点交点 - 对标 geometry.ts computeNodeIntersection
     *
     * 先确保外部点确实在外部，然后计算交点。
     */
    fun computeNodeIntersection(bounds: RectBounds, outside: Point, center: Point): Point {
        val outside2 = ensureTrulyOutside(bounds, outside)
        return fallbackIntersection(bounds, outside2, center)
    }

    /**
     * 替换端点 - 对标 geometry.ts replaceEndpoint
     *
     * @param points 点列表
     * @param which "start" 或 "end"
     * @param value 新端点值
     * @param tol 重复检测容差
     */
    fun replaceEndpoint(
        points: MutableList<Point>,
        which: String,
        value: Point?,
        tol: Double = 0.1
    ) {
        if (value == null || points.isEmpty()) return

        if (which == "start") {
            if (points.isNotEmpty() &&
                abs(points[0].x - value.x) < tol &&
                abs(points[0].y - value.y) < tol
            ) {
                // 重复的起点 - 移除它
                points.removeAt(0)
            } else {
                points[0] = value
            }
        } else {
            val last = points.size - 1
            if (points.isNotEmpty() &&
                abs(points[last].x - value.x) < tol &&
                abs(points[last].y - value.y) < tol
            ) {
                // 重复的终点 - 移除它
                points.removeAt(last)
            } else {
                points[last] = value
            }
        }
    }

    /**
     * 近似相等 - 对标 render.ts approxEq
     */
    fun approxEq(a: Double, b: Double, eps: Double = 1e-6): Boolean =
        abs(a - b) < eps

    /**
     * 检测点是否近似等于节点中心 - 对标 render.ts isCenterApprox
     */
    fun isCenterApprox(pt: Point, nodeX: Double, nodeY: Double): Boolean =
        approxEq(pt.x, nodeX) && approxEq(pt.y, nodeY)
}
