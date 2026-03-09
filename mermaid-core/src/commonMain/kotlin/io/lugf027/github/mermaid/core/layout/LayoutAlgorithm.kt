package io.lugf027.github.mermaid.core.layout

/**
 * 布局算法接口 - 对标 mermaid-js render.ts 的 LayoutAlgorithm
 */
interface LayoutAlgorithm {
    /**
     * 执行布局计算，更新节点和边的坐标
     *
     * @param data 布局数据（包含节点、边、配置）
     * @return 布局后的数据（坐标已更新）
     */
    fun layout(data: LayoutData): LayoutData
}
