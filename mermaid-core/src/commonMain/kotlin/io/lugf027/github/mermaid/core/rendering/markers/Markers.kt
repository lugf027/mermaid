package io.lugf027.github.mermaid.core.rendering.markers

import io.lugf027.github.mermaid.core.rendering.svg.*

/**
 * 箭头标记定义 - 对标 mermaid-js markers.js
 *
 * 实现 SVG <marker> 元素，用于边的箭头渲染。
 */
object Markers {

    /**
     * 创建所有标准标记，添加到 <defs> 中
     */
    fun addMarkers(defs: SvgDefs, id: String) {
        addArrowHead(defs, id)
        addArrowHeadFilled(defs, id)
        addCircleMarker(defs, id)
        addCrossMarker(defs, id)
        addDiamondMarker(defs, id)
        addArrowPoint(defs, id)
    }

    /** 标准箭头（空心） */
    private fun addArrowHead(defs: SvgDefs, id: String) {
        defs.marker {
            setup(
                id = "arrowhead-${id}",
                viewBox = "0 0 10 10",
                refX = 9.0, refY = 5.0,
                markerWidth = 6.0, markerHeight = 8.0,
                orient = "auto"
            )
            children.add(SvgPath().d("M 0 0 L 10 5 L 0 10 z").apply {
                addClass("arrowMarkerPath")
                attr("style", "stroke-width: 1; stroke-dasharray: 1, 0;")
            })
        }
    }

    /** 实心箭头 */
    private fun addArrowHeadFilled(defs: SvgDefs, id: String) {
        defs.marker {
            setup(
                id = "arrowhead-filled-${id}",
                viewBox = "0 0 10 10",
                refX = 9.0, refY = 5.0,
                markerWidth = 6.0, markerHeight = 8.0,
                orient = "auto"
            )
            children.add(SvgPath().d("M 0 0 L 10 5 L 0 10 z").apply {
                addClass("arrowMarkerPath")
            })
        }
    }

    /** 圆形标记 */
    private fun addCircleMarker(defs: SvgDefs, id: String) {
        defs.marker {
            setup(
                id = "circle-${id}",
                viewBox = "0 0 10 10",
                refX = 5.0, refY = 5.0,
                markerWidth = 5.0, markerHeight = 5.0,
                orient = "auto"
            )
            children.add(SvgCircle().center(5.0, 5.0, 4.0).apply {
                addClass("arrowMarkerPath")
                attr("style", "stroke-width: 1; stroke-dasharray: 1, 0;")
            })
        }
    }

    /** 十字标记 */
    private fun addCrossMarker(defs: SvgDefs, id: String) {
        defs.marker {
            setup(
                id = "cross-${id}",
                viewBox = "0 0 10 10",
                refX = 5.0, refY = 5.0,
                markerWidth = 5.0, markerHeight = 5.0,
                orient = "auto"
            )
            children.add(SvgPath().d("M 0 0 L 10 10 M 10 0 L 0 10").apply {
                addClass("arrowMarkerPath")
                attr("style", "stroke-width: 2; stroke-dasharray: 1, 0;")
            })
        }
    }

    /** 菱形标记（聚合） */
    private fun addDiamondMarker(defs: SvgDefs, id: String) {
        defs.marker {
            setup(
                id = "aggregation-${id}",
                viewBox = "0 0 20 10",
                refX = 18.0, refY = 5.0,
                markerWidth = 10.0, markerHeight = 10.0,
                orient = "auto"
            )
            children.add(SvgPath().d("M 0 5 L 10 0 L 20 5 L 10 10 z").apply {
                addClass("arrowMarkerPath")
            })
        }
    }

    /** 箭头点标记 */
    private fun addArrowPoint(defs: SvgDefs, id: String) {
        defs.marker {
            setup(
                id = "arrow-point-${id}",
                viewBox = "0 0 12 12",
                refX = 9.0, refY = 6.0,
                markerWidth = 12.0, markerHeight = 12.0,
                orient = "auto"
            )
            children.add(SvgPath().d("M 0 0 L 12 6 L 0 12 z").apply {
                addClass("arrowMarkerPath")
            })
        }
    }

    /**
     * 获取标记 URL 引用
     */
    fun markerUrl(markerId: String, diagramId: String): String {
        return "url(#${markerId}-${diagramId})"
    }
}
