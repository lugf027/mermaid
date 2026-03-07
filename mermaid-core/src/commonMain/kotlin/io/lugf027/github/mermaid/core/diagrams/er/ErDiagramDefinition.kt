package io.lugf027.github.mermaid.core.diagrams.er

import io.lugf027.github.mermaid.core.types.*

class ErDiagramDefinition : DiagramDefinition {
    private val _db = ErDb()
    override val parser: ParserDefinition = ErParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = ErRenderer()
}
