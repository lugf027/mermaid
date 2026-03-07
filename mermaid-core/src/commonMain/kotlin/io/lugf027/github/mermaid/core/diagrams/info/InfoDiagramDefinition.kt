package io.lugf027.github.mermaid.core.diagrams.info

import io.lugf027.github.mermaid.core.types.*

class InfoDiagramDefinition : DiagramDefinition {
    private val _db = InfoDb()
    override val parser: ParserDefinition = InfoParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = InfoRenderer()
}
