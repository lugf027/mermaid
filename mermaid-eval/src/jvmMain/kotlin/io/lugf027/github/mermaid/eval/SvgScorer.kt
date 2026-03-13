package io.lugf027.github.mermaid.eval

import io.lugf027.github.mermaid.eval.strategies.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * SVG 差异评分器 — 对比 mermaid-kmp 和 mermaid-js 生成的 SVG，输出 [0.0, 1.0] 综合得分
 *
 * 支持多图表类型：通过自动检测 SVG 中的图表类型标识，选择对应的评分策略。
 * 每种评分策略定义了该图表类型特有的 SVG 特征提取方式和评分维度/权重。
 *
 * 通用评分维度:
 * - viewBox / max-width 对齐度
 * - SVG 元素结构相似度
 * - 路径/形状精度
 * - CSS 类名匹配
 * - 文本内容匹配
 */
object SvgScorer {

    // ── 通用正则 ─────────────────────────────────────
    private val RE_VIEWBOX = Regex("""viewBox="([^"]+)"""")
    private val RE_MAX_WIDTH = Regex("""max-width:\s*([\d.]+)""")
    private val RE_NUMBER = Regex("""[-+]?\d*\.?\d+""")
    private val RE_D_ATTR = Regex("""\bd="(M[^"]+)"""")
    private val RE_MARKER_END = Regex("""marker-end="url\(#([^)]+)\)"""")
    private val RE_MARKER_START = Regex("""marker-start="url\(#([^)]+)\)"""")

    // ── 图表类型检测正则 ─────────────────────────────
    private val RE_DETECT_FLOWCHART = Regex("""class="[^"]*flowchart[^"]*"""")
    private val RE_DETECT_PIE = Regex("""class="[^"]*pieCircle[^"]*"|class="[^"]*pie[^"]*"""")
    private val RE_DETECT_SEQUENCE = Regex("""class="[^"]*actor[^"]*"|class="[^"]*sequenceDiagram[^"]*"""")
    private val RE_DETECT_CLASS = Regex("""class="[^"]*classGroup[^"]*"|class="[^"]*classDiagram[^"]*"""")
    private val RE_DETECT_STATE = Regex("""class="[^"]*stateDiagram[^"]*"|class="[^"]*state-note[^"]*"""")
    private val RE_DETECT_ER = Regex("""class="erDiagram"|class="[^"]*\bmarker\b[^"]*\ber\b[^"]*"""")
    private val RE_DETECT_GANTT = Regex("""class="[^"]*gantt[^"]*"|class="[^"]*section[0-9]+[^"]*"""")
    private val RE_DETECT_GITGRAPH = Regex("""class="[^"]*gitGraph[^"]*"|class="[^"]*commit[^"]*"""")
    private val RE_DETECT_JOURNEY = Regex("""class="[^"]*journey[^"]*"""")
    private val RE_DETECT_MINDMAP = Regex("""class="[^"]*mindmap[^"]*"""")
    private val RE_DETECT_TIMELINE = Regex("""class="[^"]*timeline[^"]*"""")
    private val RE_DETECT_C4 = Regex("""class="[^"]*c4[^"]*"""")
    private val RE_DETECT_QUADRANT = Regex("""class="[^"]*quadrant[^"]*"""")
    private val RE_DETECT_XYCHART = Regex("""class="[^"]*xychart[^"]*"""")
    private val RE_DETECT_SANKEY = Regex("""class="[^"]*sankey[^"]*"""")
    private val RE_DETECT_RADAR = Regex("""class="[^"]*radar[^"]*"""")
    private val RE_DETECT_REQUIREMENT = Regex("""class="requirementDiagram"|class="[^"]*requirement-diagram[^"]*"|class="[^"]*requirement[^"]*"""")
    private val RE_DETECT_KANBAN = Regex("""class="[^"]*kanban[^"]*"""")
    private val RE_DETECT_BLOCK = Regex("""class="[^"]*block[^"]*"""")
    private val RE_DETECT_ARCHITECTURE = Regex("""class="[^"]*architecture[^"]*"""")
    private val RE_DETECT_PACKET = Regex("""class="[^"]*packet[^"]*"""")
    private val RE_DETECT_ISHIKAWA = Regex("""class="[^"]*ishikawa[^"]*"""")
    private val RE_DETECT_VENN = Regex("""class="[^"]*venn[^"]*"""")
    private val RE_DETECT_TREEMAP = Regex("""class="[^"]*treemap[^"]*"""")

    // ════════════════════════════════════════════════════
    //  公共 API
    // ════════════════════════════════════════════════════

    /**
     * 对比两段 SVG 文本，返回 [ScoreResult]
     * 自动检测图表类型并选择对应的评分策略
     */
    fun score(jsSvg: String, kmpSvg: String): ScoreResult {
        val diagramType = detectDiagramType(jsSvg, kmpSvg)
        val strategy = getStrategy(diagramType)
        return strategy.score(jsSvg, kmpSvg)
    }

    /**
     * 指定图表类型进行评分
     */
    fun score(jsSvg: String, kmpSvg: String, diagramType: String): ScoreResult {
        val strategy = getStrategy(diagramType)
        return strategy.score(jsSvg, kmpSvg)
    }

    /**
     * 检测 SVG 中的图表类型
     */
    fun detectDiagramType(jsSvg: String, kmpSvg: String): String {
        // 优先从 JS SVG 检测（更权威），然后从 KMP SVG 检测
        return detectFromSvg(jsSvg) ?: detectFromSvg(kmpSvg) ?: "generic"
    }

    // ════════════════════════════════════════════════════
    //  图表类型检测
    // ════════════════════════════════════════════════════

    private fun detectFromSvg(svg: String): String? {
        // 按特异性从高到低检测，避免 false positive
        return when {
            RE_DETECT_SEQUENCE.containsMatchIn(svg) -> "sequence"
            RE_DETECT_REQUIREMENT.containsMatchIn(svg) -> "requirement"  // before ER to avoid false match
            RE_DETECT_ER.containsMatchIn(svg) -> "er"
            RE_DETECT_STATE.containsMatchIn(svg) -> "state"
            RE_DETECT_CLASS.containsMatchIn(svg) -> "class"
            RE_DETECT_GANTT.containsMatchIn(svg) -> "gantt"
            RE_DETECT_PIE.containsMatchIn(svg) -> "pie"
            RE_DETECT_GITGRAPH.containsMatchIn(svg) -> "gitGraph"
            RE_DETECT_JOURNEY.containsMatchIn(svg) -> "journey"
            RE_DETECT_MINDMAP.containsMatchIn(svg) -> "mindmap"
            RE_DETECT_TIMELINE.containsMatchIn(svg) -> "timeline"
            RE_DETECT_C4.containsMatchIn(svg) -> "c4"
            RE_DETECT_QUADRANT.containsMatchIn(svg) -> "quadrant"
            RE_DETECT_XYCHART.containsMatchIn(svg) -> "xychart"
            RE_DETECT_SANKEY.containsMatchIn(svg) -> "sankey"
            RE_DETECT_RADAR.containsMatchIn(svg) -> "radar"
            RE_DETECT_KANBAN.containsMatchIn(svg) -> "kanban"
            RE_DETECT_BLOCK.containsMatchIn(svg) -> "block"
            RE_DETECT_ARCHITECTURE.containsMatchIn(svg) -> "architecture"
            RE_DETECT_PACKET.containsMatchIn(svg) -> "packet"
            RE_DETECT_ISHIKAWA.containsMatchIn(svg) -> "ishikawa"
            RE_DETECT_VENN.containsMatchIn(svg) -> "venn"
            RE_DETECT_TREEMAP.containsMatchIn(svg) -> "treemap"
            RE_DETECT_FLOWCHART.containsMatchIn(svg) -> "flowchart"
            else -> null
        }
    }

    private fun getStrategy(diagramType: String): ScoringStrategy {
        return when (diagramType) {
            "flowchart" -> FlowchartStrategy
            "pie" -> PieStrategy
            "sequence" -> SequenceStrategy
            "class" -> ClassDiagramStrategy
            "state" -> StateDiagramStrategy
            "er" -> ErDiagramStrategy
            "gantt" -> GanttStrategy
            "gitGraph" -> GitGraphStrategy
            "journey" -> JourneyStrategy
            "mindmap" -> MindmapStrategy
            "timeline" -> TimelineStrategy
            "c4" -> C4Strategy
            "quadrant" -> QuadrantStrategy
            "xychart" -> XYChartStrategy
            "sankey" -> SankeyStrategy
            "radar" -> RadarStrategy
            "requirement" -> RequirementStrategy
            "kanban" -> KanbanStrategy
            "block" -> BlockStrategy
            "architecture" -> ArchitectureStrategy
            "packet" -> PacketStrategy
            "ishikawa" -> IshikawaStrategy
            "venn" -> VennStrategy
            "treemap" -> TreemapStrategy
            else -> GenericStrategy
        }
    }

    // ════════════════════════════════════════════════════
    //  评分策略接口
    // ════════════════════════════════════════════════════

    interface ScoringStrategy {
        val typeName: String
        fun score(jsSvg: String, kmpSvg: String): ScoreResult
    }

    // ════════════════════════════════════════════════════
    //  通用工具方法（所有策略共享）
    // ════════════════════════════════════════════════════

    internal fun extractViewBox(svg: String): List<Double>? {
        val m = RE_VIEWBOX.find(svg) ?: return null
        return m.groupValues[1].split(" ", ",").mapNotNull { it.trim().toDoubleOrNull() }
    }

    internal fun extractMaxWidth(svg: String): Double? {
        val m = RE_MAX_WIDTH.find(svg) ?: return null
        return m.groupValues[1].toDoubleOrNull()
    }

    internal fun extractNums(pathD: String): List<Double> {
        return RE_NUMBER.findAll(pathD).map { it.value.toDouble() }.toList()
    }

    internal fun normalizeMarker(marker: String): String {
        return if (marker.contains("-")) marker.substringAfterLast("-") else marker
    }

    /**
     * 规范化 viewBox: 如果有负偏移（mmdc padding），将 [x,y,w,h] 转为 [0,0, w+2*x, h+2*y]
     */
    internal fun normalizeViewBox(vb: List<Double>): List<Double> {
        if (vb.size < 4) return vb
        val (x, y, w, h) = vb
        return if (x < 0 || y < 0) {
            listOf(0.0, 0.0, w - abs(x) * 2, h - abs(y) * 2)
        } else {
            vb
        }
    }

    /** 两个值的相对差异比 (0~1) */
    internal fun relativeDiff(a: Double, b: Double): Double {
        val denom = max(abs(a), abs(b))
        return if (denom < 1e-9) 0.0 else abs(a - b) / denom
    }

    /** 线性映射：diff <= perfect → 1.0, diff >= zero → 0.0 */
    internal fun linearScore(diff: Double, perfect: Double, zero: Double): Double {
        if (diff <= perfect) return 1.0
        if (diff >= zero) return 0.0
        return 1.0 - (diff - perfect) / (zero - perfect)
    }

    /**
     * 通用 viewBox 评分
     */
    internal fun scoreViewBox(jsSvg: String, kmpSvg: String, weight: Double): DimensionScore {
        val jsVb = extractViewBox(jsSvg)
        val kmpVb = extractViewBox(kmpSvg)

        if (jsVb == null && kmpVb == null) {
            return DimensionScore("viewBox", 1.0, weight, "both missing, trivially equal")
        }
        if (jsVb == null || kmpVb == null) {
            return DimensionScore("viewBox", 0.0, weight, "one side missing viewBox")
        }

        val jsNorm = normalizeViewBox(jsVb)
        val kmpNorm = normalizeViewBox(kmpVb)

        val widthRatio = relativeDiff(jsNorm.getOrElse(2) { 0.0 }, kmpNorm.getOrElse(2) { 0.0 })
        val heightRatio = relativeDiff(jsNorm.getOrElse(3) { 0.0 }, kmpNorm.getOrElse(3) { 0.0 })
        val maxRatio = max(widthRatio, heightRatio)

        val s = linearScore(maxRatio, 0.001, 0.50)
        val absDiff = jsNorm.zip(kmpNorm).maxOfOrNull { (a, b) -> abs(a - b) } ?: 0.0
        return DimensionScore("viewBox", s, weight,
            "relDiff=%.1f%%, absDiff=%.3fpx".format(maxRatio * 100, absDiff))
    }

    /**
     * 通用位置元素评分：比较两组 (x,y) 坐标
     */
    internal fun scorePositions(
        jsPositions: List<Pair<Double, Double>>,
        kmpPositions: List<Pair<Double, Double>>,
        name: String,
        weight: Double,
        perfectPx: Double = 0.05,
        zeroPx: Double = 30.0
    ): DimensionScore {
        if (jsPositions.isEmpty() && kmpPositions.isEmpty()) {
            return DimensionScore(name, 1.0, weight, "no $name")
        }
        if (jsPositions.isEmpty() || kmpPositions.isEmpty()) {
            return DimensionScore(name, 0.0, weight, "one side has no $name")
        }
        val pairCount = min(jsPositions.size, kmpPositions.size)
        var sumScore = 0.0
        var maxDiff = 0.0
        for (i in 0 until pairCount) {
            val (jx, jy) = jsPositions[i]
            val (kx, ky) = kmpPositions[i]
            val diff = max(abs(jx - kx), abs(jy - ky))
            maxDiff = max(maxDiff, diff)
            sumScore += linearScore(diff, perfectPx, zeroPx)
        }
        val countPenalty = if (jsPositions.size != kmpPositions.size) {
            val missing = abs(jsPositions.size - kmpPositions.size)
            missing.toDouble() / max(jsPositions.size, kmpPositions.size)
        } else 0.0
        val avg = sumScore / pairCount
        val s = avg * (1.0 - countPenalty)
        return DimensionScore(name, s, weight,
            "maxDiff=%.3fpx, paired=%d/%d/%d".format(maxDiff, pairCount, jsPositions.size, kmpPositions.size))
    }

    /**
     * 通用路径评分：比较两组 SVG path d 属性
     */
    internal fun scorePaths(
        jsPaths: List<String>,
        kmpPaths: List<String>,
        name: String,
        weight: Double,
        perfectPx: Double = 0.5,
        zeroPx: Double = 50.0
    ): DimensionScore {
        if (jsPaths.isEmpty() && kmpPaths.isEmpty()) {
            return DimensionScore(name, 1.0, weight, "no $name")
        }
        if (jsPaths.isEmpty() || kmpPaths.isEmpty()) {
            return DimensionScore(name, 0.0, weight, "one side has no $name")
        }
        val pairCount = min(jsPaths.size, kmpPaths.size)
        var sumScore = 0.0
        var maxDiff = 0.0
        for (i in 0 until pairCount) {
            val jNums = extractNums(jsPaths[i])
            val kNums = extractNums(kmpPaths[i])
            val numPairs = min(jNums.size, kNums.size)
            if (numPairs == 0) {
                sumScore += 0.0
                continue
            }
            var pathMaxDiff = 0.0
            for (j in 0 until numPairs) {
                pathMaxDiff = max(pathMaxDiff, abs(jNums[j] - kNums[j]))
            }
            val numPenalty = if (jNums.size != kNums.size) 0.1 else 0.0
            maxDiff = max(maxDiff, pathMaxDiff)
            sumScore += linearScore(pathMaxDiff, perfectPx, zeroPx) * (1.0 - numPenalty)
        }
        val countPenalty = if (jsPaths.size != kmpPaths.size) {
            val missing = abs(jsPaths.size - kmpPaths.size)
            missing.toDouble() / max(jsPaths.size, kmpPaths.size)
        } else 0.0
        val avg = sumScore / pairCount
        val s = avg * (1.0 - countPenalty)
        return DimensionScore(name, s, weight,
            "maxDiff=%.3fpx, paired=%d/%d/%d".format(maxDiff, pairCount, jsPaths.size, kmpPaths.size))
    }

    /**
     * 通用字符串列表匹配评分
     */
    internal fun scoreStringMatch(
        jsList: List<String>,
        kmpList: List<String>,
        name: String,
        weight: Double
    ): DimensionScore {
        if (jsList.isEmpty() && kmpList.isEmpty()) {
            return DimensionScore(name, 1.0, weight, "no $name")
        }
        if (jsList.isEmpty() || kmpList.isEmpty()) {
            return DimensionScore(name, 0.0, weight, "one side missing $name")
        }
        val pairCount = min(jsList.size, kmpList.size)
        var matched = 0
        for (i in 0 until pairCount) {
            if (jsList[i] == kmpList[i]) matched++
        }
        val countPenalty = if (jsList.size != kmpList.size) {
            abs(jsList.size - kmpList.size).toDouble() / max(jsList.size, kmpList.size)
        } else 0.0
        val s = (matched.toDouble() / pairCount) * (1.0 - countPenalty)
        return DimensionScore(name, s, weight,
            "matched=%d/%d".format(matched, pairCount))
    }

    /**
     * 通用 marker 评分
     */
    internal fun scoreMarkers(jsSvg: String, kmpSvg: String, weight: Double): DimensionScore {
        val jsEnds = RE_MARKER_END.findAll(jsSvg).map { normalizeMarker(it.groupValues[1]) }.toList()
        val kmpEnds = RE_MARKER_END.findAll(kmpSvg).map { normalizeMarker(it.groupValues[1]) }.toList()
        val jsStarts = RE_MARKER_START.findAll(jsSvg).map { normalizeMarker(it.groupValues[1]) }.toList()
        val kmpStarts = RE_MARKER_START.findAll(kmpSvg).map { normalizeMarker(it.groupValues[1]) }.toList()

        val allJs = jsEnds + jsStarts
        val allKmp = kmpEnds + kmpStarts
        return scoreStringMatch(allJs, allKmp, "markers", weight)
    }

    /**
     * 通用结构完整性评分：基于元素数量的比率对
     */
    internal fun scoreStructureCounts(
        counts: List<Pair<Int, Int>>, // (jsCount, kmpCount) pairs
        labels: List<String>,
        weight: Double
    ): DimensionScore {
        if (counts.isEmpty()) {
            return DimensionScore("structure", 1.0, weight, "no structure to compare")
        }
        var totalRatio = 0.0
        val details = mutableListOf<String>()
        for ((idx, pair) in counts.withIndex()) {
            val (js, kmp) = pair
            val ratio = if (max(js, kmp) > 0) {
                min(js, kmp).toDouble() / max(js, kmp)
            } else 1.0
            totalRatio += ratio
            details += "${labels[idx]}=$js/$kmp"
        }
        val s = totalRatio / counts.size
        return DimensionScore("structure", s, weight, details.joinToString(", "))
    }

    /**
     * 通用 SVG 文本内容提取
     */
    internal fun extractTextContents(svg: String): List<String> {
        val re = Regex("""<text[^>]*>(.*?)</text>""", RegexOption.DOT_MATCHES_ALL)
        return re.findAll(svg).map { it.groupValues[1].replace(Regex("<[^>]+>"), "").trim() }
            .filter { it.isNotBlank() }
            .toList()
    }

    /**
     * 通用提取 translate 坐标的 <g> 元素
     */
    internal fun extractGroupTranslates(svg: String, classPattern: String): List<Pair<Double, Double>> {
        val re = Regex("""<g[^>]*class="[^"]*$classPattern[^"]*"[^>]*transform="translate\(([^)]+)\)"""")
        return re.findAll(svg).map { m ->
            val coords = m.groupValues[1].replace(",", " ").split(" ").filter { it.isNotBlank() }
            val x = coords.getOrNull(0)?.toDoubleOrNull() ?: 0.0
            val y = coords.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            x to y
        }.toList()
    }

    /**
     * 提取所有 path d 属性（按 CSS 类名过滤）
     */
    internal fun extractPathsByClass(svg: String, classPattern: String): List<String> {
        val re = Regex("""<path\s[^>]*?class="[^"]*$classPattern[^"]*"[^>]*/>""")
        return re.findAll(svg).mapNotNull { m ->
            RE_D_ATTR.find(m.value)?.groupValues?.get(1)
        }.toList()
    }

    /**
     * 提取所有 path d 属性（按 data 属性过滤）
     */
    internal fun extractPathsByDataAttr(svg: String, dataAttr: String): List<String> {
        val re = Regex("""<path\s[^>]*?$dataAttr[^>]*/>""")
        return re.findAll(svg).mapNotNull { m ->
            RE_D_ATTR.find(m.value)?.groupValues?.get(1)
        }.toList()
    }

    /**
     * 提取所有含指定类名的元素数量
     */
    internal fun countElementsByClass(svg: String, classPattern: String): Int {
        val re = Regex("""class="[^"]*$classPattern[^"]*"""")
        return re.findAll(svg).count()
    }

    /**
     * 提取所有 <rect> 元素的位置和大小
     */
    internal fun extractRects(svg: String, classPattern: String? = null): List<RectInfo> {
        val pattern = if (classPattern != null) {
            """<rect[^>]*class="[^"]*$classPattern[^"]*"[^>]*"""
        } else {
            """<rect[^>]*"""
        }
        val re = Regex(pattern)
        val reX = Regex("""\bx="([^"]+)"""")
        val reY = Regex("""\by="([^"]+)"""")
        val reW = Regex("""\bwidth="([^"]+)"""")
        val reH = Regex("""\bheight="([^"]+)"""")

        return re.findAll(svg).map { m ->
            val s = m.value
            RectInfo(
                x = reX.find(s)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
                y = reY.find(s)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
                width = reW.find(s)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
                height = reH.find(s)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            )
        }.toList()
    }

    /**
     * 评分矩形列表
     */
    internal fun scoreRects(
        jsRects: List<RectInfo>,
        kmpRects: List<RectInfo>,
        name: String,
        weight: Double,
        perfectPx: Double = 0.5,
        zeroPx: Double = 30.0
    ): DimensionScore {
        if (jsRects.isEmpty() && kmpRects.isEmpty()) {
            return DimensionScore(name, 1.0, weight, "no $name")
        }
        if (jsRects.isEmpty() || kmpRects.isEmpty()) {
            return DimensionScore(name, 0.0, weight, "one side has no $name")
        }
        val pairCount = min(jsRects.size, kmpRects.size)
        var sumScore = 0.0
        var maxDiff = 0.0
        for (i in 0 until pairCount) {
            val j = jsRects[i]
            val k = kmpRects[i]
            val diff = maxOf(abs(j.x - k.x), abs(j.y - k.y), abs(j.width - k.width), abs(j.height - k.height))
            maxDiff = max(maxDiff, diff)
            sumScore += linearScore(diff, perfectPx, zeroPx)
        }
        val countPenalty = if (jsRects.size != kmpRects.size) {
            val missing = abs(jsRects.size - kmpRects.size)
            missing.toDouble() / max(jsRects.size, kmpRects.size)
        } else 0.0
        val avg = sumScore / pairCount
        val s = avg * (1.0 - countPenalty)
        return DimensionScore(name, s, weight,
            "maxDiff=%.3fpx, paired=%d/%d/%d".format(maxDiff, pairCount, jsRects.size, kmpRects.size))
    }

    /**
     * 提取所有 <line> 元素
     */
    internal fun extractLines(svg: String, classPattern: String? = null): List<LineInfo> {
        val pattern = if (classPattern != null) {
            """<line[^>]*class="[^"]*$classPattern[^"]*"[^>]*"""
        } else {
            """<line[^>]*"""
        }
        val re = Regex(pattern)
        val reX1 = Regex("""\bx1="([^"]+)"""")
        val reY1 = Regex("""\by1="([^"]+)"""")
        val reX2 = Regex("""\bx2="([^"]+)"""")
        val reY2 = Regex("""\by2="([^"]+)"""")

        return re.findAll(svg).map { m ->
            val s = m.value
            LineInfo(
                x1 = reX1.find(s)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
                y1 = reY1.find(s)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
                x2 = reX2.find(s)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
                y2 = reY2.find(s)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            )
        }.toList()
    }

    /**
     * 评分线段列表
     */
    internal fun scoreLines(
        jsLines: List<LineInfo>,
        kmpLines: List<LineInfo>,
        name: String,
        weight: Double,
        perfectPx: Double = 0.5,
        zeroPx: Double = 50.0
    ): DimensionScore {
        if (jsLines.isEmpty() && kmpLines.isEmpty()) {
            return DimensionScore(name, 1.0, weight, "no $name")
        }
        if (jsLines.isEmpty() || kmpLines.isEmpty()) {
            return DimensionScore(name, 0.0, weight, "one side has no $name")
        }
        val pairCount = min(jsLines.size, kmpLines.size)
        var sumScore = 0.0
        var maxDiff = 0.0
        for (i in 0 until pairCount) {
            val j = jsLines[i]
            val k = kmpLines[i]
            val diff = maxOf(abs(j.x1 - k.x1), abs(j.y1 - k.y1), abs(j.x2 - k.x2), abs(j.y2 - k.y2))
            maxDiff = max(maxDiff, diff)
            sumScore += linearScore(diff, perfectPx, zeroPx)
        }
        val countPenalty = if (jsLines.size != kmpLines.size) {
            val missing = abs(jsLines.size - kmpLines.size)
            missing.toDouble() / max(jsLines.size, kmpLines.size)
        } else 0.0
        val avg = sumScore / pairCount
        val s = avg * (1.0 - countPenalty)
        return DimensionScore(name, s, weight,
            "maxDiff=%.3fpx, paired=%d/%d/%d".format(maxDiff, pairCount, jsLines.size, kmpLines.size))
    }

    /**
     * 提取所有 <circle> 元素
     */
    internal fun extractCircles(svg: String, classPattern: String? = null): List<CircleInfo> {
        val pattern = if (classPattern != null) {
            """<circle[^>]*class="[^"]*$classPattern[^"]*"[^>]*"""
        } else {
            """<circle[^>]*"""
        }
        val re = Regex(pattern)
        val reCx = Regex("""\bcx="([^"]+)"""")
        val reCy = Regex("""\bcy="([^"]+)"""")
        val reR = Regex("""\br="([^"]+)"""")

        return re.findAll(svg).map { m ->
            val s = m.value
            CircleInfo(
                cx = reCx.find(s)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
                cy = reCy.find(s)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
                r = reR.find(s)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            )
        }.toList()
    }
}

// ════════════════════════════════════════════════════════
//  几何数据类
// ════════════════════════════════════════════════════════

data class RectInfo(val x: Double, val y: Double, val width: Double, val height: Double)
data class LineInfo(val x1: Double, val y1: Double, val x2: Double, val y2: Double)
data class CircleInfo(val cx: Double, val cy: Double, val r: Double)

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
    val dimensions: List<DimensionScore>,
    val diagramType: String = "generic"
) {
    /** 是否通过（默认阈值 0.95） */
    fun passed(threshold: Double = 0.95): Boolean = total >= threshold
}
