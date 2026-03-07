package io.lugf027.github.mermaid.core.diagrams.architecture

import io.lugf027.github.mermaid.core.db.CommonDb

enum class ArchNodeType { SERVICE, JUNCTION }
data class ArchNode(val id: String, val type: ArchNodeType, val title: String = "", val icon: String? = null, val groupId: String? = null)
data class ArchGroup(val id: String, val title: String = "", val icon: String? = null, val parentId: String? = null)
data class ArchEdge(val lhsId: String, val lhsDir: String, val rhsId: String, val rhsDir: String, val title: String = "", val arrow: Boolean = false)

class ArchitectureDb : CommonDb() {
    private val nodes = mutableListOf<ArchNode>()
    private val groups = mutableListOf<ArchGroup>()
    private val edges = mutableListOf<ArchEdge>()

    fun addNode(node: ArchNode) { nodes.add(node) }
    fun addGroup(group: ArchGroup) { groups.add(group) }
    fun addEdge(edge: ArchEdge) { edges.add(edge) }
    fun getNodes() = nodes; fun getGroups() = groups; fun getEdges() = edges

    override fun clear() { super.clear(); nodes.clear(); groups.clear(); edges.clear() }
}
