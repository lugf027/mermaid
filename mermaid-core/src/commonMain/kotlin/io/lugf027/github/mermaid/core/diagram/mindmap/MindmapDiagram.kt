package io.lugf027.github.mermaid.core.diagram.mindmap

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * Mindmap 图表 DiagramDefinition 组装 - 对标 mermaid-js mindmap-definition.ts
 */
object MindmapDiagram {
    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "mindmap",
        detector = { text -> Regex("^\\s*mindmap").containsMatchIn(text) },
        dbFactory = { MindmapDb() },
        parser = MindmapParser(),
        renderer = MindmapRenderer(),
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
                sb.appendLine(".section-$i rect, .section-$i path, .section-$i circle, .section-$i polygon { fill: ${colors[i]}; }")
                sb.appendLine(".section-$i text { fill: ${tv.textColor}; }")
                sb.appendLine(".section-edge-$i { stroke: ${colors[i]}; }")
                val edgeWidth = (17 - 3 * (i + 1)).coerceAtLeast(1)
                sb.appendLine(".edge-depth-$i { stroke-width: ${edgeWidth}; }")
                sb.appendLine(".section-$i line { stroke: ${tv.lineColor}; stroke-width: 3; }")
            }

            // 全局样式
            sb.appendLine("""
.edge { stroke-width: 3; fill: none; }
.section-root rect, .section-root path, .section-root circle, .section-root polygon { fill: ${tv.git0}; }
.section-root text { fill: ${tv.gitBranchLabel0}; }
.icon-container { height: 100%; display: flex; justify-content: center; align-items: center; }
.mindmap-node-label { dy: 1em; alignment-baseline: middle; text-anchor: middle; dominant-baseline: middle; text-align: center; }
.disabled, .disabled circle, .disabled text { fill: lightgray; }
.disabled text { fill: #efefef; }
""".trimIndent())

            sb.toString()
        }
    )
}
