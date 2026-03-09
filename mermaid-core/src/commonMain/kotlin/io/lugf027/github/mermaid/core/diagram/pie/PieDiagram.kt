package io.lugf027.github.mermaid.core.diagram.pie

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * 饼图 DiagramDefinition 组装 - 对标 mermaid-js pieDiagram.ts
 *
 * 将饼图的检测器、解析器、数据库、渲染器和样式生成器组装为完整定义。
 */
object PieDiagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "pie",
        detector = { text ->
            Regex("^\\s*pie").containsMatchIn(text)
        },
        dbFactory = { PieDb() },
        parser = PieParser(),
        renderer = PieRenderer(),
        styles = { themeVariables ->
            """
.pieCircle {
  stroke: ${themeVariables.pieStrokeColor};
  stroke-width: ${themeVariables.pieStrokeWidth};
  opacity: ${themeVariables.pieOpacity};
}
.pieOuterCircle {
  stroke: ${themeVariables.pieOuterStrokeColor};
  stroke-width: ${themeVariables.pieOuterStrokeWidth};
  fill: none;
}
.pieTitleText {
  text-anchor: middle;
  font-size: ${themeVariables.pieTitleTextSize};
  fill: ${themeVariables.pieTitleTextColor};
  font-family: ${themeVariables.fontFamily};
}
.slice {
  font-family: ${themeVariables.fontFamily};
  fill: ${themeVariables.pieSectionTextColor};
  font-size: ${themeVariables.pieSectionTextSize};
}
.legend text {
  fill: ${themeVariables.pieLegendTextColor};
  font-family: ${themeVariables.fontFamily};
  font-size: ${themeVariables.pieLegendTextSize};
}
""".trimIndent()
        }
    )
}
