package io.lugf027.github.mermaid.core.diagram.info

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser

/**
 * Info 图表解析器 - 对标 mermaid-js infoParser.ts
 *
 * 极简解析器，仅需识别 "info" 关键字。
 */
class InfoParser : DiagramParser {
    override fun parse(text: String, db: DiagramDB) {
        val infoDb = db as? InfoDb ?: throw IllegalArgumentException("Expected InfoDb")
        infoDb.clear()
        // Info 图表不需要解析任何内容，只要检测到 "info" 就显示版本
    }
}
