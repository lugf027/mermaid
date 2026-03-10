package io.lugf027.github.mermaid.core.util

/**
 * 文本工具函数 - 对标 mermaid-js utils.ts 中的文本处理函数
 */
object TextUtils {

    /**
     * trebuchet ms 字体 16px 下的字符宽度查找表。
     * 数据来自 Chromium puppeteer canvas measureText 精确度量。
     */
    private val TREBUCHET_MS_16PX_WIDTHS: Map<Char, Double> = mapOf(
        // 大写字母
        'A' to 9.4375, 'B' to 9.0546875, 'C' to 9.5703125, 'D' to 9.8125,
        'E' to 8.5703125, 'F' to 8.3984375, 'G' to 10.8203125, 'H' to 10.46875,
        'I' to 4.453125, 'J' to 7.625, 'K' to 9.2109375, 'L' to 8.1015625,
        'M' to 11.3515625, 'N' to 10.2109375, 'O' to 10.78125, 'P' to 8.921875,
        'Q' to 10.8125, 'R' to 9.3125, 'S' to 7.6953125, 'T' to 9.2890625,
        'U' to 10.375, 'V' to 9.3984375, 'W' to 13.6328125, 'X' to 8.90625,
        'Y' to 9.125, 'Z' to 8.8046875,
        // 小写字母
        'a' to 8.40625, 'b' to 8.9140625, 'c' to 7.921875, 'd' to 8.9140625,
        'e' to 8.7265625, 'f' to 5.9140625, 'g' to 8.03125, 'h' to 8.7421875,
        'i' to 4.5625, 'j' to 5.8671875, 'k' to 8.0703125, 'l' to 4.71875,
        'm' to 13.28125, 'n' to 8.7421875, 'o' to 8.5859375, 'p' to 8.9140625,
        'q' to 8.9140625, 'r' to 6.21875, 's' to 6.4765625, 't' to 6.34375,
        'u' to 8.7421875, 'v' to 7.8359375, 'w' to 11.90625, 'x' to 8.015625,
        'y' to 7.890625, 'z' to 7.59375,
        // 数字
        '0' to 8.390625, '1' to 8.390625, '2' to 8.390625, '3' to 8.390625,
        '4' to 8.390625, '5' to 8.390625, '6' to 8.390625, '7' to 8.390625,
        '8' to 8.390625, '9' to 8.390625,
        // 常用标点符号和特殊字符
        ' ' to 4.8203125, '!' to 5.875, '"' to 5.1953125, '#' to 8.390625,
        '$' to 8.390625, '%' to 9.6015625, '&' to 11.296875, '\'' to 2.5546875,
        '(' to 5.875, ')' to 5.875, '*' to 5.875, '+' to 8.390625,
        ',' to 5.875, '-' to 5.875, '.' to 5.875, '/' to 8.390625,
        ':' to 5.875, ';' to 5.875, '<' to 8.390625, '=' to 8.390625,
        '>' to 8.390625, '?' to 5.875, '@' to 12.328125,
    )

    /**
     * trebuchet ms 字体 16px 下的字距调整（kerning）对照表。
     * 数据来自 Chromium puppeteer canvas measureText 精确度量。
     * 仅包含 kern 值 < -0.001 的字符对。
     */
    private val KERNING_PAIRS_16PX: Map<String, Double> = mapOf(
        "LV" to -2.21875, "Tw" to -2.2109375, "LY" to -2.0703125, "Tu" to -2.0703125,
        "LW" to -2.0, "Ta" to -1.9921875, "Tc" to -1.9921875, "Te" to -1.9921875,
        "To" to -1.9921875, "Ts" to -1.921875, "Yq" to -1.9140625, "Ty" to -1.84375,
        "Yo" to -1.8359375, "PA" to -1.78125, "Tr" to -1.7578125, "AY" to -1.703125,
        "YA" to -1.703125, "FA" to -1.6953125, "Ye" to -1.6796875, "LT" to -1.6328125,
        "VA" to -1.6328125, "AT" to -1.5546875, "TA" to -1.5546875, "Ya" to -1.484375,
        "Yp" to -1.484375, "AV" to -1.40625, "AW" to -1.40625, "WA" to -1.40625,
        "Ly" to -1.328125, "Va" to -1.2578125, "Yu" to -1.1796875, "Vu" to -1.0390625,
        "Ve" to -1.03125, "Vo" to -1.03125, "RW" to -1.0234375, "RY" to -1.0234375,
        "Vr" to -0.9609375, "Yv" to -0.953125, "Wa" to -0.890625, "Yi" to -0.890625,
        "Av" to -0.8828125, "TO" to -0.8828125, "Wr" to -0.8046875, "Pa" to -0.75,
        "Pe" to -0.75, "Ph" to -0.75, "Pi" to -0.75, "Po" to -0.75, "Pr" to -0.75,
        "Aw" to -0.734375, "RV" to -0.734375, "We" to -0.734375, "Wo" to -0.734375,
        "Ti" to -0.6640625, "Ay" to -0.65625, "RT" to -0.65625, "Wu" to -0.65625,
        "Re" to -0.6484375, "Ro" to -0.6484375, "Vy" to -0.59375, "Ke" to -0.5,
        "Ki" to -0.5, "Kn" to -0.5, "Ko" to -0.5, "Ku" to -0.5, "Kw" to -0.5,
        "Ru" to -0.4609375, "Vi" to -0.2890625, "Wy" to -0.2890625, "Wi" to -0.21875,
    )

    /**
     * 空格相关的 DOM 布局 kerning 表。
     * 这些值在 DOM getBoundingClientRect 中体现，但 canvas measureText 不包含。
     * 格式：key = "X " 表示字符 X 后接空格的调整，" X" 表示空格后接字符 X 的调整。
     */
    private val SPACE_KERNING_16PX: Map<String, Double> = mapOf(
        // 空格后接字母的调整（DOM 特有）
        " A" to -0.8828125,
        " T" to -0.2890625,
        " Y" to -0.2890625,
        // 字母后接空格的调整（DOM 特有）
        "A " to -0.8828125,
        "L " to -0.59375,
        "P " to -0.2890625,
        "T " to -0.2890625,
        "Y " to -0.2890625,
    )

    /** trebuchet ms 16px 下的默认字符宽度 */
    private const val DEFAULT_CHAR_WIDTH_16PX = 8.0

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
     * 精确估算文本渲染宽度（模拟 trebuchet ms 字体）
     *
     * 使用字符级宽度查找表 + 字距调整（kerning）对照表，按 fontSize / 16 缩放。
     * 结果接近 Chromium canvas.measureText() 的精度。
     *
     * 注意：此方法模拟的是 canvas.measureText()，不包含 DOM 布局特有的空格 kerning。
     * 如需模拟 DOM getBoundingClientRect()，请使用 [estimateDomTextWidth]。
     */
    fun estimateTextWidth(text: String, fontSize: Double = 16.0): Double {
        val scale = fontSize / 16.0
        var width = 0.0

        // 累加各字符宽度
        for (ch in text) {
            width += (TREBUCHET_MS_16PX_WIDTHS[ch] ?: DEFAULT_CHAR_WIDTH_16PX)
        }

        // 应用字距调整
        for (i in 0 until text.length - 1) {
            val pair = text.substring(i, i + 2)
            val kern = KERNING_PAIRS_16PX[pair]
            if (kern != null) {
                width += kern
            }
        }

        return width * scale
    }

    /**
     * 精确估算文本在 DOM 中的渲染宽度（模拟 getBoundingClientRect）
     *
     * mermaid-js 使用 DOM 的 getBoundingClientRect() 测量文本宽度，
     * 该方法的结果会包含空格与字母之间的 kerning，而 canvas.measureText() 不包含。
     * 此方法在 [estimateTextWidth] 基础上额外应用 [SPACE_KERNING_16PX]。
     */
    fun estimateDomTextWidth(text: String, fontSize: Double = 16.0): Double {
        val scale = fontSize / 16.0
        var width = 0.0

        // 累加各字符宽度
        for (ch in text) {
            width += (TREBUCHET_MS_16PX_WIDTHS[ch] ?: DEFAULT_CHAR_WIDTH_16PX)
        }

        // 应用标准字距调整（canvas-level kerning）
        for (i in 0 until text.length - 1) {
            val pair = text.substring(i, i + 2)
            val kern = KERNING_PAIRS_16PX[pair]
            if (kern != null) {
                width += kern
            }
        }

        // 应用 DOM 特有的空格 kerning
        for (i in 0 until text.length - 1) {
            val pair = text.substring(i, i + 2)
            val spaceKern = SPACE_KERNING_16PX[pair]
            if (spaceKern != null) {
                width += spaceKern
            }
        }

        return width * scale
    }

    /**
     * 估算文本渲染高度
     */
    fun estimateTextHeight(text: String, fontSize: Double = 16.0, lineHeight: Double = 1.5): Double {
        val lines = text.count { it == '\n' } + 1
        return lines * fontSize * lineHeight
    }
}
