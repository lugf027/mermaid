package io.lugf027.github.mermaid.core.export

/**
 * 图表导出结果。
 */
data class ExportResult(
    /** 导出的图片字节数据（PNG 格式） */
    val pngBytes: ByteArray?,
    /** 导出是否成功 */
    val success: Boolean,
    /** 错误消息（如果失败） */
    val errorMessage: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExportResult) return false
        return success == other.success &&
                errorMessage == other.errorMessage &&
                pngBytes.contentEquals(other.pngBytes)
    }

    override fun hashCode(): Int {
        var result = pngBytes?.contentHashCode() ?: 0
        result = 31 * result + success.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        return result
    }
}

/**
 * 平台特定的图片导出接口。
 * 不同平台使用不同的 API 实现将 Compose Canvas 内容导出为 PNG 图片。
 */
expect object PlatformExporter {
    /**
     * 返回当前平台是否支持图片导出。
     */
    fun isExportSupported(): Boolean

    /**
     * 获取平台名称，用于 UI 显示。
     */
    fun getPlatformName(): String
}
