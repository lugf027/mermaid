package io.lugf027.github.mermaid.eval.strategies

import io.lugf027.github.mermaid.eval.*

/**
 * 通用评分策略 — 用于未实现专用策略的图表类型
 *
 * 使用 SVG 结构性对比：viewBox、所有 path、所有 text、所有 rect/circle、结构元素数量
 *
 * 评分维度:
 * - viewBox (10%)
 * - 所有 path (30%)
 * - 文本内容 (20%)
 * - 几何元素 (20%) — rect/circle/line 位置
 * - 结构 (20%) — 各类元素数量
 */
object GenericStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "generic"

    private val RE_D_ATTR = Regex("""\bd="(M[^"]+)"""")

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        // ① viewBox
        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.10)

        // ② 所有 path
        val jsPaths = RE_D_ATTR.findAll(jsSvg).map { it.groupValues[1] }.toList()
        val kmpPaths = RE_D_ATTR.findAll(kmpSvg).map { it.groupValues[1] }.toList()
        dims += SvgScorer.scorePaths(jsPaths, kmpPaths, "paths", 0.30, 0.5, 50.0)

        // ③ 文本内容
        val jsTexts = SvgScorer.extractTextContents(jsSvg)
        val kmpTexts = SvgScorer.extractTextContents(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsTexts, kmpTexts, "texts", 0.20)

        // ④ 几何元素
        val jsRects = SvgScorer.extractRects(jsSvg)
        val kmpRects = SvgScorer.extractRects(kmpSvg)
        dims += SvgScorer.scoreRects(jsRects, kmpRects, "rects", 0.20, 0.5, 30.0)

        // ⑤ 结构
        dims += SvgScorer.scoreStructureCounts(
            listOf(
                jsPaths.size to kmpPaths.size,
                jsTexts.size to kmpTexts.size,
                jsRects.size to kmpRects.size
            ),
            listOf("paths", "texts", "rects"),
            0.20
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}

// ════════════════════════════════════════════════════════
// 第三批图表类型策略 — 基于 GenericStrategy 模式，加入图表特有的特征提取
// ════════════════════════════════════════════════════════

/**
 * GitGraph 评分策略
 */
object GitGraphStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "gitGraph"

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.05)

        // commit 节点（circle 元素）
        val jsCommits = SvgScorer.extractCircles(jsSvg, "commit")
        val kmpCommits = SvgScorer.extractCircles(kmpSvg, "commit")
        val jsPos = jsCommits.map { it.cx to it.cy }
        val kmpPos = kmpCommits.map { it.cx to it.cy }
        dims += SvgScorer.scorePositions(jsPos, kmpPos, "commits", 0.30, 0.5, 30.0)

        // 分支线路径
        val jsBranches = SvgScorer.extractPathsByClass(jsSvg, "branch")
        val kmpBranches = SvgScorer.extractPathsByClass(kmpSvg, "branch")
        dims += SvgScorer.scorePaths(jsBranches, kmpBranches, "branches", 0.30, 0.5, 50.0)

        // 文本标签
        val jsTexts = SvgScorer.extractTextContents(jsSvg)
        val kmpTexts = SvgScorer.extractTextContents(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsTexts, kmpTexts, "labels", 0.15)

        // 结构
        dims += SvgScorer.scoreStructureCounts(
            listOf(jsCommits.size to kmpCommits.size, jsBranches.size to kmpBranches.size),
            listOf("commits", "branches"),
            0.20
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}

/**
 * Journey 评分策略
 */
object JourneyStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "journey"

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.10)

        // 任务条
        val jsRects = SvgScorer.extractRects(jsSvg, "task")
        val kmpRects = SvgScorer.extractRects(kmpSvg, "task")
        dims += SvgScorer.scoreRects(jsRects, kmpRects, "tasks", 0.30, 0.5, 30.0)

        // 文本
        val jsTexts = SvgScorer.extractTextContents(jsSvg)
        val kmpTexts = SvgScorer.extractTextContents(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsTexts, kmpTexts, "labels", 0.25)

        // 圆点(评分)
        val jsCircles = SvgScorer.extractCircles(jsSvg, "face")
        val kmpCircles = SvgScorer.extractCircles(kmpSvg, "face")
        val jsPos = jsCircles.map { it.cx to it.cy }
        val kmpPos = kmpCircles.map { it.cx to it.cy }
        dims += SvgScorer.scorePositions(jsPos, kmpPos, "faces", 0.15, 0.5, 30.0)

        dims += SvgScorer.scoreStructureCounts(
            listOf(jsRects.size to kmpRects.size, jsTexts.size to kmpTexts.size),
            listOf("tasks", "labels"),
            0.20
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}

/**
 * Mindmap 评分策略
 */
object MindmapStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "mindmap"

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.10)

        val jsNodes = SvgScorer.extractGroupTranslates(jsSvg, "mindmap-node")
            .ifEmpty { SvgScorer.extractGroupTranslates(jsSvg, "node") }
        val kmpNodes = SvgScorer.extractGroupTranslates(kmpSvg, "mindmap-node")
            .ifEmpty { SvgScorer.extractGroupTranslates(kmpSvg, "node") }
        dims += SvgScorer.scorePositions(jsNodes, kmpNodes, "nodes", 0.30, 0.5, 30.0)

        val jsTexts = SvgScorer.extractTextContents(jsSvg)
        val kmpTexts = SvgScorer.extractTextContents(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsTexts, kmpTexts, "labels", 0.25)

        // 连接线
        val jsPaths = SvgScorer.extractPathsByClass(jsSvg, "edge")
        val kmpPaths = SvgScorer.extractPathsByClass(kmpSvg, "edge")
        dims += SvgScorer.scorePaths(jsPaths, kmpPaths, "edges", 0.15, 1.0, 50.0)

        dims += SvgScorer.scoreStructureCounts(
            listOf(jsNodes.size to kmpNodes.size, jsTexts.size to kmpTexts.size),
            listOf("nodes", "labels"),
            0.20
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}

/**
 * Timeline 评分策略
 */
object TimelineStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "timeline"

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.10)

        val jsRects = SvgScorer.extractRects(jsSvg)
        val kmpRects = SvgScorer.extractRects(kmpSvg)
        dims += SvgScorer.scoreRects(jsRects, kmpRects, "sections", 0.30, 0.5, 30.0)

        val jsTexts = SvgScorer.extractTextContents(jsSvg)
        val kmpTexts = SvgScorer.extractTextContents(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsTexts, kmpTexts, "labels", 0.25)

        val jsCircles = SvgScorer.extractCircles(jsSvg)
        val kmpCircles = SvgScorer.extractCircles(kmpSvg)
        val jsPos = jsCircles.map { it.cx to it.cy }
        val kmpPos = kmpCircles.map { it.cx to it.cy }
        dims += SvgScorer.scorePositions(jsPos, kmpPos, "timepoints", 0.15, 0.5, 30.0)

        dims += SvgScorer.scoreStructureCounts(
            listOf(jsRects.size to kmpRects.size, jsTexts.size to kmpTexts.size),
            listOf("rects", "labels"),
            0.20
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}

/**
 * C4 评分策略
 */
object C4Strategy : SvgScorer.ScoringStrategy {
    override val typeName = "c4"

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.05)

        val jsNodes = SvgScorer.extractGroupTranslates(jsSvg, "node")
        val kmpNodes = SvgScorer.extractGroupTranslates(kmpSvg, "node")
        dims += SvgScorer.scorePositions(jsNodes, kmpNodes, "components", 0.30, 0.5, 30.0)

        val jsRects = SvgScorer.extractRects(jsSvg)
        val kmpRects = SvgScorer.extractRects(kmpSvg)
        dims += SvgScorer.scoreRects(jsRects, kmpRects, "boundaries", 0.20, 0.5, 30.0)

        val jsTexts = SvgScorer.extractTextContents(jsSvg)
        val kmpTexts = SvgScorer.extractTextContents(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsTexts, kmpTexts, "labels", 0.20)

        dims += SvgScorer.scoreMarkers(jsSvg, kmpSvg, 0.05)

        dims += SvgScorer.scoreStructureCounts(
            listOf(jsNodes.size to kmpNodes.size, jsRects.size to kmpRects.size),
            listOf("components", "boundaries"),
            0.20
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}

/**
 * QuadrantChart 评分策略
 */
object QuadrantStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "quadrant"

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.10)

        // 象限矩形
        val jsRects = SvgScorer.extractRects(jsSvg)
        val kmpRects = SvgScorer.extractRects(kmpSvg)
        dims += SvgScorer.scoreRects(jsRects, kmpRects, "quadrants", 0.25, 0.5, 30.0)

        // 数据点
        val jsCircles = SvgScorer.extractCircles(jsSvg)
        val kmpCircles = SvgScorer.extractCircles(kmpSvg)
        val jsPos = jsCircles.map { it.cx to it.cy }
        val kmpPos = kmpCircles.map { it.cx to it.cy }
        dims += SvgScorer.scorePositions(jsPos, kmpPos, "points", 0.30, 0.5, 30.0)

        val jsTexts = SvgScorer.extractTextContents(jsSvg)
        val kmpTexts = SvgScorer.extractTextContents(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsTexts, kmpTexts, "labels", 0.15)

        dims += SvgScorer.scoreStructureCounts(
            listOf(jsCircles.size to kmpCircles.size, jsTexts.size to kmpTexts.size),
            listOf("points", "labels"),
            0.20
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}

/**
 * XYChart 评分策略
 */
object XYChartStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "xychart"

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.05)

        // bar 矩形
        val jsRects = SvgScorer.extractRects(jsSvg, "bar")
            .ifEmpty { SvgScorer.extractRects(jsSvg) }
        val kmpRects = SvgScorer.extractRects(kmpSvg, "bar")
            .ifEmpty { SvgScorer.extractRects(kmpSvg) }
        dims += SvgScorer.scoreRects(jsRects, kmpRects, "bars", 0.30, 0.5, 30.0)

        // line path
        val reD = Regex("""\bd="(M[^"]+)"""")
        val jsPaths = reD.findAll(jsSvg).map { it.groupValues[1] }.toList()
        val kmpPaths = reD.findAll(kmpSvg).map { it.groupValues[1] }.toList()
        dims += SvgScorer.scorePaths(jsPaths, kmpPaths, "lines", 0.25, 0.5, 50.0)

        val jsTexts = SvgScorer.extractTextContents(jsSvg)
        val kmpTexts = SvgScorer.extractTextContents(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsTexts, kmpTexts, "labels", 0.20)

        dims += SvgScorer.scoreStructureCounts(
            listOf(jsRects.size to kmpRects.size, jsTexts.size to kmpTexts.size),
            listOf("bars", "labels"),
            0.20
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}

/**
 * Sankey 评分策略
 */
object SankeyStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "sankey"

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.10)

        // 节点矩形
        val jsRects = SvgScorer.extractRects(jsSvg, "sankey-node")
            .ifEmpty { SvgScorer.extractRects(jsSvg) }
        val kmpRects = SvgScorer.extractRects(kmpSvg, "sankey-node")
            .ifEmpty { SvgScorer.extractRects(kmpSvg) }
        dims += SvgScorer.scoreRects(jsRects, kmpRects, "nodes", 0.25, 0.5, 30.0)

        // 流路径
        val reD = Regex("""\bd="(M[^"]+)"""")
        val jsPaths = reD.findAll(jsSvg).map { it.groupValues[1] }
            .filter { it.contains("C") }.toList()
        val kmpPaths = reD.findAll(kmpSvg).map { it.groupValues[1] }
            .filter { it.contains("C") }.toList()
        dims += SvgScorer.scorePaths(jsPaths, kmpPaths, "flows", 0.30, 1.0, 50.0)

        val jsTexts = SvgScorer.extractTextContents(jsSvg)
        val kmpTexts = SvgScorer.extractTextContents(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsTexts, kmpTexts, "labels", 0.15)

        dims += SvgScorer.scoreStructureCounts(
            listOf(jsRects.size to kmpRects.size, jsPaths.size to kmpPaths.size),
            listOf("nodes", "flows"),
            0.20
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}

/**
 * Radar 评分策略
 */
object RadarStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "radar"

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.10)

        // 数据区域路径
        val reD = Regex("""\bd="(M[^"]+)"""")
        val jsPaths = reD.findAll(jsSvg).map { it.groupValues[1] }.toList()
        val kmpPaths = reD.findAll(kmpSvg).map { it.groupValues[1] }.toList()
        dims += SvgScorer.scorePaths(jsPaths, kmpPaths, "radarPaths", 0.35, 0.5, 50.0)

        val jsTexts = SvgScorer.extractTextContents(jsSvg)
        val kmpTexts = SvgScorer.extractTextContents(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsTexts, kmpTexts, "labels", 0.25)

        dims += SvgScorer.scoreStructureCounts(
            listOf(jsPaths.size to kmpPaths.size, jsTexts.size to kmpTexts.size),
            listOf("paths", "labels"),
            0.30
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}

/**
 * Requirement 评分策略
 */
object RequirementStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "requirement"

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.05)

        val jsNodes = SvgScorer.extractGroupTranslates(jsSvg, "node")
        val kmpNodes = SvgScorer.extractGroupTranslates(kmpSvg, "node")
        dims += SvgScorer.scorePositions(jsNodes, kmpNodes, "elements", 0.30, 0.5, 30.0)

        val jsRects = SvgScorer.extractRects(jsSvg)
        val kmpRects = SvgScorer.extractRects(kmpSvg)
        dims += SvgScorer.scoreRects(jsRects, kmpRects, "boxes", 0.20, 0.5, 30.0)

        val jsTexts = SvgScorer.extractTextContents(jsSvg)
        val kmpTexts = SvgScorer.extractTextContents(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsTexts, kmpTexts, "labels", 0.20)

        dims += SvgScorer.scoreMarkers(jsSvg, kmpSvg, 0.05)

        dims += SvgScorer.scoreStructureCounts(
            listOf(jsNodes.size to kmpNodes.size, jsRects.size to kmpRects.size),
            listOf("elements", "boxes"),
            0.20
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}

/**
 * Kanban 评分策略
 */
object KanbanStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "kanban"

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.10)

        val jsRects = SvgScorer.extractRects(jsSvg)
        val kmpRects = SvgScorer.extractRects(kmpSvg)
        dims += SvgScorer.scoreRects(jsRects, kmpRects, "cards", 0.30, 0.5, 30.0)

        val jsTexts = SvgScorer.extractTextContents(jsSvg)
        val kmpTexts = SvgScorer.extractTextContents(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsTexts, kmpTexts, "labels", 0.30)

        dims += SvgScorer.scoreStructureCounts(
            listOf(jsRects.size to kmpRects.size, jsTexts.size to kmpTexts.size),
            listOf("cards", "labels"),
            0.30
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}

/**
 * Block 评分策略
 */
object BlockStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "block"

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.05)

        val jsNodes = SvgScorer.extractGroupTranslates(jsSvg, "node")
        val kmpNodes = SvgScorer.extractGroupTranslates(kmpSvg, "node")
        dims += SvgScorer.scorePositions(jsNodes, kmpNodes, "blocks", 0.30, 0.5, 30.0)

        val jsRects = SvgScorer.extractRects(jsSvg)
        val kmpRects = SvgScorer.extractRects(kmpSvg)
        dims += SvgScorer.scoreRects(jsRects, kmpRects, "boxes", 0.20, 0.5, 30.0)

        val jsTexts = SvgScorer.extractTextContents(jsSvg)
        val kmpTexts = SvgScorer.extractTextContents(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsTexts, kmpTexts, "labels", 0.20)

        dims += SvgScorer.scoreMarkers(jsSvg, kmpSvg, 0.05)

        dims += SvgScorer.scoreStructureCounts(
            listOf(jsNodes.size to kmpNodes.size, jsRects.size to kmpRects.size),
            listOf("blocks", "boxes"),
            0.20
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}

/**
 * Architecture 评分策略
 */
object ArchitectureStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "architecture"

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.05)

        val jsNodes = SvgScorer.extractGroupTranslates(jsSvg, "node")
            .ifEmpty { SvgScorer.extractGroupTranslates(jsSvg, "architecture") }
        val kmpNodes = SvgScorer.extractGroupTranslates(kmpSvg, "node")
            .ifEmpty { SvgScorer.extractGroupTranslates(kmpSvg, "architecture") }
        dims += SvgScorer.scorePositions(jsNodes, kmpNodes, "services", 0.30, 0.5, 30.0)

        val jsRects = SvgScorer.extractRects(jsSvg)
        val kmpRects = SvgScorer.extractRects(kmpSvg)
        dims += SvgScorer.scoreRects(jsRects, kmpRects, "groups", 0.20, 0.5, 30.0)

        val jsTexts = SvgScorer.extractTextContents(jsSvg)
        val kmpTexts = SvgScorer.extractTextContents(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsTexts, kmpTexts, "labels", 0.20)

        dims += SvgScorer.scoreMarkers(jsSvg, kmpSvg, 0.05)

        dims += SvgScorer.scoreStructureCounts(
            listOf(jsNodes.size to kmpNodes.size, jsRects.size to kmpRects.size),
            listOf("services", "groups"),
            0.20
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}

/**
 * Packet 评分策略
 */
object PacketStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "packet"

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.10)

        val jsRects = SvgScorer.extractRects(jsSvg)
        val kmpRects = SvgScorer.extractRects(kmpSvg)
        dims += SvgScorer.scoreRects(jsRects, kmpRects, "fields", 0.35, 0.5, 30.0)

        val jsTexts = SvgScorer.extractTextContents(jsSvg)
        val kmpTexts = SvgScorer.extractTextContents(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsTexts, kmpTexts, "labels", 0.25)

        dims += SvgScorer.scoreStructureCounts(
            listOf(jsRects.size to kmpRects.size, jsTexts.size to kmpTexts.size),
            listOf("fields", "labels"),
            0.30
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}

/**
 * Ishikawa (鱼骨图) 评分策略
 */
object IshikawaStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "ishikawa"

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.10)

        // 鱼骨线条
        val jsLines = SvgScorer.extractLines(jsSvg)
        val kmpLines = SvgScorer.extractLines(kmpSvg)
        dims += SvgScorer.scoreLines(jsLines, kmpLines, "bones", 0.30, 0.5, 50.0)

        val jsTexts = SvgScorer.extractTextContents(jsSvg)
        val kmpTexts = SvgScorer.extractTextContents(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsTexts, kmpTexts, "labels", 0.30)

        dims += SvgScorer.scoreStructureCounts(
            listOf(jsLines.size to kmpLines.size, jsTexts.size to kmpTexts.size),
            listOf("bones", "labels"),
            0.30
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}

/**
 * Venn 评分策略
 */
object VennStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "venn"

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.10)

        // 韦恩圆/椭圆
        val jsCircles = SvgScorer.extractCircles(jsSvg)
        val kmpCircles = SvgScorer.extractCircles(kmpSvg)
        val jsPos = jsCircles.map { it.cx to it.cy }
        val kmpPos = kmpCircles.map { it.cx to it.cy }
        dims += SvgScorer.scorePositions(jsPos, kmpPos, "circles", 0.30, 0.5, 30.0)

        val jsTexts = SvgScorer.extractTextContents(jsSvg)
        val kmpTexts = SvgScorer.extractTextContents(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsTexts, kmpTexts, "labels", 0.25)

        dims += SvgScorer.scoreStructureCounts(
            listOf(jsCircles.size to kmpCircles.size, jsTexts.size to kmpTexts.size),
            listOf("circles", "labels"),
            0.35
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}

/**
 * Treemap 评分策略
 */
object TreemapStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "treemap"

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.10)

        val jsRects = SvgScorer.extractRects(jsSvg)
        val kmpRects = SvgScorer.extractRects(kmpSvg)
        dims += SvgScorer.scoreRects(jsRects, kmpRects, "cells", 0.35, 0.5, 30.0)

        val jsTexts = SvgScorer.extractTextContents(jsSvg)
        val kmpTexts = SvgScorer.extractTextContents(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsTexts, kmpTexts, "labels", 0.25)

        dims += SvgScorer.scoreStructureCounts(
            listOf(jsRects.size to kmpRects.size, jsTexts.size to kmpTexts.size),
            listOf("cells", "labels"),
            0.30
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}
