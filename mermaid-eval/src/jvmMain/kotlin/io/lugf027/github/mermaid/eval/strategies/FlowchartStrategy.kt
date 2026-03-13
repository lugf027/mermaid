package io.lugf027.github.mermaid.eval.strategies

import io.lugf027.github.mermaid.eval.*

/**
 * Flowchart 评分策略 — 与原 SvgScorer 逻辑完全一致
 *
 * 评分维度:
 * - viewBox (5%)
 * - 节点位置 (35%)
 * - 边路径 (35%)
 * - CSS 类名 (10%)
 * - marker (5%)
 * - 结构完整性 (10%)
 */
object FlowchartStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "flowchart"

    // ── flowchart 专用正则 ──────────────────────────
    private val RE_NODE = Regex("""<g[^>]*class="[^"]*node[^"]*"[^>]*transform="translate\(([^)]+)\)"""")
    private val RE_FLOWCHART_LINK_PATH = Regex("""<path\s[^>]*?class="[^"]*flowchart-link[^"]*"[^>]*/>""")
    private val RE_DATA_EDGE_PATH = Regex("""<path\s[^>]*?data-edge="true"[^>]*/>""")
    private val RE_D_ATTR = Regex("""\bd="(M[^"]+)"""")
    private val RE_EDGE_CLASS = Regex("""class="([^"]*(?:edge|flowchart-link)[^"]*?)"""")

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        // ① viewBox
        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.05)

        // ② 节点位置
        val jsNodes = extractNodes(jsSvg)
        val kmpNodes = extractNodes(kmpSvg)
        dims += SvgScorer.scorePositions(jsNodes, kmpNodes, "nodes", 0.35, 0.05, 30.0)

        // ③ 边路径
        val jsPaths = extractEdgePaths(jsSvg)
        val kmpPaths = extractEdgePaths(kmpSvg)
        dims += SvgScorer.scorePaths(jsPaths, kmpPaths, "edges", 0.35, 0.5, 50.0)

        // ④ CSS 类名
        val jsClasses = RE_EDGE_CLASS.findAll(jsSvg).map { it.groupValues[1] }.toList()
        val kmpClasses = RE_EDGE_CLASS.findAll(kmpSvg).map { it.groupValues[1] }.toList()
        dims += SvgScorer.scoreStringMatch(jsClasses, kmpClasses, "css", 0.10)

        // ⑤ marker
        dims += SvgScorer.scoreMarkers(jsSvg, kmpSvg, 0.05)

        // ⑥ 结构完整性
        dims += SvgScorer.scoreStructureCounts(
            listOf(jsNodes.size to kmpNodes.size, jsPaths.size to kmpPaths.size),
            listOf("nodes", "edges"),
            0.10
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }

    private fun extractNodes(svg: String): List<Pair<Double, Double>> {
        return RE_NODE.findAll(svg).map { m ->
            val coords = m.groupValues[1].replace(",", " ").split(" ").filter { it.isNotBlank() }
            val x = coords.getOrNull(0)?.toDoubleOrNull() ?: 0.0
            val y = coords.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            x to y
        }.toList()
    }

    private fun extractEdgePaths(svg: String): List<String> {
        val fromLink = RE_FLOWCHART_LINK_PATH.findAll(svg).mapNotNull { m ->
            RE_D_ATTR.find(m.value)?.groupValues?.get(1)
        }.toList()
        if (fromLink.isNotEmpty()) return fromLink

        val fromDataEdge = RE_DATA_EDGE_PATH.findAll(svg).mapNotNull { m ->
            RE_D_ATTR.find(m.value)?.groupValues?.get(1)
        }.toList()
        if (fromDataEdge.isNotEmpty()) return fromDataEdge

        return RE_D_ATTR.findAll(svg).map { it.groupValues[1] }
            .filter { it.contains("C") && it.length > 50 }
            .toList()
    }
}
