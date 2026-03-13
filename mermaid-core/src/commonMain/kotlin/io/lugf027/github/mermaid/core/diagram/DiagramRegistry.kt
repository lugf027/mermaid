package io.lugf027.github.mermaid.core.diagram

import io.lugf027.github.mermaid.core.util.Logger

/**
 * 图表注册表 - 对标 mermaid-js diagramAPI.ts
 *
 * 管理所有已注册的图表定义。通过 ID 查找图表定义。
 */
object DiagramRegistry {

    private val log = Logger("DiagramRegistry")

    /** 已注册的图表定义 */
    private val definitions = mutableMapOf<String, DiagramDefinition>()

    /**
     * 注册图表定义
     *
     * @param id 图表类型 ID
     * @param definition 图表定义
     */
    fun register(id: String, definition: DiagramDefinition) {
        log.debug("Registering diagram: $id")
        definitions[id] = definition
    }

    /**
     * 注册图表定义（使用定义自身的 ID）
     */
    fun register(definition: DiagramDefinition) {
        register(definition.id, definition)
    }

    /**
     * 获取图表定义
     *
     * @param id 图表类型 ID
     * @return 图表定义，未找到时返回 null
     */
    fun get(id: String): DiagramDefinition? = definitions[id]

    /**
     * 获取图表定义，未找到时抛出异常
     */
    fun getOrThrow(id: String): DiagramDefinition {
        return definitions[id]
            ?: throw IllegalArgumentException("Unknown diagram type: $id. Available: ${definitions.keys}")
    }

    /**
     * 检查是否已注册
     */
    fun contains(id: String): Boolean = definitions.containsKey(id)

    /**
     * 获取所有已注册的图表 ID
     */
    fun getRegisteredIds(): Set<String> = definitions.keys.toSet()

    /**
     * 清空所有注册
     */
    fun clear() {
        definitions.clear()
    }
}
