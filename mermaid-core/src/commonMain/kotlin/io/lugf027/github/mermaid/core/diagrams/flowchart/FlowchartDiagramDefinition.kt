package io.lugf027.github.mermaid.core.diagrams.flowchart

import io.lugf027.github.mermaid.core.types.DiagramDB
import io.lugf027.github.mermaid.core.types.DiagramDefinition
import io.lugf027.github.mermaid.core.types.DiagramRenderer
import io.lugf027.github.mermaid.core.types.ParserDefinition

/**
 * 流程图定义 — 完整实现。
 * 使用 FlowDb 存储数据、FlowParser 解析语法、FlowRenderer 绘制图表。
 */
class FlowchartDiagramDefinition : DiagramDefinition {
    private val _db = FlowDb()
    override val parser: ParserDefinition = FlowParser(_db)
    override val db: DiagramDB = _db
    override val renderer: DiagramRenderer = FlowRenderer()
}
