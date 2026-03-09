package io.lugf027.github.mermaid.core.diagram.error

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.*
import io.lugf027.github.mermaid.core.rendering.svg.SvgRoot
import io.lugf027.github.mermaid.core.rendering.svg.buildSvg
import io.lugf027.github.mermaid.core.rendering.svg.group
import io.lugf027.github.mermaid.core.rendering.svg.rect
import io.lugf027.github.mermaid.core.rendering.svg.text
import io.lugf027.github.mermaid.core.themes.ThemeVariables

/**
 * 错误图表 - 对标 mermaid-js errorDiagram.ts
 *
 * 当图表解析失败或类型无法识别时显示的默认错误图。
 */
object ErrorDiagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "error",
        detector = { false }, // 错误图表从不通过检测匹配，仅作为 fallback
        dbFactory = { ErrorDb() },
        parser = ErrorParser(),
        renderer = ErrorRenderer(),
    )
}

/** 错误图表数据库 */
internal class ErrorDb : DiagramDB {
    private var title = ""
    private var accTitle = ""
    private var accDesc = ""
    private var errorMessage = "Syntax error in diagram"

    override fun clear() {
        title = ""
        accTitle = ""
        accDesc = ""
        errorMessage = "Syntax error in diagram"
    }

    override fun setDiagramTitle(title: String) { this.title = title }
    override fun getDiagramTitle(): String = title
    override fun setAccTitle(title: String) { accTitle = title }
    override fun getAccTitle(): String = accTitle
    override fun setAccDescription(desc: String) { accDesc = desc }
    override fun getAccDescription(): String = accDesc

    fun setErrorMessage(msg: String) { errorMessage = msg }
    fun getErrorMessage(): String = errorMessage
}

/** 错误图表解析器（不做任何解析） */
internal class ErrorParser : DiagramParser {
    override fun parse(text: String, db: DiagramDB) {
        // 错误图表不需要解析
    }
}

/** 错误图表渲染器 - 显示错误信息 */
internal class ErrorRenderer : DiagramRenderer {
    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val errorDb = db as? ErrorDb
        val errorMessage = errorDb?.getErrorMessage() ?: "Syntax error in diagram"

        val width = 500.0
        val height = 200.0

        return buildSvg {
            attr("id", diagramId)
            addClass("mermaid-error")
            attr("xmlns", "http://www.w3.org/2000/svg")
            viewBox(0.0, 0.0, width, height)
            attr("width", "${width.toInt()}")
            attr("height", "${height.toInt()}")

            group {
                // 背景
                rect(0.0, 0.0, width, height) {
                    attr("fill", "#fafafa")
                    attr("stroke", "#ccc")
                    attr("stroke-width", "1")
                }

                // 错误图标 (红色感叹号)
                text("⚠", width / 2, 70.0) {
                    attr("text-anchor", "middle")
                    attr("font-size", "50")
                    attr("fill", "#E74C3C")
                }

                // 错误标题
                text("Syntax error in diagram", width / 2, 120.0) {
                    attr("text-anchor", "middle")
                    attr("font-size", "16")
                    attr("fill", "#333")
                    attr("font-weight", "bold")
                    attr("font-family", themeVariables.fontFamily)
                }

                // 错误详情
                if (errorMessage != "Syntax error in diagram") {
                    text(errorMessage, width / 2, 150.0) {
                        attr("text-anchor", "middle")
                        attr("font-size", "12")
                        attr("fill", "#666")
                        attr("font-family", themeVariables.fontFamily)
                    }
                }
            }
        }
    }
}
