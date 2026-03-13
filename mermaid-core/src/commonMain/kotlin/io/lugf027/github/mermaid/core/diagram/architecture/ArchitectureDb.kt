package io.lugf027.github.mermaid.core.diagram.architecture

import io.lugf027.github.mermaid.core.diagram.DiagramDB

/**
 * 架构图数据库 - 对标 mermaid-js architectureDb.ts
 *
 * 存储 service 节点、junction 节点、group 分组和 edge 边。
 * 边带有方向（L/R/T/B）和箭头（into）属性。
 */
class ArchitectureDb : DiagramDB {

    /** 方向 */
    enum class Direction { L, R, T, B }

    /** 服务节点 */
    data class Service(
        val id: String,
        var icon: String = "",
        var iconText: String = "",
        var title: String = "",
        var inGroup: String = ""
    )

    /** 连接点节点 */
    data class Junction(
        val id: String,
        var inGroup: String = ""
    )

    /** 分组 */
    data class Group(
        val id: String,
        var icon: String = "",
        var title: String = "",
        var inGroup: String = ""
    )

    /** 边 */
    data class Edge(
        val lhsId: String,
        val lhsDir: Direction,
        val lhsInto: Boolean,     // 左端有箭头
        val lhsGroup: Boolean,    // 左端穿越 group 边界
        val rhsId: String,
        val rhsDir: Direction,
        val rhsInto: Boolean,     // 右端有箭头
        val rhsGroup: Boolean,    // 右端穿越 group 边界
        val title: String = ""
    )

    // --- 内部状态 ---
    private val services = mutableMapOf<String, Service>()
    private val junctions = mutableMapOf<String, Junction>()
    private val groups = mutableMapOf<String, Group>()
    private val edges = mutableListOf<Edge>()

    // --- DiagramDB ---
    private var diagramTitle = ""
    private var accTitle = ""
    private var accDescription = ""

    override fun clear() {
        services.clear()
        junctions.clear()
        groups.clear()
        edges.clear()
        diagramTitle = ""
        accTitle = ""
        accDescription = ""
    }

    override fun setDiagramTitle(title: String) { diagramTitle = title }
    override fun getDiagramTitle(): String = diagramTitle
    override fun setAccTitle(title: String) { accTitle = title }
    override fun getAccTitle(): String = accTitle
    override fun setAccDescription(desc: String) { accDescription = desc }
    override fun getAccDescription(): String = accDescription

    // --- 操作 ---

    fun addService(id: String, icon: String = "", iconText: String = "", title: String = "", inGroup: String = "") {
        services[id] = Service(id, icon, iconText, title, inGroup)
    }

    fun addJunction(id: String, inGroup: String = "") {
        junctions[id] = Junction(id, inGroup)
    }

    fun addGroup(id: String, icon: String = "", title: String = "", inGroup: String = "") {
        groups[id] = Group(id, icon, title, inGroup)
    }

    fun addEdge(edge: Edge) {
        edges.add(edge)
    }

    // --- 查询 ---

    fun getServices(): Map<String, Service> = services.toMap()
    fun getJunctions(): Map<String, Junction> = junctions.toMap()
    fun getGroups(): Map<String, Group> = groups.toMap()
    fun getEdges(): List<Edge> = edges.toList()

    fun getAllNodeIds(): Set<String> = services.keys + junctions.keys
    fun getGroupChildren(groupId: String): List<String> {
        val result = mutableListOf<String>()
        for ((id, s) in services) if (s.inGroup == groupId) result.add(id)
        for ((id, j) in junctions) if (j.inGroup == groupId) result.add(id)
        return result
    }
}
