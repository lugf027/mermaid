package io.lugf027.github.mermaid.core.rendering.shapes

import io.lugf027.github.mermaid.core.layout.LayoutNode
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.TextUtils

/**
 * 基础形状实现 - 精确对标 mermaid-js shapes/ 目录下的核心形状
 *
 * mermaid-js 节点结构：
 * <g class="node default" id="flowchart-A-0" transform="translate(x, y)">
 *   <rect class="basic label-container" style="" x="..." y="..." width="..." height="..."/>
 *   <g class="label" style="" transform="translate(-textWidth/2, -12)">
 *     <rect/>
 *     <foreignObject width="textWidth" height="24">
 *       <div xmlns="http://www.w3.org/1999/xhtml"
 *            style="display: table-cell; white-space: nowrap; line-height: 1.5; max-width: 200px; text-align: center;">
 *         <span class="nodeLabel"><p>labelText</p></span>
 *       </div>
 *     </foreignObject>
 *   </g>
 * </g>
 */
object BasicShapes {

    /**
     * 创建 mermaid-js 风格的标签组（foreignObject + HTML）
     */
    private fun createLabelGroup(g: SvgGroup, label: String, fontSize: Double = 16.0) {
        val textWidth = TextUtils.estimateDomTextWidth(label, fontSize)
        val textHeight = 24.0  // mermaid-js foreignObject 固定高度

        val labelGroup = SvgGroup()
        labelGroup.addClass("label")
        labelGroup.attr("style", "")
        labelGroup.translate(-textWidth / 2, -12.0)

        // 空 rect（mermaid-js 的结构中有此元素）
        labelGroup.children.add(SvgRect())

        // foreignObject + HTML 内容
        val fo = SvgForeignObject()
        fo.attr("width", SvgElement.formatNumber(textWidth))
        fo.attr("height", SvgElement.formatNumber(textHeight))

        val htmlContent = "<div xmlns=\"http://www.w3.org/1999/xhtml\" " +
            "style=\"display: table-cell; white-space: nowrap; line-height: 1.5; max-width: 200px; text-align: center;\">" +
            "<span class=\"nodeLabel\"><p>${label}</p></span></div>"
        fo.children.add(SvgRawHtml(htmlContent))

        labelGroup.append(fo)
        g.append(labelGroup)
    }

    /** 方形矩形 */
    fun squareRect(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val w = node.width
        val h = node.height

        // rect 使用 class 而非内联 fill/stroke
        val rect = SvgRect()
        rect.addClass("basic").addClass("label-container")
        rect.attr("style", "")
        rect.bounds(-w / 2, -h / 2, w, h)
        g.children.add(rect)

        if (!node.label.isNullOrEmpty()) {
            createLabelGroup(g, node.label!!)
        }

        return g
    }

    /** 圆角矩形 */
    fun roundedRect(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val w = node.width
        val h = node.height
        val rx = 15.0

        val rect = SvgRect()
        rect.addClass("basic").addClass("label-container")
        rect.attr("style", "")
        rect.bounds(-w / 2, -h / 2, w, h)
        rect.rounded(rx)
        g.children.add(rect)

        if (!node.label.isNullOrEmpty()) {
            createLabelGroup(g, node.label!!)
        }

        return g
    }

    /**
     * 圆形 — 对标 mermaid-js circle.ts
     *
     * mermaid-js 公式: r = bbox.width / 2 + padding
     * 其中 padding = node.padding / 2 (halfPadding)
     *
     * 但在 dagre 布局中，node.width = 直径 = 2r，所以:
     *   r = node.width / 2
     *
     * 而 dagre 中 circle 的 width 已经在 buildGraph 里按 JS 公式计算。
     * 所以这里直接用 node.width / 2 作为半径即可。
     */
    fun circle(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        // 半径 = textWidth/2 + halfPadding（对标 JS）
        // dagre 中 width/height 已经被设为正确的值，直接取 width/2
        val r = node.width / 2
        val circle = SvgCircle()
        circle.center(0.0, 0.0, r)
        circle.addClass("basic").addClass("label-container")
        circle.attr("style", "")
        g.children.add(circle)

        if (!node.label.isNullOrEmpty()) {
            createLabelGroup(g, node.label!!)
        }

        return g
    }

    /** 双圆 */
    fun doubleCircle(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val r = maxOf(node.width, node.height) / 2
        val outer = SvgCircle()
        outer.center(0.0, 0.0, r)
        outer.addClass("basic").addClass("label-container")
        outer.attr("style", "")
        g.children.add(outer)

        val inner = SvgCircle()
        inner.center(0.0, 0.0, r - 5)
        inner.addClass("basic").addClass("label-container")
        inner.attr("style", "")
        g.children.add(inner)

        if (!node.label.isNullOrEmpty()) {
            createLabelGroup(g, node.label!!)
        }

        return g
    }

    /**
     * 菱形 - 精确匹配 mermaid-js insertPolygonShape
     *
     * mermaid-js 菱形格式：
     * <polygon points="s/2,0 s,-s/2 s/2,-s 0,-s/2"
     *          class="label-container" transform="translate(-s/2+0.5, s/2)"/>
     * 其中 s = textWidth + node.padding + textHeight + node.padding
     *
     * 但从实际 SVG 观察，mermaid-js 使用:
     * points="{halfDiag},0 {fullDiag},{-halfDiag} {halfDiag},{-fullDiag} 0,{-halfDiag}"
     * transform="translate(-{halfDiag-0.5}, {halfDiag})"
     */
    fun diamond(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        // mermaid-js 菱形的实际尺寸
        val halfDiag = node.width / 2  // == node.height / 2 (菱形是正方形旋转)
        val fullDiag = node.width  // == node.height

        // 使用 mermaid-js insertPolygonShape 的坐标格式
        val polygon = SvgPolygon()
        polygon.attr("points",
            "${SvgElement.formatNumber(halfDiag)},0 " +
            "${SvgElement.formatNumber(fullDiag)},${SvgElement.formatNumber(-halfDiag)} " +
            "${SvgElement.formatNumber(halfDiag)},${SvgElement.formatNumber(-fullDiag)} " +
            "0,${SvgElement.formatNumber(-halfDiag)}"
        )
        polygon.addClass("label-container")
        polygon.translate(-halfDiag + 0.5, halfDiag)
        g.children.add(polygon)

        if (!node.label.isNullOrEmpty()) {
            createLabelGroup(g, node.label!!)
        }

        return g
    }

    /** 六边形 — 对标 mermaid-js hexagon.ts / dagre-wrapper nodes.js */
    fun hexagon(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val w = node.width
        val h = node.height
        // JS: m = h / 4（f=4），不是 w * 0.2
        val m = h / 4

        // JS 点定义（左上为原点）: (m,0), (w-m,0), (w,-h/2), (w-m,-h), (m,-h), (0,-h/2)
        // 转换为居中坐标系：
        val polygon = SvgPolygon()
        polygon.points(listOf(
            -w / 2 to 0.0,             // 对应 JS (0, -h/2)
            -w / 2 + m to -h / 2,       // 对应 JS (m, -h)
            w / 2 - m to -h / 2,        // 对应 JS (w-m, -h)
            w / 2 to 0.0,              // 对应 JS (w, -h/2)
            w / 2 - m to h / 2,         // 对应 JS (w-m, 0)
            -w / 2 + m to h / 2         // 对应 JS (m, 0)
        ))
        polygon.addClass("basic").addClass("label-container")
        polygon.attr("style", "")
        g.children.add(polygon)

        if (!node.label.isNullOrEmpty()) {
            createLabelGroup(g, node.label!!)
        }

        return g
    }

    /** 体育场形（两端半圆） */
    fun stadium(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val w = node.width
        val h = node.height
        val r = h / 2

        val rect = SvgRect()
        rect.bounds(-w / 2, -h / 2, w, h)
        rect.rounded(r)
        rect.addClass("basic").addClass("label-container")
        rect.attr("style", "")
        g.children.add(rect)

        if (!node.label.isNullOrEmpty()) {
            createLabelGroup(g, node.label!!)
        }

        return g
    }

    /** 圆柱体 — 对标 mermaid-js dagre-wrapper/nodes.js cylinder 形状 */
    fun cylinder(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val w = node.width
        val rx = w / 2
        // 动态 ry — 对标 JS: ry = rx / (2.5 + w / 50)
        val ry = rx / (2.5 + w / 50)
        // 高度包含 ry 部分
        val h = node.height

        // JS 渲染方式：
        // path d="M0,{ry} a{rx},{ry} 0,0,0 {w},0 a{rx},{ry} 0,0,0 -{w},0 l0,{h-2*ry} a{rx},{ry} 0,0,0 {w},0 l0,-{h-2*ry}"
        // translate(-rx, -(h/2 + ry))
        // 这里我们用 KMP 居中坐标系实现
        val pathBuilder = SvgPathBuilder()
        pathBuilder.moveTo(-rx, -h / 2 + ry)
        pathBuilder.arcTo(rx, ry, 0.0, false, true, rx, -h / 2 + ry)
        pathBuilder.lineTo(rx, h / 2 - ry)
        pathBuilder.arcTo(rx, ry, 0.0, false, true, -rx, h / 2 - ry)
        pathBuilder.closePath()

        val path = SvgPath()
        path.d(pathBuilder.build())
        path.addClass("basic").addClass("label-container")
        path.attr("style", "")
        g.children.add(path)

        // 顶部椭圆
        val ellipse = SvgEllipse()
        ellipse.center(0.0, -h / 2 + ry, rx, ry)
        ellipse.addClass("basic").addClass("label-container")
        ellipse.attr("style", "")
        g.children.add(ellipse)

        if (!node.label.isNullOrEmpty()) {
            createLabelGroup(g, node.label!!)
        }

        return g
    }

    /** 椭圆 */
    fun ellipseShape(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val ellipse = SvgEllipse()
        ellipse.center(0.0, 0.0, node.width / 2, node.height / 2)
        ellipse.addClass("basic").addClass("label-container")
        ellipse.attr("style", "")
        g.children.add(ellipse)

        if (!node.label.isNullOrEmpty()) {
            createLabelGroup(g, node.label!!)
        }

        return g
    }
}
