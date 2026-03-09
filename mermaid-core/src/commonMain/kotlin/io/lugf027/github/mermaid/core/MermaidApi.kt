package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.config.ConfigManager
import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.detect.DiagramTypeDetector
import io.lugf027.github.mermaid.core.diagram.DiagramOrchestration
import io.lugf027.github.mermaid.core.diagram.DiagramRegistry
import io.lugf027.github.mermaid.core.diagram.error.ErrorDiagram
import io.lugf027.github.mermaid.core.preprocess.Preprocessor
import io.lugf027.github.mermaid.core.rendering.shapes.ShapeRegistry
import io.lugf027.github.mermaid.core.rendering.svg.SvgSerializer
import io.lugf027.github.mermaid.core.themes.ThemeManager
import io.lugf027.github.mermaid.core.util.IdGenerator
import io.lugf027.github.mermaid.core.util.Logger

/**
 * 公共 API 入口 - 对标 mermaid-js mermaidAPI.ts
 *
 * 提供 Mermaid 图表的解析、渲染和 SVG 导出功能。
 *
 * 典型用法：
 * ```kotlin
 * // 初始化
 * MermaidApi.initialize(MermaidConfig(theme = "dark"))
 *
 * // 渲染为 SVG 字符串
 * val svg = MermaidApi.renderToSvg("pie\\n  \"Dogs\" : 30\\n  \"Cats\" : 70")
 *
 * // 或分步执行
 * val diagram = MermaidApi.parse("flowchart LR\\n  A --> B")
 * val svgRoot = MermaidApi.render(diagram)
 * val svgString = SvgSerializer.serialize(svgRoot)
 * ```
 */
object MermaidApi {

    private val log = Logger("MermaidApi")

    /** 是否已初始化 */
    private var initialized = false

    /**
     * 初始化 Mermaid 库
     *
     * 设置全局配置并注册所有图表类型。
     * 可多次调用，后续调用会更新配置。
     *
     * @param config 全局配置（可选）
     */
    fun initialize(config: MermaidConfig? = null) {
        log.info("Initializing MermaidApi")

        // 设置配置
        if (config != null) {
            ConfigManager.setSiteConfig(config)
            ConfigManager.saveConfigFromInitialize(config)
        }

        // 注册所有图表
        DiagramOrchestration.registerAll()

        // 注册内置形状
        ShapeRegistry.registerBuiltinShapes()

        initialized = true
        log.info("MermaidApi initialized successfully")
    }

    /**
     * 解析 Mermaid 文本，返回 Diagram 对象
     *
     * @param text 原始 Mermaid 输入文本
     * @return 解析后的 Diagram 对象
     * @throws IllegalArgumentException 如果图表类型无法识别
     */
    fun parse(text: String): Diagram {
        ensureInitialized()

        // 1. 预处理
        val preprocessResult = Preprocessor.process(text)
        val code = preprocessResult.code

        // 2. 应用指令配置
        ConfigManager.reset()
        for (directive in preprocessResult.directives) {
            ConfigManager.addDirective(directive)
        }
        if (preprocessResult.config != null) {
            ConfigManager.addDirective(preprocessResult.config)
        }

        val config = ConfigManager.getConfig()

        // 3. 检测图表类型
        val diagramType = DiagramTypeDetector.detect(code)
        log.info("Detected diagram type: $diagramType")

        // 4. 获取图表定义
        val definition = DiagramRegistry.get(diagramType)
            ?: DiagramRegistry.get("error")
            ?: ErrorDiagram.definition()

        // 5. 创建 DB 并解析
        val db = definition.dbFactory()

        // 解析
        try {
            definition.parser.parse(code, db)
        } catch (e: Exception) {
            log.error("Parse error for $diagramType: ${e.message}")
            // 如果不是错误图表，回退到错误图表
            if (diagramType != "error") {
                val errorDef = ErrorDiagram.definition()
                val errorDb = errorDef.dbFactory()
                return Diagram(
                    type = "error",
                    text = text,
                    code = code,
                    db = errorDb,
                    parser = errorDef.parser,
                    renderer = errorDef.renderer,
                    config = config
                )
            }
        }

        // 设置从 frontmatter 提取的标题（在解析之后，避免被 parser.clear() 覆盖）
        if (preprocessResult.title != null) {
            db.setDiagramTitle(preprocessResult.title)
        }

        return Diagram(
            type = diagramType,
            text = text,
            code = code,
            db = db,
            parser = definition.parser,
            renderer = definition.renderer,
            config = config
        )
    }

    /**
     * 渲染 Diagram 为 SVG Root 节点
     *
     * @param diagram 已解析的图表对象
     * @param diagramId SVG 元素 ID（可选，自动生成）
     * @return SVG 根节点
     */
    fun render(diagram: Diagram, diagramId: String? = null): io.lugf027.github.mermaid.core.rendering.svg.SvgRoot {
        ensureInitialized()

        val id = diagramId ?: IdGenerator.next("mermaid")
        val themeVariables = ThemeManager.getThemeVariables(diagram.config.theme)

        log.info("Rendering diagram: type=${diagram.type}, id=$id")

        return diagram.renderer.draw(
            db = diagram.db,
            config = diagram.config,
            themeVariables = themeVariables,
            diagramId = id
        )
    }

    /**
     * 一步完成：解析文本并渲染为 SVG 字符串
     *
     * @param text 原始 Mermaid 输入文本
     * @param diagramId SVG 元素 ID（可选）
     * @param indent 是否格式化 SVG 输出
     * @return SVG XML 字符串
     */
    fun renderToSvg(text: String, diagramId: String? = null, indent: Boolean = false): String {
        val diagram = parse(text)
        val svgRoot = render(diagram, diagramId)
        return SvgSerializer.serialize(svgRoot, indent)
    }

    /**
     * 一步完成：解析文本并渲染为 SVG 内容字符串（不含 XML 声明）
     *
     * 适用于嵌入 HTML 的场景。
     *
     * @param text 原始 Mermaid 输入文本
     * @param diagramId SVG 元素 ID（可选）
     * @param indent 是否格式化 SVG 输出
     * @return SVG 内容字符串
     */
    fun renderToSvgContent(text: String, diagramId: String? = null, indent: Boolean = false): String {
        val diagram = parse(text)
        val svgRoot = render(diagram, diagramId)
        return SvgSerializer.serializeContent(svgRoot, indent)
    }

    /**
     * 重置 MermaidApi 状态
     *
     * 清空所有注册、重置配置和 ID 生成器。主要用于测试。
     */
    fun reset() {
        DiagramOrchestration.reset()
        ConfigManager.reset()
        IdGenerator.reset()
        initialized = false
    }

    /**
     * 获取当前配置
     */
    fun getConfig(): MermaidConfig = ConfigManager.getConfig()

    /**
     * 获取所有已注册的图表类型
     */
    fun getRegisteredDiagramTypes(): Set<String> = DiagramRegistry.getRegisteredIds()

    /** 确保已初始化 */
    private fun ensureInitialized() {
        if (!initialized) {
            initialize()
        }
    }
}
