package io.lugf027.github.mermaid.core.diagram.sankey

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * 桑基图 DiagramDefinition 组装
 */
object SankeyDiagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "sankey",
        detector = { text ->
            Regex("^\\s*sankey(-beta)?\\b").containsMatchIn(text)
        },
        dbFactory = { SankeyDb() },
        parser = SankeyParser(),
        renderer = SankeyRenderer(),
        styles = { tv ->
            buildString {
                appendLine(".sankey .node rect { stroke: #333; }")
                appendLine(".sankey .link { stroke-opacity: 0.4; }")
                appendLine(".sankey text { fill: ${tv.textColor}; font-size: 12px; }")
            }
        }
    )
}
