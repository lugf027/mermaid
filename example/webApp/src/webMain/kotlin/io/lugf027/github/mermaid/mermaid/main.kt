package io.lugf027.github.mermaid.mermaid

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import io.lugf027.github.mermaid.core.core.MermaidKMP
import io.lugf027.github.mermaid.core.renderer.compose.MermaidView
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val params = parseUrlParams()
    val mode = params["mode"]

    when (mode) {
        "render" -> {
            // Headless 渲染模式：仅渲染指定图表到全屏 Canvas，不显示 App UI
            val text = params["text"] ?: ""
            startHeadlessRender(text)
        }
        "samples" -> {
            // 导出 SampleData 为 JSON
            exportSamplesJson()
        }
        else -> {
            // 正常模式：启动完整 App
            ComposeViewport {
                App()
            }
        }
    }
}

/**
 * 解析 URL 查询参数。
 */
private fun parseUrlParams(): Map<String, String> {
    val search = window.location.search
    if (search.isBlank() || search == "?") return emptyMap()

    return search.removePrefix("?")
        .split("&")
        .mapNotNull { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                val key = decodeURIComponent(parts[0])
                val value = decodeURIComponent(parts[1])
                key to value
            } else null
        }
        .toMap()
}

/**
 * Headless 渲染模式：初始化 MermaidKMP 后渲染指定图表到全屏 Canvas。
 * 支持通过 postMessage 动态更新图表文本。
 */
@OptIn(ExperimentalComposeUiApi::class)
private fun startHeadlessRender(initialText: String) {
    // 移除 loading spinner
    document.body?.innerHTML = ""

    // 创建用于 ComposeViewport 的容器
    val container = document.createElement("div").also {
        it.setAttribute("id", "mermaid-kmp-root")
        it.setAttribute("style", "width:100%;height:100%;overflow:hidden;background:white;")
    }
    document.body?.appendChild(container)

    // 使用一个 JS 变量持有当前文本，通过 window 属性暴露
    js("""
        window.__mermaidKmpText = initialText;
        window.__mermaidKmpReady = false;
        window.updateMermaidKmpText = function(newText) {
            window.__mermaidKmpText = newText;
            window.__mermaidKmpTextChanged = true;
        };
    """)

    ComposeViewport {
        LaunchedEffect(Unit) {
            MermaidKMP.initialize()
            js("window.__mermaidKmpReady = true")
        }

        val currentText = getMermaidKmpText()

        MermaidView(
            text = currentText,
            modifier = Modifier.fillMaxSize(),
            onError = { e: Exception ->
                println("MermaidKMP render error: ${e.message}")
            },
        )
    }
}

/**
 * 从 JS window 属性获取当前图表文本。
 * 在 Compose 中通过轮询感知文本变化。
 */
@Composable
private fun getMermaidKmpText(): String {
    var text by remember {
        mutableStateOf(
            js("window.__mermaidKmpText || ''").unsafeCast<String>()
        )
    }

    // 每 200ms 检查文本是否变化
    LaunchedEffect(Unit) {
        while (true) {
            delay(200)
            val changed = js("window.__mermaidKmpTextChanged === true").unsafeCast<Boolean>()
            if (changed) {
                text = js("window.__mermaidKmpText || ''").unsafeCast<String>()
                js("window.__mermaidKmpTextChanged = false")
            }
        }
    }

    return text
}

/**
 * 将 SampleData 导出为 JSON，写入页面 body。
 */
private fun exportSamplesJson() {
    val jsonArray = SampleData.samples.joinToString(",\n", prefix = "[\n", postfix = "\n]") { sample ->
        """  {"name": ${escapeJson(sample.name)}, "type": ${escapeJson(sample.type)}, "text": ${escapeJson(sample.text)}}"""
    }

    document.body?.innerHTML = ""
    val pre = document.createElement("pre").also {
        it.setAttribute("id", "samples-json")
        it.textContent = jsonArray
    }
    document.body?.appendChild(pre)
}

private fun escapeJson(str: String): String {
    return "\"" + str
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t") + "\""
}

// JS interop
private fun decodeURIComponent(encoded: String): String =
    js("decodeURIComponent(encoded)").unsafeCast<String>()
