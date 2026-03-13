package io.lugf027.github.mermaid.core.diagram.pie

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.config.PieDiagramConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.util.Logger

/**
 * 饼图数据库 - 对标 mermaid-js pieDb.ts
 *
 * 存储饼图解析结果：扇区数据（标签→数值映射）、showData 标志、标题等。
 */
class PieDb : DiagramDB {

    private val log = Logger("PieDb")

    /** 扇区数据：标签 → 数值，使用 LinkedHashMap 保持插入顺序 */
    private val sections: LinkedHashMap<String, Double> = linkedMapOf()

    /** 是否显示原始数据值 */
    private var showData: Boolean = false

    /** 图表标题 */
    private var diagramTitle: String = ""

    /** 无障碍标题 */
    private var accTitle: String = ""

    /** 无障碍描述 */
    private var accDescription: String = ""

    /** 配置引用 */
    private var config: PieDiagramConfig = PieDiagramConfig()

    override fun clear() {
        sections.clear()
        showData = false
        diagramTitle = ""
        accTitle = ""
        accDescription = ""
        config = PieDiagramConfig()
    }

    override fun setDiagramTitle(title: String) {
        diagramTitle = title
    }

    override fun getDiagramTitle(): String = diagramTitle

    override fun setAccTitle(title: String) {
        accTitle = title
    }

    override fun getAccTitle(): String = accTitle

    override fun setAccDescription(desc: String) {
        accDescription = desc
    }

    override fun getAccDescription(): String = accDescription

    /**
     * 添加一个扇区
     *
     * 对标 mermaid-js pieDb.ts 的 addSection：
     * - 相同标签不会重复添加
     * - 负值会抛出异常
     *
     * @param label 扇区标签
     * @param value 扇区数值
     */
    fun addSection(label: String, value: Double) {
        if (value < 0) {
            log.error("Pie chart section value cannot be negative: '$label' = $value")
            throw IllegalArgumentException("Pie chart section value cannot be negative")
        }
        if (sections.containsKey(label)) {
            log.warn("Duplicate section label ignored: '$label'")
            return
        }
        sections[label] = value
        log.debug("Added section: '$label' = $value")
    }

    /** 获取所有扇区数据 */
    fun getSections(): Map<String, Double> = sections.toMap()

    /** 设置是否显示数据值 */
    fun setShowData(show: Boolean) {
        showData = show
    }

    /** 获取是否显示数据值 */
    fun getShowData(): Boolean = showData

    /** 获取饼图配置 */
    fun getPieConfig(): PieDiagramConfig = config

    /** 设置饼图配置 */
    fun setPieConfig(cfg: PieDiagramConfig) {
        config = cfg
    }
}
