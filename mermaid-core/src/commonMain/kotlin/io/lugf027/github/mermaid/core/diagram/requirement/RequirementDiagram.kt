package io.lugf027.github.mermaid.core.diagram.requirement

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * 需求图 DiagramDefinition 组装
 */
object RequirementDiagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "requirement",
        detector = { text ->
            Regex("^\\s*requirementDiagram\\b").containsMatchIn(text)
        },
        dbFactory = { RequirementDb() },
        parser = RequirementParser(),
        renderer = RequirementRenderer(),
        styles = { tv ->
            buildString {
                appendLine(".requirement-node rect { fill: #f9f9f9; stroke: ${tv.lineColor}; }")
                appendLine(".requirement-node text { fill: ${tv.textColor}; }")
                appendLine(".relation line { stroke: ${tv.lineColor}; }")
                appendLine(".relation text { fill: ${tv.textColor}; }")
            }
        }
    )
}
