package io.lugf027.github.mermaid.core.diagram.c4

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser

/**
 * C4 图解析器 - 对标 mermaid-js c4Diagram.jison
 *
 * 支持 5 种图表类型：C4Context, C4Container, C4Component, C4Dynamic, C4Deployment
 * 元素语法：Person(alias, "label", "descr")
 * 边界语法：System_Boundary(alias, "label") { ... }
 * 关系语法：Rel(from, to, "label", "techn")
 */
class C4Parser : DiagramParser {

    // 图表类型声明
    private val RE_C4_TYPE = Regex(
        "^\\s*(C4Context|C4Container|C4Component|C4Dynamic|C4Deployment)\\s*$",
        RegexOption.IGNORE_CASE
    )

    // Person/System 类（6参数）
    private val RE_PERSON_SYSTEM = Regex(
        "^\\s*(Person|Person_Ext|System|System_Ext|SystemDb|SystemDb_Ext|SystemQueue|SystemQueue_Ext)\\s*\\((.+)\\)\\s*$",
        RegexOption.IGNORE_CASE
    )

    // Container 类（7参数含 techn）
    private val RE_CONTAINER = Regex(
        "^\\s*(Container|Container_Ext|ContainerDb|ContainerDb_Ext|ContainerQueue|ContainerQueue_Ext)\\s*\\((.+)\\)\\s*$",
        RegexOption.IGNORE_CASE
    )

    // Component 类（7参数含 techn）
    private val RE_COMPONENT = Regex(
        "^\\s*(Component|Component_Ext|ComponentDb|ComponentDb_Ext|ComponentQueue|ComponentQueue_Ext)\\s*\\((.+)\\)\\s*$",
        RegexOption.IGNORE_CASE
    )

    // 边界声明（后跟 {）
    private val RE_BOUNDARY = Regex(
        "^\\s*(Enterprise_Boundary|System_Boundary|Container_Boundary|Boundary)\\s*\\((.+)\\)\\s*\\{\\s*$",
        RegexOption.IGNORE_CASE
    )

    // 部署节点
    private val RE_DEPLOYMENT_NODE = Regex(
        "^\\s*(Deployment_Node|Node|Node_L|Node_R)\\s*\\((.+)\\)\\s*\\{\\s*$",
        RegexOption.IGNORE_CASE
    )

    // 关系
    private val RE_REL = Regex(
        "^\\s*(Rel|BiRel|Rel_Up|Rel_U|Rel_Down|Rel_D|Rel_Left|Rel_L|Rel_Right|Rel_R|Rel_Back)\\s*\\((.+)\\)\\s*$",
        RegexOption.IGNORE_CASE
    )

    // 样式更新
    private val RE_UPDATE_EL_STYLE = Regex(
        "^\\s*UpdateElementStyle\\s*\\((.+)\\)\\s*$",
        RegexOption.IGNORE_CASE
    )
    private val RE_UPDATE_REL_STYLE = Regex(
        "^\\s*UpdateRelStyle\\s*\\((.+)\\)\\s*$",
        RegexOption.IGNORE_CASE
    )
    private val RE_UPDATE_LAYOUT = Regex(
        "^\\s*UpdateLayoutConfig\\s*\\((.+)\\)\\s*$",
        RegexOption.IGNORE_CASE
    )

    // 边界关闭
    private val RE_BOUNDARY_END = Regex("^\\s*}\\s*$")

    // 标题
    private val RE_TITLE = Regex("^\\s*title\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val RE_ACC_TITLE = Regex("^\\s*accTitle\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val RE_ACC_DESCR = Regex("^\\s*accDescr\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)

    override fun parse(text: String, db: DiagramDB) {
        val c4Db = db as C4Db
        c4Db.clear()

        val lines = text.lines()
        var started = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("%%")) continue

            // 匹配图表声明
            if (!started) {
                RE_C4_TYPE.find(trimmed)?.let {
                    c4Db.setC4Type(it.groupValues[1])
                    started = true
                }
                continue
            }

            // 标题
            RE_TITLE.find(trimmed)?.let {
                c4Db.setDiagramTitle(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_ACC_TITLE.find(trimmed)?.let {
                c4Db.setAccTitle(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_ACC_DESCR.find(trimmed)?.let {
                c4Db.setAccDescription(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            // 边界关闭
            if (RE_BOUNDARY_END.matches(trimmed)) {
                c4Db.popBoundaryParseStack()
                continue
            }

            // Person/System
            RE_PERSON_SYSTEM.find(trimmed)?.let { m ->
                val keyword = m.groupValues[1]
                val args = parseArgs(m.groupValues[2])
                val typeC4Shape = keywordToTypeC4Shape(keyword)
                if (args.size >= 2) {
                    c4Db.addPersonOrSystem(
                        typeC4Shape = typeC4Shape,
                        alias = args[0],
                        label = args[1],
                        descr = args.getOrElse(2) { "" },
                        sprite = args.getOrElse(3) { "" },
                        tags = args.getOrElse(4) { "" },
                        link = args.getOrElse(5) { "" }
                    )
                }
                return@let
            }?.also { continue }

            // Container
            RE_CONTAINER.find(trimmed)?.let { m ->
                val keyword = m.groupValues[1]
                val args = parseArgs(m.groupValues[2])
                val typeC4Shape = keywordToTypeC4Shape(keyword)
                if (args.size >= 2) {
                    c4Db.addContainer(
                        typeC4Shape = typeC4Shape,
                        alias = args[0],
                        label = args[1],
                        techn = args.getOrElse(2) { "" },
                        descr = args.getOrElse(3) { "" },
                        sprite = args.getOrElse(4) { "" },
                        tags = args.getOrElse(5) { "" },
                        link = args.getOrElse(6) { "" }
                    )
                }
                return@let
            }?.also { continue }

            // Component
            RE_COMPONENT.find(trimmed)?.let { m ->
                val keyword = m.groupValues[1]
                val args = parseArgs(m.groupValues[2])
                val typeC4Shape = keywordToTypeC4Shape(keyword)
                if (args.size >= 2) {
                    c4Db.addComponent(
                        typeC4Shape = typeC4Shape,
                        alias = args[0],
                        label = args[1],
                        techn = args.getOrElse(2) { "" },
                        descr = args.getOrElse(3) { "" },
                        sprite = args.getOrElse(4) { "" },
                        tags = args.getOrElse(5) { "" },
                        link = args.getOrElse(6) { "" }
                    )
                }
                return@let
            }?.also { continue }

            // Boundary
            RE_BOUNDARY.find(trimmed)?.let { m ->
                val keyword = m.groupValues[1]
                val args = parseArgs(m.groupValues[2])
                if (args.size >= 2) {
                    val defaultType = when (keyword.lowercase()) {
                        "enterprise_boundary" -> "ENTERPRISE"
                        "system_boundary" -> "SYSTEM"
                        "container_boundary" -> "CONTAINER"
                        else -> args.getOrElse(2) { "" }
                    }
                    c4Db.addPersonOrSystemBoundary(
                        alias = args[0],
                        label = args[1],
                        type = if (keyword.lowercase() == "boundary") args.getOrElse(2) { "" } else defaultType,
                        tags = args.getOrElse(3) { "" },
                        link = args.getOrElse(4) { "" }
                    )
                }
                return@let
            }?.also { continue }

            // Deployment Node
            RE_DEPLOYMENT_NODE.find(trimmed)?.let { m ->
                val keyword = m.groupValues[1]
                val args = parseArgs(m.groupValues[2])
                val nodeType = when (keyword.lowercase()) {
                    "node_l" -> "nodeL"
                    "node_r" -> "nodeR"
                    else -> "node"
                }
                if (args.size >= 2) {
                    c4Db.addDeploymentNode(
                        nodeType = nodeType,
                        alias = args[0],
                        label = args[1],
                        type = args.getOrElse(2) { "" },
                        descr = args.getOrElse(3) { "" },
                        sprite = args.getOrElse(4) { "" },
                        tags = args.getOrElse(5) { "" },
                        link = args.getOrElse(6) { "" }
                    )
                }
                return@let
            }?.also { continue }

            // Relationship
            RE_REL.find(trimmed)?.let { m ->
                val keyword = m.groupValues[1]
                val args = parseArgs(m.groupValues[2])
                val relType = when (keyword.lowercase()) {
                    "birel" -> "birel"
                    "rel_up", "rel_u" -> "rel_u"
                    "rel_down", "rel_d" -> "rel_d"
                    "rel_left", "rel_l" -> "rel_l"
                    "rel_right", "rel_r" -> "rel_r"
                    "rel_back" -> "rel_b"
                    else -> "rel"
                }
                if (args.size >= 3) {
                    c4Db.addRel(
                        type = relType,
                        from = args[0],
                        to = args[1],
                        label = args[2],
                        techn = args.getOrElse(3) { "" },
                        descr = args.getOrElse(4) { "" },
                        sprite = args.getOrElse(5) { "" },
                        tags = args.getOrElse(6) { "" },
                        link = args.getOrElse(7) { "" }
                    )
                }
                return@let
            }?.also { continue }

            // UpdateElementStyle
            RE_UPDATE_EL_STYLE.find(trimmed)?.let { m ->
                val args = parseArgs(m.groupValues[1])
                if (args.isNotEmpty()) {
                    c4Db.updateElStyle(
                        elementName = args[0],
                        bgColor = args.getOrElse(1) { "" },
                        fontColor = args.getOrElse(2) { "" },
                        borderColor = args.getOrElse(3) { "" }
                    )
                }
                return@let
            }?.also { continue }

            // UpdateRelStyle
            RE_UPDATE_REL_STYLE.find(trimmed)?.let { m ->
                val args = parseArgs(m.groupValues[1])
                if (args.size >= 2) {
                    c4Db.updateRelStyle(
                        from = args[0],
                        to = args[1],
                        textColor = args.getOrElse(2) { "" },
                        lineColor = args.getOrElse(3) { "" },
                        offsetX = args.getOrElse(4) { "" },
                        offsetY = args.getOrElse(5) { "" }
                    )
                }
                return@let
            }?.also { continue }

            // UpdateLayoutConfig
            RE_UPDATE_LAYOUT.find(trimmed)?.let { m ->
                val args = parseArgs(m.groupValues[1])
                c4Db.updateLayoutConfig(
                    shapeInRow = args.getOrElse(0) { "" },
                    boundaryInRow = args.getOrElse(1) { "" }
                )
                return@let
            }
        }
    }

    /**
     * 解析括号内的参数列表，支持引号字符串
     */
    private fun parseArgs(input: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var depth = 0

        for (ch in input) {
            when {
                ch == '"' && depth == 0 -> inQuotes = !inQuotes
                ch == '(' && !inQuotes -> depth++
                ch == ')' && !inQuotes -> depth--
                ch == ',' && !inQuotes && depth == 0 -> {
                    result.add(current.toString().trim().removeSurrounding("\""))
                    current = StringBuilder()
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) {
            result.add(current.toString().trim().removeSurrounding("\""))
        }
        return result
    }

    /**
     * 关键字到 typeC4Shape 的映射
     */
    private fun keywordToTypeC4Shape(keyword: String): String {
        return when (keyword.lowercase()) {
            "person" -> "person"
            "person_ext" -> "external_person"
            "system" -> "system"
            "system_ext" -> "external_system"
            "systemdb" -> "system_db"
            "systemdb_ext" -> "external_system_db"
            "systemqueue" -> "system_queue"
            "systemqueue_ext" -> "external_system_queue"
            "container" -> "container"
            "container_ext" -> "external_container"
            "containerdb" -> "container_db"
            "containerdb_ext" -> "external_container_db"
            "containerqueue" -> "container_queue"
            "containerqueue_ext" -> "external_container_queue"
            "component" -> "component"
            "component_ext" -> "external_component"
            "componentdb" -> "component_db"
            "componentdb_ext" -> "external_component_db"
            "componentqueue" -> "component_queue"
            "componentqueue_ext" -> "external_component_queue"
            else -> keyword.lowercase()
        }
    }
}
