package io.lugf027.github.mermaid.core.diagram.flowchart

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser
import io.lugf027.github.mermaid.core.util.Logger

/**
 * 流程图解析器 - 手写递归下降解析器
 *
 * 对标 mermaid-js flow.jison 语法文件。
 * 支持解析：
 * - 图表方向声明：graph/flowchart TB/LR/BT/RL
 * - 节点定义：A[text], B((text)), C{text}, D(text), E>text], F[/text/], G[\text\]等
 * - 边定义：-->, ---, -.->  , ==> 等（含标签）
 * - 子图：subgraph ... end
 * - 样式类：classDef, class
 * - 点击事件：click
 */
class FlowchartParser : DiagramParser {
    private val log = Logger("FlowchartParser")

    private var pos = 0
    private var text = ""
    private var lines = listOf<String>()

    /**
     * 子图栈 — 追踪当前嵌套的 subgraph...end 块
     *
     * 对标 mermaid-js flow.jison 的递归 document 规则：
     * JISON 语法天然形成栈（内层子图先完成解析、先注册），
     * 手写解析器需要显式维护栈来模拟这个行为。
     *
     * 每个元素是 (subgraphId, 收集到的节点ID列表)
     */
    private val subGraphStack = mutableListOf<Pair<String, MutableList<String>>>()

    override fun parse(text: String, db: DiagramDB) {
        val flowDb = db as FlowchartDb
        this.text = text.trim()
        this.lines = this.text.lines()
        this.pos = 0

        if (lines.isEmpty()) return

        // 清除子图栈
        subGraphStack.clear()

        // 解析第一行：graph/flowchart + 方向
        parseHeader(flowDb)

        // 解析剩余行
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            parseLine(line, flowDb)
        }
    }

    /** 解析头部：graph/flowchart + 方向 */
    private fun parseHeader(db: FlowchartDb) {
        val firstLine = lines[0].trim()
        val headerRegex = Regex("""^(graph|flowchart)\s+(\w+)?""")
        val match = headerRegex.find(firstLine)

        if (match != null) {
            val direction = match.groupValues[2].uppercase().ifEmpty { "TB" }
            db.setDirection(when (direction) {
                "TB", "TD" -> direction
                "BT" -> "BT"
                "LR" -> "LR"
                "RL" -> "RL"
                else -> "TB"
            })
        }
    }

    /** 解析单行 */
    private fun parseLine(line: String, db: FlowchartDb) {
        val trimmed = line.trim()

        // 跳过 subgraph/end/classDef/class/click/style/linkStyle
        when {
            trimmed.startsWith("subgraph ") -> parseSubgraph(trimmed, db)
            trimmed == "end" -> {
                // 子图结束 — 弹出栈顶，将收集到的节点传给 db
                if (subGraphStack.isNotEmpty()) {
                    val (sgId, nodeIds) = subGraphStack.removeAt(subGraphStack.size - 1)
                    // 获取已注册的子图并更新其节点列表
                    db.updateSubGraphNodes(sgId, nodeIds)
                    // 将子图 ID 注册到外层子图（子图作为外层子图的一个"节点"）
                    // 对标 mermaid-js：JISON 返回子图 id，被外层 document 规则收集
                    if (subGraphStack.isNotEmpty()) {
                        subGraphStack.last().second.add(sgId)
                    }
                }
            }
            trimmed.startsWith("classDef ") -> parseClassDef(trimmed, db)
            trimmed.startsWith("class ") -> parseClassAssignment(trimmed, db)
            trimmed.startsWith("click ") -> { /* 忽略点击事件 */ }
            trimmed.startsWith("style ") -> { /* 忽略内联样式 */ }
            trimmed.startsWith("linkStyle ") -> { /* 忽略链接样式 */ }
            trimmed.startsWith("direction ") -> {
                val dir = trimmed.removePrefix("direction").trim().uppercase()
                db.setDirection(dir)
            }
            else -> parseNodeAndEdge(trimmed, db)
        }
    }

    /** 解析节点和边定义 */
    private fun parseNodeAndEdge(line: String, db: FlowchartDb) {
        // 拆分多条语句（用 ; 分隔）
        val statements = line.split(";").map { it.trim() }.filter { it.isNotEmpty() }

        for (stmt in statements) {
            parseStatement(stmt, db)
        }
    }

    /** 解析单条语句 */
    private fun parseStatement(stmt: String, db: FlowchartDb) {
        // 使用基于位置的扫描来分离节点和边
        // 查找边操作符的位置
        val edgeOperators = listOf(
            "<-->", "x--x", "o--o",
            "-.->", "==>", "-->", "---", "--x", "--o",
        )

        // 也检查 -->|text| 模式
        val labelEdgeRegex = Regex("""(-->|---|-\.->|==>)\|([^|]*)\|""")
        val labelMatch = labelEdgeRegex.find(stmt)
        if (labelMatch != null) {
            val edgeStart = labelMatch.range.first
            val edgeEnd = labelMatch.range.last + 1
            val leftPart = stmt.substring(0, edgeStart).trim()
            val rightPart = stmt.substring(edgeEnd).trim()
            val edgeOp = labelMatch.groupValues[1]
            val edgeLabel = labelMatch.groupValues[2]

            if (leftPart.isNotEmpty() && rightPart.isNotEmpty()) {
                val startId = parseNodeId(leftPart)
                val endId = parseNodeId(rightPart)
                parseNodeDefinition(leftPart, db)
                parseNodeDefinition(rightPart, db)
                val (arrowType, stroke) = parseEdgeType(edgeOp)
                db.addEdge(startId, endId, arrowType, edgeLabel.ifEmpty { null }, stroke)
                return
            }
        }

        // 查找简单边操作符
        for (op in edgeOperators) {
            val idx = stmt.indexOf(op)
            if (idx > 0) {
                val leftPart = stmt.substring(0, idx).trim()
                val rightPart = stmt.substring(idx + op.length).trim()

                if (leftPart.isNotEmpty() && rightPart.isNotEmpty()) {
                    val startId = parseNodeId(leftPart)
                    val endId = parseNodeId(rightPart)
                    parseNodeDefinition(leftPart, db)
                    parseNodeDefinition(rightPart, db)
                    val (arrowType, stroke) = parseEdgeType(op)
                    db.addEdge(startId, endId, arrowType, null, stroke)
                    return
                }
            }
        }

        // 检查长边模式 (---->  , ====>)
        val longEdgeRegex = Regex("""^(.+?)\s+([-]{3,}>|[=]{3,}>)\s+(.+)$""")
        val longMatch = longEdgeRegex.find(stmt)
        if (longMatch != null) {
            val leftPart = longMatch.groupValues[1].trim()
            val edgeOp = longMatch.groupValues[2]
            val rightPart = longMatch.groupValues[3].trim()

            val startId = parseNodeId(leftPart)
            val endId = parseNodeId(rightPart)
            parseNodeDefinition(leftPart, db)
            parseNodeDefinition(rightPart, db)
            val (arrowType, stroke) = parseEdgeType(edgeOp)
            db.addEdge(startId, endId, arrowType, null, stroke)
            return
        }

        // 不是边定义，解析为独立节点
        parseNodeDefinition(stmt, db)
    }

    /** 从文本中提取节点 ID */
    private fun parseNodeId(text: String): String {
        val idMatch = Regex("""^(\w+)""").find(text.trim())
        return idMatch?.groupValues?.get(1) ?: text.trim()
    }

    /** 解析节点定义（可能包含形状标记） */
    private fun parseNodeDefinition(text: String, db: FlowchartDb) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        // 匹配节点形状模式
        // 关键：复合分隔符（如 (()), {{}}, ([]), [()]）必须在对应的
        // 简单分隔符之前匹配，否则外层分隔符会被简单正则抢先匹配。
        // 例如 A([Stadium]) 会被 roundedRect 的 (…) 匹配为标签 "[Stadium]"。
        val shapePatterns = listOf(
            // === 复合分隔符（必须在前） ===
            // A((text)) - 圆形
            Regex("""^(\w+)\(\(([^)]*)\)\)$""") to "circle",
            // A{{text}} - 六边形
            Regex("""^(\w+)\{\{([^}]*)\}\}$""") to "hexagon",
            // A([text]) - 体育场
            Regex("""^(\w+)\(\[([^\]]*)\]\)$""") to "stadium",
            // A[(text)] - 圆柱
            Regex("""^(\w+)\[\(([^)]*)\)\]$""") to "cylinder",
            // A[/text/] - 平行四边形
            Regex("""^(\w+)\[/([^/]*)(/|\\)\]$""") to "parallelogram",
            // A[\text\] - 反向平行四边形
            Regex("""^(\w+)\[\\([^\\]*)\\\]$""") to "lean_left",
            // A[/text\] - 梯形
            Regex("""^(\w+)\[/([^\\]*)\\\]$""") to "trapezoid",
            // A[\text/] - 倒梯形
            Regex("""^(\w+)\[\\([^/]*)\/\]$""") to "inv_trapezoid",
            // === 简单分隔符（在后） ===
            // A[text] - 方形
            Regex("""^(\w+)\[([^\]]*)\]$""") to "squareRect",
            // A(text) - 圆角
            Regex("""^(\w+)\(([^)]*)\)$""") to "roundedRect",
            // A{text} - 菱形
            Regex("""^(\w+)\{([^}]*)\}$""") to "diamond",
            // A>text] - 旗帜
            Regex("""^(\w+)>([^\]]*)\]$""") to "odd",
        )

        for ((pattern, shape) in shapePatterns) {
            val match = pattern.find(trimmed)
            if (match != null) {
                val id = match.groupValues[1]
                val label = match.groupValues[2]
                db.addVertex(id, label, shape)
                // 注册节点到当前活跃子图
                registerNodeToCurrentSubgraph(id)
                return
            }
        }

        // 纯 ID（无形状标记）
        val idMatch = Regex("""^(\w+)$""").find(trimmed)
        if (idMatch != null) {
            db.addVertex(idMatch.groupValues[1])
            // 注册节点到当前活跃子图
            registerNodeToCurrentSubgraph(idMatch.groupValues[1])
        }
    }

    /**
     * 将节点 ID 注册到当前活跃的子图栈顶
     *
     * 对标 mermaid-js：JISON 的 document 规则自动收集语句中的节点 ID。
     * 在手写解析器中，需要显式在解析到节点时将其加入栈顶子图的节点列表。
     */
    private fun registerNodeToCurrentSubgraph(nodeId: String) {
        if (subGraphStack.isNotEmpty()) {
            val nodeList = subGraphStack.last().second
            if (nodeId !in nodeList) {
                nodeList.add(nodeId)
            }
        }
    }

    /** 解析边类型 */
    private fun parseEdgeType(edgeStr: String): Pair<String, String> {
        return when {
            edgeStr.contains("==>") || edgeStr.matches(Regex("[=]+>")) -> "arrow_point" to "thick"
            edgeStr.contains("-.->") || edgeStr.contains("-..->") -> "arrow_point" to "dotted"
            edgeStr.contains("-->") || edgeStr.matches(Regex("[-]+>")) -> "arrow_point" to "normal"
            edgeStr.contains("---") || edgeStr.matches(Regex("[-]+")) -> "none" to "normal"
            edgeStr.contains("--x") -> "arrow_cross" to "normal"
            edgeStr.contains("--o") -> "arrow_circle" to "normal"
            edgeStr.contains("<-->") -> "double_arrow_point" to "normal"
            edgeStr.contains("x--x") -> "double_arrow_cross" to "normal"
            edgeStr.contains("o--o") -> "double_arrow_circle" to "normal"
            else -> "arrow_point" to "normal"
        }
    }

    /** 解析子图 — 对标 mermaid-js flow.jison subgraph 规则 */
    private fun parseSubgraph(line: String, db: FlowchartDb) {
        val match = Regex("""^subgraph\s+(\w+)(?:\s*\[([^\]]*)\])?""").find(line)
        if (match != null) {
            val id = match.groupValues[1]
            val title = match.groupValues[2].ifEmpty { id }
            // 先注册空子图到 DB（节点列表后面在 end 时更新）
            db.addSubGraph(id, title, emptyList())
            // 压栈：开始收集这个子图内的节点
            subGraphStack.add(Pair(id, mutableListOf()))
        }
    }

    /** 解析 classDef */
    private fun parseClassDef(line: String, db: FlowchartDb) {
        val match = Regex("""^classDef\s+(\w+)\s+(.+)$""").find(line)
        if (match != null) {
            val name = match.groupValues[1]
            val styles = match.groupValues[2].split(",").map { it.trim() }
            db.addClass(name, styles)
        }
    }

    /** 解析 class 赋值 */
    private fun parseClassAssignment(line: String, db: FlowchartDb) {
        val match = Regex("""^class\s+([\w,]+)\s+(\w+)$""").find(line)
        if (match != null) {
            val nodeIds = match.groupValues[1].split(",").map { it.trim() }
            val className = match.groupValues[2]
            for (nodeId in nodeIds) {
                db.getVertices()[nodeId]?.classes?.add(className)
            }
        }
    }
}
