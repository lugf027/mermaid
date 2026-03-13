package io.lugf027.github.mermaid.core.diagram.block

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * 块图 DiagramDefinition 组装 - 对标 mermaid-js blockDiagram.ts
 */
object BlockDiagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "block",
        detector = { text ->
            Regex("^\\s*block(-beta)?\\b").containsMatchIn(text)
        },
        dbFactory = { BlockDb() },
        parser = BlockParser(),
        renderer = BlockRenderer(),
        styles = { themeVariables ->
            buildString {
                appendLine(".node rect { fill: ${themeVariables.primaryColor}; stroke: ${themeVariables.primaryBorderColor}; stroke-width: 1px; }")
                appendLine(".node text { fill: ${themeVariables.primaryTextColor}; font-family: 'trebuchet ms', verdana, arial, sans-serif; font-size: 14px; }")
                appendLine(".node.composite rect { fill: ${themeVariables.mainBkg}; }")
                appendLine(".edge-path { stroke: ${themeVariables.lineColor}; stroke-width: 2px; fill: none; }")
                appendLine(".edge-label { fill: ${themeVariables.textColor}; font-size: 12px; }")
                appendLine(".block-title { fill: ${themeVariables.textColor}; font-size: 16px; }")
            }
        }
    )
}
