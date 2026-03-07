package io.lugf027.github.mermaid.core.diagrams.ishikawa

import io.lugf027.github.mermaid.core.types.*

class IshikawaDiagramDefinition : DiagramDefinition {
    private val _db = IshikawaDb()
    override val parser: ParserDefinition = IshikawaParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = IshikawaRenderer()
}
