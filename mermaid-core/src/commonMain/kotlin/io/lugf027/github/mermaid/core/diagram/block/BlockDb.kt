package io.lugf027.github.mermaid.core.diagram.block

import io.lugf027.github.mermaid.core.diagram.DiagramDB

/**
 * 块图数据库 - 对标 mermaid-js blockDB.ts
 *
 * 存储 block（块节点）和 edge（连线），支持嵌套 block...end 结构和 grid 网格布局。
 * Block 图使用 columns 设定网格列数，节点可通过 widthInColumns 跨多列。
 */
class BlockDb : DiagramDB {

    /** 块节点类型 */
    enum class BlockType {
        BLOCK,       // 普通块（带形状）
        SPACE,       // 空白占位
        COMPOSITE    // 复合块 (block...end)
    }

    /**
     * 块节点
     */
    data class Block(
        val id: String,
        val label: String = "",
        val type: BlockType = BlockType.BLOCK,
        val shape: String = "rect",  // rect, round, stadium, subroutine, cylinder, circle, diamond, hexagon, lean_right, lean_left, trapezoid, inv_trapezoid, double_circle
        val widthInColumns: Int = 1,  // 跨列数（auto = -1）
        val parentId: String = "root",
        val children: MutableList<Block> = mutableListOf(),
        val columns: Int = -1,  // 本块内的子块列数，-1 表示 auto
        // 渲染后填充
        var x: Double = 0.0,
        var y: Double = 0.0,
        var width: Double = 0.0,
        var height: Double = 0.0,
        // 样式
        val styleClasses: MutableList<String> = mutableListOf(),
        var styles: Map<String, String> = emptyMap()
    )

    /**
     * 边（连线）
     */
    data class Edge(
        val source: String,
        val target: String,
        val label: String = "",
        val arrowTypeStart: String = "none",
        val arrowTypeEnd: String = "arrow_point",
        val lineType: String = "normal"  // normal, thick, dotted
    )

    // --- 内部状态 ---
    private val rootBlock = Block(
        id = "root",
        type = BlockType.COMPOSITE,
        columns = -1
    )
    private val blocks = mutableMapOf<String, Block>("root" to rootBlock)
    private val edges = mutableListOf<Edge>()
    private var defaultColumns = -1  // 全局默认列数

    // 解析栈：跟踪当前解析上下文
    private val parseStack = mutableListOf("root")
    private val classDefs = mutableMapOf<String, Map<String, String>>()

    // --- DiagramDB ---
    private var diagramTitle = ""
    private var accTitle = ""
    private var accDescription = ""

    override fun clear() {
        blocks.clear()
        edges.clear()
        parseStack.clear()
        parseStack.add("root")
        classDefs.clear()
        defaultColumns = -1
        diagramTitle = ""
        accTitle = ""
        accDescription = ""
        val newRoot = Block(id = "root", type = BlockType.COMPOSITE, columns = -1)
        blocks["root"] = newRoot
    }

    override fun setDiagramTitle(title: String) { diagramTitle = title }
    override fun getDiagramTitle(): String = diagramTitle
    override fun setAccTitle(title: String) { accTitle = title }
    override fun getAccTitle(): String = accTitle
    override fun setAccDescription(desc: String) { accDescription = desc }
    override fun getAccDescription(): String = accDescription

    // --- Block 操作 ---

    /**
     * 设置全局列数
     */
    fun setColumns(columns: Int) {
        defaultColumns = columns
        // 也设置当前上下文的列数
        val currentParentId = parseStack.last()
        blocks[currentParentId]?.let {
            blocks[currentParentId] = it.copy(columns = columns)
        }
    }

    /**
     * 获取全局列数
     */
    fun getColumns(): Int = defaultColumns

    /**
     * 添加块节点
     */
    fun addBlock(
        id: String,
        label: String = "",
        shape: String = "rect",
        widthInColumns: Int = 1,
        styleClasses: List<String> = emptyList()
    ) {
        val parentId = parseStack.last()
        val block = Block(
            id = id,
            label = label.ifEmpty { id },
            type = BlockType.BLOCK,
            shape = shape,
            widthInColumns = widthInColumns,
            parentId = parentId,
            styleClasses = styleClasses.toMutableList()
        )
        blocks[id] = block
        blocks[parentId]?.children?.add(block)
    }

    /**
     * 添加空白占位
     */
    fun addSpace(widthInColumns: Int = 1) {
        val parentId = parseStack.last()
        val id = "space_${blocks.size}"
        val block = Block(
            id = id,
            type = BlockType.SPACE,
            widthInColumns = widthInColumns,
            parentId = parentId
        )
        blocks[id] = block
        blocks[parentId]?.children?.add(block)
    }

    /**
     * 开始复合块 (block...end)
     */
    fun startComposite(id: String, label: String = "", columns: Int = -1) {
        val parentId = parseStack.last()
        val block = Block(
            id = id,
            label = label.ifEmpty { id },
            type = BlockType.COMPOSITE,
            columns = columns,
            parentId = parentId
        )
        blocks[id] = block
        blocks[parentId]?.children?.add(block)
        parseStack.add(id)
    }

    /**
     * 结束复合块
     */
    fun endComposite() {
        if (parseStack.size > 1) {
            parseStack.removeAt(parseStack.size - 1)
        }
    }

    /**
     * 添加边
     */
    fun addEdge(
        source: String,
        target: String,
        label: String = "",
        arrowTypeStart: String = "none",
        arrowTypeEnd: String = "arrow_point",
        lineType: String = "normal"
    ) {
        edges.add(Edge(
            source = source,
            target = target,
            label = label,
            arrowTypeStart = arrowTypeStart,
            arrowTypeEnd = arrowTypeEnd,
            lineType = lineType
        ))
    }

    /**
     * 添加 classDef
     */
    fun addClassDef(name: String, styles: Map<String, String>) {
        classDefs[name] = styles
    }

    /**
     * 应用 class 到节点
     */
    fun applyClass(nodeIds: List<String>, className: String) {
        for (id in nodeIds) {
            blocks[id]?.let {
                it.styleClasses.add(className)
                // 合并 classDef 样式
                classDefs[className]?.let { styles ->
                    val existing = it.styles.toMutableMap()
                    existing.putAll(styles)
                    blocks[id] = it.copy(styles = existing)
                }
            }
        }
    }

    // --- 查询 ---

    fun getRootBlock(): Block = blocks["root"]!!

    fun getBlock(id: String): Block? = blocks[id]

    fun getAllBlocks(): Map<String, Block> = blocks.toMap()

    fun getEdges(): List<Edge> = edges.toList()

    fun getClassDefs(): Map<String, Map<String, String>> = classDefs.toMap()

    /**
     * 获取块的直接子块
     */
    fun getChildren(parentId: String): List<Block> {
        return blocks[parentId]?.children ?: emptyList()
    }
}
