package io.lugf027.github.mermaid.core.core

import io.lugf027.github.mermaid.core.config.ConfigManager
import io.lugf027.github.mermaid.core.types.DiagramDB
import io.lugf027.github.mermaid.core.types.DiagramDefinition
import io.lugf027.github.mermaid.core.types.DiagramRenderer
import io.lugf027.github.mermaid.core.types.ParseResult

/**
 * Diagram 类 - 每个解析后的图表实例。
 * 封装图表的类型、解析器、DB 和渲染器引用。
 * 对应 mermaid-js 的 Diagram.ts。
 */
class Diagram private constructor(
    /** 图表类型标识 */
    val type: String,
    /** 图表定义 */
    private val definition: DiagramDefinition,
    /** 原始文本 */
    val text: String
) {
    /** 数据库引用 */
    val db: DiagramDB get() = definition.db

    /** 渲染器引用 */
    val renderer: DiagramRenderer get() = definition.renderer

    companion object {
        /**
         * 工厂方法：从文本创建 Diagram 实例。
         * 1. 预处理文本（提取 frontmatter/directive）
         * 2. 检测图表类型
         * 3. 加载对应的 DiagramDefinition
         * 4. 解析文本，填充 DB
         *
         * @param text 原始 Mermaid 图表文本
         * @return Diagram 实例
         */
        fun fromText(text: String): Diagram {
            // 预处理
            val preprocessed = Preprocessor.preprocess(text)

            // 应用 directive 配置
            preprocessed.directives.forEach { directive ->
                directive.args.forEach { (key, value) ->
                    ConfigManager.addDirective(key, value)
                }
            }

            // 获取处理后的文本（去除 frontmatter 和 directive）
            val cleanText = preprocessed.cleanText

            // 检测类型
            val type = DiagramRegistry.detectType(cleanText)

            // 获取图表定义
            val definition = DiagramRegistry.getDiagram(type)

            // 初始化图表
            definition.init(ConfigManager.getConfig())

            // 清除旧数据
            definition.db.clear()

            // 设置 frontmatter 中的标题（如果有）
            preprocessed.frontmatter?.title?.let {
                definition.db.setDiagramTitle(it)
            }

            // 解析文本
            definition.parser.parse(cleanText)

            return Diagram(type, definition, text)
        }

        /**
         * 尝试解析文本，返回解析结果。
         * 不抛出异常，而是返回结果对象。
         */
        fun tryParse(text: String): ParseResult {
            return try {
                val preprocessed = Preprocessor.preprocess(text)
                val type = DiagramRegistry.detectType(preprocessed.cleanText)
                ParseResult(diagramType = type, success = true)
            } catch (e: Exception) {
                ParseResult(
                    diagramType = "error",
                    success = false,
                    error = io.lugf027.github.mermaid.core.types.MermaidError(
                        message = e.message ?: "Unknown error"
                    )
                )
            }
        }
    }
}
