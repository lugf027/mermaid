package io.lugf027.github.mermaid.core.diagrams.error

import io.lugf027.github.mermaid.core.types.*

class ErrorDiagramDefinition : DiagramDefinition {
    private val _db = ErrorDb()
    override val parser: ParserDefinition = ErrorParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = ErrorRenderer()
}
