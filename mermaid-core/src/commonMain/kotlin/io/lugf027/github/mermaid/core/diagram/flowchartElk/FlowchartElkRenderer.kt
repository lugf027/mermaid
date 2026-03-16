package io.lugf027.github.mermaid.core.diagram.flowchartElk

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.diagram.flowchart.FlowchartDb
import io.lugf027.github.mermaid.core.layout.*
import io.lugf027.github.mermaid.core.layout.elk.ElkLayout
import io.lugf027.github.mermaid.core.rendering.clusters.ClusterRenderer
import io.lugf027.github.mermaid.core.rendering.edges.EdgeRenderer
import io.lugf027.github.mermaid.core.rendering.markers.Markers
import io.lugf027.github.mermaid.core.rendering.shapes.ShapeRegistry
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.themes.ThemeManager
import io.lugf027.github.mermaid.core.themes.ThemeVariables

/**
 * Flowchart-ELK 渲染器 - 使用 ELK 布局引擎的流程图渲染器
 *
 * 与 FlowchartRenderer 的区别：
 * - 使用 ElkLayout 替代 DagreLayout 进行布局计算
 * - SVG 结构与 FlowchartRenderer 完全一致（clusters + edgePaths + edgeLabels + nodes）
 *
 * 对标 mermaid-js:
 * - 解析/DB 层：复用 FlowchartParser + FlowchartDb
 * - 布局层：ElkLayout（对标 @mermaid-js/layout-elk render.ts）
 * - 渲染层：与 FlowchartRenderer 相同的 SVG 构建逻辑
 */
class FlowchartElkRenderer : DiagramRenderer {

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val flowDb = db as FlowchartDb
        val layoutData = flowDb.getData(config)

        // 1. 使用 ELK 布局（替代 Dagre）
        val elkLayout = ElkLayout()
        val laidOut = elkLayout.layout(layoutData)

        // 2. 构建 SVG — 精确匹配 mermaid-js ELK 渲染器的 SVG 结构
        // JS ELK 组顺序: subgraphs → nodes → edgeLabels → edges (edgePaths)
        return buildSvg {
            // mermaid-js SVG 属性顺序: id, width, xmlns, xmlns:xlink, class, style, viewBox, role, aria-*
            attr("id", diagramId)
            attr("width", "100%")
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("xmlns:xlink", "http://www.w3.org/1999/xlink")
            attr("class", "flowchart")
            // style 和 viewBox 由 setupViewBox 设置（在 role/aria 之前占位）

            // 生成样式 - style 直接在 SVG 根下
            val css = ThemeManager.generateStyles(themeVariables, "flowchart", diagramId)
            style(css)

            // 外层 <g> 包含 markers
            val outerGroup = group {}

            // markers — ELK 使用 "flowchart-elk" 类型
            // JS flowchart 只注册基础的 3 种 markers: point, circle, cross
            Markers.addBasicMarkers(outerGroup, diagramId, "flowchart-elk")

            // JS ELK 渲染器结构：subgraphs → nodes → edges → edgeLabels
            // (对标 render.ts 第 780-790 行 + addEdges 中的 edgeLabels 创建)

            // 渲染子图（subgraphs）— 对标 mermaid-js class="subgraphs"
            val subgraphGroup = group { addClass("subgraphs") }
            for (node in laidOut.nodes) {
                if (node.isGroup) {
                    val cluster = ClusterRenderer.render(node, themeVariables)
                    subgraphGroup.append(cluster)
                }
            }

            // 渲染节点 — nodes 在 edges 之前（与 Dagre 版不同）
            val nodeGroup = group { addClass("nodes") }
            for (node in laidOut.nodes) {
                if (!node.isGroup) {
                    val nodeSvg = ShapeRegistry.render(node, themeVariables)
                    nodeGroup.append(nodeSvg)
                }
            }

            // 渲染边路径 — 对标 mermaid-js class="edges edgePaths"（在 edgeLabels 之前）
            val edgePathGroup = group {
                addClass("edges")
                addClass("edgePaths")
            }
            for (edge in laidOut.edges) {
                val edgePath = EdgeRenderer.renderPath(edge, diagramId, themeVariables, "flowchart-elk")
                edgePathGroup.append(edgePath)
            }

            // 渲染边标签 — edgeLabels 在 edges 之后
            val edgeLabelGroup = group { addClass("edgeLabels") }
            for (edge in laidOut.edges) {
                val edgeLabel = EdgeRenderer.renderLabel(edge, diagramId, themeVariables)
                edgeLabelGroup.append(edgeLabel)
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

    /**
     * 根据布局结果设置 viewBox - 对标 mermaid-js setupViewPortForSVG
     *
     * 使用默认 diagramPadding（flowchart 默认为 8）
     */
    private fun setupViewBox(root: SvgRoot, data: LayoutData) {
        if (data.nodes.isEmpty()) {
            root.attr("style", "max-width: 100px; background-color: white;")
            root.viewBox(0.0, 0.0, 100.0, 100.0)
            root.attr("role", "graphics-document document")
            root.attr("aria-roledescription", "flowchart-elk")
            return
        }

        val padding = data.diagramPadding.toDouble()
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = Double.MIN_VALUE
        var maxY = Double.MIN_VALUE

        // 遍历节点（节点是矩形区域，需要考虑 width/height）
        for (node in data.nodes) {
            minX = minOf(minX, node.x - node.width / 2)
            minY = minOf(minY, node.y - node.height / 2)
            maxX = maxOf(maxX, node.x + node.width / 2)
            maxY = maxOf(maxY, node.y + node.height / 2)
        }

        // 遍历边的路径点
        for (edge in data.edges) {
            for (point in edge.points) {
                minX = minOf(minX, point.x)
                minY = minOf(minY, point.y)
                maxX = maxOf(maxX, point.x)
                maxY = maxOf(maxY, point.y)
            }
            // 边标签位置
            if (edge.x != 0.0 || edge.y != 0.0) {
                val labelHalfH = 12.0
                minY = minOf(minY, edge.y - labelHalfH)
                maxY = maxOf(maxY, edge.y + labelHalfH)
            }
        }

        val width = maxX - minX + padding * 2
        val height = maxY - minY + padding * 2

        root.attr("style", "max-width: ${SvgElement.formatNumber(width)}px; background-color: white;")
        root.viewBox(minX - padding, minY - padding, width, height)
        root.attr("role", "graphics-document document")
        root.attr("aria-roledescription", "flowchart-elk")
    }
}
