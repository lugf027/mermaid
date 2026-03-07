package io.lugf027.github.mermaid.core.diagrams.treemap

import io.lugf027.github.mermaid.core.db.CommonDb

data class TreemapNode(val label: String, val value: Float = 0f, val children: MutableList<TreemapNode> = mutableListOf())

class TreemapDb : CommonDb() {
    var root: TreemapNode? = null
    override fun clear() { super.clear(); root = null }
}
