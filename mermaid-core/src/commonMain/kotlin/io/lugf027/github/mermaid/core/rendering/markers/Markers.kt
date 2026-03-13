package io.lugf027.github.mermaid.core.rendering.markers

import io.lugf027.github.mermaid.core.rendering.svg.*

/**
 * 箭头标记定义 - 精确对标 mermaid-js markers.js
 *
 * mermaid-js 的 markers 直接放在 <g> 中（不是 <defs>），
 * ID 格式为 {diagramId}_{diagramType}-{markerType}{End/Start}。
 */
object Markers {

    /**
     * 创建所有标准标记，添加到指定的父组中
     *
     * @param parent 父元素（通常是 markers 所在的 <g>）
     * @param diagramId SVG 的 id（如 "my-svg"）
     * @param diagramType 图表类型标识（如 "flowchart-v2"）
     */
    fun addMarkers(parent: SvgElement, diagramId: String, diagramType: String = "flowchart-v2") {
        val prefix = "${diagramId}_${diagramType}"

        // 标准标记
        addPointEnd(parent, prefix, diagramType)
        addPointStart(parent, prefix, diagramType)
        addCircleEnd(parent, prefix, diagramType)
        addCircleStart(parent, prefix, diagramType)
        addCrossEnd(parent, prefix, diagramType)
        addCrossStart(parent, prefix, diagramType)

        // 类图/ER 图专用标记
        addAggregationEnd(parent, prefix, diagramType)
        addAggregationStart(parent, prefix, diagramType)
        addExtensionEnd(parent, prefix, diagramType)
        addExtensionStart(parent, prefix, diagramType)
        addCompositionEnd(parent, prefix, diagramType)
        addCompositionStart(parent, prefix, diagramType)
        addDependencyEnd(parent, prefix, diagramType)
        addDependencyStart(parent, prefix, diagramType)
        addLollipopEnd(parent, prefix, diagramType)
        addLollipopStart(parent, prefix, diagramType)

        // barb marker（状态图用）
        addBarbEnd(parent, prefix, diagramType)
    }

    /** pointEnd - 标准箭头（末端） */
    private fun addPointEnd(parent: SvgElement, prefix: String, diagramType: String) {
        val marker = SvgMarker()
        // mermaid-js 属性顺序: id, class, viewBox, refX, refY, markerUnits, markerWidth, markerHeight, orient
        marker.attr("id", "${prefix}-pointEnd")
        marker.addClass("marker").addClass(diagramType)
        marker.attr("viewBox", "0 0 10 10")
        marker.attr("refX", "5")
        marker.attr("refY", "5")
        marker.attr("markerUnits", "userSpaceOnUse")
        marker.attr("markerWidth", "8")
        marker.attr("markerHeight", "8")
        marker.attr("orient", "auto")
        marker.children.add(SvgPath().d("M 0 0 L 10 5 L 0 10 z").apply {
            addClass("arrowMarkerPath")
            attr("style", "stroke-width: 1; stroke-dasharray: 1, 0;")
        })
        parent.append(marker)
    }

    /** pointStart - 标准箭头（起始端） */
    private fun addPointStart(parent: SvgElement, prefix: String, diagramType: String) {
        val marker = SvgMarker()
        marker.attr("id", "${prefix}-pointStart")
        marker.addClass("marker").addClass(diagramType)
        marker.attr("viewBox", "0 0 10 10")
        marker.attr("refX", "4.5")
        marker.attr("refY", "5")
        marker.attr("markerUnits", "userSpaceOnUse")
        marker.attr("markerWidth", "8")
        marker.attr("markerHeight", "8")
        marker.attr("orient", "auto")
        marker.children.add(SvgPath().d("M 0 5 L 10 10 L 10 0 z").apply {
            addClass("arrowMarkerPath")
            attr("style", "stroke-width: 1; stroke-dasharray: 1, 0;")
        })
        parent.append(marker)
    }

    /** circleEnd - 圆形标记（末端） */
    private fun addCircleEnd(parent: SvgElement, prefix: String, diagramType: String) {
        val marker = SvgMarker()
        marker.attr("id", "${prefix}-circleEnd")
        marker.addClass("marker").addClass(diagramType)
        marker.attr("viewBox", "0 0 10 10")
        marker.attr("refX", "11")
        marker.attr("refY", "5")
        marker.attr("markerUnits", "userSpaceOnUse")
        marker.attr("markerWidth", "11")
        marker.attr("markerHeight", "11")
        marker.attr("orient", "auto")
        marker.children.add(SvgCircle().center(5.0, 5.0, 5.0).apply {
            addClass("arrowMarkerPath")
            attr("style", "stroke-width: 1; stroke-dasharray: 1, 0;")
        })
        parent.append(marker)
    }

    /** circleStart - 圆形标记（起始端） */
    private fun addCircleStart(parent: SvgElement, prefix: String, diagramType: String) {
        val marker = SvgMarker()
        marker.attr("id", "${prefix}-circleStart")
        marker.addClass("marker").addClass(diagramType)
        marker.attr("viewBox", "0 0 10 10")
        marker.attr("refX", "-1")
        marker.attr("refY", "5")
        marker.attr("markerUnits", "userSpaceOnUse")
        marker.attr("markerWidth", "11")
        marker.attr("markerHeight", "11")
        marker.attr("orient", "auto")
        marker.children.add(SvgCircle().center(5.0, 5.0, 5.0).apply {
            addClass("arrowMarkerPath")
            attr("style", "stroke-width: 1; stroke-dasharray: 1, 0;")
        })
        parent.append(marker)
    }

    /** crossEnd - 十字标记（末端） */
    private fun addCrossEnd(parent: SvgElement, prefix: String, diagramType: String) {
        val marker = SvgMarker()
        marker.attr("id", "${prefix}-crossEnd")
        marker.addClass("marker").addClass("cross").addClass(diagramType)
        marker.attr("viewBox", "0 0 11 11")
        marker.attr("refX", "12")
        marker.attr("refY", "5.2")
        marker.attr("markerUnits", "userSpaceOnUse")
        marker.attr("markerWidth", "11")
        marker.attr("markerHeight", "11")
        marker.attr("orient", "auto")
        marker.children.add(SvgPath().d("M 1,1 l 9,9 M 10,1 l -9,9").apply {
            addClass("arrowMarkerPath")
            attr("style", "stroke-width: 2; stroke-dasharray: 1, 0;")
        })
        parent.append(marker)
    }

    /** crossStart - 十字标记（起始端） */
    private fun addCrossStart(parent: SvgElement, prefix: String, diagramType: String) {
        val marker = SvgMarker()
        marker.attr("id", "${prefix}-crossStart")
        marker.addClass("marker").addClass("cross").addClass(diagramType)
        marker.attr("viewBox", "0 0 11 11")
        marker.attr("refX", "-1")
        marker.attr("refY", "5.2")
        marker.attr("markerUnits", "userSpaceOnUse")
        marker.attr("markerWidth", "11")
        marker.attr("markerHeight", "11")
        marker.attr("orient", "auto")
        marker.children.add(SvgPath().d("M 1,1 l 9,9 M 10,1 l -9,9").apply {
            addClass("arrowMarkerPath")
            attr("style", "stroke-width: 2; stroke-dasharray: 1, 0;")
        })
        parent.append(marker)
    }

    /** aggregationEnd - 空心菱形（末端） */
    private fun addAggregationEnd(parent: SvgElement, prefix: String, diagramType: String) {
        val marker = SvgMarker()
        marker.attr("id", "${prefix}-aggregationEnd")
        marker.addClass("marker").addClass(diagramType)
        marker.attr("viewBox", "0 0 20 15")
        marker.attr("refX", "18")
        marker.attr("refY", "7")
        marker.attr("markerUnits", "userSpaceOnUse")
        marker.attr("markerWidth", "20")
        marker.attr("markerHeight", "15")
        marker.attr("orient", "auto")
        marker.children.add(SvgPath().d("M 18,7 L9,13 L1,7 L9,1 Z").apply {
            addClass("arrowMarkerPath")
            attr("style", "stroke-width: 1; stroke-dasharray: 1, 0; fill: transparent;")
        })
        parent.append(marker)
    }

    /** aggregationStart - 空心菱形（起始端） */
    private fun addAggregationStart(parent: SvgElement, prefix: String, diagramType: String) {
        val marker = SvgMarker()
        marker.attr("id", "${prefix}-aggregationStart")
        marker.addClass("marker").addClass(diagramType)
        marker.attr("viewBox", "0 0 20 15")
        marker.attr("refX", "1")
        marker.attr("refY", "7")
        marker.attr("markerUnits", "userSpaceOnUse")
        marker.attr("markerWidth", "20")
        marker.attr("markerHeight", "15")
        marker.attr("orient", "auto")
        marker.children.add(SvgPath().d("M 18,7 L9,13 L1,7 L9,1 Z").apply {
            addClass("arrowMarkerPath")
            attr("style", "stroke-width: 1; stroke-dasharray: 1, 0; fill: transparent;")
        })
        parent.append(marker)
    }

    /** extensionEnd - 空心三角（末端） */
    private fun addExtensionEnd(parent: SvgElement, prefix: String, diagramType: String) {
        val marker = SvgMarker()
        marker.attr("id", "${prefix}-extensionEnd")
        marker.addClass("marker").addClass(diagramType)
        marker.attr("viewBox", "0 0 20 15")
        marker.attr("refX", "18")
        marker.attr("refY", "7")
        marker.attr("markerUnits", "userSpaceOnUse")
        marker.attr("markerWidth", "20")
        marker.attr("markerHeight", "15")
        marker.attr("orient", "auto")
        marker.children.add(SvgPath().d("M 1,1 V 13 L18,7 Z").apply {
            addClass("arrowMarkerPath")
            attr("style", "stroke-width: 1; stroke-dasharray: 1, 0; fill: transparent;")
        })
        parent.append(marker)
    }

    /** extensionStart - 空心三角（起始端） */
    private fun addExtensionStart(parent: SvgElement, prefix: String, diagramType: String) {
        val marker = SvgMarker()
        marker.attr("id", "${prefix}-extensionStart")
        marker.addClass("marker").addClass(diagramType)
        marker.attr("viewBox", "0 0 20 15")
        marker.attr("refX", "1")
        marker.attr("refY", "7")
        marker.attr("markerUnits", "userSpaceOnUse")
        marker.attr("markerWidth", "20")
        marker.attr("markerHeight", "15")
        marker.attr("orient", "auto")
        marker.children.add(SvgPath().d("M 1,7 L18,13 V 1 Z").apply {
            addClass("arrowMarkerPath")
            attr("style", "stroke-width: 1; stroke-dasharray: 1, 0; fill: transparent;")
        })
        parent.append(marker)
    }

    /** compositionEnd - 实心菱形（末端） */
    private fun addCompositionEnd(parent: SvgElement, prefix: String, diagramType: String) {
        val marker = SvgMarker()
        marker.attr("id", "${prefix}-compositionEnd")
        marker.addClass("marker").addClass(diagramType)
        marker.attr("viewBox", "0 0 20 15")
        marker.attr("refX", "18")
        marker.attr("refY", "7")
        marker.attr("markerUnits", "userSpaceOnUse")
        marker.attr("markerWidth", "20")
        marker.attr("markerHeight", "15")
        marker.attr("orient", "auto")
        marker.children.add(SvgPath().d("M 18,7 L9,13 L1,7 L9,1 Z").apply {
            addClass("arrowMarkerPath")
            attr("style", "stroke-width: 1; stroke-dasharray: 1, 0;")
        })
        parent.append(marker)
    }

    /** compositionStart - 实心菱形（起始端） */
    private fun addCompositionStart(parent: SvgElement, prefix: String, diagramType: String) {
        val marker = SvgMarker()
        marker.attr("id", "${prefix}-compositionStart")
        marker.addClass("marker").addClass(diagramType)
        marker.attr("viewBox", "0 0 20 15")
        marker.attr("refX", "1")
        marker.attr("refY", "7")
        marker.attr("markerUnits", "userSpaceOnUse")
        marker.attr("markerWidth", "20")
        marker.attr("markerHeight", "15")
        marker.attr("orient", "auto")
        marker.children.add(SvgPath().d("M 18,7 L9,13 L1,7 L9,1 Z").apply {
            addClass("arrowMarkerPath")
            attr("style", "stroke-width: 1; stroke-dasharray: 1, 0;")
        })
        parent.append(marker)
    }

    /** dependencyEnd - 开放箭头（末端） */
    private fun addDependencyEnd(parent: SvgElement, prefix: String, diagramType: String) {
        val marker = SvgMarker()
        marker.attr("id", "${prefix}-dependencyEnd")
        marker.addClass("marker").addClass(diagramType)
        marker.attr("viewBox", "0 0 20 15")
        marker.attr("refX", "18")
        marker.attr("refY", "7")
        marker.attr("markerUnits", "userSpaceOnUse")
        marker.attr("markerWidth", "20")
        marker.attr("markerHeight", "15")
        marker.attr("orient", "auto")
        marker.children.add(SvgPath().d("M 18,7 L9,13 L14,7 L9,1 Z").apply {
            addClass("arrowMarkerPath")
            attr("style", "stroke-width: 1; stroke-dasharray: 1, 0;")
        })
        parent.append(marker)
    }

    /** dependencyStart - 开放箭头（起始端） */
    private fun addDependencyStart(parent: SvgElement, prefix: String, diagramType: String) {
        val marker = SvgMarker()
        marker.attr("id", "${prefix}-dependencyStart")
        marker.addClass("marker").addClass(diagramType)
        marker.attr("viewBox", "0 0 20 15")
        marker.attr("refX", "1")
        marker.attr("refY", "7")
        marker.attr("markerUnits", "userSpaceOnUse")
        marker.attr("markerWidth", "20")
        marker.attr("markerHeight", "15")
        marker.attr("orient", "auto")
        marker.children.add(SvgPath().d("M 5,7 L9,13 L1,7 L9,1 Z").apply {
            addClass("arrowMarkerPath")
            attr("style", "stroke-width: 1; stroke-dasharray: 1, 0;")
        })
        parent.append(marker)
    }

    /** lollipopEnd - 圆形（末端） */
    private fun addLollipopEnd(parent: SvgElement, prefix: String, diagramType: String) {
        val marker = SvgMarker()
        marker.attr("id", "${prefix}-lollipopEnd")
        marker.addClass("marker").addClass(diagramType)
        marker.attr("viewBox", "0 0 15 15")
        marker.attr("refX", "13")
        marker.attr("refY", "7")
        marker.attr("markerUnits", "userSpaceOnUse")
        marker.attr("markerWidth", "15")
        marker.attr("markerHeight", "15")
        marker.attr("orient", "auto")
        marker.children.add(SvgCircle().center(7.0, 7.0, 6.0).apply {
            addClass("arrowMarkerPath")
            attr("style", "stroke-width: 1; stroke-dasharray: 1, 0; fill: transparent;")
        })
        parent.append(marker)
    }

    /** lollipopStart - 圆形（起始端） */
    private fun addLollipopStart(parent: SvgElement, prefix: String, diagramType: String) {
        val marker = SvgMarker()
        marker.attr("id", "${prefix}-lollipopStart")
        marker.addClass("marker").addClass(diagramType)
        marker.attr("viewBox", "0 0 15 15")
        marker.attr("refX", "1")
        marker.attr("refY", "7")
        marker.attr("markerUnits", "userSpaceOnUse")
        marker.attr("markerWidth", "15")
        marker.attr("markerHeight", "15")
        marker.attr("orient", "auto")
        marker.children.add(SvgCircle().center(7.0, 7.0, 6.0).apply {
            addClass("arrowMarkerPath")
            attr("style", "stroke-width: 1; stroke-dasharray: 1, 0; fill: transparent;")
        })
        parent.append(marker)
    }

    /** barbEnd - barb 箭头（状态图用） */
    private fun addBarbEnd(parent: SvgElement, prefix: String, diagramType: String) {
        val marker = SvgMarker()
        marker.attr("id", "${prefix}-barbEnd")
        marker.addClass("marker").addClass(diagramType)
        marker.attr("viewBox", "0 0 10 10")
        marker.attr("refX", "9")
        marker.attr("refY", "5")
        marker.attr("markerUnits", "userSpaceOnUse")
        marker.attr("markerWidth", "12")
        marker.attr("markerHeight", "12")
        marker.attr("orient", "auto")
        marker.children.add(SvgPath().d("M 0 0 L 10 5 L 0 10 z").apply {
            addClass("arrowMarkerPath")
            attr("style", "stroke-width: 1; stroke-dasharray: 1, 0;")
        })
        parent.append(marker)
    }

    /**
     * 获取标记 URL 引用
     * @param markerType 标记类型（如 "pointEnd"）
     * @param diagramId SVG id
     * @param diagramType 图表类型标识（如 "flowchart-v2"）
     */
    fun markerUrl(markerType: String, diagramId: String, diagramType: String = "flowchart-v2"): String {
        return "url(#${diagramId}_${diagramType}-${markerType})"
    }
}
