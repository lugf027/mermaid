package io.lugf027.github.mermaid.core.diagram.mindmap

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.TextUtils
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Mindmap 渲染器 - 对标 mermaid-js mindmapRenderer.ts + svgDraw.ts
 *
 * 使用简化的径向树布局算法（替代 cose-bilkent），
 * 支持 7 种节点形状渲染。
 */
class MindmapRenderer : DiagramRenderer {

    companion object {
        private const val HORIZONTAL_SPACING = 80.0
        private const val VERTICAL_SPACING = 40.0
        private const val NODE_PADDING = 10.0
        private const val FONT_SIZE = 14.0
        private const val LINE_HEIGHT_FACTOR = 1.1
    }

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val mindmapDb = db as? MindmapDb ?: throw IllegalArgumentException("Expected MindmapDb")
        val root = mindmapDb.getRootNode()
            ?: return buildEmptySvg(diagramId)

        val conf = config.mindmap ?: io.lugf027.github.mermaid.core.config.MindmapDiagramConfig()
        val maxNodeWidth = conf.maxNodeWidth ?: 200

        // 1. 计算每个节点的尺寸
        calculateNodeSizes(root, maxNodeWidth)

        // 2. 布局树
        layoutTree(root, 0.0, 0.0)

        // 3. 计算边界
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = Double.MIN_VALUE
        var maxY = Double.MIN_VALUE
        collectBounds(root) { node ->
            val halfW = node.width / 2
            val halfH = node.height / 2
            if (node.x - halfW < minX) minX = node.x - halfW
            if (node.y - halfH < minY) minY = node.y - halfH
            if (node.x + halfW > maxX) maxX = node.x + halfW
            if (node.y + halfH > maxY) maxY = node.y + halfH
        }

        val padding = 20.0
        val viewWidth = maxX - minX + padding * 2
        val viewHeight = maxY - minY + padding * 2

        return buildSvg {
            attr("id", diagramId)
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("width", "100%")
            attr("style", "max-width: ${SvgElement.formatNumber(viewWidth)}px;")
            viewBox(minX - padding, minY - padding, viewWidth, viewHeight)
            attr("role", "graphics-document document")

            // 主容器
            group {
                // 先画边（在节点下面）
                group {
                    addClass("edgePaths")
                    drawEdges(this, root, themeVariables)
                }

                // 再画节点
                group {
                    addClass("nodes")
                    drawNodes(this, root, themeVariables)
                }
            }
        }
    }

    /**
     * 计算节点尺寸
     */
    private fun calculateNodeSizes(node: MindmapNode, maxWidth: Int) {
        val textWidth = TextUtils.estimateTextWidth(node.descr, FONT_SIZE)
        val clampedWidth = textWidth.coerceAtMost(maxWidth.toDouble())
        node.width = clampedWidth + node.padding * 2.0 + 8.0
        node.height = FONT_SIZE * LINE_HEIGHT_FACTOR + node.padding * 2.0

        // 圆形节点需要等宽等高
        if (node.type == MindmapNodeType.CIRCLE) {
            val diameter = max(node.width, node.height)
            node.width = diameter
            node.height = diameter
        }

        for (child in node.children) {
            calculateNodeSizes(child, maxWidth)
        }
    }

    /**
     * 简化的树布局：根节点居中，子节点垂直排列在右侧
     */
    private fun layoutTree(node: MindmapNode, x: Double, y: Double) {
        node.x = x
        node.y = y

        if (node.children.isEmpty()) return

        // 计算子树总高度
        var totalChildHeight = 0.0
        for (child in node.children) {
            totalChildHeight += getSubtreeHeight(child)
        }
        totalChildHeight += (node.children.size - 1) * VERTICAL_SPACING

        // 从顶部开始排列子节点
        var currentY = y - totalChildHeight / 2.0
        val childX = x + node.width / 2.0 + HORIZONTAL_SPACING

        for (child in node.children) {
            val subtreeH = getSubtreeHeight(child)
            val childCenterY = currentY + subtreeH / 2.0
            layoutTree(child, childX, childCenterY)
            currentY += subtreeH + VERTICAL_SPACING
        }
    }

    /**
     * 计算子树占用的垂直高度
     */
    private fun getSubtreeHeight(node: MindmapNode): Double {
        if (node.children.isEmpty()) return node.height

        var totalChildHeight = 0.0
        for (child in node.children) {
            totalChildHeight += getSubtreeHeight(child)
        }
        totalChildHeight += (node.children.size - 1) * VERTICAL_SPACING

        return max(node.height, totalChildHeight)
    }

    /**
     * 收集所有节点边界
     */
    private fun collectBounds(node: MindmapNode, action: (MindmapNode) -> Unit) {
        action(node)
        for (child in node.children) {
            collectBounds(child, action)
        }
    }

    /**
     * 绘制所有边
     */
    private fun drawEdges(parent: SvgElement, node: MindmapNode, themeVariables: ThemeVariables) {
        for (child in node.children) {
            val depthClass = "edge-depth-${child.level.coerceAtMost(7)}"
            val sectionClass = if (child.section >= 0) "section-edge-${child.section}" else ""

            // 贝塞尔曲线连接父子节点
            val x1 = node.x + node.width / 2
            val y1 = node.y
            val x2 = child.x - child.width / 2
            val y2 = child.y
            val midX = (x1 + x2) / 2

            val d = "M ${SvgElement.formatNumber(x1)} ${SvgElement.formatNumber(y1)} " +
                    "C ${SvgElement.formatNumber(midX)} ${SvgElement.formatNumber(y1)}, " +
                    "${SvgElement.formatNumber(midX)} ${SvgElement.formatNumber(y2)}, " +
                    "${SvgElement.formatNumber(x2)} ${SvgElement.formatNumber(y2)}"

            parent.path(d) {
                addClass("edge $sectionClass $depthClass".trim())
                attr("fill", "none")
            }

            // 递归绘制子树的边
            drawEdges(parent, child, themeVariables)
        }
    }

    /**
     * 绘制所有节点
     */
    private fun drawNodes(parent: SvgElement, node: MindmapNode, themeVariables: ThemeVariables) {
        val sectionClass = if (node.isRoot) "section-root section--1" else "section-${node.section}"

        parent.group {
            addClass("mindmap-node $sectionClass")
            translate(node.x - node.width / 2, node.y - node.height / 2)

            // 绘制背景形状
            group {
                drawNodeBackground(this, node)
            }

            // 绘制文字
            group {
                translate(node.width / 2, node.height / 2)
                text(node.descr, 0.0, 0.0) {
                    addClass("mindmap-node-label")
                    attr("dy", "0.35em")
                    attr("text-anchor", "middle")
                    attr("dominant-baseline", "middle")
                    attr("font-size", FONT_SIZE.toString())
                    attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")
                }
            }
        }

        // 递归绘制子节点
        for (child in node.children) {
            drawNodes(parent, child, themeVariables)
        }
    }

    /**
     * 绘制节点背景形状
     */
    private fun drawNodeBackground(parent: SvgElement, node: MindmapNode) {
        val w = node.width
        val h = node.height
        val sectionClass = if (node.isRoot) "section-root" else "section-${node.section}"

        when (node.type) {
            MindmapNodeType.DEFAULT -> {
                // 圆角路径 + 底部线
                val rd = 5.0
                val d = "M0 ${SvgElement.formatNumber(h - rd)} " +
                        "v${SvgElement.formatNumber(-h + 2 * rd)} " +
                        "q0,-${SvgElement.formatNumber(rd)} ${SvgElement.formatNumber(rd)},-${SvgElement.formatNumber(rd)} " +
                        "h${SvgElement.formatNumber(w - 2 * rd)} " +
                        "q${SvgElement.formatNumber(rd)},0 ${SvgElement.formatNumber(rd)},${SvgElement.formatNumber(rd)} " +
                        "v${SvgElement.formatNumber(h - rd)} H0 Z"
                parent.path(d) {
                    addClass("node-bkg node-default")
                }
                parent.line(0.0, h, w, h) {
                    addClass("node-line-${node.section}")
                }
            }

            MindmapNodeType.RECT -> {
                parent.rect(0.0, 0.0, w, h) {
                    addClass("node-bkg node-rect")
                }
            }

            MindmapNodeType.ROUNDED_RECT -> {
                parent.rect(0.0, 0.0, w, h) {
                    addClass("node-bkg node-rounded")
                    attr("rx", node.padding.toString())
                    attr("ry", node.padding.toString())
                }
            }

            MindmapNodeType.CIRCLE -> {
                val r = w / 2
                parent.circle(r, r, r) {
                    addClass("node-bkg node-circle")
                }
            }

            MindmapNodeType.CLOUD -> {
                // 简化的云朵形（多弧线路径）
                val d = buildCloudPath(w, h)
                parent.path(d) {
                    addClass("node-bkg node-cloud")
                }
            }

            MindmapNodeType.BANG -> {
                // 简化的爆炸形
                val d = buildBangPath(w, h)
                parent.path(d) {
                    addClass("node-bkg node-bang")
                }
            }

            MindmapNodeType.HEXAGON -> {
                // 六边形
                val dx = w * 0.15
                val points = listOf(
                    dx to 0.0,
                    w - dx to 0.0,
                    w to h / 2,
                    w - dx to h,
                    dx to h,
                    0.0 to h / 2
                )
                parent.polygon(points) {
                    addClass("node-bkg node-hexagon")
                }
            }
        }
    }

    /**
     * 构建云朵形路径
     */
    private fun buildCloudPath(w: Double, h: Double): String {
        val rx = w / 2
        val ry = h / 2
        val sb = StringBuilder()

        // 简化的云朵：用多个弧线构成
        val cx = w / 2
        val cy = h / 2
        val numArcs = 8
        val angleStep = 2 * PI / numArcs

        sb.append("M ${SvgElement.formatNumber(w)} ${SvgElement.formatNumber(cy)} ")
        for (i in 1..numArcs) {
            val angle = i * angleStep
            val wobble = if (i % 2 == 0) 1.15 else 0.85
            val px = cx + rx * wobble * cos(angle)
            val py = cy + ry * wobble * sin(angle)
            val cpx = cx + rx * 1.3 * cos(angle - angleStep / 2)
            val cpy = cy + ry * 1.3 * sin(angle - angleStep / 2)
            sb.append("Q ${SvgElement.formatNumber(cpx)} ${SvgElement.formatNumber(cpy)}, ")
            sb.append("${SvgElement.formatNumber(px)} ${SvgElement.formatNumber(py)} ")
        }
        sb.append("Z")
        return sb.toString()
    }

    /**
     * 构建爆炸形路径
     */
    private fun buildBangPath(w: Double, h: Double): String {
        val cx = w / 2
        val cy = h / 2
        val points = 10
        val sb = StringBuilder()

        for (i in 0 until points) {
            val angle = i * 2 * PI / points - PI / 2
            val r = if (i % 2 == 0) max(w, h) * 0.55 else max(w, h) * 0.35
            val px = cx + r * cos(angle)
            val py = cy + r * sin(angle)
            if (i == 0) {
                sb.append("M ${SvgElement.formatNumber(px)} ${SvgElement.formatNumber(py)} ")
            } else {
                sb.append("L ${SvgElement.formatNumber(px)} ${SvgElement.formatNumber(py)} ")
            }
        }
        sb.append("Z")
        return sb.toString()
    }

    /**
     * 构建空 SVG
     */
    private fun buildEmptySvg(diagramId: String): SvgRoot {
        return buildSvg {
            attr("id", diagramId)
            attr("xmlns", "http://www.w3.org/2000/svg")
            viewBox(0.0, 0.0, 100.0, 100.0)
        }
    }
}
