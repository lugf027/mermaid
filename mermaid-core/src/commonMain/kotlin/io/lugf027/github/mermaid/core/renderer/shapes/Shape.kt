package io.lugf027.github.mermaid.core.renderer.shapes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import io.lugf027.github.mermaid.core.types.Bounds
import io.lugf027.github.mermaid.core.types.Point
import io.lugf027.github.mermaid.core.types.ShapeId

/**
 * Shape 抽象接口。
 * 定义节点形状的绘制、边界计算和交点检测方法。
 * 对应 mermaid-js rendering-util/rendering-elements/shapes/ 中的各形状。
 */
interface Shape {
    /** 形状标识 */
    val shapeId: ShapeId

    /**
     * 在 DrawScope 中绘制该形状。
     * @param drawScope 绘制上下文
     * @param center 节点中心坐标
     * @param size 节点大小
     * @param label 标签文本
     * @param fillColor 填充颜色
     * @param borderColor 边框颜色
     * @param borderWidth 边框宽度
     * @param textMeasurer 文本测量器
     * @param textColor 文本颜色
     * @param fontSize 字号
     */
    fun draw(
        drawScope: DrawScope,
        center: Offset,
        size: Size,
        label: String,
        fillColor: Color,
        borderColor: Color,
        borderWidth: Float = 2f,
        textMeasurer: TextMeasurer,
        textColor: Color = Color.Black,
        fontSize: Float = 14f,
    )

    /**
     * 获取形状边界。
     */
    fun getBounds(center: Offset, size: Size): Bounds {
        return Bounds(
            x = center.x - size.width / 2f,
            y = center.y - size.height / 2f,
            width = size.width,
            height = size.height,
        )
    }

    /**
     * 计算从外部点到形状边界的交点。
     * 用于边的终点计算。
     * @param from 外部点
     * @param center 形状中心
     * @param size 形状大小
     * @return 交点坐标
     */
    fun getIntersection(from: Point, center: Point, size: Size): Point {
        // 默认使用矩形交点计算
        val dx = from.x - center.x
        val dy = from.y - center.y
        val halfW = size.width / 2f
        val halfH = size.height / 2f

        if (dx == 0f && dy == 0f) return center

        val scaleX = if (dx != 0f) halfW / kotlin.math.abs(dx) else Float.MAX_VALUE
        val scaleY = if (dy != 0f) halfH / kotlin.math.abs(dy) else Float.MAX_VALUE
        val scale = kotlin.math.min(scaleX, scaleY)

        return Point(center.x + dx * scale, center.y + dy * scale)
    }

    /**
     * 计算给定标签文本后的推荐节点大小。
     */
    fun calculateSize(
        textMeasurer: TextMeasurer,
        label: String,
        fontSize: Float = 14f,
        padding: Float = 16f,
    ): Size {
        val style = androidx.compose.ui.text.TextStyle(fontSize = androidx.compose.ui.unit.TextUnit(fontSize, androidx.compose.ui.unit.TextUnitType.Sp))
        val result = textMeasurer.measure(label, style)
        return Size(
            result.size.width + padding * 2,
            result.size.height + padding * 2,
        )
    }
}
