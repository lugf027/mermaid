/**
 * Diagram showcase component for demonstrating Mermaid diagrams.
 */
package io.github.lugf027.mermaid.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.lugf027.mermaid.MermaidComposition
import io.github.lugf027.mermaid.MermaidCompositionResult
import io.github.lugf027.mermaid.MermaidDiagram
import io.github.lugf027.mermaid.rememberMermaidComposition
import io.github.lugf027.mermaid.theme.MermaidTheme

/**
 * Main showcase screen for demonstrating Mermaid diagrams.
 */
@Composable
public fun DiagramShowcase() {
    val samples = remember { SampleDiagrams.getAllSamples() }
    var selectedIndex by remember { mutableStateOf(0) }
    var currentTheme by remember { mutableStateOf(MermaidTheme.Default) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "KMP Mermaid Demo",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Kotlin Multiplatform Mermaid Diagram Renderer",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Theme selector
        ThemeSelector(
            currentTheme = currentTheme,
            onThemeChange = { currentTheme = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Main content
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sample list
            LazyColumn(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(samples) { index, (name, _) ->
                    SampleItem(
                        name = name,
                        isSelected = index == selectedIndex,
                        onClick = { selectedIndex = index }
                    )
                }
            }

            // Diagram preview
            val (name, diagram) = samples[selectedIndex]
            DiagramPreview(
                name = name,
                diagram = diagram,
                theme = currentTheme,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ThemeSelector(
    currentTheme: MermaidTheme,
    onThemeChange: (MermaidTheme) -> Unit
) {
    val themes = listOf(
        "Default" to MermaidTheme.Default,
        "Dark" to MermaidTheme.Dark,
        "Forest" to MermaidTheme.Forest,
        "Neutral" to MermaidTheme.Neutral
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Theme:",
            style = MaterialTheme.typography.bodyMedium
        )

        themes.forEach { (name, theme) ->
            FilterChip(
                selected = currentTheme == theme,
                onClick = { onThemeChange(theme) },
                label = { Text(name) }
            )
        }
    }
}

@Composable
private fun SampleItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun DiagramPreview(
    name: String,
    diagram: String,
    theme: MermaidTheme,
    modifier: Modifier = Modifier
) {
    val compositionResult = rememberMermaidComposition(diagram)

    Card(
        modifier = modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Source code
            Text(
                text = "Source:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp)
            ) {
                Text(
                    text = diagram,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Diagram render
            Text(
                text = "Rendered:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                when (compositionResult) {
                    is MermaidCompositionResult.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is MermaidCompositionResult.Success -> {
                        MermaidDiagram(
                            composition = compositionResult.composition,
                            modifier = Modifier.fillMaxSize(),
                            theme = theme,
                            enableZoom = true
                        )
                    }
                    is MermaidCompositionResult.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Error parsing diagram",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = compositionResult.exception.message ?: "Unknown error",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // Size info
            if (compositionResult is MermaidCompositionResult.Success) {
                val composition = compositionResult.composition
                Text(
                    text = "Size: ${composition.width.toInt()} x ${composition.height.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
