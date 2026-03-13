package io.lugf027.github.mermaid.core.diagram.venn

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * 韦恩图 DiagramDefinition 组装
 */
object VennDiagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "venn",
        detector = { text ->
            Regex("^\\s*venn(-beta)?\\b").containsMatchIn(text)
        },
        dbFactory = { VennDb() },
        parser = VennParser(),
        renderer = VennRenderer(),
        styles = { tv ->
            buildString {
                appendLine(".venn circle { stroke-width: 2; }")
                appendLine(".venn text { fill: ${tv.textColor}; }")
                appendLine(".venn-set circle { fill-opacity: 0.25; }")
            }
        }
    )
}
