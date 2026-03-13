package io.lugf027.github.mermaid.core.themes

/** 暗色主题 - 对标 mermaid-js theme-dark.js */
object DarkTheme : ThemeProvider {
    override fun getVariables(): ThemeVariables = ThemeVariables(
        background = "#333",
        primaryColor = "#1f2020",
        secondaryColor = "#4f4f4f",
        tertiaryColor = "#3f5258",
        primaryBorderColor = "#cccccc",
        primaryTextColor = "#e0dfdf",
        secondaryBorderColor = "#666",
        secondaryTextColor = "#e0dfdf",
        lineColor = "lightgrey",
        textColor = "#ccc",
        mainBkg = "#1f2020",
        nodeBorder = "#ccc",
        nodeBkg = "#1f2020",
        clusterBkg = "#555",
        clusterBorder = "#999",
        titleColor = "#F9FFFE",
        edgeLabelBackground = "#333",
        noteBkgColor = "#fff5ad",
        noteTextColor = "#333",
        noteBorderColor = "#aaaa33",
        pie1 = "#1f2020", pie2 = "#0b0000", pie3 = "#4d1037",
        pie4 = "#3f5258", pie5 = "#4f2f1b", pie6 = "#6e0a0a",
        pie7 = "#3b0048", pie8 = "#995a01", pie9 = "#154706",
        pie10 = "#161722", pie11 = "#00296f", pie12 = "#01629c",
    )
}
