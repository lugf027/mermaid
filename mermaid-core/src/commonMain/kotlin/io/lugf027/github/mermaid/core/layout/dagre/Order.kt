package io.lugf027.github.mermaid.core.layout.dagre

/**
 * 层内排序算法 - 对标 dagre order 模块
 *
 * 使用 barycenter 启发式算法最小化边交叉。
 * Sugiyama 分层布局的第二步。
 */
object Order {

    /**
     * 对各层节点进行排序，最小化边交叉
     */
    fun order(graph: Graph) {
        val layers = Rank.layers(graph)

        // 初始化 order
        for ((layerIdx, layer) in layers.withIndex()) {
            for ((idx, nodeId) in layer.withIndex()) {
                graph.getNode(nodeId)?.order = idx
            }
        }

        // 执行多遍 barycenter 排序
        val maxIterations = 24
        for (iter in 0 until maxIterations) {
            if (iter % 2 == 0) {
                // 从上往下
                sweepDownward(graph, layers)
            } else {
                // 从下往上
                sweepUpward(graph, layers)
            }
        }

        // 更新最终 order
        val finalLayers = Rank.layers(graph)
        for (layer in finalLayers) {
            val sorted = layer.sortedBy { graph.getNode(it)?.order ?: 0 }
            for ((idx, nodeId) in sorted.withIndex()) {
                graph.getNode(nodeId)?.order = idx
            }
        }
    }

    /**
     * 从上往下扫描：根据上层（固定）节点的位置，对下层（自由）节点排序
     */
    private fun sweepDownward(graph: Graph, layers: List<List<String>>) {
        for (i in 1 until layers.size) {
            val fixedLayer = layers[i - 1]
            val freeLayer = layers[i].toMutableList()

            barycenterSort(graph, fixedLayer, freeLayer, true)
        }
    }

    /**
     * 从下往上扫描
     */
    private fun sweepUpward(graph: Graph, layers: List<List<String>>) {
        for (i in layers.size - 2 downTo 0) {
            val fixedLayer = layers[i + 1]
            val freeLayer = layers[i].toMutableList()

            barycenterSort(graph, fixedLayer, freeLayer, false)
        }
    }

    /**
     * Barycenter 排序：计算每个自由节点相对于固定层的重心位置
     */
    private fun barycenterSort(
        graph: Graph,
        fixedLayer: List<String>,
        freeLayer: List<String>,
        downward: Boolean
    ) {
        val fixedPositions = mutableMapOf<String, Int>()
        for ((idx, nodeId) in fixedLayer.withIndex()) {
            fixedPositions[nodeId] = idx
        }

        val barycenters = mutableMapOf<String, Double>()

        for (nodeId in freeLayer) {
            val neighbors = if (downward) {
                graph.inEdgesOf(nodeId).map { it.source }
            } else {
                graph.outEdgesOf(nodeId).map { it.target }
            }

            val positions = neighbors.mapNotNull { fixedPositions[it] }
            if (positions.isNotEmpty()) {
                barycenters[nodeId] = positions.average()
            } else {
                // 保持原位
                barycenters[nodeId] = (graph.getNode(nodeId)?.order ?: 0).toDouble()
            }
        }

        // 根据重心排序
        val sorted = freeLayer.sortedBy { barycenters[it] ?: 0.0 }
        for ((idx, nodeId) in sorted.withIndex()) {
            graph.getNode(nodeId)?.order = idx
        }
    }
}
