package io.lugf027.github.mermaid.core.core

import io.lugf027.github.mermaid.core.config.ConfigManager
import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.types.ParseResult

/**
 * MermaidKMP 公共入口对象。
 * 提供 parse()、initialize() 等顶层 API。
 * 对应 mermaid-js 的 mermaid.ts。
 */
object MermaidKMP {

    /** 是否已初始化 */
    private var initialized = false

    /**
     * 初始化 MermaidKMP。
     * 注册所有内置图表类型，设置全局配置。
     * @param config 全局配置（可选）
     */
    fun initialize(config: MermaidConfig = MermaidConfig()) {
        ConfigManager.setSiteConfig(config)

        // 注册所有内置图表类型
        if (!initialized) {
            DiagramOrchestration.registerAll()
            initialized = true
        }
    }

    /**
     * 解析 Mermaid 文本。
     * @param text Mermaid 图表文本
     * @return 解析后的 Diagram 实例
     */
    fun parse(text: String): Diagram {
        ensureInitialized()
        ConfigManager.reset()
        return Diagram.fromText(text)
    }

    /**
     * 尝试解析，不抛出异常。
     * @param text Mermaid 图表文本
     * @return 解析结果
     */
    fun tryParse(text: String): ParseResult {
        ensureInitialized()
        return Diagram.tryParse(text)
    }

    /**
     * 获取当前配置。
     */
    fun getConfig(): MermaidConfig = ConfigManager.getConfig()

    /**
     * 更新配置。
     */
    fun updateConfig(updater: (MermaidConfig) -> MermaidConfig) {
        ConfigManager.updateConfig(updater)
    }

    /**
     * 重置到默认状态。
     */
    fun reset() {
        ConfigManager.resetAll()
        DiagramRegistry.clear()
        initialized = false
    }

    /**
     * 获取所有已注册的图表类型。
     */
    fun getRegisteredDiagramTypes(): List<String> {
        ensureInitialized()
        return DiagramRegistry.getRegisteredTypes()
    }

    /**
     * 确保已初始化。
     */
    private fun ensureInitialized() {
        if (!initialized) {
            initialize()
        }
    }
}
