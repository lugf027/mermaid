package io.lugf027.github.mermaid.core.diagram.gantt

import io.lugf027.github.mermaid.core.config.GanttDiagramConfig
import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.Logger
import io.lugf027.github.mermaid.core.util.TextUtils
import kotlin.math.max
import kotlin.math.min

/**
 * 甘特图渲染器 - 对标 mermaid-js ganttRenderer.js
 *
 * 自定义渲染模式（不使用 Dagre），直接计算坐标构建 SVG 元素树。
 *
 * SVG 元素层次:
 * 1. 排除日期背景区域 (exclude-range)
 * 2. 时间轴 + 网格线 (grid)
 * 3. Section 背景行
 * 4. 任务条和文本
 * 5. Section 标签
 * 6. 今日标记线
 * 7. 图表标题
 */
class GanttRenderer : DiagramRenderer {

    private val log = Logger("GanttRenderer")

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val ganttDb = db as? GanttDb ?: throw IllegalArgumentException("Expected GanttDb")
        val ganttConfig = config.gantt ?: GanttDiagramConfig()

        // 配置参数
        val titleTopMargin = ganttConfig.titleTopMargin ?: 25
        val barHeight = ganttConfig.barHeight ?: 20
        val barGap = ganttConfig.barGap ?: 4
        val topPadding = ganttConfig.topPadding ?: 50
        val rightPadding = ganttConfig.rightPadding ?: 75
        val leftPadding = ganttConfig.leftPadding ?: 75
        val gridLineStartPadding = ganttConfig.gridLineStartPadding ?: 35
        val fontSize = ganttConfig.fontSize ?: 11
        val sectionFontSize = ganttConfig.sectionFontSize ?: 11
        val numberSectionStyles = ganttConfig.numberSectionStyles ?: 4
        val gap = barHeight + barGap

        // 获取编译后的任务
        val tasks = ganttDb.getTasks()
        if (tasks.isEmpty()) {
            return buildSvg {
                attr("id", diagramId)
                attr("xmlns", "http://www.w3.org/2000/svg")
                viewBox(0.0, 0.0, 100.0, 100.0)
                text("No tasks defined", 50.0, 50.0) {
                    attr("text-anchor", "middle")
                    attr("font-size", "14px")
                }
            }
        }

        val categories = ganttDb.getCategories()

        // 计算时间范围
        val minTime = tasks.minOf { it.startTime }
        val maxTime = tasks.maxOf { it.endTime }
        val timeSpan = max(maxTime - minTime, GanttDb.DAY_MS) // 至少 1 天

        // 计算尺寸
        val w = 1200 // 固定宽度（对标 JS 默认）
        val chartWidth = w - leftPadding - rightPadding
        val h = topPadding * 2 + tasks.size * gap + titleTopMargin

        return buildSvg {
            attr("id", diagramId)
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("xmlns:xlink", "http://www.w3.org/1999/xlink")
            attr("role", "graphics-document document")
            attr("aria-roledescription", "gantt")

            viewBox(0.0, 0.0, w.toDouble(), h.toDouble())
            attr("width", "100%")
            attr("style", "max-width: ${w}px;")

            // 标题
            val titleText = ganttDb.getDiagramTitle()
            if (titleText.isNotEmpty()) {
                title(titleText)
            }

            // 样式
            defs {
                style(generateGanttStyles(themeVariables, numberSectionStyles))
            }

            // === 1. 排除日期背景 ===
            drawExcludeDays(this, ganttDb, minTime, maxTime, chartWidth, leftPadding,
                gridLineStartPadding, h, topPadding, timeSpan)

            // === 2. 时间轴 + 网格线 ===
            drawGrid(this, ganttDb, minTime, maxTime, timeSpan, chartWidth, leftPadding,
                topPadding, h, gridLineStartPadding, fontSize)

            // === 3. Section 背景行 ===
            drawSectionBackgrounds(this, tasks, categories, gap, topPadding,
                w, rightPadding, numberSectionStyles)

            // === 4. 任务条和文本 ===
            drawTasks(this, tasks, categories, minTime, timeSpan, chartWidth,
                leftPadding, topPadding, barHeight, gap, fontSize, numberSectionStyles)

            // === 5. Section 标签 ===
            drawSectionLabels(this, tasks, categories, gap, topPadding,
                sectionFontSize, numberSectionStyles)

            // === 6. 今日标记线 ===
            drawTodayMarker(this, ganttDb, minTime, timeSpan, chartWidth,
                leftPadding, titleTopMargin, h)

            // === 7. 图表标题 ===
            if (titleText.isNotEmpty()) {
                text(titleText, w / 2.0, titleTopMargin.toDouble()) {
                    addClass("titleText")
                    attr("text-anchor", "middle")
                    attr("font-size", "18px")
                    attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")
                    attr("fill", themeVariables.titleColor)
                }
            }
        }
    }

    // ════════════════════════════════════════════════════
    //  绘制方法
    // ════════════════════════════════════════════════════

    /**
     * 时间比例尺: 将时间戳映射到 x 坐标
     */
    private fun timeToX(time: Long, minTime: Long, timeSpan: Long, chartWidth: Int): Double {
        return (time - minTime).toDouble() / timeSpan * chartWidth
    }

    /**
     * 绘制排除日期背景区域
     */
    private fun drawExcludeDays(
        svg: SvgRoot,
        ganttDb: GanttDb,
        minTime: Long,
        maxTime: Long,
        chartWidth: Int,
        leftPadding: Int,
        gridLineStartPadding: Int,
        h: Int,
        topPadding: Int,
        timeSpan: Long
    ) {
        if (ganttDb.getExcludes().isEmpty()) return

        // 安全保护: 超过 5 年跳过
        if (maxTime - minTime > 5L * 365 * GanttDb.DAY_MS) return

        val excludeRanges = mutableListOf<Pair<Long, Long>>() // start, end
        var current = minTime
        var rangeStart: Long? = null

        while (current <= maxTime) {
            if (ganttDb.isInvalidDate(current)) {
                if (rangeStart == null) rangeStart = current
            } else {
                if (rangeStart != null) {
                    excludeRanges.add(rangeStart to current)
                    rangeStart = null
                }
            }
            current += GanttDb.DAY_MS
        }
        if (rangeStart != null) {
            excludeRanges.add(rangeStart to current)
        }

        val excGroup = svg.group {}
        for ((start, end) in excludeRanges) {
            val x1 = timeToX(start, minTime, timeSpan, chartWidth) + leftPadding
            val x2 = timeToX(end, minTime, timeSpan, chartWidth) + leftPadding
            val dateStr = GanttDb.formatDate(start)

            excGroup.rect(x1, gridLineStartPadding.toDouble(), x2 - x1,
                (h - topPadding - gridLineStartPadding).toDouble()) {
                attr("id", "exclude-$dateStr")
                addClass("exclude-range")
            }
        }
    }

    /**
     * 绘制时间轴网格线
     */
    private fun drawGrid(
        svg: SvgRoot,
        ganttDb: GanttDb,
        minTime: Long,
        maxTime: Long,
        timeSpan: Long,
        chartWidth: Int,
        leftPadding: Int,
        topPadding: Int,
        h: Int,
        gridLineStartPadding: Int,
        fontSize: Int
    ) {
        // 底部轴
        val gridGroup = svg.group {
            addClass("grid")
            attr("transform", "translate($leftPadding, ${h - 50})")
        }

        // 计算 tick 间隔
        val tickIntervalMs = resolveTickInterval(ganttDb.getTickInterval(), timeSpan)
        val tickSize = -(h - topPadding - gridLineStartPadding).toDouble()

        // 生成 tick 位置
        var tickTime = alignToTick(minTime, tickIntervalMs)
        while (tickTime <= maxTime + tickIntervalMs) {
            val x = timeToX(tickTime, minTime, timeSpan, chartWidth)

            gridGroup.group {
                addClass("tick")
                attr("transform", "translate(${"%.1f".format(x)}, 0)")

                // 网格线
                line(0.0, 0.0, 0.0, tickSize) {
                    attr("stroke", "#ccc")
                    attr("stroke-width", "0.5")
                }

                // 日期标签
                text(formatAxisLabel(tickTime, ganttDb.getAxisFormat()), 0.0, 15.0) {
                    attr("text-anchor", "middle")
                    attr("font-size", "${fontSize}px")
                    attr("fill", "#333")
                    attr("dy", ".71em")
                }
            }

            tickTime += tickIntervalMs
        }

        // 顶部轴（如果启用）
        if (ganttDb.isTopAxis()) {
            val topGridGroup = svg.group {
                addClass("grid")
                attr("transform", "translate($leftPadding, $topPadding)")
            }

            tickTime = alignToTick(minTime, tickIntervalMs)
            while (tickTime <= maxTime + tickIntervalMs) {
                val x = timeToX(tickTime, minTime, timeSpan, chartWidth)

                topGridGroup.group {
                    addClass("tick")
                    attr("transform", "translate(${"%.1f".format(x)}, 0)")

                    text(formatAxisLabel(tickTime, ganttDb.getAxisFormat()), 0.0, -5.0) {
                        attr("text-anchor", "middle")
                        attr("font-size", "${fontSize}px")
                        attr("fill", "#333")
                    }
                }

                tickTime += tickIntervalMs
            }
        }
    }

    /**
     * 绘制 section 背景行
     */
    private fun drawSectionBackgrounds(
        svg: SvgRoot,
        tasks: List<GanttTask>,
        categories: List<String>,
        gap: Int,
        topPadding: Int,
        w: Int,
        rightPadding: Int,
        numberSectionStyles: Int
    ) {
        val bgGroup = svg.group {}
        val orders = tasks.map { it.order }.distinct().sorted()

        for (order in orders) {
            val task = tasks.first { it.order == order }
            val sectionIdx = categories.indexOf(task.section).coerceAtLeast(0)

            bgGroup.rect(0.0, (order * gap + topPadding - 2).toDouble(),
                (w - rightPadding / 2).toDouble(), gap.toDouble()) {
                addClass("section section${sectionIdx % numberSectionStyles}")
            }
        }
    }

    /**
     * 绘制任务条和任务文本
     */
    private fun drawTasks(
        svg: SvgRoot,
        tasks: List<GanttTask>,
        categories: List<String>,
        minTime: Long,
        timeSpan: Long,
        chartWidth: Int,
        leftPadding: Int,
        topPadding: Int,
        barHeight: Int,
        gap: Int,
        fontSize: Int,
        numberSectionStyles: Int
    ) {
        val taskGroup = svg.group {}

        for (task in tasks) {
            val sectionIdx = categories.indexOf(task.section).coerceAtLeast(0)
            val startX = timeToX(task.startTime, minTime, timeSpan, chartWidth)
            val endX = timeToX(task.endTime, minTime, timeSpan, chartWidth)
            val taskWidth = max(endX - startX, 1.0) // 至少 1px
            val taskY = task.order * gap + topPadding
            val taskClass = task.buildTaskClass(sectionIdx, numberSectionStyles)

            if (task.milestone) {
                // 里程碑: 旋转45度的正方形
                val milestoneX = startX + leftPadding + taskWidth / 2 - barHeight.toDouble() / 2
                val milestoneY = taskY.toDouble()
                val transformOriginX = startX + leftPadding + taskWidth / 2
                val transformOriginY = taskY + barHeight.toDouble() / 2

                taskGroup.rect(milestoneX, milestoneY,
                    barHeight.toDouble(), barHeight.toDouble()) {
                    attr("id", task.id)
                    attr("rx", "3")
                    attr("ry", "3")
                    addClass(taskClass)
                    attr("transform", "rotate(45)")
                    attr("transform-origin", "${"%.1f".format(transformOriginX)} ${"%.1f".format(transformOriginY)}")
                    attr("style", "transform: rotate(45deg) scale(0.8, 0.8); transform-origin: ${"%.1f".format(transformOriginX)}px ${"%.1f".format(transformOriginY)}px;")
                }
            } else {
                // 普通任务条
                taskGroup.rect(startX + leftPadding, taskY.toDouble(),
                    taskWidth, barHeight.toDouble()) {
                    attr("id", task.id)
                    attr("rx", "3")
                    attr("ry", "3")
                    addClass(taskClass)
                }
            }

            // 任务文本
            val textWidth = TextUtils.estimateTextWidth(task.description, fontSize.toDouble())
            val textX: Double
            val textClass: String

            if (textWidth <= taskWidth) {
                // 文本在条内居中
                textX = startX + leftPadding + taskWidth / 2
                textClass = task.buildTextClass(sectionIdx, numberSectionStyles, true)
            } else {
                // 文本溢出到右侧
                textX = endX + leftPadding + 5
                textClass = "taskTextOutsideRight taskTextOutside${sectionIdx % numberSectionStyles}"
            }

            val textY = taskY + barHeight / 2.0 + (fontSize / 2.0 - 2)

            taskGroup.text(task.description, textX, textY) {
                attr("id", "${task.id}-text")
                addClass(textClass)
                attr("text-anchor", if (textWidth <= taskWidth) "middle" else "start")
                attr("font-size", "${fontSize}px")
                attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")
            }
        }
    }

    /**
     * 绘制 section 标签
     */
    private fun drawSectionLabels(
        svg: SvgRoot,
        tasks: List<GanttTask>,
        categories: List<String>,
        gap: Int,
        topPadding: Int,
        sectionFontSize: Int,
        numberSectionStyles: Int
    ) {
        val labelGroup = svg.group {}

        for ((sectionIdx, section) in categories.withIndex()) {
            val sectionTasks = tasks.filter { it.section == section }
            if (sectionTasks.isEmpty()) continue

            val minOrder = sectionTasks.minOf { it.order }
            val maxOrder = sectionTasks.maxOf { it.order }
            val sectionHeight = (maxOrder - minOrder + 1) * gap
            val centerY = minOrder * gap + topPadding + sectionHeight / 2.0

            // 支持多行标签 (通过 <br> 分割)
            val labelLines = section.split("<br>", "<br/>", "<br />")

            labelGroup.text("", 10.0, centerY) {
                addClass("sectionTitle sectionTitle${sectionIdx % numberSectionStyles}")
                attr("font-size", "${sectionFontSize}px")
                attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")

                for ((lineIdx, labelLine) in labelLines.withIndex()) {
                    tspan(labelLine.trim()) {
                        attr("alignment-baseline", "central")
                        attr("x", "10")
                        if (lineIdx > 0) {
                            attr("dy", "1em")
                        }
                    }
                }
            }
        }
    }

    /**
     * 绘制今日标记线
     */
    private fun drawTodayMarker(
        svg: SvgRoot,
        ganttDb: GanttDb,
        minTime: Long,
        timeSpan: Long,
        chartWidth: Int,
        leftPadding: Int,
        titleTopMargin: Int,
        h: Int
    ) {
        val todayMarker = ganttDb.getTodayMarker()
        if (todayMarker.lowercase() == "off") return

        val today = GanttDb.todayMillis()
        if (today < minTime || today > minTime + timeSpan) return

        val todayX = timeToX(today, minTime, timeSpan, chartWidth) + leftPadding

        svg.group {
            addClass("today")
            line(todayX, titleTopMargin.toDouble(), todayX, (h - titleTopMargin).toDouble()) {
                addClass("today")
                if (todayMarker.isNotEmpty()) {
                    val style = todayMarker.replace(",", ";")
                    attr("style", style)
                }
            }
        }
    }

    // ════════════════════════════════════════════════════
    //  辅助方法
    // ════════════════════════════════════════════════════

    /**
     * 解析 tick 间隔为毫秒
     */
    private fun resolveTickInterval(tickInterval: String?, timeSpan: Long): Long {
        if (tickInterval != null) {
            val match = RE_TICK_INTERVAL.find(tickInterval)
            if (match != null) {
                val amount = match.groupValues[1].toLong()
                val unit = match.groupValues[2].lowercase()
                val interval = when (unit) {
                    "millisecond" -> amount
                    "second" -> amount * GanttDb.SECOND_MS
                    "minute" -> amount * GanttDb.MINUTE_MS
                    "hour" -> amount * GanttDb.HOUR_MS
                    "day" -> amount * GanttDb.DAY_MS
                    "week" -> amount * GanttDb.WEEK_MS
                    "month" -> amount * 30 * GanttDb.DAY_MS
                    else -> null
                }
                if (interval != null && interval > 0) {
                    // 安全保护: 估算 tick 数不超过 10000
                    if (timeSpan / interval < 10000) return interval
                }
            }
        }

        // 自动计算合理的 tick 间隔
        return when {
            timeSpan <= 7 * GanttDb.DAY_MS -> GanttDb.DAY_MS
            timeSpan <= 30 * GanttDb.DAY_MS -> 7 * GanttDb.DAY_MS
            timeSpan <= 180 * GanttDb.DAY_MS -> 30 * GanttDb.DAY_MS
            timeSpan <= 365 * GanttDb.DAY_MS -> 60 * GanttDb.DAY_MS
            else -> 90 * GanttDb.DAY_MS
        }
    }

    /**
     * 对齐时间到 tick 边界
     */
    private fun alignToTick(time: Long, tickInterval: Long): Long {
        return if (tickInterval >= GanttDb.DAY_MS) {
            (time / GanttDb.DAY_MS) * GanttDb.DAY_MS
        } else {
            (time / tickInterval) * tickInterval
        }
    }

    /**
     * 格式化轴标签
     * 支持部分 strftime 格式: %Y, %m, %d, %H, %M, %B, %b, %a, %A
     */
    private fun formatAxisLabel(millis: Long, format: String): String {
        val (year, month, day) = GanttDb.epochMillisToDate(millis)

        val monthNames = listOf("", "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December")
        val monthAbbrs = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val dayAbbrs = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        val dow = GanttDb.dayOfWeek(millis) - 1 // 0-indexed

        return format
            .replace("%Y", year.toString())
            .replace("%m", month.toString().padStart(2, '0'))
            .replace("%d", day.toString().padStart(2, '0'))
            .replace("%H", "00")
            .replace("%M", "00")
            .replace("%B", if (month in 1..12) monthNames[month] else "")
            .replace("%b", if (month in 1..12) monthAbbrs[month] else "")
            .replace("%A", if (dow in 0..6) dayNames[dow] else "")
            .replace("%a", if (dow in 0..6) dayAbbrs[dow] else "")
    }

    // ════════════════════════════════════════════════════
    //  样式生成
    // ════════════════════════════════════════════════════

    private fun generateGanttStyles(tv: ThemeVariables, numStyles: Int): String {
        val sb = StringBuilder()

        // 排除区域
        sb.appendLine("""
.exclude-range {
  fill: ${tv.excludeBkgColor};
  opacity: 0.3;
}""".trimIndent())

        // Section 样式
        for (i in 0 until numStyles) {
            val bgColor = if (i % 2 == 0) tv.sectionBkgColor else tv.altSectionBkgColor
            sb.appendLine("""
.section${i} {
  fill: $bgColor;
  opacity: 0.2;
}""".trimIndent())
            sb.appendLine("""
.sectionTitle${i} {
  fill: ${tv.titleColor};
}""".trimIndent())
        }

        // 网格
        sb.appendLine("""
.grid .tick line {
  stroke: ${tv.gridColor};
  opacity: 0.3;
  shape-rendering: crispEdges;
}
.grid .tick text {
  fill: ${tv.textColor};
}
.grid path {
  stroke-width: 0;
}""".trimIndent())

        // 今日标记
        sb.appendLine("""
.today line {
  stroke: ${tv.todayLineColor};
  stroke-width: 2;
}""".trimIndent())

        // 任务样式（按 section 编号）
        for (i in 0 until numStyles) {
            sb.appendLine("""
.task${i} {
  fill: ${tv.taskBkgColor};
  stroke: ${tv.taskBorderColor};
}
.taskText${i} {
  fill: ${tv.taskTextColor};
}
.taskTextOutside${i} {
  fill: ${tv.taskTextOutsideColor};
}
.active${i} {
  fill: ${tv.activeTaskBkgColor};
  stroke: ${tv.activeTaskBorderColor};
}
.activeText${i} {
  fill: ${tv.taskTextDarkColor};
}
.done${i} {
  fill: ${tv.doneTaskBkgColor};
  stroke: ${tv.doneTaskBorderColor};
  stroke-width: 2;
}
.doneText${i} {
  fill: ${tv.taskTextDarkColor};
}
.crit${i} {
  fill: ${tv.critBkgColor};
  stroke: ${tv.critBorderColor};
  stroke-width: 2;
  stroke-dasharray: 3;
}
.activeCrit${i} {
  fill: ${tv.activeTaskBkgColor};
  stroke: ${tv.critBorderColor};
  stroke-width: 2;
  stroke-dasharray: 3;
}
.doneCrit${i} {
  fill: ${tv.doneTaskBkgColor};
  stroke: ${tv.critBorderColor};
  stroke-width: 2;
  stroke-dasharray: 3;
}
.activeCritText${i} {
  fill: ${tv.taskTextDarkColor};
}
.doneCritText${i} {
  fill: ${tv.taskTextDarkColor};
}""".trimIndent())
        }

        // 通用样式
        sb.appendLine("""
.task {
  stroke-width: 2;
}
.milestone {
  transform: rotate(45deg) scale(0.8, 0.8);
}
.milestoneText {
  font-style: italic;
}
.titleText {
  text-anchor: middle;
  font-size: 18px;
  fill: ${tv.titleColor};
  font-family: 'trebuchet ms', verdana, arial, sans-serif;
}
.clickable {
  cursor: pointer;
}""".trimIndent())

        return sb.toString()
    }

    companion object {
        private val RE_TICK_INTERVAL = Regex("^([1-9]\\d*)(millisecond|second|minute|hour|day|week|month)$")
    }
}
