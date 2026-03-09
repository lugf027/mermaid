package io.lugf027.github.mermaid.core.rendering.clusters

import io.lugf027.github.mermaid.core.layout.LayoutNode
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.themes.ThemeVariables

/**
 * 集群/子图渲染器 - 对标 mermaid-js clusters.js
 *
 * 渲染子图的边框和标题。
 */
object ClusterRenderer {

    /**
     * 渲染子图
     */
    fun render(node: LayoutNode, themeVariables: ThemeVariables): SvgGroup {
        val g = SvgGroup()
        g.addClass("cluster")
        g.attr("id", node.domId ?: "cluster-${node.id}")
        g.translate(node.x, node.y)

        val w = node.width
        val h = node.height
        val tv = themeVariables

        // 子图背景
        g.rect(-w / 2, -h / 2, w, h) {
            rounded(5.0)
            attr("fill", tv.clusterBkg)
            attr("stroke", tv.clusterBorder)
            attr("stroke-width", "1")
        }

        // 子图标题
        if (!node.label.isNullOrEmpty()) {
            g.text(node.label!!, 0.0, -h / 2 + 18.0) {
                attr("text-anchor", "middle")
                attr("fill", tv.titleColor)
                attr("font-weight", "bold")
                attr("font-size", "14")
            }
        }

        return g
    }
}
