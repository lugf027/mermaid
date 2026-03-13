package io.lugf027.github.mermaid.core.diagram.sequence

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * 时序图 DiagramDefinition 组装 - 对标 mermaid-js sequenceDiagram.ts
 *
 * 将时序图的检测器、解析器、数据库、渲染器和样式生成器组装为完整定义。
 */
object SequenceDiagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "sequence",
        detector = { text ->
            Regex("^\\s*sequenceDiagram").containsMatchIn(text)
        },
        dbFactory = { SequenceDb() },
        parser = SequenceParser(),
        renderer = SequenceRenderer(),
        styles = { themeVariables ->
            """
.actor {
  stroke: ${themeVariables.actorBorder};
  fill: ${themeVariables.actorBkg};
}
text.actor > tspan {
  fill: ${themeVariables.actorTextColor};
  stroke: none;
}
.actor-line {
  stroke: ${themeVariables.actorLineColor};
}
.messageLine0 {
  stroke-width: 1.5;
  stroke-dasharray: none;
  stroke: ${themeVariables.signalColor};
}
.messageLine1 {
  stroke-width: 1.5;
  stroke-dasharray: 2, 2;
  stroke: ${themeVariables.signalColor};
}
#arrowhead path {
  fill: ${themeVariables.signalColor};
  stroke: ${themeVariables.signalColor};
}
.sequenceNumber {
  fill: ${themeVariables.sequenceNumberColor};
}
#crosshead path {
  fill: ${themeVariables.signalColor};
  stroke: ${themeVariables.signalColor};
}
.messageText {
  fill: ${themeVariables.signalTextColor};
  stroke: none;
}
.labelBox {
  stroke: ${themeVariables.labelBoxBorderColor};
  fill: ${themeVariables.labelBoxBkgColor};
}
.labelText, .labelText > tspan {
  fill: ${themeVariables.loopTextColor};
  stroke: none;
}
.loopText, .loopText > tspan {
  fill: ${themeVariables.loopTextColor};
  stroke: none;
}
.loopLine {
  stroke-width: 2px;
  stroke-dasharray: 2, 2;
  stroke: ${themeVariables.labelBoxBorderColor};
  fill: ${themeVariables.labelBoxBkgColor};
}
.note {
  stroke: ${themeVariables.noteBorderColor};
  fill: ${themeVariables.noteBkgColor};
}
.noteText, .noteText > tspan {
  fill: ${themeVariables.noteTextColor};
  stroke: none;
}
.activation0 {
  fill: ${themeVariables.activationBkgColor};
  stroke: ${themeVariables.activationBorderColor};
}
.activation1 {
  fill: ${themeVariables.activationBkgColor};
  stroke: ${themeVariables.activationBorderColor};
}
.activation2 {
  fill: ${themeVariables.activationBkgColor};
  stroke: ${themeVariables.activationBorderColor};
}
.actor-man line, .actor-man circle {
  stroke: ${themeVariables.actorBorder};
  fill: ${themeVariables.actorBkg};
  stroke-width: 2px;
}
""".trimIndent()
        }
        // Note: MermaidConfig 是不可变 data class，init 回调无法修改 config 属性。
        // wrap 的传递在渲染器中通过直接读取 config.wrap 和 config.sequence?.wrap 实现。
    )
}
