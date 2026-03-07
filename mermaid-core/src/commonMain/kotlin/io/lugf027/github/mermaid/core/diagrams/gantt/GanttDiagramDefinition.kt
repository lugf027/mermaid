package io.lugf027.github.mermaid.core.diagrams.gantt

import io.lugf027.github.mermaid.core.types.*

class GanttDiagramDefinition : DiagramDefinition {
    private val _db = GanttDb()
    override val parser: ParserDefinition = GanttParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = GanttRenderer()
}
