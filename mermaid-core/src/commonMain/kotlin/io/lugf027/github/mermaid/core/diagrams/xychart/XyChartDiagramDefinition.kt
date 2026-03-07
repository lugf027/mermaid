package io.lugf027.github.mermaid.core.diagrams.xychart

import io.lugf027.github.mermaid.core.types.*

class XyChartDiagramDefinition : DiagramDefinition {
    private val _db = XyChartDb()
    override val parser: ParserDefinition = XyChartParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = XyChartRenderer()
}
