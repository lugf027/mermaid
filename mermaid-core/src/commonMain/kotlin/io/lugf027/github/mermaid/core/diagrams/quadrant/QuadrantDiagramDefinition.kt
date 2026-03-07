package io.lugf027.github.mermaid.core.diagrams.quadrant

import io.lugf027.github.mermaid.core.types.*

class QuadrantDiagramDefinition : DiagramDefinition {
    private val _db = QuadrantDb()
    override val parser: ParserDefinition = QuadrantParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = QuadrantRenderer()
}
