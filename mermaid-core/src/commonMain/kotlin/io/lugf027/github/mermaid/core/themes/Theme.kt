package io.lugf027.github.mermaid.core.themes

import io.lugf027.github.mermaid.core.config.ThemeName

/**
 * 主题抽象基类。
 * 每种内置主题继承此类实现。
 */
abstract class Theme {
    /** 获取该主题的变量 */
    abstract fun getThemeVariables(): ThemeVariables

    companion object {
        /**
         * 根据主题名称获取主题实例。
         */
        fun getTheme(name: ThemeName): Theme {
            return when (name) {
                ThemeName.DEFAULT -> DefaultTheme()
                ThemeName.DARK -> DarkTheme()
                ThemeName.FOREST -> ForestTheme()
                ThemeName.NEUTRAL -> NeutralTheme()
                ThemeName.BASE -> BaseTheme()
            }
        }

        /**
         * 获取默认主题变量。
         */
        fun getDefaultThemeVariables(): ThemeVariables {
            return DefaultTheme().getThemeVariables()
        }
    }
}

/** 默认主题 */
class DefaultTheme : Theme() {
    override fun getThemeVariables(): ThemeVariables = ThemeVariables()
}

/** 暗色主题 */
class DarkTheme : Theme() {
    override fun getThemeVariables(): ThemeVariables = ThemeVariables(
        background = "#333333",
        mainBkg = "#1f2020",
        secondBkg = "#555555",
        lineColor = "#cccccc",
        border1 = "#81B1DB",
        border2 = "#aaaa33",
        textColor = "#cccccc",
        primaryColor = "#1f2020",
        primaryTextColor = "#e0e0e0",
        primaryBorderColor = "#81B1DB",
        secondaryColor = "#555555",
        secondaryTextColor = "#e0e0e0",
        secondaryBorderColor = "#aaaa33",
        tertiaryColor = "#333333",
        tertiaryTextColor = "#e0e0e0",
        tertiaryBorderColor = "#666666",
        noteBkgColor = "#555555",
        noteTextColor = "#e0e0e0",
        noteBorderColor = "#aaaa33",
        nodeBkg = "#1f2020",
        nodeBorder = "#81B1DB",
        clusterBkg = "#555555",
        clusterBorder = "#aaaa33",
        edgeLabelBackground = "#333333",
        defaultLinkColor = "#cccccc",
        actorBkg = "#1f2020",
        actorBorder = "#81B1DB",
        actorTextColor = "#e0e0e0",
        signalColor = "#cccccc",
        signalTextColor = "#cccccc",
    )
}

/** 森林主题 */
class ForestTheme : Theme() {
    override fun getThemeVariables(): ThemeVariables = ThemeVariables(
        primaryColor = "#cde498",
        primaryTextColor = "#333333",
        primaryBorderColor = "#13540c",
        secondaryColor = "#ffffde",
        secondaryTextColor = "#333333",
        secondaryBorderColor = "#aaaa33",
        tertiaryColor = "#f4f4f4",
        tertiaryTextColor = "#333333",
        tertiaryBorderColor = "#cccccc",
        lineColor = "#333333",
        nodeBkg = "#cde498",
        nodeBorder = "#13540c",
        clusterBkg = "#ffffde",
        clusterBorder = "#aaaa33",
        actorBkg = "#cde498",
        actorBorder = "#13540c",
    )
}

/** 中性主题 */
class NeutralTheme : Theme() {
    override fun getThemeVariables(): ThemeVariables = ThemeVariables(
        primaryColor = "#f4f4f4",
        primaryTextColor = "#333333",
        primaryBorderColor = "#cccccc",
        secondaryColor = "#f4f4f4",
        secondaryTextColor = "#333333",
        secondaryBorderColor = "#cccccc",
        tertiaryColor = "#f4f4f4",
        tertiaryTextColor = "#333333",
        tertiaryBorderColor = "#cccccc",
        lineColor = "#666666",
        nodeBkg = "#f4f4f4",
        nodeBorder = "#cccccc",
        clusterBkg = "#f4f4f4",
        clusterBorder = "#cccccc",
        actorBkg = "#f4f4f4",
        actorBorder = "#cccccc",
    )
}

/** 基础主题 (用于自定义) */
class BaseTheme : Theme() {
    override fun getThemeVariables(): ThemeVariables = ThemeVariables()
}
