package io.lugf027.github.mermaid.mermaid

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import io.lugf027.github.mermaid.core.core.MermaidKMP

/**
 * Mermaid-KMP Demo App 主入口。
 * 底部导航栏切换 Editor / Gallery 两个屏幕。
 */
@Composable
fun App() {
    // 确保 MermaidKMP 已初始化
    LaunchedEffect(Unit) {
        MermaidKMP.initialize()
    }

    MaterialTheme(
        colorScheme = dynamicColorScheme(),
    ) {
        var selectedTab by remember { mutableStateOf(0) }
        var editorText by remember { mutableStateOf(SampleData.defaultSample.text) }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {},
                        label = { Text("Editor") },
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {},
                        label = { Text("Gallery") },
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (selectedTab) {
                    0 -> EditorScreen(initialText = editorText)
                    1 -> GalleryScreen(
                        onSampleSelected = { sample ->
                            editorText = sample.text
                            selectedTab = 0
                        }
                    )
                }
            }
        }
    }
}

/**
 * 生成默认 Material3 配色方案。
 */
@Composable
private fun dynamicColorScheme(): ColorScheme {
    return lightColorScheme()
}
