package io.lugf027.github.mermaid.core.diagram.flowchartElk

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition
import io.lugf027.github.mermaid.core.diagram.flowchart.FlowchartDb
import io.lugf027.github.mermaid.core.diagram.flowchart.FlowchartParser

/**
 * Flowchart-ELK DiagramDefinition 组装
 *
 * 在 mermaid-js 中，flowchart-elk 使用 ELK 布局引擎替代 Dagre，
 * 但共享相同的解析器和数据库。在 KMP 中我们复用 Flowchart 的
 * Parser 和 Db，但使用 FlowchartElkRenderer（内部调用 ElkLayout）
 * 进行布局和渲染，实现与 mermaid-js ELK 渲染器像素级一致的输出。
 */
object FlowchartElkDiagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "flowchart-elk",
        detector = { text ->
            // 匹配 %%{init: {'flowchart': {'defaultRenderer': 'elk'}}}%% 或
            // 直接使用 flowchart-elk 关键字
            val elkDirective = Regex("""defaultRenderer['":\s]*elk""", RegexOption.IGNORE_CASE)
            val elkKeyword = Regex("""^\s*flowchart-elk""")
            elkDirective.containsMatchIn(text) || elkKeyword.containsMatchIn(text)
        },
        dbFactory = { FlowchartDb() },
        parser = FlowchartParser(),
        renderer = FlowchartElkRenderer(),
        styles = { tv ->
            buildString {
                appendLine(".flowchart-elk .node rect { fill: ${tv.mainBkg}; stroke: ${tv.nodeBorder}; }")
                appendLine(".flowchart-elk .edgePath path { stroke: ${tv.lineColor}; }")
            }
        }
    )
}
