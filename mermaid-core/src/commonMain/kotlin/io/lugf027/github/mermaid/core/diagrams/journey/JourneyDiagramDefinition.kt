package io.lugf027.github.mermaid.core.diagrams.journey

import io.lugf027.github.mermaid.core.types.*

class JourneyDiagramDefinition : DiagramDefinition {
    private val _db = JourneyDb()
    override val parser: ParserDefinition = JourneyParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = JourneyRenderer()
}
