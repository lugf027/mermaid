package io.lugf027.github.mermaid.core.preprocess

import io.lugf027.github.mermaid.core.config.MermaidConfig
import kotlinx.serialization.json.Json

/**
 * 指令解析器 - 对标 mermaid-js preprocess.ts 的 processDirectives
 *
 * 提取 %%{init: {...}}%% 和 %%{wrap}%% 指令。
 */
object DirectiveParser {

    private val DIRECTIVE_REGEX = Regex("""%%\{([\s\S]*?)\}%%""")
    private val INIT_REGEX = Regex("""init\s*:\s*(\{[\s\S]*\})""")

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * 从文本中提取所有指令配置
     */
    fun extractDirectives(text: String): List<MermaidConfig> {
        val directives = mutableListOf<MermaidConfig>()

        DIRECTIVE_REGEX.findAll(text).forEach { match ->
            val content = match.groupValues[1].trim()

            // 处理 init 指令
            val initMatch = INIT_REGEX.find(content)
            if (initMatch != null) {
                try {
                    val jsonStr = initMatch.groupValues[1]
                    val config = json.decodeFromString<MermaidConfig>(jsonStr)
                    directives.add(config)
                } catch (_: Exception) {
                    // 忽略无法解析的指令
                }
            }

            // wrap 指令转换为配置
            if (content == "wrap") {
                directives.add(MermaidConfig(wrap = true))
            }
        }

        return directives
    }

    /**
     * 从文本中移除所有指令
     */
    fun removeDirectives(text: String): String {
        return DIRECTIVE_REGEX.replace(text, "").trim()
    }
}
