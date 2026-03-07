package io.lugf027.github.mermaid.core.parser

import io.lugf027.github.mermaid.core.types.ErrorType
import io.lugf027.github.mermaid.core.types.MermaidError

/**
 * 递归下降解析器基类。
 * 提供 expect/match/peek/advance 等解析原语和错误恢复机制。
 * 各图表解析器继承此类实现具体语法解析。
 */
abstract class ParserBase(protected val tokens: List<Token>) {

    /** 当前 token 索引 */
    protected var current: Int = 0

    /** 是否已到达末尾 */
    protected val isAtEnd: Boolean
        get() = current >= tokens.size || peek().type == TokenType.EOF

    /**
     * 查看当前 token（不消费）。
     */
    protected fun peek(): Token {
        return if (current < tokens.size) tokens[current]
        else Token(TokenType.EOF, "", 0, 0)
    }

    /**
     * 查看下一个 token（不消费）。
     */
    protected fun peekNext(): Token {
        return if (current + 1 < tokens.size) tokens[current + 1]
        else Token(TokenType.EOF, "", 0, 0)
    }

    /**
     * 消费并返回当前 token，前进到下一个。
     */
    protected fun advance(): Token {
        val token = peek()
        if (!isAtEnd) current++
        return token
    }

    /**
     * 检查当前 token 是否为指定类型。
     */
    protected fun check(type: TokenType): Boolean {
        return !isAtEnd && peek().type == type
    }

    /**
     * 检查当前 token 是否为指定类型且值匹配。
     */
    protected fun check(type: TokenType, value: String): Boolean {
        return !isAtEnd && peek().type == type && peek().value == value
    }

    /**
     * 如果当前 token 匹配指定类型则消费它，否则返回 false。
     */
    protected fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    /**
     * 期望当前 token 为指定类型，否则报错。
     */
    protected fun expect(type: TokenType, errorMessage: String = ""): Token {
        if (check(type)) return advance()
        val token = peek()
        throw IllegalStateException(
            MermaidError(
                message = errorMessage.ifEmpty {
                    "Expected $type but got ${token.type}('${token.value}')"
                },
                line = token.line,
                column = token.column,
                type = ErrorType.PARSE_ERROR
            ).toString()
        )
    }

    /**
     * 跳过连续的换行 token。
     */
    protected fun skipNewlines() {
        while (check(TokenType.NEWLINE)) advance()
    }

    /**
     * 跳过可选的分号和换行。
     */
    protected fun skipSeparators() {
        while (check(TokenType.NEWLINE) || check(TokenType.SEMICOLON)) {
            advance()
        }
    }

    /**
     * 尝试解析，如果失败则回退到当前位置。
     * @return 解析结果，如果失败则返回 null
     */
    protected fun <T> tryParse(block: () -> T): T? {
        val savedPos = current
        return try {
            block()
        } catch (e: Exception) {
            current = savedPos
            null
        }
    }

    /**
     * 收集指定类型之间的所有 token 值。
     */
    protected fun collectUntil(endType: TokenType): List<Token> {
        val result = mutableListOf<Token>()
        while (!isAtEnd && !check(endType)) {
            result.add(advance())
        }
        return result
    }

    /**
     * 解析入口（子类实现）。
     */
    abstract fun parse()
}
