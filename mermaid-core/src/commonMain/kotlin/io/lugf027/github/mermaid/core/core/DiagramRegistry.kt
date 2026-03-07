package io.lugf027.github.mermaid.core.core

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.types.DiagramDefinition
import io.lugf027.github.mermaid.core.types.DiagramDetector
import io.lugf027.github.mermaid.core.types.DiagramLoader
import io.lugf027.github.mermaid.core.types.ErrorType
import io.lugf027.github.mermaid.core.types.MermaidError

/**
 * 图表注册中心。
 * 管理所有图表类型的注册、检测和加载。
 * 对应 mermaid-js 的 diagramAPI.ts + detectType.ts。
 */
object DiagramRegistry {

    /**
     * 已注册的图表检测器。
     * 按注册顺序存储，检测时按序遍历。
     */
    private val detectors = mutableListOf<DetectorEntry>()

    /**
     * 已加载的图表定义缓存。
     */
    private val loadedDiagrams = mutableMapOf<String, DiagramDefinition>()

    /**
     * 图表加载器（懒加载用）。
     */
    private val loaders = mutableMapOf<String, DiagramLoader>()

    /**
     * 注册图表检测器和加载器。
     * @param id 图表类型标识
     * @param detector 类型检测函数
     * @param loader 图表定义懒加载函数
     */
    fun registerDetector(
        id: String,
        detector: DiagramDetector,
        loader: DiagramLoader
    ) {
        detectors.add(DetectorEntry(id, detector))
        loaders[id] = loader
    }

    /**
     * 直接注册图表定义（非懒加载）。
     * @param id 图表类型标识
     * @param detector 类型检测函数
     * @param definition 图表定义实例
     */
    fun registerDiagram(
        id: String,
        detector: DiagramDetector,
        definition: DiagramDefinition
    ) {
        detectors.add(DetectorEntry(id, detector))
        loadedDiagrams[id] = definition
    }

    /**
     * 检测文本对应的图表类型。
     * 遍历所有注册的检测器，返回第一个匹配的类型 ID。
     * @param text 待检测的 Mermaid 文本
     * @param config 当前配置（可选）
     * @return 匹配的图表类型 ID
     * @throws MermaidError 如果没有匹配的类型
     */
    fun detectType(text: String, config: MermaidConfig? = null): String {
        val cleanText = text.trim()
        for (entry in detectors) {
            if (entry.detector(cleanText, config)) {
                return entry.id
            }
        }
        throw IllegalStateException(
            MermaidError(
                message = "No diagram type detected for text starting with: ${
                    cleanText.take(50).replace("\n", "\\n")
                }",
                type = ErrorType.DETECTION_ERROR
            ).toString()
        )
    }

    /**
     * 获取图表定义（按需加载）。
     * @param id 图表类型标识
     * @return 图表定义实例
     * @throws IllegalStateException 如果未注册
     */
    fun getDiagram(id: String): DiagramDefinition {
        // 先检查缓存
        loadedDiagrams[id]?.let { return it }

        // 尝试懒加载
        val loader = loaders[id]
            ?: throw IllegalStateException("Diagram type '$id' is not registered")

        val definition = loader()
        loadedDiagrams[id] = definition
        return definition
    }

    /**
     * 检查图表类型是否已注册。
     */
    fun isRegistered(id: String): Boolean {
        return detectors.any { it.id == id }
    }

    /**
     * 获取所有已注册的图表类型 ID。
     */
    fun getRegisteredTypes(): List<String> {
        return detectors.map { it.id }
    }

    /**
     * 清除所有注册信息（测试用）。
     */
    fun clear() {
        detectors.clear()
        loadedDiagrams.clear()
        loaders.clear()
    }

    private data class DetectorEntry(
        val id: String,
        val detector: DiagramDetector
    )
}
