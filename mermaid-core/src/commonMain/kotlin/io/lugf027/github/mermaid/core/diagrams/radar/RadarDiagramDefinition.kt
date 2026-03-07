package io.lugf027.github.mermaid.core.diagrams.radar

import io.lugf027.github.mermaid.core.types.*

class RadarDiagramDefinition : DiagramDefinition {
    private val _db = RadarDb()
    override val parser: ParserDefinition = RadarParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = RadarRenderer()
}
