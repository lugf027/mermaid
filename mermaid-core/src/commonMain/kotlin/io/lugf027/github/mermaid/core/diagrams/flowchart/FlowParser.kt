package io.lugf027.github.mermaid.core.diagrams.flowchart

import io.lugf027.github.mermaid.core.types.ParserDefinition
import io.lugf027.github.mermaid.core.utils.Logger

/**
 * 流程图递归下降解析器。
 * 解析 flowchart/graph 语法，填充 FlowDb。
 *
 * 支持的语法概要：
 * - 图声明: `flowchart LR` 或 `graph TB`
 * - 节点定义: `A[text]`, `B(text)`, `C{text}`, `D((text))` 等
 * - 边定义: `-->`, `---`, `-.->`, `==>`, `--text-->`, `<-->` 等
 * - 子图: `subgraph id [title] ... end`
 * - 样式: `classDef`, `class`, `style`, `linkStyle`
 * - 交互: `click nodeId callback/href "url"`
 *
 * 对应 mermaid-js flow.jison。
 */
class FlowParser(private val db: FlowDb) : ParserDefinition {

    private val tag = "FlowParser"

    private var source = ""
    private var pos = 0
    private var line = 1
    private var col = 1

    override fun parse(input: String) {
        source = input
        pos = 0
        line = 1
        col = 1

        parseDocument()
    }

    // ─── 顶层文档解析 ──────────────────────────────

    private fun parseDocument() {
        skipWhitespaceAndComments()

        // 解析第一行：flowchart/graph + 方向
        parseGraphDeclaration()

        // 解析后续语句
        while (!isAtEnd()) {
            skipWhitespaceAndComments()
            if (isAtEnd()) break
            parseStatement()
        }
    }

    /**
     * 解析图声明行。
     * flowchart LR / graph TB / flowchart-elk RL 等。
     */
    private fun parseGraphDeclaration() {
        val keyword = readWord()
        if (keyword !in listOf("flowchart", "graph", "flowchart-elk")) {
            Logger.warn(tag, "Expected 'flowchart' or 'graph', got '$keyword'")
            return
        }

        skipSpaces()

        // 可选的方向参数
        if (!isAtEnd() && !isNewline()) {
            val dir = readWord()
            if (dir.isNotEmpty()) {
                db.setDirection(dir)
            }
        }

        skipToNextLine()
    }

    // ─── 语句解析 ──────────────────────────────────

    private fun parseStatement() {
        skipWhitespaceAndComments()
        if (isAtEnd()) return

        val saved = pos

        // 尝试匹配关键字语句
        val word = peekWord()
        when (word.lowercase()) {
            "subgraph" -> { parseSubgraph(); return }
            "end" -> { advanceWord(); skipToNextLine(); return }
            "classdef" -> { parseClassDef(); return }
            "class" -> { parseClassAssignment(); return }
            "style" -> { parseStyleDef(); return }
            "linkstyle" -> { parseLinkStyle(); return }
            "click" -> { parseClick(); return }
            "direction" -> { parseDirection(); return }
            "" -> { skipToNextLine(); return }
        }

        // 尝试解析节点链（A --> B --> C）
        parseNodeChain()
    }

    // ─── 节点链解析 ────────────────────────────────

    /**
     * 解析节点链语句。
     * 例: A[text] --> B(text) -.-> C
     * 支持 & 运算符: A & B --> C & D
     */
    private fun parseNodeChain() {
        val startNodes = parseNodeGroup()
        if (startNodes.isEmpty()) {
            // 无法解析任何节点，跳过本行
            skipToNextLine()
            return
        }

        // 检查是否有后续的边连接
        while (!isAtEnd() && !isNewline()) {
            skipSpaces()
            if (isAtEnd() || isNewline()) break

            // 尝试解析边
            val linkInfo = tryParseLink()
            if (linkInfo == null) {
                // 可能有 ::: 类名
                if (peekString(":::")) {
                    advance(3)
                    val className = readWord()
                    startNodes.forEach { db.setClass(it, className) }
                }
                break
            }

            skipSpaces()

            // 解析目标节点组
            val endNodes = parseNodeGroup()
            if (endNodes.isEmpty()) break

            // 添加边
            db.addLink(startNodes, endNodes, linkInfo.first, linkInfo.second)

            // 后续可能继续链接
            startNodes.clear()
            startNodes.addAll(endNodes)
        }

        skipToNextLine()
    }

    /**
     * 解析节点组（支持 & 分隔）。
     * 例: A & B & C
     */
    private fun parseNodeGroup(): MutableList<String> {
        val nodes = mutableListOf<String>()

        val firstNode = tryParseNode()
        if (firstNode != null) {
            nodes.add(firstNode)

            while (true) {
                skipSpaces()
                if (!peekString("&")) break
                advance(1)
                skipSpaces()
                val node = tryParseNode() ?: break
                nodes.add(node)
            }
        }

        return nodes
    }

    /**
     * 尝试解析一个节点定义。
     * 可能的格式:
     * - 纯 ID: A, node1, _n123
     * - ID + 形状: A[text], B(text), C{text}, D((text)), etc.
     * - 带 ::: 类: A:::className
     *
     * 返回节点 ID，或 null。
     */
    private fun tryParseNode(): String? {
        skipSpaces()
        if (isAtEnd() || isNewline()) return null

        val id = readNodeId() ?: return null

        skipSpaces()

        // 检查形状括号
        val (text, type) = tryParseNodeShape()

        // 如果有形状括号，解析文本和类型
        if (type != null || text != null) {
            db.addVertex(id, text, type)
        } else {
            db.addVertex(id)
        }

        // 检查 ::: 类
        skipSpaces()
        if (peekString(":::")) {
            advance(3)
            val className = readWord()
            if (className.isNotEmpty()) {
                db.setClass(id, className)
            }
        }

        return id
    }

    /**
     * 读取节点 ID。
     * ID 可以是字母数字下划线组合，也可以是引号字符串。
     */
    private fun readNodeId(): String? {
        if (isAtEnd()) return null

        // 引号 ID
        if (current() == '"') {
            return readQuotedString()
        }

        // 普通 ID：字母、数字、下划线、连字符
        val start = pos
        while (!isAtEnd() && isIdChar(current())) {
            advance(1)
        }
        if (pos == start) return null
        return source.substring(start, pos)
    }

    private fun isIdChar(c: Char): Boolean =
        c.isLetterOrDigit() || c == '_' || c == '-'

    /**
     * 尝试解析节点形状括号及其中的文本。
     * 返回 (text, type) 对。
     */
    private fun tryParseNodeShape(): Pair<String?, FlowVertexType?> {
        if (isAtEnd() || isNewline()) return null to null

        return when {
            // ((( text ))) → double_circle
            peekString("(((") -> {
                advance(3)
                val text = readUntil(")))")
                advance(3)
                text to FlowVertexType.DOUBLE_CIRCLE
            }
            // (( text )) → circle
            peekString("((") -> {
                advance(2)
                val text = readUntil("))")
                advance(2)
                text to FlowVertexType.CIRCLE
            }
            // ([ text ]) → stadium
            peekString("([") -> {
                advance(2)
                val text = readUntil("])")
                advance(2)
                text to FlowVertexType.STADIUM
            }
            // (- text -) → ellipse
            peekString("(-") -> {
                advance(2)
                val text = readUntil("-)")
                advance(2)
                text to FlowVertexType.ELLIPSE
            }
            // ( text ) → round
            peekString("(") && !peekString("((") -> {
                advance(1)
                val text = readBalanced('(', ')')
                text to FlowVertexType.ROUND
            }
            // {{ text }} → hexagon
            peekString("{{") -> {
                advance(2)
                val text = readUntil("}}")
                advance(2)
                text to FlowVertexType.HEXAGON
            }
            // { text } → diamond
            peekString("{") && !peekString("{{") -> {
                advance(1)
                val text = readBalanced('{', '}')
                text to FlowVertexType.DIAMOND
            }
            // [[ text ]] → subroutine
            peekString("[[") -> {
                advance(2)
                val text = readUntil("]]")
                advance(2)
                text to FlowVertexType.SUBROUTINE
            }
            // [( text )] → cylinder
            peekString("[(") -> {
                advance(2)
                val text = readUntil(")]")
                advance(2)
                text to FlowVertexType.CYLINDER
            }
            // [/ text \] → trapezoid
            peekString("[/") -> {
                advance(2)
                val text = readUntilOneOf(listOf("\\]", "/]"))
                val isTrapezoid = if (!isAtEnd() && current() == '\\') {
                    advance(2) // \]
                    true
                } else {
                    advance(2) // /]
                    false
                }
                if (isTrapezoid) {
                    text to FlowVertexType.TRAPEZOID
                } else {
                    text to FlowVertexType.LEAN_RIGHT
                }
            }
            // [\ text /] → inv_trapezoid  or  [\ text \] → lean_left
            peekString("[\\") -> {
                advance(2)
                val text = readUntilOneOf(listOf("/]", "\\]"))
                val isInvTrap = if (!isAtEnd() && current() == '/') {
                    advance(2) // /]
                    true
                } else {
                    advance(2) // \]
                    false
                }
                if (isInvTrap) {
                    text to FlowVertexType.INV_TRAPEZOID
                } else {
                    text to FlowVertexType.LEAN_LEFT
                }
            }
            // [ text ] → square
            peekString("[") -> {
                advance(1)
                val text = readBalanced('[', ']')
                text to FlowVertexType.SQUARE
            }
            // > text ] → odd
            peekString(">") && !peekString(">>") && !isLinkChar() -> {
                advance(1)
                val text = readUntil("]")
                advance(1)
                text to FlowVertexType.ODD
            }
            else -> null to null
        }
    }

    // ─── 边解析 ────────────────────────────────────

    /**
     * 尝试解析边语法。
     * 返回 (LinkInfo, edgeText) 或 null。
     *
     * 支持格式：
     * -->, ---, -.->，==>，<-->, x--x, o--o
     * 带文本：--text-->, --|text|-->, -.text.->
     * 长边：--->, ====>, -...->
     */
    private fun tryParseLink(): Pair<LinkInfo, String>? {
        val saved = pos
        val savedLine = line
        val savedCol = col

        // 检测起始字符
        val startChar = current()

        // ~~~ 隐形线
        if (peekString("~~~")) {
            advance(3)
            return LinkInfo(ArrowType.ARROW_OPEN, StrokeStyle.INVISIBLE, 1) to ""
        }

        // 解析起始标记（双箭头前缀）
        var isDoubleStart = false
        var doubleStartType: Char? = null

        when {
            peekString("<-") || peekString("<.") || peekString("<=") -> {
                isDoubleStart = true
                doubleStartType = '<'
                advance(1)
            }
            peekString("x-") || peekString("x.") || peekString("x=") -> {
                isDoubleStart = true
                doubleStartType = 'x'
                advance(1)
            }
            peekString("o-") || peekString("o.") || peekString("o=") -> {
                isDoubleStart = true
                doubleStartType = 'o'
                advance(1)
            }
        }

        // 解析线条主体
        val strokeChar = if (!isAtEnd()) current() else ' '
        val strokeStyle: StrokeStyle
        val dashChar: Char
        val endChar: Char

        when (strokeChar) {
            '-' -> {
                // 检查是否是虚线 -.
                if (pos + 1 < source.length && source[pos + 1] == '.') {
                    strokeStyle = StrokeStyle.DOTTED
                    dashChar = '.'
                    endChar = '-'
                    advance(2) // skip -.
                } else {
                    strokeStyle = StrokeStyle.NORMAL
                    dashChar = '-'
                    endChar = '-'
                    advance(1) // skip -
                }
            }
            '=' -> {
                strokeStyle = StrokeStyle.THICK
                dashChar = '='
                endChar = '='
                advance(1) // skip =
            }
            else -> {
                // 不是边，恢复位置
                pos = saved; line = savedLine; col = savedCol
                return null
            }
        }

        // 计算长度（中间的 dash/dot 字符数）
        var length = 1
        var linkText = ""

        // 检查是否有 |text| 模式
        if (!isAtEnd() && current() == '|') {
            advance(1)
            linkText = readUntil("|")
            advance(1) // skip closing |
        } else if (!isAtEnd() && current() != endChar && current() != '>' && current() != 'x' && current() != 'o' && !isNewline()) {
            // 内联文本模式: --text--> 或 -.text.->
            val textBuilder = StringBuilder()
            while (!isAtEnd() && !isNewline()) {
                // 检查是否到达结束模式
                if (strokeStyle == StrokeStyle.DOTTED) {
                    if (peekString(".->") || peekString(".-")) break
                } else if (strokeStyle == StrokeStyle.THICK) {
                    if (peekString("==>") || peekString("==")) break
                } else {
                    if (peekString("-->") || peekString("--")) break
                }
                textBuilder.append(current())
                advance(1)
            }
            linkText = textBuilder.toString().trim()
        }

        // 消费结尾的 dash/dot 字符
        while (!isAtEnd() && (current() == dashChar || current() == endChar)) {
            length++
            advance(1)
        }

        // 解析结束箭头标记
        val endType: Char? = if (!isAtEnd()) {
            when (current()) {
                '>' -> { advance(1); '>' }
                'x' -> { advance(1); 'x' }
                'o' -> { advance(1); 'o' }
                else -> null
            }
        } else null

        // 确定箭头类型
        val arrowType = determineArrowType(isDoubleStart, doubleStartType, endType)
            ?: run {
                pos = saved; line = savedLine; col = savedCol
                return null
            }

        return LinkInfo(arrowType, strokeStyle, length.coerceAtMost(10)) to linkText
    }

    /**
     * 根据起止标记确定箭头类型。
     */
    private fun determineArrowType(isDouble: Boolean, startType: Char?, endType: Char?): ArrowType? {
        if (isDouble) {
            return when {
                startType == '<' && endType == '>' -> ArrowType.DOUBLE_ARROW_POINT
                startType == 'x' && endType == 'x' -> ArrowType.DOUBLE_ARROW_CROSS
                startType == 'o' && endType == 'o' -> ArrowType.DOUBLE_ARROW_CIRCLE
                startType == '<' -> ArrowType.DOUBLE_ARROW_POINT
                else -> null
            }
        }
        return when (endType) {
            '>' -> ArrowType.ARROW_POINT
            'x' -> ArrowType.ARROW_CROSS
            'o' -> ArrowType.ARROW_CIRCLE
            null -> ArrowType.ARROW_OPEN
            else -> null
        }
    }

    // ─── 关键字语句解析 ────────────────────────────

    private fun parseSubgraph() {
        advanceWord() // skip "subgraph"
        skipSpaces()

        var id: String? = null
        var title: String? = null

        // 尝试读取 ID 和标题
        if (!isAtEnd() && !isNewline()) {
            val firstPart = readWord()
            skipSpaces()

            if (!isAtEnd() && !isNewline() && current() == '[') {
                // subgraph id [title]
                id = firstPart
                advance(1)
                title = readUntil("]")
                advance(1)
            } else {
                // subgraph id/title（同一个）
                id = firstPart
                title = firstPart
            }
        }

        skipToNextLine()

        // 收集子图内的节点
        val nodeIds = mutableListOf<String>()
        val savedSubgraphs = mutableListOf<String>()
        var depth = 1

        while (!isAtEnd() && depth > 0) {
            skipWhitespaceAndComments()
            if (isAtEnd()) break

            val word = peekWord()
            when (word.lowercase()) {
                "subgraph" -> {
                    depth++
                    parseStatement()
                }
                "end" -> {
                    depth--
                    if (depth == 0) {
                        advanceWord()
                        skipToNextLine()
                    } else {
                        parseStatement()
                    }
                }
                "direction" -> {
                    parseDirection()
                }
                else -> {
                    // 解析子图内的语句，收集涉及的节点
                    val verticesBefore = db.getVertices().keys.toSet()
                    parseStatement()
                    val verticesAfter = db.getVertices().keys.toSet()
                    nodeIds.addAll(verticesAfter - verticesBefore)
                }
            }
        }

        // 添加到 FlowDb
        if (id != null) {
            db.addSubGraph(id, nodeIds, title)
        }
    }

    private fun parseClassDef() {
        advanceWord() // skip "classDef"
        skipSpaces()
        val className = readWord()
        skipSpaces()
        val styleStr = readToEndOfLine()
        db.addClass(className, styleStr)
        skipToNextLine()
    }

    private fun parseClassAssignment() {
        advanceWord() // skip "class"
        skipSpaces()
        val nodeIds = readWord()
        skipSpaces()
        val className = readWord()
        db.setClass(nodeIds, className)
        skipToNextLine()
    }

    private fun parseStyleDef() {
        advanceWord() // skip "style"
        skipSpaces()
        val nodeId = readWord()
        skipSpaces()
        val styleStr = readToEndOfLine()
        // 直接给节点添加内联样式
        val styles = styleStr.split(",").map { it.trim() }
        db.getVertices()[nodeId]?.styles?.addAll(styles)
        skipToNextLine()
    }

    private fun parseLinkStyle() {
        advanceWord() // skip "linkStyle"
        skipSpaces()
        val posStr = readWord()
        skipSpaces()

        // 解析位置（可以是 default 或数字）
        val positions = if (posStr.lowercase() == "default") {
            listOf(-1)
        } else {
            posStr.split(",").mapNotNull { it.trim().toIntOrNull() }
        }

        // 检查是否有 interpolate
        val nextWord = peekWord()
        if (nextWord.lowercase() == "interpolate") {
            advanceWord()
            skipSpaces()
            val interpolate = readWord()
            db.updateLinkInterpolate(positions, interpolate)
            skipSpaces()
        }

        val styleStr = readToEndOfLine()
        if (styleStr.isNotEmpty()) {
            val styles = styleStr.split(",").map { it.trim() }
            db.updateLinkStyle(positions, styles)
        }
        skipToNextLine()
    }

    private fun parseClick() {
        advanceWord() // skip "click"
        skipSpaces()
        val nodeId = readWord()
        skipSpaces()

        val next = peekWord()
        when (next.lowercase()) {
            "href" -> {
                advanceWord()
                skipSpaces()
                val url = readQuotedStringOrWord()
                skipSpaces()
                val tooltip = if (!isAtEnd() && !isNewline() && current() == '"') {
                    readQuotedString()
                } else null
                skipSpaces()
                val target = if (!isAtEnd() && !isNewline()) readWord() else null
                db.setLink(nodeId, url, target)
                if (tooltip != null) db.setTooltip(nodeId, tooltip)
            }
            "call" -> {
                advanceWord()
                skipSpaces()
                // 忽略回调（Kotlin 不支持动态回调）
                skipToEndOfLine()
            }
            else -> {
                // click nodeId "url" 或 click nodeId callback
                val urlOrCallback = readQuotedStringOrWord()
                if (urlOrCallback.startsWith("http") || urlOrCallback.startsWith("/")) {
                    db.setLink(nodeId, urlOrCallback)
                }
                // 忽略其他回调场景
            }
        }
        skipToNextLine()
    }

    private fun parseDirection() {
        advanceWord() // skip "direction"
        skipSpaces()
        val dir = readWord()
        db.setDirection(dir)
        skipToNextLine()
    }

    // ─── 辅助方法 ──────────────────────────────────

    private fun isAtEnd(): Boolean = pos >= source.length
    private fun current(): Char = source[pos]

    private fun advance(n: Int) {
        repeat(n) {
            if (pos < source.length) {
                if (source[pos] == '\n') { line++; col = 1 } else { col++ }
                pos++
            }
        }
    }

    private fun isNewline(): Boolean = !isAtEnd() && (current() == '\n' || current() == '\r')

    private fun isLinkChar(): Boolean {
        if (isAtEnd()) return false
        val c = current()
        return c == '-' || c == '=' || c == '.' || c == '~'
    }

    private fun peekString(s: String): Boolean {
        if (pos + s.length > source.length) return false
        return source.substring(pos, pos + s.length) == s
    }

    private fun skipSpaces() {
        while (!isAtEnd() && (current() == ' ' || current() == '\t')) advance(1)
    }

    private fun skipToNextLine() {
        while (!isAtEnd() && current() != '\n') advance(1)
        if (!isAtEnd()) advance(1)
    }

    private fun skipToEndOfLine() {
        while (!isAtEnd() && current() != '\n') advance(1)
    }

    private fun skipWhitespaceAndComments() {
        while (!isAtEnd()) {
            when {
                current().isWhitespace() -> advance(1)
                peekString("%%") -> skipToNextLine() // 注释
                current() == ';' -> advance(1) // 语句分隔符
                else -> break
            }
        }
    }

    private fun readWord(): String {
        val sb = StringBuilder()
        while (!isAtEnd() && !current().isWhitespace() && current() != ';' && current() != '\n') {
            sb.append(current())
            advance(1)
        }
        return sb.toString()
    }

    private fun peekWord(): String {
        val saved = pos
        val savedLine = line
        val savedCol = col
        val word = readWord()
        pos = saved; line = savedLine; col = savedCol
        return word
    }

    private fun advanceWord() {
        readWord()
    }

    private fun readToEndOfLine(): String {
        val sb = StringBuilder()
        while (!isAtEnd() && current() != '\n') {
            sb.append(current())
            advance(1)
        }
        return sb.toString().trim()
    }

    private fun readQuotedString(): String {
        if (isAtEnd() || current() != '"') return ""
        advance(1) // skip opening "
        val sb = StringBuilder()
        while (!isAtEnd() && current() != '"') {
            if (current() == '\\' && pos + 1 < source.length) {
                advance(1)
                sb.append(current())
            } else {
                sb.append(current())
            }
            advance(1)
        }
        if (!isAtEnd()) advance(1) // skip closing "
        return sb.toString()
    }

    private fun readQuotedStringOrWord(): String {
        return if (!isAtEnd() && current() == '"') {
            readQuotedString()
        } else {
            readWord()
        }
    }

    private fun readUntil(delimiter: String): String {
        val sb = StringBuilder()
        while (!isAtEnd() && !peekString(delimiter)) {
            sb.append(current())
            advance(1)
        }
        return sb.toString().trim()
    }

    private fun readUntilOneOf(delimiters: List<String>): String {
        val sb = StringBuilder()
        while (!isAtEnd()) {
            if (delimiters.any { peekString(it) }) break
            sb.append(current())
            advance(1)
        }
        return sb.toString().trim()
    }

    /**
     * 读取平衡括号内的文本。
     * 处理嵌套括号。
     */
    private fun readBalanced(open: Char, close: Char): String {
        val sb = StringBuilder()
        var depth = 0
        while (!isAtEnd()) {
            when (current()) {
                open -> { depth++; if (depth > 0) sb.append(current()) }
                close -> {
                    if (depth <= 0) {
                        advance(1) // skip closing bracket
                        return sb.toString().trim()
                    }
                    depth--
                    sb.append(current())
                }
                else -> sb.append(current())
            }
            advance(1)
        }
        return sb.toString().trim()
    }
}
