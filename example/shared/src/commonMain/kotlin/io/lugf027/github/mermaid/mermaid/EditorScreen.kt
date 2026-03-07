package io.lugf027.github.mermaid.mermaid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lugf027.github.mermaid.core.renderer.compose.MermaidView

/**
 * 实时编辑屏幕：左侧代码编辑器 + 右侧实时渲染预览。
 * 在竖屏（窄屏）模式下上下布局。
 */
@Composable
fun EditorScreen(
    initialText: String = SampleData.defaultSample.text,
) {
    var mermaidText by remember { mutableStateOf(initialText) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth > 700.dp

        if (isWide) {
            // 横屏：左右布局
            Row(modifier = Modifier.fillMaxSize()) {
                // 左侧编辑器
                EditorPanel(
                    text = mermaidText,
                    onTextChange = { mermaidText = it; errorMessage = null },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                // 分隔线
                VerticalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))
                // 右侧预览
                PreviewPanel(
                    text = mermaidText,
                    errorMessage = errorMessage,
                    onError = { errorMessage = it },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        } else {
            // 竖屏：上下布局
            Column(modifier = Modifier.fillMaxSize()) {
                // 上方预览
                PreviewPanel(
                    text = mermaidText,
                    errorMessage = errorMessage,
                    onError = { errorMessage = it },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                HorizontalDivider(modifier = Modifier.fillMaxWidth().height(1.dp))
                // 下方编辑器
                EditorPanel(
                    text = mermaidText,
                    onTextChange = { mermaidText = it; errorMessage = null },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun EditorPanel(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(8.dp)) {
        Text(
            "Editor",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxSize(),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            ),
            shape = RoundedCornerShape(8.dp),
        )
    }
}

@Composable
private fun PreviewPanel(
    text: String,
    errorMessage: String?,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(8.dp)) {
        Text(
            "Preview",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .background(Color.White, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (text.isBlank()) {
                Text(
                    "Enter Mermaid syntax to preview",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (errorMessage != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text("Parse Error", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            } else {
                MermaidView(
                    text = text,
                    modifier = Modifier.fillMaxSize(),
                    onError = { onError(it.message ?: "Unknown error") },
                )
            }
        }
    }
}
