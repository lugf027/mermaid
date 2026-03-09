package io.lugf027.github.mermaid.core.rendering.shapes

import io.lugf027.github.mermaid.core.layout.LayoutNode
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.themes.ThemeVariables

/**
 * 基础形状实现 - 对标 mermaid-js shapes/ 目录下的核心形状
 */
object BasicShapes {

    /** 方形矩形 */
    fun squareRect(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val w = node.width
        val h = node.height
        g.rect(-w / 2, -h / 2, w, h) {
            attr("fill", tv.nodeBkg)
            attr("stroke", tv.nodeBorder)
            attr("stroke-width", "1")
        }

        if (!node.label.isNullOrEmpty()) {
            g.text(node.label!!, 0.0, 5.0) {
                attr("text-anchor", "middle")
                attr("dominant-baseline", "middle")
                attr("fill", tv.primaryTextColor)
                addClass("nodeLabel")
            }
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
        g.rect(-w / 2, -h / 2, w, h) {
            rounded(rx)
            attr("fill", tv.nodeBkg)
            attr("stroke", tv.nodeBorder)
            attr("stroke-width", "1")
        }

        if (!node.label.isNullOrEmpty()) {
            g.text(node.label!!, 0.0, 5.0) {
                attr("text-anchor", "middle")
                attr("dominant-baseline", "middle")
                attr("fill", tv.primaryTextColor)
                addClass("nodeLabel")
            }
        }

        return g
    }

    /** 圆形 */
    fun circle(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val r = maxOf(node.width, node.height) / 2
        g.circle(0.0, 0.0, r) {
            attr("fill", tv.nodeBkg)
            attr("stroke", tv.nodeBorder)
            attr("stroke-width", "1")
        }

        if (!node.label.isNullOrEmpty()) {
            g.text(node.label!!, 0.0, 5.0) {
                attr("text-anchor", "middle")
                attr("dominant-baseline", "middle")
                attr("fill", tv.primaryTextColor)
                addClass("nodeLabel")
            }
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
        g.circle(0.0, 0.0, r) {
            attr("fill", tv.nodeBkg); attr("stroke", tv.nodeBorder); attr("stroke-width", "1")
        }
        g.circle(0.0, 0.0, r - 5) {
            attr("fill", tv.nodeBkg); attr("stroke", tv.nodeBorder); attr("stroke-width", "1")
        }

        if (!node.label.isNullOrEmpty()) {
            g.text(node.label!!, 0.0, 5.0) {
                attr("text-anchor", "middle"); attr("dominant-baseline", "middle")
                attr("fill", tv.primaryTextColor); addClass("nodeLabel")
            }
        }

        return g
    }

    /** 菱形 */
    fun diamond(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val w = node.width / 2
        val h = node.height / 2
        g.polygon(listOf(0.0 to -h, w to 0.0, 0.0 to h, -w to 0.0)) {
            attr("fill", tv.nodeBkg); attr("stroke", tv.nodeBorder); attr("stroke-width", "1")
        }

        if (!node.label.isNullOrEmpty()) {
            g.text(node.label!!, 0.0, 5.0) {
                attr("text-anchor", "middle"); attr("dominant-baseline", "middle")
                attr("fill", tv.primaryTextColor); addClass("nodeLabel")
            }
        }

        return g
    }

    /** 六边形 */
    fun hexagon(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val w = node.width / 2
        val h = node.height / 2
        val offset = w * 0.2
        g.polygon(listOf(
            -w to 0.0, -w + offset to -h, w - offset to -h,
            w to 0.0, w - offset to h, -w + offset to h
        )) {
            attr("fill", tv.nodeBkg); attr("stroke", tv.nodeBorder); attr("stroke-width", "1")
        }

        if (!node.label.isNullOrEmpty()) {
            g.text(node.label!!, 0.0, 5.0) {
                attr("text-anchor", "middle"); attr("dominant-baseline", "middle")
                attr("fill", tv.primaryTextColor); addClass("nodeLabel")
            }
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
        g.rect(-w / 2, -h / 2, w, h) {
            rounded(r)
            attr("fill", tv.nodeBkg); attr("stroke", tv.nodeBorder); attr("stroke-width", "1")
        }

        if (!node.label.isNullOrEmpty()) {
            g.text(node.label!!, 0.0, 5.0) {
                attr("text-anchor", "middle"); attr("dominant-baseline", "middle")
                attr("fill", tv.primaryTextColor); addClass("nodeLabel")
            }
        }

        return g
    }

    /** 圆柱体 */
    fun cylinder(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        val w = node.width
        val h = node.height
        val ry = 10.0

        // 使用路径绘制圆柱体
        val pathBuilder = SvgPathBuilder()
        pathBuilder.moveTo(-w / 2, -h / 2 + ry)
        pathBuilder.arcTo(w / 2, ry, 0.0, false, true, w / 2, -h / 2 + ry)
        pathBuilder.lineTo(w / 2, h / 2 - ry)
        pathBuilder.arcTo(w / 2, ry, 0.0, false, true, -w / 2, h / 2 - ry)
        pathBuilder.closePath()

        g.path(pathBuilder.build()) {
            attr("fill", tv.nodeBkg); attr("stroke", tv.nodeBorder); attr("stroke-width", "1")
        }

        // 顶部椭圆
        g.ellipse(0.0, -h / 2 + ry, w / 2, ry) {
            attr("fill", tv.nodeBkg); attr("stroke", tv.nodeBorder); attr("stroke-width", "1")
        }

        if (!node.label.isNullOrEmpty()) {
            g.text(node.label!!, 0.0, 5.0) {
                attr("text-anchor", "middle"); attr("dominant-baseline", "middle")
                attr("fill", tv.primaryTextColor); addClass("nodeLabel")
            }
        }

        return g
    }

    /** 椭圆 */
    fun ellipseShape(node: LayoutNode, tv: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("node default")
        g.attr("id", node.domId ?: node.id)
        g.translate(node.x, node.y)

        g.ellipse(0.0, 0.0, node.width / 2, node.height / 2) {
            attr("fill", tv.nodeBkg); attr("stroke", tv.nodeBorder); attr("stroke-width", "1")
        }

        if (!node.label.isNullOrEmpty()) {
            g.text(node.label!!, 0.0, 5.0) {
                attr("text-anchor", "middle"); attr("dominant-baseline", "middle")
                attr("fill", tv.primaryTextColor); addClass("nodeLabel")
            }
        }

        return g
    }
}
