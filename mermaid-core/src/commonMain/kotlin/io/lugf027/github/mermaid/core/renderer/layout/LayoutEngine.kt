package io.lugf027.github.mermaid.core.renderer.layout

import io.lugf027.github.mermaid.core.types.LayoutData
import io.lugf027.github.mermaid.core.types.RenderData

/**
 * 布局引擎接口。
 * 接收 LayoutData（节点+边），计算坐标，返回 RenderData。
 */
interface LayoutEngine {
    /** 布局算法名称 */
    val name: String

    /**
     * 执行布局计算。
     * @param data 输入的布局数据
     * @return 带坐标的渲染数据
     */
    fun layout(data: LayoutData): RenderData
}

/**
 * 布局引擎注册中心。
 */
object LayoutRegistry {
    private val engines = mutableMapOf<String, LayoutEngine>()

    fun register(engine: LayoutEngine) {
        engines[engine.name] = engine
    }

    fun getEngine(name: String): LayoutEngine {
        return engines[name] ?: engines.values.firstOrNull()
            ?: throw IllegalStateException("No layout engine registered")
    }

    fun getDefaultEngine(): LayoutEngine {
        return engines["dagre"] ?: engines.values.firstOrNull()
            ?: throw IllegalStateException("No layout engine registered")
    }
}
