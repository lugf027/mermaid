package io.lugf027.github.mermaid.core.parser

import io.lugf027.github.mermaid.core.types.MermaidError
import io.lugf027.github.mermaid.core.types.ErrorType

/**
 * 通用词法分析器基类。
 * 提供 Token 化、关键字匹配、字符串/数字字面量解析等基础能力。
 * 各图表解析器可继承此类或直接使用。
 */
open class Lexer(protected val input: String) {
    /** 当前位置 */
    protected var pos: Int = 0

    /** 当前行号（从 1 开始） */
    protected var line: Int = 1

    /** 当前列号（从 1 开始） */
    protected var column: Int = 1

    /** 已生成的 Token 列表 */
    protected val tokens = mutableListOf<Token>()

    /** 输入长度 */
    protected val length: Int = input.length

    /** 是否已到达末尾 */
    val isAtEnd: Boolean get() = pos >= length

    /** 当前字符 */
    protected fun current(): Char = if (pos < length) input[pos] else '\u0000'

    /** 前瞻 n 个字符 */
    protected fun peek(offset: Int = 0): Char {
        val idx = pos + offset
        return if (idx < length) input[idx] else '\u0000'
    }

    /** 前进一个字符 */
    protected fun advance(): Char {
        val c = current()
        pos++
        if (c == '\n') {
            line++
            column = 1
        } else {
            column++
        }
        return c
    }

    /** 跳过空白字符（不含换行） */
    protected fun skipWhitespace() {
        while (!isAtEnd && current().let { it == ' ' || it == '\t' || it == '\r' }) {
            advance()
        }
    }

    /** 跳过空白字符（含换行） */
    protected fun skipWhitespaceAndNewlines() {
        while (!isAtEnd && current().isWhitespace()) {
            advance()
        }
    }

    /** 跳过当前行剩余内容 */
    protected fun skipLine() {
        while (!isAtEnd && current() != '\n') advance()
        if (!isAtEnd) advance() // skip \n
    }

    /**
     * 匹配并消费指定字符串。
     * @return 是否匹配成功
     */
    protected fun match(expected: String, ignoreCase: Boolean = false): Boolean {
        if (pos + expected.length > length) return false
        val sub = input.substring(pos, pos + expected.length)
        if (sub.equals(expected, ignoreCase = ignoreCase)) {
            repeat(expected.length) { advance() }
            return true
        }
        return false
    }

    /**
     * 检查是否匹配（不消费）。
     */
    protected fun check(expected: String, ignoreCase: Boolean = false): Boolean {
        if (pos + expected.length > length) return false
        return input.substring(pos, pos + expected.length).equals(expected, ignoreCase = ignoreCase)
    }

    /**
     * 读取标识符（字母/数字/下划线/连字符）。
     */
    protected fun readIdentifier(): String {
        val start = pos
        while (!isAtEnd && (current().isLetterOrDigit() || current() == '_' || current() == '-')) {
            advance()
        }
        return input.substring(start, pos)
    }

    /**
     * 读取带引号的字符串。
     */
    protected fun readQuotedString(quote: Char = '"'): String {
        advance() // skip opening quote
        val sb = StringBuilder()
        while (!isAtEnd && current() != quote) {
            if (current() == '\\') {
                advance()
                when (current()) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    '\\' -> sb.append('\\')
                    else -> sb.append(current())
                }
            } else {
                sb.append(current())
            }
            advance()
        }
        if (!isAtEnd) advance() // skip closing quote
        return sb.toString()
    }

    /**
     * 读取数字（整数或浮点数）。
     */
    protected fun readNumber(): String {
        val start = pos
        if (current() == '-' || current() == '+') advance()
        while (!isAtEnd && current().isDigit()) advance()
        if (!isAtEnd && current() == '.') {
            advance()
            while (!isAtEnd && current().isDigit()) advance()
        }
        return input.substring(start, pos)
    }

    /**
     * 读取到行尾的文本。
     */
    protected fun readToEndOfLine(): String {
        val start = pos
        while (!isAtEnd && current() != '\n') advance()
        return input.substring(start, pos).trim()
    }

    /**
     * 读取到指定终止符的文本。
     */
    protected fun readUntil(terminator: Char): String {
        val start = pos
        while (!isAtEnd && current() != terminator) advance()
        return input.substring(start, pos)
    }

    /**
     * 添加 Token。
     */
    protected fun addToken(type: TokenType, value: String) {
        tokens.add(Token(type, value, line, column))
    }

    /**
     * 创建解析错误。
     */
    protected fun error(message: String): MermaidError {
        return MermaidError(
            message = message,
            line = line,
            column = column,
            type = ErrorType.PARSE_ERROR
        )
    }
}

/**
 * Token 类型枚举。
 */
enum class TokenType {
    // 基础类型
    IDENTIFIER,
    STRING,
    NUMBER,
    NEWLINE,
    EOF,

    // 分隔符
    COLON,
    SEMICOLON,
    COMMA,
    DOT,
    PIPE,
    HASH,
    AT,

    // 括号
    LPAREN,
    RPAREN,
    LBRACKET,
    RBRACKET,
    LBRACE,
    RBRACE,

    // 箭头/连接符
    ARROW,
    ARROW_DOTTED,
    ARROW_THICK,
    ARROW_OPEN,
    DASH,
    DOUBLE_DASH,

    // 特殊
    KEYWORD,
    OPERATOR,
    LABEL,
    COMMENT,
    DIRECTIVE,
    TEXT,

    // 各图表类型可扩展的自定义 token 类型
    CUSTOM
}

/**
 * Token 数据类。
 */
data class Token(
    val type: TokenType,
    val value: String,
    val line: Int = 0,
    val column: Int = 0
)
