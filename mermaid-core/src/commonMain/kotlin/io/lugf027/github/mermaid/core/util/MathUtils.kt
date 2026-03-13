package io.lugf027.github.mermaid.core.util

import kotlin.math.*

/**
 * 数学工具 - 弧形计算、贝塞尔曲线、角度转换等
 */
object MathUtils {

    /** 2D 坐标点 */
    data class Point(val x: Double, val y: Double)

    /** 度 → 弧度 */
    fun degToRad(degrees: Double): Double = degrees * PI / 180.0

    /** 弧度 → 度 */
    fun radToDeg(radians: Double): Double = radians * 180.0 / PI

    /**
     * 在弧形上根据角度获取坐标点
     */
    fun pointOnArc(cx: Double, cy: Double, radius: Double, angleDeg: Double): Point {
        val rad = degToRad(angleDeg)
        return Point(cx + radius * cos(rad), cy + radius * sin(rad))
    }

    /**
     * 计算两点之间的距离
     */
    fun distance(p1: Point, p2: Point): Double {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * 线段与矩形的交点
     */
    fun intersectRect(
        rectX: Double, rectY: Double, rectW: Double, rectH: Double,
        pointX: Double, pointY: Double
    ): Point {
        val cx = rectX + rectW / 2
        val cy = rectY + rectH / 2
        val dx = pointX - cx
        val dy = pointY - cy

        if (dx == 0.0 && dy == 0.0) return Point(cx, cy)

        val hw = rectW / 2
        val hh = rectH / 2

        val sx: Double
        val sy: Double

        if (abs(dy) * hw > abs(dx) * hh) {
            // 上/下边
            sy = if (dy > 0) hh else -hh
            sx = sy * dx / dy
        } else {
            // 左/右边
            sx = if (dx > 0) hw else -hw
            sy = sx * dy / dx
        }

        return Point(cx + sx, cy + sy)
    }

    /**
     * 线段与圆的交点
     */
    fun intersectCircle(
        cx: Double, cy: Double, radius: Double,
        pointX: Double, pointY: Double
    ): Point {
        val dx = pointX - cx
        val dy = pointY - cy
        val dist = sqrt(dx * dx + dy * dy)
        if (dist == 0.0) return Point(cx + radius, cy)
        return Point(
            cx + dx * radius / dist,
            cy + dy * radius / dist
        )
    }

    /**
     * 线段与菱形的交点
     */
    fun intersectDiamond(
        cx: Double, cy: Double, hw: Double, hh: Double,
        pointX: Double, pointY: Double
    ): Point {
        val dx = pointX - cx
        val dy = pointY - cy

        if (dx == 0.0 && dy == 0.0) return Point(cx, cy)

        val absDx = abs(dx)
        val absDy = abs(dy)

        // 菱形的四条边的参数
        val t = if (absDx * hh + absDy * hw == 0.0) 0.0
        else (hw * hh) / (absDx * hh + absDy * hw)

        return Point(cx + t * dx, cy + t * dy)
    }

    /**
     * 线性插值
     */
    fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t

    /**
     * 将值限制在范围内
     */
    fun clamp(value: Double, min: Double, max: Double): Double =
        max(min, min(max, value))

    /**
     * 三次贝塞尔曲线上的点
     */
    fun cubicBezier(
        p0: Point, p1: Point, p2: Point, p3: Point, t: Double
    ): Point {
        val u = 1 - t
        val uu = u * u
        val uuu = uu * u
        val tt = t * t
        val ttt = tt * t

        return Point(
            uuu * p0.x + 3 * uu * t * p1.x + 3 * u * tt * p2.x + ttt * p3.x,
            uuu * p0.y + 3 * uu * t * p1.y + 3 * u * tt * p2.y + ttt * p3.y
        )
    }
}
