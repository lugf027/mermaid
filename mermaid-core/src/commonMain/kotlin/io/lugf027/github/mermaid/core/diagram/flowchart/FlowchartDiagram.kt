package io.lugf027.github.mermaid.core.diagram.flowchart

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * 流程图 DiagramDefinition 组装 - 对标 mermaid-js flowDiagram.ts
 */
object FlowchartDiagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "flowchart-v2",
        detector = { text ->
            val flowchartRegex = Regex("^\\s*flowchart")
            val graphRegex = Regex("^\\s*graph")
            flowchartRegex.containsMatchIn(text) || graphRegex.containsMatchIn(text)
        },
        dbFactory = { FlowchartDb() },
        parser = FlowchartParser(),
        renderer = FlowchartRenderer(),
    )

    /** 旧版 flowchart 定义 */
    fun legacyDefinition(): DiagramDefinition = DiagramDefinition(
        id = "flowchart",
        detector = { text ->
            Regex("^\\s*graph").containsMatchIn(text)
        },
        dbFactory = { FlowchartDb() },
        parser = FlowchartParser(),
        renderer = FlowchartRenderer(),
    )
}
