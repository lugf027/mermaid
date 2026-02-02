/**
 * KMP Mermaid - Kotlin Multiplatform Mermaid Diagram Library
 * 
 * Global configuration and utilities for the Mermaid library.
 */
package io.github.lugf027.mermaid

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Global Mermaid library configuration.
 */
public object Mermaid {
    /**
     * Library version.
     */
    public const val VERSION: String = "0.1.0-SNAPSHOT"

    /**
     * Enable debug logging.
     */
    public var debugMode: Boolean = false

    /**
     * Default IO dispatcher for parsing operations.
     */
    public val ioDispatcher: CoroutineDispatcher = Dispatchers.Default

    /**
     * Log a debug message if debug mode is enabled.
     */
    internal fun log(tag: String, message: String) {
        if (debugMode) {
            println("[$tag] $message")
        }
    }
}
