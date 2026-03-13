package io.lugf027.github.mermaid.core.diagram.packet

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.rendering.svg.buildSvg
import io.lugf027.github.mermaid.core.themes.ThemeVariables

/**
 * 数据包图渲染器 - 对标 mermaid-js packet/renderer.ts
 *
 * 纯手动 SVG 渲染：
 * 每行一个 word，每个 block 渲染为矩形+标签+位号。
 * 位宽按比例分配宽度。
 */
class PacketRenderer : DiagramRenderer {

    companion object {
        const val BIT_WIDTH = 32.0     // 每位像素宽度
        const val ROW_HEIGHT = 32.0    // 每行高度
        const val PADDING_X = 2.0      // 块间水平间距
        const val PADDING_Y = 5.0      // 行间垂直间距
        const val LABEL_FONT_SIZE = 12
        const val BYTE_FONT_SIZE = 10
    }

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val pdb = db as PacketDb
        val words = pdb.getWords()
        val bitsPerRow = pdb.getBitsPerRow()
        val title = pdb.getDiagramTitle()

        val pktConfig = config.packet
        val showBits = pktConfig?.showBits ?: true
        val paddingX = (pktConfig?.paddingX ?: 5).toDouble()
        val paddingY = (pktConfig?.paddingY ?: 5).toDouble()

        val bitWidth = BIT_WIDTH
        val rowHeight = ROW_HEIGHT
        val svgWidth = bitWidth * bitsPerRow + 2
        val titleH = if (title.isNotEmpty()) 30.0 else 0.0
        val svgHeight = titleH + (rowHeight + paddingY) * words.size + paddingY

        return buildSvg {
            attr("id", diagramId)
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("xmlns:xlink", "http://www.w3.org/1999/xlink")

            group {
                addClass("packet-diagram")

                // 标题
                if (title.isNotEmpty()) {
                    text(title, svgWidth / 2, 20.0) {
                        addClass("packetTitle")
                        attr("text-anchor", "middle")
                        attr("font-size", "14")
                        attr("font-weight", "bold")
                        attr("fill", themeVariables.textColor)
                    }
                }

                // 每行
                for ((rowIdx, word) in words.withIndex()) {
                    val y = titleH + paddingY + rowIdx * (rowHeight + paddingY)

                    for (block in word) {
                        val x = block.start * bitWidth + 1
                        val w = (block.end - block.start + 1) * bitWidth - paddingX

                        // 块矩形
                        rect(x, y, w, rowHeight) {
                            addClass("packetBlock")
                            attr("fill", themeVariables.mainBkg)
                            attr("stroke", themeVariables.lineColor)
                            attr("stroke-width", "1")
                        }

                        // 块标签（居中）
                        text(block.label, x + w / 2, y + rowHeight / 2 + 4) {
                            addClass("packetLabel")
                            attr("text-anchor", "middle")
                            attr("font-size", "$LABEL_FONT_SIZE")
                            attr("fill", themeVariables.textColor)
                        }

                        // 位编号
                        if (showBits) {
                            // 起始位
                            text(block.start.toString(), x + 4, y + rowHeight - 4) {
                                addClass("packetByte start")
                                attr("text-anchor", "start")
                                attr("font-size", "$BYTE_FONT_SIZE")
                                attr("fill", themeVariables.textColor)
                            }
                            // 终止位（仅当不同于起始位时）
                            if (block.end != block.start) {
                                text(block.end.toString(), x + w - 4, y + rowHeight - 4) {
                                    addClass("packetByte end")
                                    attr("text-anchor", "end")
                                    attr("font-size", "$BYTE_FONT_SIZE")
                                    attr("fill", themeVariables.textColor)
                                }
                            }
                        }
                    }
                }
            }

            viewBox(0.0, 0.0, svgWidth, svgHeight)
            attr("width", "100%")
            attr("style", "max-width: ${svgWidth.toInt()}px;")
        }
    }
}
