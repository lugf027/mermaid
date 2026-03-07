package io.lugf027.github.mermaid.core.export

actual object PlatformExporter {
    actual fun isExportSupported(): Boolean = true
    actual fun getPlatformName(): String = "Web (JS)"
}
