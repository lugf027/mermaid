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
            // CSS 样式全部在一行内（无换行符），匹配 mermaid-js 的输出格式
            append("#${id}{font-family:${tv.fontFamily};font-size:${tv.fontSize};fill:${tv.textColor};}")
            append("@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}")
            append("@keyframes dash{to{stroke-dashoffset:0;}}")
            append("#${id} .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}")
            append("#${id} .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}")
            append("#${id} .error-icon{fill:#552222;}")
            append("#${id} .error-text{fill:#552222;stroke:#552222;}")
            append("#${id} .edge-thickness-normal{stroke-width:1px;}")
            append("#${id} .edge-thickness-thick{stroke-width:3.5px;}")
            append("#${id} .edge-pattern-solid{stroke-dasharray:0;}")
            append("#${id} .edge-thickness-invisible{stroke-width:0;fill:none;}")
            append("#${id} .edge-pattern-dashed{stroke-dasharray:3;}")
            append("#${id} .edge-pattern-dotted{stroke-dasharray:2;}")
            append("#${id} .marker{fill:${tv.lineColor};stroke:${tv.lineColor};}")
            append("#${id} .marker.cross{stroke:${tv.lineColor};}")
            append("#${id} svg{font-family:${tv.fontFamily};font-size:${tv.fontSize};}")
            append("#${id} p{margin:0;}")
            append("#${id} .label{font-family:${tv.fontFamily};color:${tv.textColor};}")
            append("#${id} .cluster-label text{fill:${tv.textColor};}")
            append("#${id} .cluster-label span{color:${tv.textColor};}")
            append("#${id} .cluster-label span p{background-color:transparent;}")
            append("#${id} .label text,#${id} span{fill:${tv.textColor};color:${tv.textColor};}")
            append("#${id} .node rect,#${id} .node circle,#${id} .node ellipse,#${id} .node polygon,#${id} .node path{fill:${tv.nodeBkg};stroke:${tv.nodeBorder};stroke-width:1px;}")
            append("#${id} .rough-node .label text,#${id} .node .label text,#${id} .image-shape .label,#${id} .icon-shape .label{text-anchor:middle;}")
            append("#${id} .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}")
            append("#${id} .rough-node .label,#${id} .node .label,#${id} .image-shape .label,#${id} .icon-shape .label{text-align:center;}")
            append("#${id} .node.clickable{cursor:pointer;}")
            append("#${id} .root .anchor path{fill:${tv.lineColor}!important;stroke-width:0;stroke:${tv.lineColor};}")
            append("#${id} .arrowheadPath{fill:${tv.lineColor};}")
            append("#${id} .edgePath .path{stroke:${tv.lineColor};stroke-width:2.0px;}")
            append("#${id} .flowchart-link{stroke:${tv.lineColor};fill:none;}")
            append("#${id} .edgeLabel{background-color:rgba(232,232,232, 0.8);text-align:center;}")
            append("#${id} .edgeLabel p{background-color:rgba(232,232,232, 0.8);}")
            append("#${id} .edgeLabel rect{opacity:0.5;background-color:rgba(232,232,232, 0.8);fill:rgba(232,232,232, 0.8);}")
            append("#${id} .labelBkg{background-color:rgba(232, 232, 232, 0.5);}")
            append("#${id} .cluster rect{fill:${tv.clusterBkg};stroke:${tv.clusterBorder};stroke-width:1px;}")
            append("#${id} .cluster text{fill:${tv.titleColor};}")
            append("#${id} .cluster span{color:${tv.titleColor};}")
            // mermaid-js tooltip
            append("#${id} div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:${tv.fontFamily};font-size:12px;background:hsl(80, 100%, 96.2745098039%);border:1px solid #aaaa33;border-radius:2px;pointer-events:none;z-index:100;}")
            // mermaid-js 使用 .flowchartTitleText（不是 .titleText）
            append("#${id} .flowchartTitleText{text-anchor:middle;font-size:18px;fill:${tv.titleColor};}")
            append("#${id} rect.text{fill:none;stroke-width:0;}")
            append("#${id} .icon-shape,#${id} .image-shape{background-color:rgba(232,232,232, 0.8);text-align:center;}")
            append("#${id} .icon-shape p,#${id} .image-shape p{background-color:rgba(232,232,232, 0.8);padding:2px;}")
            append("#${id} .icon-shape rect,#${id} .image-shape rect{opacity:0.5;background-color:rgba(232,232,232, 0.8);fill:rgba(232,232,232, 0.8);}")
            append("#${id} .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}")
            append("#${id} .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}")
            append("#${id} :root{--mermaid-font-family:${tv.fontFamily};}")
            if (diagramType == "pie") {
                append("#${id} .pieCircle{stroke:${tv.pieStrokeColor};stroke-width:${tv.pieStrokeWidth};opacity:${tv.pieOpacity};}")
            }
            // 末尾换行符，使 </style> 在新行
            append("\n")
        }
    }
}
