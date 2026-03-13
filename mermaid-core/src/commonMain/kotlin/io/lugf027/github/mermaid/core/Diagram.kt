package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer

/**
 * Diagram 数据类 - 对标 mermaid-js Diagram.ts
 *
 * 持有一次解析的完整上下文：图表类型、原始文本、数据库、解析器、渲染器。
 */
data class Diagram(
    /** 图表类型 ID，如 "flowchart-v2", "pie" */
    val type: String,

    /** 原始输入文本（预处理前） */
    val text: String,

    /** 预处理后的代码文本 */
    val code: String,

    /** 图表数据库实例（存储解析结果） */
    val db: DiagramDB,

    /** 解析器引用 */
    val parser: DiagramParser,

    /** 渲染器引用 */
    val renderer: DiagramRenderer,

    /** 合并后的配置 */
    val config: MermaidConfig
) {
    /** 图表标题 */
    val title: String get() = db.getDiagramTitle()

    /** 无障碍标题 */
    val accTitle: String get() = db.getAccTitle()

    /** 无障碍描述 */
    val accDescription: String get() = db.getAccDescription()
}
