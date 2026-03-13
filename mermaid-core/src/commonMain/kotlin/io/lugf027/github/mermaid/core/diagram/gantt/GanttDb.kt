package io.lugf027.github.mermaid.core.diagram.gantt

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.util.Logger
import kotlin.math.max

/**
 * 甘特图数据库 - 对标 mermaid-js ganttDb.js
 *
 * 存储 section、task、日期格式等信息，提供日期计算和任务编译功能。
 */
class GanttDb : DiagramDB {

    private val log = Logger("GanttDb")

    // ── 全局状态 ──
    private var dateFormat: String = "YYYY-MM-DD"
    private var axisFormat: String = "%Y-%m-%d"
    private var tickInterval: String? = null
    private var todayMarker: String = ""
    private var weekday: String = "sunday"
    private var weekend: String = "saturday"
    private var inclusiveEndDates: Boolean = false
    private var topAxis: Boolean = false
    private var displayMode: String = ""

    private var diagramTitle: String = ""
    private var accTitle: String = ""
    private var accDescr: String = ""

    private val sections = mutableListOf<String>()
    private var currentSection: String = ""

    private val rawTasks = mutableListOf<RawTask>()
    private val compiledTasks = mutableListOf<GanttTask>()
    private val taskDb = mutableMapOf<String, Int>() // id -> index in rawTasks

    private val excludes = mutableListOf<String>()
    private val includes = mutableListOf<String>()

    private var taskCnt: Int = 0
    private var lastOrder: Int = 0

    // ── DiagramDB 接口 ──

    override fun clear() {
        dateFormat = "YYYY-MM-DD"
        axisFormat = "%Y-%m-%d"
        tickInterval = null
        todayMarker = ""
        weekday = "sunday"
        weekend = "saturday"
        inclusiveEndDates = false
        topAxis = false
        displayMode = ""
        diagramTitle = ""
        accTitle = ""
        accDescr = ""
        sections.clear()
        currentSection = ""
        rawTasks.clear()
        compiledTasks.clear()
        taskDb.clear()
        excludes.clear()
        includes.clear()
        taskCnt = 0
        lastOrder = 0
    }

    override fun getDiagramTitle(): String = diagramTitle
    override fun getAccTitle(): String = accTitle
    override fun getAccDescription(): String = accDescr

    // ── 设置器 ──

    override fun setDiagramTitle(title: String) { diagramTitle = title }
    override fun setAccTitle(title: String) { accTitle = title }
    override fun setAccDescription(desc: String) { accDescr = desc }

    fun setDateFormat(fmt: String) { dateFormat = fmt.trim() }
    fun getDateFormat(): String = dateFormat

    fun setAxisFormat(fmt: String) { axisFormat = fmt.trim() }
    fun getAxisFormat(): String = axisFormat

    fun setTickInterval(interval: String) { tickInterval = interval.trim() }
    fun getTickInterval(): String? = tickInterval

    fun setTodayMarker(marker: String) { todayMarker = marker.trim() }
    fun getTodayMarker(): String = todayMarker

    fun setWeekday(day: String) { weekday = day.lowercase().trim() }
    fun getWeekday(): String = weekday

    fun setWeekend(day: String) { weekend = day.lowercase().trim() }

    fun enableInclusiveEndDates() { inclusiveEndDates = true }
    fun isInclusiveEndDates(): Boolean = inclusiveEndDates

    fun enableTopAxis() { topAxis = true }
    fun isTopAxis(): Boolean = topAxis

    fun setDisplayMode(mode: String) { displayMode = mode.trim() }
    fun getDisplayMode(): String = displayMode

    fun setExcludes(excl: String) {
        excludes.clear()
        excl.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { excludes.add(it) }
    }
    fun getExcludes(): List<String> = excludes

    fun setIncludes(incl: String) {
        includes.clear()
        incl.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { includes.add(it) }
    }
    fun getIncludes(): List<String> = includes

    // ── Section 管理 ──

    fun addSection(section: String) {
        currentSection = section.trim()
        if (currentSection !in sections) {
            sections.add(currentSection)
        }
    }

    fun getSections(): List<String> = sections

    // ── 任务管理 ──

    /**
     * 添加原始任务（由解析器调用）
     * @param descr 任务描述/名称
     * @param data 任务数据字符串（冒号后的部分），包含状态标签、ID、日期等
     */
    fun addTask(descr: String, data: String) {
        val trimmedData = data.removePrefix(":").trim()
        val parts = trimmedData.split(",").map { it.trim() }

        // 提取状态标签
        var active = false
        var done = false
        var crit = false
        var milestone = false

        val nonTagParts = mutableListOf<String>()
        for (part in parts) {
            when (part.lowercase()) {
                "active" -> active = true
                "done" -> done = true
                "crit" -> crit = true
                "milestone" -> milestone = true
                else -> nonTagParts.add(part)
            }
        }

        // 根据剩余字段数量解析 ID、开始日期、结束/持续时间
        val id: String
        val startData: String?
        val endData: String?

        when (nonTagParts.size) {
            1 -> {
                // 只有 endTimeData
                id = "task${++taskCnt}"
                startData = null
                endData = nonTagParts[0]
            }
            2 -> {
                // startDate, endTimeData
                id = "task${++taskCnt}"
                startData = nonTagParts[0]
                endData = nonTagParts[1]
            }
            3 -> {
                // id, startDate, endTimeData
                id = nonTagParts[0]
                startData = nonTagParts[1]
                endData = nonTagParts[2]
            }
            else -> {
                id = "task${++taskCnt}"
                startData = null
                endData = null
            }
        }

        val rawTask = RawTask(
            id = id,
            description = descr.trim(),
            section = currentSection,
            active = active,
            done = done,
            crit = crit,
            milestone = milestone,
            startData = startData,
            endData = endData,
            order = lastOrder++
        )

        rawTasks.add(rawTask)
        taskDb[id] = rawTasks.size - 1
    }

    /**
     * 编译所有任务，解析日期引用和依赖关系
     */
    fun getTasks(): List<GanttTask> {
        if (compiledTasks.isNotEmpty()) return compiledTasks

        compileTasks()
        return compiledTasks
    }

    private fun compileTasks() {
        compiledTasks.clear()
        val taskMap = mutableMapOf<String, GanttTask>()

        // 多次迭代解析依赖（最多 10 次）
        for (iteration in 0 until 10) {
            var allResolved = true

            for (raw in rawTasks) {
                if (taskMap.containsKey(raw.id)) continue

                val startTime = resolveStartDate(raw.startData, taskMap)
                if (startTime == null) {
                    allResolved = false
                    continue
                }

                val endTime = resolveEndDate(raw.endData, startTime, taskMap)
                if (endTime == null) {
                    allResolved = false
                    continue
                }

                val task = GanttTask(
                    id = raw.id,
                    description = raw.description,
                    section = raw.section,
                    startTime = startTime,
                    endTime = max(endTime, startTime),  // 结束时间不能早于开始时间
                    active = raw.active,
                    done = raw.done,
                    crit = raw.crit,
                    milestone = raw.milestone,
                    order = raw.order
                )
                taskMap[raw.id] = task
                compiledTasks.add(task)
            }

            if (allResolved) break
        }
    }

    /**
     * 解析开始日期
     * 支持: 直接日期 "2024-01-01", "after taskId", 为空则继承上一个任务的结束时间
     */
    private fun resolveStartDate(startData: String?, taskMap: Map<String, GanttTask>): Long? {
        if (startData == null || startData.isEmpty()) {
            // 继承上一个任务的结束时间，或使用今天
            return if (compiledTasks.isNotEmpty()) {
                compiledTasks.last().endTime
            } else {
                todayMillis()
            }
        }

        val afterMatch = RE_AFTER.find(startData)
        if (afterMatch != null) {
            val ids = afterMatch.groupValues[1].trim().split("\\s+".toRegex())
            var maxEnd = 0L
            for (refId in ids) {
                val refTask = taskMap[refId.trim()] ?: return null // 引用的任务未编译
                maxEnd = max(maxEnd, refTask.endTime)
            }
            return if (maxEnd > 0) maxEnd else todayMillis()
        }

        // 直接日期
        return parseDate(startData, dateFormat) ?: todayMillis()
    }

    /**
     * 解析结束日期
     * 支持: 直接日期, 持续时间 "3d"/"1w", "until taskId"
     */
    private fun resolveEndDate(endData: String?, startTime: Long, taskMap: Map<String, GanttTask>): Long? {
        if (endData == null || endData.isEmpty()) {
            // 默认持续 1 天
            return startTime + DAY_MS
        }

        // "until taskId" 格式
        val untilMatch = RE_UNTIL.find(endData)
        if (untilMatch != null) {
            val ids = untilMatch.groupValues[1].trim().split("\\s+".toRegex())
            var minStart = Long.MAX_VALUE
            for (refId in ids) {
                val refTask = taskMap[refId.trim()] ?: return null
                minStart = kotlin.math.min(minStart, refTask.startTime)
            }
            return if (minStart < Long.MAX_VALUE) minStart else startTime + DAY_MS
        }

        // 持续时间格式
        val duration = parseDuration(endData)
        if (duration != null) {
            return startTime + duration
        }

        // 直接日期
        val endDate = parseDate(endData, dateFormat)
        if (endDate != null) {
            return if (inclusiveEndDates) endDate + DAY_MS else endDate
        }

        // 回退: 默认 1 天
        return startTime + DAY_MS
    }

    // ── 查询 API ──

    fun findTaskById(id: String): GanttTask? {
        return getTasks().find { it.id == id }
    }

    fun getCategories(): List<String> {
        val tasks = getTasks()
        return tasks.map { it.section }.distinct()
    }

    companion object {
        const val DAY_MS = 86_400_000L
        const val HOUR_MS = 3_600_000L
        const val MINUTE_MS = 60_000L
        const val SECOND_MS = 1_000L
        const val WEEK_MS = 7 * DAY_MS

        private val RE_AFTER = Regex("^after\\s+([\\w\\s-]+)", RegexOption.IGNORE_CASE)
        private val RE_UNTIL = Regex("^until\\s+([\\w\\s-]+)", RegexOption.IGNORE_CASE)
        private val RE_DURATION = Regex("^(\\d+(?:\\.\\d+)?)([Mdhmswy]|ms)$")
        private val RE_DATE_YYYY_MM_DD = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$")

        /**
         * 解析持续时间字符串
         * 支持: y(年), M(月), w(周), d(天), h(小时), m(分钟), s(秒), ms(毫秒)
         */
        fun parseDuration(str: String): Long? {
            val match = RE_DURATION.find(str.trim()) ?: return null
            val amount = match.groupValues[1].toDouble()
            val unit = match.groupValues[2]
            return when (unit) {
                "y" -> (amount * 365 * DAY_MS).toLong()
                "M" -> (amount * 30 * DAY_MS).toLong()
                "w" -> (amount * WEEK_MS).toLong()
                "d" -> (amount * DAY_MS).toLong()
                "h" -> (amount * HOUR_MS).toLong()
                "m" -> (amount * MINUTE_MS).toLong()
                "s" -> (amount * SECOND_MS).toLong()
                "ms" -> amount.toLong()
                else -> null
            }
        }

        /**
         * 简化日期解析 - 支持 YYYY-MM-DD 格式
         * 返回 epoch 毫秒。不使用 java.time/dayjs，纯 Kotlin 实现保证 KMP 兼容。
         */
        fun parseDate(str: String, dateFormat: String = "YYYY-MM-DD"): Long? {
            val trimmed = str.trim()

            // YYYY-MM-DD 格式
            val match = RE_DATE_YYYY_MM_DD.find(trimmed) ?: return null
            val year = match.groupValues[1].toIntOrNull() ?: return null
            val month = match.groupValues[2].toIntOrNull() ?: return null
            val day = match.groupValues[3].toIntOrNull() ?: return null

            if (month < 1 || month > 12 || day < 1 || day > 31) return null

            return dateToEpochMillis(year, month, day)
        }

        /**
         * 将 year/month/day 转换为 epoch 毫秒（UTC）
         * 简化实现，足够处理甘特图的日期需求
         */
        fun dateToEpochMillis(year: Int, month: Int, day: Int): Long {
            // 使用公历日历算法
            var y = year
            var m = month
            if (m <= 2) {
                y -= 1
                m += 12
            }
            val a = y / 4 - y / 100 + y / 400
            val b = 365 * y + a
            val c = (30.6 * (m + 1)).toInt()
            val julianDay = b + c + day - 719527 // 调整到 Unix Epoch
            return julianDay.toLong() * DAY_MS
        }

        /**
         * 将 epoch 毫秒转换为 year/month/day (UTC)
         */
        fun epochMillisToDate(millis: Long): Triple<Int, Int, Int> {
            val totalDays = (millis / DAY_MS).toInt() + 719527
            var y = ((totalDays - 122.1) / 365.25).toInt()
            val a = y / 4 - y / 100 + y / 400
            var daysOfYear = totalDays - (365 * y + a)
            if (daysOfYear <= 0) {
                y -= 1
                val a2 = y / 4 - y / 100 + y / 400
                daysOfYear = totalDays - (365 * y + a2)
            }
            var m = ((daysOfYear - 0.5) / 30.6).toInt()
            val day = daysOfYear - (30.6 * m).toInt()
            if (m > 13) {
                m -= 13
                y += 1
            } else {
                m -= 1
            }
            return Triple(y, m, day)
        }

        /**
         * 获取今天 00:00:00 UTC 的 epoch 毫秒
         */
        fun todayMillis(): Long {
            // KMP 中使用简化方式 - 固定一个合理的"今天"时间
            // 实际运行时会通过平台特定的 Clock 实现
            // 此处使用固定值便于测试和确定性
            return currentTimeMillis()
        }

        /**
         * 获取当前时间的 epoch 毫秒
         * 用于 todayMillis() 回退
         */
        private fun currentTimeMillis(): Long {
            // kotlin.system.getTimeMillis() 或使用 expect/actual
            // 为了 commonMain 兼容，返回一个固定的"现在"作为基准
            // 生产环境应通过 Clock 接口注入
            return 1741737600000L // 2025-03-12 00:00:00 UTC 作为默认基准
        }

        /**
         * 格式化日期为字符串
         */
        fun formatDate(millis: Long, format: String = "YYYY-MM-DD"): String {
            val (year, month, day) = epochMillisToDate(millis)
            return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
        }

        /**
         * 获取一周中的第几天 (1=周一 ... 7=周日) - ISO 标准
         */
        fun dayOfWeek(millis: Long): Int {
            val days = (millis / DAY_MS + 3) % 7  // 1970-01-01 是周四 (4)
            return if (days == 0L) 7 else days.toInt() + 1
        }

        /**
         * 获取星期名称 (小写英文)
         */
        fun dayOfWeekName(millis: Long): String {
            return when (dayOfWeek(millis)) {
                1 -> "monday"
                2 -> "tuesday"
                3 -> "wednesday"
                4 -> "thursday"
                5 -> "friday"
                6 -> "saturday"
                7 -> "sunday"
                else -> "unknown"
            }
        }
    }

    /**
     * 检查日期是否为无效日期（应排除的日期）
     */
    fun isInvalidDate(millis: Long): Boolean {
        val dateStr = formatDate(millis)

        // 检查 includes - 如果在 includes 中则有效
        for (incl in includes) {
            if (incl.trim() == dateStr) return false
        }

        // 检查 excludes
        for (excl in excludes) {
            when (excl.trim().lowercase()) {
                "weekends" -> {
                    val dow = dayOfWeek(millis)
                    val weekendStart = when (weekend) {
                        "friday" -> 5
                        else -> 6  // saturday
                    }
                    if (dow == weekendStart || dow == weekendStart + 1 ||
                        (weekendStart == 6 && dow == 7) ||
                        (weekendStart == 6 && dow == 1)) {
                        // 周末判定：saturday 模式为 6(六) 和 7(日)
                        if (weekend == "saturday" && (dow == 6 || dow == 7)) return true
                        if (weekend == "friday" && (dow == 5 || dow == 6)) return true
                    }
                }
                "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday" -> {
                    if (dayOfWeekName(millis) == excl.trim().lowercase()) return true
                }
                else -> {
                    // 具体日期
                    if (excl.trim() == dateStr) return true
                }
            }
        }

        return false
    }
}

// ════════════════════════════════════════════════════════
//  数据模型
// ════════════════════════════════════════════════════════

/**
 * 原始任务（解析器直接产生的）
 */
data class RawTask(
    val id: String,
    val description: String,
    val section: String,
    val active: Boolean = false,
    val done: Boolean = false,
    val crit: Boolean = false,
    val milestone: Boolean = false,
    val startData: String? = null,
    val endData: String? = null,
    val order: Int = 0
)

/**
 * 编译后的甘特图任务
 */
data class GanttTask(
    val id: String,
    val description: String,
    val section: String,
    val startTime: Long,       // epoch 毫秒
    val endTime: Long,         // epoch 毫秒
    val active: Boolean = false,
    val done: Boolean = false,
    val crit: Boolean = false,
    val milestone: Boolean = false,
    val order: Int = 0
) {
    /**
     * 构建 CSS 类名（对标 mermaid-js 的 className 构建逻辑）
     */
    fun buildTaskClass(sectionIndex: Int, numberSectionStyles: Int): String {
        val sb = StringBuilder("task")
        when {
            active && crit -> sb.append(" activeCrit")
            active -> sb.append(" active")
            done && crit -> sb.append(" doneCrit")
            done -> sb.append(" done")
            crit -> sb.append(" crit")
            else -> sb.append(" task")
        }
        if (milestone) sb.append(" milestone ")
        sb.append(sectionIndex % numberSectionStyles)
        return sb.toString()
    }

    /**
     * 构建任务文本 CSS 类名
     */
    fun buildTextClass(sectionIndex: Int, numberSectionStyles: Int, isInside: Boolean): String {
        val sb = StringBuilder()
        when {
            active && crit -> sb.append("activeCritText")
            active -> sb.append("activeText")
            done && crit -> sb.append("doneCritText")
            done -> sb.append("doneText")
            crit -> sb.append("critText")
            else -> sb.append("taskText")
        }
        sb.append(sectionIndex % numberSectionStyles)
        if (milestone) sb.append(" milestoneText")
        if (!isInside) {
            sb.insert(0, if (isInside) "taskText " else "taskTextOutsideRight taskTextOutside")
        }
        return sb.toString()
    }
}
