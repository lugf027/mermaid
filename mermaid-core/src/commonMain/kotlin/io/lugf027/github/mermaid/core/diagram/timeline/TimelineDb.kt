package io.lugf027.github.mermaid.core.diagram.timeline

import io.lugf027.github.mermaid.core.diagram.DiagramDB

/**
 * 时间线任务/时间段数据类
 */
data class TimelineTask(
    val id: Int,
    val section: String,
    val type: String,
    val task: String,
    val score: Int = 0,
    val events: MutableList<String> = mutableListOf()
)

/**
 * Timeline 数据库 - 对标 mermaid-js timelineDb.js
 *
 * 存储时间线图的 sections、time periods 和 events。
 */
class TimelineDb : DiagramDB {
    private var diagramTitle: String = ""
    private var accTitle: String = ""
    private var accDescr: String = ""

    private var currentSection: String = ""
    private var currentTaskId: Int = 0
    private val sections: MutableList<String> = mutableListOf()
    private val tasks: MutableList<TimelineTask> = mutableListOf()

    override fun clear() {
        diagramTitle = ""
        accTitle = ""
        accDescr = ""
        currentSection = ""
        currentTaskId = 0
        sections.clear()
        tasks.clear()
    }

    override fun setDiagramTitle(title: String) { diagramTitle = title }
    override fun getDiagramTitle(): String = diagramTitle
    override fun setAccTitle(title: String) { accTitle = title }
    override fun getAccTitle(): String = accTitle
    override fun setAccDescription(desc: String) { accDescr = desc }
    override fun getAccDescription(): String = accDescr

    /**
     * 添加 section
     */
    fun addSection(txt: String) {
        currentSection = txt.trim()
        sections.add(currentSection)
    }

    /**
     * 获取所有 sections
     */
    fun getSections(): List<String> = sections.toList()

    /**
     * 添加时间段（period）
     *
     * @param period 时间段文本
     * @param length 长度/分值
     * @param event 可选的第一个事件
     */
    fun addTask(period: String, length: Int = 0, event: String = "") {
        val task = TimelineTask(
            id = currentTaskId++,
            section = currentSection,
            type = currentSection,
            task = period.trim(),
            score = length
        )
        if (event.isNotBlank()) {
            task.events.add(event.trim())
        }
        tasks.add(task)
    }

    /**
     * 添加事件到最近的时间段
     */
    fun addEvent(event: String) {
        if (tasks.isEmpty()) return
        tasks.last().events.add(event.trim())
    }

    /**
     * 获取所有任务/时间段
     */
    fun getTasks(): List<TimelineTask> = tasks.toList()

    /**
     * 获取指定 section 下的任务
     */
    fun getTasksForSection(section: String): List<TimelineTask> =
        tasks.filter { it.section == section }
}
