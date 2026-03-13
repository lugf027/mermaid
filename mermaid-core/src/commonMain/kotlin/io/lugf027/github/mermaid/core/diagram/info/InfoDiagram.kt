package io.lugf027.github.mermaid.core.diagram.info

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * Info 图表 DiagramDefinition 组装 - 对标 mermaid-js infoDiagram.ts
 */
object InfoDiagram {
    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "info",
        detector = { text -> Regex("^\\s*info").containsMatchIn(text) },
        dbFactory = { InfoDb() },
        parser = InfoParser(),
        renderer = InfoRenderer(),
        styles = { tv ->
            """
.version {
  font-size: 32px;
  fill: ${tv.textColor};
}
""".trimIndent()
        }
    )
}
