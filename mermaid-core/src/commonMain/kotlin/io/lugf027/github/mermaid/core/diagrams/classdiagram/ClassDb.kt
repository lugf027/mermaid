package io.lugf027.github.mermaid.core.diagrams.classdiagram

import io.lugf027.github.mermaid.core.db.CommonDb
import io.lugf027.github.mermaid.core.types.*

/**
 * 类图数据存储层。
 * 管理 classes、relations、namespaces。
 * 对应 mermaid-js classDb.ts。
 */
class ClassDb : CommonDb() {

    private val classes = mutableMapOf<String, ClassNode>()
    private val relations = mutableListOf<ClassRelation>()
    private val namespaces = mutableMapOf<String, NamespaceNode>()
    private var direction: String = "TB"

    fun addClass(id: String, label: String? = null) {
        if (id !in classes) {
            classes[id] = ClassNode(id = id, label = label ?: id)
        } else if (label != null) {
            classes[id]?.label = label
        }
    }

    fun addMember(classId: String, memberText: String) {
        val cls = classes[classId] ?: run { addClass(classId); classes[classId]!! }
        val member = parseMember(memberText)
        if (member.memberType == MemberType.METHOD) {
            cls.methods.add(member)
        } else {
            cls.members.add(member)
        }
    }

    fun addAnnotation(classId: String, annotation: String) {
        val cls = classes[classId] ?: run { addClass(classId); classes[classId]!! }
        cls.annotations.add(annotation)
    }

    fun addRelation(relation: ClassRelation) {
        // 确保两端类存在
        addClass(relation.id1)
        addClass(relation.id2)
        relations.add(relation)
    }

    fun addNamespace(id: String) {
        if (id !in namespaces) {
            namespaces[id] = NamespaceNode(id)
        }
    }

    fun addClassToNamespace(nsId: String, classId: String) {
        val ns = namespaces[nsId] ?: return
        val cls = classes[classId] ?: return
        cls.parent = nsId
        ns.classes[classId] = cls
    }

    fun setDirection(dir: String) { direction = dir.uppercase() }
    fun getDirection(): String = direction
    fun getClasses(): Map<String, ClassNode> = classes.toMap()
    fun getRelations(): List<ClassRelation> = relations.toList()
    fun getNamespaces(): Map<String, NamespaceNode> = namespaces.toMap()

    /**
     * 转换为通用 LayoutData。
     */
    fun getData(): LayoutData {
        val nodes = mutableListOf<Node>()
        val edges = mutableListOf<Edge>()

        // 命名空间作为 group
        for ((_, ns) in namespaces) {
            nodes.add(Node(id = ns.id, label = ns.id, shape = ShapeId.ROUNDED_RECT, isGroup = true))
        }

        // 类作为节点
        for ((_, cls) in classes) {
            val label = buildClassLabel(cls)
            nodes.add(Node(
                id = cls.id,
                label = label,
                shape = ShapeId.CLASS_BOX,
                parentId = cls.parent,
            ))
        }

        // 关系作为边
        for (rel in relations) {
            val strokeType = if (rel.relation.lineType == ClassLineType.DOTTED_LINE) StrokeType.DOTTED else StrokeType.NORMAL
            val arrowEnd = when (rel.relation.type2) {
                RelationType.EXTENSION -> EdgeType.ARROW_POINT
                RelationType.DEPENDENCY -> EdgeType.ARROW_OPEN
                else -> EdgeType.ARROW_NONE
            }
            edges.add(Edge(
                start = rel.id1,
                end = rel.id2,
                label = rel.title,
                stroke = strokeType,
                arrowTypeEnd = arrowEnd,
            ))
        }

        val dir = when (direction) { "BT" -> Direction.BT; "LR" -> Direction.LR; "RL" -> Direction.RL; else -> Direction.TB }
        return LayoutData(nodes = nodes, edges = edges, direction = dir)
    }

    private fun buildClassLabel(cls: ClassNode): String {
        val sb = StringBuilder()
        if (cls.annotations.isNotEmpty()) sb.appendLine("<<${cls.annotations.joinToString(", ")}>>")
        sb.append(cls.label)
        if (cls.type.isNotEmpty()) sb.append("~${cls.type}~")
        if (cls.members.isNotEmpty() || cls.methods.isNotEmpty()) {
            sb.appendLine()
            for (m in cls.members) sb.appendLine("${m.visibility.symbol}${m.text}")
            if (cls.members.isNotEmpty() && cls.methods.isNotEmpty()) sb.appendLine("---")
            for (m in cls.methods) sb.appendLine("${m.visibility.symbol}${m.text}(${m.parameters})${if (m.returnType.isNotEmpty()) " : ${m.returnType}" else ""}")
        }
        return sb.toString().trimEnd()
    }

    private fun parseMember(text: String): ClassMember {
        var t = text.trim()
        var visibility = Visibility.NONE
        var classifier = ""

        // 提取可见性
        if (t.isNotEmpty() && t[0] in "+-#~") {
            visibility = Visibility.fromChar(t[0])
            t = t.substring(1).trim()
        }

        // 提取修饰符 $ (static) 或 * (abstract)
        if (t.endsWith("$") || t.endsWith("*")) {
            classifier = t.last().toString()
            t = t.dropLast(1).trim()
        }

        // 判断是否是方法
        val isMethod = t.contains("(")
        if (isMethod) {
            val parenIdx = t.indexOf('(')
            val name = t.substring(0, parenIdx).trim()
            val rest = t.substring(parenIdx + 1)
            val closeIdx = rest.indexOf(')')
            val params = if (closeIdx >= 0) rest.substring(0, closeIdx) else rest
            var returnType = ""
            if (closeIdx >= 0 && closeIdx + 1 < rest.length) {
                val afterClose = rest.substring(closeIdx + 1).trim()
                if (afterClose.startsWith(":")) returnType = afterClose.substring(1).trim()
                else returnType = afterClose.trim()
            }
            return ClassMember(
                id = name, memberType = MemberType.METHOD,
                visibility = visibility, text = name,
                classifier = classifier, parameters = params, returnType = returnType,
            )
        } else {
            return ClassMember(
                id = t, memberType = MemberType.ATTRIBUTE,
                visibility = visibility, text = t,
                classifier = classifier,
            )
        }
    }

    override fun clear() {
        super.clear()
        classes.clear()
        relations.clear()
        namespaces.clear()
        direction = "TB"
    }
}
