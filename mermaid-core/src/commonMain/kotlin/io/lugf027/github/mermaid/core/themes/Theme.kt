package io.lugf027.github.mermaid.core.themes

/**
 * 主题变量 - 对标 mermaid-js 主题系统中的颜色变量
 *
 * 包含所有颜色/样式变量，用于生成 CSS 和控制渲染器颜色。
 */
data class ThemeVariables(
    // 基础颜色
    val background: String = "#f4f4f4",
    val primaryColor: String = "#ECECFF",
    val secondaryColor: String = "#ffffde",
    val tertiaryColor: String = "#efefef",

    // 边框与文字颜色
    val primaryBorderColor: String = "#9370DB",
    val primaryTextColor: String = "#131313",
    val secondaryBorderColor: String = "#aaaa33",
    val secondaryTextColor: String = "#333",
    val tertiaryBorderColor: String = "#aaaaaa",
    val tertiaryTextColor: String = "#333",

    // 通用颜色
    val lineColor: String = "#333333",
    val textColor: String = "#333",
    val mainBkg: String = "#ECECFF",
    val nodeBorder: String = "#9370DB",
    val nodeBkg: String = "#ECECFF",
    val clusterBkg: String = "#ffffde",
    val clusterBorder: String = "#aaaa33",
    val titleColor: String = "#333",
    val edgeLabelBackground: String = "#e8e8e8",

    // 注释
    val noteBkgColor: String = "#fff5ad",
    val noteTextColor: String = "#333",
    val noteBorderColor: String = "#aaaa33",

    // 字体
    val fontFamily: String = "\"trebuchet ms\",verdana,arial,sans-serif",
    val fontSize: String = "16px",

    // 饼图颜色 (pie1 - pie12)
    val pie1: String = "#ECECFF",
    val pie2: String = "#ffffde",
    val pie3: String = "#8B7EC8",
    val pie4: String = "#D3D3FF",
    val pie5: String = "#E6E6A3",
    val pie6: String = "#A3A3D2",
    val pie7: String = "#C8C8FF",
    val pie8: String = "#9F8FDB",
    val pie9: String = "#8B8BFF",
    val pie10: String = "#BEBEFF",
    val pie11: String = "#6E5EAD",
    val pie12: String = "#7B7BDB",

    // 饼图标签颜色
    val pieTitleTextSize: String = "25px",
    val pieTitleTextColor: String = "#333",
    val pieSectionTextSize: String = "17px",
    val pieSectionTextColor: String = "#333",
    val pieLegendTextSize: String = "17px",
    val pieLegendTextColor: String = "#333",
    val pieStrokeColor: String = "black",
    val pieStrokeWidth: String = "2px",
    val pieOuterStrokeWidth: String = "2px",
    val pieOuterStrokeColor: String = "black",
    val pieOpacity: String = "0.7",

    // 流程图特定
    val labelBackground: String = "#e8e8e8",
    val labelColor: String = "#333",

    // 时序图特定
    val actorBorder: String = "#9370DB",
    val actorBkg: String = "#ECECFF",
    val actorTextColor: String = "#333",
    val actorLineColor: String = "#333",
    val activationBorderColor: String = "#9370DB",
    val activationBkgColor: String = "#ECECFF",
    val sequenceNumberColor: String = "white",
    val signalColor: String = "#333",
    val signalTextColor: String = "#333",
    val loopTextColor: String = "#333",

    // 甘特图特定
    val sectionBkgColor: String = "rgba(102, 102, 255, 0.49)",
    val altSectionBkgColor: String = "white",
    val sectionBkgColor2: String = "#EAE8D9",
    val taskBorderColor: String = "#534fbc",
    val taskBkgColor: String = "#8a90dd",
    val taskTextLightColor: String = "white",
    val taskTextColor: String = "white",
    val taskTextDarkColor: String = "#333",
    val taskTextOutsideColor: String = "#333",
    val taskTextClickableColor: String = "#003163",
    val activeTaskBorderColor: String = "#534fbc",
    val activeTaskBkgColor: String = "#bfc7ff",
    val doneTaskBkgColor: String = "#d9e5ff",
    val doneTaskBorderColor: String = "grey",
    val critBorderColor: String = "#ff8888",
    val critBkgColor: String = "red",
    val todayLineColor: String = "red",

    // Git 图特定
    val git0: String = "#ECECFF",
    val git1: String = "#ffffde",
    val git2: String = "#AACCFF",
    val git3: String = "#FFD8B1",
    val git4: String = "#DDFFDD",
    val git5: String = "#FFB6C1",
    val git6: String = "#E6E6FA",
    val git7: String = "#DDA0DD",
    val gitBranchLabel0: String = "#131313",
    val gitBranchLabel1: String = "#131313",
    val gitBranchLabel2: String = "#131313",
    val gitBranchLabel3: String = "#131313",
    val gitInv0: String = "#131313",
) {
    /** 获取饼图颜色列表 */
    fun getPieColors(): List<String> = listOf(
        pie1, pie2, pie3, pie4, pie5, pie6,
        pie7, pie8, pie9, pie10, pie11, pie12
    )

    /** 按索引获取饼图颜色（循环） */
    fun getPieColor(index: Int): String = getPieColors()[index % 12]
}
