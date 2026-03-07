package io.lugf027.github.mermaid.core.diagrams.block

import io.lugf027.github.mermaid.core.db.CommonDb

data class BlockNode(val id: String, val label: String = id, val type: String = "square", val children: MutableList<BlockNode> = mutableListOf(), val columns: Int = -1)
data class BlockEdge(val start: String, val end: String, val label: String = "")

class BlockDb : CommonDb() {
    private val blocks = mutableListOf<BlockNode>()
    private val edges = mutableListOf<BlockEdge>()
    var columns = 0

    fun addBlock(block: BlockNode) { blocks.add(block) }
    fun addEdge(edge: BlockEdge) { edges.add(edge) }
    fun getBlocks() = blocks; fun getEdges() = edges

    override fun clear() { super.clear(); blocks.clear(); edges.clear(); columns = 0 }
}
