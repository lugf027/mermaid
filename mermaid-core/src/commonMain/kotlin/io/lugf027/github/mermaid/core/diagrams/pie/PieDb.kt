package io.lugf027.github.mermaid.core.diagrams.pie

import io.lugf027.github.mermaid.core.db.CommonDb

/**
 * 饼图数据存储层。
 * 管理 sections(Map<String,Double>)、showData 等数据。
 * 对应 mermaid-js 的 pieDb.ts。
 */
class PieDb : CommonDb() {
    /** 饼图数据段：标签 → 值 */
    private val sections = mutableMapOf<String, Double>()

    /** 是否在图例中显示原始数值 */
    private var showData: Boolean = false

    /** 文本标签径向位置 (0=中心, 1=边缘)，默认 0.75 */
    var textPosition: Float = 0.75f

    /**
     * 添加数据段。
     * 负值将被忽略（mermaid-js 抛异常，这里静默忽略）。
     * 重复标签仅保留第一次出现的。
     */
    fun addSection(label: String, value: Double) {
        if (value < 0) return
        if (label !in sections) {
            sections[label] = value
        }
    }

    /** 获取所有数据段 */
    fun getSections(): Map<String, Double> = sections.toMap()

    /** 设置是否显示数值 */
    fun setShowData(show: Boolean) {
        showData = show
    }

    /** 获取是否显示数值 */
    fun getShowData(): Boolean = showData

    override fun clear() {
        super.clear()
        sections.clear()
        showData = false
        textPosition = 0.75f
    }
}
