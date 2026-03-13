package io.lugf027.github.mermaid.core.diagram.treemap

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * 树形图 DiagramDefinition 组装
 */
object TreemapDiagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "treemap",
        detector = { text ->
            Regex("^\\s*treemap(-beta)?\\b").containsMatchIn(text)
        },
        dbFactory = { TreemapDb() },
        parser = TreemapParser(),
        renderer = TreemapRenderer(),
        styles = { tv ->
            buildString {
                appendLine(".treemap-node rect { stroke: #fff; stroke-width: 1; }")
                appendLine(".treemap-node text { fill: ${tv.textColor}; }")
            }
        }
    )
}
