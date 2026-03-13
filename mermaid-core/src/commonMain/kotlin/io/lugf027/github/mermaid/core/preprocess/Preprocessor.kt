package io.lugf027.github.mermaid.core.preprocess

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.util.TextUtils

/**
 * 预处理器主入口 - 对标 mermaid-js preprocess.ts
 *
 * 处理流程：清理文本 → 提取 frontmatter → 提取指令 → 移除注释 → 输出
 */
object Preprocessor {

    /**
     * 预处理结果
     */
    data class PreprocessResult(
        /** 清理后的图表代码（已移除 frontmatter、指令、注释） */
        val code: String,
        /** 从 frontmatter 提取的标题 */
        val title: String?,
        /** 从 frontmatter 和指令中提取的配置 */
        val config: MermaidConfig?,
        /** 从指令中提取的配置列表 */
        val directives: List<MermaidConfig>
    )

    /**
     * 执行预处理
     *
     * @param text 原始 Mermaid 输入文本
     * @return 预处理结果
     */
    fun process(text: String): PreprocessResult {
        // 1. 清理文本
        var code = TextUtils.cleanupText(text)

        // 2. 提取 frontmatter
        val frontmatter = FrontmatterParser.parse(code)
        code = FrontmatterParser.removeFrontmatter(code)

        // 3. 提取指令
        val directives = DirectiveParser.extractDirectives(code)
        code = DirectiveParser.removeDirectives(code)

        // 4. 移除注释
        code = CommentCleaner.removeComments(code)

        // 5. 清理多余空行
        code = code.trim()

        return PreprocessResult(
            code = code,
            title = frontmatter?.title,
            config = frontmatter?.config,
            directives = directives
        )
    }
}
