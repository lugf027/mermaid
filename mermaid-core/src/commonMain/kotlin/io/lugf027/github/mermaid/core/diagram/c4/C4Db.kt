package io.lugf027.github.mermaid.core.diagram.c4

import io.lugf027.github.mermaid.core.diagram.DiagramDB

/**
 * C4 图数据库 - 对标 mermaid-js c4Db.js
 *
 * 支持 C4Context/C4Container/C4Component/C4Deployment 四种图表类型。
 * 存储 Person/System/Container/Component 元素、Boundary 边界、Relationship 关系。
 */
class C4Db : DiagramDB {

    /** C4 元素 */
    data class C4Shape(
        val alias: String,
        val label: String = "",
        val descr: String = "",
        val techn: String = "",
        val sprite: String = "",
        val tags: String = "",
        val link: String = "",
        val typeC4Shape: String = "", // person/system/container/component + external/db/queue 变体
        val parentBoundary: String = "global",
        val wrap: Boolean = false,
        // 渲染后填充
        var x: Double = 0.0,
        var y: Double = 0.0,
        var width: Double = 0.0,
        var height: Double = 0.0,
        // 样式覆盖
        var bgColor: String = "",
        var fontColor: String = "",
        var borderColor: String = ""
    )

    /** 边界 */
    data class Boundary(
        val alias: String,
        val label: String = "",
        val type: String = "",
        val tags: String = "",
        val link: String = "",
        val parentBoundary: String = "",
        val wrap: Boolean = false,
        // 渲染后填充
        var x: Double = 0.0,
        var y: Double = 0.0,
        var width: Double = 0.0,
        var height: Double = 0.0,
        // 样式覆盖
        var bgColor: String = "",
        var fontColor: String = "",
        var borderColor: String = ""
    )

    /** 关系 */
    data class Relationship(
        val type: String = "rel", // rel/birel/rel_u/rel_d/rel_l/rel_r/rel_b
        val from: String = "",
        val to: String = "",
        val label: String = "",
        val techn: String = "",
        val descr: String = "",
        val sprite: String = "",
        val tags: String = "",
        val link: String = "",
        val wrap: Boolean = false,
        // 样式覆盖
        var textColor: String = "",
        var lineColor: String = "",
        var offsetX: Int = 0,
        var offsetY: Int = 0
    )

    // --- 内部状态 ---
    private val c4Shapes = mutableListOf<C4Shape>()
    private val boundaries = mutableListOf<Boundary>()
    private val rels = mutableListOf<Relationship>()
    private val boundaryParseStack = mutableListOf("")
    private var currentBoundaryParse = "global"
    private var parentBoundaryParse = ""
    private var c4Type = "C4Context"
    private var wrapEnabled = false
    private var c4ShapeInRow = 4
    private var c4BoundaryInRow = 2

    // --- DiagramDB ---
    private var diagramTitle = ""
    private var accTitle = ""
    private var accDescription = ""

    init {
        boundaries.add(Boundary(
            alias = "global",
            label = "global",
            type = "global",
            parentBoundary = ""
        ))
    }

    override fun clear() {
        c4Shapes.clear()
        boundaries.clear()
        rels.clear()
        boundaryParseStack.clear()
        boundaryParseStack.add("")
        currentBoundaryParse = "global"
        parentBoundaryParse = ""
        c4Type = "C4Context"
        wrapEnabled = false
        c4ShapeInRow = 4
        c4BoundaryInRow = 2
        diagramTitle = ""
        accTitle = ""
        accDescription = ""
        boundaries.add(Boundary(
            alias = "global",
            label = "global",
            type = "global",
            parentBoundary = ""
        ))
    }

    override fun setDiagramTitle(title: String) { diagramTitle = title }
    override fun getDiagramTitle(): String = diagramTitle
    override fun setAccTitle(title: String) { accTitle = title }
    override fun getAccTitle(): String = accTitle
    override fun setAccDescription(desc: String) { accDescription = desc }
    override fun getAccDescription(): String = accDescription

    // --- C4 操作 ---

    fun setC4Type(type: String) { c4Type = type }
    fun getC4Type(): String = c4Type

    fun setWrap(wrap: Boolean) { wrapEnabled = wrap }
    fun autoWrap(): Boolean = wrapEnabled

    fun getC4ShapeInRow(): Int = c4ShapeInRow
    fun getC4BoundaryInRow(): Int = c4BoundaryInRow

    /**
     * 添加 Person/System 类元素
     */
    fun addPersonOrSystem(
        typeC4Shape: String,
        alias: String,
        label: String,
        descr: String = "",
        sprite: String = "",
        tags: String = "",
        link: String = ""
    ) {
        c4Shapes.add(C4Shape(
            alias = alias,
            label = label,
            descr = descr,
            sprite = sprite,
            tags = tags,
            link = link,
            typeC4Shape = typeC4Shape,
            parentBoundary = currentBoundaryParse,
            wrap = wrapEnabled
        ))
    }

    /**
     * 添加 Container 类元素
     */
    fun addContainer(
        typeC4Shape: String,
        alias: String,
        label: String,
        techn: String = "",
        descr: String = "",
        sprite: String = "",
        tags: String = "",
        link: String = ""
    ) {
        c4Shapes.add(C4Shape(
            alias = alias,
            label = label,
            techn = techn,
            descr = descr,
            sprite = sprite,
            tags = tags,
            link = link,
            typeC4Shape = typeC4Shape,
            parentBoundary = currentBoundaryParse,
            wrap = wrapEnabled
        ))
    }

    /**
     * 添加 Component 类元素
     */
    fun addComponent(
        typeC4Shape: String,
        alias: String,
        label: String,
        techn: String = "",
        descr: String = "",
        sprite: String = "",
        tags: String = "",
        link: String = ""
    ) = addContainer(typeC4Shape, alias, label, techn, descr, sprite, tags, link)

    /**
     * 添加边界
     */
    fun addPersonOrSystemBoundary(
        alias: String,
        label: String,
        type: String = "",
        tags: String = "",
        link: String = ""
    ) {
        parentBoundaryParse = currentBoundaryParse
        currentBoundaryParse = alias
        boundaryParseStack.add(parentBoundaryParse)
        boundaries.add(Boundary(
            alias = alias,
            label = label,
            type = type,
            tags = tags,
            link = link,
            parentBoundary = parentBoundaryParse,
            wrap = wrapEnabled
        ))
    }

    fun addContainerBoundary(
        alias: String,
        label: String,
        type: String = "",
        tags: String = "",
        link: String = ""
    ) = addPersonOrSystemBoundary(alias, label, type, tags, link)

    /**
     * 添加部署节点（作为边界）
     */
    fun addDeploymentNode(
        nodeType: String,
        alias: String,
        label: String,
        type: String = "",
        descr: String = "",
        sprite: String = "",
        tags: String = "",
        link: String = ""
    ) {
        addPersonOrSystemBoundary(alias, label, type.ifEmpty { "node" }, tags, link)
    }

    /**
     * 弹出边界解析栈
     */
    fun popBoundaryParseStack() {
        if (boundaryParseStack.size > 1) {
            boundaryParseStack.removeAt(boundaryParseStack.size - 1)
            currentBoundaryParse = if (boundaryParseStack.isNotEmpty()) {
                val prev = boundaryParseStack.last()
                if (prev.isEmpty()) "global" else prev
            } else "global"
        } else {
            currentBoundaryParse = "global"
        }
    }

    /**
     * 添加关系
     */
    fun addRel(
        type: String,
        from: String,
        to: String,
        label: String = "",
        techn: String = "",
        descr: String = "",
        sprite: String = "",
        tags: String = "",
        link: String = ""
    ) {
        rels.add(Relationship(
            type = type,
            from = from,
            to = to,
            label = label,
            techn = techn,
            descr = descr,
            sprite = sprite,
            tags = tags,
            link = link,
            wrap = wrapEnabled
        ))
    }

    /**
     * 更新元素样式
     */
    fun updateElStyle(
        elementName: String,
        bgColor: String = "",
        fontColor: String = "",
        borderColor: String = ""
    ) {
        c4Shapes.find { it.alias == elementName }?.let {
            if (bgColor.isNotEmpty()) it.bgColor = bgColor
            if (fontColor.isNotEmpty()) it.fontColor = fontColor
            if (borderColor.isNotEmpty()) it.borderColor = borderColor
        }
    }

    /**
     * 更新关系样式
     */
    fun updateRelStyle(
        from: String,
        to: String,
        textColor: String = "",
        lineColor: String = "",
        offsetX: String = "",
        offsetY: String = ""
    ) {
        rels.find { it.from == from && it.to == to }?.let {
            if (textColor.isNotEmpty()) it.textColor = textColor
            if (lineColor.isNotEmpty()) it.lineColor = lineColor
            if (offsetX.isNotEmpty()) it.offsetX = offsetX.toIntOrNull() ?: 0
            if (offsetY.isNotEmpty()) it.offsetY = offsetY.toIntOrNull() ?: 0
        }
    }

    /**
     * 更新布局配置
     */
    fun updateLayoutConfig(shapeInRow: String = "", boundaryInRow: String = "") {
        if (shapeInRow.isNotEmpty()) c4ShapeInRow = shapeInRow.toIntOrNull() ?: 4
        if (boundaryInRow.isNotEmpty()) c4BoundaryInRow = boundaryInRow.toIntOrNull() ?: 2
    }

    // --- 查询 ---

    fun getC4ShapeArray(parentBoundary: String? = null): List<C4Shape> {
        return if (parentBoundary != null) {
            c4Shapes.filter { it.parentBoundary == parentBoundary }
        } else c4Shapes.toList()
    }

    fun getC4Shape(alias: String): C4Shape? = c4Shapes.find { it.alias == alias }

    fun getBoundaries(parentBoundary: String? = null): List<Boundary> {
        return if (parentBoundary != null) {
            boundaries.filter { it.parentBoundary == parentBoundary }
        } else boundaries.toList()
    }

    fun getRels(): List<Relationship> = rels.toList()

    fun setTitle(title: String) { diagramTitle = title }
    fun getTitle(): String = diagramTitle
}
