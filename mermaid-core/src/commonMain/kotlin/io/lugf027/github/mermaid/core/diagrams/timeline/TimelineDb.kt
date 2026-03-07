package io.lugf027.github.mermaid.core.diagrams.timeline

import io.lugf027.github.mermaid.core.db.CommonDb

class TimelineDb : CommonDb() {
    private val events = mutableListOf<TimelineEvent>()
    private var currentSection = ""

    fun addSection(name: String) { currentSection = name }

    fun addEvent(period: String, eventTexts: List<String>) {
        val event = TimelineEvent(period = period, section = currentSection)
        event.events.addAll(eventTexts)
        events.add(event)
    }

    fun getEvents(): List<TimelineEvent> = events

    override fun clear() { super.clear(); events.clear(); currentSection = "" }
}
