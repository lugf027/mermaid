package io.lugf027.github.mermaid.core.diagrams.kanban

data class KanbanColumn(
    val id: String,
    val label: String,
    val items: MutableList<KanbanItem> = mutableListOf(),
)

data class KanbanItem(
    val id: String,
    val label: String,
    val priority: String? = null,
    val assigned: String? = null,
)
