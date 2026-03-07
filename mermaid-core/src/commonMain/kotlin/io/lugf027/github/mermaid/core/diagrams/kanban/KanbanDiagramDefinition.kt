package io.lugf027.github.mermaid.core.diagrams.kanban

import io.lugf027.github.mermaid.core.types.*

class KanbanDiagramDefinition : DiagramDefinition {
    private val _db = KanbanDb()
    override val parser: ParserDefinition = KanbanParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = KanbanRenderer()
}
