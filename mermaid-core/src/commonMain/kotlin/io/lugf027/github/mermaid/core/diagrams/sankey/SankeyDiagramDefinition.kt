package io.lugf027.github.mermaid.core.diagrams.sankey

import io.lugf027.github.mermaid.core.types.*

class SankeyDiagramDefinition : DiagramDefinition {
    private val _db = SankeyDb()
    override val parser: ParserDefinition = SankeyParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = SankeyRenderer()
}
