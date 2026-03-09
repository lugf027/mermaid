package io.lugf027.github.mermaid.core.rendering.shapes

import io.lugf027.github.mermaid.core.layout.LayoutNode
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.themes.ThemeVariables

/**
 * 特殊形状实现 - 对标 mermaid-js shapes/ 目录下的特殊形状
 */
object SpecialShapes {

    /** 子程序（双竖线矩形） */
    fun subroutine(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val w = node.width; val h = node.height
        val inset = 8.0
        g.rect(-w / 2, -h / 2, w, h) {
            attr("fill", tv.nodeBkg); attr("stroke", tv.nodeBorder); attr("stroke-width", "1")
        }
        // 内侧竖线
        g.line(-w / 2 + inset, -h / 2, -w / 2 + inset, h / 2) {
            attr("stroke", tv.nodeBorder); attr("stroke-width", "1")
        }
        g.line(w / 2 - inset, -h / 2, w / 2 - inset, h / 2) {
            attr("stroke", tv.nodeBorder); attr("stroke-width", "1")
        }

        addLabel(g, node, tv)
        return g
    }

    /** 梯形 */
    fun trapezoid(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val w = node.width / 2; val h = node.height / 2
        val offset = w * 0.2
        g.polygon(listOf(-w + offset to -h, w - offset to -h, w to h, -w to h)) {
            attr("fill", tv.nodeBkg); attr("stroke", tv.nodeBorder); attr("stroke-width", "1")
        }

        addLabel(g, node, tv)
        return g
    }

    /** 倒梯形 */
    fun invertedTrapezoid(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val w = node.width / 2; val h = node.height / 2
        val offset = w * 0.2
        g.polygon(listOf(-w to -h, w to -h, w - offset to h, -w + offset to h)) {
            attr("fill", tv.nodeBkg); attr("stroke", tv.nodeBorder); attr("stroke-width", "1")
        }

        addLabel(g, node, tv)
        return g
    }

    /** 平行四边形（右倾） */
    fun parallelogram(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val w = node.width / 2; val h = node.height / 2
        val offset = w * 0.2
        g.polygon(listOf(-w + offset to -h, w + offset to -h, w - offset to h, -w - offset to h)) {
            attr("fill", tv.nodeBkg); attr("stroke", tv.nodeBorder); attr("stroke-width", "1")
        }

        addLabel(g, node, tv)
        return g
    }

    /** 平行四边形（左倾） */
    fun parallelogramLeft(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val w = node.width / 2; val h = node.height / 2
        val offset = w * 0.2
        g.polygon(listOf(-w - offset to -h, w - offset to -h, w + offset to h, -w + offset to h)) {
            attr("fill", tv.nodeBkg); attr("stroke", tv.nodeBorder); attr("stroke-width", "1")
        }

        addLabel(g, node, tv)
        return g
    }

    /** 奇数形（旗帜形） */
    fun odd(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val w = node.width / 2; val h = node.height / 2
        g.polygon(listOf(-w to -h, w to -h, w to 0.0, w - w * 0.15 to h, -w to h)) {
            attr("fill", tv.nodeBkg); attr("stroke", tv.nodeBorder); attr("stroke-width", "1")
        }

        addLabel(g, node, tv)
        return g
    }

    /** 云朵 */
    fun cloud(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val w = node.width; val h = node.height
        // 简化的云朵路径
        val path = SvgPathBuilder()
        path.moveTo(-w * 0.3, h * 0.1)
        path.cubicTo(-w * 0.5, -h * 0.1, -w * 0.4, -h * 0.4, -w * 0.1, -h * 0.35)
        path.cubicTo(-w * 0.1, -h * 0.5, w * 0.1, -h * 0.5, w * 0.15, -h * 0.35)
        path.cubicTo(w * 0.35, -h * 0.45, w * 0.5, -h * 0.2, w * 0.4, h * 0.0)
        path.cubicTo(w * 0.5, h * 0.2, w * 0.3, h * 0.35, w * 0.1, h * 0.3)
        path.cubicTo(w * 0.0, h * 0.45, -w * 0.2, h * 0.4, -w * 0.3, h * 0.1)
        path.closePath()

        g.path(path.build()) {
            attr("fill", tv.nodeBkg); attr("stroke", tv.nodeBorder); attr("stroke-width", "1")
        }

        addLabel(g, node, tv)
        return g
    }

    /** 闪电 */
    fun bolt(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val w = node.width / 2; val h = node.height / 2
        g.polygon(listOf(
            0.0 to -h, w * 0.6 to -h * 0.1, w * 0.15 to -h * 0.1,
            w * 0.4 to h, -w * 0.15 to h * 0.1, w * 0.15 to h * 0.1
        )) {
            attr("fill", tv.nodeBkg); attr("stroke", tv.nodeBorder); attr("stroke-width", "1")
        }

        addLabel(g, node, tv)
        return g
    }

    /** 通用标签添加 */
    private fun addLabel(g: SvgGroup, node: LayoutNode, tv: ThemeVariables) {
        if (!node.label.isNullOrEmpty()) {
            g.text(node.label!!, 0.0, 5.0) {
                attr("text-anchor", "middle")
                attr("dominant-baseline", "middle")
                attr("fill", tv.primaryTextColor)
                addClass("nodeLabel")
            }
        }
    }
}
