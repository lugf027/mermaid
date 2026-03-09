package io.lugf027.github.mermaid.core.rendering.svg

/**
 * SVG 序列化器 - 将 SvgElement 树递归序列化为格式正确的 SVG XML 字符串
 *
 * 处理属性转义、命名空间声明、缩进格式化。
 */
object SvgSerializer {

    /**
     * 将 SVG 元素树序列化为 SVG XML 字符串
     *
     * @param root SVG 根节点
     * @param indent 是否格式化缩进（调试用）
     * @return SVG XML 字符串
     */
    fun serialize(root: SvgElement, indent: Boolean = false): String {
        val sb = StringBuilder()
        if (root is SvgRoot) {
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        }
        serializeElement(root, sb, if (indent) 0 else -1)
        return sb.toString()
    }

    /**
     * 仅序列化 SVG 内容（不含 XML 声明），用于嵌入 HTML
     */
    fun serializeContent(root: SvgElement, indent: Boolean = false): String {
        val sb = StringBuilder()
        serializeElement(root, sb, if (indent) 0 else -1)
        return sb.toString()
    }

    private fun serializeElement(element: SvgElement, sb: StringBuilder, indentLevel: Int) {
        val doIndent = indentLevel >= 0
        val prefix = if (doIndent) "  ".repeat(indentLevel) else ""
        val tag = element.tagName

        // 特殊处理 <style> 和 <text>/<tspan>/<title>/<desc>
        val isTextElement = element is SvgText || element is SvgTspan ||
            element is SvgTitle || element is SvgDesc
        val isStyleElement = element is SvgStyle
        val isRootElement = element is SvgRoot
        val isSelfClosing = element.children.isEmpty() && !isTextElement && !isStyleElement && !isRootElement

        if (doIndent) sb.append(prefix)

        // 开始标签
        sb.append("<").append(tag)

        // 属性
        for ((key, value) in element.attributes) {
            sb.append(" ").append(key).append("=\"").append(escapeXmlAttribute(value)).append("\"")
        }

        if (isSelfClosing) {
            sb.append("/>")
            if (doIndent) sb.append("\n")
            return
        }

        sb.append(">")

        // 内容
        when (element) {
            is SvgStyle -> {
                if (doIndent) sb.append("\n")
                if (element.cssContent.isNotEmpty()) {
                    sb.append(element.cssContent)
                    if (doIndent) sb.append("\n")
                }
                // style 也可能有子元素
                for (child in element.children) {
                    serializeElement(child, sb, if (doIndent) indentLevel + 1 else -1)
                }
                if (doIndent) sb.append(prefix)
            }

            is SvgText -> {
                sb.append(escapeXmlContent(element.textContent))
                for (child in element.children) {
                    serializeElement(child, sb, -1) // text children inline
                }
            }

            is SvgTspan -> {
                sb.append(escapeXmlContent(element.textContent))
                for (child in element.children) {
                    serializeElement(child, sb, -1)
                }
            }

            is SvgTitle -> {
                sb.append(escapeXmlContent(element.textContent))
            }

            is SvgDesc -> {
                sb.append(escapeXmlContent(element.textContent))
            }

            else -> {
                if (doIndent && element.children.isNotEmpty()) sb.append("\n")
                for (child in element.children) {
                    serializeElement(child, sb, if (doIndent) indentLevel + 1 else -1)
                }
                if (doIndent && element.children.isNotEmpty()) sb.append(prefix)
            }
        }

        // 结束标签
        sb.append("</").append(tag).append(">")
        if (doIndent) sb.append("\n")
    }

    /** 转义 XML 属性值 */
    private fun escapeXmlAttribute(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    /** 转义 XML 文本内容 */
    private fun escapeXmlContent(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}
