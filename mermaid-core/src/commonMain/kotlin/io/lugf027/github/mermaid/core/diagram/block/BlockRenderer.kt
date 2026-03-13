package io.lugf027.github.mermaid.core.diagram.block

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.rendering.svg.buildSvg
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.TextUtils
import kotlin.math.max
import kotlin.math.min

/**
 * 块图渲染器 - 对标 mermaid-js renderHelpers.ts + layout.ts
 *
 * 使用 grid/table 布局（三遍渲染）：
 * 1. setBlockSizes - 计算每个块的尺寸
 * 2. layoutBlocks - 按网格布局分配位置
 * 3. findBounds - 计算总边界
 *
 * 复用 flowchart 的形状风格进行节点渲染。
 */
class BlockRenderer : DiagramRenderer {

    companion object {
        const val DEFAULT_PADDING = 8
        const val NODE_PADDING = 10
        const val MIN_BLOCK_WIDTH = 100.0
        const val MIN_BLOCK_HEIGHT = 50.0
        const val BLOCK_MARGIN = 10.0
        const val COMPOSITE_PADDING = 20.0
        const val EDGE_PADDING = 5.0
    }

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val blockDb = db as BlockDb
        val padding = config.block?.padding ?: DEFAULT_PADDING
        val root = blockDb.getRootBlock()
        val edges = blockDb.getEdges()
        val title = blockDb.getDiagramTitle()

        // 1. 计算每个块的尺寸
        setBlockSizes(root, blockDb, themeVariables)

        // 2. 布局
        layoutBlocks(root, 0.0, if (title.isNotEmpty()) 40.0 else 0.0, blockDb)

        // 3. 计算边界
        val bounds = findBounds(root)

        val totalW = bounds.maxX - bounds.minX + padding * 2
        val totalH = bounds.maxY - bounds.minY + padding * 2 + (if (title.isNotEmpty()) 40 else 0)

        return buildSvg {
            attr("id", diagramId)
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("xmlns:xlink", "http://www.w3.org/1999/xlink")

            // 标题
            if (title.isNotEmpty()) {
                text(title, totalW / 2, 20.0) {
                    addClass("block-title")
                    attr("text-anchor", "middle")
                    attr("font-size", "16")
                    attr("fill", themeVariables.textColor)
                    attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")
                }
            }

            // 绘制块
            group {
                addClass("block")
                drawBlocksRecursive(root, blockDb, themeVariables)
            }

            // 绘制边
            if (edges.isNotEmpty()) {
                group {
                    addClass("block-edges")
                    for (edge in edges) {
                        drawEdge(edge, blockDb, themeVariables)
                    }
                }
            }

            viewBox(
                bounds.minX - padding,
                bounds.minY - padding - (if (title.isNotEmpty()) 40 else 0),
                totalW,
                totalH
            )
            attr("width", "100%")
            attr("style", "max-width: ${totalW.toInt()}px;")
        }
    }

    // ===== Pass 1: 计算尺寸 =====

    private fun setBlockSizes(block: BlockDb.Block, db: BlockDb, tv: ThemeVariables) {
        when (block.type) {
            BlockDb.BlockType.SPACE -> {
                block.width = MIN_BLOCK_WIDTH
                block.height = MIN_BLOCK_HEIGHT
            }
            BlockDb.BlockType.BLOCK -> {
                val labelWidth = TextUtils.estimateTextWidth(block.label, 14.0) + NODE_PADDING * 2
                block.width = max(MIN_BLOCK_WIDTH, labelWidth)
                block.height = MIN_BLOCK_HEIGHT
            }
            BlockDb.BlockType.COMPOSITE -> {
                // 先计算子块尺寸
                for (child in block.children) {
                    setBlockSizes(child, db, tv)
                }
                // 复合块尺寸由子块布局决定，暂设最小值
                if (block.children.isEmpty()) {
                    block.width = MIN_BLOCK_WIDTH
                    block.height = MIN_BLOCK_HEIGHT
                }
            }
        }
    }

    // ===== Pass 2: 布局 =====

    private fun layoutBlocks(block: BlockDb.Block, startX: Double, startY: Double, db: BlockDb) {
        if (block.type != BlockDb.BlockType.COMPOSITE || block.children.isEmpty()) {
            block.x = startX
            block.y = startY
            return
        }

        val columns = if (block.columns > 0) block.columns else {
            // auto: 所有子块排一行
            block.children.size
        }

        // 计算列宽度：找出每列的最大宽度
        val colWidths = DoubleArray(columns) { MIN_BLOCK_WIDTH }
        val rows = mutableListOf<List<BlockDb.Block>>()
        var currentRow = mutableListOf<BlockDb.Block>()
        var colCount = 0

        for (child in block.children) {
            val span = child.widthInColumns.coerceIn(1, columns)
            if (colCount + span > columns && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                colCount = 0
            }
            currentRow.add(child)
            colCount += span
        }
        if (currentRow.isNotEmpty()) rows.add(currentRow)

        // 计算列宽
        for (row in rows) {
            var col = 0
            for (child in row) {
                val span = child.widthInColumns.coerceIn(1, columns)
                val widthPerCol = child.width / span
                for (c in col until (col + span).coerceAtMost(columns)) {
                    colWidths[c] = max(colWidths[c], widthPerCol)
                }
                col += span
            }
        }

        // 布局每行
        val offsetX = startX + (if (block.id == "root") 0.0 else COMPOSITE_PADDING)
        var currentY = startY + (if (block.id == "root") 0.0 else COMPOSITE_PADDING + 20) // 20 for label

        for (row in rows) {
            var currentX = offsetX
            var rowMaxH = 0.0
            var col = 0

            for (child in row) {
                val span = child.widthInColumns.coerceIn(1, columns)
                // 计算此块的总宽度
                var totalWidth = 0.0
                for (c in col until (col + span).coerceAtMost(columns)) {
                    totalWidth += colWidths[c]
                }
                totalWidth += (span - 1) * BLOCK_MARGIN

                child.width = totalWidth
                child.x = currentX
                child.y = currentY

                if (child.type == BlockDb.BlockType.COMPOSITE) {
                    layoutBlocks(child, currentX, currentY, db)
                    // 更新复合块的尺寸
                    val childBounds = findBounds(child)
                    child.width = max(child.width, childBounds.maxX - childBounds.minX + COMPOSITE_PADDING * 2)
                    child.height = max(child.height, childBounds.maxY - childBounds.minY + COMPOSITE_PADDING * 2 + 20)
                }

                currentX += totalWidth + BLOCK_MARGIN
                rowMaxH = max(rowMaxH, child.height)
                col += span
            }

            // 统一行高
            for (child in row) {
                child.height = max(child.height, rowMaxH)
            }

            currentY += rowMaxH + BLOCK_MARGIN
        }

        // 更新复合块尺寸
        if (block.id != "root") {
            val totalW = colWidths.sum() + (columns - 1) * BLOCK_MARGIN + COMPOSITE_PADDING * 2
            val totalH = currentY - startY + COMPOSITE_PADDING
            block.width = max(block.width, totalW)
            block.height = max(block.height, totalH)
        }
    }

    // ===== Pass 3: 边界计算 =====

    private data class Bounds(val minX: Double, val minY: Double, val maxX: Double, val maxY: Double)

    private fun findBounds(block: BlockDb.Block): Bounds {
        if (block.children.isEmpty()) {
            return Bounds(block.x, block.y, block.x + block.width, block.y + block.height)
        }

        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = Double.MIN_VALUE
        var maxY = Double.MIN_VALUE

        for (child in block.children) {
            val cb = findBounds(child)
            minX = min(minX, cb.minX)
            minY = min(minY, cb.minY)
            maxX = max(maxX, cb.maxX)
            maxY = max(maxY, cb.maxY)
        }

        return Bounds(minX, minY, maxX, maxY)
    }

    // ===== 绘制 =====

    private fun SvgElement.drawBlocksRecursive(block: BlockDb.Block, db: BlockDb, tv: ThemeVariables) {
        for (child in block.children) {
            when (child.type) {
                BlockDb.BlockType.SPACE -> {
                    // 不绘制空白占位
                }
                BlockDb.BlockType.BLOCK -> {
                    drawSingleBlock(child, tv)
                }
                BlockDb.BlockType.COMPOSITE -> {
                    drawCompositeBlock(child, db, tv)
                }
            }
        }
    }

    private fun SvgElement.drawSingleBlock(block: BlockDb.Block, tv: ThemeVariables) {
        val fill = block.styles["fill"] ?: tv.primaryColor
        val stroke = block.styles["stroke"] ?: tv.primaryBorderColor
        val textColor = block.styles["color"] ?: tv.primaryTextColor

        group {
            addClass("node")
            attr("id", "node-${block.id}")

            when (block.shape) {
                "stadium" -> {
                    val r = block.height / 2
                    rect(block.x, block.y, block.width, block.height) {
                        attr("fill", fill)
                        attr("stroke", stroke)
                        attr("stroke-width", "1")
                        attr("rx", "${r}")
                        attr("ry", "${r}")
                    }
                }
                "circle" -> {
                    val r = min(block.width, block.height) / 2
                    val cx = block.x + block.width / 2
                    val cy = block.y + block.height / 2
                    circle(cx, cy, r) {
                        attr("fill", fill)
                        attr("stroke", stroke)
                        attr("stroke-width", "1")
                    }
                }
                "diamond" -> {
                    val cx = block.x + block.width / 2
                    val cy = block.y + block.height / 2
                    val hw = block.width / 2
                    val hh = block.height / 2
                    polygon(listOf(
                        Pair(cx, block.y),
                        Pair(block.x + block.width, cy),
                        Pair(cx, block.y + block.height),
                        Pair(block.x, cy)
                    )) {
                        attr("fill", fill)
                        attr("stroke", stroke)
                        attr("stroke-width", "1")
                    }
                }
                "hexagon" -> {
                    val cx = block.x + block.width / 2
                    val offset = block.height / 4
                    polygon(listOf(
                        Pair(block.x + offset, block.y),
                        Pair(block.x + block.width - offset, block.y),
                        Pair(block.x + block.width, block.y + block.height / 2),
                        Pair(block.x + block.width - offset, block.y + block.height),
                        Pair(block.x + offset, block.y + block.height),
                        Pair(block.x, block.y + block.height / 2)
                    )) {
                        attr("fill", fill)
                        attr("stroke", stroke)
                        attr("stroke-width", "1")
                    }
                }
                "subroutine" -> {
                    // 子过程 - 双边线矩形
                    rect(block.x, block.y, block.width, block.height) {
                        attr("fill", fill)
                        attr("stroke", stroke)
                        attr("stroke-width", "1")
                    }
                    line(block.x + 8, block.y, block.x + 8, block.y + block.height) {
                        attr("stroke", stroke)
                        attr("stroke-width", "1")
                    }
                    line(block.x + block.width - 8, block.y, block.x + block.width - 8, block.y + block.height) {
                        attr("stroke", stroke)
                        attr("stroke-width", "1")
                    }
                }
                else -> {
                    // 默认矩形
                    rect(block.x, block.y, block.width, block.height) {
                        attr("fill", fill)
                        attr("stroke", stroke)
                        attr("stroke-width", "1")
                        attr("rx", "5")
                        attr("ry", "5")
                    }
                }
            }

            // 文本标签
            text(block.label, block.x + block.width / 2, block.y + block.height / 2 + 5) {
                attr("fill", textColor)
                attr("text-anchor", "middle")
                attr("dominant-baseline", "middle")
                attr("font-size", "14")
                attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")
            }
        }
    }

    private fun SvgElement.drawCompositeBlock(block: BlockDb.Block, db: BlockDb, tv: ThemeVariables) {
        group {
            addClass("node composite")
            attr("id", "node-${block.id}")

            // 外框
            rect(block.x, block.y, block.width, block.height) {
                attr("fill", tv.mainBkg)
                attr("stroke", tv.primaryBorderColor)
                attr("stroke-width", "1")
                attr("rx", "5")
                attr("ry", "5")
            }

            // 标签
            text(block.label, block.x + COMPOSITE_PADDING, block.y + 16.0) {
                attr("fill", tv.textColor)
                attr("font-size", "14")
                attr("font-weight", "bold")
                attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")
            }

            // 递归绘制子块
            drawBlocksRecursive(block, db, tv)
        }
    }

    private fun SvgElement.drawEdge(edge: BlockDb.Edge, db: BlockDb, tv: ThemeVariables) {
        val source = db.getBlock(edge.source) ?: return
        val target = db.getBlock(edge.target) ?: return

        val sx = source.x + source.width / 2
        val sy = source.y + source.height / 2
        val tx = target.x + target.width / 2
        val ty = target.y + target.height / 2

        // 计算连接点（简化：中心到中心）
        val startPoint = calculateEdgePoint(source, tx, ty)
        val endPoint = calculateEdgePoint(target, sx, sy)

        val strokeStyle = when (edge.lineType) {
            "dotted" -> "stroke-dasharray: 3;"
            "thick" -> "stroke-width: 3.5;"
            else -> "stroke-width: 2;"
        }

        group {
            // 线
            path("M ${startPoint.first} ${startPoint.second} L ${endPoint.first} ${endPoint.second}") {
                addClass("edge-path")
                attr("stroke", tv.lineColor)
                attr("fill", "none")
                attr("style", strokeStyle)
            }

            // 箭头
            if (edge.arrowTypeEnd == "arrow_point") {
                val angle = kotlin.math.atan2(endPoint.second - startPoint.second, endPoint.first - startPoint.first)
                val arrowSize = 8.0
                val ax1 = endPoint.first - arrowSize * kotlin.math.cos(angle - 0.4)
                val ay1 = endPoint.second - arrowSize * kotlin.math.sin(angle - 0.4)
                val ax2 = endPoint.first - arrowSize * kotlin.math.cos(angle + 0.4)
                val ay2 = endPoint.second - arrowSize * kotlin.math.sin(angle + 0.4)
                polygon(listOf(
                    Pair(endPoint.first, endPoint.second),
                    Pair(ax1, ay1),
                    Pair(ax2, ay2)
                )) {
                    attr("fill", tv.lineColor)
                }
            }

            // 标签
            if (edge.label.isNotEmpty()) {
                val labelX = (startPoint.first + endPoint.first) / 2
                val labelY = (startPoint.second + endPoint.second) / 2 - 5
                text(edge.label, labelX, labelY) {
                    addClass("edge-label")
                    attr("fill", tv.textColor)
                    attr("text-anchor", "middle")
                    attr("font-size", "12")
                    attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")
                }
            }
        }
    }

    /**
     * 计算边从块边界出发的点
     */
    private fun calculateEdgePoint(block: BlockDb.Block, targetX: Double, targetY: Double): Pair<Double, Double> {
        val cx = block.x + block.width / 2
        val cy = block.y + block.height / 2
        val hw = block.width / 2
        val hh = block.height / 2

        val dx = targetX - cx
        val dy = targetY - cy

        if (dx == 0.0 && dy == 0.0) return Pair(cx, cy)

        // 简化：矩形边界交点
        val absDx = kotlin.math.abs(dx)
        val absDy = kotlin.math.abs(dy)

        return if (absDx / hw > absDy / hh) {
            // 从左或右边出
            val signX = if (dx > 0) 1.0 else -1.0
            Pair(cx + signX * hw, cy + dy * hw / absDx)
        } else {
            // 从上或下边出
            val signY = if (dy > 0) 1.0 else -1.0
            Pair(cx + dx * hh / absDy, cy + signY * hh)
        }
    }
}
