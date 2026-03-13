package io.lugf027.github.mermaid.core.rendering.shapes

import io.lugf027.github.mermaid.core.layout.LayoutNode
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.TextUtils

/**
 * 类图/状态图/ER 图专用形状 - 对标 mermaid-js 各图表特定形状
 */
object DiagramShapes {

    /**
     * classBox - 类图节点形状
     *
     * 结构: 上部类名 + 分隔线 + 属性区 + 分隔线 + 方法区
     */
    fun classBox(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default classGroup")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val fontSize = 14.0
        val lineHeight = 20.0
        val padding = 8.0
        val label = node.label ?: node.id

        // 估算文本宽度
        val titleWidth = TextUtils.estimateTextWidth(label, fontSize)
        val minWidth = maxOf(titleWidth + padding * 2, node.width, 100.0)

        // 简化实现：只显示标题区域的矩形
        val h = maxOf(node.height, lineHeight + padding * 2)
        val w = minWidth

        val rect = SvgRect()
        rect.addClass("basic").addClass("label-container")
        rect.attr("style", "")
        rect.bounds(-w / 2, -h / 2, w, h)
        g.children.add(rect)

        // 标题文本
        val titleText = SvgText(label)
        titleText.position(0.0, -h / 2 + padding + fontSize)
        titleText.attr("text-anchor", "middle")
        titleText.addClass("classTitle")
        g.children.add(titleText)

        // 分隔线
        val dividerY = -h / 2 + lineHeight + padding
        val dividerLine = SvgLine()
        dividerLine.coords(-w / 2, dividerY, w / 2, dividerY)
        dividerLine.attr("class", "divider")
        g.children.add(dividerLine)

        return g
    }

    /**
     * stateStart - 起始状态（实心黑圆）
     */
    fun stateStart(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val r = 7.0
        node.width = r * 2
        node.height = r * 2

        val circle = SvgCircle()
        circle.center(0.0, 0.0, r)
        circle.addClass("state-start")
        circle.attr("style", "fill: ${tv.lineColor}; stroke: ${tv.lineColor};")
        g.children.add(circle)

        return g
    }

    /**
     * stateEnd - 终止状态（双圆环）
     */
    fun stateEnd(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val r = 9.0
        node.width = r * 2
        node.height = r * 2

        // 外圆
        val outer = SvgCircle()
        outer.center(0.0, 0.0, r)
        outer.addClass("state-end")
        outer.attr("style", "fill: ${tv.primaryBorderColor}; stroke: ${tv.background}; stroke-width: 1.5;")
        g.children.add(outer)

        // 内圆
        val inner = SvgCircle()
        inner.center(0.0, 0.0, r * 0.6)
        inner.addClass("end-state-inner")
        inner.attr("style", "fill: ${tv.background}; stroke: ${tv.background};")
        g.children.add(inner)

        return g
    }

    /**
     * fork/join - 分叉/汇合状态（黑色粗条）
     */
    fun forkJoin(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val w = 70.0
        val h = 7.0
        node.width = w
        node.height = h

        val rect = SvgRect()
        rect.bounds(-w / 2, -h / 2, w, h)
        rect.addClass("fork-join")
        rect.attr("style", "fill: ${tv.lineColor}; stroke: ${tv.lineColor};")
        g.children.add(rect)

        return g
    }

    /**
     * note - 注释形状（黄色矩形）
     */
    fun note(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val fontSize = 14.0
        val label = node.label ?: ""
        val padding = node.padding

        val textWidth = TextUtils.estimateTextWidth(label, fontSize)
        val w = maxOf(textWidth + padding * 2, node.width, 50.0)
        val h = maxOf(node.height, 30.0)

        val rect = SvgRect()
        rect.bounds(-w / 2, -h / 2, w, h)
        rect.attr("style", "fill: ${tv.noteBkgColor}; stroke: ${tv.noteBorderColor}; stroke-width: 1px;")
        g.children.add(rect)

        // 文本
        if (label.isNotEmpty()) {
            val text = SvgText(label)
            text.position(0.0, 5.0)
            text.attr("text-anchor", "middle")
            text.attr("style", "fill: ${tv.noteTextColor}; font-size: ${fontSize}px;")
            g.children.add(text)
        }

        node.width = w
        node.height = h

        return g
    }

    /**
     * erBox - ER 图实体形状
     */
    fun erBox(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default er")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val fontSize = 12.0
        val label = node.label ?: node.id
        val padding = 15.0

        val textWidth = TextUtils.estimateTextWidth(label, fontSize)
        val w = maxOf(textWidth + padding * 2, node.width, 100.0)
        val h = maxOf(node.height, 75.0)

        // 实体框
        val rect = SvgRect()
        rect.addClass("entityBox")
        rect.bounds(-w / 2, -h / 2, w, h)
        rect.attr("style", "fill: ${tv.mainBkg}; stroke: ${tv.nodeBorder};")
        g.children.add(rect)

        // 实体名文本
        val text = SvgText(label)
        text.position(0.0, -h / 2 + 20.0)
        text.attr("text-anchor", "middle")
        text.addClass("entityLabel")
        text.attr("style", "fill: ${tv.primaryTextColor}; font-weight: bold;")
        g.children.add(text)

        node.width = w
        node.height = h

        return g
    }

    /**
     * divider - 并发分隔线
     */
    fun divider(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val w = node.width
        val h = 2.0
        node.height = h

        val line = SvgLine()
        line.coords(-w / 2, 0.0, w / 2, 0.0)
        line.attr("class", "divider")
        line.attr("style", "stroke: ${tv.lineColor}; stroke-dasharray: 10,10;")
        g.children.add(line)

        return g
    }

    /**
     * rectWithTitle - 带标题的矩形（状态图带描述的状态）
     */
    fun rectWithTitle(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val w = node.width
        val h = node.height
        val rx = 5.0

        // 外框
        val rect = SvgRect()
        rect.addClass("basic").addClass("label-container")
        rect.attr("style", "")
        rect.bounds(-w / 2, -h / 2, w, h)
        rect.rounded(rx)
        g.children.add(rect)

        // 标题（第一行）和描述（后续行）
        val label = node.label ?: node.id
        val parts = label.split("\\n")
        if (parts.isNotEmpty()) {
            val titleText = SvgText(parts[0])
            titleText.position(0.0, -h / 2 + 20.0)
            titleText.attr("text-anchor", "middle")
            titleText.addClass("state-title")
            titleText.attr("style", "font-weight: bolder;")
            g.children.add(titleText)

            // 分隔线
            val dividerY = -h / 2 + 30.0
            val dividerLine = SvgLine()
            dividerLine.coords(-w / 2, dividerY, w / 2, dividerY)
            dividerLine.attr("class", "descr-divider")
            g.children.add(dividerLine)

            // 描述文本
            for (i in 1 until parts.size) {
                val descText = SvgText(parts[i])
                descText.position(0.0, dividerY + 15.0 * i)
                descText.attr("text-anchor", "middle")
                descText.addClass("state-description")
                g.children.add(descText)
            }
        }

        return g
    }
}
