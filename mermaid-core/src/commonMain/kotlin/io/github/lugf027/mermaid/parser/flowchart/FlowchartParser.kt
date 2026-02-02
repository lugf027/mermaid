/**
 * FlowchartParser - Parser for flowchart/graph diagrams.
 */
package io.github.lugf027.mermaid.parser.flowchart

import io.github.lugf027.mermaid.MermaidParseException
import io.github.lugf027.mermaid.model.Direction
import io.github.lugf027.mermaid.model.flowchart.*
import io.github.lugf027.mermaid.parser.FlowchartDetector

/**
 * Parser for Mermaid flowchart diagrams.
 * 
 * Supports:
 * - Node definitions with various shapes
 * - Edge definitions with labels
 * - Subgraphs
 * - Style definitions
 * - Click handlers
 */
public object FlowchartParser {

    /**
     * Parse flowchart text into FlowchartData.
     */
    public fun parse(text: String): FlowchartData {
        val parser = FlowchartParserImpl(text)
        return parser.parse()
    }
}

/**
 * Internal implementation of the flowchart parser.
 */
internal class FlowchartParserImpl(private val text: String) {
    private var pos: Int = 0
    private var line: Int = 1
    private var column: Int = 1
    private val data = FlowchartData()
    private val subgraphStack = mutableListOf<FlowSubgraph>()

    fun parse(): FlowchartData {
        skipWhitespaceAndComments()
        parseHeader()
        
        while (!isAtEnd()) {
            skipWhitespaceAndComments()
            if (!isAtEnd()) {
                parseStatement()
            }
        }
        
        return data
    }

    private fun parseHeader() {
        // Parse "flowchart" or "graph" keyword
        val keyword = readWord()
        if (keyword.lowercase() !in listOf("flowchart", "graph")) {
            throw parseError("Expected 'flowchart' or 'graph'")
        }

        skipWhitespace()

        // Parse optional direction
        if (!isAtEnd() && peek().isLetter()) {
            val dirStr = readWord()
            data.direction = Direction.fromKeyword(dirStr)
        }

        skipToEndOfLine()
    }

    private fun parseStatement() {
        skipWhitespaceAndComments()
        if (isAtEnd()) return

        val word = peekWord()
        
        when (word.lowercase()) {
            "subgraph" -> parseSubgraph()
            "end" -> parseEnd()
            "direction" -> parseDirectionStatement()
            "classdef" -> parseClassDef()
            "class" -> parseClass()
            "click" -> parseClick()
            "style" -> parseStyle()
            "linkstyle" -> parseLinkStyle()
            else -> parseNodeOrEdge()
        }
    }

    private fun parseSubgraph() {
        readWord() // consume "subgraph"
        skipWhitespace()

        // Parse subgraph ID
        val id = if (peek() == '[' || peek() == '"' || peek() == '\'') {
            "subgraph_${data.subgraphs.size}"
        } else {
            readIdentifier()
        }

        skipWhitespace()

        // Parse optional title in brackets
        val title = if (peek() == '[') {
            advance() // [
            val t = readUntil(']')
            advance() // ]
            t
        } else if (!isAtEnd() && !isEndOfLine()) {
            readToEndOfLine().trim()
        } else {
            id
        }

        val subgraph = FlowSubgraph(id = id, title = title)
        data.addSubgraph(subgraph)
        
        // Add to parent or root
        if (subgraphStack.isNotEmpty()) {
            subgraphStack.last().subgraphIds.add(id)
        } else {
            data.rootSubgraphIds.add(id)
        }
        
        subgraphStack.add(subgraph)
        skipToEndOfLine()
    }

    private fun parseEnd() {
        readWord() // consume "end"
        
        if (subgraphStack.isNotEmpty()) {
            subgraphStack.removeLast()
        }
        
        skipToEndOfLine()
    }

    private fun parseDirectionStatement() {
        readWord() // consume "direction"
        skipWhitespace()
        
        val dirStr = readWord()
        val direction = Direction.fromKeyword(dirStr)
        
        if (subgraphStack.isNotEmpty()) {
            // Direction for current subgraph
            val subgraph = subgraphStack.last()
            data.subgraphs[subgraph.id] = subgraph.copy(direction = direction)
        }
        
        skipToEndOfLine()
    }

    private fun parseClassDef() {
        readWord() // consume "classdef"
        skipWhitespace()
        
        val className = readIdentifier()
        skipWhitespace()
        
        val style = readToEndOfLine().trim()
        data.classDefs[className] = style
    }

    private fun parseClass() {
        readWord() // consume "class"
        skipWhitespace()
        
        // Parse node IDs
        val nodeIds = mutableListOf<String>()
        while (!isAtEnd() && peek() != ':' && !isEndOfLine()) {
            val id = readIdentifier()
            if (id.isNotEmpty()) {
                nodeIds.add(id)
            }
            skipWhitespace()
            if (peek() == ',') {
                advance()
                skipWhitespace()
            }
        }
        
        // Skip :::
        while (!isAtEnd() && (peek() == ':' || peek() == ' ')) {
            advance()
        }
        
        val className = readIdentifier()
        
        // Apply class to nodes
        for (nodeId in nodeIds) {
            val node = data.vertices[nodeId]
            if (node != null) {
                data.vertices[nodeId] = node.copy(
                    cssClasses = node.cssClasses + className
                )
            }
        }
        
        skipToEndOfLine()
    }

    private fun parseClick() {
        readWord() // consume "click"
        skipWhitespace()
        
        val nodeId = readIdentifier()
        skipWhitespace()
        
        val callback = readIdentifierOrString()
        skipWhitespace()
        
        val tooltip = if (!isAtEnd() && peek() == '"') {
            readString()
        } else {
            null
        }
        
        data.clickHandlers[nodeId] = ClickHandler(nodeId, callback, tooltip)
        skipToEndOfLine()
    }

    private fun parseStyle() {
        readWord() // consume "style"
        skipWhitespace()
        
        val nodeId = readIdentifier()
        skipWhitespace()
        
        val style = readToEndOfLine().trim()
        
        val node = data.vertices[nodeId]
        if (node != null) {
            // Store style - would need to parse CSS here
        }
        
        skipToEndOfLine()
    }

    private fun parseLinkStyle() {
        readWord() // consume "linkstyle"
        skipWhitespace()
        
        // Parse link index(es)
        val indexStr = readWord()
        skipWhitespace()
        
        val style = readToEndOfLine().trim()
        
        // Would apply style to edge at index
        skipToEndOfLine()
    }

    private fun parseNodeOrEdge() {
        // Parse first node
        val firstNode = parseNode()
        if (firstNode == null) {
            skipToEndOfLine()
            return
        }
        
        skipWhitespace()
        
        // Check for edge
        if (!isAtEnd() && isEdgeStart()) {
            parseEdgeChain(firstNode)
        }
        
        skipToEndOfLine()
    }

    private fun parseNode(): FlowVertex? {
        skipWhitespace()
        if (isAtEnd()) return null

        // Read node ID
        val id = readIdentifier()
        if (id.isEmpty()) return null

        skipWhitespace()

        // Check for shape definition
        var label = id
        var shape = NodeShape.DEFAULT
        var cssClasses = emptyList<String>()

        if (!isAtEnd()) {
            when (peek()) {
                '[' -> {
                    val (l, s) = parseSquareBracketShape()
                    label = l
                    shape = s
                }
                '(' -> {
                    val (l, s) = parseParenShape()
                    label = l
                    shape = s
                }
                '{' -> {
                    val (l, s) = parseBraceShape()
                    label = l
                    shape = s
                }
                '>' -> {
                    advance() // >
                    label = readUntil(']')
                    advance() // ]
                    shape = NodeShape.ASYMMETRIC
                }
            }
        }

        // Check for class annotation :::className
        if (!isAtEnd() && peek() == ':' && peekNext() == ':' && peekAhead(2) == ':') {
            advance(); advance(); advance() // :::
            val className = readIdentifier()
            cssClasses = listOf(className)
        }

        // Create or update vertex
        val existing = data.vertices[id]
        val vertex = if (existing != null) {
            existing.copy(
                label = if (label != id) label else existing.label,
                shape = if (shape != NodeShape.DEFAULT) shape else existing.shape,
                cssClasses = existing.cssClasses + cssClasses
            )
        } else {
            FlowVertex(id = id, label = label, shape = shape, cssClasses = cssClasses)
        }

        data.vertices[id] = vertex
        
        // Add to current subgraph or root
        if (subgraphStack.isNotEmpty()) {
            if (id !in subgraphStack.last().vertexIds) {
                subgraphStack.last().vertexIds.add(id)
            }
        } else {
            if (id !in data.rootVertexIds) {
                data.rootVertexIds.add(id)
            }
        }

        return vertex
    }

    private fun parseSquareBracketShape(): Pair<String, NodeShape> {
        advance() // [
        
        return when {
            peek() == '[' -> {
                advance() // second [
                val label = readUntil(']')
                advance() // first ]
                if (peek() == ']') advance() // second ]
                label to NodeShape.SUBROUTINE
            }
            peek() == '(' -> {
                advance() // (
                val label = readUntil(')')
                advance() // )
                if (peek() == ']') advance() // ]
                label to NodeShape.CYLINDER
            }
            peek() == '/' -> {
                advance() // /
                val label = readUntilOneOf("/\\")
                when {
                    peek() == '/' -> {
                        advance() // /
                        if (peek() == ']') advance()
                        label to NodeShape.PARALLELOGRAM
                    }
                    peek() == '\\' -> {
                        advance() // \
                        if (peek() == ']') advance()
                        label to NodeShape.TRAPEZOID
                    }
                    else -> {
                        if (peek() == ']') advance()
                        label to NodeShape.PARALLELOGRAM
                    }
                }
            }
            peek() == '\\' -> {
                advance() // \
                val label = readUntilOneOf("/\\")
                when {
                    peek() == '/' -> {
                        advance() // /
                        if (peek() == ']') advance()
                        label to NodeShape.TRAPEZOID_ALT
                    }
                    peek() == '\\' -> {
                        advance() // \
                        if (peek() == ']') advance()
                        label to NodeShape.PARALLELOGRAM_ALT
                    }
                    else -> {
                        if (peek() == ']') advance()
                        label to NodeShape.PARALLELOGRAM_ALT
                    }
                }
            }
            else -> {
                val label = readUntil(']')
                advance() // ]
                label to NodeShape.RECTANGLE
            }
        }
    }

    private fun parseParenShape(): Pair<String, NodeShape> {
        advance() // (
        
        return when {
            peek() == '(' -> {
                advance() // second (
                if (peek() == '(') {
                    advance() // third (
                    val label = readUntil(')')
                    advance() // first )
                    if (peek() == ')') advance() // second )
                    if (peek() == ')') advance() // third )
                    label to NodeShape.DOUBLE_CIRCLE
                } else {
                    val label = readUntil(')')
                    advance() // first )
                    if (peek() == ')') advance() // second )
                    label to NodeShape.CIRCLE
                }
            }
            peek() == '[' -> {
                advance() // [
                val label = readUntil(']')
                advance() // ]
                if (peek() == ')') advance() // )
                label to NodeShape.STADIUM
            }
            else -> {
                val label = readUntil(')')
                advance() // )
                label to NodeShape.ROUNDED
            }
        }
    }

    private fun parseBraceShape(): Pair<String, NodeShape> {
        advance() // {
        
        return when {
            peek() == '{' -> {
                advance() // second {
                val label = readUntil('}')
                advance() // first }
                if (peek() == '}') advance() // second }
                label to NodeShape.HEXAGON
            }
            else -> {
                val label = readUntil('}')
                advance() // }
                label to NodeShape.DIAMOND
            }
        }
    }

    private fun parseEdgeChain(firstNode: FlowVertex) {
        var sourceNode = firstNode
        
        while (!isAtEnd() && isEdgeStart()) {
            skipWhitespace()
            
            // Parse edge
            val edge = parseEdge(sourceNode.id)
            if (edge != null) {
                data.addEdge(edge)
                
                skipWhitespace()
                
                // Parse target node
                val targetNode = parseNode()
                if (targetNode != null) {
                    // Update edge with target
                    val updatedEdge = edge.copy(targetId = targetNode.id)
                    data.edges[data.edges.lastIndex] = updatedEdge
                    sourceNode = targetNode
                }
            }
        }
    }

    private fun parseEdge(sourceId: String): FlowEdge? {
        skipWhitespace()
        
        var linkType = LinkType.ARROW
        var label: String? = null
        var length = 1
        var startArrow = ArrowHead.NONE
        var endArrow = ArrowHead.ARROW
        
        // Check for start arrow markers
        if (peek() == '<') {
            advance()
            startArrow = ArrowHead.ARROW
        } else if (peek() == 'o') {
            advance()
            startArrow = ArrowHead.CIRCLE
        } else if (peek() == 'x') {
            advance()
            startArrow = ArrowHead.CROSS
        }
        
        // Parse link type
        when {
            checkSequence("-.") -> {
                advanceBy(2)
                linkType = LinkType.DOTTED
                // Count dots/dashes for length
                while (!isAtEnd() && (peek() == '.' || peek() == '-')) {
                    advance()
                }
                if (peek() == '>') {
                    advance()
                    linkType = LinkType.DOTTED_ARROW
                    endArrow = ArrowHead.ARROW
                } else {
                    endArrow = ArrowHead.NONE
                }
            }
            checkSequence("==") -> {
                advanceBy(2)
                linkType = LinkType.THICK
                // Count = for length
                while (!isAtEnd() && peek() == '=') {
                    advance()
                    length++
                }
                if (peek() == '>') {
                    advance()
                    linkType = LinkType.THICK_ARROW
                    endArrow = ArrowHead.ARROW
                } else {
                    endArrow = ArrowHead.NONE
                }
            }
            checkSequence("~~") -> {
                while (!isAtEnd() && peek() == '~') {
                    advance()
                }
                linkType = LinkType.INVISIBLE
                endArrow = ArrowHead.NONE
            }
            checkSequence("--") -> {
                advanceBy(2)
                linkType = LinkType.LINE
                // Count - for length
                while (!isAtEnd() && peek() == '-') {
                    advance()
                    length++
                }
                if (peek() == '>') {
                    advance()
                    linkType = LinkType.ARROW
                    endArrow = ArrowHead.ARROW
                } else {
                    endArrow = ArrowHead.NONE
                }
            }
        }
        
        // Check for end arrow markers
        if (peek() == 'o') {
            advance()
            endArrow = ArrowHead.CIRCLE
        } else if (peek() == 'x') {
            advance()
            endArrow = ArrowHead.CROSS
        }
        
        skipWhitespace()
        
        // Parse edge label |text|
        if (peek() == '|') {
            advance() // |
            label = readUntil('|')
            advance() // |
        }
        
        return FlowEdge(
            sourceId = sourceId,
            targetId = "", // Will be filled in by parseEdgeChain
            label = label,
            linkType = linkType,
            startArrow = startArrow,
            endArrow = endArrow,
            length = length
        )
    }

    // ===== Helper methods =====

    private fun isAtEnd(): Boolean = pos >= text.length

    private fun peek(): Char = if (isAtEnd()) '\u0000' else text[pos]

    private fun peekNext(): Char = if (pos + 1 >= text.length) '\u0000' else text[pos + 1]

    private fun peekAhead(offset: Int): Char = 
        if (pos + offset >= text.length) '\u0000' else text[pos + offset]

    private fun advance(): Char {
        val c = peek()
        pos++
        if (c == '\n') {
            line++
            column = 1
        } else {
            column++
        }
        return c
    }

    private fun advanceBy(count: Int) {
        repeat(count) { advance() }
    }

    private fun checkSequence(seq: String): Boolean {
        for (i in seq.indices) {
            if (peekAhead(i) != seq[i]) return false
        }
        return true
    }

    private fun isEndOfLine(): Boolean = peek() == '\n' || peek() == '\r'

    private fun isWhitespace(): Boolean = peek() == ' ' || peek() == '\t'

    private fun skipWhitespace() {
        while (!isAtEnd() && isWhitespace()) {
            advance()
        }
    }

    private fun skipWhitespaceAndComments() {
        while (!isAtEnd()) {
            when {
                isWhitespace() || isEndOfLine() -> advance()
                peek() == '%' && peekNext() == '%' -> skipComment()
                else -> break
            }
        }
    }

    private fun skipComment() {
        advance() // %
        advance() // %
        while (!isAtEnd() && !isEndOfLine()) {
            advance()
        }
    }

    private fun skipToEndOfLine() {
        while (!isAtEnd() && !isEndOfLine()) {
            advance()
        }
        if (!isAtEnd() && isEndOfLine()) {
            advance()
        }
    }

    private fun isEdgeStart(): Boolean {
        return when {
            peek() == '-' -> true
            peek() == '=' -> true
            peek() == '~' -> true
            peek() == '<' || peek() == 'o' || peek() == 'x' -> {
                peekNext() == '-' || peekNext() == '=' || peekNext() == '~'
            }
            else -> false
        }
    }

    private fun readWord(): String {
        val builder = StringBuilder()
        while (!isAtEnd() && (peek().isLetterOrDigit() || peek() == '_' || peek() == '-')) {
            builder.append(advance())
        }
        return builder.toString()
    }

    private fun peekWord(): String {
        var tempPos = pos
        val builder = StringBuilder()
        while (tempPos < text.length && (text[tempPos].isLetterOrDigit() || text[tempPos] == '_' || text[tempPos] == '-')) {
            builder.append(text[tempPos])
            tempPos++
        }
        return builder.toString()
    }

    private fun readIdentifier(): String {
        val builder = StringBuilder()
        
        // Handle quoted identifiers
        if (peek() == '"' || peek() == '\'') {
            return readString()
        }
        
        while (!isAtEnd() && (peek().isLetterOrDigit() || peek() == '_' || peek() == '-')) {
            builder.append(advance())
        }
        return builder.toString()
    }

    private fun readIdentifierOrString(): String {
        skipWhitespace()
        return if (peek() == '"' || peek() == '\'') {
            readString()
        } else {
            readIdentifier()
        }
    }

    private fun readString(): String {
        val quote = advance() // " or '
        val builder = StringBuilder()
        while (!isAtEnd() && peek() != quote) {
            if (peek() == '\\' && peekNext() == quote) {
                advance() // skip backslash
            }
            builder.append(advance())
        }
        if (!isAtEnd()) advance() // closing quote
        return builder.toString()
    }

    private fun readUntil(end: Char): String {
        val builder = StringBuilder()
        while (!isAtEnd() && peek() != end) {
            builder.append(advance())
        }
        return builder.toString().trim()
    }

    private fun readUntilOneOf(chars: String): String {
        val builder = StringBuilder()
        while (!isAtEnd() && peek() !in chars) {
            builder.append(advance())
        }
        return builder.toString().trim()
    }

    private fun readToEndOfLine(): String {
        val builder = StringBuilder()
        while (!isAtEnd() && !isEndOfLine()) {
            builder.append(advance())
        }
        return builder.toString()
    }

    private fun parseError(message: String): MermaidParseException {
        return MermaidParseException(message, line, column)
    }
}
