package io.lugf027.github.mermaid.core.themes

/** 基础主题（可自定义）- 对标 mermaid-js theme-base.js */
object BaseTheme : ThemeProvider {
    override fun getVariables(): ThemeVariables = ThemeVariables(
        background = "#f4f4f4",
        primaryColor = "#fff4dd",
        secondaryColor = "#d4e4ff",
        tertiaryColor = "#ffefef",
        primaryBorderColor = "#e8c98e",
        primaryTextColor = "#333",
        secondaryBorderColor = "#a0b0c0",
        secondaryTextColor = "#333",
        lineColor = "#0b0b0b",
        textColor = "#333",
        mainBkg = "#fff4dd",
        nodeBorder = "#e8c98e",
        nodeBkg = "#fff4dd",
        clusterBkg = "#d4e4ff",
        clusterBorder = "#a0b0c0",
        titleColor = "#333",
        edgeLabelBackground = "#e8e8e8",
        noteBkgColor = "#fff5ad",
        noteTextColor = "#333",
        noteBorderColor = "#aaaa33",
        pie1 = "#fff4dd", pie2 = "#d4e4ff", pie3 = "#ffefef",
        pie4 = "#EDE4CC", pie5 = "#BED4EE", pie6 = "#EEDFDF",
        pie7 = "#F5E8C8", pie8 = "#E8D4B8", pie9 = "#D8E4D8",
        pie10 = "#E8D8BB", pie11 = "#C8B4A8", pie12 = "#C8D4C8",
    )
}
