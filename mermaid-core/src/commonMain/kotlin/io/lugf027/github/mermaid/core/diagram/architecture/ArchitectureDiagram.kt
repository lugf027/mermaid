package io.lugf027.github.mermaid.core.diagram.architecture

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * 架构图 DiagramDefinition 组装
 */
object ArchitectureDiagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "architecture",
        detector = { text ->
            Regex("^\\s*architecture(-beta)?\\b").containsMatchIn(text)
        },
        dbFactory = { ArchitectureDb() },
        parser = ArchitectureParser(),
        renderer = ArchitectureRenderer(),
        styles = { tv ->
            buildString {
                appendLine(".service rect { fill: ${tv.mainBkg}; stroke: ${tv.lineColor}; }")
                appendLine(".service text { fill: ${tv.textColor}; }")
                appendLine(".edge line { stroke: ${tv.lineColor}; }")
                appendLine(".architecture-group rect { stroke: ${tv.lineColor}; }")
            }
        }
    )
}
