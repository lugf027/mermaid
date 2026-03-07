package io.lugf027.github.mermaid.core.diagrams.venn

import io.lugf027.github.mermaid.core.types.*

class VennDiagramDefinition : DiagramDefinition {
    private val _db = VennDb()
    override val parser: ParserDefinition = VennParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = VennRenderer()
}
