package io.lugf027.github.mermaid.core.diagrams.state

import io.lugf027.github.mermaid.core.types.*

class StateDiagramDefinition : DiagramDefinition {
    private val _db = StateDb()
    override val parser: ParserDefinition = StateParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = StateRenderer()
}
