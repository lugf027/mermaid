package io.lugf027.github.mermaid.core.diagram.er

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * ER 图 DiagramDefinition 组装 - 对标 mermaid-js erDiagram.ts
 *
 * 将 ER 图的检测器、解析器、数据库、渲染器和样式生成器组装为完整定义。
 */
object ErDiagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "er",
        detector = { text ->
            Regex("^\\s*erDiagram").containsMatchIn(text)
        },
        dbFactory = { ErDb() },
        parser = ErParser(),
        renderer = ErRenderer(),
        styles = { themeVariables ->
            """
.er.entityBox {
  fill: ${themeVariables.mainBkg};
  stroke: ${themeVariables.nodeBorder};
}
.er.entityLabel {
  fill: ${themeVariables.primaryTextColor};
}
.er.attributeBoxOdd {
  fill: ${themeVariables.background};
  stroke: ${themeVariables.nodeBorder};
}
.er.attributeBoxEven {
  fill: ${themeVariables.primaryColor};
  stroke: ${themeVariables.nodeBorder};
}
.er.relationshipLine {
  stroke: ${themeVariables.lineColor};
}
.er.relationshipLabel {
  fill: ${themeVariables.textColor};
}
""".trimIndent()
        }
    )
}
