package io.lugf027.github.mermaid.core.diagram.journey

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * Journey 图表 DiagramDefinition 组装 - 对标 mermaid-js journeyDiagram.ts
 */
object JourneyDiagram {
    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "journey",
        detector = { text -> Regex("^\\s*journey").containsMatchIn(text) },
        dbFactory = { JourneyDb() },
        parser = JourneyParser(),
        renderer = JourneyRenderer(),
        styles = { tv ->
            """
.label {
  font-family: ${tv.fontFamily};
  color: ${tv.textColor};
}
.mouth {
  stroke: #666;
}
line {
  stroke: ${tv.textColor};
}
.legend {
  fill: ${tv.textColor};
  font-family: ${tv.fontFamily};
}
.label text {
  fill: #333;
}
.face {
  fill: #FFF8DC;
  stroke: #999;
}
.node rect,
.node circle,
.node ellipse,
.node polygon,
.node path {
  fill: ${tv.mainBkg};
  stroke: ${tv.nodeBorder};
  stroke-width: 1px;
}
.arrowheadPath {
  fill: ${tv.lineColor};
}
.edgePath .path {
  stroke: ${tv.lineColor};
  stroke-width: 1.5px;
}
.flowchart-link {
  stroke: ${tv.lineColor};
  fill: none;
}
.task-type-0, .section-type-0 { fill: ${tv.primaryColor}; }
.task-type-1, .section-type-1 { fill: ${tv.secondaryColor}; }
.task-type-2, .section-type-2 { fill: ${tv.tertiaryColor}; }
.task-type-3, .section-type-3 { fill: ${tv.primaryColor}; }
.task-type-4, .section-type-4 { fill: ${tv.secondaryColor}; }
.task-type-5, .section-type-5 { fill: ${tv.tertiaryColor}; }
.task-type-6, .section-type-6 { fill: ${tv.primaryColor}; }
.actor-0 { fill: #8FBC8F; }
.actor-1 { fill: #7CFC00; }
.actor-2 { fill: #00FFFF; }
.actor-3 { fill: #20B2AA; }
.actor-4 { fill: #B0E0E6; }
.actor-5 { fill: #FFFFE0; }
""".trimIndent()
        }
    )
}
