package io.lugf027.github.mermaid.core.diagrams.packet

import io.lugf027.github.mermaid.core.db.CommonDb

data class PacketBlock(val start: Int, val end: Int, val label: String)

class PacketDb : CommonDb() {
    private val blocks = mutableListOf<PacketBlock>()
    fun addBlock(block: PacketBlock) { blocks.add(block) }
    fun getBlocks() = blocks
    override fun clear() { super.clear(); blocks.clear() }
}
