package io.lugf027.github.mermaid.core.layout.dagre

import io.lugf027.github.mermaid.core.layout.Point

/**
 * 非循环化处理 — 精确对标 dagre acyclic.js
 *
 * dagre 要求输入图是 DAG（有向无环图），但 mermaid 的 flowchart 可能包含循环边。
 * 这个模块通过 DFS 找到反向边（back edges），将它们反转（swap source/target），
 * 使图变成 DAG。布局完成后再通过 undo() 恢复原始方向并反转 points。
 */
object Acyclic {

    /**
     * 对标 acyclic.run(g)
     *
     * 使用 DFS 找到 feedback arc set（反向边集合），然后反转这些边。
     */
    fun run(graph: Graph) {
        val fas = dfsFAS(graph)

        for ((v, w) in fas) {
            val label = graph.getEdge(v, w) ?: continue
            graph.removeEdge(v, w)

            // 标记为反转边
            label.extra["reversed"] = true

            // 反转方向：将 v->w 变为 w->v
            graph.setEdge(w, v, label.copy(
                source = w,
                target = v
            ).also {
                it.extra["reversed"] = true
            })
        }
    }

    /**
     * DFS-based Feedback Arc Set — 对标 acyclic.js dfsFAS(g)
     *
     * 使用 DFS 找到所有反向边（即当遍历到一个正在当前 DFS 路径上的节点时，
     * 该边就是反向边）。返回需要反转的边列表 (source, target)。
     */
    private fun dfsFAS(graph: Graph): List<Pair<String, String>> {
        val fas = mutableListOf<Pair<String, String>>()
        val stack = mutableSetOf<String>()
        val visited = mutableSetOf<String>()

        fun dfs(v: String) {
            if (v in visited) return
            visited.add(v)
            stack.add(v)

            for (edge in graph.outEdgesOf(v)) {
                if (edge.target in stack) {
                    // 发现反向边
                    fas.add(v to edge.target)
                } else {
                    dfs(edge.target)
                }
            }

            stack.remove(v)
        }

        for (nodeId in graph.nodeIds()) {
            dfs(nodeId)
        }

        return fas
    }

    /**
     * 对标 acyclic.undo(g)
     *
     * 恢复被反转的边：将 w->v 恢复为 v->w，并标记 reversed。
     */
    fun undo(graph: Graph) {
        val edgesToReverse = mutableListOf<Triple<String, String, Graph.EdgeData>>()

        for (edge in graph.edges()) {
            if (edge.extra["reversed"] == true) {
                edgesToReverse.add(Triple(edge.source, edge.target, edge))
            }
        }

        for ((source, target, label) in edgesToReverse) {
            graph.removeEdge(source, target)

            // 恢复原始方向
            label.extra.remove("reversed")
            graph.setEdge(target, source, label.copy(
                source = target,
                target = source
            ))
        }
    }

    /**
     * 对标 layout.js reversePointsForReversedEdges(g)
     *
     * 反转被标记为 reversed 的边的 points 顺序。
     * 注意：这在 acyclic.undo 之前调用（在 dagre layout 流程中）。
     */
    fun reversePointsForReversedEdges(graph: Graph) {
        for (edge in graph.edges()) {
            if (edge.extra["reversed"] == true) {
                edge.points.reverse()
            }
        }
    }
}
