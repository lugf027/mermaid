package io.lugf027.github.mermaid.eval.strategies

import io.lugf027.github.mermaid.eval.*

/**
 * Pie 图评分策略
 *
 * 评分维度:
 * - viewBox (10%)
 * - 扇形路径 (35%) — 比较 arc path 的 d 属性数值
 * - 标签文本 (20%) — 比较文字内容
 * - CSS 类/颜色 (15%) — 比较 fill 等样式
 * - 结构 (20%) — 扇形数量、文本元素数量
 */
object PieStrategy : SvgScorer.ScoringStrategy {
    override val typeName = "pie"

    private val RE_PIE_ARC_PATH = Regex("""<path[^>]*class="[^"]*pieCircle[^"]*"[^>]*""")
    private val RE_D_ATTR = Regex("""\bd="([^"]+)"""")
    private val RE_SLICE_CLASS = Regex("""class="([^"]*pieCircle[^"]*?)"""")

    override fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        // ① viewBox
        dims += SvgScorer.scoreViewBox(jsSvg, kmpSvg, 0.10)

        // ② 扇形 arc 路径
        val jsArcs = extractArcPaths(jsSvg)
        val kmpArcs = extractArcPaths(kmpSvg)
        dims += SvgScorer.scorePaths(jsArcs, kmpArcs, "arcs", 0.35, 0.5, 30.0)

        // ③ 标签文本
        val jsTexts = SvgScorer.extractTextContents(jsSvg)
        val kmpTexts = SvgScorer.extractTextContents(kmpSvg)
        dims += SvgScorer.scoreStringMatch(jsTexts, kmpTexts, "labels", 0.20)

        // ④ CSS 类
        val jsClasses = RE_SLICE_CLASS.findAll(jsSvg).map { it.groupValues[1] }.toList()
        val kmpClasses = RE_SLICE_CLASS.findAll(kmpSvg).map { it.groupValues[1] }.toList()
        dims += SvgScorer.scoreStringMatch(jsClasses, kmpClasses, "css", 0.15)

        // ⑤ 结构
        dims += SvgScorer.scoreStructureCounts(
            listOf(jsArcs.size to kmpArcs.size, jsTexts.size to kmpTexts.size),
            listOf("arcs", "labels"),
            0.20
        )

        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims, diagramType = typeName)
    }

    private fun extractArcPaths(svg: String): List<String> {
        return RE_PIE_ARC_PATH.findAll(svg).mapNotNull { m ->
            RE_D_ATTR.find(m.value)?.groupValues?.get(1)
        }.toList()
    }
}
