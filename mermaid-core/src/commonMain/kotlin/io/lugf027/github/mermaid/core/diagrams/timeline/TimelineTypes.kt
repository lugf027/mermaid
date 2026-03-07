package io.lugf027.github.mermaid.core.diagrams.timeline

data class TimelineEvent(
    val period: String,
    val events: MutableList<String> = mutableListOf(),
    val section: String = "",
)
