package io.lugf027.github.mermaid.core.rendering.svg

/**
 * SVG 构建器 DSL - 提供类 d3 的声明式 SVG 构建 API
 *
 * 用法示例：
 * ```kotlin
 * val svg = buildSvg {
 *     attr("width", "800")
 *     attr("height", "600")
 *     viewBox(0.0, 0.0, 800.0, 600.0)
 *
 *     defs {
 *         style("text { font-family: sans-serif; }")
 *     }
 *
 *     group {
 *         addClass("nodes")
 *         rect(10.0, 20.0, 100.0, 50.0) {
 *             attr("fill", "#f9f9f9")
 *             rounded(5.0)
 *         }
 *         text("Hello", 60.0, 50.0) {
 *             attr("text-anchor", "middle")
 *         }
 *     }
 * }
 * ```
 */
fun buildSvg(block: SvgRoot.() -> Unit): SvgRoot {
    return SvgRoot().apply(block)
}

// ===== SvgElement 扩展函数 =====

/** 添加 <g> 子组 */
fun SvgElement.group(block: SvgGroup.() -> Unit = {}): SvgGroup {
    val g = SvgGroup().apply(block)
    children.add(g)
    return g
}

/** 添加 <rect> */
fun SvgElement.rect(
    x: Double, y: Double, width: Double, height: Double,
    block: SvgRect.() -> Unit = {}
): SvgRect {
    val r = SvgRect().apply {
        bounds(x, y, width, height)
        block()
    }
    children.add(r)
    return r
}

/** 添加 <circle> */
fun SvgElement.circle(
    cx: Double, cy: Double, r: Double,
    block: SvgCircle.() -> Unit = {}
): SvgCircle {
    val c = SvgCircle().apply {
        center(cx, cy, r)
        block()
    }
    children.add(c)
    return c
}

/** 添加 <ellipse> */
fun SvgElement.ellipse(
    cx: Double, cy: Double, rx: Double, ry: Double,
    block: SvgEllipse.() -> Unit = {}
): SvgEllipse {
    val e = SvgEllipse().apply {
        center(cx, cy, rx, ry)
        block()
    }
    children.add(e)
    return e
}

/** 添加 <path> */
fun SvgElement.path(
    d: String = "",
    block: SvgPath.() -> Unit = {}
): SvgPath {
    val p = SvgPath().apply {
        if (d.isNotEmpty()) d(d)
        block()
    }
    children.add(p)
    return p
}

/** 添加 <line> */
fun SvgElement.line(
    x1: Double, y1: Double, x2: Double, y2: Double,
    block: SvgLine.() -> Unit = {}
): SvgLine {
    val l = SvgLine().apply {
        coords(x1, y1, x2, y2)
        block()
    }
    children.add(l)
    return l
}

/** 添加 <polyline> */
fun SvgElement.polyline(
    points: List<Pair<Double, Double>>,
    block: SvgPolyline.() -> Unit = {}
): SvgPolyline {
    val pl = SvgPolyline().apply {
        points(points)
        block()
    }
    children.add(pl)
    return pl
}

/** 添加 <polygon> */
fun SvgElement.polygon(
    points: List<Pair<Double, Double>>,
    block: SvgPolygon.() -> Unit = {}
): SvgPolygon {
    val pg = SvgPolygon().apply {
        points(points)
        block()
    }
    children.add(pg)
    return pg
}

/** 添加 <text> */
fun SvgElement.text(
    content: String, x: Double = 0.0, y: Double = 0.0,
    block: SvgText.() -> Unit = {}
): SvgText {
    val t = SvgText(content).apply {
        position(x, y)
        block()
    }
    children.add(t)
    return t
}

/** 添加 <tspan> */
fun SvgElement.tspan(
    content: String,
    block: SvgTspan.() -> Unit = {}
): SvgTspan {
    val ts = SvgTspan(content).apply(block)
    children.add(ts)
    return ts
}

/** 添加 <defs> */
fun SvgElement.defs(block: SvgDefs.() -> Unit = {}): SvgDefs {
    val d = SvgDefs().apply(block)
    children.add(d)
    return d
}

/** 添加 <style> */
fun SvgElement.style(css: String): SvgStyle {
    val s = SvgStyle(css)
    children.add(s)
    return s
}

/** 添加 <marker> */
fun SvgElement.marker(block: SvgMarker.() -> Unit = {}): SvgMarker {
    val m = SvgMarker().apply(block)
    children.add(m)
    return m
}

/** 添加 <title> */
fun SvgElement.title(content: String): SvgTitle {
    val t = SvgTitle(content)
    children.add(t)
    return t
}

/** 添加 <desc> */
fun SvgElement.desc(content: String): SvgDesc {
    val d = SvgDesc(content)
    children.add(d)
    return d
}

/** 添加 <clipPath> */
fun SvgElement.clipPath(block: SvgClipPath.() -> Unit = {}): SvgClipPath {
    val cp = SvgClipPath().apply(block)
    children.add(cp)
    return cp
}
