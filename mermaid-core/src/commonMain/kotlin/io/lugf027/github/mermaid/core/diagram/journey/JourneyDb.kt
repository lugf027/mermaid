package io.lugf027.github.mermaid.core.diagram.journey

import io.lugf027.github.mermaid.core.diagram.DiagramDB

/**
 * Journey 图表任务数据类
 */
data class JourneyTask(
    val section: String,
    val type: String,
    val task: String,
    val score: Int,
    val people: List<String>
)

/**
 * Journey 图表数据库 - 对标 mermaid-js journeyDb.js
 *
 * 存储用户旅程图的 sections、tasks 和 actors 数据。
 */
class JourneyDb : DiagramDB {
    private var diagramTitle: String = ""
    private var accTitle: String = ""
    private var accDescr: String = ""

    private var currentSection: String = ""
    private val sections: MutableList<String> = mutableListOf()
    private val tasks: MutableList<JourneyTask> = mutableListOf()

    override fun clear() {
        diagramTitle = ""
        accTitle = ""
        accDescr = ""
        currentSection = ""
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
     * 添加任务
     *
     * @param descr 任务描述
     * @param taskData 任务数据，格式为 ":score:actor1, actor2" 或 ":score"
     */
    fun addTask(descr: String, taskData: String) {
        val data = taskData.trimStart().removePrefix(":")
        val pieces = data.split(":")
        val score: Int
        val people: List<String>

        if (pieces.size == 1) {
            score = pieces[0].trim().toIntOrNull() ?: 0
            people = emptyList()
        } else {
            score = pieces[0].trim().toIntOrNull() ?: 0
            people = pieces[1].split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }

        val task = JourneyTask(
            section = currentSection,
            type = currentSection,
            task = descr.trim(),
            score = score,
            people = people
        )
        tasks.add(task)
    }

    /**
     * 获取所有任务
     */
    fun getTasks(): List<JourneyTask> = tasks.toList()

    /**
     * 获取所有参与者（去重排序）
     */
    fun getActors(): List<String> {
        val actorSet = mutableSetOf<String>()
        for (task in tasks) {
            for (person in task.people) {
                actorSet.add(person)
            }
        }
        return actorSet.sorted()
    }
}
