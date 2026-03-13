package io.lugf027.github.mermaid.core.diagram.radar

import io.lugf027.github.mermaid.core.diagram.DiagramDB

/**
 * 雷达图数据库 - 对标 mermaid-js radarDb.ts
 *
 * 存储轴线标签（axes）和多个数据集（datasets），
 * 每个数据集对应一个多边形区域。
 */
class RadarDb : DiagramDB {

    /** 数据集 */
    data class Dataset(
        val name: String,
        val values: List<Double>
    )

    // --- 内部状态 ---
    private val axes = mutableListOf<String>()
    private val datasets = mutableListOf<Dataset>()
    private var showLegend = true
    private var maxValue: Double? = null

    // --- DiagramDB ---
    private var diagramTitle = ""
    private var accTitle = ""
    private var accDescription = ""

    override fun clear() {
        axes.clear()
        datasets.clear()
        showLegend = true
        maxValue = null
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

    fun setAxes(axisLabels: List<String>) {
        axes.clear()
        axes.addAll(axisLabels)
    }

    fun addDataset(name: String, values: List<Double>) {
        datasets.add(Dataset(name, values))
    }

    fun setShowLegend(show: Boolean) { showLegend = show }
    fun setMaxValue(v: Double) { maxValue = v }

    // --- 查询 ---

    fun getAxes(): List<String> = axes.toList()
    fun getDatasets(): List<Dataset> = datasets.toList()
    fun getShowLegend(): Boolean = showLegend

    /**
     * 获取有效最大值：用户设定值或数据中最大值
     */
    fun getEffectiveMaxValue(): Double {
        return maxValue ?: datasets.flatMap { it.values }.maxOrNull() ?: 100.0
    }

    /**
     * 获取轴数量
     */
    fun getAxisCount(): Int = axes.size
}
