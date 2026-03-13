package io.lugf027.github.mermaid.core.diagram.kanban

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * 看板图 DiagramDefinition 组装
 */
object KanbanDiagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "kanban",
        detector = { text ->
            Regex("^\\s*kanban\\b").containsMatchIn(text)
        },
        dbFactory = { KanbanDb() },
        parser = KanbanParser(),
        renderer = KanbanRenderer(),
        styles = { tv ->
            buildString {
                appendLine(".kanban-column rect { stroke: ${tv.lineColor}; }")
                appendLine(".kanban-section-title { fill: ${tv.textColor}; font-weight: bold; }")
                appendLine(".kanban-item rect { fill: white; stroke: ${tv.lineColor}; }")
                appendLine(".kanban-item-label { fill: ${tv.textColor}; }")
            }
        }
    )
}
