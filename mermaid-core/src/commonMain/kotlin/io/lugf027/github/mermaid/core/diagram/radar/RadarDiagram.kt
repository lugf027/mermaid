package io.lugf027.github.mermaid.core.diagram.radar

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * 雷达图 DiagramDefinition 组装
 */
object RadarDiagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "radar",
        detector = { text ->
            Regex("^\\s*radar(-beta)?\\b").containsMatchIn(text)
        },
        dbFactory = { RadarDb() },
        parser = RadarParser(),
        renderer = RadarRenderer(),
        styles = { tv ->
            buildString {
                appendLine(".radar-chart .grid path { stroke: ${tv.lineColor}; }")
                appendLine(".radar-chart .axes line { stroke: ${tv.lineColor}; }")
                appendLine(".radar-chart text { fill: ${tv.textColor}; }")
            }
        }
    )
}
