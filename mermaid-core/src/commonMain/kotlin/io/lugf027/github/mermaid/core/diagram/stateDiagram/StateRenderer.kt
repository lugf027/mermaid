package io.lugf027.github.mermaid.core.diagram.stateDiagram

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
 * 状态图渲染器 - 对标 mermaid-js stateRenderer-v3-unified.ts
 *
 * 使用 Dagre 统一渲染模式，复用 ShapeRegistry + EdgeRenderer。
 */
class StateRenderer : DiagramRenderer {

    private val log = Logger("StateRenderer")

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val stateDb = db as? StateDb ?: throw IllegalArgumentException("Expected StateDb")

        // 1. 构建 LayoutData
        val layoutData = stateDb.getData(config)

        // 2. Dagre 布局
        val layout = DagreLayout()
        val laidOut = layout.layout(layoutData)

        // 3. 构建 SVG
        return buildSvg {
            attr("id", diagramId)
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("xmlns:xlink", "http://www.w3.org/1999/xlink")
            attr("role", "graphics-document document")
            attr("aria-roledescription", "stateDiagram")

            val titleText = stateDb.getDiagramTitle()
            if (titleText.isNotEmpty()) {
                title(titleText)
            }

            // 样式
            defs {
                style(generateStateStyles(themeVariables))
            }

            // 外层 group
            val outerGroup = group {}

            // markers
            Markers.addMarkers(outerGroup, diagramId, "stateDiagram")

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

    private fun generateStateStyles(tv: ThemeVariables): String = """
g.stateGroup text {
  fill: ${tv.nodeBorder};
  stroke: none;
  font-size: 10px;
}
g.stateGroup text {
  fill: ${tv.textColor};
  stroke: none;
  font-size: 10px;
}
g.stateGroup .state-title {
  font-weight: bolder;
  fill: ${tv.primaryTextColor};
}
g.stateGroup rect {
  fill: ${tv.mainBkg};
  stroke: ${tv.nodeBorder};
}
g.stateGroup line {
  stroke: ${tv.lineColor};
  stroke-width: 0.5;
}
.transition {
  stroke: ${tv.lineColor};
  stroke-width: 1;
  fill: none;
}
.stateGroup .composit {
  fill: ${tv.background};
  border-bottom: 1px;
}
.stateGroup .alt-composit {
  fill: #e0e0e0;
  border-bottom: 1px;
}
.state-note {
  stroke: ${tv.noteBorderColor};
  fill: ${tv.noteBkgColor};
}
.state-note text {
  fill: ${tv.noteTextColor};
  font-size: 10px;
}
.stateLabel .box {
  stroke: none;
  stroke-width: 0;
  fill: ${tv.mainBkg};
  opacity: 0.5;
}
.edgeLabel .label rect {
  fill: ${tv.tertiaryColor};
  opacity: 0.5;
}
.edgeLabel .label text {
  fill: ${tv.tertiaryTextColor};
}
.label div .edgeLabel {
  color: ${tv.tertiaryTextColor};
}
.stateLabel text {
  fill: ${tv.primaryTextColor};
  font-size: 10px;
  font-weight: bold;
}
.node circle.state-start {
  fill: ${tv.lineColor};
  stroke: ${tv.lineColor};
}
.node .fork-join {
  fill: ${tv.lineColor};
  stroke: ${tv.lineColor};
}
.node circle.state-end {
  fill: ${tv.primaryBorderColor};
  stroke: ${tv.background};
  stroke-width: 1.5;
}
.end-state-inner {
  fill: ${tv.background};
  stroke: ${tv.background};
}
.node rect {
  fill: ${tv.mainBkg};
  stroke: ${tv.nodeBorder};
  stroke-width: 1px;
}
.node polygon {
  fill: ${tv.mainBkg};
  stroke: ${tv.nodeBorder};
  stroke-width: 1px;
}
.statediagram-cluster rect {
  fill: ${tv.clusterBkg};
  stroke: ${tv.clusterBorder};
  stroke-width: 1px;
}
.cluster-label, .cluster-label span {
  fill: ${tv.textColor};
  color: ${tv.textColor};
}
.statediagram-cluster rect.outer {
  rx: 5px;
  ry: 5px;
}
.statediagram-state .divider {
  stroke: ${tv.lineColor};
}
.statediagram-state .title-state {
  rx: 5px;
  ry: 5px;
}
.statediagram-state rect.basic {
  rx: 5px;
  ry: 5px;
}
.statediagram-state rect.divider {
  stroke-dasharray: 10,10;
  fill: ${tv.clusterBkg};
}
.note-edge path {
  stroke-dasharray: 5;
}
.statediagram-note rect {
  fill: ${tv.noteBkgColor};
  stroke: ${tv.noteBorderColor};
  stroke-width: 1px;
  rx: 0;
  ry: 0;
}
.statediagram-note rect {
  fill: ${tv.noteBkgColor};
  stroke: ${tv.noteBorderColor};
  stroke-width: 1px;
  rx: 0;
  ry: 0;
}
.statediagram-note text {
  fill: ${tv.noteTextColor};
}
.statediagram-note .nodeLabel {
  color: ${tv.noteTextColor};
}
.statediagramTitleText {
  text-anchor: middle;
  font-size: 18px;
  fill: ${tv.textColor};
}
#statediagram-barbEnd {
  fill: ${tv.lineColor};
}
""".trimIndent()
}
