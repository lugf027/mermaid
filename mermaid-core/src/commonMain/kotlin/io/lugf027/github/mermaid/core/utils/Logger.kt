package io.lugf027.github.mermaid.core.utils

/**
 * 日志系统。
 * 支持 debug/info/warn/error 四级别。
 */
object Logger {
    /** 日志级别 */
    enum class Level(val value: Int) {
        DEBUG(1), INFO(2), WARN(3), ERROR(4), SILENT(5)
    }

    var level: Level = Level.WARN

    fun debug(tag: String, message: String) {
        if (level.value <= Level.DEBUG.value) {
            println("[DEBUG][$tag] $message")
        }
    }

    fun info(tag: String, message: String) {
        if (level.value <= Level.INFO.value) {
            println("[INFO][$tag] $message")
        }
    }

    fun warn(tag: String, message: String) {
        if (level.value <= Level.WARN.value) {
            println("[WARN][$tag] $message")
        }
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        if (level.value <= Level.ERROR.value) {
            println("[ERROR][$tag] $message")
            throwable?.printStackTrace()
        }
    }
}
