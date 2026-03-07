package io.lugf027.github.mermaid.core.types

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.themes.ThemeVariables

/**
 * 图表定义核心接口 - 每种图表类型实现此接口。
 * 对应 mermaid-js 的 DiagramDefinition。
 * 每种图表类型由四个核心组件组成：detector + parser + db + renderer (+ 可选 styles)。
 */
interface DiagramDefinition {
    /** 解析器定义 */
    val parser: ParserDefinition

    /** 数据库（数据存储层） */
    val db: DiagramDB

    /** 渲染器 */
    val renderer: DiagramRenderer

    /** 样式提供器（可选） */
    val styles: DiagramStylesProvider?
        get() = null

    /** 初始化回调，在图表注册时调用 */
    fun init(config: MermaidConfig) {}
}

/**
 * 解析器定义接口。
 * 负责将 Mermaid 文本语法解析为结构化数据并填充到对应的 DB 中。
 */
interface ParserDefinition {
    /**
     * 解析输入文本。
     * 解析后的数据通过副作用填充到关联的 DiagramDB 中。
     * @param input 待解析的 Mermaid 图表文本（已去除 frontmatter/directive）
     */
    fun parse(input: String)
}

/**
 * 图表数据库接口 - 存储解析后的图表数据。
 * 每种图表类型有自己的 DB 实现，负责管理该类型的特定数据。
 * 对应 mermaid-js 的 DiagramDB。
 */
interface DiagramDB {
    /** 清除所有数据，恢复初始状态 */
    fun clear()

    /** 设置图表标题 */
    fun setDiagramTitle(title: String)

    /** 获取图表标题 */
    fun getDiagramTitle(): String

    /** 设置无障碍标题 */
    fun setAccTitle(title: String)

    /** 获取无障碍标题 */
    fun getAccTitle(): String

    /** 设置无障碍描述 */
    fun setAccDescription(desc: String)

    /** 获取无障碍描述 */
    fun getAccDescription(): String
}

/**
 * 图表渲染器接口 - 使用 Compose Canvas 绘制图表。
 * 负责将 DB 中的数据转换为可视化图形。
 */
interface DiagramRenderer {
    /**
     * 在 Compose Canvas DrawScope 中绘制图表。
     * @param drawScope Canvas 绘制作用域
     * @param db 图表数据
     * @param config 当前配置
     * @param theme 当前主题变量
     * @param textMeasurer 文本测量器
     * @param size 画布大小
     */
    fun draw(
        drawScope: DrawScope,
        db: DiagramDB,
        config: MermaidConfig,
        theme: ThemeVariables,
        textMeasurer: TextMeasurer,
        size: Size
    )
}

/**
 * 图表类型检测器函数类型。
 * 根据输入文本判断是否匹配该图表类型。
 * @return 如果匹配则返回 true
 */
typealias DiagramDetector = (text: String, config: MermaidConfig?) -> Boolean

/**
 * 图表懒加载器函数类型。
 * 返回 DiagramDefinition 实例，用于按需加载图表实现。
 */
typealias DiagramLoader = () -> DiagramDefinition

/**
 * 图表样式提供器接口。
 * 根据主题变量生成该图表类型特定的样式参数。
 */
interface DiagramStylesProvider {
    /**
     * 获取该图表类型的样式字符串（用于调试/日志）。
     * @param theme 主题变量
     * @return 样式描述
     */
    fun getStyles(theme: ThemeVariables): Map<String, Any>
}
