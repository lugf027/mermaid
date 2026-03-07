package io.lugf027.github.mermaid.core.diagrams.journey

import io.lugf027.github.mermaid.core.db.CommonDb

data class JourneyTask(val task: String, val score: Int, val people: List<String>, val section: String)

class JourneyDb : CommonDb() {
    private val tasks = mutableListOf<JourneyTask>()
    private val sections = mutableListOf<String>()
    private var currentSection = ""

    fun addSection(name: String) { currentSection = name; sections.add(name) }
    fun addTask(task: String, score: Int, people: List<String>) { tasks.add(JourneyTask(task, score, people, currentSection)) }
    fun getTasks() = tasks; fun getSections() = sections

    override fun clear() { super.clear(); tasks.clear(); sections.clear(); currentSection = "" }
}
