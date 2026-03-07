package io.lugf027.github.mermaid.core.config

/**
 * 配置管理器。
 * 实现 siteConfig/currentConfig/directive 三层配置合并逻辑。
 * 对应 mermaid-js 的 config.ts。
 *
 * 配置优先级（从低到高）：
 * 1. defaultConfig - 内置默认配置
 * 2. siteConfig - 全局站点级别配置（通过 initialize() 设置）
 * 3. currentConfig - 当前绘图配置
 * 4. directiveConfig - 通过 %%{init: {...}}%% 指令覆盖的配置
 */
object ConfigManager {
    /** 默认配置（不可变） */
    private val defaultConfig = MermaidConfig()

    /** 站点级别配置（通过 initialize 设置） */
    private var siteConfig: MermaidConfig = defaultConfig.copy()

    /** 当前配置（合并 site + directive） */
    private var currentConfig: MermaidConfig = defaultConfig.copy()

    /** directive 级别配置覆盖 */
    private var directiveOverrides: MutableMap<String, Any?> = mutableMapOf()

    /**
     * 获取当前生效的配置。
     */
    fun getConfig(): MermaidConfig = currentConfig

    /**
     * 获取站点级别配置。
     */
    fun getSiteConfig(): MermaidConfig = siteConfig

    /**
     * 获取默认配置。
     */
    fun getDefaultConfig(): MermaidConfig = defaultConfig

    /**
     * 使用站点配置初始化。
     * 通常在应用启动时调用一次。
     * @param config 站点级别配置
     */
    fun setSiteConfig(config: MermaidConfig) {
        siteConfig = config
        currentConfig = config.copy()
    }

    /**
     * 更新当前配置。
     * @param updater 配置更新函数
     */
    fun updateConfig(updater: (MermaidConfig) -> MermaidConfig) {
        currentConfig = updater(currentConfig)
    }

    /**
     * 添加 directive 配置覆盖。
     * 从 %%{init: {...}}%% 指令中提取的配置。
     * @param key 配置键
     * @param value 配置值
     */
    fun addDirective(key: String, value: Any?) {
        directiveOverrides[key] = value
    }

    /**
     * 重置当前配置为站点配置。
     * 每次解析新图表时调用。
     */
    fun reset() {
        currentConfig = siteConfig.copy()
        directiveOverrides.clear()
    }

    /**
     * 完全重置，恢复到默认配置。
     */
    fun resetAll() {
        siteConfig = defaultConfig.copy()
        currentConfig = defaultConfig.copy()
        directiveOverrides.clear()
    }
}
