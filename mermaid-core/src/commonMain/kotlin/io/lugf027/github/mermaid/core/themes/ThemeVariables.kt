package io.lugf027.github.mermaid.core.themes

import kotlinx.serialization.Serializable

/**
 * 主题变量数据类。
 * 包含所有颜色/字体/尺寸变量，用于控制图表的视觉外观。
 * 对应 mermaid-js 的 ThemeVariables。
 */
@Serializable
data class ThemeVariables(
    // ─── 基础颜色 ──────────────────────────────────────────────
    val primaryColor: String = "#326CE5",
    val primaryTextColor: String = "#ffffff",
    val primaryBorderColor: String = "#2456B3",
    val secondaryColor: String = "#ffffde",
    val secondaryTextColor: String = "#333333",
    val secondaryBorderColor: String = "#aaaa33",
    val tertiaryColor: String = "#f4f4f4",
    val tertiaryTextColor: String = "#333333",
    val tertiaryBorderColor: String = "#cccccc",

    // ─── 背景/前景 ─────────────────────────────────────────────
    val background: String = "#ffffff",
    val mainBkg: String = "#ECECFF",
    val secondBkg: String = "#ffffde",
    val lineColor: String = "#333333",
    val border1: String = "#9370DB",
    val border2: String = "#aaaa33",
    val textColor: String = "#333333",

    // ─── 注释/标签 ─────────────────────────────────────────────
    val noteBkgColor: String = "#fff5ad",
    val noteTextColor: String = "#333333",
    val noteBorderColor: String = "#aaaa33",
    val labelColor: String = "#333333",
    val labelTextColor: String = "#333333",
    val labelBoxBkgColor: String = "#ECECFF",
    val labelBoxBorderColor: String = "#326CE5",

    // ─── 字体 ──────────────────────────────────────────────────
    val fontFamily: String = "\"trebuchet ms\", verdana, arial, sans-serif",
    val fontSize: String = "16px",

    // ─── 流程图颜色 ─────────────────────────────────────────────
    val nodeBkg: String = "#ECECFF",
    val nodeBorder: String = "#9370DB",
    val clusterBkg: String = "#ffffde",
    val clusterBorder: String = "#aaaa33",
    val edgeLabelBackground: String = "#e8e8e8",
    val defaultLinkColor: String = "#333333",

    // ─── 时序图颜色 ─────────────────────────────────────────────
    val actorBkg: String = "#ECECFF",
    val actorBorder: String = "#9370DB",
    val actorTextColor: String = "#333333",
    val actorLineColor: String = "#666666",
    val signalColor: String = "#333333",
    val signalTextColor: String = "#333333",
    val activationBkgColor: String = "#f4f4f4",
    val activationBorderColor: String = "#666666",
    val sequenceNumberColor: String = "#ffffff",

    // ─── 饼图颜色 ──────────────────────────────────────────────
    val pie1: String = "#326CE5",
    val pie2: String = "#009688",
    val pie3: String = "#FF6F00",
    val pie4: String = "#795548",
    val pie5: String = "#9C27B0",
    val pie6: String = "#EF5350",
    val pie7: String = "#FFB300",
    val pie8: String = "#43A047",
    val pie9: String = "#1565C0",
    val pie10: String = "#AD1457",
    val pie11: String = "#00ACC1",
    val pie12: String = "#D4E157",
    val pieTitleTextSize: String = "25px",
    val pieTitleTextColor: String = "#333333",
    val pieSectionTextSize: String = "17px",
    val pieSectionTextColor: String = "#ffffff",
    val pieLegendTextSize: String = "17px",
    val pieLegendTextColor: String = "#333333",
    val pieStrokeColor: String = "#ffffff",
    val pieStrokeWidth: String = "2px",
    val pieOuterStrokeColor: String = "#ffffff",
    val pieOuterStrokeWidth: String = "2px",
    val pieOpacity: String = "0.7",

    // ─── 甘特图颜色 ─────────────────────────────────────────────
    val sectionBkgColor: String = "#fafafa",
    val altSectionBkgColor: String = "#ffffff",
    val sectionBkgColor2: String = "#EAE8D9",
    val taskBorderColor: String = "#534fbc",
    val taskBkgColor: String = "#8a90dd",
    val activeTaskBorderColor: String = "#534fbc",
    val activeTaskBkgColor: String = "#bfc7ff",
    val gridColor: String = "#cccccc",
    val doneTaskBkgColor: String = "#cccccc",
    val doneTaskBorderColor: String = "#666666",
    val critBorderColor: String = "#ff8888",
    val critBkgColor: String = "#880000",
    val todayLineColor: String = "#DB5757",
    val taskTextColor: String = "#333333",
    val taskTextOutsideColor: String = "#333333",
    val taskTextLightColor: String = "#ffffff",
    val taskTextDarkColor: String = "#333333",
    val taskTextClickableColor: String = "#003163",

    // ─── 状态图颜色 ─────────────────────────────────────────────
    val transitionColor: String = "#333333",
    val transitionLabelColor: String = "#333333",
    val stateLabelColor: String = "#333333",
    val stateBkg: String = "#ECECFF",
    val innerEndBackground: String = "#333333",
    val specialStateColor: String = "#333333",
    val compositeBackground: String = "#f4f4f4",
    val compositeBorder: String = "#333333",
    val compositeTitleBackground: String = "#e8e8e8",

    // ─── 类图颜色 ──────────────────────────────────────────────
    val classText: String = "#333333",

    // ─── Git 颜色 ──────────────────────────────────────────────
    val git0: String = "#326CE5",
    val git1: String = "#009688",
    val git2: String = "#FF6F00",
    val git3: String = "#795548",
    val git4: String = "#9C27B0",
    val git5: String = "#EF5350",
    val git6: String = "#FFB300",
    val git7: String = "#43A047",
    val gitInv0: String = "#ffffff",
    val gitInv1: String = "#ffffff",
    val gitInv2: String = "#ffffff",
    val gitInv3: String = "#ffffff",
    val gitInv4: String = "#ffffff",
    val gitInv5: String = "#ffffff",
    val gitInv6: String = "#ffffff",
    val gitInv7: String = "#ffffff",
    val gitBranchLabel0: String = "#ffffff",
    val gitBranchLabel1: String = "#ffffff",
    val gitBranchLabel2: String = "#ffffff",
    val gitBranchLabel3: String = "#ffffff",
    val gitBranchLabel4: String = "#ffffff",
    val gitBranchLabel5: String = "#ffffff",
    val gitBranchLabel6: String = "#ffffff",
    val gitBranchLabel7: String = "#ffffff",
    val tagLabelColor: String = "#333333",
    val tagLabelBackground: String = "#ffffde",
    val tagLabelBorder: String = "#aaaa33",
    val tagLabelFontSize: String = "10px",
    val commitLabelColor: String = "#333333",
    val commitLabelBackground: String = "#e8e8e8",
    val commitLabelFontSize: String = "10px",

    // ─── 需求图颜色 ─────────────────────────────────────────────
    val requirementBackground: String = "#f9f9f9",
    val requirementBorderColor: String = "#bbb",
    val requirementTextColor: String = "#333",
    val relationColor: String = "#333",
    val relationLabelBackground: String = "#e8e8e8",
    val relationLabelColor: String = "#333",
)

/**
 * 获取饼图扇区颜色列表。
 */
fun ThemeVariables.getPieColors(): List<String> {
    return listOf(
        pie1, pie2, pie3, pie4, pie5, pie6,
        pie7, pie8, pie9, pie10, pie11, pie12
    )
}

/**
 * 获取 Git 分支颜色列表。
 */
fun ThemeVariables.getGitColors(): List<String> {
    return listOf(git0, git1, git2, git3, git4, git5, git6, git7)
}
