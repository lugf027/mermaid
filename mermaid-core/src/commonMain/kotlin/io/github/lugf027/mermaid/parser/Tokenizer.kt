/**
 * Tokenizer - Lexical analyzer for Mermaid syntax.
 */
package io.github.lugf027.mermaid.parser

import io.github.lugf027.mermaid.MermaidParseException

/**
 * Lexical analyzer that converts Mermaid text into a stream of tokens.
 */
public class Tokenizer(
    private val input: String
) {
    private var pos: Int = 0
    private var line: Int = 1
    private var column: Int = 1
    private val tokens: MutableList<Token> = mutableListOf()

    /**
     * Tokenize the entire input and return all tokens.
     */
    public fun tokenize(): List<Token> {
        tokens.clear()
        pos = 0
        line = 1
        column = 1

        while (!isAtEnd()) {
            skipWhitespaceAndComments()
            if (!isAtEnd()) {
                val token = nextToken()
                if (token != null) {
                    tokens.add(token)
                }
            }
        }

        tokens.add(Token(TokenType.EOF, "", line, column))
        return tokens.toList()
    }

    private fun isAtEnd(): Boolean = pos >= input.length

    private fun peek(): Char = if (isAtEnd()) '\u0000' else input[pos]

    private fun peekNext(): Char = if (pos + 1 >= input.length) '\u0000' else input[pos + 1]

    private fun peekAhead(offset: Int): Char = 
        if (pos + offset >= input.length) '\u0000' else input[pos + offset]

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

    private fun skipWhitespaceAndComments() {
        while (!isAtEnd()) {
            when (peek()) {
                ' ', '\t', '\r' -> advance()
                '\n' -> {
                    advance()
                    // Optionally emit NEWLINE token for some diagram types
                }
                '%' -> {
                    if (peekNext() == '%') {
                        skipComment()
                    } else {
                        break
                    }
                }
                else -> break
            }
        }
    }

    private fun skipComment() {
        // Skip %% comment
        advance() // %
        advance() // %
        
        // Check for directive %%{...}%%
        if (peek() == '{') {
            skipDirective()
            return
        }

        // Single line comment - skip to end of line
        while (!isAtEnd() && peek() != '\n') {
            advance()
        }
    }

    private fun skipDirective() {
        // Skip %%{...}%%
        val startLine = line
        val startCol = column
        val builder = StringBuilder("%%{")
        advance() // {

        var braceCount = 1
        while (!isAtEnd() && braceCount > 0) {
            val c = advance()
            builder.append(c)
            when (c) {
                '{' -> braceCount++
                '}' -> braceCount--
            }
        }

        // Skip closing %%
        if (peek() == '%') {
            advance()
            builder.append('%')
        }
        if (peek() == '%') {
            advance()
            builder.append('%')
        }

        tokens.add(Token(TokenType.DIRECTIVE, builder.toString(), startLine, startCol))
    }

    private fun nextToken(): Token? {
        val startLine = line
        val startCol = column
        val c = peek()

        return when {
            c.isLetter() || c == '_' -> readIdentifierOrKeyword()
            c.isDigit() -> readNumber()
            c == '"' || c == '\'' -> readString(c)
            c == '`' -> readString(c)
            else -> readOperator()
        }
    }

    private fun readIdentifierOrKeyword(): Token {
        val startLine = line
        val startCol = column
        val builder = StringBuilder()

        while (!isAtEnd() && (peek().isLetterOrDigit() || peek() == '_' || peek() == '-')) {
            builder.append(advance())
        }

        val value = builder.toString()
        val type = when (value.lowercase()) {
            "flowchart" -> TokenType.FLOWCHART
            "graph" -> TokenType.GRAPH
            "subgraph" -> TokenType.SUBGRAPH
            "end" -> TokenType.END
            "direction" -> TokenType.DIRECTION
            "classdef" -> TokenType.CLASS_DEF
            "class" -> TokenType.CLASS
            "click" -> TokenType.CLICK
            "style" -> TokenType.STYLE
            "linkstyle" -> TokenType.LINK_STYLE
            "tb", "td", "bt", "lr", "rl" -> TokenType.DIRECTION
            else -> TokenType.IDENTIFIER
        }

        return Token(type, value, startLine, startCol)
    }

    private fun readNumber(): Token {
        val startLine = line
        val startCol = column
        val builder = StringBuilder()

        while (!isAtEnd() && (peek().isDigit() || peek() == '.')) {
            builder.append(advance())
        }

        return Token(TokenType.NUMBER, builder.toString(), startLine, startCol)
    }

    private fun readString(quote: Char): Token {
        val startLine = line
        val startCol = column
        val builder = StringBuilder()

        advance() // Skip opening quote

        while (!isAtEnd() && peek() != quote) {
            if (peek() == '\\' && peekNext() == quote) {
                advance() // Skip backslash
            }
            builder.append(advance())
        }

        if (!isAtEnd()) {
            advance() // Skip closing quote
        }

        return Token(TokenType.STRING, builder.toString(), startLine, startCol)
    }

    private fun readOperator(): Token {
        val startLine = line
        val startCol = column

        // Check multi-character operators first
        val twoChar = if (pos + 1 < input.length) input.substring(pos, pos + 2) else ""
        val threeChar = if (pos + 2 < input.length) input.substring(pos, pos + 3) else ""
        val fourChar = if (pos + 3 < input.length) input.substring(pos, pos + 4) else ""

        // Check for arrows with various lengths (-->, --->, ===>, etc.)
        return when {
            // Dotted arrows
            threeChar == "-.>" || fourChar.startsWith("-.->") -> {
                readDottedArrow(startLine, startCol)
            }
            twoChar == "-." -> {
                readDottedLine(startLine, startCol)
            }
            // Thick arrows
            threeChar == "==>" -> {
                advance(); advance(); advance()
                Token(TokenType.THICK_ARROW, "==>", startLine, startCol)
            }
            threeChar == "===" -> {
                advance(); advance(); advance()
                Token(TokenType.THICK_LINE, "===", startLine, startCol)
            }
            // Regular arrows
            threeChar == "-->" -> {
                readArrow(startLine, startCol)
            }
            threeChar == "---" -> {
                readLine(startLine, startCol)
            }
            twoChar == "--" -> {
                readArrow(startLine, startCol)
            }
            // Invisible link
            threeChar == "~~~" -> {
                advance(); advance(); advance()
                Token(TokenType.INVISIBLE_LINK, "~~~", startLine, startCol)
            }
            // Double brackets
            twoChar == "[[" -> {
                advance(); advance()
                Token(TokenType.DOUBLE_BRACKET, "[[", startLine, startCol)
            }
            twoChar == "]]" -> {
                advance(); advance()
                Token(TokenType.BRACKET_CLOSE, "]]", startLine, startCol)
            }
            // Triple parens
            threeChar == "(((" -> {
                advance(); advance(); advance()
                Token(TokenType.TRIPLE_PAREN, "(((", startLine, startCol)
            }
            threeChar == ")))" -> {
                advance(); advance(); advance()
                Token(TokenType.PAREN_CLOSE, ")))", startLine, startCol)
            }
            // Double parens
            twoChar == "((" -> {
                advance(); advance()
                Token(TokenType.DOUBLE_PAREN, "((", startLine, startCol)
            }
            twoChar == "))" -> {
                advance(); advance()
                Token(TokenType.PAREN_CLOSE, "))", startLine, startCol)
            }
            // Double braces
            twoChar == "{{" -> {
                advance(); advance()
                Token(TokenType.DOUBLE_BRACE, "{{", startLine, startCol)
            }
            twoChar == "}}" -> {
                advance(); advance()
                Token(TokenType.BRACE_CLOSE, "}}", startLine, startCol)
            }
            // Stadium shape
            twoChar == "([" -> {
                advance(); advance()
                Token(TokenType.STADIUM_OPEN, "([", startLine, startCol)
            }
            twoChar == "])" -> {
                advance(); advance()
                Token(TokenType.STADIUM_CLOSE, "])", startLine, startCol)
            }
            // Cylinder shape
            twoChar == "[(" -> {
                advance(); advance()
                Token(TokenType.CYLINDER_OPEN, "[(", startLine, startCol)
            }
            twoChar == ")]" -> {
                advance(); advance()
                Token(TokenType.CYLINDER_CLOSE, ")]", startLine, startCol)
            }
            // Parallelogram
            twoChar == "[/" -> {
                advance(); advance()
                Token(TokenType.PARALLELOGRAM_OPEN, "[/", startLine, startCol)
            }
            twoChar == "/]" -> {
                advance(); advance()
                Token(TokenType.PARALLELOGRAM_CLOSE, "/]", startLine, startCol)
            }
            twoChar == "[\\" -> {
                advance(); advance()
                Token(TokenType.TRAPEZOID_OPEN, "[\\", startLine, startCol)
            }
            twoChar == "\\]" -> {
                advance(); advance()
                Token(TokenType.TRAPEZOID_CLOSE, "\\]", startLine, startCol)
            }
            // Double colon
            twoChar == "::" -> {
                advance(); advance()
                Token(TokenType.DOUBLE_COLON, "::", startLine, startCol)
            }
            // Single character operators
            else -> readSingleCharOperator(startLine, startCol)
        }
    }

    private fun readArrow(startLine: Int, startCol: Int): Token {
        val builder = StringBuilder()
        
        // Read dashes
        while (!isAtEnd() && peek() == '-') {
            builder.append(advance())
        }
        
        // Check for arrow head
        if (peek() == '>') {
            builder.append(advance())
            return Token(TokenType.ARROW, builder.toString(), startLine, startCol)
        }
        
        return Token(TokenType.ARROW_OPEN, builder.toString(), startLine, startCol)
    }

    private fun readLine(startLine: Int, startCol: Int): Token {
        val builder = StringBuilder()
        
        // Read dashes
        while (!isAtEnd() && peek() == '-') {
            builder.append(advance())
        }
        
        // Check for arrow head
        if (peek() == '>') {
            builder.append(advance())
            return Token(TokenType.ARROW, builder.toString(), startLine, startCol)
        }
        
        return Token(TokenType.ARROW_OPEN, builder.toString(), startLine, startCol)
    }

    private fun readDottedArrow(startLine: Int, startCol: Int): Token {
        val builder = StringBuilder()
        
        // Read -.-
        while (!isAtEnd() && (peek() == '-' || peek() == '.')) {
            builder.append(advance())
        }
        
        // Check for arrow head
        if (peek() == '>') {
            builder.append(advance())
            return Token(TokenType.DOTTED_ARROW, builder.toString(), startLine, startCol)
        }
        
        return Token(TokenType.DOTTED_LINE, builder.toString(), startLine, startCol)
    }

    private fun readDottedLine(startLine: Int, startCol: Int): Token {
        val builder = StringBuilder()
        
        // Read -.
        while (!isAtEnd() && (peek() == '-' || peek() == '.')) {
            builder.append(advance())
        }
        
        // Check for arrow head
        if (peek() == '>') {
            builder.append(advance())
            return Token(TokenType.DOTTED_ARROW, builder.toString(), startLine, startCol)
        }
        
        return Token(TokenType.DOTTED_LINE, builder.toString(), startLine, startCol)
    }

    private fun readSingleCharOperator(startLine: Int, startCol: Int): Token {
        val c = advance()
        val type = when (c) {
            '[' -> TokenType.BRACKET_OPEN
            ']' -> TokenType.BRACKET_CLOSE
            '(' -> TokenType.PAREN_OPEN
            ')' -> TokenType.PAREN_CLOSE
            '{' -> TokenType.BRACE_OPEN
            '}' -> TokenType.BRACE_CLOSE
            '|' -> TokenType.PIPE
            '&' -> TokenType.AMPERSAND
            ';' -> TokenType.SEMICOLON
            ':' -> TokenType.COLON
            ',' -> TokenType.COMMA
            '>' -> TokenType.ASYMMETRIC_OPEN
            '\n' -> TokenType.NEWLINE
            else -> TokenType.UNKNOWN
        }
        return Token(type, c.toString(), startLine, startCol)
    }
}
