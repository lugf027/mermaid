package io.lugf027.github.mermaid.mermaid

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lugf027.github.mermaid.core.MermaidApi

/**
 * mermaid-kmp 示例应用 - 展示 Mermaid 图表解析和 SVG 渲染
 */
@Composable
fun App() {
    // 初始化 MermaidApi
    LaunchedEffect(Unit) {
        MermaidApi.initialize()
    }

    MaterialTheme {
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("Flowchart", "Pie Chart", "Custom Input")

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize()
        ) {
            // 顶部标题
            Text(
                text = "mermaid-kmp Demo",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )

            // Tab 选择
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // 内容区域
            when (selectedTab) {
                0 -> FlowchartDemo()
                1 -> PieChartDemo()
                2 -> CustomInputDemo()
            }
        }
    }
}

@Composable
private fun FlowchartDemo() {
    val mermaidText = """
        flowchart LR
            A[Start] --> B{Decision}
            B -->|Yes| C[Process A]
            B -->|No| D[Process B]
            C --> E[End]
            D --> E
    """.trimIndent()

    DiagramDisplay(
        title = "Flowchart Example",
        mermaidText = mermaidText
    )
}

@Composable
private fun PieChartDemo() {
    val mermaidText = """
        pie showData
            title Browser Market Share
            "Chrome" : 65
            "Safari" : 19
            "Firefox" : 4
            "Edge" : 4
            "Other" : 8
    """.trimIndent()

    DiagramDisplay(
        title = "Pie Chart Example",
        mermaidText = mermaidText
    )
}

@Composable
private fun CustomInputDemo() {
    var input by remember {
        mutableStateOf(
            """
pie
    title My Pets
    "Dogs" : 30
    "Cats" : 40
    "Birds" : 20
    "Fish" : 10
            """.trimIndent()
        )
    }
    var svgOutput by remember { mutableStateOf("") }
    var diagramType by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Input Mermaid Code:",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                try {
                    val diagram = MermaidApi.parse(input)
                    diagramType = diagram.type
                    svgOutput = MermaidApi.renderToSvg(input, indent = true)
                    errorMsg = ""
                } catch (e: Exception) {
                    errorMsg = "Error: ${e.message}"
                    svgOutput = ""
                    diagramType = "error"
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Render")
        }

        if (errorMsg.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                errorMsg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (svgOutput.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Detected Type: $diagramType",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(8.dp))
            SvgOutputCard(svgOutput)
        }
    }
}

@Composable
private fun DiagramDisplay(title: String, mermaidText: String) {
    var svgOutput by remember { mutableStateOf("") }
    var diagramType by remember { mutableStateOf("") }

    LaunchedEffect(mermaidText) {
        try {
            val diagram = MermaidApi.parse(mermaidText)
            diagramType = diagram.type
            svgOutput = MermaidApi.renderToSvg(mermaidText, indent = true)
        } catch (e: Exception) {
            svgOutput = "Error: ${e.message}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        // 源码展示
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = mermaidText,
                modifier = Modifier.padding(12.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        if (diagramType.isNotEmpty()) {
            Text(
                "Detected Type: $diagramType",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(8.dp))
        }

        // SVG 输出展示
        if (svgOutput.isNotEmpty()) {
            SvgOutputCard(svgOutput)
        }
    }
}

@Composable
private fun SvgOutputCard(svgOutput: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "SVG Output",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = svgOutput,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
