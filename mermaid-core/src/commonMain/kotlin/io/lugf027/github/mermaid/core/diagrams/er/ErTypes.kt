package io.lugf027.github.mermaid.core.diagrams.er

/**
 * ER 实体关系图类型定义。
 * 对应 mermaid-js erTypes.ts。
 */

/** 基数（Cardinality） */
enum class Cardinality(val symbol: String) {
    ZERO_OR_ONE("o|"),
    ZERO_OR_MORE("o{"),
    ONE_OR_MORE("|{"),
    ONLY_ONE("||"),
    MD_PARENT("}o");

    companion object {
        fun fromSymbol(s: String): Cardinality = entries.firstOrNull { it.symbol == s } ?: ONLY_ONE
    }
}

/** 关系标识类型 */
enum class Identification {
    NON_IDENTIFYING,  // 虚线
    IDENTIFYING        // 实线
}

/** 实体属性 */
data class ErAttribute(
    val type: String,
    val name: String,
    val keys: List<String> = emptyList(),  // PK, FK, UK
    val comment: String = "",
)

/** 实体节点 */
data class ErEntity(
    val id: String,
    val label: String = id,
    val alias: String? = null,
    val attributes: MutableList<ErAttribute> = mutableListOf(),
)

/** 实体间关系 */
data class ErRelationship(
    val entityA: String,
    val entityB: String,
    val roleLabel: String,
    val cardA: Cardinality,
    val cardB: Cardinality,
    val identification: Identification = Identification.NON_IDENTIFYING,
)
