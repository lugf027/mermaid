package io.lugf027.github.mermaid.core.diagrams.git

import io.lugf027.github.mermaid.core.types.*

class GitDiagramDefinition : DiagramDefinition {
    private val _db = GitDb()
    override val parser: ParserDefinition = GitParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = GitRenderer()
}
