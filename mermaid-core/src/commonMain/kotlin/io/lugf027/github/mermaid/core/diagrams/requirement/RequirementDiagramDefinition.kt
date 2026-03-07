package io.lugf027.github.mermaid.core.diagrams.requirement

import io.lugf027.github.mermaid.core.types.*

class RequirementDiagramDefinition : DiagramDefinition {
    private val _db = RequirementDb()
    override val parser: ParserDefinition = RequirementParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = RequirementRenderer()
}
