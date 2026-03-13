package io.lugf027.github.mermaid.eval.strategies

import io.lugf027.github.mermaid.eval.*

/**
 * ClassDiagram 评分策略 — 使用 Dagre 统一渲染
 *
 * 评分维度:
 * - viewBox (5%)
 * - 类节点位置 (30%)
 * - 关系线路径 (30%)
 * - CSS 类名 (10%)
 * - marker (10%)
 * - 结构 (15%)
 */
object ClassDiagramStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "class"

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.05)

        // 类节点
        val jsNodes = SvgScorer.extractGroupTranslates(jsSvg, "node")
        val kmpNodes = SvgScorer.extractGroupTranslates(kmpSvg, "node")
        dims += SvgScorer.scorePositions(jsNodes, kmpNodes, "classNodes", 0.30, 0.05, 30.0)

        // 关系线
        val jsPaths = SvgScorer.extractPathsByDataAttr(jsSvg, """data-edge="true"""")
            .ifEmpty { extractGenericEdgePaths(jsSvg) }
        val kmpPaths = SvgScorer.extractPathsByDataAttr(kmpSvg, """data-edge="true"""")
            .ifEmpty { extractGenericEdgePaths(kmpSvg) }
        dims += SvgScorer.scorePaths(jsPaths, kmpPaths, "relations", 0.30, 0.5, 50.0)

        // CSS
        val jsClasses = extractEdgeClasses(jsSvg)
        val kmpClasses = extractEdgeClasses(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsClasses, kmpClasses, "css", 0.10)

        // markers
        dims += SvgScorer.scoreMarkers(jsSvg, kmpSvg, 0.10)

        // 结构
        dims += SvgScorer.scoreStructureCounts(
            listOf(jsNodes.size to kmpNodes.size, jsPaths.size to kmpPaths.size),
            listOf("classNodes", "relations"),
            0.15
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}

/**
 * StateDiagram 评分策略 — 使用 Dagre 统一渲染
 */
object StateDiagramStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "state"

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.05)

        // 状态节点
        val jsNodes = SvgScorer.extractGroupTranslates(jsSvg, "node")
        val kmpNodes = SvgScorer.extractGroupTranslates(kmpSvg, "node")
        dims += SvgScorer.scorePositions(jsNodes, kmpNodes, "stateNodes", 0.30, 0.05, 30.0)

        // 转换线
        val jsPaths = SvgScorer.extractPathsByDataAttr(jsSvg, """data-edge="true"""")
            .ifEmpty { extractGenericEdgePaths(jsSvg) }
        val kmpPaths = SvgScorer.extractPathsByDataAttr(kmpSvg, """data-edge="true"""")
            .ifEmpty { extractGenericEdgePaths(kmpSvg) }
        dims += SvgScorer.scorePaths(jsPaths, kmpPaths, "transitions", 0.30, 0.5, 50.0)

        // CSS
        val jsClasses = extractEdgeClasses(jsSvg)
        val kmpClasses = extractEdgeClasses(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsClasses, kmpClasses, "css", 0.10)

        // markers
        dims += SvgScorer.scoreMarkers(jsSvg, kmpSvg, 0.10)

        // 结构
        dims += SvgScorer.scoreStructureCounts(
            listOf(jsNodes.size to kmpNodes.size, jsPaths.size to kmpPaths.size),
            listOf("stateNodes", "transitions"),
            0.15
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}

/**
 * ER Diagram 评分策略 — 使用 Dagre 统一渲染
 */
object ErDiagramStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "er"

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.05)

        // 实体节点
        val jsNodes = SvgScorer.extractGroupTranslates(jsSvg, "node")
            .ifEmpty { SvgScorer.extractGroupTranslates(jsSvg, "er") }
        val kmpNodes = SvgScorer.extractGroupTranslates(kmpSvg, "node")
            .ifEmpty { SvgScorer.extractGroupTranslates(kmpSvg, "er") }
        dims += SvgScorer.scorePositions(jsNodes, kmpNodes, "entities", 0.30, 0.05, 30.0)

        // 关系线
        val jsPaths = SvgScorer.extractPathsByClass(jsSvg, "er.relationshipLine")
            .ifEmpty { SvgScorer.extractPathsByDataAttr(jsSvg, """data-edge="true"""") }
            .ifEmpty { extractGenericEdgePaths(jsSvg) }
        val kmpPaths = SvgScorer.extractPathsByClass(kmpSvg, "er.relationshipLine")
            .ifEmpty { SvgScorer.extractPathsByDataAttr(kmpSvg, """data-edge="true"""") }
            .ifEmpty { extractGenericEdgePaths(kmpSvg) }
        dims += SvgScorer.scorePaths(jsPaths, kmpPaths, "relationships", 0.30, 0.5, 50.0)

        // markers (crow's foot 等)
        dims += SvgScorer.scoreMarkers(jsSvg, kmpSvg, 0.15)

        // 文本标签
        val jsTexts = SvgScorer.extractTextContents(jsSvg)
        val kmpTexts = SvgScorer.extractTextContents(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsTexts, kmpTexts, "labels", 0.05)

        // 结构
        dims += SvgScorer.scoreStructureCounts(
            listOf(jsNodes.size to kmpNodes.size, jsPaths.size to kmpPaths.size),
            listOf("entities", "relationships"),
            0.15
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}

// ── 共用工具 ──

private val RE_D_ATTR = Regex("""\bd="(M[^"]+)"""")
private val RE_EDGE_CLASS = Regex("""class="([^"]*(?:edge|link|transition|relationship)[^"]*?)"""")

private fun extractEdgeClasses(svg: String): List<String> {
    return RE_EDGE_CLASS.findAll(svg).map { it.groupValues[1] }.toList()
}

private fun extractGenericEdgePaths(svg: String): List<String> {
    return RE_D_ATTR.findAll(svg).map { it.groupValues[1] }
        .filter { it.contains("C") && it.length > 50 }
        .toList()
}
