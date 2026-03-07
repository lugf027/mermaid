package io.lugf027.github.mermaid.core.diagrams.pie

import io.lugf027.github.mermaid.core.types.DiagramDB
import io.lugf027.github.mermaid.core.types.DiagramDefinition
import io.lugf027.github.mermaid.core.types.DiagramRenderer
import io.lugf027.github.mermaid.core.types.ParserDefinition

/**
 * 饼图定义 — 完整实现。
 * 使用 PieDb 存储数据、PieParser 解析语法、PieRenderer 绘制图表。
 */
class PieDiagramDefinition : DiagramDefinition {
    private val _db = PieDb()
    override val parser: ParserDefinition = PieParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = PieRenderer()
}
