package io.lugf027.github.mermaid.core.themes

/** 森林主题 - 对标 mermaid-js theme-forest.js */
object ForestTheme : ThemeProvider {
    override fun getVariables(): ThemeVariables = ThemeVariables(
        background = "white",
        primaryColor = "#cde498",
        secondaryColor = "#cdffb2",
        tertiaryColor = "#e0f5c7",
        primaryBorderColor = "#13540c",
        primaryTextColor = "#131313",
        secondaryBorderColor = "#6eaa49",
        secondaryTextColor = "#333",
        lineColor = "#000000",
        textColor = "#000000",
        mainBkg = "#cde498",
        nodeBorder = "#13540c",
        nodeBkg = "#cde498",
        clusterBkg = "#cdffb2",
        clusterBorder = "#6eaa49",
        titleColor = "#333",
        edgeLabelBackground = "#e8e8e8",
        noteBkgColor = "#fff5ad",
        noteTextColor = "#333",
        noteBorderColor = "#aaaa33",
        pie1 = "#cde498", pie2 = "#cdffb2", pie3 = "#e0f5c7",
        pie4 = "#8CB65A", pie5 = "#96D97B", pie6 = "#7A9E58",
        pie7 = "#BDE498", pie8 = "#98C498", pie9 = "#68C498",
        pie10 = "#7EC458", pie11 = "#5EA458", pie12 = "#48A468",
    )
}
