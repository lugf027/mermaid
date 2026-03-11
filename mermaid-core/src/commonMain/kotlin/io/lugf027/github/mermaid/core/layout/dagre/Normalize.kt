package io.lugf027.github.mermaid.core.layout.dagre

import io.lugf027.github.mermaid.core.layout.Point

/**
 * 边规范化 — 精确对标 dagre normalize.js
 *
 * 将跨越多个 rank 的长边拆分为多段短边（每段跨 1 rank），
 * 每段使用一个 dummy 节点。这使得 position 算法能正确处理
 * 所有边段的横向对齐。
 *
 * 在布局完成后，undo() 将 dummy 节点的坐标收集为边的 points。
 */
object Normalize {

    /**
     * 将长边拆分为短边 — 对标 normalize.run(g)
     *
     * Pre-conditions:
     *   1. 图是 DAG
     *   2. 每个节点有 "rank" 属性
     *
     * Post-conditions:
     *   1. 所有边长度为 1
     *   2. dummy 节点被添加到拆分的位置
     *   3. graph.dummyChains 记录了每条链的首个 dummy 节点
     */
    fun run(graph: Graph) {
        graph.dummyChains.clear()

        // 收集所有边的快照（因为后续会修改图）
        val edgeSnapshot = graph.edges().map { Triple(it.source, it.target, it) }

        for ((source, target, edgeData) in edgeSnapshot) {
            normalizeEdge(graph, source, target, edgeData)
        }
    }

    /**
     * 规范化单条边 — 对标 normalizeEdge(g, e)
     */
    private fun normalizeEdge(graph: Graph, origSource: String, origTarget: String, edgeData: Graph.EdgeData) {
        var v = origSource
        val vRankStart = graph.getNode(v)?.rank ?: return
        val wRank = graph.getNode(origTarget)?.rank ?: return
        val edgeLabel = edgeData
        val labelRank = edgeLabel.labelRank

        var vRank = vRankStart

        // 如果边只跨 1 rank，不需要拆分
        if (wRank == vRank + 1) return

        // 移除原始边
        graph.removeEdge(origSource, origTarget)

        var i = 0
        vRank++
        while (vRank < wRank) {
            edgeLabel.points = mutableListOf()  // 清空 points

            val attrs = Graph.NodeData(
                id = "",  // 稍后由 addDummyNode 设置
                width = 0.0,
                height = 0.0,
                dummy = "edge",
                edgeLabel = edgeLabel,
                edgeObj = Graph.EdgeKey(origSource, origTarget),
                rank = vRank
            )

            // 如果当前 rank 是标签所在 rank，设置标签尺寸
            if (vRank == labelRank) {
                attrs.width = edgeLabel.width
                attrs.height = edgeLabel.height
                attrs.dummy = "edge-label"
                attrs.labelpos = edgeLabel.labelpos
            }

            val dummy = graph.uniqueId("_d")
            attrs.extra["id"] = dummy
            graph.setNode(dummy, attrs.copy(id = dummy))

            // 设置从 v 到 dummy 的边
            graph.setEdge(v, dummy, Graph.EdgeData(
                source = v,
                target = dummy,
                weight = edgeLabel.weight
            ))

            if (i == 0) {
                graph.dummyChains.add(dummy)
            }

            v = dummy
            i++
            vRank++
        }

        // 从最后一个 dummy 到目标节点
        graph.setEdge(v, origTarget, Graph.EdgeData(
            source = v,
            target = origTarget,
            weight = edgeLabel.weight
        ))
    }

    /**
     * 反规范化 — 对标 normalize.undo(g)
     *
     * 遍历 dummy 链，将 dummy 节点的坐标收集到原始边的 points 中，
     * 然后移除 dummy 节点并恢复原始边。
     */
    fun undo(graph: Graph) {
        for (dummyId in graph.dummyChains) {
            var v = dummyId
            var node = graph.getNode(v) ?: continue
            val origLabel = node.edgeLabel ?: continue
            val edgeObj = node.edgeObj ?: continue

            // 恢复原始边
            graph.setEdge(edgeObj.v, edgeObj.w, origLabel)

            // 遍历 dummy 链，收集坐标
            while (node.dummy != null) {
                val successorList = graph.successors(v)
                val w = successorList.firstOrNull() ?: break

                graph.removeNode(v)
                origLabel.points.add(Point(node.x, node.y))

                if (node.dummy == "edge-label") {
                    origLabel.x = node.x
                    origLabel.y = node.y
                    origLabel.width = node.width
                    origLabel.height = node.height
                }

                v = w
                node = graph.getNode(v) ?: break
            }
        }
    }
}
