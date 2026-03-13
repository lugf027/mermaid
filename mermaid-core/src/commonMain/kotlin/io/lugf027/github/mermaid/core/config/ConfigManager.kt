package io.lugf027.github.mermaid.core.config

import io.lugf027.github.mermaid.core.util.Logger

/**
 * 配置管理器 - 对标 mermaid-js config.ts
 *
 * 管理配置的层级合并：defaultConfig → siteConfig → directives → currentConfig
 * 线程安全的配置管理，支持配置重置。
 */
object ConfigManager {
    private val log = Logger("ConfigManager")

    /** 默认配置（不可变） */
    private val defaultConfig: MermaidConfig = MermaidConfig.DEFAULT

    /** 站点级配置 */
    private var siteConfig: MermaidConfig = defaultConfig.copy()

    /** 来自 initialize() 的配置 */
    private var configFromInitialize: MermaidConfig = MermaidConfig()

    /** 指令列表 */
    private val directives: MutableList<MermaidConfig> = mutableListOf()

    /** 当前生效的运行时配置 */
    private var currentConfig: MermaidConfig = defaultConfig.copy()

    /**
     * 设置站点配置（从 initialize 调用）
     */
    fun setSiteConfig(conf: MermaidConfig): MermaidConfig {
        siteConfig = mergeConfigs(defaultConfig, conf)
        currentConfig = siteConfig.copy()
        return siteConfig
    }

    /**
     * 保存初始化配置
     */
    fun saveConfigFromInitialize(conf: MermaidConfig) {
        configFromInitialize = conf
    }

    /**
     * 更新站点配置
     */
    fun updateSiteConfig(conf: MermaidConfig): MermaidConfig {
        siteConfig = mergeConfigs(siteConfig, conf)
        currentConfig = mergeConfigs(currentConfig, conf)
        return siteConfig
    }

    /**
     * 获取站点配置副本
     */
    fun getSiteConfig(): MermaidConfig = siteConfig.copy()

    /**
     * 设置当前配置
     */
    fun setConfig(conf: MermaidConfig): MermaidConfig {
        currentConfig = mergeConfigs(currentConfig, conf)
        return currentConfig
    }

    /**
     * 获取当前生效的配置副本
     */
    fun getConfig(): MermaidConfig = currentConfig.copy()

    /**
     * 获取默认配置
     */
    fun getDefaultConfig(): MermaidConfig = defaultConfig

    /**
     * 添加指令级配置
     */
    fun addDirective(directive: MermaidConfig) {
        directives.add(directive)
        updateCurrentConfig()
    }

    /**
     * 重置配置到站点配置
     */
    fun reset(config: MermaidConfig? = null) {
        directives.clear()
        if (config != null) {
            currentConfig = config.copy()
        } else {
            currentConfig = siteConfig.copy()
        }
    }

    /**
     * 合并两个配置，后者覆盖前者（非 null 字段覆盖）
     */
    fun mergeConfigs(base: MermaidConfig, override: MermaidConfig): MermaidConfig {
        return MermaidConfig(
            theme = override.theme ?: base.theme,
            themeCSS = override.themeCSS ?: base.themeCSS,
            look = override.look ?: base.look,
            handDrawnSeed = override.handDrawnSeed ?: base.handDrawnSeed,
            layout = override.layout ?: base.layout,
            maxTextSize = override.maxTextSize ?: base.maxTextSize,
            maxEdges = override.maxEdges ?: base.maxEdges,
            darkMode = override.darkMode ?: base.darkMode,
            htmlLabels = override.htmlLabels ?: base.htmlLabels,
            fontFamily = override.fontFamily ?: base.fontFamily,
            altFontFamily = override.altFontFamily ?: base.altFontFamily,
            logLevel = override.logLevel ?: base.logLevel,
            securityLevel = override.securityLevel ?: base.securityLevel,
            arrowMarkerAbsolute = override.arrowMarkerAbsolute ?: base.arrowMarkerAbsolute,
            deterministicIds = override.deterministicIds ?: base.deterministicIds,
            deterministicIDSeed = override.deterministicIDSeed ?: base.deterministicIDSeed,
            fontSize = override.fontSize ?: base.fontSize,
            markdownAutoWrap = override.markdownAutoWrap ?: base.markdownAutoWrap,
            suppressErrorRendering = override.suppressErrorRendering ?: base.suppressErrorRendering,
            wrap = override.wrap ?: base.wrap,
            flowchart = override.flowchart ?: base.flowchart,
            sequence = override.sequence ?: base.sequence,
            gantt = override.gantt ?: base.gantt,
            journey = override.journey ?: base.journey,
            timeline = override.timeline ?: base.timeline,
            `class` = override.`class` ?: base.`class`,
            state = override.state ?: base.state,
            er = override.er ?: base.er,
            pie = override.pie ?: base.pie,
            quadrantChart = override.quadrantChart ?: base.quadrantChart,
            xyChart = override.xyChart ?: base.xyChart,
            requirement = override.requirement ?: base.requirement,
            architecture = override.architecture ?: base.architecture,
            mindmap = override.mindmap ?: base.mindmap,
            ishikawa = override.ishikawa ?: base.ishikawa,
            kanban = override.kanban ?: base.kanban,
            gitGraph = override.gitGraph ?: base.gitGraph,
            c4 = override.c4 ?: base.c4,
            sankey = override.sankey ?: base.sankey,
            packet = override.packet ?: base.packet,
            block = override.block ?: base.block,
            radar = override.radar ?: base.radar,
            venn = override.venn ?: base.venn,
        )
    }

    /** 根据指令更新当前配置 */
    private fun updateCurrentConfig() {
        currentConfig = siteConfig.copy()
        for (directive in directives) {
            currentConfig = mergeConfigs(currentConfig, directive)
        }
    }
}
