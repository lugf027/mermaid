package io.lugf027.github.mermaid.core.renderer.compose

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import io.lugf027.github.mermaid.core.types.TextDimensions

/**
 * 文本测量辅助工具。
 * 封装 TextMeasurer 进行文本宽高计算，
 * 这是像素级还原的关键——布局阶段需要准确的文本尺寸。
 */
class TextMeasureHelper(private val textMeasurer: TextMeasurer) {

    /**
     * 测量文本尺寸。
     * @param text 文本内容
     * @param fontSize 字号（sp）
     * @param fontFamily 字体族（当前忽略，使用默认字体）
     * @param maxWidth 最大宽度（用于自动换行），0 表示不限制
     * @return 文本尺寸
     */
    fun measure(
        text: String,
        fontSize: Float = 14f,
        fontFamily: String = "",
        maxWidth: Int = 0,
    ): TextDimensions {
        if (text.isEmpty()) return TextDimensions(0f, 0f, fontSize * 1.2f)

        val style = TextStyle(fontSize = fontSize.sp)
        val constraints = if (maxWidth > 0) {
            androidx.compose.ui.unit.Constraints(maxWidth = maxWidth)
        } else {
            androidx.compose.ui.unit.Constraints()
        }

        val result = textMeasurer.measure(text, style, constraints = constraints)
        return TextDimensions(
            width = result.size.width.toFloat(),
            height = result.size.height.toFloat(),
            lineHeight = fontSize * 1.2f,
        )
    }

    /**
     * 批量测量文本列表（缓存优化）。
     */
    fun measureAll(texts: List<String>, fontSize: Float = 14f): List<TextDimensions> {
        return texts.map { measure(it, fontSize) }
    }
}
