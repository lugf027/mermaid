package io.lugf027.github.mermaid.core.diagram.er

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
 * ER 图渲染器 - 对标 mermaid-js erRenderer-unified.ts
 *
 * 使用 Dagre 统一渲染模式，复用 ShapeRegistry + EdgeRenderer。
 */
class ErRenderer : DiagramRenderer {

    private val log = Logger("ErRenderer")

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val erDb = db as? ErDb ?: throw IllegalArgumentException("Expected ErDb")

        // 1. 构建 LayoutData
        val layoutData = erDb.getData(config)

        // 2. Dagre 布局
        val layout = DagreLayout()
        val laidOut = layout.layout(layoutData)

        // 3. 构建 SVG
        return buildSvg {
            attr("id", diagramId)
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("xmlns:xlink", "http://www.w3.org/1999/xlink")
            attr("role", "graphics-document document")
            attr("aria-roledescription", "er")

            val titleText = erDb.getDiagramTitle()
            if (titleText.isNotEmpty()) {
                title(titleText)
            }

            // 样式
            defs {
                style(generateErStyles(themeVariables))
            }

            // 外层 group
            val outerGroup = group {}

            // markers
            Markers.addMarkers(outerGroup, diagramId, "erDiagram")

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

    private fun generateErStyles(tv: ThemeVariables): String = """
.er.entityBox {
  fill: ${tv.mainBkg};
  stroke: ${tv.nodeBorder};
}
.er.entityLabel {
  fill: ${tv.primaryTextColor};
}
.er.attributeBoxOdd {
  fill: ${tv.background};
  stroke: ${tv.nodeBorder};
}
.er.attributeBoxEven {
  fill: ${tv.primaryColor};
  stroke: ${tv.nodeBorder};
}
.er.relationshipLine {
  stroke: ${tv.lineColor};
}
.er.relationshipLabel {
  fill: ${tv.textColor};
}
""".trimIndent()
}
