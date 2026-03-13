package io.lugf027.github.mermaid.core.diagram.quadrantChart

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * 象限图 DiagramDefinition 组装
 */
object QuadrantDiagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "quadrantChart",
        detector = { text ->
            Regex("^\\s*quadrantChart").containsMatchIn(text)
        },
        dbFactory = { QuadrantDb() },
        parser = QuadrantParser(),
        renderer = QuadrantRenderer(),
        styles = { tv ->
            buildString {
                appendLine(".quadrants rect { stroke: ${tv.lineColor}; }")
                appendLine(".point { fill: ${tv.primaryColor}; }")
                appendLine(".point-label { fill: ${tv.textColor}; font-size: 12px; }")
            }
        }
    )
}
