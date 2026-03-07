package io.lugf027.github.mermaid.core.diagrams.packet

import io.lugf027.github.mermaid.core.types.*

class PacketDiagramDefinition : DiagramDefinition {
    private val _db = PacketDb()
    override val parser: ParserDefinition = PacketParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = PacketRenderer()
}
