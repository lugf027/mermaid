package io.lugf027.github.mermaid.core.diagrams.c4

import io.lugf027.github.mermaid.core.types.*

class C4DiagramDefinition : DiagramDefinition {
    private val _db = C4Db()
    override val parser: ParserDefinition = C4Parser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = C4Renderer()
}
