package io.lugf027.github.mermaid.core.diagram.kanban

import io.lugf027.github.mermaid.core.diagram.DiagramDB

/**
 * 看板图数据库 - 对标 mermaid-js kanbanDb.ts
 *
 * 基于缩进的列式看板，section（level=0）作为列，item（level>0）作为卡片。
 * 支持 @{...} 元数据语法指定 priority/assigned/ticket/icon。
 */
class KanbanDb : DiagramDB {

    /** 看板节点 */
    data class KanbanNode(
        val id: String,
        val label: String,
        val level: Int,        // 0=section, >0=item
        val isSection: Boolean,
        var parentId: String = "",  // section's id
        var priority: String = "",  // high/medium/low
        var assigned: String = "",
        var ticket: String = "",
        var icon: String = "",
        var cssClass: String = ""
    )

    // --- 内部状态 ---
    private val nodes = mutableListOf<KanbanNode>()
    private val sections = mutableListOf<KanbanNode>()

    // --- DiagramDB ---
    private var diagramTitle = ""
    private var accTitle = ""
    private var accDescription = ""

    override fun clear() {
        nodes.clear()
        sections.clear()
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

    fun addNode(id: String, label: String, level: Int, metadata: Map<String, String> = emptyMap()) {
        val isSection = level == 0
        val parentId = if (!isSection && sections.isNotEmpty()) sections.last().id else ""

        val node = KanbanNode(
            id = id,
            label = label,
            level = level,
            isSection = isSection,
            parentId = parentId,
            priority = metadata["priority"] ?: "",
            assigned = metadata["assigned"] ?: "",
            ticket = metadata["ticket"] ?: "",
            icon = metadata["icon"] ?: "",
            cssClass = metadata["class"] ?: ""
        )

        nodes.add(node)
        if (isSection) sections.add(node)
    }

    // --- 查询 ---

    fun getNodes(): List<KanbanNode> = nodes.toList()
    fun getSections(): List<KanbanNode> = sections.toList()

    fun getItemsBySection(sectionId: String): List<KanbanNode> =
        nodes.filter { !it.isSection && it.parentId == sectionId }
}
