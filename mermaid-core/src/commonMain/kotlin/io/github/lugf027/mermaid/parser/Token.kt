/**
 * Token - Represents a lexical token in Mermaid syntax.
 */
package io.github.lugf027.mermaid.parser

/**
 * Token types for Mermaid lexical analysis.
 */
public enum class TokenType {
    // Keywords
    FLOWCHART,
    GRAPH,
    SUBGRAPH,
    END,
    DIRECTION,
    CLASS_DEF,
    CLASS,
    CLICK,
    STYLE,
    LINK_STYLE,

    // Identifiers and literals
    IDENTIFIER,
    STRING,
    NUMBER,

    // Node shapes
    BRACKET_OPEN,      // [
    BRACKET_CLOSE,     // ]
    PAREN_OPEN,        // (
    PAREN_CLOSE,       // )
    BRACE_OPEN,        // {
    BRACE_CLOSE,       // }
    DOUBLE_BRACKET,    // [[
    DOUBLE_PAREN,      // ((
    DOUBLE_BRACE,      // {{
    STADIUM_OPEN,      // ([
    STADIUM_CLOSE,     // ])
    CYLINDER_OPEN,     // [(
    CYLINDER_CLOSE,    // )]
    ASYMMETRIC_OPEN,   // >
    PARALLELOGRAM_OPEN, // [/
    PARALLELOGRAM_CLOSE, // /]
    TRAPEZOID_OPEN,    // [/
    TRAPEZOID_CLOSE,   // \]
    TRIPLE_PAREN,      // (((
    
    // Arrows/Links
    ARROW,             // -->
    ARROW_OPEN,        // ---
    DOTTED_ARROW,      // -.->
    DOTTED_LINE,       // -.-
    THICK_ARROW,       // ==>
    THICK_LINE,        // ===
    INVISIBLE_LINK,    // ~~~
    ARROW_TEXT,        // |text|

    // Operators and punctuation
    PIPE,              // |
    AMPERSAND,         // &
    SEMICOLON,         // ;
    COLON,             // :
    COMMA,             // ,
    DOUBLE_COLON,      // ::

    // Special
    NEWLINE,
    COMMENT,
    DIRECTIVE,         // %%{...}%%
    EOF,
    UNKNOWN
}

/**
 * A single token from lexical analysis.
 */
public data class Token(
    /**
     * Type of this token.
     */
    val type: TokenType,

    /**
     * Raw text value.
     */
    val value: String,

    /**
     * Line number (1-based).
     */
    val line: Int,

    /**
     * Column number (1-based).
     */
    val column: Int
) {
    override fun toString(): String = "Token($type, '$value', $line:$column)"
}
