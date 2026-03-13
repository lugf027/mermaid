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
 * 生成与 mermaid-js 完全一致的 SVG 结构：
 * <svg>
 *   <style>...</style>
 *   <g>
 *     <marker .../> ...
 *     <g class="root">
 *       <g class="clusters"/>
 *       <g class="edgePaths">
 *       <g class="edgeLabels">
 *       <g class="nodes">
 *     </g>
 *   </g>
 * </svg>
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

        // 2. 构建 SVG — 精确匹配 mermaid-js 的 SVG 结构
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

            // 外层 <g> 包含 markers 和 root 组
            val outerGroup = group {}

            // markers 直接在外层 <g> 中（不在 <defs> 中）
            Markers.addMarkers(outerGroup, diagramId, "flowchart-v2")

            // root 组
            val rootGroup = outerGroup.group {
                addClass("root")
            }

            // 渲染集群（子图）
            val clusterGroup = rootGroup.group { addClass("clusters") }
            for (node in laidOut.nodes) {
                if (node.isGroup) {
                    val cluster = ClusterRenderer.render(node, themeVariables)
                    clusterGroup.append(cluster)
                }
            }

            // 渲染边路径（edgePaths 和 edgeLabels 分开）
            val edgePathGroup = rootGroup.group { addClass("edgePaths") }
            val edgeLabelGroup = rootGroup.group { addClass("edgeLabels") }

            for (edge in laidOut.edges) {
                // 渲染边路径
                val edgePath = EdgeRenderer.renderPath(edge, diagramId, themeVariables)
                edgePathGroup.append(edgePath)

                // 渲染边标签
                val edgeLabel = EdgeRenderer.renderLabel(edge, diagramId, themeVariables)
                edgeLabelGroup.append(edgeLabel)
            }

            // 渲染节点
            val nodeGroup = rootGroup.group { addClass("nodes") }
            for (node in laidOut.nodes) {
                if (!node.isGroup) {
                    val nodeSvg = ShapeRegistry.render(node, themeVariables)
                    nodeGroup.append(nodeSvg)
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

    /**
     * 根据布局结果设置 viewBox - 对标 mermaid-js 的 setupViewPortForSVG
     *
     * mermaid-js 使用 svg.node().getBBox() 获取所有 SVG 子元素的真实包围盒，
     * 这包括节点、边路径的所有点。KMP 没有 DOM，所以需要手动遍历节点和边的
     * 所有坐标点来计算等效的 bounding box。
     */
    private fun setupViewBox(root: SvgRoot, data: LayoutData) {
        if (data.nodes.isEmpty()) {
            root.attr("style", "max-width: 100px; background-color: white;")
            root.viewBox(0.0, 0.0, 100.0, 100.0)
            root.attr("role", "graphics-document document")
            root.attr("aria-roledescription", "flowchart-v2")
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

        // 遍历边的路径点 — 对标 getBBox() 包含边路径的行为
        // 边路径（如回旋边）可能超出节点的 bounding box
        for (edge in data.edges) {
            for (point in edge.points) {
                minX = minOf(minX, point.x)
                minY = minOf(minY, point.y)
                maxX = maxOf(maxX, point.x)
                maxY = maxOf(maxY, point.y)
            }
            // 边标签位置 — 对标 getBBox() 包含 foreignObject 标签的行为
            // 标签的 (x, y) 是中心坐标，foreignObject 宽 = 文本宽度, 高 = 24px
            if (edge.x != 0.0 || edge.y != 0.0) {
                val labelHalfH = 12.0  // foreignObject 高度固定 24px / 2
                minY = minOf(minY, edge.y - labelHalfH)
                maxY = maxOf(maxY, edge.y + labelHalfH)
                // X 方向不需要额外处理，因为标签 X 通常在路径范围内
            }
        }

        val width = maxX - minX + padding * 2
        val height = maxY - minY + padding * 2

        // mermaid-js 属性顺序: ... class, style, viewBox, role, aria-roledescription
        root.attr("style", "max-width: ${SvgElement.formatNumber(width)}px; background-color: white;")
        root.viewBox(minX - padding, minY - padding, width, height)
        root.attr("role", "graphics-document document")
        root.attr("aria-roledescription", "flowchart-v2")
    }
}
