package io.lugf027.github.mermaid.core.themes

/**
 * 默认主题 - 对标 mermaid-js theme-default.js
 */
object DefaultTheme : ThemeProvider {
    override fun getVariables(): ThemeVariables = ThemeVariables(
        background = "white",
        primaryColor = "#ECECFF",
        secondaryColor = "#ffffde",
        tertiaryColor = "#8B7EC8",
        primaryBorderColor = "#9370DB",
        primaryTextColor = "#131313",
        secondaryBorderColor = "#aaaa33",
        secondaryTextColor = "#333",
        lineColor = "#333333",
        textColor = "#333",
        mainBkg = "#ECECFF",
        nodeBorder = "#9370DB",
        nodeBkg = "#ECECFF",
        clusterBkg = "#ffffde",
        clusterBorder = "#aaaa33",
        titleColor = "#333",
        edgeLabelBackground = "#e8e8e8",
        noteBkgColor = "#fff5ad",
        noteTextColor = "#333",
        noteBorderColor = "#aaaa33",
        // 饼图颜色
        pie1 = "#ECECFF", pie2 = "#ffffde", pie3 = "#8B7EC8",
        pie4 = "#D3D3FF", pie5 = "#E6E6A3", pie6 = "#A3A3D2",
        pie7 = "#C8C8FF", pie8 = "#9F8FDB", pie9 = "#8B8BFF",
        pie10 = "#BEBEFF", pie11 = "#6E5EAD", pie12 = "#7B7BDB",
    )
}
