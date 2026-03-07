package io.lugf027.github.mermaid.core.diagrams.classdiagram

import io.lugf027.github.mermaid.core.types.*

class ClassDiagramDefinition : DiagramDefinition {
    private val _db = ClassDb()
    override val parser: ParserDefinition = ClassParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = ClassRenderer()
}
