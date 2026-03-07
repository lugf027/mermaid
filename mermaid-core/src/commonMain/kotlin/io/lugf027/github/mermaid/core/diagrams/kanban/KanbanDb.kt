package io.lugf027.github.mermaid.core.diagrams.kanban

import io.lugf027.github.mermaid.core.db.CommonDb

class KanbanDb : CommonDb() {
    private val columns = mutableListOf<KanbanColumn>()
    private var itemCounter = 0

    fun addColumn(label: String) {
        columns.add(KanbanColumn(id = "col_${columns.size}", label = label))
    }

    fun addItem(label: String, metadata: Map<String, String> = emptyMap()) {
        val col = columns.lastOrNull() ?: return
        col.items.add(KanbanItem(
            id = "item_${itemCounter++}",
            label = label,
            priority = metadata["priority"],
            assigned = metadata["assigned"],
        ))
    }

    fun getColumns(): List<KanbanColumn> = columns

    override fun clear() { super.clear(); columns.clear(); itemCounter = 0 }
}
