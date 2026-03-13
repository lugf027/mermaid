package io.lugf027.github.mermaid.eval.strategies

import io.lugf027.github.mermaid.eval.*

/**
 * Sequence 图评分策略
 *
 * 评分维度:
 * - viewBox (5%)
 * - Actor 位置 (25%) — actor 的 translate 坐标
 * - 消息线/箭头 (30%) — message line 的 path/line 坐标
 * - 激活框 (15%) — activation rect 的位置和大小
 * - 文本标签 (10%) — actor label/message text
 * - 结构 (15%) — actor/message/activation 数量
 */
object SequenceStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "sequence"

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        // ① viewBox
        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.05)

        // ② Actor 位置
        val jsActors = SvgScorer.extractGroupTranslates(jsSvg, "actor")
        val kmpActors = SvgScorer.extractGroupTranslates(kmpSvg, "actor")
        dims += SvgScorer.scorePositions(jsActors, kmpActors, "actors", 0.25, 0.5, 30.0)

        // ③ 消息线 — 提取 messageLine 类的 line 元素
        val jsLines = SvgScorer.extractLines(jsSvg, "messageLine")
        val kmpLines = SvgScorer.extractLines(kmpSvg, "messageLine")
        dims += SvgScorer.scoreLines(jsLines, kmpLines, "messages", 0.30, 0.5, 50.0)

        // ④ 激活框
        val jsActivations = SvgScorer.extractRects(jsSvg, "activation")
        val kmpActivations = SvgScorer.extractRects(kmpSvg, "activation")
        dims += SvgScorer.scoreRects(jsActivations, kmpActivations, "activations", 0.15, 0.5, 30.0)

        // ⑤ 文本标签
        val jsTexts = SvgScorer.extractTextContents(jsSvg)
        val kmpTexts = SvgScorer.extractTextContents(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsTexts, kmpTexts, "labels", 0.10)

        // ⑥ 结构
        dims += SvgScorer.scoreStructureCounts(
            listOf(
                jsActors.size to kmpActors.size,
                jsLines.size to kmpLines.size,
                jsActivations.size to kmpActivations.size
            ),
            listOf("actors", "messages", "activations"),
            0.15
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }
}
