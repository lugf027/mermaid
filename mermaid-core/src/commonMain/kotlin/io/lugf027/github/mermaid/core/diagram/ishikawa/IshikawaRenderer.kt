package io.lugf027.github.mermaid.core.diagram.ishikawa

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.rendering.svg.buildSvg
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.TextUtils
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * 鱼骨图渲染器 - 对标 mermaid-js ishikawaRenderer.ts
 *
 * 水平脊柱线从左到右延伸，原因类别交替排列在上方和下方，
 * 分支以斜线延伸，鱼头在右端。
 */
class IshikawaRenderer : DiagramRenderer {

    companion object {
        const val SPINE_Y = 250.0          // 脊柱 Y 坐标
        const val CATEGORY_SPACING = 200.0  // 类别间距
        const val BRANCH_LENGTH = 120.0     // 分支线长度
        const val SUB_BRANCH_SPACING = 25.0 // 子分支间距
        const val MARGIN = 40.0
        const val HEAD_WIDTH = 60.0         // 鱼头宽度
        const val HEAD_HEIGHT = 40.0        // 鱼头高度
        const val FONT_SIZE = 12
        const val ANGLE = 82.0 * PI / 180.0 // 分支角度 82°
    }

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val idb = db as IshikawaDb
        val root = idb.getRootNode()
        val categories = idb.getCategories()
        val title = idb.getDiagramTitle()

        // 计算布局尺寸
        val numCategories = categories.size
        val spineLength = max(400.0, numCategories * CATEGORY_SPACING + HEAD_WIDTH + MARGIN * 2)
        val svgW = spineLength + MARGIN * 2
        val svgH = SPINE_Y * 2 + MARGIN * 2

        return buildSvg {
            attr("id", diagramId)
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("xmlns:xlink", "http://www.w3.org/1999/xlink")

            // 箭头标记
            defs {
                marker {
                    attr("id", "ishikawa-arrow")
                    attr("markerWidth", "10")
                    attr("markerHeight", "10")
                    attr("refX", "10")
                    attr("refY", "5")
                    attr("orient", "auto")
                    path("M 0 0 L 10 5 L 0 10 Z") {
                        attr("fill", themeVariables.lineColor)
                    }
                }
            }

            group {
                addClass("ishikawa")

                // 标题
                if (title.isNotEmpty()) {
                    text(title, svgW / 2, 20.0) {
                        attr("text-anchor", "middle")
                        attr("font-size", "16")
                        attr("font-weight", "bold")
                        attr("fill", themeVariables.textColor)
                    }
                }

                // 脊柱线 (水平主线)
                val spineStartX = MARGIN
                val spineEndX = svgW - MARGIN - HEAD_WIDTH
                line(spineStartX, SPINE_Y, spineEndX, SPINE_Y) {
                    attr("stroke", themeVariables.lineColor)
                    attr("stroke-width", "2")
                    attr("marker-end", "url(#ishikawa-arrow)")
                }

                // 鱼头 (效果标签 + 鱼头形状)
                if (root != null) {
                    group {
                        addClass("head")
                        // 鱼头矩形
                        rect(spineEndX, SPINE_Y - HEAD_HEIGHT / 2, HEAD_WIDTH, HEAD_HEIGHT) {
                            attr("fill", themeVariables.mainBkg)
                            attr("stroke", themeVariables.lineColor)
                            attr("stroke-width", "1.5")
                            attr("rx", "5")
                            attr("ry", "5")
                        }
                        // 效果文本
                        text(root.text, spineEndX + HEAD_WIDTH / 2, SPINE_Y + 4) {
                            attr("text-anchor", "middle")
                            attr("font-size", "$FONT_SIZE")
                            attr("font-weight", "bold")
                            attr("fill", themeVariables.textColor)
                        }
                    }
                }

                // 原因类别分支
                for ((idx, category) in categories.withIndex()) {
                    val isAbove = idx % 2 == 0  // 交替上下
                    val categoryX = spineStartX + (idx + 1) * CATEGORY_SPACING
                    val direction = if (isAbove) -1.0 else 1.0

                    group {
                        addClass("category")

                        // 主分支线 (从脊柱到类别标签)
                        val branchEndX = categoryX - BRANCH_LENGTH * cos(ANGLE)
                        val branchEndY = SPINE_Y + direction * BRANCH_LENGTH * sin(ANGLE)

                        line(categoryX, SPINE_Y, branchEndX, branchEndY) {
                            attr("stroke", themeVariables.lineColor)
                            attr("stroke-width", "1.5")
                        }

                        // 类别标签
                        text(category.text, branchEndX, branchEndY + direction * 16) {
                            attr("text-anchor", "middle")
                            attr("font-size", "$FONT_SIZE")
                            attr("font-weight", "bold")
                            attr("fill", themeVariables.textColor)
                        }

                        // 子原因 (沿分支线排列)
                        for ((subIdx, subCause) in category.children.withIndex()) {
                            val t = (subIdx + 1).toDouble() / (category.children.size + 1)
                            val subX = categoryX - t * (categoryX - branchEndX)
                            val subY = SPINE_Y + t * (branchEndY - SPINE_Y)

                            // 水平子分支线
                            val subLineLength = TextUtils.estimateTextWidth(subCause.text, FONT_SIZE.toDouble()) + 10
                            val subEndX = subX - subLineLength * 0.7
                            line(subX, subY, subEndX, subY) {
                                attr("stroke", themeVariables.lineColor)
                                attr("stroke-width", "1")
                            }

                            // 子原因标签
                            text(subCause.text, subEndX - 5, subY - 4) {
                                attr("text-anchor", "end")
                                attr("font-size", "${FONT_SIZE - 1}")
                                attr("fill", themeVariables.textColor)
                            }
                        }
                    }
                }
            }

            viewBox(0.0, 0.0, svgW, svgH)
            attr("width", "100%")
            attr("style", "max-width: ${svgW.toInt()}px;")
        }
    }
}
