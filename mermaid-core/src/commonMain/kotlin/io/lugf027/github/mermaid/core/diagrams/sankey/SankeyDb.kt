package io.lugf027.github.mermaid.core.diagrams.sankey

import io.lugf027.github.mermaid.core.db.CommonDb

data class SankeyLink(val source: String, val target: String, val value: Float)

class SankeyDb : CommonDb() {
    private val nodes = mutableSetOf<String>()
    private val links = mutableListOf<SankeyLink>()

    fun addLink(source: String, target: String, value: Float) {
        nodes.add(source); nodes.add(target)
        links.add(SankeyLink(source, target, value))
    }

    fun getNodes() = nodes.toList(); fun getLinks() = links

    override fun clear() { super.clear(); nodes.clear(); links.clear() }
}
