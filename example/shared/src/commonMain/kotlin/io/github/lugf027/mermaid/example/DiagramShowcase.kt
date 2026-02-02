/**
 * Diagram showcase component for demonstrating Mermaid diagrams.
 * Implements responsive layout for mobile, tablet, and desktop platforms.
 */
package io.github.lugf027.mermaid.example

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lugf027.mermaid.MermaidCompositionResult
import io.github.lugf027.mermaid.MermaidDiagram
import io.github.lugf027.mermaid.rememberMermaidComposition
import io.github.lugf027.mermaid.theme.MermaidTheme

// ============================================================================
// Window Size Class - Responsive Layout Breakpoints
// ============================================================================

/**
 * Defines window size classes for responsive layout.
 */
private enum class WindowSizeClass {
    /** Compact: < 600dp - Mobile portrait */
    Compact,
    /** Medium: 600-900dp - Tablet/medium window with collapsible sidebar */
    Medium,
    /** Expanded: >= 900dp - Desktop with fixed two-column layout */
    Expanded
}

/**
 * Calculate window size class based on available width.
 */
private fun calculateWindowSizeClass(widthDp: Dp): WindowSizeClass = when {
    widthDp < 600.dp -> WindowSizeClass.Compact
    widthDp < 900.dp -> WindowSizeClass.Medium
    else -> WindowSizeClass.Expanded
}

// ============================================================================
// Main Entry Point
// ============================================================================

/**
 * Main showcase screen for demonstrating Mermaid diagrams.
 * Automatically adapts layout based on window size.
 */
@Composable
public fun DiagramShowcase() {
    val samples = remember { SampleDiagrams.getAllSamples() }
    var selectedIndex by remember { mutableStateOf(0) }
    var currentTheme by remember { mutableStateOf(MermaidTheme.Default) }
    var sidebarExpanded by remember { mutableStateOf(true) }
    // For compact mode: null means show list, non-null means show detail
    var showDetailInCompact by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val windowSizeClass = calculateWindowSizeClass(maxWidth)

        when (windowSizeClass) {
            WindowSizeClass.Compact -> CompactLayout(
                samples = samples,
                selectedIndex = selectedIndex,
                currentTheme = currentTheme,
                showDetail = showDetailInCompact,
                onSampleSelect = { index ->
                    selectedIndex = index
                    showDetailInCompact = true
                },
                onBackToList = { showDetailInCompact = false },
                onThemeChange = { currentTheme = it }
            )

            WindowSizeClass.Medium -> MediumLayout(
                samples = samples,
                selectedIndex = selectedIndex,
                currentTheme = currentTheme,
                sidebarExpanded = sidebarExpanded,
                onSampleSelect = { selectedIndex = it },
                onThemeChange = { currentTheme = it },
                onToggleSidebar = { sidebarExpanded = !sidebarExpanded }
            )

            WindowSizeClass.Expanded -> ExpandedLayout(
                samples = samples,
                selectedIndex = selectedIndex,
                currentTheme = currentTheme,
                onSampleSelect = { selectedIndex = it },
                onThemeChange = { currentTheme = it }
            )
        }
    }
}

// ============================================================================
// Compact Layout - Mobile Portrait (List -> Detail Navigation)
// ============================================================================

@Composable
private fun CompactLayout(
    samples: List<Pair<String, String>>,
    selectedIndex: Int,
    currentTheme: MermaidTheme,
    showDetail: Boolean,
    onSampleSelect: (Int) -> Unit,
    onBackToList: () -> Unit,
    onThemeChange: (MermaidTheme) -> Unit
) {
    AnimatedContent(
        targetState = showDetail,
        transitionSpec = {
            if (targetState) {
                // Navigating to detail: slide in from right
                slideInHorizontally(
                    animationSpec = tween(300),
                    initialOffsetX = { fullWidth -> fullWidth }
                ) togetherWith slideOutHorizontally(
                    animationSpec = tween(300),
                    targetOffsetX = { fullWidth -> -fullWidth }
                )
            } else {
                // Navigating back to list: slide in from left
                slideInHorizontally(
                    animationSpec = tween(300),
                    initialOffsetX = { fullWidth -> -fullWidth }
                ) togetherWith slideOutHorizontally(
                    animationSpec = tween(300),
                    targetOffsetX = { fullWidth -> fullWidth }
                )
            }
        },
        label = "CompactLayoutTransition"
    ) { isDetailVisible ->
        if (isDetailVisible) {
            // Detail Page
            CompactDetailPage(
                sample = samples[selectedIndex],
                theme = currentTheme,
                onBack = onBackToList,
                onThemeChange = onThemeChange
            )
        } else {
            // List Page
            CompactListPage(
                samples = samples,
                selectedIndex = selectedIndex,
                onSampleSelect = onSampleSelect
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactListPage(
    samples: List<Pair<String, String>>,
    selectedIndex: Int,
    onSampleSelect: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "KMP Mermaid Demo",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Kotlin Multiplatform Mermaid Renderer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            itemsIndexed(samples) { index, (name, _) ->
                SampleItem(
                    name = name,
                    isSelected = index == selectedIndex,
                    onClick = { onSampleSelect(index) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactDetailPage(
    sample: Pair<String, String>,
    theme: MermaidTheme,
    onBack: () -> Unit,
    onThemeChange: (MermaidTheme) -> Unit
) {
    val (name, diagram) = sample

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("← Back", style = MaterialTheme.typography.bodyLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Theme selector with flow layout for narrow screens
            AdaptiveThemeSelector(
                currentTheme = theme,
                onThemeChange = onThemeChange,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Source code section
            SourceCodeSection(
                diagram = diagram,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Rendered diagram
            DiagramRenderSection(
                diagram = diagram,
                theme = theme,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 300.dp, max = 500.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ============================================================================
// Medium Layout - Tablet/Medium Window (Collapsible Sidebar)
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediumLayout(
    samples: List<Pair<String, String>>,
    selectedIndex: Int,
    currentTheme: MermaidTheme,
    sidebarExpanded: Boolean,
    onSampleSelect: (Int) -> Unit,
    onThemeChange: (MermaidTheme) -> Unit,
    onToggleSidebar: () -> Unit
) {
    val sidebarWidth by animateDpAsState(
        targetValue = if (sidebarExpanded) 280.dp else 0.dp,
        animationSpec = tween(300),
        label = "SidebarWidth"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "KMP Mermaid Demo",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Kotlin Multiplatform Mermaid Renderer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onToggleSidebar) {
                        Text(
                            text = if (sidebarExpanded) "⊗ Close" else "☰ Menu",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Collapsible Sidebar
            AnimatedVisibility(
                visible = sidebarExpanded,
                enter = slideInHorizontally(animationSpec = tween(300)) { -it } + fadeIn(tween(300)),
                exit = slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut(tween(300))
            ) {
                Surface(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(samples) { index, (name, _) ->
                            SampleItem(
                                name = name,
                                isSelected = index == selectedIndex,
                                onClick = { onSampleSelect(index) }
                            )
                        }
                    }
                }
            }

            // Main content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val (name, diagram) = samples[selectedIndex]

                // Title
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Theme selector
                AdaptiveThemeSelector(
                    currentTheme = currentTheme,
                    onThemeChange = onThemeChange,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Source code
                SourceCodeSection(
                    diagram = diagram,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Rendered diagram
                DiagramRenderSection(
                    diagram = diagram,
                    theme = currentTheme,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 300.dp, max = 600.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ============================================================================
// Expanded Layout - Desktop (Fixed Two-Column)
// ============================================================================

@Composable
private fun ExpandedLayout(
    samples: List<Pair<String, String>>,
    selectedIndex: Int,
    currentTheme: MermaidTheme,
    onSampleSelect: (Int) -> Unit,
    onThemeChange: (MermaidTheme) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "KMP Mermaid Demo",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Kotlin Multiplatform Mermaid Diagram Renderer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Theme selector in header for expanded layout
            AdaptiveThemeSelector(
                currentTheme = currentTheme,
                onThemeChange = onThemeChange
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main content with fixed sidebar
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Fixed sidebar
            Surface(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(samples) { index, (name, _) ->
                        SampleItem(
                            name = name,
                            isSelected = index == selectedIndex,
                            onClick = { onSampleSelect(index) }
                        )
                    }
                }
            }

            // Diagram preview
            val (name, diagram) = samples[selectedIndex]
            DiagramPreviewCard(
                name = name,
                diagram = diagram,
                theme = currentTheme,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ============================================================================
// Shared Components
// ============================================================================

/**
 * Adaptive theme selector that uses FlowRow for narrow screens.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdaptiveThemeSelector(
    currentTheme: MermaidTheme,
    onThemeChange: (MermaidTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    val themes = listOf(
        "Default" to MermaidTheme.Default,
        "Dark" to MermaidTheme.Dark,
        "Forest" to MermaidTheme.Forest,
        "Neutral" to MermaidTheme.Neutral
    )

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Theme:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.CenterVertically)
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
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(16.dp),
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun SourceCodeSection(
    diagram: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Source:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
        ) {
            Text(
                text = diagram,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun DiagramRenderSection(
    diagram: String,
    theme: MermaidTheme,
    modifier: Modifier = Modifier
) {
    val compositionResult = rememberMermaidComposition(diagram)

    Column(modifier = modifier) {
        Text(
            text = "Rendered:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
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

/**
 * Full diagram preview card for expanded layout.
 */
@Composable
private fun DiagramPreviewCard(
    name: String,
    diagram: String,
    theme: MermaidTheme,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Source code
            SourceCodeSection(
                diagram = diagram,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Rendered diagram
            DiagramRenderSection(
                diagram = diagram,
                theme = theme,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}
