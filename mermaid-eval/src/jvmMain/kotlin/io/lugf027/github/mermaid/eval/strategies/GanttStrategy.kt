package io.lugf027.github.mermaid.eval.strategies

import io.lugf027.github.mermaid.eval.*

/**
 * Gantt 图评分策略
 *
 * 评分维度:
 * - viewBox (5%)
 * - 任务条位置和大小 (35%) — task bar rect 的 x/y/width/height
 * - 文本标签 (25%) — task name/section name/date labels
 * - 网格线/坐标轴 (15%) — grid line 位置
 * - 结构 (20%) — task/section 数量
 */
object GanttStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "gantt"

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        // ① viewBox
        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.05)

        // ② 任务条
        val jsRects = SvgScorer.extractRects(jsSvg, "task")
        val kmpRects = SvgScorer.extractRects(kmpSvg, "task")
        dims += SvgScorer.scoreRects(jsRects, kmpRects, "taskBars", 0.35, 0.5, 30.0)

        // ③ 文本标签
        val jsTexts = SvgScorer.extractTextContents(jsSvg)
        val kmpTexts = SvgScorer.extractTextContents(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsTexts, kmpTexts, "labels", 0.25)

        // ④ 网格线
        val jsLines = SvgScorer.extractLines(jsSvg, "grid")
        val kmpLines = SvgScorer.extractLines(kmpSvg, "grid")
        dims += SvgScorer.scoreLines(jsLines, kmpLines, "gridLines", 0.15, 1.0, 50.0)

        // ⑤ 结构
        val jsSections = SvgScorer.countElementsByClass(jsSvg, "section")
        val kmpSections = SvgScorer.countElementsByClass(kmpSvg, "section")
        dims += SvgScorer.scoreStructureCounts(
            listOf(jsRects.size to kmpRects.size, jsSections to kmpSections),
            listOf("tasks", "sections"),
            0.20
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}
