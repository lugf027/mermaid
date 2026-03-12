package io.lugf027.github.mermaid.eval

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * SVG 差异评分器 — 对比 mermaid-kmp 和 mermaid-js 生成的 SVG，输出 [0.0, 1.0] 综合得分
 *
 * 评分维度及权重:
 * - viewBox / max-width 对齐度 (15%)
 * - 节点位置精度 (30%)
 * - 边路径精度 (30%)
 * - 边 CSS 类匹配 (10%)
 * - marker 匹配 (5%)
 * - 结构完整性：节点数/边数一致性 (10%)
 */
object SvgScorer {

    // ── 权重 ──────────────────────────────────────────
    // viewBox 只作为参考，因无 DOM 环境下文本宽度估算存在系统性差异
    private const val W_VIEWBOX = 0.05
    private const val W_NODES = 0.35
    private const val W_EDGES = 0.35
    private const val W_CSS = 0.10
    private const val W_MARKER = 0.05
    private const val W_STRUCTURE = 0.10

    // ── 像素容差 ─────────────────────────────────────
    /** viewBox 宽高相对差异 <= 此值得满分 (0.1%) */
    private const val VB_PERFECT_RATIO = 0.001
    /** viewBox 宽高相对差异 >= 此值得零分 (50%) */
    private const val VB_ZERO_RATIO = 0.50

    /** 节点位置差异满分阈值 */
    private const val NODE_PERFECT_PX = 0.05
    /** 节点位置差异零分阈值 */
    private const val NODE_ZERO_PX = 30.0

    /** 边路径差异满分阈值 */
    private const val EDGE_PERFECT_PX = 0.5
    /** 边路径差异零分阈值 */
    private const val EDGE_ZERO_PX = 50.0

    // ── 正则 ─────────────────────────────────────────
    private val RE_VIEWBOX = Regex("""viewBox="([^"]+)"""")
    private val RE_MAX_WIDTH = Regex("""max-width:\s*([\d.]+)""")
    private val RE_NODE = Regex("""<g[^>]*class="[^"]*node[^"]*"[^>]*transform="translate\(([^)]+)\)"""")
    private val RE_FLOWCHART_LINK_PATH = Regex("""<path\s[^>]*?class="[^"]*flowchart-link[^"]*"[^>]*/>""")
    private val RE_DATA_EDGE_PATH = Regex("""<path\s[^>]*?data-edge="true"[^>]*/>""")
    private val RE_D_ATTR = Regex("""\bd="(M[^"]+)"""")
    private val RE_NUMBER = Regex("""[-+]?\d*\.?\d+""")
    private val RE_EDGE_CLASS = Regex("""class="([^"]*(?:edge|flowchart-link)[^"]*?)"""")
    private val RE_MARKER_END = Regex("""marker-end="url\(#([^)]+)\)"""")
    private val RE_MARKER_START = Regex("""marker-start="url\(#([^)]+)\)"""")

    // ════════════════════════════════════════════════════
    //  公共 API
    // ════════════════════════════════════════════════════

    /**
     * 对比两段 SVG 文本，返回 [ScoreResult]
     */
    fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val dims = mutableListOf<DimensionScore>()

        // ① viewBox + max-width
        dims += scoreViewBox(jsSvg, kmpSvg)

        // ② 节点位置
        val jsNodes = extractNodes(jsSvg)
        val kmpNodes = extractNodes(kmpSvg)
        dims += scoreNodes(jsNodes, kmpNodes)

        // ③ 边路径
        val jsPaths = extractEdgePaths(jsSvg)
        val kmpPaths = extractEdgePaths(kmpSvg)
        dims += scoreEdges(jsPaths, kmpPaths)

        // ④ 边 CSS 类
        val jsClasses = RE_EDGE_CLASS.findAll(jsSvg).map { it.groupValues[1] }.toList()
        val kmpClasses = RE_EDGE_CLASS.findAll(kmpSvg).map { it.groupValues[1] }.toList()
        dims += scoreCssClasses(jsClasses, kmpClasses)

        // ⑤ marker
        dims += scoreMarkers(jsSvg, kmpSvg)

        // ⑥ 结构完整性
        dims += scoreStructure(jsNodes.size, kmpNodes.size, jsPaths.size, kmpPaths.size)

        // 加权总分
        val total = dims.sumOf { it.weight * it.score }
        return ScoreResult(total = total, dimensions = dims)
    }

    // ════════════════════════════════════════════════════
    //  内部评分方法
    // ════════════════════════════════════════════════════

    private fun scoreViewBox(jsSvg: String, kmpSvg: String): DimensionScore {
        val jsVb = extractViewBox(jsSvg)
        val kmpVb = extractViewBox(kmpSvg)

        if (jsVb == null && kmpVb == null) {
            return DimensionScore("viewBox", 1.0, W_VIEWBOX, "both missing, trivially equal")
        }
        if (jsVb == null || kmpVb == null) {
            return DimensionScore("viewBox", 0.0, W_VIEWBOX, "one side missing viewBox")
        }

        // mmdc (Puppeteer) 通常给 viewBox 加固定 padding: 偏移 (-8, -9), 宽高 (+16, +18)
        // 规范化为实际内容区域大小进行比较
        val jsNorm = normalizeViewBox(jsVb)
        val kmpNorm = normalizeViewBox(kmpVb)

        // 使用相对差异比来评估（避免文本宽度估算的系统性偏差造成不公平的绝对值惩罚）
        // 取 width 和 height 的相对差异比的最大值
        val widthRatio = relativeDiff(jsNorm.getOrElse(2) { 0.0 }, kmpNorm.getOrElse(2) { 0.0 })
        val heightRatio = relativeDiff(jsNorm.getOrElse(3) { 0.0 }, kmpNorm.getOrElse(3) { 0.0 })
        val maxRatio = max(widthRatio, heightRatio)

        val s = linearScore(maxRatio, VB_PERFECT_RATIO, VB_ZERO_RATIO)
        val absDiff = jsNorm.zip(kmpNorm).maxOfOrNull { (a, b) -> abs(a - b) } ?: 0.0
        return DimensionScore("viewBox", s, W_VIEWBOX,
            "relDiff=%.1f%%, absDiff=%.3fpx".format(maxRatio * 100, absDiff))
    }

    /**
     * 规范化 viewBox: 如果有负偏移（mmdc padding），将 [x,y,w,h] 转为 [0,0, w+2*x, h+2*y]
     * 即去掉 padding 影响，只比较实际内容区域
     */
    private fun normalizeViewBox(vb: List<Double>): List<Double> {
        if (vb.size < 4) return vb
        val (x, y, w, h) = vb
        return if (x < 0 || y < 0) {
            listOf(0.0, 0.0, w - abs(x) * 2, h - abs(y) * 2)
        } else {
            vb
        }
    }

    /** 两个值的相对差异比 (0~1) */
    private fun relativeDiff(a: Double, b: Double): Double {
        val denom = max(abs(a), abs(b))
        return if (denom < 1e-9) 0.0 else abs(a - b) / denom
    }

    private fun scoreNodes(jsNodes: List<Pair<Double, Double>>, kmpNodes: List<Pair<Double, Double>>): DimensionScore {
        if (jsNodes.isEmpty() && kmpNodes.isEmpty()) {
            return DimensionScore("nodes", 1.0, W_NODES, "no nodes")
        }
        if (jsNodes.isEmpty() || kmpNodes.isEmpty()) {
            return DimensionScore("nodes", 0.0, W_NODES, "one side has no nodes")
        }
        val pairCount = min(jsNodes.size, kmpNodes.size)
        var sumScore = 0.0
        var maxDiff = 0.0
        for (i in 0 until pairCount) {
            val (jx, jy) = jsNodes[i]
            val (kx, ky) = kmpNodes[i]
            val diff = max(abs(jx - kx), abs(jy - ky))
            maxDiff = max(maxDiff, diff)
            sumScore += linearScore(diff, NODE_PERFECT_PX, NODE_ZERO_PX)
        }
        // 数量不匹配的惩罚
        val countPenalty = if (jsNodes.size != kmpNodes.size) {
            val missing = abs(jsNodes.size - kmpNodes.size)
            missing.toDouble() / max(jsNodes.size, kmpNodes.size)
        } else 0.0
        val avg = sumScore / pairCount
        val s = avg * (1.0 - countPenalty)
        return DimensionScore("nodes", s, W_NODES,
            "maxDiff=%.3fpx, paired=%d/%d/%d".format(maxDiff, pairCount, jsNodes.size, kmpNodes.size))
    }

    private fun scoreEdges(jsPaths: List<String>, kmpPaths: List<String>): DimensionScore {
        if (jsPaths.isEmpty() && kmpPaths.isEmpty()) {
            return DimensionScore("edges", 1.0, W_EDGES, "no edges")
        }
        if (jsPaths.isEmpty() || kmpPaths.isEmpty()) {
            return DimensionScore("edges", 0.0, W_EDGES, "one side has no edges")
        }
        val pairCount = min(jsPaths.size, kmpPaths.size)
        var sumScore = 0.0
        var maxDiff = 0.0
        for (i in 0 until pairCount) {
            val jNums = extractNums(jsPaths[i])
            val kNums = extractNums(kmpPaths[i])
            val numPairs = min(jNums.size, kNums.size)
            if (numPairs == 0) {
                // 空路径
                sumScore += 0.0
                continue
            }
            var pathMaxDiff = 0.0
            for (j in 0 until numPairs) {
                pathMaxDiff = max(pathMaxDiff, abs(jNums[j] - kNums[j]))
            }
            // 数值数量不同也有轻微惩罚
            val numPenalty = if (jNums.size != kNums.size) 0.1 else 0.0
            maxDiff = max(maxDiff, pathMaxDiff)
            sumScore += linearScore(pathMaxDiff, EDGE_PERFECT_PX, EDGE_ZERO_PX) * (1.0 - numPenalty)
        }
        val countPenalty = if (jsPaths.size != kmpPaths.size) {
            val missing = abs(jsPaths.size - kmpPaths.size)
            missing.toDouble() / max(jsPaths.size, kmpPaths.size)
        } else 0.0
        val avg = sumScore / pairCount
        val s = avg * (1.0 - countPenalty)
        return DimensionScore("edges", s, W_EDGES,
            "maxDiff=%.3fpx, paired=%d/%d/%d".format(maxDiff, pairCount, jsPaths.size, kmpPaths.size))
    }

    private fun scoreCssClasses(jsClasses: List<String>, kmpClasses: List<String>): DimensionScore {
        if (jsClasses.isEmpty() && kmpClasses.isEmpty()) {
            return DimensionScore("css", 1.0, W_CSS, "no edge classes")
        }
        if (jsClasses.isEmpty() || kmpClasses.isEmpty()) {
            return DimensionScore("css", 0.0, W_CSS, "one side missing edge classes")
        }
        val pairCount = min(jsClasses.size, kmpClasses.size)
        var matched = 0
        for (i in 0 until pairCount) {
            if (jsClasses[i] == kmpClasses[i]) matched++
        }
        val countPenalty = if (jsClasses.size != kmpClasses.size) {
            abs(jsClasses.size - kmpClasses.size).toDouble() / max(jsClasses.size, kmpClasses.size)
        } else 0.0
        val s = (matched.toDouble() / pairCount) * (1.0 - countPenalty)
        return DimensionScore("css", s, W_CSS,
            "matched=%d/%d".format(matched, pairCount))
    }

    private fun scoreMarkers(jsSvg: String, kmpSvg: String): DimensionScore {
        val jsEnds = RE_MARKER_END.findAll(jsSvg).map { normalizeMarker(it.groupValues[1]) }.toList()
        val kmpEnds = RE_MARKER_END.findAll(kmpSvg).map { normalizeMarker(it.groupValues[1]) }.toList()
        val jsStarts = RE_MARKER_START.findAll(jsSvg).map { normalizeMarker(it.groupValues[1]) }.toList()
        val kmpStarts = RE_MARKER_START.findAll(kmpSvg).map { normalizeMarker(it.groupValues[1]) }.toList()

        val allJs = jsEnds + jsStarts
        val allKmp = kmpEnds + kmpStarts
        if (allJs.isEmpty() && allKmp.isEmpty()) {
            return DimensionScore("markers", 1.0, W_MARKER, "no markers")
        }
        if (allJs.isEmpty() || allKmp.isEmpty()) {
            return DimensionScore("markers", 0.0, W_MARKER, "one side missing markers")
        }
        val pairCount = min(allJs.size, allKmp.size)
        var matched = 0
        for (i in 0 until pairCount) {
            if (allJs[i] == allKmp[i]) matched++
        }
        val countPenalty = if (allJs.size != allKmp.size) {
            abs(allJs.size - allKmp.size).toDouble() / max(allJs.size, allKmp.size)
        } else 0.0
        val s = (matched.toDouble() / pairCount) * (1.0 - countPenalty)
        return DimensionScore("markers", s, W_MARKER,
            "matched=%d/%d".format(matched, pairCount))
    }

    private fun scoreStructure(
        jsNodeCount: Int, kmpNodeCount: Int,
        jsEdgeCount: Int, kmpEdgeCount: Int
    ): DimensionScore {
        val nodeRatio = if (max(jsNodeCount, kmpNodeCount) > 0) {
            min(jsNodeCount, kmpNodeCount).toDouble() / max(jsNodeCount, kmpNodeCount)
        } else 1.0
        val edgeRatio = if (max(jsEdgeCount, kmpEdgeCount) > 0) {
            min(jsEdgeCount, kmpEdgeCount).toDouble() / max(jsEdgeCount, kmpEdgeCount)
        } else 1.0
        val s = (nodeRatio + edgeRatio) / 2.0
        return DimensionScore("structure", s, W_STRUCTURE,
            "nodes=%d/%d, edges=%d/%d".format(jsNodeCount, kmpNodeCount, jsEdgeCount, kmpEdgeCount))
    }

    // ════════════════════════════════════════════════════
    //  SVG 特征提取
    // ════════════════════════════════════════════════════

    private fun extractViewBox(svg: String): List<Double>? {
        val m = RE_VIEWBOX.find(svg) ?: return null
        return m.groupValues[1].split(" ", ",").mapNotNull { it.trim().toDoubleOrNull() }
    }

    private fun extractMaxWidth(svg: String): Double? {
        val m = RE_MAX_WIDTH.find(svg) ?: return null
        return m.groupValues[1].toDoubleOrNull()
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
        // 优先匹配 flowchart-link 类
        val fromLink = RE_FLOWCHART_LINK_PATH.findAll(svg).mapNotNull { m ->
            RE_D_ATTR.find(m.value)?.groupValues?.get(1)
        }.toList()
        if (fromLink.isNotEmpty()) return fromLink

        // 备选：data-edge 属性
        val fromDataEdge = RE_DATA_EDGE_PATH.findAll(svg).mapNotNull { m ->
            RE_D_ATTR.find(m.value)?.groupValues?.get(1)
        }.toList()
        if (fromDataEdge.isNotEmpty()) return fromDataEdge

        // 最后备选：所有含贝塞尔曲线的长路径
        return RE_D_ATTR.findAll(svg).map { it.groupValues[1] }
            .filter { it.contains("C") && it.length > 50 }
            .toList()
    }

    private fun extractNums(pathD: String): List<Double> {
        return RE_NUMBER.findAll(pathD).map { it.value.toDouble() }.toList()
    }

    private fun normalizeMarker(marker: String): String {
        // mermaid-js marker 含 flowchart-xxx 前缀，取最后一段统一比较
        return if (marker.contains("-")) marker.substringAfterLast("-") else marker
    }

    // ════════════════════════════════════════════════════
    //  工具方法
    // ════════════════════════════════════════════════════

    /** 线性映射：diff <= perfect → 1.0, diff >= zero → 0.0 */
    private fun linearScore(diff: Double, perfect: Double, zero: Double): Double {
        if (diff <= perfect) return 1.0
        if (diff >= zero) return 0.0
        return 1.0 - (diff - perfect) / (zero - perfect)
    }
}

// ════════════════════════════════════════════════════════
//  数据模型
// ════════════════════════════════════════════════════════

/** 单个维度的评分 */
data class DimensionScore(
    val name: String,
    val score: Double,
    val weight: Double,
    val detail: String
)

/** 综合评分结果 */
data class ScoreResult(
    val total: Double,
    val dimensions: List<DimensionScore>
) {
    /** 是否通过（默认阈值 0.95） */
    fun passed(threshold: Double = 0.95): Boolean = total >= threshold
}
