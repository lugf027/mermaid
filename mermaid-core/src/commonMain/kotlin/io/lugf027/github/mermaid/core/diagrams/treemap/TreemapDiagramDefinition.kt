package io.lugf027.github.mermaid.core.diagrams.treemap

import io.lugf027.github.mermaid.core.types.*

class TreemapDiagramDefinition : DiagramDefinition {
    private val _db = TreemapDb()
    override val parser: ParserDefinition = TreemapParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = TreemapRenderer()
}
