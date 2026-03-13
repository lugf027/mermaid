package io.lugf027.github.mermaid.core.diagram.requirement

import io.lugf027.github.mermaid.core.diagram.DiagramDB

/**
 * 需求图数据库 - 对标 mermaid-js requirementDb.ts
 *
 * 存储需求节点（Requirement）、元素节点（Element）和关系（Relation）。
 * 6种需求类型、4种验证方法、3种风险级别、7种关系类型。
 */
class RequirementDb : DiagramDB {

    /** 需求类型 */
    enum class RequirementType(val displayName: String) {
        REQUIREMENT("Requirement"),
        FUNCTIONAL("Functional Requirement"),
        INTERFACE("Interface Requirement"),
        PERFORMANCE("Performance Requirement"),
        PHYSICAL("Physical Requirement"),
        DESIGN_CONSTRAINT("Design Constraint")
    }

    /** 风险级别 */
    enum class RiskLevel { LOW, MEDIUM, HIGH }

    /** 验证方法 */
    enum class VerifyMethod { ANALYSIS, DEMONSTRATION, INSPECTION, TEST }

    /** 关系类型 */
    enum class RelationType(val label: String) {
        CONTAINS("contains"),
        COPIES("copies"),
        DERIVES("derives"),
        SATISFIES("satisfies"),
        VERIFIES("verifies"),
        REFINES("refines"),
        TRACES("traces")
    }

    /** 需求节点 */
    data class Requirement(
        val name: String,
        val type: RequirementType,
        var requirementId: String = "",
        var text: String = "",
        var risk: RiskLevel? = null,
        var verifyMethod: VerifyMethod? = null
    )

    /** 元素节点 */
    data class Element(
        val name: String,
        var type: String = "",
        var docRef: String = ""
    )

    /** 关系 */
    data class Relation(
        val type: RelationType,
        val src: String,
        val dst: String
    )

    // --- 内部状态 ---
    private val requirements = mutableMapOf<String, Requirement>()
    private val elements = mutableMapOf<String, Element>()
    private val relations = mutableListOf<Relation>()
    private var direction = "TB"

    // 解析中间状态
    var latestRequirement: Requirement? = null
    var latestElement: Element? = null

    // --- DiagramDB ---
    private var diagramTitle = ""
    private var accTitle = ""
    private var accDescription = ""

    override fun clear() {
        requirements.clear()
        elements.clear()
        relations.clear()
        direction = "TB"
        latestRequirement = null
        latestElement = null
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

    fun addRequirement(name: String, type: RequirementType) {
        val req = Requirement(name, type)
        requirements[name] = req
        latestRequirement = req
        latestElement = null
    }

    fun addElement(name: String) {
        val elem = Element(name)
        elements[name] = elem
        latestElement = elem
        latestRequirement = null
    }

    fun addRelation(type: RelationType, src: String, dst: String) {
        relations.add(Relation(type, src, dst))
    }

    override fun setDirection(dir: String) {
        direction = dir.uppercase()
    }

    // --- 查询 ---

    fun getRequirements(): Map<String, Requirement> = requirements.toMap()
    fun getElements(): Map<String, Element> = elements.toMap()
    fun getRelations(): List<Relation> = relations.toList()
    override fun getDirection(): String = direction
}
