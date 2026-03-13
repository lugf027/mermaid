package io.lugf027.github.mermaid.core.diagram.mindmap

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.util.TextUtils

/**
 * 思维导图节点形状类型
 */
enum class MindmapNodeType(val value: Int) {
    DEFAULT(0),        // 无边框，底部有一条线
    ROUNDED_RECT(1),   // 圆角矩形
    RECT(2),           // 直角矩形
    CIRCLE(3),         // 圆形
    CLOUD(4),          // 云朵形
    BANG(5),           // 爆炸形
    HEXAGON(6);        // 六边形
}

/**
 * 思维导图节点数据类
 */
data class MindmapNode(
    val id: Int,
    val nodeId: String,
    val level: Int,
    val descr: String,
    val type: MindmapNodeType,
    val children: MutableList<MindmapNode> = mutableListOf(),
    var width: Double = 200.0,
    var height: Double = 0.0,
    val padding: Int = 10,
    var section: Int = -1,
    var icon: String? = null,
    var cssClass: String? = null,
    var isRoot: Boolean = false,
    var x: Double = 0.0,
    var y: Double = 0.0
)

/**
 * Mindmap 数据库 - 对标 mermaid-js mindmapDb.ts
 *
 * 管理树形结构的思维导图节点。
 */
class MindmapDb : DiagramDB {
    private var diagramTitle: String = ""
    private var accTitle: String = ""
    private var accDescr: String = ""

    private var nodeCount: Int = 0
    private var baseLevel: Int = -1
    private val nodes: MutableList<MindmapNode> = mutableListOf()
    private var rootNode: MindmapNode? = null

    companion object {
        private const val MAX_SECTIONS = 12

        /**
         * 从文本的首尾定界符检测节点形状类型
         */
        fun getType(startStr: String, endStr: String): MindmapNodeType {
            return when {
                startStr == "[" && endStr == "]" -> MindmapNodeType.RECT
                startStr == "(" && endStr == ")" -> MindmapNodeType.ROUNDED_RECT
                startStr == "((" && endStr == "))" -> MindmapNodeType.CIRCLE
                startStr == ")" && endStr == "(" -> MindmapNodeType.CLOUD
                startStr == "(-" && endStr == "-)" -> MindmapNodeType.CLOUD
                startStr == "))" && endStr == "((" -> MindmapNodeType.BANG
                startStr == "{{" && endStr == "}}" -> MindmapNodeType.HEXAGON
                else -> MindmapNodeType.DEFAULT
            }
        }
    }

    override fun clear() {
        diagramTitle = ""
        accTitle = ""
        accDescr = ""
        nodeCount = 0
        baseLevel = -1
        nodes.clear()
        rootNode = null
    }

    override fun setDiagramTitle(title: String) { diagramTitle = title }
    override fun getDiagramTitle(): String = diagramTitle
    override fun setAccTitle(title: String) { accTitle = title }
    override fun getAccTitle(): String = accTitle
    override fun setAccDescription(desc: String) { accDescr = desc }
    override fun getAccDescription(): String = accDescr

    /**
     * 添加节点到树中
     *
     * @param level 缩进级别（空格数）
     * @param nodeId 节点 ID（文本）
     * @param descr 节点描述（显示文本）
     * @param type 节点形状类型
     */
    fun addNode(level: Int, nodeId: String, descr: String, type: MindmapNodeType) {
        val sanitizedId = TextUtils.sanitizeText(nodeId.trim())
        val sanitizedDescr = TextUtils.sanitizeText(descr.trim())

        val adjustedLevel: Int
        val isRoot: Boolean

        if (nodes.isEmpty()) {
            // 第一个节点 = 根节点
            baseLevel = level
            adjustedLevel = 0
            isRoot = true
        } else {
            adjustedLevel = level - baseLevel
            isRoot = false
        }

        // 计算 padding：某些形状的 padding 翻倍
        val basePadding = 10
        val actualPadding = when (type) {
            MindmapNodeType.ROUNDED_RECT, MindmapNodeType.RECT, MindmapNodeType.HEXAGON -> basePadding * 2
            else -> basePadding
        }

        val node = MindmapNode(
            id = nodeCount++,
            nodeId = sanitizedId,
            level = adjustedLevel,
            descr = sanitizedDescr.ifEmpty { sanitizedId },
            type = type,
            padding = actualPadding,
            isRoot = isRoot
        )

        nodes.add(node)

        if (isRoot) {
            rootNode = node
        } else {
            // 找到父节点
            val parent = getParent(adjustedLevel)
            if (parent != null) {
                parent.children.add(node)
            } else {
                throw IllegalStateException("There can be only one root node in a mindmap")
            }
        }
    }

    /**
     * 装饰最近添加的节点（设置 icon 或 class）
     */
    fun decorateNode(icon: String? = null, cssClass: String? = null) {
        if (nodes.isEmpty()) return
        val lastNode = nodes.last()
        if (icon != null) nodes[nodes.lastIndex] = lastNode.copy(icon = icon)
        if (cssClass != null) nodes[nodes.lastIndex] = lastNode.copy(cssClass = cssClass)
        // 更新引用
        if (lastNode.isRoot) {
            rootNode = nodes.last()
        }
    }

    /**
     * 获取根节点
     */
    fun getRootNode(): MindmapNode? = rootNode

    /**
     * 获取所有节点（扁平列表）
     */
    fun getNodes(): List<MindmapNode> = nodes.toList()

    /**
     * 分配 section 编号（颜色分区）
     */
    fun assignSections() {
        val root = rootNode ?: return
        root.section = -1 // 根节点没有 section

        for ((index, child) in root.children.withIndex()) {
            val sectionNum = index % (MAX_SECTIONS - 1)
            assignSectionRecursive(child, sectionNum)
        }
    }

    private fun assignSectionRecursive(node: MindmapNode, section: Int) {
        // 更新 nodes 列表中对应的节点
        val idx = nodes.indexOfFirst { it.id == node.id }
        if (idx >= 0) {
            nodes[idx] = nodes[idx].copy(section = section)
            node.section = section
        }
        for (child in node.children) {
            assignSectionRecursive(child, section)
        }
    }

    /**
     * 从 nodes 数组末尾向前搜索，找到第一个 level 小于给定 level 的节点
     */
    private fun getParent(level: Int): MindmapNode? {
        for (i in nodes.lastIndex downTo 0) {
            if (nodes[i].level < level) {
                return nodes[i]
            }
        }
        return null
    }
}
