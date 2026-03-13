package io.lugf027.github.mermaid.core.diagram.ishikawa

import io.lugf027.github.mermaid.core.diagram.DiagramDB

/**
 * 鱼骨图(因果图)数据库 - 对标 mermaid-js ishikawaDb.ts
 *
 * 极简的递归树结构：根节点为"效果"(鱼头)，
 * 一级子节点为"原因类别"，更深层为子原因。
 */
class IshikawaDb : DiagramDB {

    /** 鱼骨图节点 */
    data class IshikawaNode(
        val text: String,
        val children: MutableList<IshikawaNode> = mutableListOf()
    )

    // --- 内部状态 ---
    private var rootNode: IshikawaNode? = null
    private val nodeStack = mutableListOf<IshikawaNode>() // 构建层级用的栈

    // --- DiagramDB ---
    private var diagramTitle = ""
    private var accTitle = ""
    private var accDescription = ""

    override fun clear() {
        rootNode = null
        nodeStack.clear()
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

    /**
     * 添加节点（基于缩进层级）
     * level 0 = 根节点 (效果/鱼头)
     * level 1 = 原因类别 (主骨)
     * level 2+ = 子原因 (分支骨)
     */
    fun addNode(level: Int, text: String) {
        val node = IshikawaNode(text)

        if (level == 0 || rootNode == null) {
            rootNode = node
            nodeStack.clear()
            nodeStack.add(node)
        } else {
            // 将栈回退到 parent 层级
            while (nodeStack.size > level) {
                nodeStack.removeAt(nodeStack.size - 1)
            }
            if (nodeStack.isNotEmpty()) {
                nodeStack.last().children.add(node)
            }
            nodeStack.add(node)
        }
    }

    // --- 查询 ---

    fun getRootNode(): IshikawaNode? = rootNode

    fun getCategories(): List<IshikawaNode> = rootNode?.children ?: emptyList()
}
