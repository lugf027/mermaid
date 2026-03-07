package io.lugf027.github.mermaid.core.renderer.compose

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import io.lugf027.github.mermaid.core.themes.ColorUtils
import io.lugf027.github.mermaid.core.types.Point
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * DrawScope 扩展函数集合。
 * 提供 drawText/drawArrow/drawPath 等通用绘制能力。
 */

/**
 * 绘制圆角矩形（填充 + 描边）。
 */
fun DrawScope.drawRoundedRect(
    topLeft: Offset,
    size: Size,
    fillColor: Color,
    borderColor: Color,
    borderWidth: Float = 2f,
    cornerRadius: Float = 4f,
) {
    drawRoundRect(
        color = fillColor,
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(cornerRadius),
        style = Fill,
    )
    drawRoundRect(
        color = borderColor,
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(cornerRadius),
        style = Stroke(width = borderWidth),
    )
}

/**
 * 绘制菱形。
 */
fun DrawScope.drawDiamond(
    center: Offset,
    width: Float,
    height: Float,
    fillColor: Color,
    borderColor: Color,
    borderWidth: Float = 2f,
) {
    val halfW = width / 2f
    val halfH = height / 2f
    val path = Path().apply {
        moveTo(center.x, center.y - halfH)
        lineTo(center.x + halfW, center.y)
        lineTo(center.x, center.y + halfH)
        lineTo(center.x - halfW, center.y)
        close()
    }
    drawPath(path, fillColor, style = Fill)
    drawPath(path, borderColor, style = Stroke(width = borderWidth))
}

/**
 * 绘制六边形。
 */
fun DrawScope.drawHexagon(
    center: Offset,
    width: Float,
    height: Float,
    fillColor: Color,
    borderColor: Color,
    borderWidth: Float = 2f,
) {
    val halfW = width / 2f
    val halfH = height / 2f
    val sideInset = halfH * 0.577f // tan(30°) ≈ 0.577
    val path = Path().apply {
        moveTo(center.x - halfW + sideInset, center.y - halfH)
        lineTo(center.x + halfW - sideInset, center.y - halfH)
        lineTo(center.x + halfW, center.y)
        lineTo(center.x + halfW - sideInset, center.y + halfH)
        lineTo(center.x - halfW + sideInset, center.y + halfH)
        lineTo(center.x - halfW, center.y)
        close()
    }
    drawPath(path, fillColor, style = Fill)
    drawPath(path, borderColor, style = Stroke(width = borderWidth))
}

/**
 * 绘制体育场/胶囊形。
 */
fun DrawScope.drawStadium(
    topLeft: Offset,
    size: Size,
    fillColor: Color,
    borderColor: Color,
    borderWidth: Float = 2f,
) {
    val radius = size.height / 2f
    drawRoundRect(
        color = fillColor,
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(radius),
        style = Fill,
    )
    drawRoundRect(
        color = borderColor,
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(radius),
        style = Stroke(width = borderWidth),
    )
}

/**
 * 绘制圆柱体（数据库形状）。
 */
fun DrawScope.drawCylinder(
    center: Offset,
    width: Float,
    height: Float,
    fillColor: Color,
    borderColor: Color,
    borderWidth: Float = 2f,
) {
    val halfW = width / 2f
    val halfH = height / 2f
    val ovalH = height * 0.12f // 椭圆高度占比

    // 主体矩形
    drawRect(
        color = fillColor,
        topLeft = Offset(center.x - halfW, center.y - halfH + ovalH),
        size = Size(width, height - 2 * ovalH),
    )
    // 左右边线
    drawLine(borderColor, Offset(center.x - halfW, center.y - halfH + ovalH), Offset(center.x - halfW, center.y + halfH - ovalH), borderWidth)
    drawLine(borderColor, Offset(center.x + halfW, center.y - halfH + ovalH), Offset(center.x + halfW, center.y + halfH - ovalH), borderWidth)

    // 顶部椭圆
    drawOval(fillColor, Offset(center.x - halfW, center.y - halfH), Size(width, ovalH * 2), style = Fill)
    drawOval(borderColor, Offset(center.x - halfW, center.y - halfH), Size(width, ovalH * 2), style = Stroke(width = borderWidth))

    // 底部椭圆（半弧）
    drawArc(
        color = fillColor,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - halfW, center.y + halfH - 2 * ovalH),
        size = Size(width, ovalH * 2),
        style = Fill,
    )
    drawArc(
        color = borderColor,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - halfW, center.y + halfH - 2 * ovalH),
        size = Size(width, ovalH * 2),
        style = Stroke(width = borderWidth),
    )
}

/**
 * 绘制三角形箭头。
 */
fun DrawScope.drawArrowHead(
    tip: Offset,
    angle: Float,
    size: Float = 10f,
    color: Color = Color.Black,
    filled: Boolean = true,
) {
    val leftAngle = angle + 2.5f
    val rightAngle = angle - 2.5f

    val left = Offset(
        tip.x - size * cos(leftAngle),
        tip.y - size * sin(leftAngle),
    )
    val right = Offset(
        tip.x - size * cos(rightAngle),
        tip.y - size * sin(rightAngle),
    )

    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(left.x, left.y)
        lineTo(right.x, right.y)
        close()
    }

    if (filled) {
        drawPath(path, color, style = Fill)
    } else {
        drawPath(path, color, style = Stroke(width = 1.5f))
    }
}

/**
 * 绘制连接两点的曲线路径。
 */
fun DrawScope.drawEdgePath(
    points: List<Point>,
    color: Color = Color.Black,
    strokeWidth: Float = 2f,
    isDotted: Boolean = false,
    isThick: Boolean = false,
) {
    if (points.size < 2) return

    val actualWidth = if (isThick) strokeWidth * 2f else strokeWidth
    val pathEffect = if (isDotted) PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f) else null

    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        if (points.size == 2) {
            lineTo(points.last().x, points.last().y)
        } else {
            // 使用三次贝塞尔曲线平滑路径
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val cx = (prev.x + curr.x) / 2f
                val cy = (prev.y + curr.y) / 2f
                quadraticBezierTo(prev.x, prev.y, cx, cy)
            }
            val last = points.last()
            lineTo(last.x, last.y)
        }
    }

    drawPath(path, color, style = Stroke(width = actualWidth, pathEffect = pathEffect))
}

/**
 * 在指定位置绘制居中文本。
 */
fun DrawScope.drawCenteredText(
    text: String,
    center: Offset,
    textMeasurer: TextMeasurer,
    style: TextStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
) {
    val result = textMeasurer.measure(text, style)
    drawText(
        textLayoutResult = result,
        topLeft = Offset(
            center.x - result.size.width / 2f,
            center.y - result.size.height / 2f,
        ),
    )
}

/**
 * 将 CSS 颜色字符串转为 Compose Color。
 */
fun String.toComposeColor(): Color {
    return try {
        ColorUtils.parseColor(this)
    } catch (_: Exception) {
        Color.Black
    }
}
