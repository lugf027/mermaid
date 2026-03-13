package io.lugf027.github.mermaid.core.diagram.gitGraph

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * Git 图 DiagramDefinition 组装 - 对标 mermaid-js gitGraphDiagram.ts
 */
object GitGraphDiagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "gitGraph",
        detector = { text ->
            Regex("^\\s*gitGraph").containsMatchIn(text)
        },
        dbFactory = { GitGraphDb() },
        parser = GitGraphParser(),
        renderer = GitGraphRenderer(),
        styles = { themeVariables ->
            val colors = listOf(
                themeVariables.git0, themeVariables.git1, themeVariables.git2, themeVariables.git3,
                themeVariables.git4, themeVariables.git5, themeVariables.git6, themeVariables.git7
            )
            buildString {
                appendLine(".commit-id, .commit-msg, .branch-label { fill: lightgrey; color: lightgrey; font-family: 'trebuchet ms', verdana, arial, sans-serif; }")
                for (i in 0 until 8) {
                    val c = colors[i]
                    appendLine(".branch-label$i { fill: ${themeVariables.textColor}; }")
                    appendLine(".commit$i { stroke: $c; fill: $c; }")
                    appendLine(".commit-highlight$i { stroke: $c; fill: $c; }")
                    appendLine(".label$i { fill: $c; }")
                    appendLine(".arrow$i { stroke: $c; }")
                }
                appendLine(".branch { stroke-width: 1; stroke: ${themeVariables.lineColor}; stroke-dasharray: 2; }")
                appendLine(".commit-label { font-size: 10px; fill: ${themeVariables.secondaryTextColor}; }")
                appendLine(".commit-label-bkg { font-size: 10px; fill: ${themeVariables.secondaryColor}; opacity: 0.5; }")
                appendLine(".tag-label { font-size: 10px; fill: ${themeVariables.primaryTextColor}; }")
                appendLine(".tag-label-bkg { fill: ${themeVariables.primaryColor}; stroke: ${themeVariables.primaryBorderColor}; }")
                appendLine(".tag-hole { fill: ${themeVariables.textColor}; }")
                appendLine(".commit-merge { stroke: ${themeVariables.primaryColor}; fill: ${themeVariables.primaryColor}; }")
                appendLine(".commit-reverse { stroke: ${themeVariables.primaryColor}; fill: ${themeVariables.primaryColor}; stroke-width: 3; }")
                appendLine(".commit-highlight-outer { }")
                appendLine(".commit-highlight-inner { stroke: ${themeVariables.primaryColor}; fill: ${themeVariables.primaryColor}; }")
                appendLine(".arrow { stroke-width: 8; stroke-linecap: round; fill: none; }")
                appendLine(".gitTitleText { text-anchor: middle; font-size: 18px; fill: ${themeVariables.textColor}; }")
            }
        }
    )
}
