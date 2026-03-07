package io.lugf027.github.mermaid.core.renderer.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import io.lugf027.github.mermaid.core.config.ConfigManager
import io.lugf027.github.mermaid.core.core.Diagram
import io.lugf027.github.mermaid.core.themes.Theme
import io.lugf027.github.mermaid.core.themes.ThemeVariables

/**
 * MermaidView - 主绘制 Composable。
 * 接收一个 Diagram 实例或 Mermaid 文本，在 Canvas 上渲染图表。
 * 支持缩放/平移手势、视口计算。
 */
@Composable
fun MermaidView(
    diagram: Diagram?,
    modifier: Modifier = Modifier,
    themeVariables: ThemeVariables? = null,
) {
    val textMeasurer = rememberTextMeasurer()

    // 缩放和平移状态
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // 获取主题
    val config = ConfigManager.getConfig()
    val theme = themeVariables ?: Theme.getTheme(config.theme).getThemeVariables()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.1f, 5f)
                    offset += pan
                }
            }
    ) {
        // 应用变换（缩放 + 平移）
        withTransform({
            translate(offset.x, offset.y)
            scale(scale, scale, Offset.Zero)
        }) {
            diagram?.let { d ->
                d.renderer.draw(
                    drawScope = this,
                    db = d.db,
                    config = config,
                    theme = theme,
                    textMeasurer = textMeasurer,
                    size = Size(size.width / scale, size.height / scale)
                )
            }
        }
    }
}

/**
 * MermaidView（文本版） - 接收 Mermaid 文本，自动解析后渲染。
 */
@Composable
fun MermaidView(
    text: String,
    modifier: Modifier = Modifier,
    themeVariables: ThemeVariables? = null,
    onError: ((Exception) -> Unit)? = null,
) {
    val diagram = remember(text) {
        try {
            io.lugf027.github.mermaid.core.core.MermaidKMP.parse(text)
        } catch (e: Exception) {
            onError?.invoke(e)
            null
        }
    }
    MermaidView(diagram = diagram, modifier = modifier, themeVariables = themeVariables)
}
