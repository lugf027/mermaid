package io.lugf027.github.mermaid.core.themes

/**
 * 主题管理器 - 对标 mermaid-js themes/index.js
 *
 * 根据配置选择主题并生成 CSS 样式字符串。
 */
object ThemeManager {

    /** 5 种内置主题 */
    private val themes = mapOf(
        "default" to DefaultTheme,
        "dark" to DarkTheme,
        "forest" to ForestTheme,
        "neutral" to NeutralTheme,
        "base" to BaseTheme,
    )

    /**
     * 根据主题名称获取主题变量
     */
    fun getThemeVariables(themeName: String?): ThemeVariables {
        val name = themeName ?: "default"
        val provider = themes[name] ?: DefaultTheme
        return provider.getVariables()
    }

    /**
     * 生成图表 CSS 样式
     */
    fun generateStyles(
        themeVariables: ThemeVariables,
        diagramType: String,
        diagramId: String
    ): String {
        val tv = themeVariables
        return buildString {
            // 通用样式
            appendLine("#${diagramId} {")
            appendLine("  font-family: ${tv.fontFamily};")
            appendLine("  font-size: ${tv.fontSize};")
            appendLine("  fill: ${tv.textColor};")
            appendLine("}")
            appendLine()

            // 节点样式
            appendLine("#${diagramId} .node rect,")
            appendLine("#${diagramId} .node circle,")
            appendLine("#${diagramId} .node ellipse,")
            appendLine("#${diagramId} .node polygon,")
            appendLine("#${diagramId} .node path {")
            appendLine("  fill: ${tv.nodeBkg};")
            appendLine("  stroke: ${tv.nodeBorder};")
            appendLine("  stroke-width: 1px;")
            appendLine("}")
            appendLine()

            // 节点文字
            appendLine("#${diagramId} .node .label {")
            appendLine("  text-align: center;")
            appendLine("  fill: ${tv.primaryTextColor};")
            appendLine("}")
            appendLine()

            // 集群样式
            appendLine("#${diagramId} .cluster rect {")
            appendLine("  fill: ${tv.clusterBkg};")
            appendLine("  stroke: ${tv.clusterBorder};")
            appendLine("  stroke-width: 1px;")
            appendLine("}")
            appendLine()

            // 边样式
            appendLine("#${diagramId} .edgePath .path {")
            appendLine("  stroke: ${tv.lineColor};")
            appendLine("  stroke-width: 2px;")
            appendLine("}")
            appendLine()

            // 边标签样式
            appendLine("#${diagramId} .edgeLabel {")
            appendLine("  background-color: ${tv.edgeLabelBackground};")
            appendLine("  fill: ${tv.edgeLabelBackground};")
            appendLine("}")
            appendLine()

            // 箭头样式
            appendLine("#${diagramId} .arrowMarkerPath {")
            appendLine("  fill: ${tv.lineColor};")
            appendLine("}")
            appendLine()

            // 标题样式
            appendLine("#${diagramId} .titleText {")
            appendLine("  text-anchor: middle;")
            appendLine("  font-size: 18px;")
            appendLine("  fill: ${tv.titleColor};")
            appendLine("}")

            // 饼图特定样式
            if (diagramType == "pie") {
                appendLine()
                appendLine("#${diagramId} .pieCircle {")
                appendLine("  stroke: ${tv.pieStrokeColor};")
                appendLine("  stroke-width: ${tv.pieStrokeWidth};")
                appendLine("  opacity: ${tv.pieOpacity};")
                appendLine("}")
            }
        }
    }
}
