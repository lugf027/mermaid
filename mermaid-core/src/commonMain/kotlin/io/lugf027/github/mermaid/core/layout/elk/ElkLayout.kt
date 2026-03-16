package io.lugf027.github.mermaid.core.layout.elk

import io.lugf027.github.mermaid.core.layout.LayoutAlgorithm
import io.lugf027.github.mermaid.core.layout.LayoutData
import io.lugf027.github.mermaid.core.layout.LayoutNode
import io.lugf027.github.mermaid.core.layout.dagre.DagreLayout
import io.lugf027.github.mermaid.core.util.TextUtils

/**
 * ELK 布局算法入口 - 实现 LayoutAlgorithm 接口
 *
 * 当前实现：使用 DagreLayout 作为核心布局引擎
 * 原因：mermaid-js 使用真正的 elkjs 库（Java），实现完整的 ELK Layered 算法需要大量工作
 * 当前方案：使用 Dagre 进行基础布局，然后应用 ELK 特有的后处理逻辑
 *
 * 对标 mermaid-js packages/mermaid-layout-elk/src/render.ts 的完整流程：
 *
 * 1. 使用 DagreLayout 进行基础布局
 * 2. 后处理：应用 ELK 特有逻辑（边裁切优化）
 * 3. 写回 LayoutData
 */
class ElkLayout : LayoutAlgorithm {

    // 使用 DagreLayout 作为核心布局引擎
    private val dagreLayout = DagreLayout()

    override fun layout(data: LayoutData): LayoutData {
        if (data.nodes.isEmpty()) return data

        // 1. 首先使用 DagreLayout 进行基础布局
        // Dagre 已经是一个成熟的分层布局算法，与 ELK Layered 在很多方面相似
        var result = dagreLayout.layout(data)

        // 2. 构建子图树数据（用于 ELK 特有的后处理）
        val treeData = buildTreeDataFromLayout(data.nodes)

        // 3. 后处理：应用 ELK 特有的边裁切逻辑
        // ELK 与 Dagre 的主要差异在于边路由和节点位置微调
        result = postProcessWithElkStyle(result, treeData)

        return result
    }

    /**
     * ELK 风格的后处理
     */
    private fun postProcessWithElkStyle(data: LayoutData, treeData: TreeData): LayoutData {
        // 获取节点信息
        val nodeDb = mutableMapOf<String, NodeBounds>()
        for (node in data.nodes) {
            nodeDb[node.id] = NodeBounds(
                x = node.x,
                y = node.y,
                width = node.width,
                height = node.height,
                isGroup = node.isGroup,
            )
        }

        // 处理每条边：应用 ELK 风格的边裁切
        val updatedEdges = data.edges.map { edge ->
            val startInfo = nodeDb[edge.start]
            val endInfo = nodeDb[edge.end]

            if (startInfo == null || endInfo == null || edge.points.isEmpty()) {
                return@map edge
            }

            // 如果边已经有 points，应用 cutter2 裁切
            if (edge.points.size >= 2) {
                val cutterStartInfo = ElkEdgeCutter.NodeInfo(
                    id = edge.start,
                    centerX = startInfo.x,
                    centerY = startInfo.y,
                    width = startInfo.width,
                    height = startInfo.height,
                    isGroup = startInfo.isGroup,
                )
                val cutterEndInfo = ElkEdgeCutter.NodeInfo(
                    id = edge.end,
                    centerX = endInfo.x,
                    centerY = endInfo.y,
                    width = endInfo.width,
                    height = endInfo.height,
                    isGroup = endInfo.isGroup,
                )

                // 转换 points 为可变列表
                val mutablePoints = edge.points.toMutableList()

                // 应用边裁切
                val processedPoints = ElkEdgeCutter.processEdge(
                    mutablePoints,
                    cutterStartInfo,
                    cutterEndInfo,
                )

                edge.copy(points = processedPoints.toMutableList())
            } else {
                edge
            }
        }

        return data.copy(edges = updatedEdges)
    }

    /**
     * 节点边界信息
     */
    private data class NodeBounds(
        val x: Double,
        val y: Double,
        val width: Double,
        val height: Double,
        val isGroup: Boolean,
    )

    /**
     * 从 LayoutNode 列表构建 TreeData
     */
    private fun buildTreeDataFromLayout(nodes: List<LayoutNode>): TreeData {
        val treeData = TreeData()
        val subgraphs = nodes.filter { it.isGroup }

        for (subgraph in subgraphs) {
            val children = nodes.filter { it.parentId == subgraph.id }
            for (child in children) {
                treeData.parentById[child.id] = subgraph.id
                treeData.childrenById.getOrPut(subgraph.id) { mutableListOf() }.add(child.id)
            }
        }

        return treeData
    }
}
