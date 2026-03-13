package io.lugf027.github.mermaid.core.diagrams.flowchart

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
import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.renderer.compose.toComposeColor
import io.lugf027.github.mermaid.core.renderer.layout.DagreLayout
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.types.*
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * 流程图 Compose Canvas 渲染器。
 * 调用 Dagre 布局引擎计算坐标，然后通过 Canvas 绘制节点和边。
 *
 * 渲染流程：
 * 1. 从 FlowDb.getData() 获取 LayoutData
 * 2. 用 TextMeasurer 测量所有标签的文本尺寸，更新 node width/height
 * 3. 调用 DagreLayout.layout() 计算坐标
 * 4. 绘制边 → 绘制节点（节点覆盖在边上方）
 * 5. 绘制标题
 */
class FlowRenderer : DiagramRenderer {

    @Suppress("unused")
    private val tag = "FlowRenderer"

    companion object {
        // mermaid-js 默认 padding = 15 (config.schema.yaml)
        private const val DEFAULT_NODE_PADDING = 15f
        private const val DEFAULT_FONT_SIZE = 16f
        private const val ARROW_HEAD_SIZE = 8f  // mermaid-js: markerWidth=8, markerHeight=8
        private const val EDGE_LABEL_FONT_SIZE = 16f  // mermaid-js: 边标签继承全局 fontSize
        private const val TITLE_FONT_SIZE = 18f  // mermaid-js: .flowchartTitleText { font-size: 18px }
        private const val NODE_STROKE_WIDTH = 1.0f  // mermaid-js: .node rect { stroke-width: 1px }
        private const val EDGE_STROKE_WIDTH = 2.0f  // mermaid-js: .edgePath .path { stroke-width: 2.0px }
        private const val MARGIN = 20f
        private const val EDGE_LABEL_PADDING = 4f
    }

    override fun draw(
        drawScope: DrawScope,
        db: DiagramDB,
        config: MermaidConfig,
        theme: ThemeVariables,
        textMeasurer: TextMeasurer,
        size: Size,
    ) {
        val flowDb = db as? FlowDb ?: return
        val layoutData = flowDb.getData()
        if (layoutData.nodes.isEmpty()) return

        val direction = flowDb.getDirection()
        val nodeSpacing = config.flowchart?.nodeSpacing?.toFloat() ?: 50f
        val rankSpacing = config.flowchart?.rankSpacing?.toFloat() ?: 50f

        // 1. 文本测量 — 计算节点尺寸
        val textStyle = TextStyle(fontSize = DEFAULT_FONT_SIZE.sp)

        val updatedNodes = layoutData.nodes.map { node ->
            if (node.isGroup) return@map node
            // 处理 fa:icon 前缀 — 去掉 "fa:fa-xxx " 只保留后面的文本
            val displayLabel = stripFontAwesome(node.label)
            val measured = textMeasurer.measure(displayLabel, textStyle)
            val textW = measured.size.width.toFloat()
            val textH = measured.size.height.toFloat()
            // 不同形状需要不同的尺寸策略（精确匹配 mermaid-js 公式）
            val (w, h) = when (node.shape) {
                ShapeId.DIAMOND -> {
                    // mermaid-js question.ts: s = (bbox.width + padding) + (bbox.height + padding)
                    // 菱形是正方形旋转 45°，宽=高=s
                    val s = (textW + DEFAULT_NODE_PADDING) + (textH + DEFAULT_NODE_PADDING)
                    s to s
                }
                ShapeId.CIRCLE, ShapeId.DOUBLE_CIRCLE -> {
                    val d = max(textW, textH) + DEFAULT_NODE_PADDING * 2
                    d to d
                }
                ShapeId.HEXAGON -> {
                    (textW + DEFAULT_NODE_PADDING * 4) to (textH + DEFAULT_NODE_PADDING * 2)
                }
                ShapeId.STADIUM -> {
                    // 体育场形状：左右额外半圆空间
                    (textW + DEFAULT_NODE_PADDING * 2 + textH) to (textH + DEFAULT_NODE_PADDING * 2)
                }
                ShapeId.ROUNDED_RECT -> {
                    // 圆角矩形 (text)：mermaid-js roundedRect.ts: labelPaddingX = padding * 1
                    // totalWidth = textW + padding * 2, totalHeight = textH + padding * 2
                    (textW + DEFAULT_NODE_PADDING * 2) to (textH + DEFAULT_NODE_PADDING * 2)
                }
                else -> {
                    // 矩形 [text]：mermaid-js squareRect.ts: labelPaddingX = padding * 2
                    // drawRect.ts: totalWidth = bbox.width + labelPaddingX * 2 = textW + padding * 4
                    // totalHeight = bbox.height + labelPaddingY * 2 = textH + padding * 2
                    (textW + DEFAULT_NODE_PADDING * 4) to (textH + DEFAULT_NODE_PADDING * 2)
                }
            }
            node.copy(width = w, height = h, label = displayLabel)
        }

        // 2. 构建含尺寸信息的 LayoutData 并执行 Dagre 布局
        val dir = when (direction.uppercase()) {
            "TB", "TD" -> Direction.TB
            "BT" -> Direction.BT
            "LR" -> Direction.LR
            "RL" -> Direction.RL
            else -> Direction.TB
        }
        val dataForLayout = layoutData.copy(
            nodes = updatedNodes,
            direction = dir,
            nodeSep = nodeSpacing,
            rankSep = rankSpacing,
        )

        val dagre = DagreLayout()
        val renderData = dagre.layout(dataForLayout)

        if (renderData.nodes.isEmpty()) return

        // 3. 计算缩放与偏移（居中）
        val bounds = renderData.bounds
        val graphWidth = bounds.width
        val graphHeight = bounds.height
        val scale = min(
            (size.width - MARGIN * 2) / graphWidth.coerceAtLeast(1f),
            (size.height - MARGIN * 2 - TITLE_FONT_SIZE - 10f) / graphHeight.coerceAtLeast(1f),
        ).coerceIn(0.3f, 2f)

        val offsetX = (size.width - graphWidth * scale) / 2f - bounds.x * scale
        val offsetY = MARGIN + TITLE_FONT_SIZE + 10f - bounds.y * scale

        // 建立节点位置查找表
        val nodeMap = mutableMapOf<String, Node>()
        for (node in renderData.nodes) {
            nodeMap[node.id] = node
        }

        with(drawScope) {
            // 4. 绘制标题
            val title = flowDb.getDiagramTitle()
            if (title.isNotEmpty()) {
                val titleStyle = TextStyle(
                    fontSize = TITLE_FONT_SIZE.sp,
                    color = theme.textColor.toComposeColor(),
                )
                val titleResult = textMeasurer.measure(title, titleStyle)
                drawText(
                    textLayoutResult = titleResult,
                    topLeft = Offset((size.width - titleResult.size.width) / 2f, MARGIN / 2f),
                )
            }

            // 5. 绘制边
            for (edge in renderData.edges) {
                val srcNode = nodeMap[edge.start] ?: continue
                val tgtNode = nodeMap[edge.end] ?: continue

                // 将布局路径点转换为屏幕坐标
                val screenPoints = edge.points.map { pt ->
                    Offset(pt.x * scale + offsetX, pt.y * scale + offsetY)
                }
                val srcHalfW = srcNode.width * scale / 2f
                val srcHalfH = srcNode.height * scale / 2f
                val tgtHalfW = tgtNode.width * scale / 2f
                val tgtHalfH = tgtNode.height * scale / 2f

                drawFlowEdge(
                    screenPoints,
                    srcHalfW, srcHalfH, tgtHalfW, tgtHalfH,
                    edge, srcNode, tgtNode, theme, textMeasurer, scale,
                )
            }

            // 6. 绘制节点
            val nodeTextStyle = TextStyle(
                fontSize = (DEFAULT_FONT_SIZE * scale).sp,
                color = theme.textColor.toComposeColor(),
            )
            for (node in renderData.nodes) {
                if (node.isGroup) continue
                val center = Offset(node.x * scale + offsetX, node.y * scale + offsetY)
                val w = node.width * scale
                val h = node.height * scale
                drawFlowNode(center, w, h, node, theme, textMeasurer, nodeTextStyle, scale)
            }
        }
    }

    // ─── 节点绘制 ──────────────────────────────────

    private fun DrawScope.drawFlowNode(
        center: Offset,
        width: Float,
        height: Float,
        node: Node,
        theme: ThemeVariables,
        textMeasurer: TextMeasurer,
        textStyle: TextStyle,
        scale: Float,
    ) {
        // 使用 mermaid-js 默认主题颜色：nodeBkg (#ECECFF) + nodeBorder (#9370DB)
        val bgColor = theme.nodeBkg.toComposeColor()
        val borderColor = theme.nodeBorder.toComposeColor()
        val left = center.x - width / 2f
        val top = center.y - height / 2f
        val sw = NODE_STROKE_WIDTH * scale

        when (node.shape) {
            ShapeId.ROUNDED_RECT -> {
                // mermaid-js 的 () 形状：较大的圆角
                val cr = androidx.compose.ui.geometry.CornerRadius(height * 0.25f)
                drawRoundRect(bgColor, Offset(left, top), Size(width, height), cr)
                drawRoundRect(borderColor, Offset(left, top), Size(width, height), cr, style = Stroke(sw))
            }
            ShapeId.SQUARE_RECT, ShapeId.RECT -> {
                drawRect(bgColor, Offset(left, top), Size(width, height))
                drawRect(borderColor, Offset(left, top), Size(width, height), style = Stroke(sw))
            }
            ShapeId.CIRCLE, ShapeId.DOUBLE_CIRCLE -> {
                val r = max(width, height) / 2f
                drawCircle(bgColor, r, center)
                drawCircle(borderColor, r, center, style = Stroke(sw))
                if (node.shape == ShapeId.DOUBLE_CIRCLE) {
                    drawCircle(borderColor, r - 4f * scale, center, style = Stroke(sw))
                }
            }
            ShapeId.DIAMOND -> {
                val path = Path().apply {
                    moveTo(center.x, top); lineTo(left + width, center.y)
                    lineTo(center.x, top + height); lineTo(left, center.y); close()
                }
                drawPath(path, bgColor); drawPath(path, borderColor, style = Stroke(sw))
            }
            ShapeId.HEXAGON -> {
                val dx = width * 0.15f
                val path = Path().apply {
                    moveTo(left + dx, top); lineTo(left + width - dx, top)
                    lineTo(left + width, center.y); lineTo(left + width - dx, top + height)
                    lineTo(left + dx, top + height); lineTo(left, center.y); close()
                }
                drawPath(path, bgColor); drawPath(path, borderColor, style = Stroke(sw))
            }
            ShapeId.STADIUM -> {
                val r = height / 2f
                val cr = androidx.compose.ui.geometry.CornerRadius(r)
                drawRoundRect(bgColor, Offset(left, top), Size(width, height), cr)
                drawRoundRect(borderColor, Offset(left, top), Size(width, height), cr, style = Stroke(sw))
            }
            ShapeId.CYLINDER -> {
                val eH = height * 0.15f
                drawRect(bgColor, Offset(left, top + eH / 2f), Size(width, height - eH))
                drawOval(bgColor, Offset(left, top), Size(width, eH))
                drawOval(borderColor, Offset(left, top), Size(width, eH), style = Stroke(sw))
                drawOval(bgColor, Offset(left, top + height - eH), Size(width, eH))
                drawOval(borderColor, Offset(left, top + height - eH), Size(width, eH), style = Stroke(sw))
                drawLine(borderColor, Offset(left, top + eH / 2f), Offset(left, top + height - eH / 2f), sw)
                drawLine(borderColor, Offset(left + width, top + eH / 2f), Offset(left + width, top + height - eH / 2f), sw)
            }
            ShapeId.SUBROUTINE -> {
                val inset = 8f * scale
                drawRect(bgColor, Offset(left, top), Size(width, height))
                drawRect(borderColor, Offset(left, top), Size(width, height), style = Stroke(sw))
                drawLine(borderColor, Offset(left + inset, top), Offset(left + inset, top + height), sw)
                drawLine(borderColor, Offset(left + width - inset, top), Offset(left + width - inset, top + height), sw)
            }
            ShapeId.TRAPEZOID -> {
                val dx = width * 0.15f
                val path = Path().apply {
                    moveTo(left + dx, top); lineTo(left + width - dx, top)
                    lineTo(left + width, top + height); lineTo(left, top + height); close()
                }
                drawPath(path, bgColor); drawPath(path, borderColor, style = Stroke(sw))
            }
            ShapeId.INV_TRAPEZOID -> {
                val dx = width * 0.15f
                val path = Path().apply {
                    moveTo(left, top); lineTo(left + width, top)
                    lineTo(left + width - dx, top + height); lineTo(left + dx, top + height); close()
                }
                drawPath(path, bgColor); drawPath(path, borderColor, style = Stroke(sw))
            }
            ShapeId.LEAN_RIGHT -> {
                val dx = width * 0.15f
                val path = Path().apply {
                    moveTo(left + dx, top); lineTo(left + width, top)
                    lineTo(left + width - dx, top + height); lineTo(left, top + height); close()
                }
                drawPath(path, bgColor); drawPath(path, borderColor, style = Stroke(sw))
            }
            ShapeId.LEAN_LEFT -> {
                val dx = width * 0.15f
                val path = Path().apply {
                    moveTo(left, top); lineTo(left + width - dx, top)
                    lineTo(left + width, top + height); lineTo(left + dx, top + height); close()
                }
                drawPath(path, bgColor); drawPath(path, borderColor, style = Stroke(sw))
            }
            ShapeId.ODD -> {
                val dx = width * 0.12f
                val path = Path().apply {
                    moveTo(left + dx, top); lineTo(left + width, top)
                    lineTo(left + width, top + height); lineTo(left + dx, top + height)
                    lineTo(left, center.y); close()
                }
                drawPath(path, bgColor); drawPath(path, borderColor, style = Stroke(sw))
            }
            ShapeId.ELLIPSE -> {
                drawOval(bgColor, Offset(left, top), Size(width, height))
                drawOval(borderColor, Offset(left, top), Size(width, height), style = Stroke(sw))
            }
            else -> {
                drawRect(bgColor, Offset(left, top), Size(width, height))
                drawRect(borderColor, Offset(left, top), Size(width, height), style = Stroke(sw))
            }
        }

        // 节点文本
        val textResult = textMeasurer.measure(node.label, textStyle)
        drawText(
            textLayoutResult = textResult,
            topLeft = Offset(center.x - textResult.size.width / 2f, center.y - textResult.size.height / 2f),
        )
    }

    // ─── 边绘制 ────────────────────────────────────

    private fun DrawScope.drawFlowEdge(
        pathPoints: List<Offset>,
        srcHalfW: Float, srcHalfH: Float,
        tgtHalfW: Float, tgtHalfH: Float,
        edge: Edge,
        srcNode: Node,
        tgtNode: Node,
        theme: ThemeVariables,
        textMeasurer: TextMeasurer,
        scale: Float,
    ) {
        if (pathPoints.size < 2) return
        val lineColor = theme.lineColor.toComposeColor()

        // 计算起终点在节点边界上的交点
        val srcCenter = pathPoints.first()
        val tgtCenter = pathPoints.last()
        // 对于折线，第二个点指示出发方向，倒数第二个点指示到达方向
        val srcDir = if (pathPoints.size > 1) pathPoints[1] else tgtCenter
        val tgtDir = if (pathPoints.size > 1) pathPoints[pathPoints.size - 2] else srcCenter
        val srcEdge = edgePointForShape(srcCenter, srcDir, srcHalfW, srcHalfH, srcNode.shape)
        val tgtEdge = edgePointForShape(tgtCenter, tgtDir, tgtHalfW, tgtHalfH, tgtNode.shape)

        // 构建实际绘制路径：替换首尾点为边界交点
        val drawPoints = mutableListOf<Offset>()
        drawPoints.add(srcEdge)
        for (i in 1 until pathPoints.size - 1) {
            drawPoints.add(pathPoints[i])
        }
        drawPoints.add(tgtEdge)

        // 线条
        val sw = when (edge.stroke) {
            StrokeType.THICK -> EDGE_STROKE_WIDTH * 2.5f * scale
            else -> EDGE_STROKE_WIDTH * scale
        }
        val pathEffect = if (edge.stroke == StrokeType.DOTTED) {
            androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f * scale, 4f * scale))
        } else null

        // 绘制多段折线，使用圆角拐角（匹配 mermaid-js curve='rounded', radius=5）
        if (drawPoints.size <= 2) {
            // 直线
            if (pathEffect != null) {
                drawLine(lineColor, drawPoints[0], drawPoints[1], strokeWidth = sw, pathEffect = pathEffect)
            } else {
                drawLine(lineColor, drawPoints[0], drawPoints[1], strokeWidth = sw)
            }
        } else {
            // 带圆角的折线路径
            val cornerRadius = 5f * scale
            val path = Path()
            path.moveTo(drawPoints[0].x, drawPoints[0].y)
            for (i in 1 until drawPoints.size - 1) {
                val prev = drawPoints[i - 1]
                val curr = drawPoints[i]
                val next = drawPoints[i + 1]
                // 从 prev→curr 方向计算拐角裁剪
                val dx1 = curr.x - prev.x; val dy1 = curr.y - prev.y
                val len1 = kotlin.math.sqrt(dx1 * dx1 + dy1 * dy1)
                val dx2 = next.x - curr.x; val dy2 = next.y - curr.y
                val len2 = kotlin.math.sqrt(dx2 * dx2 + dy2 * dy2)
                val r = minOf(cornerRadius, len1 / 2f, len2 / 2f)
                if (r > 0.5f && len1 > 0.001f && len2 > 0.001f) {
                    // 拐角前的点
                    val bx = curr.x - dx1 / len1 * r
                    val by = curr.y - dy1 / len1 * r
                    // 拐角后的点
                    val ax = curr.x + dx2 / len2 * r
                    val ay = curr.y + dy2 / len2 * r
                    path.lineTo(bx, by)
                    path.quadraticBezierTo(curr.x, curr.y, ax, ay)
                } else {
                    path.lineTo(curr.x, curr.y)
                }
            }
            path.lineTo(drawPoints.last().x, drawPoints.last().y)
            if (pathEffect != null) {
                drawPath(path, lineColor, style = Stroke(width = sw, pathEffect = pathEffect))
            } else {
                drawPath(path, lineColor, style = Stroke(width = sw))
            }
        }

        // 箭头
        if (edge.arrowTypeEnd != EdgeType.ARROW_NONE && drawPoints.size >= 2) {
            val tip = drawPoints.last()
            val from = drawPoints[drawPoints.size - 2]
            drawArrowHead(tip, from, tip, ARROW_HEAD_SIZE * scale, lineColor)
        }
        if (edge.arrowTypeStart != EdgeType.ARROW_NONE && drawPoints.size >= 2) {
            val tip = drawPoints.first()
            val from = drawPoints[1]
            drawArrowHead(tip, from, tip, ARROW_HEAD_SIZE * scale, lineColor)
        }

        // 标签 — 居中在路径上
        if (edge.label.isNotEmpty()) {
            val labelStyle = TextStyle(
                fontSize = (EDGE_LABEL_FONT_SIZE * scale).sp,
                color = theme.textColor.toComposeColor(),
            )
            val labelResult = textMeasurer.measure(edge.label, labelStyle)

            // 在路径总长度的中点位置放置标签
            val (lx, ly) = findPathMidpoint(drawPoints)

            val padH = EDGE_LABEL_PADDING * scale
            val padV = EDGE_LABEL_PADDING * 0.5f * scale
            val bgRect = Size(
                labelResult.size.width + padH * 2,
                labelResult.size.height + padV * 2,
            )
            val bgOffset = Offset(
                lx - bgRect.width / 2f,
                ly - bgRect.height / 2f,
            )
            // mermaid-js: .edgeLabel rect { opacity: 0.5 }
            val labelBgColor = theme.edgeLabelBackground.toComposeColor().copy(alpha = 0.5f)
            drawRoundRect(
                labelBgColor,
                bgOffset,
                bgRect,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * scale),
            )
            drawText(
                labelResult,
                topLeft = Offset(lx - labelResult.size.width / 2f, ly - labelResult.size.height / 2f),
            )
        }
    }

    /**
     * 在折线路径的总长度中点位置找到坐标。
     */
    private fun findPathMidpoint(points: List<Offset>): Pair<Float, Float> {
        if (points.size < 2) return (points.firstOrNull()?.x ?: 0f) to (points.firstOrNull()?.y ?: 0f)

        // 计算总路径长度
        var totalLen = 0f
        for (i in 0 until points.size - 1) {
            val dx = points[i + 1].x - points[i].x
            val dy = points[i + 1].y - points[i].y
            totalLen += kotlin.math.sqrt(dx * dx + dy * dy)
        }

        // 在总长度的中点插值
        val halfLen = totalLen / 2f
        var accumulated = 0f
        for (i in 0 until points.size - 1) {
            val dx = points[i + 1].x - points[i].x
            val dy = points[i + 1].y - points[i].y
            val segLen = kotlin.math.sqrt(dx * dx + dy * dy)
            if (accumulated + segLen >= halfLen && segLen > 0.001f) {
                val t = (halfLen - accumulated) / segLen
                return (points[i].x + dx * t) to (points[i].y + dy * t)
            }
            accumulated += segLen
        }
        // fallback
        val mid = points[points.size / 2]
        return mid.x to mid.y
    }

    /**
     * 根据形状计算边线从节点边界出发的交点。
     * 菱形使用菱形边界计算而非矩形。
     */
    private fun edgePointForShape(
        center: Offset, target: Offset,
        halfW: Float, halfH: Float,
        shape: ShapeId,
    ): Offset {
        val dx = target.x - center.x
        val dy = target.y - center.y
        if (dx == 0f && dy == 0f) return center

        return when (shape) {
            ShapeId.DIAMOND -> {
                // 菱形边界：|x/halfW| + |y/halfH| = 1
                val adx = kotlin.math.abs(dx)
                val ady = kotlin.math.abs(dy)
                if (adx < 0.001f && ady < 0.001f) return center
                val t = 1f / (adx / halfW + ady / halfH)
                Offset(center.x + dx * t, center.y + dy * t)
            }
            ShapeId.CIRCLE, ShapeId.DOUBLE_CIRCLE -> {
                val r = max(halfW, halfH)
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist < 0.001f) return center
                Offset(center.x + dx * r / dist, center.y + dy * r / dist)
            }
            else -> {
                // 矩形边界
                val angle = atan2(dy, dx)
                val tanA = kotlin.math.tan(angle)
                val xB = if (dx > 0) halfW else -halfW
                val yFx = xB * tanA
                if (kotlin.math.abs(yFx) <= halfH) {
                    Offset(center.x + xB, center.y + yFx)
                } else {
                    val yB = if (dy > 0) halfH else -halfH
                    Offset(center.x + yB / tanA, center.y + yB)
                }
            }
        }
    }

    /**
     * 绘制实心箭头。mermaid-js 默认使用实心三角形箭头。
     */
    private fun DrawScope.drawArrowHead(
        tip: Offset, from: Offset, to: Offset, sz: Float, color: Color,
    ) {
        val a = atan2((to.y - from.y).toDouble(), (to.x - from.x).toDouble())
        val halfAngle = PI / 7.0 // ~25.7° — mermaid-js 箭头角度
        val l = Offset(
            (tip.x - sz * cos(a - halfAngle)).toFloat(),
            (tip.y - sz * sin(a - halfAngle)).toFloat(),
        )
        val r = Offset(
            (tip.x - sz * cos(a + halfAngle)).toFloat(),
            (tip.y - sz * sin(a + halfAngle)).toFloat(),
        )
        drawPath(
            Path().apply { moveTo(tip.x, tip.y); lineTo(l.x, l.y); lineTo(r.x, r.y); close() },
            color,
            style = Fill,
        )
    }

    /**
     * 去掉 Font Awesome 图标前缀 (fa:fa-xxx)。
     * mermaid-js 会渲染图标，KMP 中暂时去掉前缀只显示文本。
     */
    private fun stripFontAwesome(text: String): String {
        // 处理 "fa:fa-car Car" → "Car", "fab:fa-twitter Hello" → "Hello"
        return text.replace(Regex("""fa[bsr]?:fa-[\w-]+\s*"""), "").trim().ifEmpty { text }
    }
}
