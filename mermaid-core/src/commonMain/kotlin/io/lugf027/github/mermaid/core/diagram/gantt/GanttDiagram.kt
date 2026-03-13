package io.lugf027.github.mermaid.core.diagram.gantt

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * 甘特图 DiagramDefinition 组装 - 对标 mermaid-js ganttDiagram.ts
 *
 * 将甘特图的检测器、解析器、数据库、渲染器和样式生成器组装为完整定义。
 */
object GanttDiagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "gantt",
        detector = { text ->
            Regex("^\\s*gantt").containsMatchIn(text)
        },
        dbFactory = { GanttDb() },
        parser = GanttParser(),
        renderer = GanttRenderer(),
        styles = { themeVariables ->
            """
.exclude-range {
  fill: ${themeVariables.excludeBkgColor};
  opacity: 0.3;
}
.section {
  opacity: 0.2;
}
.task {
  stroke-width: 2;
}
.milestone {
  transform: rotate(45deg) scale(0.8, 0.8);
}
.milestoneText {
  font-style: italic;
}
.grid .tick line {
  stroke: ${themeVariables.gridColor};
  opacity: 0.3;
  shape-rendering: crispEdges;
}
.grid .tick text {
  fill: ${themeVariables.textColor};
}
.grid path {
  stroke-width: 0;
}
.today line {
  stroke: ${themeVariables.todayLineColor};
  stroke-width: 2;
}
.titleText {
  text-anchor: middle;
  font-size: 18px;
  fill: ${themeVariables.titleColor};
  font-family: 'trebuchet ms', verdana, arial, sans-serif;
}
""".trimIndent()
        }
    )
}
