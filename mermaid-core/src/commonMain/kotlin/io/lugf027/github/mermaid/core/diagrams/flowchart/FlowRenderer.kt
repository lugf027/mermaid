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
        private const val DEFAULT_NODE_PADDING = 15f
        private const val DEFAULT_FONT_SIZE = 14f
        private const val ARROW_HEAD_SIZE = 8f
        private const val EDGE_LABEL_FONT_SIZE = 12f
        private const val TITLE_FONT_SIZE = 20f
        private const val STROKE_WIDTH = 2f
        private const val MARGIN = 20f
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
            val measured = textMeasurer.measure(node.label, textStyle)
            node.copy(
                width = measured.size.width.toFloat() + DEFAULT_NODE_PADDING * 2,
                height = measured.size.height.toFloat() + DEFAULT_NODE_PADDING * 2,
            )
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

                val srcCenter = Offset(srcNode.x * scale + offsetX, srcNode.y * scale + offsetY)
                val tgtCenter = Offset(tgtNode.x * scale + offsetX, tgtNode.y * scale + offsetY)
                val srcHalfW = srcNode.width * scale / 2f
                val srcHalfH = srcNode.height * scale / 2f
                val tgtHalfW = tgtNode.width * scale / 2f
                val tgtHalfH = tgtNode.height * scale / 2f

                drawFlowEdge(
                    srcCenter, tgtCenter,
                    srcHalfW, srcHalfH, tgtHalfW, tgtHalfH,
                    edge, theme, textMeasurer, scale,
                )
            }

            // 6. 绘制节点
            val nodeTextStyle = TextStyle(
                fontSize = (DEFAULT_FONT_SIZE * scale).sp,
                color = theme.primaryTextColor.toComposeColor(),
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
        val bgColor = theme.primaryColor.toComposeColor()
        val borderColor = theme.nodeBorder.toComposeColor()
        val left = center.x - width / 2f
        val top = center.y - height / 2f

        when (node.shape) {
            ShapeId.ROUNDED_RECT -> {
                val cr = androidx.compose.ui.geometry.CornerRadius(5f * scale)
                drawRoundRect(bgColor, Offset(left, top), Size(width, height), cr)
                drawRoundRect(borderColor, Offset(left, top), Size(width, height), cr, style = Stroke(STROKE_WIDTH * scale))
            }
            ShapeId.SQUARE_RECT, ShapeId.RECT -> {
                drawRect(bgColor, Offset(left, top), Size(width, height))
                drawRect(borderColor, Offset(left, top), Size(width, height), style = Stroke(STROKE_WIDTH * scale))
            }
            ShapeId.CIRCLE, ShapeId.DOUBLE_CIRCLE -> {
                val r = max(width, height) / 2f
                drawCircle(bgColor, r, center)
                drawCircle(borderColor, r, center, style = Stroke(STROKE_WIDTH * scale))
                if (node.shape == ShapeId.DOUBLE_CIRCLE) {
                    drawCircle(borderColor, r - 4f * scale, center, style = Stroke(STROKE_WIDTH * scale))
                }
            }
            ShapeId.DIAMOND -> {
                val path = Path().apply {
                    moveTo(center.x, top); lineTo(left + width, center.y)
                    lineTo(center.x, top + height); lineTo(left, center.y); close()
                }
                drawPath(path, bgColor); drawPath(path, borderColor, style = Stroke(STROKE_WIDTH * scale))
            }
            ShapeId.HEXAGON -> {
                val dx = width * 0.15f
                val path = Path().apply {
                    moveTo(left + dx, top); lineTo(left + width - dx, top)
                    lineTo(left + width, center.y); lineTo(left + width - dx, top + height)
                    lineTo(left + dx, top + height); lineTo(left, center.y); close()
                }
                drawPath(path, bgColor); drawPath(path, borderColor, style = Stroke(STROKE_WIDTH * scale))
            }
            ShapeId.STADIUM -> {
                val r = height / 2f
                val cr = androidx.compose.ui.geometry.CornerRadius(r)
                drawRoundRect(bgColor, Offset(left, top), Size(width, height), cr)
                drawRoundRect(borderColor, Offset(left, top), Size(width, height), cr, style = Stroke(STROKE_WIDTH * scale))
            }
            ShapeId.CYLINDER -> {
                val eH = height * 0.15f
                drawRect(bgColor, Offset(left, top + eH / 2f), Size(width, height - eH))
                drawOval(bgColor, Offset(left, top), Size(width, eH))
                drawOval(borderColor, Offset(left, top), Size(width, eH), style = Stroke(STROKE_WIDTH * scale))
                drawOval(bgColor, Offset(left, top + height - eH), Size(width, eH))
                drawOval(borderColor, Offset(left, top + height - eH), Size(width, eH), style = Stroke(STROKE_WIDTH * scale))
                drawLine(borderColor, Offset(left, top + eH / 2f), Offset(left, top + height - eH / 2f), STROKE_WIDTH * scale)
                drawLine(borderColor, Offset(left + width, top + eH / 2f), Offset(left + width, top + height - eH / 2f), STROKE_WIDTH * scale)
            }
            ShapeId.SUBROUTINE -> {
                val inset = 8f * scale
                drawRect(bgColor, Offset(left, top), Size(width, height))
                drawRect(borderColor, Offset(left, top), Size(width, height), style = Stroke(STROKE_WIDTH * scale))
                drawLine(borderColor, Offset(left + inset, top), Offset(left + inset, top + height), STROKE_WIDTH * scale)
                drawLine(borderColor, Offset(left + width - inset, top), Offset(left + width - inset, top + height), STROKE_WIDTH * scale)
            }
            ShapeId.TRAPEZOID -> {
                val dx = width * 0.15f
                val path = Path().apply {
                    moveTo(left + dx, top); lineTo(left + width - dx, top)
                    lineTo(left + width, top + height); lineTo(left, top + height); close()
                }
                drawPath(path, bgColor); drawPath(path, borderColor, style = Stroke(STROKE_WIDTH * scale))
            }
            ShapeId.INV_TRAPEZOID -> {
                val dx = width * 0.15f
                val path = Path().apply {
                    moveTo(left, top); lineTo(left + width, top)
                    lineTo(left + width - dx, top + height); lineTo(left + dx, top + height); close()
                }
                drawPath(path, bgColor); drawPath(path, borderColor, style = Stroke(STROKE_WIDTH * scale))
            }
            ShapeId.LEAN_RIGHT -> {
                val dx = width * 0.15f
                val path = Path().apply {
                    moveTo(left + dx, top); lineTo(left + width, top)
                    lineTo(left + width - dx, top + height); lineTo(left, top + height); close()
                }
                drawPath(path, bgColor); drawPath(path, borderColor, style = Stroke(STROKE_WIDTH * scale))
            }
            ShapeId.LEAN_LEFT -> {
                val dx = width * 0.15f
                val path = Path().apply {
                    moveTo(left, top); lineTo(left + width - dx, top)
                    lineTo(left + width, top + height); lineTo(left + dx, top + height); close()
                }
                drawPath(path, bgColor); drawPath(path, borderColor, style = Stroke(STROKE_WIDTH * scale))
            }
            ShapeId.ODD -> {
                val dx = width * 0.12f
                val path = Path().apply {
                    moveTo(left + dx, top); lineTo(left + width, top)
                    lineTo(left + width, top + height); lineTo(left + dx, top + height)
                    lineTo(left, center.y); close()
                }
                drawPath(path, bgColor); drawPath(path, borderColor, style = Stroke(STROKE_WIDTH * scale))
            }
            ShapeId.ELLIPSE -> {
                drawOval(bgColor, Offset(left, top), Size(width, height))
                drawOval(borderColor, Offset(left, top), Size(width, height), style = Stroke(STROKE_WIDTH * scale))
            }
            else -> {
                drawRect(bgColor, Offset(left, top), Size(width, height))
                drawRect(borderColor, Offset(left, top), Size(width, height), style = Stroke(STROKE_WIDTH * scale))
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
        srcCenter: Offset, tgtCenter: Offset,
        srcHalfW: Float, srcHalfH: Float,
        tgtHalfW: Float, tgtHalfH: Float,
        edge: Edge,
        theme: ThemeVariables,
        textMeasurer: TextMeasurer,
        scale: Float,
    ) {
        val lineColor = theme.lineColor.toComposeColor()
        val srcEdge = edgePoint(srcCenter, tgtCenter, srcHalfW, srcHalfH)
        val tgtEdge = edgePoint(tgtCenter, srcCenter, tgtHalfW, tgtHalfH)

        // 线条
        val sw = when (edge.stroke) {
            StrokeType.THICK -> STROKE_WIDTH * 2f * scale
            else -> STROKE_WIDTH * scale
        }
        if (edge.stroke == StrokeType.DOTTED) {
            val pe = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f * scale, 4f * scale))
            drawLine(lineColor, srcEdge, tgtEdge, strokeWidth = sw, pathEffect = pe)
        } else {
            drawLine(lineColor, srcEdge, tgtEdge, strokeWidth = sw)
        }

        // 箭头
        if (edge.arrowTypeEnd != EdgeType.ARROW_NONE) {
            drawArrow(tgtEdge, srcEdge, tgtEdge, ARROW_HEAD_SIZE * scale, lineColor)
        }
        if (edge.arrowTypeStart != EdgeType.ARROW_NONE) {
            drawArrow(srcEdge, tgtEdge, srcEdge, ARROW_HEAD_SIZE * scale, lineColor)
        }

        // 标签
        if (edge.label.isNotEmpty()) {
            val labelStyle = TextStyle(fontSize = (EDGE_LABEL_FONT_SIZE * scale).sp, color = theme.textColor.toComposeColor())
            val labelResult = textMeasurer.measure(edge.label, labelStyle)
            val lx = (srcEdge.x + tgtEdge.x) / 2f
            val ly = (srcEdge.y + tgtEdge.y) / 2f
            drawRect(theme.edgeLabelBackground.toComposeColor(),
                Offset(lx - labelResult.size.width / 2f - 2f * scale, ly - labelResult.size.height / 2f - 1f * scale),
                Size(labelResult.size.width + 4f * scale, labelResult.size.height + 2f * scale))
            drawText(labelResult, topLeft = Offset(lx - labelResult.size.width / 2f, ly - labelResult.size.height / 2f))
        }
    }

    private fun edgePoint(center: Offset, target: Offset, halfW: Float, halfH: Float): Offset {
        val dx = target.x - center.x
        val dy = target.y - center.y
        if (dx == 0f && dy == 0f) return center
        val angle = atan2(dy, dx)
        val tanA = kotlin.math.tan(angle)
        val xB = if (dx > 0) halfW else -halfW
        val yFx = xB * tanA
        return if (kotlin.math.abs(yFx) <= halfH) {
            Offset(center.x + xB, center.y + yFx)
        } else {
            val yB = if (dy > 0) halfH else -halfH
            Offset(center.x + yB / tanA, center.y + yB)
        }
    }

    private fun DrawScope.drawArrow(tip: Offset, from: Offset, to: Offset, sz: Float, color: Color) {
        val a = atan2((to.y - from.y).toDouble(), (to.x - from.x).toDouble())
        val l = Offset((tip.x - sz * cos(a - PI / 6)).toFloat(), (tip.y - sz * sin(a - PI / 6)).toFloat())
        val r = Offset((tip.x - sz * cos(a + PI / 6)).toFloat(), (tip.y - sz * sin(a + PI / 6)).toFloat())
        drawPath(Path().apply { moveTo(tip.x, tip.y); lineTo(l.x, l.y); lineTo(r.x, r.y); close() }, color, style = Fill)
    }
}
