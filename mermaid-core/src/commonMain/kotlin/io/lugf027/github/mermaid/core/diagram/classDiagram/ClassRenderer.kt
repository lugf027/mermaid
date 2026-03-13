package io.lugf027.github.mermaid.core.diagram.classDiagram

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.layout.dagre.DagreLayout
import io.lugf027.github.mermaid.core.rendering.edges.EdgeRenderer
import io.lugf027.github.mermaid.core.rendering.markers.Markers
import io.lugf027.github.mermaid.core.rendering.shapes.ShapeRegistry
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.Logger

/**
 * 类图渲染器 - 对标 mermaid-js classRenderer-v3-unified.ts
 *
 * 使用 Dagre 统一渲染模式，复用 ShapeRegistry + EdgeRenderer。
 */
class ClassRenderer : DiagramRenderer {

    private val log = Logger("ClassRenderer")

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val classDb = db as? ClassDb ?: throw IllegalArgumentException("Expected ClassDb")

        // 1. 构建 LayoutData
        val layoutData = classDb.getData(config)

        // 2. Dagre 布局
        val layout = DagreLayout()
        val laidOut = layout.layout(layoutData)

        // 3. 构建 SVG
        return buildSvg {
            attr("id", diagramId)
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("xmlns:xlink", "http://www.w3.org/1999/xlink")
            attr("role", "graphics-document document")
            attr("aria-roledescription", "classDiagram")

            val titleText = classDb.getDiagramTitle()
            if (titleText.isNotEmpty()) {
                title(titleText)
            }

            // 样式
            defs {
                style(generateClassStyles(themeVariables))
            }

            // 外层 group
            val outerGroup = group {}

            // markers
            Markers.addMarkers(outerGroup, diagramId, "classDiagram")

            // 根 group
            val rootGroup = outerGroup.group {
                attr("id", "root")
            }

            // 边路径
            val edgePaths = rootGroup.group {
                addClass("edgePaths")
            }
            for (edge in laidOut.edges) {
                val edgePath = EdgeRenderer.renderPath(edge, diagramId, themeVariables)
                edgePaths.append(edgePath)
            }

            // 边标签
            val edgeLabels = rootGroup.group {
                addClass("edgeLabels")
            }
            for (edge in laidOut.edges) {
                val edgeLabel = EdgeRenderer.renderLabel(edge, diagramId, themeVariables)
                edgeLabels.append(edgeLabel)
            }

            // 节点
            val nodesGroup = rootGroup.group {
                addClass("nodes")
            }
            for (node in laidOut.nodes) {
                if (!node.isGroup) {
                    val nodeSvg = ShapeRegistry.render(node, themeVariables)
                    nodesGroup.append(nodeSvg)
                }
            }

            // 设置 viewBox
            val padding = laidOut.diagramPadding
            setupViewBox(laidOut, padding)
        }
    }

    private fun SvgRoot.setupViewBox(layoutData: io.lugf027.github.mermaid.core.layout.LayoutData, padding: Int) {
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = Double.MIN_VALUE
        var maxY = Double.MIN_VALUE

        for (node in layoutData.nodes) {
            val nx = node.x - node.width / 2
            val ny = node.y - node.height / 2
            minX = minOf(minX, nx)
            minY = minOf(minY, ny)
            maxX = maxOf(maxX, node.x + node.width / 2)
            maxY = maxOf(maxY, node.y + node.height / 2)
        }

        if (minX == Double.MAX_VALUE) {
            minX = 0.0; minY = 0.0; maxX = 100.0; maxY = 100.0
        }

        val w = maxX - minX + padding * 2
        val h = maxY - minY + padding * 2
        viewBox(minX - padding, minY - padding, w, h)
        attr("width", "100%")
        attr("style", "max-width: ${w.toInt()}px;")
        attr("height", "${h.toInt()}")
    }

    private fun generateClassStyles(tv: ThemeVariables): String = """
g.classGroup text {
  fill: ${tv.nodeBorder};
  fill: ${tv.primaryTextColor};
  stroke: none;
  font-family: ${tv.fontFamily};
  font-size: 10px;
}
g.classGroup text .title {
  font-weight: bolder;
}
g.classGroup rect {
  fill: ${tv.mainBkg};
  stroke: ${tv.nodeBorder};
}
g.classGroup line {
  stroke: ${tv.nodeBorder};
  stroke-width: 0.5;
}
.classLabel .box {
  stroke: none;
  stroke-width: 0;
  fill: ${tv.mainBkg};
  opacity: 0.5;
}
.classLabel .label {
  fill: ${tv.nodeBorder};
  font-size: 10px;
}
.relation {
  stroke: ${tv.lineColor};
  stroke-width: 1;
  fill: none;
}
.dashed-line {
  stroke-dasharray: 3;
}
.dotted-line {
  stroke-dasharray: 1 2;
}
#compositionStart, .composition {
  fill: ${tv.lineColor} !important;
  stroke: ${tv.lineColor} !important;
  stroke-width: 1;
}
#extensionStart, .extension {
  fill: transparent !important;
  stroke: ${tv.lineColor} !important;
  stroke-width: 1;
}
#aggregationStart, .aggregation {
  fill: transparent !important;
  stroke: ${tv.lineColor} !important;
  stroke-width: 1;
}
#dependencyStart, .dependency {
  fill: ${tv.lineColor} !important;
  stroke: ${tv.lineColor} !important;
  stroke-width: 1;
}
#lollipopStart, .lollipop {
  fill: ${tv.mainBkg} !important;
  stroke: ${tv.lineColor} !important;
  stroke-width: 1;
}
.edgeTerminals {
  font-size: 11px;
}
.classTitleText {
  text-anchor: middle;
  font-size: 18px;
  fill: ${tv.textColor};
}
.classDiagramTitleText {
  text-anchor: middle;
  font-size: 18px;
  fill: ${tv.textColor};
}
""".trimIndent()
}
