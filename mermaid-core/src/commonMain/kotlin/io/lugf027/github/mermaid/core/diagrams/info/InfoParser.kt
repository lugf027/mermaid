package io.lugf027.github.mermaid.core.diagrams.info

import io.lugf027.github.mermaid.core.types.ParserDefinition

class InfoParser(private val db: InfoDb) : ParserDefinition {
    override fun parse(input: String) { /* info 图不需要解析额外内容 */ }
}
