package io.lugf027.github.mermaid.core.diagram.flowchartElk

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition
import io.lugf027.github.mermaid.core.diagram.flowchart.FlowchartDb
import io.lugf027.github.mermaid.core.diagram.flowchart.FlowchartParser
import io.lugf027.github.mermaid.core.diagram.flowchart.FlowchartRenderer

/**
 * Flowchart-ELK DiagramDefinition 组装
 *
 * 在 mermaid-js 中，flowchart-elk 使用 ELK 布局引擎替代 Dagre，
 * 但共享相同的解析器和渲染逻辑。在 KMP 中我们复用 Flowchart 的
 * 完整实现（Db/Parser/Renderer），仅作为独立的图表类型注册，
 * 这样未来可以在布局阶段切换到 ELK 引擎。
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
        renderer = FlowchartRenderer(),
        styles = { tv ->
            buildString {
                appendLine(".flowchart-elk .node rect { fill: ${tv.mainBkg}; stroke: ${tv.nodeBorder}; }")
                appendLine(".flowchart-elk .edgePath path { stroke: ${tv.lineColor}; }")
            }
        }
    )
}
