package io.lugf027.github.mermaid.core.layout

/**
 * 布局算法注册表 - 对标 mermaid-js render.ts 的 layoutAlgorithms
 */
object LayoutRegistry {

    private val algorithms = mutableMapOf<String, LayoutAlgorithm>()

    /**
     * 注册布局算法
     */
    fun register(name: String, algorithm: LayoutAlgorithm) {
        algorithms[name] = algorithm
    }

    /**
     * 获取布局算法
     */
    fun get(name: String): LayoutAlgorithm? = algorithms[name]

    /**
     * 获取默认布局算法
     */
    fun getDefault(): LayoutAlgorithm = algorithms["dagre"]
        ?: throw IllegalStateException("Default dagre layout algorithm not registered")

    /**
     * 清空注册表
     */
    fun clear() {
        algorithms.clear()
    }
}
