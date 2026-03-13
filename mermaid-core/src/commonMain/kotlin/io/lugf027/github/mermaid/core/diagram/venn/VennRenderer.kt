package io.lugf027.github.mermaid.core.diagram.venn

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.rendering.svg.buildSvg
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 韦恩图渲染器 - 对标 mermaid-js vennRenderer.ts
 *
 * 简化布局：圆心等角度分布在中心周围，半径基于 size 值。
 * 替代 @upsetjs/venn.js 库的复杂布局算法。
 */
class VennRenderer : DiagramRenderer {

    companion object {
        const val WIDTH = 500.0
        const val HEIGHT = 400.0
        const val PADDING = 40.0
        const val FONT_SIZE = 14
        val COLORS = listOf(
            "#4e79a7", "#f28e2b", "#e15759", "#76b7b2",
            "#59a14f", "#edc948", "#b07aa1", "#ff9da7"
        )
    }

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val vdb = db as VennDb
        val sets = vdb.getSets()
        val unions = vdb.getUnions()
        val textNodes = vdb.getTextNodes()
        val title = vdb.getDiagramTitle()

        val titleH = if (title.isNotEmpty()) 30.0 else 0.0
        val cx = WIDTH / 2
        val cy = (HEIGHT + titleH) / 2

        // 计算每个集合的圆心和半径
        data class CircleLayout(
            val id: String,
            val label: String,
            val cx: Double,
            val cy: Double,
            val r: Double,
            val color: String
        )

        val maxSize = sets.maxOfOrNull { it.size } ?: 10.0
        val maxR = min(WIDTH, HEIGHT) / 2 - PADDING - 20
        val circles = mutableListOf<CircleLayout>()

        if (sets.size == 1) {
            // 单圆
            val s = sets[0]
            val r = maxR * 0.7
            circles.add(CircleLayout(s.sets[0], s.label, cx, cy, r, COLORS[0]))
        } else {
            // 多圆：均匀分布在中心周围
            val spreadR = maxR * 0.4 // 圆心分布半径
            val angleStep = 2 * PI / sets.size

            for ((idx, s) in sets.withIndex()) {
                val angle = idx * angleStep - PI / 2
                val circCx = cx + spreadR * cos(angle)
                val circCy = cy + spreadR * sin(angle)
                val r = maxR * 0.5 * sqrt(s.size / maxSize)
                val color = COLORS[idx % COLORS.size]
                circles.add(CircleLayout(s.sets[0], s.label, circCx, circCy, r.coerceAtLeast(30.0), color))
            }
        }

        val circleMap = circles.associateBy { it.id }

        return buildSvg {
            attr("id", diagramId)
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("xmlns:xlink", "http://www.w3.org/1999/xlink")

            group {
                addClass("venn")

                // 标题
                if (title.isNotEmpty()) {
                    text(title, cx, 24.0) {
                        attr("text-anchor", "middle")
                        attr("font-size", "16")
                        attr("font-weight", "bold")
                        attr("fill", themeVariables.textColor)
                    }
                }

                // 绘制圆形
                for (circle in circles) {
                    group {
                        addClass("venn-set")
                        circle(circle.cx, circle.cy, circle.r) {
                            attr("fill", circle.color)
                            attr("fill-opacity", "0.25")
                            attr("stroke", circle.color)
                            attr("stroke-width", "2")
                        }
                        // 集合标签
                        text(circle.label, circle.cx, circle.cy - circle.r - 8) {
                            attr("text-anchor", "middle")
                            attr("font-size", "$FONT_SIZE")
                            attr("font-weight", "bold")
                            attr("fill", themeVariables.textColor)
                        }
                    }
                }

                // 交集标签
                for (union in unions) {
                    if (union.label.isNotEmpty() && union.sets.size >= 2) {
                        // 计算交集中心 = 所有相关圆心的平均值
                        val relatedCircles = union.sets.mapNotNull { circleMap[it] }
                        if (relatedCircles.isNotEmpty()) {
                            val ucx = relatedCircles.sumOf { it.cx } / relatedCircles.size
                            val ucy = relatedCircles.sumOf { it.cy } / relatedCircles.size
                            text(union.label, ucx, ucy + 4) {
                                attr("text-anchor", "middle")
                                attr("font-size", "${FONT_SIZE - 1}")
                                attr("fill", themeVariables.textColor)
                            }
                        }
                    }
                }

                // 文本节点
                for (tn in textNodes) {
                    val relatedCircles = tn.sets.mapNotNull { circleMap[it] }
                    if (relatedCircles.isNotEmpty()) {
                        val tcx = relatedCircles.sumOf { it.cx } / relatedCircles.size
                        val tcy = relatedCircles.sumOf { it.cy } / relatedCircles.size
                        text(tn.label.ifEmpty { tn.id }, tcx, tcy + 20) {
                            attr("text-anchor", "middle")
                            attr("font-size", "${FONT_SIZE - 2}")
                            attr("fill", themeVariables.textColor)
                        }
                    }
                }
            }

            viewBox(0.0, 0.0, WIDTH, HEIGHT + titleH)
            attr("width", "100%")
            attr("style", "max-width: ${WIDTH.toInt()}px;")
        }
    }
}
