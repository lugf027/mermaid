package io.lugf027.github.mermaid.core.diagrams.sequence

import io.lugf027.github.mermaid.core.types.*

class SequenceDiagramDefinition : DiagramDefinition {
    private val _db = SequenceDb()
    override val parser: ParserDefinition = SequenceParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = SequenceRenderer()
}
