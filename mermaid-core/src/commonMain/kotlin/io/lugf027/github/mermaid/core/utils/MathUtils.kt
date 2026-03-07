package io.lugf027.github.mermaid.core.utils

import io.lugf027.github.mermaid.core.types.Point
import kotlin.math.*

/**
 * 数学工具函数。
 * 提供角度计算、贝塞尔曲线、多边形运算等功能。
 */
object MathUtils {

    /** 度数转弧度 */
    fun toRadians(degrees: Float): Float = degrees * PI.toFloat() / 180f

    /** 弧度转度数 */
    fun toDegrees(radians: Float): Float = radians * 180f / PI.toFloat()

    /**
     * 计算两点间距离。
     */
    fun distance(p1: Point, p2: Point): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * 计算两点间角度（弧度）。
     */
    fun angle(p1: Point, p2: Point): Float {
        return atan2(p2.y - p1.y, p2.x - p1.x)
    }

    /**
     * 线性插值。
     */
    fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t
    }

    /**
     * 线段与矩形的交点。
     * @param lineStart 线段起点
     * @param lineEnd 线段终点
     * @param rectCenter 矩形中心
     * @param rectWidth 矩形宽度
     * @param rectHeight 矩形高度
     * @return 交点坐标，如果没有交点则返回 null
     */
    fun lineRectIntersect(
        lineStart: Point,
        lineEnd: Point,
        rectCenter: Point,
        rectWidth: Float,
        rectHeight: Float
    ): Point? {
        val halfW = rectWidth / 2f
        val halfH = rectHeight / 2f

        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y

        if (dx == 0f && dy == 0f) return null

        val scaleX = if (dx != 0f) (halfW / abs(dx)) else Float.MAX_VALUE
        val scaleY = if (dy != 0f) (halfH / abs(dy)) else Float.MAX_VALUE
        val scale = min(scaleX, scaleY)

        return Point(
            x = rectCenter.x + dx * scale,
            y = rectCenter.y + dy * scale
        )
    }

    /**
     * 线段与圆的交点。
     */
    fun lineCircleIntersect(
        lineStart: Point,
        lineEnd: Point,
        center: Point,
        radius: Float
    ): Point? {
        val angle = atan2(lineEnd.y - lineStart.y, lineEnd.x - lineStart.x)
        return Point(
            x = center.x + radius * cos(angle),
            y = center.y + radius * sin(angle)
        )
    }

    /**
     * 线段与菱形的交点。
     */
    fun lineDiamondIntersect(
        lineStart: Point,
        lineEnd: Point,
        center: Point,
        width: Float,
        height: Float
    ): Point? {
        val halfW = width / 2f
        val halfH = height / 2f

        // 菱形的四条边
        val top = Point(center.x, center.y - halfH)
        val right = Point(center.x + halfW, center.y)
        val bottom = Point(center.x, center.y + halfH)
        val left = Point(center.x - halfW, center.y)

        val edges = listOf(
            top to right, right to bottom,
            bottom to left, left to top
        )

        for ((edgeStart, edgeEnd) in edges) {
            val intersection = segmentIntersect(lineStart, lineEnd, edgeStart, edgeEnd)
            if (intersection != null) return intersection
        }
        return lineRectIntersect(lineStart, lineEnd, center, width, height)
    }

    /**
     * 两线段交点。
     */
    fun segmentIntersect(p1: Point, p2: Point, p3: Point, p4: Point): Point? {
        val d1x = p2.x - p1.x
        val d1y = p2.y - p1.y
        val d2x = p4.x - p3.x
        val d2y = p4.y - p3.y

        val denom = d1x * d2y - d1y * d2x
        if (abs(denom) < 1e-6f) return null

        val t = ((p3.x - p1.x) * d2y - (p3.y - p1.y) * d2x) / denom
        val u = ((p3.x - p1.x) * d1y - (p3.y - p1.y) * d1x) / denom

        if (t in 0f..1f && u in 0f..1f) {
            return Point(p1.x + t * d1x, p1.y + t * d1y)
        }
        return null
    }

    /**
     * 计算二次贝塞尔曲线上的点。
     */
    fun quadraticBezier(p0: Point, p1: Point, p2: Point, t: Float): Point {
        val mt = 1f - t
        return Point(
            x = mt * mt * p0.x + 2 * mt * t * p1.x + t * t * p2.x,
            y = mt * mt * p0.y + 2 * mt * t * p1.y + t * t * p2.y
        )
    }

    /**
     * 计算三次贝塞尔曲线上的点。
     */
    fun cubicBezier(p0: Point, p1: Point, p2: Point, p3: Point, t: Float): Point {
        val mt = 1f - t
        val mt2 = mt * mt
        val t2 = t * t
        return Point(
            x = mt2 * mt * p0.x + 3 * mt2 * t * p1.x + 3 * mt * t2 * p2.x + t2 * t * p3.x,
            y = mt2 * mt * p0.y + 3 * mt2 * t * p1.y + 3 * mt * t2 * p2.y + t2 * t * p3.y
        )
    }

    /**
     * 将值限制在指定范围内。
     */
    fun clamp(value: Float, min: Float, max: Float): Float {
        return value.coerceIn(min, max)
    }
}
