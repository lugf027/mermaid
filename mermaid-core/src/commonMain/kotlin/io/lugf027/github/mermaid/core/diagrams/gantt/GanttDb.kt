package io.lugf027.github.mermaid.core.diagrams.gantt

import io.lugf027.github.mermaid.core.db.CommonDb

/**
 * 甘特图数据存储层。
 */
class GanttDb : CommonDb() {

    private val sections = mutableListOf<GanttSection>()
    private val tasks = mutableListOf<GanttTask>()
    private var currentSection = ""
    var dateFormat: String = "YYYY-MM-DD"
        private set
    var axisFormat: String = "%Y-%m-%d"
        private set
    var todayMarker: String = "today"
        private set
    var topAxis: Boolean = false
        private set
    private val excludes = mutableListOf<String>()
    private var taskCounter = 0

    fun setDateFormat(fmt: String) { dateFormat = fmt }
    fun setAxisFormat(fmt: String) { axisFormat = fmt }
    fun setTodayMarker(m: String) { todayMarker = m }
    fun setTopAxis(b: Boolean) { topAxis = b }
    fun addExclude(e: String) { excludes.add(e) }

    fun addSection(name: String) {
        currentSection = name
        sections.add(GanttSection(name))
    }

    fun addTask(line: String) {
        // 解析格式: Task Name :tag1, tag2, id, start, duration
        val parts = line.split(":")
        val taskName = parts[0].trim()
        val metadata = if (parts.size > 1) parts[1].trim() else ""

        var id = "task${taskCounter++}"
        var isActive = false
        var isDone = false
        var isCrit = false
        var isMilestone = false
        var startTime = ""
        var duration = ""
        var afterId: String? = null

        val tokens = metadata.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val dataTokens = mutableListOf<String>()

        for (token in tokens) {
            when (token.lowercase()) {
                "active" -> isActive = true
                "done" -> isDone = true
                "crit" -> isCrit = true
                "milestone" -> isMilestone = true
                else -> dataTokens.add(token)
            }
        }

        // 解析 id, start, duration
        for ((idx, token) in dataTokens.withIndex()) {
            when {
                token.startsWith("after ") -> afterId = token.substringAfter("after ").trim()
                idx == 0 && !token.contains("-") && !token.endsWith("d") && !token.endsWith("w") -> id = token
                token.contains("-") || token.matches(Regex("\\d{4}.*")) -> startTime = token
                else -> duration = token
            }
        }

        val task = GanttTask(
            id = id,
            section = currentSection,
            task = taskName,
            startTime = startTime,
            duration = duration,
            isActive = isActive,
            isDone = isDone,
            isCrit = isCrit,
            isMilestone = isMilestone,
            afterId = afterId,
            order = tasks.size,
        )
        tasks.add(task)
        sections.lastOrNull()?.tasks?.add(task)
    }

    fun getSections(): List<GanttSection> = sections
    fun getTasks(): List<GanttTask> = tasks
    fun getExcludes(): List<String> = excludes

    override fun clear() {
        super.clear()
        sections.clear()
        tasks.clear()
        currentSection = ""
        taskCounter = 0
        excludes.clear()
        dateFormat = "YYYY-MM-DD"
        axisFormat = "%Y-%m-%d"
    }

}
