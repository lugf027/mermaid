package io.lugf027.github.mermaid.core.diagrams.classdiagram

/**
 * 类图成员可见性。
 */
enum class Visibility(val symbol: String) {
    PUBLIC("+"),
    PRIVATE("-"),
    PROTECTED("#"),
    PACKAGE("~"),
    NONE(""),
    ;

    companion object {
        fun fromChar(c: Char): Visibility = when (c) {
            '+' -> PUBLIC; '-' -> PRIVATE; '#' -> PROTECTED; '~' -> PACKAGE; else -> NONE
        }
    }
}

/**
 * 类成员（属性或方法）。
 */
data class ClassMember(
    val id: String,
    val memberType: MemberType,
    var visibility: Visibility = Visibility.NONE,
    var text: String = "",
    var classifier: String = "",   // $ = static, * = abstract
    var parameters: String = "",
    var returnType: String = "",
)

enum class MemberType { ATTRIBUTE, METHOD }

/**
 * 类节点。
 */
data class ClassNode(
    val id: String,
    var label: String = id,
    var type: String = "",                       // 泛型
    val members: MutableList<ClassMember> = mutableListOf(),
    val methods: MutableList<ClassMember> = mutableListOf(),
    val annotations: MutableList<String> = mutableListOf(),
    val styles: MutableList<String> = mutableListOf(),
    var parent: String? = null,                  // 所属命名空间
    var link: String? = null,
    var tooltip: String? = null,
)

/**
 * 关系类型。
 */
enum class RelationType(val value: Int) {
    AGGREGATION(0),    // o
    EXTENSION(1),      // <|
    COMPOSITION(2),    // *
    DEPENDENCY(3),     // >
    LOLLIPOP(4),       // ()
}

/**
 * 线型。
 */
enum class ClassLineType(val value: Int) {
    LINE(0),          // -- 实线
    DOTTED_LINE(1),   // .. 虚线
}

/**
 * 类关系。
 */
data class ClassRelation(
    val id1: String,
    val id2: String,
    var relationTitle1: String = "",  // 左侧基数标签
    var relationTitle2: String = "",  // 右侧基数标签
    var title: String = "",          // 关系上的标签
    var relation: RelationDetail = RelationDetail(),
)

data class RelationDetail(
    var type1: RelationType? = null,
    var type2: RelationType? = null,
    var lineType: ClassLineType = ClassLineType.LINE,
)

/**
 * 命名空间。
 */
data class NamespaceNode(
    val id: String,
    val classes: MutableMap<String, ClassNode> = mutableMapOf(),
)
