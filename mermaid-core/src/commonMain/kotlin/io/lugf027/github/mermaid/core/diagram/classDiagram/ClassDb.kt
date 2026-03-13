package io.lugf027.github.mermaid.core.diagram.classDiagram

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.layout.LayoutData
import io.lugf027.github.mermaid.core.layout.LayoutEdge
import io.lugf027.github.mermaid.core.layout.LayoutNode
import io.lugf027.github.mermaid.core.util.Logger

/**
 * 类图数据库 - 对标 mermaid-js classDb.ts
 *
 * 管理类(ClassNode)、关系(ClassRelation)、命名空间和注释。
 */
class ClassDb : DiagramDB {

    private val log = Logger("ClassDb")

    // ── 状态 ──────────────────────────────────────────
    private val classes: LinkedHashMap<String, ClassNode> = linkedMapOf()
    private val relations: MutableList<ClassRelation> = mutableListOf()
    private val notes: MutableList<ClassNote> = mutableListOf()
    private val namespaces: LinkedHashMap<String, NamespaceNode> = linkedMapOf()
    private val styleClasses: LinkedHashMap<String, ClassStyleDef> = linkedMapOf()
    private var nodeCount = 0

    // ── DiagramDB 基础接口 ────────────────────────────
    private var diagramTitle: String = ""
    private var accTitle: String = ""
    private var accDescription: String = ""
    private var direction: String = "TB"

    override fun clear() {
        classes.clear()
        relations.clear()
        notes.clear()
        namespaces.clear()
        styleClasses.clear()
        nodeCount = 0
        diagramTitle = ""
        accTitle = ""
        accDescription = ""
        direction = "TB"
    }

    override fun setDiagramTitle(title: String) { diagramTitle = title }
    override fun getDiagramTitle(): String = diagramTitle
    override fun setAccTitle(title: String) { accTitle = title }
    override fun getAccTitle(): String = accTitle
    override fun setAccDescription(desc: String) { accDescription = desc }
    override fun getAccDescription(): String = accDescription
    override fun getDirection(): String = direction
    override fun setDirection(direction: String) { this.direction = direction }

    // ── 类管理 ──────────────────────────────────────

    fun addClass(id: String, label: String? = null, genericType: String? = null) {
        if (!classes.containsKey(id)) {
            val displayLabel = label ?: id
            val text = if (genericType != null) "$displayLabel&lt;$genericType&gt;" else displayLabel
            classes[id] = ClassNode(
                id = id,
                label = displayLabel,
                type = genericType ?: "",
                text = text,
                shape = "classBox",
                cssClasses = "default",
                members = mutableListOf(),
                methods = mutableListOf(),
                annotations = mutableListOf(),
                domId = "classId-$id-${nodeCount++}"
            )
            log.debug("Added class: $id")
        }
    }

    fun addMember(className: String, memberText: String) {
        addClass(className)
        val node = classes[className] ?: return
        val trimmed = memberText.trim()
        if (trimmed.isEmpty()) return

        // 注解: <<interface>> 等
        if (trimmed.startsWith("<<") && trimmed.endsWith(">>")) {
            node.annotations.add(trimmed.removePrefix("<<").removeSuffix(">>").trim())
            return
        }

        val member = parseMember(trimmed)
        if (member.memberType == "method") {
            node.methods.add(member)
        } else {
            node.members.add(member)
        }
    }

    fun addAnnotation(className: String, annotation: String) {
        addClass(className)
        classes[className]?.annotations?.add(annotation)
    }

    fun getClasses(): Map<String, ClassNode> = classes

    // ── 关系管理 ──────────────────────────────────────

    fun addRelation(relation: ClassRelation) {
        addClass(relation.id1)
        addClass(relation.id2)
        relations.add(relation)
        log.debug("Added relation: ${relation.id1} -> ${relation.id2}")
    }

    fun getRelations(): List<ClassRelation> = relations

    // ── 注释管理 ──────────────────────────────────────

    fun addNote(text: String, forClass: String? = null) {
        val noteId = "note${notes.size}"
        notes.add(ClassNote(id = noteId, text = text, forClass = forClass))
    }

    fun getNotes(): List<ClassNote> = notes

    // ── 命名空间管理 ──────────────────────────────────

    fun addNamespace(id: String) {
        if (!namespaces.containsKey(id)) {
            namespaces[id] = NamespaceNode(id = id, classes = mutableListOf())
        }
    }

    fun addClassToNamespace(namespaceId: String, classId: String) {
        addNamespace(namespaceId)
        namespaces[namespaceId]?.classes?.add(classId)
        classes[classId]?.parent = namespaceId
    }

    // ── 样式管理 ──────────────────────────────────────

    fun addStyleClass(id: String, styles: List<String>) {
        styleClasses[id] = ClassStyleDef(id = id, styles = styles)
    }

    fun applyStyleClass(classIds: List<String>, styleId: String) {
        for (classId in classIds) {
            classes[classId]?.let { node ->
                node.cssClasses = if (node.cssClasses.isEmpty() || node.cssClasses == "default") {
                    styleId
                } else {
                    "${node.cssClasses} $styleId"
                }
            }
        }
    }

    // ── LayoutData 构建 ──────────────────────────────

    fun getData(config: MermaidConfig): LayoutData {
        val classConfig = config.`class`
        val nodeSpacing = classConfig?.nodeSpacing ?: 50
        val rankSpacing = classConfig?.rankSpacing ?: 50
        val padding = classConfig?.diagramPadding ?: 8

        val nodes = mutableListOf<LayoutNode>()
        val edges = mutableListOf<LayoutEdge>()

        // 命名空间 → group 节点
        for ((nsId, ns) in namespaces) {
            nodes.add(LayoutNode(
                id = nsId,
                label = nsId,
                shape = "rect",
                isGroup = true,
                padding = (classConfig?.padding ?: 16).toDouble(),
                domId = "ns-$nsId",
                cssClasses = "default",
                look = config.look
            ))
        }

        // 类 → 节点
        for ((classId, classNode) in classes) {
            nodes.add(LayoutNode(
                id = classId,
                label = classNode.text,
                shape = "classBox",
                isGroup = false,
                parentId = classNode.parent,
                domId = classNode.domId,
                cssClasses = classNode.cssClasses,
                look = config.look
            ))
        }

        // 注释 → 节点 + 边
        for ((idx, note) in notes.withIndex()) {
            nodes.add(LayoutNode(
                id = note.id,
                label = note.text,
                shape = "note",
                isGroup = false,
                padding = 6.0,
                domId = "note-$idx",
                cssClasses = "default"
            ))
            if (note.forClass != null) {
                edges.add(LayoutEdge(
                    id = "edgeNote$idx",
                    start = note.id,
                    end = note.forClass,
                    arrowTypeStart = null,
                    arrowTypeEnd = null,
                    pattern = "dotted",
                    stroke = "normal",
                    thickness = "normal"
                ))
            }
        }

        // 关系 → 边
        for ((idx, rel) in relations.withIndex()) {
            edges.add(LayoutEdge(
                id = "edge$idx",
                start = rel.id1,
                end = rel.id2,
                label = rel.title,
                arrowTypeStart = getArrowMarker(rel.relation.type1),
                arrowTypeEnd = getArrowMarker(rel.relation.type2),
                pattern = if (rel.relation.lineType == LineType.DOTTED_LINE) "dashed" else "solid",
                stroke = "normal",
                thickness = "normal",
                labelpos = "c"
            ))
        }

        return LayoutData(
            nodes = nodes,
            edges = edges,
            config = config,
            direction = direction,
            nodeSpacing = nodeSpacing,
            rankSpacing = rankSpacing,
            diagramPadding = padding,
            markers = listOf("aggregation", "extension", "composition", "dependency", "lollipop")
        )
    }

    // ── 成员解析 ──────────────────────────────────────

    private fun parseMember(text: String): ClassMember {
        // 判断是否为方法：包含 '(' 和 ')'
        return if (text.contains("(") && text.contains(")")) {
            parseMethod(text)
        } else {
            parseAttribute(text)
        }
    }

    private fun parseMethod(text: String): ClassMember {
        // 正则: ([#+~-])? (.+) \( (.*) \) ([\s$*])? (.*) ([$*])?
        val regex = Regex("""^([#+~-])?(.+?)\((.*?)\)\s*([$$*])?\s*(.*?)\s*([$$*])?$""")
        val match = regex.matchEntire(text)
        return if (match != null) {
            val visibility = match.groupValues[1]
            val methodName = match.groupValues[2].trim()
            val parameters = match.groupValues[3]
            val classifier1 = match.groupValues[4]
            val returnType = match.groupValues[5].trim()
            val classifier2 = match.groupValues[6]
            val classifier = classifier1.ifEmpty { classifier2 }
            ClassMember(
                id = methodName,
                text = text,
                memberType = "method",
                visibility = visibility,
                classifier = classifier,
                parameters = parameters,
                returnType = returnType,
                cssStyle = classifierToCss(classifier)
            )
        } else {
            ClassMember(id = text, text = text, memberType = "method")
        }
    }

    private fun parseAttribute(text: String): ClassMember {
        var visibility = ""
        var classifier = ""
        var trimmed = text

        // 首字符可见性
        if (trimmed.isNotEmpty() && trimmed[0] in setOf('+', '-', '#', '~')) {
            visibility = trimmed[0].toString()
            trimmed = trimmed.substring(1).trim()
        }

        // 尾字符分类符
        if (trimmed.isNotEmpty() && trimmed.last() in setOf('$', '*')) {
            classifier = trimmed.last().toString()
            trimmed = trimmed.dropLast(1).trim()
        }

        // 提取属性名：如果包含空格（"Type name" 格式），取最后一个词作为 id
        val parts = trimmed.split("\\s+".toRegex())
        val attrId = if (parts.size > 1) parts.last() else trimmed

        return ClassMember(
            id = attrId,
            text = text,
            memberType = "attribute",
            visibility = visibility,
            classifier = classifier,
            cssStyle = classifierToCss(classifier)
        )
    }

    private fun classifierToCss(classifier: String): String = when (classifier) {
        "$" -> "text-decoration:underline;"
        "*" -> "font-style:italic;"
        else -> ""
    }

    private fun getArrowMarker(type: Int): String = when (type) {
        RelationType.AGGREGATION -> "aggregation"
        RelationType.EXTENSION -> "extension"
        RelationType.COMPOSITION -> "composition"
        RelationType.DEPENDENCY -> "dependency"
        RelationType.LOLLIPOP -> "lollipop"
        else -> "none"
    }
}

// ════════════════════════════════════════════════════════
//  数据模型
// ════════════════════════════════════════════════════════

/**
 * 类节点数据类 - 对标 mermaid-js ClassNode
 */
data class ClassNode(
    val id: String,
    var label: String = "",
    var type: String = "",           // 泛型参数
    var text: String = "",           // 显示文本
    var shape: String = "classBox",
    var cssClasses: String = "default",
    val members: MutableList<ClassMember> = mutableListOf(),
    val methods: MutableList<ClassMember> = mutableListOf(),
    val annotations: MutableList<String> = mutableListOf(),
    var domId: String = "",
    var parent: String? = null,      // 所属命名空间
    var link: String? = null,
    var linkTarget: String? = null,
    var tooltip: String? = null,
)

/**
 * 类成员数据类 - 对标 mermaid-js ClassMember
 */
data class ClassMember(
    val id: String,
    val text: String = "",
    val memberType: String = "attribute", // "method" or "attribute"
    val visibility: String = "",          // "+", "-", "#", "~", ""
    val classifier: String = "",          // "$"=static, "*"=abstract
    val parameters: String = "",          // 方法参数
    val returnType: String = "",          // 返回类型
    val cssStyle: String = "",
)

/**
 * 类关系数据类 - 对标 mermaid-js ClassRelation
 */
data class ClassRelation(
    val id1: String,
    val id2: String,
    val relation: RelationSpec,
    val title: String = "",
    val relationTitle1: String = "",  // 源端基数
    val relationTitle2: String = "",  // 目标端基数
)

/**
 * 关系规格 - 对标 mermaid-js relation
 */
data class RelationSpec(
    val type1: Int,      // 源端关系类型 (RelationType)
    val type2: Int,      // 目标端关系类型
    val lineType: Int,   // 线型 (LineType)
)

/**
 * 关系类型枚举
 */
object RelationType {
    const val AGGREGATION = 0
    const val EXTENSION = 1
    const val COMPOSITION = 2
    const val DEPENDENCY = 3
    const val LOLLIPOP = 4
    const val NONE = -1
}

/**
 * 线型枚举
 */
object LineType {
    const val LINE = 0
    const val DOTTED_LINE = 1
}

/**
 * 类注释数据类
 */
data class ClassNote(
    val id: String,
    val text: String,
    val forClass: String? = null,
)

/**
 * 命名空间数据类
 */
data class NamespaceNode(
    val id: String,
    val classes: MutableList<String> = mutableListOf(),
)

/**
 * 样式定义
 */
data class ClassStyleDef(
    val id: String,
    val styles: List<String> = emptyList(),
)
