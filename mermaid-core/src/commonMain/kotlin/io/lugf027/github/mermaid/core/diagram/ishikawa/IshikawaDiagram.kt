package io.lugf027.github.mermaid.core.diagram.ishikawa

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * 鱼骨图 DiagramDefinition 组装
 */
object IshikawaDiagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "ishikawa",
        detector = { text ->
            Regex("^\\s*ishikawa(-beta)?\\b").containsMatchIn(text)
        },
        dbFactory = { IshikawaDb() },
        parser = IshikawaParser(),
        renderer = IshikawaRenderer(),
        styles = { tv ->
            buildString {
                appendLine(".ishikawa line { stroke: ${tv.lineColor}; }")
                appendLine(".ishikawa text { fill: ${tv.textColor}; }")
                appendLine(".head rect { fill: ${tv.mainBkg}; stroke: ${tv.lineColor}; }")
            }
        }
    )
}
