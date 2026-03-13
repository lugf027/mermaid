package io.lugf027.github.mermaid.core.diagram.xychart

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * XY 图表 DiagramDefinition 组装
 */
object XYChartDiagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "xychart",
        detector = { text ->
            Regex("^\\s*xychart(-beta)?\\b").containsMatchIn(text)
        },
        dbFactory = { XYChartDb() },
        parser = XYChartParser(),
        renderer = XYChartRenderer(),
        styles = { tv ->
            buildString {
                appendLine(".background { fill: ${tv.mainBkg}; }")
                appendLine(".chart-title { fill: ${tv.textColor}; font-size: 16px; }")
            }
        }
    )
}
