/**
 * Exception types for the Mermaid library.
 */
package io.github.lugf027.mermaid

/**
 * Base exception for Mermaid library errors.
 */
public open class MermaidException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when parsing Mermaid syntax fails.
 */
public class MermaidParseException(
    message: String,
    public val line: Int = -1,
    public val column: Int = -1,
    cause: Throwable? = null
) : MermaidException(
    if (line >= 0) "Parse error at line $line, column $column: $message" else "Parse error: $message",
    cause
)

/**
 * Exception thrown when an unsupported diagram type is encountered.
 */
public class UnsupportedDiagramException(
    public val diagramType: String
) : MermaidException("Unsupported diagram type: $diagramType")

/**
 * Exception thrown when rendering fails.
 */
public class MermaidRenderException(
    message: String,
    cause: Throwable? = null
) : MermaidException("Render error: $message", cause)
