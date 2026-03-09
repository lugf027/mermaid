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
     * 生成图表 CSS 样式 - 对齐 mermaid-js 的完整样式规则集
     */
    fun generateStyles(
        themeVariables: ThemeVariables,
        diagramType: String,
        diagramId: String
    ): String {
        val tv = themeVariables
        val id = diagramId
        return buildString {
            // 根元素样式
            appendLine("#${id}{font-family:${tv.fontFamily};font-size:${tv.fontSize};fill:${tv.textColor};}")

            // 动画 keyframes
            appendLine("@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}")
            appendLine("@keyframes dash{to{stroke-dashoffset:0;}}")

            // 边动画样式
            appendLine("#${id} .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}")
            appendLine("#${id} .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}")

            // 错误图标和文本
            appendLine("#${id} .error-icon{fill:#552222;}")
            appendLine("#${id} .error-text{fill:#552222;stroke:#552222;}")

            // 边粗细和模式样式
            appendLine("#${id} .edge-thickness-normal{stroke-width:1px;}")
            appendLine("#${id} .edge-thickness-thick{stroke-width:3.5px;}")
            appendLine("#${id} .edge-pattern-solid{stroke-dasharray:0;}")
            appendLine("#${id} .edge-thickness-invisible{stroke-width:0;fill:none;}")
            appendLine("#${id} .edge-pattern-dashed{stroke-dasharray:3;}")
            appendLine("#${id} .edge-pattern-dotted{stroke-dasharray:2;}")

            // marker 样式
            appendLine("#${id} .marker{fill:${tv.lineColor};stroke:${tv.lineColor};}")
            appendLine("#${id} .marker.cross{stroke:${tv.lineColor};}")

            // svg 内层字体继承
            appendLine("#${id} svg{font-family:${tv.fontFamily};font-size:${tv.fontSize};}")

            // 段落
            appendLine("#${id} p{margin:0;}")

            // 标签样式
            appendLine("#${id} .label{font-family:${tv.fontFamily};color:${tv.textColor};}")

            // 集群标签
            appendLine("#${id} .cluster-label text{fill:${tv.textColor};}")
            appendLine("#${id} .cluster-label span{color:${tv.textColor};}")
            appendLine("#${id} .cluster-label span p{background-color:transparent;}")

            // label text 和 span
            appendLine("#${id} .label text,#${id} span{fill:${tv.textColor};color:${tv.textColor};}")

            // 节点形状样式
            appendLine("#${id} .node rect,#${id} .node circle,#${id} .node ellipse,#${id} .node polygon,#${id} .node path{fill:${tv.nodeBkg};stroke:${tv.nodeBorder};stroke-width:1px;}")

            // 节点标签居中
            appendLine("#${id} .rough-node .label text,#${id} .node .label text,#${id} .image-shape .label,#${id} .icon-shape .label{text-anchor:middle;}")

            // katex 路径
            appendLine("#${id} .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}")

            // 标签对齐
            appendLine("#${id} .rough-node .label,#${id} .node .label,#${id} .image-shape .label,#${id} .icon-shape .label{text-align:center;}")

            // 可点击节点
            appendLine("#${id} .node.clickable{cursor:pointer;}")

            // 锚点路径
            appendLine("#${id} .root .anchor path{fill:${tv.lineColor}!important;stroke-width:0;stroke:${tv.lineColor};}")

            // arrowheadPath
            appendLine("#${id} .arrowheadPath{fill:${tv.lineColor};}")

            // 边路径样式
            appendLine("#${id} .edgePath .path{stroke:${tv.lineColor};stroke-width:2.0px;}")

            // flowchart-link
            appendLine("#${id} .flowchart-link{stroke:${tv.lineColor};fill:none;}")

            // 边标签
            appendLine("#${id} .edgeLabel{background-color:rgba(232,232,232, 0.8);text-align:center;}")
            appendLine("#${id} .edgeLabel p{background-color:rgba(232,232,232, 0.8);}")
            appendLine("#${id} .edgeLabel rect{opacity:0.5;background-color:rgba(232,232,232, 0.8);fill:rgba(232,232,232, 0.8);}")

            // labelBkg
            appendLine("#${id} .labelBkg{background-color:rgba(232, 232, 232, 0.5);}")

            // 集群样式
            appendLine("#${id} .cluster rect{fill:${tv.clusterBkg};stroke:${tv.clusterBorder};stroke-width:1px;}")
            appendLine("#${id} .cluster text{fill:${tv.titleColor};}")

            // 集群 span
            appendLine("#${id} .cluster span{color:${tv.titleColor};}")

            // 标题样式
            appendLine("#${id} .titleText{text-anchor:middle;font-size:18px;fill:${tv.titleColor};}")

            // 饼图特定样式
            if (diagramType == "pie") {
                appendLine("#${id} .pieCircle{stroke:${tv.pieStrokeColor};stroke-width:${tv.pieStrokeWidth};opacity:${tv.pieOpacity};}")
            }
        }
    }
}
