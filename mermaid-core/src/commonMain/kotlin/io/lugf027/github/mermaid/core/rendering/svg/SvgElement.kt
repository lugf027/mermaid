package io.lugf027.github.mermaid.core.rendering.svg

/**
 * SVG IR 核心节点定义 - 替代 d3+DOM 的纯 Kotlin SVG 数据模型
 *
 * 所有渲染操作转化为对 SvgElement 树的构建操作，最终序列化为 SVG XML 字符串。
 * 每个节点持有 attributes Map 和 children List，支持链式 API。
 */
sealed class SvgElement {
    /** 元素属性 */
    val attributes: MutableMap<String, String> = mutableMapOf()

    /** 子元素列表 */
    val children: MutableList<SvgElement> = mutableListOf()

    /** SVG 标签名 */
    abstract val tagName: String

    /** 设置属性（链式 API） */
    fun attr(key: String, value: String): SvgElement {
        attributes[key] = value
        return this
    }

    /** 设置属性（数值） */
    fun attr(key: String, value: Number): SvgElement {
        attributes[key] = formatNumber(value.toDouble())
        return this
    }

    /** 添加子元素 */
    fun append(child: SvgElement): SvgElement {
        children.add(child)
        return child
    }

    /** 在开头插入子元素 */
    fun prepend(child: SvgElement): SvgElement {
        children.add(0, child)
        return child
    }

    /** 添加 CSS class */
    fun addClass(className: String): SvgElement {
        val existing = attributes["class"] ?: ""
        attributes["class"] = if (existing.isEmpty()) className else "$existing $className"
        return this
    }

    /** 设置 transform 属性 */
    fun translate(x: Double, y: Double): SvgElement {
        attributes["transform"] = "translate(${formatNumber(x)}, ${formatNumber(y)})"
        return this
    }

    /** 查找子元素（按 tagName） */
    fun findByTag(tag: String): SvgElement? {
        return children.find { it.tagName == tag }
    }

    /** 递归查找所有匹配的元素 */
    fun findAllByTag(tag: String): List<SvgElement> {
        val result = mutableListOf<SvgElement>()
        if (this.tagName == tag) result.add(this)
        children.forEach { result.addAll(it.findAllByTag(tag)) }
        return result
    }

    companion object {
        /** 格式化数值，避免不必要的小数 */
        fun formatNumber(value: Double): String {
            return if (value == value.toLong().toDouble()) {
                value.toLong().toString()
            } else {
                // 保留最多4位小数（KMP 兼容方式）
                val rounded = kotlin.math.round(value * 10000) / 10000.0
                val str = rounded.toString()
                // 移除尾部多余的零
                if (str.contains('.')) {
                    str.trimEnd('0').trimEnd('.')
                } else {
                    str
                }
            }
        }
    }
}

/** SVG 根元素 <svg> */
class SvgRoot : SvgElement() {
    override val tagName = "svg"

    init {
        attr("xmlns", "http://www.w3.org/2000/svg")
        attr("xmlns:xlink", "http://www.w3.org/1999/xlink")
    }

    /** 设置 viewBox */
    fun viewBox(x: Double, y: Double, width: Double, height: Double): SvgRoot {
        attr("viewBox", "${formatNumber(x)} ${formatNumber(y)} ${formatNumber(width)} ${formatNumber(height)}")
        return this
    }
}

/** SVG 组 <g> */
class SvgGroup : SvgElement() {
    override val tagName = "g"
}

/** SVG 矩形 <rect> */
class SvgRect : SvgElement() {
    override val tagName = "rect"

    fun bounds(x: Double, y: Double, width: Double, height: Double): SvgRect {
        attr("x", x); attr("y", y); attr("width", width); attr("height", height)
        return this
    }

    fun rounded(rx: Double, ry: Double = rx): SvgRect {
        attr("rx", rx); attr("ry", ry)
        return this
    }
}

/** SVG 圆 <circle> */
class SvgCircle : SvgElement() {
    override val tagName = "circle"

    fun center(cx: Double, cy: Double, r: Double): SvgCircle {
        attr("cx", cx); attr("cy", cy); attr("r", r)
        return this
    }
}

/** SVG 椭圆 <ellipse> */
class SvgEllipse : SvgElement() {
    override val tagName = "ellipse"

    fun center(cx: Double, cy: Double, rx: Double, ry: Double): SvgEllipse {
        attr("cx", cx); attr("cy", cy); attr("rx", rx); attr("ry", ry)
        return this
    }
}

/** SVG 路径 <path> */
class SvgPath : SvgElement() {
    override val tagName = "path"

    fun d(pathData: String): SvgPath {
        attr("d", pathData)
        return this
    }
}

/** SVG 直线 <line> */
class SvgLine : SvgElement() {
    override val tagName = "line"

    fun coords(x1: Double, y1: Double, x2: Double, y2: Double): SvgLine {
        attr("x1", x1); attr("y1", y1); attr("x2", x2); attr("y2", y2)
        return this
    }
}

/** SVG 折线 <polyline> */
class SvgPolyline : SvgElement() {
    override val tagName = "polyline"

    fun points(pts: List<Pair<Double, Double>>): SvgPolyline {
        attr("points", pts.joinToString(" ") { "${formatNumber(it.first)},${formatNumber(it.second)}" })
        return this
    }
}

/** SVG 多边形 <polygon> */
class SvgPolygon : SvgElement() {
    override val tagName = "polygon"

    fun points(pts: List<Pair<Double, Double>>): SvgPolygon {
        attr("points", pts.joinToString(" ") { "${formatNumber(it.first)},${formatNumber(it.second)}" })
        return this
    }
}

/** SVG 文本 <text> */
class SvgText(var textContent: String = "") : SvgElement() {
    override val tagName = "text"

    fun text(content: String): SvgText {
        textContent = content
        return this
    }

    fun position(x: Double, y: Double): SvgText {
        attr("x", x); attr("y", y)
        return this
    }
}

/** SVG tspan <tspan> */
class SvgTspan(var textContent: String = "") : SvgElement() {
    override val tagName = "tspan"
}

/** SVG defs <defs> */
class SvgDefs : SvgElement() {
    override val tagName = "defs"
}

/** SVG style <style> */
class SvgStyle(var cssContent: String = "") : SvgElement() {
    override val tagName = "style"
}

/** SVG marker <marker> */
class SvgMarker : SvgElement() {
    override val tagName = "marker"

    fun setup(
        id: String, viewBox: String, refX: Double, refY: Double,
        markerWidth: Double, markerHeight: Double, orient: String = "auto"
    ): SvgMarker {
        attr("id", id)
        attr("viewBox", viewBox)
        attr("refX", refX); attr("refY", refY)
        attr("markerWidth", markerWidth); attr("markerHeight", markerHeight)
        attr("orient", orient)
        return this
    }
}

/** SVG use <use> */
class SvgUse : SvgElement() {
    override val tagName = "use"
}

/** SVG foreignObject <foreignObject> */
class SvgForeignObject : SvgElement() {
    override val tagName = "foreignObject"
}

/** SVG clipPath <clipPath> */
class SvgClipPath : SvgElement() {
    override val tagName = "clipPath"
}

/** SVG title <title> */
class SvgTitle(var textContent: String = "") : SvgElement() {
    override val tagName = "title"
}

/** SVG desc <desc> */
class SvgDesc(var textContent: String = "") : SvgElement() {
    override val tagName = "desc"
}
