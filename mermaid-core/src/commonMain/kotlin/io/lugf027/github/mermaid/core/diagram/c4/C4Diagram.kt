package io.lugf027.github.mermaid.core.diagram.c4

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * C4 图 DiagramDefinition 组装 - 对标 mermaid-js c4Diagram.ts
 */
object C4Diagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "c4",
        detector = { text ->
            Regex("^\\s*C4(Context|Container|Component|Dynamic|Deployment)", RegexOption.IGNORE_CASE)
                .containsMatchIn(text)
        },
        dbFactory = { C4Db() },
        parser = C4Parser(),
        renderer = C4Renderer(),
        styles = { themeVariables ->
            """
.person {
  stroke: ${themeVariables.primaryBorderColor};
  fill: ${themeVariables.mainBkg};
}
""".trimIndent()
        }
    )
}
