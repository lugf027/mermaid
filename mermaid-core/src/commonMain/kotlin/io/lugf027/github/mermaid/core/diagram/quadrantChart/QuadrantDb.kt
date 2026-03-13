package io.lugf027.github.mermaid.core.diagram.quadrantChart

import io.lugf027.github.mermaid.core.diagram.DiagramDB

/**
 * 象限图数据库 - 对标 mermaid-js quadrantDb.ts + quadrantBuilder.ts
 *
 * 存储四个象限标签、X/Y轴标签和数据点（坐标归一化到 0~1）。
 */
class QuadrantDb : DiagramDB {

    /** 数据点 */
    data class Point(
        val x: Double,     // 0~1
        val y: Double,     // 0~1
        val text: String,
        val className: String = "",
        val radius: Double = 5.0,
        val color: String = "",
        val strokeColor: String = "",
        val strokeWidth: String = ""
    )

    /** classDef 样式 */
    data class ClassDef(
        val name: String,
        val radius: Double? = null,
        val color: String? = null,
        val strokeColor: String? = null,
        val strokeWidth: String? = null
    )

    // --- 内部状态 ---
    var quadrant1Text = ""  // 右上
    var quadrant2Text = ""  // 左上
    var quadrant3Text = ""  // 左下
    var quadrant4Text = ""  // 右下
    var xAxisLeftText = ""
    var xAxisRightText = ""
    var yAxisBottomText = ""
    var yAxisTopText = ""
    private val points = mutableListOf<Point>()
    private val classDefs = mutableMapOf<String, ClassDef>()

    // --- DiagramDB ---
    private var diagramTitle = ""
    private var accTitle = ""
    private var accDescription = ""

    override fun clear() {
        quadrant1Text = ""
        quadrant2Text = ""
        quadrant3Text = ""
        quadrant4Text = ""
        xAxisLeftText = ""
        xAxisRightText = ""
        yAxisBottomText = ""
        yAxisTopText = ""
        points.clear()
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

    fun addPoint(x: Double, y: Double, text: String, className: String = "") {
        val cd = classDefs[className]
        points.add(Point(
            x = x.coerceIn(0.0, 1.0),
            y = y.coerceIn(0.0, 1.0),
            text = text,
            className = className,
            radius = cd?.radius ?: 5.0,
            color = cd?.color ?: "",
            strokeColor = cd?.strokeColor ?: "",
            strokeWidth = cd?.strokeWidth ?: ""
        ))
    }

    fun addClassDef(name: String, styles: Map<String, String>) {
        classDefs[name] = ClassDef(
            name = name,
            radius = styles["radius"]?.toDoubleOrNull(),
            color = styles["color"],
            strokeColor = styles["stroke-color"],
            strokeWidth = styles["stroke-width"]
        )
    }

    // --- 查询 ---

    fun getPoints(): List<Point> = points.toList()
    fun getClassDefs(): Map<String, ClassDef> = classDefs.toMap()
}
