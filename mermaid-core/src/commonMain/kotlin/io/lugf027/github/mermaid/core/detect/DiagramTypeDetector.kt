package io.lugf027.github.mermaid.core.detect

/**
 * 图表类型检测 - 对标 mermaid-js detectType.ts
 *
 * 遍历已注册的检测器，返回第一个匹配的图表类型。
 */
object DiagramTypeDetector {

    /**
     * 检测图表类型
     *
     * @param text 预处理后的 Mermaid 文本（已移除 frontmatter/指令/注释）
     * @return 图表类型 ID，如 "flowchart-v2", "pie" 等；未匹配时返回 "error"
     */
    fun detect(text: String): String {
        val detectors = DetectorRegistry.getAll()

        for (entry in detectors) {
            try {
                if (entry.detector(text)) {
                    return entry.id
                }
            } catch (_: Exception) {
                // 检测器执行出错，跳过
            }
        }

        return "error"
    }
}
