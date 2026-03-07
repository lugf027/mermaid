package io.lugf027.github.mermaid.core.diagrams.mindmap

import io.lugf027.github.mermaid.core.types.*

class MindmapDiagramDefinition : DiagramDefinition {
    private val _db = MindmapDb()
    override val parser: ParserDefinition = MindmapParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = MindmapRenderer()
}
