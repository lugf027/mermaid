package io.lugf027.github.mermaid.core.diagrams.architecture

import io.lugf027.github.mermaid.core.types.*

class ArchitectureDiagramDefinition : DiagramDefinition {
    private val _db = ArchitectureDb()
    override val parser: ParserDefinition = ArchitectureParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = ArchitectureRenderer()
}
