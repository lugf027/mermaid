package io.lugf027.github.mermaid.core.diagram.flowchart

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.layout.*
import io.lugf027.github.mermaid.core.layout.dagre.DagreLayout
import io.lugf027.github.mermaid.core.rendering.clusters.ClusterRenderer
import io.lugf027.github.mermaid.core.rendering.edges.EdgeRenderer
import io.lugf027.github.mermaid.core.rendering.markers.Markers
import io.lugf027.github.mermaid.core.rendering.shapes.ShapeRegistry
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.themes.ThemeManager
import io.lugf027.github.mermaid.core.themes.ThemeVariables

/**
 * 流程图渲染器（统一渲染模式）- 对标 mermaid-js flowRenderer-v3-unified.ts
 *
 * 流程：db.getData() → LayoutData → dagre 布局 → SVG IR 构建
 */
class FlowchartRenderer : DiagramRenderer {

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val flowDb = db as FlowchartDb
        val layoutData = flowDb.getData(config)

        // 1. 执行布局
        val layout = LayoutRegistry.get("dagre") ?: DagreLayout()
        val laidOut = layout.layout(layoutData)

        // 2. 构建 SVG
        return buildSvg {
            attr("id", diagramId)
            attr("class", "flowchart")
            attr("role", "graphics-document document")
            attr("aria-roledescription", "flowchart-v2")

            // 生成样式 - style 直接在 SVG 根下（mermaid-js 风格）
            val css = ThemeManager.generateStyles(themeVariables, "flowchart", diagramId)
            style(css)

            // markers 在 defs 中
            defs {
                Markers.addMarkers(this, diagramId)
            }

            // 主内容组
            val contentGroup = group {
                addClass("output")

                // 渲染集群（子图）
                val clusterGroup = group { addClass("clusters") }
                for (node in laidOut.nodes) {
                    if (node.isGroup) {
                        val cluster = ClusterRenderer.render(node, themeVariables)
                        clusterGroup.append(cluster)
                    }
                }

                // 渲染边
                val edgeGroup = group { addClass("edges edgePath") }
                for (edge in laidOut.edges) {
                    val edgeSvg = EdgeRenderer.render(edge, diagramId, themeVariables)
                    edgeGroup.append(edgeSvg)
                }

                // 渲染节点
                val nodeGroup = group { addClass("nodes") }
                for (node in laidOut.nodes) {
                    if (!node.isGroup) {
                        val nodeSvg = ShapeRegistry.render(node, themeVariables)
                        nodeGroup.append(nodeSvg)
                    }
                }
            }

            // 添加标题
            val title = flowDb.getDiagramTitle()
            if (title.isNotEmpty()) {
                text(title, 0.0, 0.0) {
                    addClass("titleText")
                    attr("text-anchor", "middle")
                    attr("font-size", "18")
                    attr("fill", themeVariables.titleColor)
                }
            }

            // 计算 viewBox
            setupViewBox(this, laidOut)
        }
    }

    /** 根据布局结果设置 viewBox */
    private fun setupViewBox(root: SvgRoot, data: LayoutData) {
        if (data.nodes.isEmpty()) {
            root.viewBox(0.0, 0.0, 100.0, 100.0)
            return
        }

        val padding = data.diagramPadding.toDouble()
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = Double.MIN_VALUE
        var maxY = Double.MIN_VALUE

        for (node in data.nodes) {
            minX = minOf(minX, node.x - node.width / 2)
            minY = minOf(minY, node.y - node.height / 2)
            maxX = maxOf(maxX, node.x + node.width / 2)
            maxY = maxOf(maxY, node.y + node.height / 2)
        }

        val width = maxX - minX + padding * 2
        val height = maxY - minY + padding * 2

        root.viewBox(minX - padding, minY - padding, width, height)
        root.attr("width", width)
        root.attr("height", height)
        root.attr("style", "max-width: ${SvgElement.formatNumber(width)}px; background-color: white;")
    }
}
