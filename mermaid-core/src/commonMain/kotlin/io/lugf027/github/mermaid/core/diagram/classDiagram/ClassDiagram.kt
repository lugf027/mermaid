package io.lugf027.github.mermaid.core.diagram.classDiagram

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * 类图 DiagramDefinition 组装 - 对标 mermaid-js classDiagram.ts
 *
 * 将类图的检测器、解析器、数据库、渲染器和样式生成器组装为完整定义。
 */
object ClassDiagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "classDiagram",
        detector = { text ->
            Regex("^\\s*classDiagram(?:-v2)?").containsMatchIn(text)
        },
        dbFactory = { ClassDb() },
        parser = ClassParser(),
        renderer = ClassRenderer(),
    )
}
