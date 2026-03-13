package io.lugf027.github.mermaid.core.diagram.treemap

import io.lugf027.github.mermaid.core.diagram.DiagramDB

/**
 * 树形图数据库 - 对标 mermaid-js treemap/db.ts
 *
 * 存储层级节点（Section 和 Leaf），构建树形结构。
 * Section 有子节点，Leaf 有值。
 */
class TreemapDb : DiagramDB {

    /** 树形图节点类型 */
    enum class NodeType { SECTION, LEAF }

    /** 树形图节点 */
    data class TreemapNode(
        val name: String,
        val type: NodeType,
        val value: Double = 0.0,
        val children: MutableList<TreemapNode> = mutableListOf(),
        var parent: TreemapNode? = null,
        var cssClass: String = ""
    ) {
        /** 获取该节点（含子节点）的总值 */
        fun totalValue(): Double {
            return if (children.isEmpty()) value
            else children.sumOf { it.totalValue() }
        }
    }

    // --- 内部状态 ---
    private var rootNode: TreemapNode? = null
    private val nodeStack = mutableListOf<TreemapNode>()
    private val classDefs = mutableMapOf<String, Map<String, String>>()

    // --- DiagramDB ---
    private var diagramTitle = ""
    private var accTitle = ""
    private var accDescription = ""

    override fun clear() {
        rootNode = null
        nodeStack.clear()
        classDefs.clear()
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

    fun addSection(name: String, level: Int, cssClass: String = "") {
        val node = TreemapNode(name, NodeType.SECTION, cssClass = cssClass)
        addNodeAtLevel(node, level)
    }

    fun addLeaf(name: String, value: Double, level: Int, cssClass: String = "") {
        val node = TreemapNode(name, NodeType.LEAF, value, cssClass = cssClass)
        addNodeAtLevel(node, level)
    }

    private fun addNodeAtLevel(node: TreemapNode, level: Int) {
        if (rootNode == null) {
            rootNode = node
            nodeStack.clear()
            nodeStack.add(node)
            return
        }

        // 回退栈到 parent 层级
        while (nodeStack.size > level) {
            nodeStack.removeAt(nodeStack.size - 1)
        }

        if (nodeStack.isNotEmpty()) {
            val parent = nodeStack.last()
            node.parent = parent
            parent.children.add(node)
        }

        nodeStack.add(node)
    }

    fun addClassDef(name: String, styles: Map<String, String>) {
        classDefs[name] = styles
    }

    // --- 查询 ---

    fun getRootNode(): TreemapNode? = rootNode
    fun getClassDefs(): Map<String, Map<String, String>> = classDefs.toMap()
}
