package io.lugf027.github.mermaid.core.rendering.edges

import io.lugf027.github.mermaid.core.layout.LayoutEdge
import io.lugf027.github.mermaid.core.layout.Point
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.TextUtils

/**
 * 边渲染器 - 精确对标 mermaid-js edges.js
 *
 * mermaid-js 将边路径和边标签分别放在不同的 <g> 组中：
 * - edgePaths: <g class="edgePaths"> 包含所有边路径 <path>
 * - edgeLabels: <g class="edgeLabels"> 包含所有边标签
 */
object EdgeRenderer {

    /**
     * 渲染边路径 - 对应 mermaid-js edgePaths 中的 <path>
     *
     * mermaid-js 格式：
     * <path d="..." id="L_A_B_0" class="edge-thickness-normal edge-pattern-solid ... flowchart-link"
     *       style=";" data-edge="true" data-et="edge" data-id="L_A_B_0"
     *       data-points="..." marker-end="url(#...)"/>
     */
    fun renderPath(edge: LayoutEdge, diagramId: String, themeVariables: ThemeVariables): SvgElement {
        val pathData = buildPathData(edge)
        val edgeId = buildEdgeId(edge)

        val path = SvgPath()
        path.d(pathData)
        path.attr("id", edgeId)

        // 边线粗细 CSS 类 — 对标 mermaid-js edges.js switch(edge.thickness)
        // mermaid-js 中 thickness = rawEdge.stroke: normal/thick/dotted/invisible
        val thicknessClass = when (edge.stroke) {
            "thick" -> "edge-thickness-thick"
            "invisible" -> "edge-thickness-invisible"
            else -> "edge-thickness-normal"  // normal, dotted 等都用 normal
        }
        // 边线样式 CSS 类 — 对标 mermaid-js edges.js switch(edge.pattern)
        // mermaid-js 中 pattern = rawEdge.stroke: normal/thick 走 default=solid, dotted 走 dotted
        val patternClass = when (edge.stroke) {
            "dotted" -> "edge-pattern-dotted"
            else -> "edge-pattern-solid"  // normal, thick 等都用 solid
        }
        path.addClass(thicknessClass)
        path.addClass(patternClass)
        // 第二组 CSS 类固定 — 对标 flowDb.ts 中 classes 字段始终为
        // 'edge-thickness-normal edge-pattern-solid flowchart-link'
        path.addClass("edge-thickness-normal")
        path.addClass("edge-pattern-solid")
        path.addClass("flowchart-link")
        path.attr("style", ";")
        path.attr("data-edge", "true")
        path.attr("data-et", "edge")
        path.attr("data-id", edgeId)

        // data-points (base64 编码的 JSON 点数组)
        val pointsJson = buildPointsJson(edge)
        path.attr("data-points", base64Encode(pointsJson))

        // 箭头标记 — 对标 mermaid-js edgeMarker.ts addEdgeMarkers
        // arrowType -> marker type 映射: arrow_point->point, arrow_cross->cross, arrow_circle->circle
        // "none" 或 null -> 不设置 marker
        val endMarkerType = arrowTypeToMarkerType(edge.arrowTypeEnd)
        if (endMarkerType != null) {
            path.attr("marker-end", "url(#${diagramId}_flowchart-v2-${endMarkerType}End)")
        }
        val startMarkerType = arrowTypeToMarkerType(edge.arrowTypeStart)
        if (startMarkerType != null) {
            path.attr("marker-start", "url(#${diagramId}_flowchart-v2-${startMarkerType}Start)")
        }

        return path
    }

    /**
     * 渲染边标签 - 对应 mermaid-js edgeLabels 中的 <g class="edgeLabel">
     *
     * mermaid-js 格式（有标签时）：
     * <g class="edgeLabel" transform="translate(x, y)">
     *   <g class="label" data-id="L_B_C_0" transform="translate(-textWidth/2, -12)">
     *     <foreignObject width="textWidth" height="24">
     *       <div xmlns="http://www.w3.org/1999/xhtml" class="labelBkg"
     *            style="display: table-cell; white-space: nowrap; line-height: 1.5; max-width: 200px; text-align: center;">
     *         <span class="edgeLabel"><p>label</p></span>
     *       </div>
     *     </foreignObject>
     *   </g>
     * </g>
     *
     * 无标签时：
     * <g class="edgeLabel">
     *   <g class="label" data-id="..." transform="translate(0, 0)">
     *     <foreignObject width="0" height="0">
     *       <div ...><span class="edgeLabel">\n</span></div>
     *     </foreignObject>
     *   </g>
     * </g>
     */
    fun renderLabel(edge: LayoutEdge, diagramId: String, themeVariables: ThemeVariables): SvgGroup {
        val edgeId = buildEdgeId(edge)
        val g = SvgGroup()
        g.addClass("edgeLabel")

        val hasLabel = !edge.label.isNullOrEmpty()

        if (hasLabel) {
            g.translate(edge.x, edge.y)
        }

        val label = edge.label ?: ""
        val textWidth = if (hasLabel) TextUtils.estimateDomTextWidth(label, 16.0) else 0.0
        val textHeight = if (hasLabel) 24.0 else 0.0

        val labelGroup = SvgGroup()
        labelGroup.addClass("label")
        labelGroup.attr("data-id", edgeId)

        if (hasLabel) {
            labelGroup.translate(-textWidth / 2, -12.0)
        } else {
            labelGroup.translate(0.0, 0.0)
        }

        // foreignObject
        val fo = SvgForeignObject()
        fo.attr("width", SvgElement.formatNumber(textWidth))
        fo.attr("height", SvgElement.formatNumber(textHeight))

        // HTML 内容
        val htmlContent = if (hasLabel) {
            "<div xmlns=\"http://www.w3.org/1999/xhtml\" class=\"labelBkg\" " +
                "style=\"display: table-cell; white-space: nowrap; line-height: 1.5; max-width: 200px; text-align: center;\">" +
                "<span class=\"edgeLabel\"><p>${label}</p></span></div>"
        } else {
            "<div xmlns=\"http://www.w3.org/1999/xhtml\" class=\"labelBkg\" " +
                "style=\"display: table-cell; white-space: nowrap; line-height: 1.5; max-width: 200px; text-align: center;\">" +
                "<span class=\"edgeLabel\">\n</span></div>"
        }
        fo.children.add(SvgRawHtml(htmlContent))

        labelGroup.append(fo)
        g.append(labelGroup)

        return g
    }

    /**
     * 旧接口兼容 - 同时渲染路径和标签
     */
    fun render(edge: LayoutEdge, diagramId: String, themeVariables: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("edgePath")
        g.attr("id", "edge-${edge.id}")
        g.append(renderPath(edge, diagramId, themeVariables))
        val label = renderLabel(edge, diagramId, themeVariables)
        g.append(label)
        return g
    }

    /**
     * 将 arrowType 映射为 marker 类型名 — 对标 mermaid-js edgeMarker.ts arrowTypesMap
     *
     * 返回 null 表示不添加 marker（如 "none" 或 null）
     */
    private fun arrowTypeToMarkerType(arrowType: String?): String? {
        if (arrowType == null || arrowType == "none") return null
        return when (arrowType) {
            "arrow_point" -> "point"
            "arrow_cross" -> "cross"
            "arrow_circle" -> "circle"
            "arrow_barb" -> "barb"
            "aggregation" -> "aggregation"
            "extension" -> "extension"
            "composition" -> "composition"
            "dependency" -> "dependency"
            "lollipop" -> "lollipop"
            else -> null  // 未知类型不添加 marker
        }
    }

    /**
     * 构建边 ID - mermaid-js 格式: L_{source}_{target}_{idx}
     */
    private fun buildEdgeId(edge: LayoutEdge): String {
        // mermaid-js 使用 "L_{source}_{target}_{counter}" 格式
        return "L_${edge.start}_${edge.end}_0"
    }

    /**
     * Marker 偏移量表 — 对应 mermaid-js lineWithOffset.ts 中的 markerOffsets
     */
    private val MARKER_OFFSETS = mapOf(
        "aggregation" to 17.25,
        "extension" to 17.25,
        "composition" to 17.25,
        "dependency" to 6.0,
        "lollipop" to 13.5,
        "arrow_point" to 4.0,
    )

    /**
     * 实现 mermaid-js 的 getLineFunctionsWithOffset
     *
     * mermaid-js 在渲染边路径前，对首尾点应用 marker 偏移：
     * - 起始点：根据 arrowTypeStart 和与下一点的角度偏移
     * - 终止点：根据 arrowTypeEnd 和与前一点的角度偏移
     * - 当点距边缘的距离小于 markerHeight 时，还有额外的近距调整
     */
    private fun applyMarkerOffsets(points: List<Point>, edge: LayoutEdge): List<Point> {
        if (points.size < 2) return points

        val result = points.map { Point(it.x, it.y) }.toMutableList()

        val arrowTypeStart = edge.arrowTypeStart
        val arrowTypeEnd = edge.arrowTypeEnd
        val startOffset = if (arrowTypeStart != null) MARKER_OFFSETS[arrowTypeStart] else null
        val endOffset = if (arrowTypeEnd != null) MARKER_OFFSETS[arrowTypeEnd] else null

        // Determine overall direction
        val directionX = if (points[0].x < points[points.size - 1].x) "left" else "right"
        val directionY = if (points[0].y < points[points.size - 1].y) "down" else "up"

        for (i in points.indices) {
            var offsetX = 0.0
            var offsetY = 0.0

            // First point offset (arrowTypeStart)
            if (i == 0 && startOffset != null) {
                val angle = calculateAngle(points[0], points[1])
                val deltaX = points[1].x - points[0].x
                val deltaY = points[1].y - points[0].y
                offsetX = startOffset * kotlin.math.cos(angle) * (if (deltaX >= 0) 1.0 else -1.0)
                offsetY = startOffset * kotlin.math.abs(kotlin.math.sin(angle)) * (if (deltaY >= 0) 1.0 else -1.0)
            }

            // Last point offset (arrowTypeEnd)
            if (i == points.size - 1 && endOffset != null) {
                val angle = calculateAngle(points[points.size - 1], points[points.size - 2])
                val deltaX = points[points.size - 2].x - points[points.size - 1].x
                val deltaY = points[points.size - 2].y - points[points.size - 1].y
                offsetX = endOffset * kotlin.math.cos(angle) * (if (deltaX >= 0) 1.0 else -1.0)
                offsetY = endOffset * kotlin.math.abs(kotlin.math.sin(angle)) * (if (deltaY >= 0) 1.0 else -1.0)
            }

            // Proximity adjustments for x
            val diffToEndX = kotlin.math.abs(points[i].x - points[points.size - 1].x)
            val diffToEndY = kotlin.math.abs(points[i].y - points[points.size - 1].y)
            val diffToStartX = kotlin.math.abs(points[i].x - points[0].x)
            val diffToStartY = kotlin.math.abs(points[i].y - points[0].y)
            val extraRoom = 1.0

            // End proximity x adjustment
            if (endOffset != null && diffToEndX < endOffset && diffToEndX > 0 && diffToEndY < endOffset) {
                var adj = endOffset + extraRoom - diffToEndX
                adj *= if (directionX == "right") -1.0 else 1.0
                offsetX -= adj
            }
            // Start proximity x adjustment
            if (startOffset != null && diffToStartX < startOffset && diffToStartX > 0 && diffToStartY < startOffset) {
                var adj = startOffset + extraRoom - diffToStartX
                adj *= if (directionX == "right") -1.0 else 1.0
                offsetX += adj
            }
            // End proximity y adjustment
            if (endOffset != null && diffToEndY < endOffset && diffToEndY > 0 && diffToEndX < endOffset) {
                var adj = endOffset + extraRoom - diffToEndY
                adj *= if (directionY == "up") -1.0 else 1.0
                offsetY -= adj
            }
            // Start proximity y adjustment
            if (startOffset != null && diffToStartY < startOffset && diffToStartY > 0 && diffToStartX < startOffset) {
                var adj = startOffset + extraRoom - diffToStartY
                adj *= if (directionY == "up") -1.0 else 1.0
                offsetY += adj
            }

            result[i] = Point(points[i].x + offsetX, points[i].y + offsetY)
        }

        return result
    }

    /**
     * 计算两点间的角度
     */
    private fun calculateAngle(p1: Point, p2: Point): Double {
        val deltaX = p2.x - p1.x
        val deltaY = p2.y - p1.y
        return kotlin.math.atan(deltaY / deltaX)
    }

    /**
     * 构建路径数据 - 使用 d3.line() 风格的曲线插值
     *
     * mermaid-js 使用 getLineFunctionsWithOffset 对点进行 marker 偏移后，
     * 再通过 d3.curveBasis 生成 B-spline 路径。
     */
    private fun buildPathData(edge: LayoutEdge): String {
        val rawPoints = edge.points
        if (rawPoints.isEmpty()) return ""

        // 应用 marker 偏移（匹配 mermaid-js getLineFunctionsWithOffset）
        val points = applyMarkerOffsets(rawPoints, edge)

        if (points.size <= 2) {
            // 两个点：直线
            val builder = SvgPathBuilder()
            builder.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                builder.lineTo(points[i].x, points[i].y)
            }
            return builder.build()
        }

        // d3.curveBasis 实现 - B-spline 曲线
        return curveBasis(points)
    }

    /**
     * d3.curveBasis 实现 - 精确匹配 d3-shape/src/curve/basis.js
     *
     * d3.curveBasis 的状态机：
     * - point=0: 首次 point(x,y)，仅记录 → point=1
     * - point=1: 第二次 point(x,y)，输出 L (5*x0+x)/6,(5*y0+y)/6 → point=2
     * - point=2: 第三次 point(x,y)，输出第一个 C 段 → point=3
     * - point=3+: 后续点输出 C 段
     * - lineEnd: 输出最后一个 C 段（重复最后点）+ L 到最后点
     *
     * lineStart 时先输出 M 到第一个 point
     */
    private fun curveBasis(points: List<Point>): String {
        val n = points.size
        if (n == 0) return ""
        if (n == 1) return "M${fmtNum(points[0].x)},${fmtNum(points[0].y)}"

        val sb = StringBuilder()

        var x0 = 0.0; var y0 = 0.0
        var x1 = 0.0; var y1 = 0.0
        var point = 0

        // d3 lineStart: 初始 point=0
        // 处理每个点
        for (i in 0 until n) {
            val x = points[i].x
            val y = points[i].y

            when (point) {
                0 -> {
                    // 第一个点 → M
                    point = 1
                    sb.append("M${fmtNum(x)},${fmtNum(y)}")
                    x0 = x; y0 = y
                }
                1 -> {
                    // 第二个点 → L (5*x0+x)/6, (5*y0+y)/6
                    point = 2
                    sb.append("L${fmtNum((5.0 * x0 + x) / 6.0)},${fmtNum((5.0 * y0 + y) / 6.0)}")
                    x1 = x; y1 = y
                }
                2 -> {
                    // 第三个点 → 第一个 C 段
                    point = 3
                    basisPoint(sb, x0, y0, x1, y1, x, y)
                    x0 = x1; y0 = y1; x1 = x; y1 = y
                }
                else -> {
                    // 后续点 → C 段
                    basisPoint(sb, x0, y0, x1, y1, x, y)
                    x0 = x1; y0 = y1; x1 = x; y1 = y
                }
            }
        }

        // d3 lineEnd: 根据 point 状态输出结尾
        when (point) {
            3 -> {
                // 重复最后点 + C 段 + L 到最后点
                basisPoint(sb, x0, y0, x1, y1, x1, y1)
                sb.append("L${fmtNum(x1)},${fmtNum(y1)}")
            }
            2 -> {
                // 只有两个点：L 到第二个点
                sb.append("L${fmtNum(x1)},${fmtNum(y1)}")
            }
            // point=1 或更少时不需要额外输出
        }

        return sb.toString()
    }

    /**
     * 生成 B-spline 的一个三次贝塞尔段
     */
    private fun basisPoint(sb: StringBuilder, x0: Double, y0: Double, x1: Double, y1: Double, x2: Double, y2: Double) {
        sb.append("C${fmtNum((2 * x0 + x1) / 3)},${fmtNum((2 * y0 + y1) / 3)}")
        sb.append(",${fmtNum((x0 + 2 * x1) / 3)},${fmtNum((y0 + 2 * y1) / 3)}")
        sb.append(",${fmtNum((x0 + 4 * x1 + x2) / 6)},${fmtNum((y0 + 4 * y1 + y2) / 6)}")
    }

    private fun buildPointsJson(edge: LayoutEdge): String {
        val parts = edge.points.joinToString(",") { p ->
            "{\"x\":${p.x},\"y\":${p.y}}"
        }
        return "[$parts]"
    }

    /**
     * 简易 Base64 编码（ASCII only）
     */
    private fun base64Encode(input: String): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val bytes = input.encodeToByteArray()
        val sb = StringBuilder()
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xFF else 0
            val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xFF else 0
            sb.append(chars[(b0 shr 2) and 0x3F])
            sb.append(chars[((b0 shl 4) or (b1 shr 4)) and 0x3F])
            sb.append(if (i + 1 < bytes.size) chars[((b1 shl 2) or (b2 shr 6)) and 0x3F] else '=')
            sb.append(if (i + 2 < bytes.size) chars[b2 and 0x3F] else '=')
            i += 3
        }
        return sb.toString()
    }

    /**
     * 格式化数值 — 与 SvgElement.formatNumber 保持一致
     */
    private fun fmtNum(value: Double): String = SvgElement.formatNumber(value)
}
