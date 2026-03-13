package io.lugf027.github.mermaid.core.diagram.stateDiagram

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * 状态图 DiagramDefinition 组装 - 对标 mermaid-js stateDiagram.ts
 *
 * 将状态图的检测器、解析器、数据库、渲染器和样式生成器组装为完整定义。
 */
object StateDiagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "stateDiagram",
        detector = { text ->
            Regex("^\\s*stateDiagram(?:-v2)?").containsMatchIn(text)
        },
        dbFactory = { StateDb() },
        parser = StateParser(),
        renderer = StateRenderer(),
    )
}
