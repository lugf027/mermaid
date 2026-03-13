package io.lugf027.github.mermaid.core.themes

/** 中性主题 - 对标 mermaid-js theme-neutral.js */
object NeutralTheme : ThemeProvider {
    override fun getVariables(): ThemeVariables = ThemeVariables(
        background = "#ffffff",
        primaryColor = "#eee",
        secondaryColor = "#d0d0d0",
        tertiaryColor = "#eeeeee",
        primaryBorderColor = "#999",
        primaryTextColor = "#333",
        secondaryBorderColor = "#aaa",
        secondaryTextColor = "#333",
        lineColor = "#666",
        textColor = "#000000",
        mainBkg = "#eee",
        nodeBorder = "#999",
        nodeBkg = "#eee",
        clusterBkg = "#d0d0d0",
        clusterBorder = "#aaa",
        titleColor = "#333",
        edgeLabelBackground = "#e8e8e8",
        noteBkgColor = "#fff5ad",
        noteTextColor = "#333",
        noteBorderColor = "#aaaa33",
        pie1 = "#555", pie2 = "#F4F4F4", pie3 = "#555",
        pie4 = "#BBB", pie5 = "#777", pie6 = "#999",
        pie7 = "#DDD", pie8 = "#FFF", pie9 = "#DDD",
        pie10 = "#BBB", pie11 = "#999", pie12 = "#555",
    )
}
