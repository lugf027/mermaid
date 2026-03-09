package io.lugf027.github.mermaid.core.preprocess

import io.lugf027.github.mermaid.core.config.MermaidConfig

/**
 * YAML Frontmatter 解析器 - 对标 mermaid-js frontmatter.ts
 *
 * 解析 --- 块内的 title/config/displayMode 字段。
 * 使用简化的 YAML 解析（不引入完整 YAML 库）。
 */
object FrontmatterParser {

    /**
     * Frontmatter 数据
     */
    data class Frontmatter(
        val title: String? = null,
        val config: MermaidConfig? = null,
        val displayMode: String? = null
    )

    private val FRONTMATTER_REGEX = Regex("""^---\s*\n([\s\S]*?)\n---\s*\n?""")

    /**
     * 解析 frontmatter
     */
    fun parse(text: String): Frontmatter? {
        val match = FRONTMATTER_REGEX.find(text) ?: return null
        val content = match.groupValues[1]

        var title: String? = null
        var displayMode: String? = null

        // 简化的 YAML key: value 解析
        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            val colonIdx = trimmed.indexOf(':')
            if (colonIdx < 0) continue

            val key = trimmed.substring(0, colonIdx).trim()
            val value = trimmed.substring(colonIdx + 1).trim().removeSurrounding("\"").removeSurrounding("'")

            when (key) {
                "title" -> title = value
                "displayMode" -> displayMode = value
            }
        }

        return Frontmatter(title = title, displayMode = displayMode)
    }

    /**
     * 从文本中移除 frontmatter 块
     */
    fun removeFrontmatter(text: String): String {
        return FRONTMATTER_REGEX.replaceFirst(text, "")
    }
}
