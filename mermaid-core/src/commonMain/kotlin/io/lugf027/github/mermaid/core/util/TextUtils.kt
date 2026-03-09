package io.lugf027.github.mermaid.core.util

/**
 * 文本工具函数 - 对标 mermaid-js utils.ts 中的文本处理函数
 */
object TextUtils {

    /**
     * 清理文本：将 CRLF 转 LF，移除非法字符
     */
    fun cleanupText(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .trimEnd()
    }

    /**
     * HTML 实体编码
     */
    fun encodeEntities(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    /**
     * 反转义 HTML 实体
     */
    fun decodeEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
    }

    /**
     * 安全清理文本：移除 HTML 标签
     */
    fun sanitizeText(text: String): String {
        return text.replace(Regex("<[^>]*>"), "")
    }

    /**
     * 将文本按最大宽度换行（简易实现）
     *
     * @param text 输入文本
     * @param maxWidth 最大字符宽度
     * @return 换行后的文本行列表
     */
    fun wrapText(text: String, maxWidth: Int): List<String> {
        if (maxWidth <= 0 || text.length <= maxWidth) return listOf(text)

        val lines = mutableListOf<String>()
        val words = text.split(" ")
        val currentLine = StringBuilder()

        for (word in words) {
            if (currentLine.isEmpty()) {
                currentLine.append(word)
            } else if (currentLine.length + 1 + word.length <= maxWidth) {
                currentLine.append(" ").append(word)
            } else {
                lines.add(currentLine.toString())
                currentLine.clear()
                currentLine.append(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    /**
     * 估算文本渲染宽度（假设等宽字体，每字符约 8px）
     */
    fun estimateTextWidth(text: String, fontSize: Double = 16.0): Double {
        return text.length * fontSize * 0.6
    }

    /**
     * 估算文本渲染高度
     */
    fun estimateTextHeight(text: String, fontSize: Double = 16.0, lineHeight: Double = 1.5): Double {
        val lines = text.count { it == '\n' } + 1
        return lines * fontSize * lineHeight
    }
}
