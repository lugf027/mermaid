package io.lugf027.github.mermaid.core.diagrams

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.db.CommonDb
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.types.DiagramDB
import io.lugf027.github.mermaid.core.types.DiagramRenderer
import io.lugf027.github.mermaid.core.types.ParserDefinition

/**
 * 占位解析器 - 尚未实现的图表类型使用。
 */
class StubParser(private val db: DiagramDB) : ParserDefinition {
    override fun parse(input: String) {
        // 占位：提取第一行作为标题
        val firstLine = input.lines().firstOrNull()?.trim() ?: ""
        db.setDiagramTitle(firstLine)
    }
}

/**
 * 占位 DB - 尚未实现的图表类型使用。
 */
class StubDb : CommonDb() {
    var rawText: String = ""

    override fun clear() {
        super.clear()
        rawText = ""
    }
}

/**
 * 占位渲染器 - 显示「该图表类型尚未实现」提示。
 */
class StubRenderer(private val typeName: String) : DiagramRenderer {
    override fun draw(
        drawScope: DrawScope,
        db: DiagramDB,
        config: MermaidConfig,
        theme: ThemeVariables,
        textMeasurer: TextMeasurer,
        size: Size
    ) {
        with(drawScope) {
            val message = "[$typeName] Not yet implemented"
            val style = TextStyle(color = Color.Gray, fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp))
            val result = textMeasurer.measure(message, style)
            drawText(
                textLayoutResult = result,
                topLeft = androidx.compose.ui.geometry.Offset(
                    (size.width - result.size.width) / 2f,
                    (size.height - result.size.height) / 2f
                )
            )
        }
    }
}
