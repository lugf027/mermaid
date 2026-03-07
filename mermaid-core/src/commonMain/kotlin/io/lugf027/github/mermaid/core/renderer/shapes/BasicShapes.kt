package io.lugf027.github.mermaid.core.renderer.shapes

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import io.lugf027.github.mermaid.core.types.Point
import io.lugf027.github.mermaid.core.types.ShapeId
import kotlin.math.*

/**
 * 矩形形状。
 */
class RectShape : Shape {
    override val shapeId = ShapeId.RECT

    override fun draw(
        drawScope: DrawScope, center: Offset, size: Size, label: String,
        fillColor: Color, borderColor: Color, borderWidth: Float,
        textMeasurer: TextMeasurer, textColor: Color, fontSize: Float,
    ) = with(drawScope) {
        val topLeft = Offset(center.x - size.width / 2f, center.y - size.height / 2f)
        drawRect(fillColor, topLeft, size, style = Fill)
        drawRect(borderColor, topLeft, size, style = Stroke(width = borderWidth))
        drawLabel(this, center, label, textMeasurer, textColor, fontSize)
    }
}

/**
 * 圆角矩形形状。
 */
class RoundedRectShape : Shape {
    override val shapeId = ShapeId.ROUNDED_RECT

    override fun draw(
        drawScope: DrawScope, center: Offset, size: Size, label: String,
        fillColor: Color, borderColor: Color, borderWidth: Float,
        textMeasurer: TextMeasurer, textColor: Color, fontSize: Float,
    ) = with(drawScope) {
        val topLeft = Offset(center.x - size.width / 2f, center.y - size.height / 2f)
        drawRoundRect(fillColor, topLeft, size, CornerRadius(8f), style = Fill)
        drawRoundRect(borderColor, topLeft, size, CornerRadius(8f), style = Stroke(width = borderWidth))
        drawLabel(this, center, label, textMeasurer, textColor, fontSize)
    }
}

/**
 * 圆形形状。
 */
class CircleShape : Shape {
    override val shapeId = ShapeId.CIRCLE

    override fun draw(
        drawScope: DrawScope, center: Offset, size: Size, label: String,
        fillColor: Color, borderColor: Color, borderWidth: Float,
        textMeasurer: TextMeasurer, textColor: Color, fontSize: Float,
    ) = with(drawScope) {
        val radius = min(size.width, size.height) / 2f
        drawCircle(fillColor, radius, center, style = Fill)
        drawCircle(borderColor, radius, center, style = Stroke(width = borderWidth))
        drawLabel(this, center, label, textMeasurer, textColor, fontSize)
    }

    override fun getIntersection(from: Point, center: Point, size: Size): Point {
        val radius = min(size.width, size.height) / 2f
        val angle = atan2(from.y - center.y, from.x - center.x)
        return Point(center.x + radius * cos(angle), center.y + radius * sin(angle))
    }
}

/**
 * 椭圆形状。
 */
class EllipseShape : Shape {
    override val shapeId = ShapeId.ELLIPSE

    override fun draw(
        drawScope: DrawScope, center: Offset, size: Size, label: String,
        fillColor: Color, borderColor: Color, borderWidth: Float,
        textMeasurer: TextMeasurer, textColor: Color, fontSize: Float,
    ) = with(drawScope) {
        val topLeft = Offset(center.x - size.width / 2f, center.y - size.height / 2f)
        drawOval(fillColor, topLeft, size, style = Fill)
        drawOval(borderColor, topLeft, size, style = Stroke(width = borderWidth))
        drawLabel(this, center, label, textMeasurer, textColor, fontSize)
    }
}

/**
 * 菱形形状。
 */
class DiamondShape : Shape {
    override val shapeId = ShapeId.DIAMOND

    override fun draw(
        drawScope: DrawScope, center: Offset, size: Size, label: String,
        fillColor: Color, borderColor: Color, borderWidth: Float,
        textMeasurer: TextMeasurer, textColor: Color, fontSize: Float,
    ) = with(drawScope) {
        val halfW = size.width / 2f
        val halfH = size.height / 2f
        val path = Path().apply {
            moveTo(center.x, center.y - halfH)
            lineTo(center.x + halfW, center.y)
            lineTo(center.x, center.y + halfH)
            lineTo(center.x - halfW, center.y)
            close()
        }
        drawPath(path, fillColor, style = Fill)
        drawPath(path, borderColor, style = Stroke(width = borderWidth))
        drawLabel(this, center, label, textMeasurer, textColor, fontSize)
    }
}

/**
 * 六边形形状。
 */
class HexagonShape : Shape {
    override val shapeId = ShapeId.HEXAGON

    override fun draw(
        drawScope: DrawScope, center: Offset, size: Size, label: String,
        fillColor: Color, borderColor: Color, borderWidth: Float,
        textMeasurer: TextMeasurer, textColor: Color, fontSize: Float,
    ) = with(drawScope) {
        val halfW = size.width / 2f
        val halfH = size.height / 2f
        val indent = halfH * 0.5f
        val path = Path().apply {
            moveTo(center.x - halfW + indent, center.y - halfH)
            lineTo(center.x + halfW - indent, center.y - halfH)
            lineTo(center.x + halfW, center.y)
            lineTo(center.x + halfW - indent, center.y + halfH)
            lineTo(center.x - halfW + indent, center.y + halfH)
            lineTo(center.x - halfW, center.y)
            close()
        }
        drawPath(path, fillColor, style = Fill)
        drawPath(path, borderColor, style = Stroke(width = borderWidth))
        drawLabel(this, center, label, textMeasurer, textColor, fontSize)
    }
}

/**
 * 体育场/胶囊形状。
 */
class StadiumShape : Shape {
    override val shapeId = ShapeId.STADIUM

    override fun draw(
        drawScope: DrawScope, center: Offset, size: Size, label: String,
        fillColor: Color, borderColor: Color, borderWidth: Float,
        textMeasurer: TextMeasurer, textColor: Color, fontSize: Float,
    ) = with(drawScope) {
        val topLeft = Offset(center.x - size.width / 2f, center.y - size.height / 2f)
        val radius = size.height / 2f
        drawRoundRect(fillColor, topLeft, size, CornerRadius(radius), style = Fill)
        drawRoundRect(borderColor, topLeft, size, CornerRadius(radius), style = Stroke(width = borderWidth))
        drawLabel(this, center, label, textMeasurer, textColor, fontSize)
    }
}

/**
 * 圆柱体形状。
 */
class CylinderShape : Shape {
    override val shapeId = ShapeId.CYLINDER

    override fun draw(
        drawScope: DrawScope, center: Offset, size: Size, label: String,
        fillColor: Color, borderColor: Color, borderWidth: Float,
        textMeasurer: TextMeasurer, textColor: Color, fontSize: Float,
    ) = with(drawScope) {
        val halfW = size.width / 2f
        val halfH = size.height / 2f
        val ovalH = size.height * 0.12f
        // 主体矩形
        drawRect(fillColor, Offset(center.x - halfW, center.y - halfH + ovalH), Size(size.width, size.height - 2 * ovalH))
        drawLine(borderColor, Offset(center.x - halfW, center.y - halfH + ovalH), Offset(center.x - halfW, center.y + halfH - ovalH), borderWidth)
        drawLine(borderColor, Offset(center.x + halfW, center.y - halfH + ovalH), Offset(center.x + halfW, center.y + halfH - ovalH), borderWidth)
        // 顶部椭圆
        drawOval(fillColor, Offset(center.x - halfW, center.y - halfH), Size(size.width, ovalH * 2))
        drawOval(borderColor, Offset(center.x - halfW, center.y - halfH), Size(size.width, ovalH * 2), style = Stroke(width = borderWidth))
        // 底部半椭圆
        drawArc(borderColor, 0f, 180f, false, Offset(center.x - halfW, center.y + halfH - 2 * ovalH), Size(size.width, ovalH * 2), style = Stroke(width = borderWidth))
        drawLabel(this, center, label, textMeasurer, textColor, fontSize)
    }
}

/**
 * 子程序/双边框矩形。
 */
class SubroutineShape : Shape {
    override val shapeId = ShapeId.SUBROUTINE

    override fun draw(
        drawScope: DrawScope, center: Offset, size: Size, label: String,
        fillColor: Color, borderColor: Color, borderWidth: Float,
        textMeasurer: TextMeasurer, textColor: Color, fontSize: Float,
    ) = with(drawScope) {
        val topLeft = Offset(center.x - size.width / 2f, center.y - size.height / 2f)
        drawRect(fillColor, topLeft, size, style = Fill)
        drawRect(borderColor, topLeft, size, style = Stroke(width = borderWidth))
        // 内侧双竖线
        val inset = 8f
        drawLine(borderColor, Offset(topLeft.x + inset, topLeft.y), Offset(topLeft.x + inset, topLeft.y + size.height), borderWidth)
        drawLine(borderColor, Offset(topLeft.x + size.width - inset, topLeft.y), Offset(topLeft.x + size.width - inset, topLeft.y + size.height), borderWidth)
        drawLabel(this, center, label, textMeasurer, textColor, fontSize)
    }
}

/**
 * 平行四边形形状。
 */
class ParallelogramShape : Shape {
    override val shapeId = ShapeId.PARALLELOGRAM

    override fun draw(
        drawScope: DrawScope, center: Offset, size: Size, label: String,
        fillColor: Color, borderColor: Color, borderWidth: Float,
        textMeasurer: TextMeasurer, textColor: Color, fontSize: Float,
    ) = with(drawScope) {
        val halfW = size.width / 2f
        val halfH = size.height / 2f
        val skew = size.height * 0.3f
        val path = Path().apply {
            moveTo(center.x - halfW + skew, center.y - halfH)
            lineTo(center.x + halfW, center.y - halfH)
            lineTo(center.x + halfW - skew, center.y + halfH)
            lineTo(center.x - halfW, center.y + halfH)
            close()
        }
        drawPath(path, fillColor, style = Fill)
        drawPath(path, borderColor, style = Stroke(width = borderWidth))
        drawLabel(this, center, label, textMeasurer, textColor, fontSize)
    }
}

/**
 * 梯形形状。
 */
class TrapezoidShape : Shape {
    override val shapeId = ShapeId.TRAPEZOID

    override fun draw(
        drawScope: DrawScope, center: Offset, size: Size, label: String,
        fillColor: Color, borderColor: Color, borderWidth: Float,
        textMeasurer: TextMeasurer, textColor: Color, fontSize: Float,
    ) = with(drawScope) {
        val halfW = size.width / 2f
        val halfH = size.height / 2f
        val indent = size.width * 0.15f
        val path = Path().apply {
            moveTo(center.x - halfW + indent, center.y - halfH)
            lineTo(center.x + halfW - indent, center.y - halfH)
            lineTo(center.x + halfW, center.y + halfH)
            lineTo(center.x - halfW, center.y + halfH)
            close()
        }
        drawPath(path, fillColor, style = Fill)
        drawPath(path, borderColor, style = Stroke(width = borderWidth))
        drawLabel(this, center, label, textMeasurer, textColor, fontSize)
    }
}

// ─── 辅助函数 ──────────────────────────────────────────────────

private fun drawLabel(
    drawScope: DrawScope,
    center: Offset,
    label: String,
    textMeasurer: TextMeasurer,
    textColor: Color,
    fontSize: Float,
) {
    if (label.isEmpty()) return
    val style = TextStyle(fontSize = fontSize.sp, color = textColor)
    val result = textMeasurer.measure(label, style)
    drawScope.drawText(
        textLayoutResult = result,
        topLeft = Offset(
            center.x - result.size.width / 2f,
            center.y - result.size.height / 2f,
        ),
    )
}
