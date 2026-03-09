package io.lugf027.github.mermaid.core.util

/**
 * KMP 兼容的日志系统 - 对标 mermaid-js logger.ts
 *
 * 日志级别：1=debug, 2=info, 3=warn, 4=error, 5=fatal (silent)
 */
class Logger(private val tag: String) {

    fun debug(message: String) {
        if (globalLevel <= DEBUG) println("[DEBUG][$tag] $message")
    }

    fun info(message: String) {
        if (globalLevel <= INFO) println("[INFO][$tag] $message")
    }

    fun warn(message: String) {
        if (globalLevel <= WARN) println("[WARN][$tag] $message")
    }

    fun error(message: String) {
        if (globalLevel <= ERROR) println("[ERROR][$tag] $message")
    }

    fun error(message: String, throwable: Throwable) {
        if (globalLevel <= ERROR) {
            println("[ERROR][$tag] $message: ${throwable.message}")
        }
    }

    companion object {
        const val DEBUG = 1
        const val INFO = 2
        const val WARN = 3
        const val ERROR = 4
        const val FATAL = 5

        /** 全局日志级别，默认 5 (silent) */
        var globalLevel: Int = FATAL

        fun setLogLevel(level: Int) {
            globalLevel = level.coerceIn(DEBUG, FATAL)
        }
    }
}
