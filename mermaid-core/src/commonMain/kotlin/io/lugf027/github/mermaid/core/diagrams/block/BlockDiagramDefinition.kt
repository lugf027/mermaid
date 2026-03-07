package io.lugf027.github.mermaid.core.diagrams.block

import io.lugf027.github.mermaid.core.types.*

class BlockDiagramDefinition : DiagramDefinition {
    private val _db = BlockDb()
    override val parser: ParserDefinition = BlockParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = BlockRenderer()
}
