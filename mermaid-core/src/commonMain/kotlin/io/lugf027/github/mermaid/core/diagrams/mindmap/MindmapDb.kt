package io.lugf027.github.mermaid.core.diagrams.mindmap

import io.lugf027.github.mermaid.core.db.CommonDb

/**
 * 思维导图数据存储层。
 */
class MindmapDb : CommonDb() {

    private var root: MindmapNode? = null
    private var nodeCounter = 0

    fun setRoot(node: MindmapNode) { root = node }
    fun getRoot(): MindmapNode? = root

    fun createNode(level: Int, text: String, type: MindmapNodeType = MindmapNodeType.DEFAULT): MindmapNode {
        return MindmapNode(
            id = "mm_${nodeCounter++}",
            level = level,
            text = text,
            type = type,
        )
    }

    override fun clear() {
        super.clear()
        root = null
        nodeCounter = 0
    }

}
