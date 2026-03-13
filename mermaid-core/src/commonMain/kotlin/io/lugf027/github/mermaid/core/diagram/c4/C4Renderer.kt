package io.lugf027.github.mermaid.core.diagram.c4

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.rendering.svg.buildSvg
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.TextUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * C4 图渲染器 - 对标 mermaid-js c4Renderer.js + svgDraw.js
 *
 * 自定义渲染模式：Bounds 类管理布局，递归绘制边界和内部元素。
 */
class C4Renderer : DiagramRenderer {

    companion object {
        // 默认配置
        const val DIAGRAM_MARGIN_X = 50
        const val DIAGRAM_MARGIN_Y = 10
        const val C4_SHAPE_MARGIN = 50
        const val C4_SHAPE_PADDING = 20
        const val DEFAULT_WIDTH = 216.0
        const val DEFAULT_HEIGHT = 60.0
        const val FONT_SIZE = 14

        // C4 颜色映射
        val COLORS = mapOf(
            "person" to Pair("#08427B", "#073B6F"),
            "external_person" to Pair("#686868", "#8A8A8A"),
            "system" to Pair("#1168BD", "#3C7FC0"),
            "external_system" to Pair("#999999", "#8A8A8A"),
            "system_db" to Pair("#1168BD", "#3C7FC0"),
            "external_system_db" to Pair("#999999", "#8A8A8A"),
            "system_queue" to Pair("#1168BD", "#3C7FC0"),
            "external_system_queue" to Pair("#999999", "#8A8A8A"),
            "container" to Pair("#438DD5", "#3C7FC0"),
            "external_container" to Pair("#B3B3B3", "#A6A6A6"),
            "container_db" to Pair("#438DD5", "#3C7FC0"),
            "external_container_db" to Pair("#B3B3B3", "#A6A6A6"),
            "container_queue" to Pair("#438DD5", "#3C7FC0"),
            "external_container_queue" to Pair("#B3B3B3", "#A6A6A6"),
            "component" to Pair("#85BBF0", "#78A8D8"),
            "external_component" to Pair("#CCCCCC", "#BFBFBF"),
            "component_db" to Pair("#85BBF0", "#78A8D8"),
            "external_component_db" to Pair("#CCCCCC", "#BFBFBF"),
            "component_queue" to Pair("#85BBF0", "#78A8D8"),
            "external_component_queue" to Pair("#CCCCCC", "#BFBFBF"),
        )
    }

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val c4Db = db as C4Db
        val margin = C4_SHAPE_MARGIN
        val padding = C4_SHAPE_PADDING
        val shapeInRow = c4Db.getC4ShapeInRow()
        val boundaryInRow = c4Db.getC4BoundaryInRow()
        val title = c4Db.getDiagramTitle()

        // 计算所有元素的尺寸
        val allShapes = c4Db.getC4ShapeArray()
        for (shape in allShapes) {
            val labelW = TextUtils.estimateTextWidth(shape.label, (FONT_SIZE + 2).toDouble())
            val descrW = if (shape.descr.isNotEmpty()) TextUtils.estimateTextWidth(shape.descr, FONT_SIZE.toDouble()) else 0.0
            val technW = if (shape.techn.isNotEmpty()) TextUtils.estimateTextWidth("[${shape.techn}]", FONT_SIZE.toDouble()) else 0.0
            shape.width = max(DEFAULT_WIDTH, max(labelW, max(descrW, technW)) + 2 * padding)

            var h = padding.toDouble()
            h += FONT_SIZE + 2 // type label
            if (shape.typeC4Shape.contains("person")) h += 48 // person icon
            h += FONT_SIZE + 4 // label
            if (shape.techn.isNotEmpty()) h += FONT_SIZE + 2
            if (shape.descr.isNotEmpty()) h += FONT_SIZE + 8
            h += padding
            shape.height = max(DEFAULT_HEIGHT, h)
        }

        // 布局元素
        var nextX = DIAGRAM_MARGIN_X.toDouble()
        var nextY = DIAGRAM_MARGIN_Y.toDouble() + (if (title.isNotEmpty()) 40.0 else 0.0)
        var rowMaxH = 0.0
        var itemInRow = 0

        // 简化布局：按 parentBoundary = "global" 的元素排成行
        val globalShapes = c4Db.getC4ShapeArray("global")
        val globalBoundaries = c4Db.getBoundaries("global")

        for (shape in globalShapes) {
            shape.x = nextX
            shape.y = nextY
            nextX += shape.width + margin
            rowMaxH = max(rowMaxH, shape.height)
            itemInRow++
            if (itemInRow >= shapeInRow) {
                nextX = DIAGRAM_MARGIN_X.toDouble()
                nextY += rowMaxH + margin
                rowMaxH = 0.0
                itemInRow = 0
            }
        }
        if (itemInRow > 0) {
            nextY += rowMaxH + margin
        }

        // 处理边界及其子元素
        var boundaryIdx = 0
        for (boundary in globalBoundaries) {
            if (boundary.alias == "global") continue
            val childShapes = c4Db.getC4ShapeArray(boundary.alias)

            var bx = DIAGRAM_MARGIN_X.toDouble() + (if (boundaryIdx % boundaryInRow > 0) 400.0 * (boundaryIdx % boundaryInRow) else 0.0)
            var by = nextY + 30
            var bw = 0.0
            var bh = 30.0

            var cx = bx + margin
            var cy = by + 30
            var cRowMaxH = 0.0
            var cItemInRow = 0

            for (shape in childShapes) {
                shape.x = cx
                shape.y = cy
                cx += shape.width + margin
                cRowMaxH = max(cRowMaxH, shape.height)
                cItemInRow++
                if (cItemInRow >= shapeInRow) {
                    cx = bx + margin
                    cy += cRowMaxH + margin
                    cRowMaxH = 0.0
                    cItemInRow = 0
                }
            }
            if (cItemInRow > 0) cy += cRowMaxH + margin / 2

            bw = max(childShapes.sumOf { it.width + margin } + margin, 200.0)
            bh = cy - by + margin
            boundary.x = bx
            boundary.y = by
            boundary.width = bw
            boundary.height = bh

            boundaryIdx++
            if (boundaryIdx % boundaryInRow == 0) {
                nextY += bh + margin
            }
        }
        if (boundaryIdx % boundaryInRow != 0) {
            val maxBH = globalBoundaries.filter { it.alias != "global" }.maxOfOrNull { it.height } ?: 0.0
            nextY += maxBH + margin
        }

        // 计算总 viewBox
        val allX = allShapes.map { it.x } + globalBoundaries.filter { it.alias != "global" }.map { it.x }
        val allXR = allShapes.map { it.x + it.width } + globalBoundaries.filter { it.alias != "global" }.map { it.x + it.width }
        val allY = allShapes.map { it.y } + globalBoundaries.filter { it.alias != "global" }.map { it.y }
        val allYB = allShapes.map { it.y + it.height } + globalBoundaries.filter { it.alias != "global" }.map { it.y + it.height }
        val minX = (allX.minOrNull() ?: 0.0) - DIAGRAM_MARGIN_X
        val maxX = (allXR.maxOrNull() ?: DEFAULT_WIDTH) + DIAGRAM_MARGIN_X
        val minY = (allY.minOrNull() ?: 0.0) - DIAGRAM_MARGIN_Y - (if (title.isNotEmpty()) 40 else 0)
        val maxY = (allYB.maxOrNull() ?: DEFAULT_HEIGHT) + DIAGRAM_MARGIN_Y

        return buildSvg {
            attr("id", diagramId)
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("xmlns:xlink", "http://www.w3.org/1999/xlink")

            // Defs: 箭头标记
            defs {
                marker {
                    setup("arrowhead", "0 0 10 10", 9.0, 5.0, 12.0, 12.0)
                    path("M 0 0 L 10 5 L 0 10 z") {
                        attr("fill", "#444444")
                    }
                }
            }

            // 标题
            if (title.isNotEmpty()) {
                text(title, (maxX - minX) / 2, (minY + DIAGRAM_MARGIN_Y + 10).toDouble()) {
                    attr("text-anchor", "middle")
                    attr("font-size", "18")
                    attr("fill", themeVariables.textColor)
                    attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")
                }
            }

            // 绘制边界
            for (boundary in globalBoundaries) {
                if (boundary.alias == "global") continue
                drawBoundary(boundary, themeVariables)
            }

            // 绘制元素
            for (shape in allShapes) {
                drawC4Shape(shape, themeVariables)
            }

            // 绘制关系
            val rels = c4Db.getRels()
            for ((idx, rel) in rels.withIndex()) {
                val fromShape = c4Db.getC4Shape(rel.from) ?: continue
                val toShape = c4Db.getC4Shape(rel.to) ?: continue

                val startX = fromShape.x + fromShape.width / 2
                val startY = fromShape.y + fromShape.height / 2
                val endX = toShape.x + toShape.width / 2
                val endY = toShape.y + toShape.height / 2

                val strokeColor = rel.lineColor.ifEmpty { "#444444" }
                val textColor = rel.textColor.ifEmpty { "#444444" }

                // 线
                line(startX, startY, endX, endY) {
                    attr("stroke", strokeColor)
                    attr("stroke-width", "1")
                    attr("marker-end", "url(#arrowhead)")
                    if (rel.type == "birel" || rel.type == "rel_b") {
                        attr("marker-start", "url(#arrowend)")
                    }
                }

                // 关系标签
                if (rel.label.isNotEmpty()) {
                    val labelX = min(startX, endX) + abs(endX - startX) / 2 + rel.offsetX
                    val labelY = min(startY, endY) + abs(endY - startY) / 2 + rel.offsetY

                    val displayLabel = if (c4Db.getC4Type() == "C4Dynamic") "${idx + 1}: ${rel.label}" else rel.label
                    text(displayLabel, labelX, labelY - 5) {
                        attr("fill", textColor)
                        attr("font-size", "12")
                        attr("text-anchor", "middle")
                        attr("font-family", "'Open Sans', sans-serif")
                    }
                    if (rel.techn.isNotEmpty()) {
                        text("[${rel.techn}]", labelX, labelY + 10) {
                            attr("fill", textColor)
                            attr("font-size", "11")
                            attr("text-anchor", "middle")
                            attr("font-style", "italic")
                            attr("font-family", "'Open Sans', sans-serif")
                        }
                    }
                }
            }

            viewBox(minX, minY, maxX - minX, maxY - minY)
            attr("width", "100%")
            attr("style", "max-width: ${(maxX - minX).toInt()}px;")
        }
    }

    /**
     * 绘制 C4 形状（矩形/圆柱/队列）
     */
    private fun SvgElement.drawC4Shape(shape: C4Db.C4Shape, tv: ThemeVariables) {
        val colors = COLORS[shape.typeC4Shape] ?: Pair("#1168BD", "#3C7FC0")
        val fillColor = shape.bgColor.ifEmpty { colors.first }
        val strokeColor = shape.borderColor.ifEmpty { colors.second }
        val fontColor = shape.fontColor.ifEmpty { "#FFFFFF" }

        group {
            // 形状
            if (shape.typeC4Shape.endsWith("_db")) {
                // 圆柱形
                val half = shape.width / 2
                val pathData = "M${shape.x},${shape.y} " +
                        "c0,-10 $half,-10 $half,-10 " +
                        "c0,0 $half,0 $half,10 " +
                        "l0,${shape.height} " +
                        "c0,10 ${-half},10 ${-half},10 " +
                        "c0,0 ${-half},0 ${-half},-10 " +
                        "l0,${-shape.height}"
                path(pathData) {
                    attr("fill", fillColor)
                    attr("stroke", strokeColor)
                    attr("stroke-width", "0.5")
                }
            } else if (shape.typeC4Shape.endsWith("_queue")) {
                // 队列形状
                val halfH = shape.height / 2
                val pathData = "M${shape.x},${shape.y} " +
                        "l${shape.width},0 " +
                        "c5,0 5,$halfH 5,$halfH " +
                        "c0,$halfH -5,$halfH -5,$halfH " +
                        "l${-shape.width},0 " +
                        "c-5,0 -5,${-halfH} -5,${-halfH} " +
                        "c0,${-halfH} 5,${-halfH} 5,${-halfH}"
                path(pathData) {
                    attr("fill", fillColor)
                    attr("stroke", strokeColor)
                    attr("stroke-width", "0.5")
                }
            } else {
                // 矩形
                rect(shape.x, shape.y, shape.width, shape.height) {
                    attr("fill", fillColor)
                    attr("stroke", strokeColor)
                    attr("stroke-width", "0.5")
                    attr("rx", "2.5")
                    attr("ry", "2.5")
                }
            }

            // 文本内容
            var textY = shape.y + C4_SHAPE_PADDING

            // 类型标注
            val typeLabel = "«${shape.typeC4Shape.replace("_", " ")}»"
            text(typeLabel, shape.x + shape.width / 2, textY + FONT_SIZE - 2) {
                attr("fill", fontColor)
                attr("font-size", "${FONT_SIZE - 2}")
                attr("font-style", "italic")
                attr("text-anchor", "middle")
                attr("font-family", "'Open Sans', sans-serif")
            }
            textY += FONT_SIZE + 2

            // 标签（加粗）
            text(shape.label, shape.x + shape.width / 2, textY + FONT_SIZE + 2) {
                attr("fill", fontColor)
                attr("font-size", "${FONT_SIZE + 2}")
                attr("font-weight", "bold")
                attr("text-anchor", "middle")
                attr("font-family", "'Open Sans', sans-serif")
            }
            textY += FONT_SIZE + 6

            // 技术
            if (shape.techn.isNotEmpty()) {
                text("[${shape.techn}]", shape.x + shape.width / 2, textY + FONT_SIZE) {
                    attr("fill", fontColor)
                    attr("font-size", "$FONT_SIZE")
                    attr("font-style", "italic")
                    attr("text-anchor", "middle")
                    attr("font-family", "'Open Sans', sans-serif")
                }
                textY += FONT_SIZE + 4
            }

            // 描述
            if (shape.descr.isNotEmpty()) {
                text(shape.descr, shape.x + shape.width / 2, textY + FONT_SIZE) {
                    attr("fill", fontColor)
                    attr("font-size", "$FONT_SIZE")
                    attr("text-anchor", "middle")
                    attr("font-family", "'Open Sans', sans-serif")
                }
            }
        }
    }

    /**
     * 绘制边界
     */
    private fun SvgElement.drawBoundary(boundary: C4Db.Boundary, tv: ThemeVariables) {
        val fillColor = boundary.bgColor.ifEmpty { "none" }
        val strokeColor = boundary.borderColor.ifEmpty { "#444444" }
        val fontColor = boundary.fontColor.ifEmpty { "black" }

        group {
            rect(boundary.x, boundary.y, boundary.width, boundary.height) {
                attr("fill", fillColor)
                attr("stroke", strokeColor)
                attr("stroke-width", "1")
                attr("stroke-dasharray", "7.0,7.0")
                attr("rx", "2.5")
                attr("ry", "2.5")
            }
            // 边界标签
            val labelText = buildString {
                append(boundary.label)
                if (boundary.type.isNotEmpty()) append(" [${boundary.type}]")
            }
            text(labelText, boundary.x + 10, boundary.y + 20) {
                attr("fill", fontColor)
                attr("font-size", "$FONT_SIZE")
                attr("font-weight", "bold")
                attr("font-family", "'Open Sans', sans-serif")
            }
        }
    }
}
