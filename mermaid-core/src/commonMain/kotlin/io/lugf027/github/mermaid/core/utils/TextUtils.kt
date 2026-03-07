package io.lugf027.github.mermaid.core.utils

/**
 * 文本处理工具函数。
 * 提供 sanitize、文本分割、Unicode 处理等功能。
 */
object TextUtils {

    /**
     * 清理/转义文本中的特殊字符。
     */
    fun sanitize(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#039;")
    }

    /**
     * 反转义。
     */
    fun desanitize(text: String): String {
        return text
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&amp;", "&")
    }

    /**
     * 将文本按指定最大宽度进行换行分割。
     * @param text 原始文本
     * @param maxWidth 最大宽度（字符数估算）
     * @return 分行后的文本列表
     */
    fun wrapText(text: String, maxWidth: Int): List<String> {
        if (maxWidth <= 0 || text.length <= maxWidth) return listOf(text)

        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = StringBuilder()

        for (word in words) {
            if (currentLine.isEmpty()) {
                currentLine.append(word)
            } else if (currentLine.length + 1 + word.length <= maxWidth) {
                currentLine.append(" ").append(word)
            } else {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    /**
     * 移除文本首尾引号。
     */
    fun removeQuotes(text: String): String {
        return text.removeSurrounding("\"").removeSurrounding("'")
    }

    /**
     * 检查文本是否包含 Markdown 语法。
     */
    fun containsMarkdown(text: String): Boolean {
        return text.contains("**") || text.contains("__") ||
            text.contains("*") || text.contains("_") ||
            text.contains("`") || text.contains("~~")
    }

    /**
     * 将 Markdown 粗体/斜体转为纯文本。
     */
    fun stripMarkdown(text: String): String {
        return text
            .replace(Regex("""\*\*(.+?)\*\*"""), "$1")
            .replace(Regex("""__(.+?)__"""), "$1")
            .replace(Regex("""\*(.+?)\*"""), "$1")
            .replace(Regex("""_(.+?)_"""), "$1")
            .replace(Regex("""~~(.+?)~~"""), "$1")
            .replace(Regex("""`(.+?)`"""), "$1")
    }
}
