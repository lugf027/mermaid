package io.lugf027.github.mermaid.core.diagram.timeline

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * Timeline 图表 DiagramDefinition 组装 - 对标 mermaid-js timeline-definition.ts
 */
object TimelineDiagram {
    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "timeline",
        detector = { text -> Regex("^\\s*timeline").containsMatchIn(text) },
        dbFactory = { TimelineDb() },
        parser = TimelineParser(),
        renderer = TimelineRenderer(),
        styles = { tv ->
            val sb = StringBuilder()

            // Section 颜色循环
            val colors = listOf(
                tv.primaryColor, tv.secondaryColor, tv.tertiaryColor,
                tv.primaryColor, tv.secondaryColor, tv.tertiaryColor,
                tv.primaryColor, tv.secondaryColor, tv.tertiaryColor,
                tv.primaryColor, tv.secondaryColor, tv.tertiaryColor
            )
            for (i in colors.indices) {
                sb.appendLine(".section-$i rect, .section-$i path, .section-$i circle, .section-$i polygon, .section-$i path { fill: ${colors[i]}; }")
                sb.appendLine(".section-$i text { fill: ${tv.textColor}; }")
                sb.appendLine(".node-icon-$i { font-size: 40px; color: ${tv.textColor}; }")
                sb.appendLine(".section-edge-$i { stroke: ${colors[i]}; }")
                val edgeWidth = (17 - 3 * (i + 1)).coerceAtLeast(1)
                sb.appendLine(".edge-depth-$i { stroke-width: ${edgeWidth}; }")
                sb.appendLine(".section-$i line { stroke: ${tv.lineColor}; stroke-width: 3; }")
                sb.appendLine(".lineWrapper line { stroke: ${tv.textColor}; }")
            }

            sb.appendLine("""
.edge { stroke-width: 3; fill: none; }
.section-root rect, .section-root path, .section-root circle { fill: ${tv.git0}; }
.section-root text { fill: ${tv.gitBranchLabel0}; }
.icon-container { height: 100%; display: flex; justify-content: center; align-items: center; }
.disabled, .disabled circle, .disabled text { fill: lightgray; }
.disabled text { fill: #efefef; }
.eventWrapper { filter: brightness(120%); }
""".trimIndent())

            sb.toString()
        }
    )
}
