package io.lugf027.github.mermaid.core.diagrams.timeline

import io.lugf027.github.mermaid.core.types.*

class TimelineDiagramDefinition : DiagramDefinition {
    private val _db = TimelineDb()
    override val parser: ParserDefinition = TimelineParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = TimelineRenderer()
}
