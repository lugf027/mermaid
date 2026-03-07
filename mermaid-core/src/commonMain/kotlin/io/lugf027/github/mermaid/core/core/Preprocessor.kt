package io.lugf027.github.mermaid.core.core

import io.lugf027.github.mermaid.core.types.DirectiveData
import io.lugf027.github.mermaid.core.types.FrontmatterData

/**
 * Mermaid 文本预处理器。
 * 负责提取 frontmatter（YAML）和 directive（%%{...}%%）指令，
 * 以及清理注释和空行。
 * 对应 mermaid-js 的 preprocess.ts 和 frontmatter.ts。
 */
object Preprocessor {

    /** Frontmatter 匹配正则（YAML 块） */
    private val FRONTMATTER_REGEX = Regex(
        """^---\s*\n([\s\S]*?)\n---\s*\n""",
        RegexOption.MULTILINE
    )

    /** Directive 匹配正则 %%{...}%% */
    private val DIRECTIVE_REGEX = Regex(
        """%%\{([\s\S]+?)\}%%"""
    )

    /** 单行注释正则 %% ... */
    private val COMMENT_REGEX = Regex("""%%(?!\{)[^\n]*""")

    /**
     * 预处理结果。
     */
    data class PreprocessResult(
        /** 清理后的文本（去除 frontmatter、directive、注释） */
        val cleanText: String,
        /** 提取的 frontmatter 数据 */
        val frontmatter: FrontmatterData?,
        /** 提取的 directive 列表 */
        val directives: List<DirectiveData>
    )

    /**
     * 预处理 Mermaid 文本。
     * @param text 原始文本
     * @return 预处理结果
     */
    fun preprocess(text: String): PreprocessResult {
        var remaining = text

        // 1. 提取 frontmatter
        val frontmatter = extractFrontmatter(remaining)
        if (frontmatter != null) {
            remaining = FRONTMATTER_REGEX.replace(remaining, "")
        }

        // 2. 提取 directives
        val directives = extractDirectives(remaining)
        remaining = DIRECTIVE_REGEX.replace(remaining, "")

        // 3. 清除注释
        remaining = COMMENT_REGEX.replace(remaining, "")

        // 4. 清理多余空行
        remaining = remaining.replace(Regex("""\n{3,}"""), "\n\n").trim()

        return PreprocessResult(
            cleanText = remaining,
            frontmatter = frontmatter,
            directives = directives
        )
    }

    /**
     * 提取 YAML frontmatter。
     */
    private fun extractFrontmatter(text: String): FrontmatterData? {
        val match = FRONTMATTER_REGEX.find(text) ?: return null
        val yamlContent = match.groupValues[1]

        // 简单的 YAML 键值解析（不使用完整 YAML 库）
        val config = mutableMapOf<String, Any?>()
        var title: String? = null
        var displayMode: String? = null

        yamlContent.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                val colonIdx = trimmed.indexOf(':')
                if (colonIdx > 0) {
                    val key = trimmed.substring(0, colonIdx).trim()
                    val value = trimmed.substring(colonIdx + 1).trim()
                        .removeSurrounding("\"").removeSurrounding("'")

                    when (key) {
                        "title" -> title = value
                        "displayMode" -> displayMode = value
                        else -> config[key] = value
                    }
                }
            }
        }

        return FrontmatterData(
            title = title,
            displayMode = displayMode,
            config = config
        )
    }

    /**
     * 提取所有 directive 指令。
     */
    private fun extractDirectives(text: String): List<DirectiveData> {
        return DIRECTIVE_REGEX.findAll(text).mapNotNull { match ->
            parseDirective(match.groupValues[1].trim())
        }.toList()
    }

    /**
     * 解析单个 directive 内容。
     * 格式: init: {"key": "value", ...} 或 type: value
     */
    private fun parseDirective(content: String): DirectiveData? {
        val colonIdx = content.indexOf(':')
        if (colonIdx <= 0) return null

        val type = content.substring(0, colonIdx).trim()
        val argsStr = content.substring(colonIdx + 1).trim()

        val args = mutableMapOf<String, Any?>()

        // 简单的 JSON-like 解析
        if (argsStr.startsWith("{") && argsStr.endsWith("}")) {
            val inner = argsStr.removeSurrounding("{", "}")
            // 简化解析：处理简单的 key:value 对
            inner.split(",").forEach { pair ->
                val pairColonIdx = pair.indexOf(':')
                if (pairColonIdx > 0) {
                    val key = pair.substring(0, pairColonIdx).trim()
                        .removeSurrounding("\"").removeSurrounding("'")
                    val value = pair.substring(pairColonIdx + 1).trim()
                        .removeSurrounding("\"").removeSurrounding("'")
                    args[key] = value
                }
            }
        } else {
            args["value"] = argsStr
        }

        return DirectiveData(type = type, args = args)
    }
}
