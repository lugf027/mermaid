package io.lugf027.github.mermaid.core.diagram.venn

import io.lugf027.github.mermaid.core.diagram.DiagramDB

/**
 * 韦恩图数据库 - 对标 mermaid-js vennDB.ts
 *
 * 存储集合(set)、交集(union)、文本节点和样式。
 */
class VennDb : DiagramDB {

    /** 集合/交集数据 */
    data class VennData(
        val sets: List<String>,  // 单元素=集合，多元素=交集
        val size: Double,
        val label: String = ""
    )

    /** 文本节点 */
    data class VennTextData(
        val sets: List<String>,
        val id: String,
        val label: String = ""
    )

    /** 样式 */
    data class VennStyleData(
        val targets: List<String>,
        val styles: Map<String, String>
    )

    // --- 内部状态 ---
    private val subsets = mutableListOf<VennData>()
    private val textNodes = mutableListOf<VennTextData>()
    private val styleEntries = mutableListOf<VennStyleData>()
    private val knownSets = mutableSetOf<String>()

    // --- DiagramDB ---
    private var diagramTitle = ""
    private var accTitle = ""
    private var accDescription = ""

    override fun clear() {
        subsets.clear()
        textNodes.clear()
        styleEntries.clear()
        knownSets.clear()
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

    fun addSet(id: String, label: String, size: Double) {
        knownSets.add(id)
        subsets.add(VennData(listOf(id), size, label))
    }

    fun addUnion(setIds: List<String>, label: String, size: Double) {
        subsets.add(VennData(setIds, size, label))
    }

    fun addTextNode(sets: List<String>, id: String, label: String) {
        textNodes.add(VennTextData(sets, id, label))
    }

    fun addStyle(targets: List<String>, styles: Map<String, String>) {
        styleEntries.add(VennStyleData(targets, styles))
    }

    // --- 查询 ---

    fun getSubsets(): List<VennData> = subsets.toList()
    fun getSets(): List<VennData> = subsets.filter { it.sets.size == 1 }
    fun getUnions(): List<VennData> = subsets.filter { it.sets.size > 1 }
    fun getTextNodes(): List<VennTextData> = textNodes.toList()
    fun getStyleEntries(): List<VennStyleData> = styleEntries.toList()
    fun getKnownSets(): Set<String> = knownSets.toSet()
}
