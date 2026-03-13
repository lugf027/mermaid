<p align="center">
  <h1 align="center">Mermaid-KMP</h1>
  <p align="center">
    <strong>Mermaid diagram engine reimplemented in Kotlin Multiplatform</strong>
  </p>
  <p align="center">
    Pure Kotlin parsing and SVG rendering of Mermaid diagrams — no browser, DOM, or JavaScript runtime required.
  </p>
  <p align="center">
    <a href="README_ZH.md">中文文档</a>
  </p>
</p>

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.0-4285F4?logo=jetbrains&logoColor=white)](https://www.jetbrains.com/compose-multiplatform/)
[![Platforms](https://img.shields.io/badge/Platforms-JVM%20|%20Android%20|%20iOS%20|%20JS%20|%20WasmJS-blue)]()
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> **⚠️ Active Development:** This project is under active iterative development. Full pixel-level fidelity for all diagram types is targeted for completion by **March 2026**. See [Diagram Support Status](#supported-diagram-types) for current progress.

## Overview

Mermaid-KMP brings the power of [Mermaid.js](https://mermaid.js.org/) to the Kotlin Multiplatform ecosystem. It provides a complete, self-contained diagramming engine that parses Mermaid syntax and generates SVG output — entirely in pure Kotlin, without any dependency on browser environments, the DOM, or d3.js.

The core library targets **6 platforms**, enabling Mermaid diagram rendering in Android, iOS, Desktop (JVM), Web (JS/WasmJS), and server-side applications.

### Key Features

- **Pure Kotlin, Truly Cross-Platform** — Custom SVG IR system (`SvgElement` sealed class hierarchy) eliminates all DOM/d3 dependencies
- **Faithful to Mermaid.js** — Code structure and rendering pipeline closely mirrors the original mermaid-js implementation
- **Complete Dagre Layout Engine** — ~100KB pure Kotlin implementation of the Sugiyama layered graph layout algorithm
- **Built-in Evaluation Framework** — `mermaid-eval` module automatically compares KMP vs JS SVG output for quality assurance
- **Multi-Platform Example App** — Fully functional demo app for Android, iOS, Desktop, and Web

## Supported Diagram Types

Mermaid-KMP supports all **28 diagram types** from the Mermaid.js ecosystem. The table below shows the current implementation status of each type:

| Diagram Type | Status | Rendering Fidelity | Description |
|:-------------|:------:|:------------------:|:------------|
| **Flowchart** | ✅ Implemented | 🏆 Pixel-level (~0.99) | Flow diagrams (v2 + legacy), directions, shapes, edge types, subgraphs, classDef |
| **Flowchart-ELK** | ✅ Implemented | ⚙️ Iterating | ELK-layout variant of flowchart (reuses flowchart engine) |
| **Pie Chart** | ✅ Implemented | ⚙️ Iterating | Pie/donut charts with showData, title, sector labels |
| **Sequence Diagram** | ✅ Implemented | ⚙️ Iterating | Messages, activations, notes, loops, alt/opt/par |
| **Class Diagram** | ✅ Implemented | ⚙️ Iterating | Classes, interfaces, relationships, methods, members |
| **State Diagram** | ✅ Implemented | ⚙️ Iterating | States, transitions, composites, forks, joins |
| **ER Diagram** | ✅ Implemented | ⚙️ Iterating | Entity-relationship diagrams |
| **Gantt Chart** | ✅ Implemented | ⚙️ Iterating | Tasks, sections, milestones, dependencies |
| **Git Graph** | ✅ Implemented | ⚙️ Iterating | Commits, branches, merges, tags |
| **C4 Diagram** | ✅ Implemented | ⚙️ Iterating | C4 model (Context, Container, Component, Code) |
| **Mindmap** | ✅ Implemented | ⚙️ Iterating | Hierarchical mind maps |
| **Timeline** | ✅ Implemented | ⚙️ Iterating | Timeline/chronology diagrams |
| **Journey** | ✅ Implemented | ⚙️ Iterating | User journey maps |
| **Quadrant Chart** | ✅ Implemented | ⚙️ Iterating | Four-quadrant categorization charts |
| **XY Chart** | ✅ Implemented | ⚙️ Iterating | Bar and line charts |
| **Sankey Diagram** | ✅ Implemented | ⚙️ Iterating | Flow/energy diagrams |
| **Radar Chart** | ✅ Implemented | ⚙️ Iterating | Spider/radar charts |
| **Requirement Diagram** | ✅ Implemented | ⚙️ Iterating | Requirements and relationships |
| **Block Diagram** | ✅ Implemented | ⚙️ Iterating | Block-based layouts |
| **Packet Diagram** | ✅ Implemented | ⚙️ Iterating | Network packet structure |
| **Kanban Board** | ✅ Implemented | ⚙️ Iterating | Kanban task boards |
| **Architecture Diagram** | ✅ Implemented | ⚙️ Iterating | System architecture diagrams |
| **Ishikawa Diagram** | ✅ Implemented | ⚙️ Iterating | Fishbone/cause-and-effect diagrams |
| **Venn Diagram** | ✅ Implemented | ⚙️ Iterating | Set diagrams with intersections |
| **Treemap** | ✅ Implemented | ⚙️ Iterating | Hierarchical area-proportional diagrams |
| **Info** | ✅ Implemented | ⚙️ Iterating | Mermaid version info |
| **Error** | ✅ Built-in | — | Fallback error diagram |

**Legend:**
- 🏆 **Pixel-level** — SVG output closely matches mermaid-js (evaluation score ≥ 0.95)
- ⚙️ **Iterating** — Parsing and rendering implemented; pixel-level fidelity is being refined

> Currently, **Flowchart** is the only diagram type that has achieved pixel-level rendering fidelity (~0.99 score). All other types have complete parsing and rendering implementations but are still undergoing iterative refinement to reach pixel-level accuracy. Full parity across all types is targeted for **March 2026**.

## Platform Support

| Platform | mermaid-core | mermaid-cli | mermaid-eval | example app |
|:---------|:---:|:---:|:---:|:---:|
| **JVM (Desktop/Server)** | ✅ | ✅ | ✅ | ✅ |
| **Android** | ✅ | — | — | ✅ |
| **iOS (arm64)** | ✅ | — | — | ✅ |
| **iOS Simulator (arm64)** | ✅ | — | — | ✅ |
| **JS (IR)** | ✅ | — | — | ✅ |
| **WasmJS** | ✅ | — | — | ✅ |

## Project Structure

```
mermaid-kmp/
├── mermaid-core/        # Core library (multiplatform) — parsing + layout + rendering
├── mermaid-cli/         # CLI tool (JVM only) — .mmd → .svg conversion
├── mermaid-eval/        # Evaluation tool (JVM only) — KMP vs JS SVG diff scoring
└── example/             # Example applications
    ├── shared/          # Shared UI (Compose Multiplatform)
    ├── androidApp/      # Android entry point
    ├── desktopApp/      # Desktop entry point
    ├── webApp/          # Web entry point (JS + WasmJS)
    └── iosApp/          # iOS entry point (SwiftUI + Compose)
```

### Module Dependencies

```
mermaid-core ← mermaid-cli
             ← mermaid-eval
             ← example/shared ← androidApp
                               ← desktopApp
                               ← webApp
                               ← iosApp
```

## Getting Started

### Prerequisites

- **JDK** 11+
- **Gradle** 8.14+ (bundled via Gradle Wrapper)
- **mmdc** (optional, required by `mermaid-eval` for generating JS reference SVGs)
  ```bash
  npm install -g @mermaid-js/mermaid-cli
  ```

### Installation

#### As a Gradle Dependency

```kotlin
// settings.gradle.kts — include mermaid-kmp as a composite build or submodule
includeBuild("path/to/mermaid-kmp")

// build.gradle.kts
dependencies {
    implementation(project(":mermaid-core"))
}
```

#### Build from Source

```bash
git clone https://github.com/user/mermaid-kmp.git
cd mermaid-kmp
./gradlew :mermaid-core:build
```

### Basic Usage

```kotlin
import io.lugf027.github.mermaid.core.MermaidApi
import io.lugf027.github.mermaid.core.rendering.svg.SvgSerializer

// Initialize (call once at app startup)
MermaidApi.initialize()

// One-step rendering: Mermaid text → SVG string
val svg = MermaidApi.renderToSvg("""
    flowchart TD
        A[Start] --> B{Decision}
        B -->|Yes| C[OK]
        B -->|No| D[Cancel]
""")

// Step-by-step for more control
val diagram = MermaidApi.parse(mermaidText)     // Parse
val svgRoot = MermaidApi.render(diagram)         // Render to SVG IR
val svgXml = SvgSerializer.serialize(svgRoot)    // Serialize to XML string
```

#### Theme Support

```kotlin
import io.lugf027.github.mermaid.core.config.MermaidConfig

// Initialize with a specific theme
MermaidApi.initialize(MermaidConfig(theme = "dark"))

// Available themes: default, dark, forest, neutral, base
```

#### Rendering for HTML Embedding

```kotlin
// Get SVG content without XML declaration (suitable for embedding in HTML)
val svgContent = MermaidApi.renderToSvgContent("""
    pie title Favorite Pets
        "Dogs" : 45
        "Cats" : 30
        "Birds" : 25
""")
```

### CLI Tool (mermaid-cli)

Convert `.mmd` files to `.svg` from the command line:

```bash
# Build the fat JAR
./gradlew :mermaid-cli:fatJar

# Convert .mmd to .svg
java -jar mermaid-cli/build/libs/mermaid-cli-all.jar -i diagram.mmd -o diagram.svg

# Use dark theme
java -jar mermaid-cli/build/libs/mermaid-cli-all.jar -i diagram.mmd --theme dark

# Or run directly via Gradle
./gradlew :mermaid-cli:jvmRun --args="-i diagram.mmd -o diagram.svg"
```

**CLI Options:**

| Option | Short | Description | Default |
|:-------|:-----:|:------------|:--------|
| `--input <file>` | `-i` | Input .mmd file (required) | — |
| `--output <file>` | `-o` | Output .svg file | `<input>.svg` |
| `--theme <name>` | `-t` | Theme (default/dark/forest/neutral) | default |
| `--indent` | — | Pretty-print SVG output | false |
| `--verbose` | `-v` | Show detailed error messages | false |
| `--help` | `-h` | Show help | — |
| `--version` | — | Show version | — |

### Evaluation Tool (mermaid-eval)

Automatically compare mermaid-kmp output against mermaid-js (mmdc) for quality assurance:

```bash
# Run evaluation (use absolute paths)
./gradlew :mermaid-eval:jvmRun --args="-d /absolute/path/to/mmd/files"

# Force regeneration + JSON report + custom threshold
./gradlew :mermaid-eval:jvmRun --args="-d /path/to/mmd -f --json report.json -t 0.98"
```

> ⚠️ The `-d` argument must use **absolute paths**, as `jvmRun`'s working directory may differ from the shell's current directory.

**Scoring Dimensions:**

| Dimension | Weight | Description |
|:----------|:------:|:------------|
| Nodes | 35% | Node position accuracy |
| Edges | 35% | Edge path accuracy |
| CSS | 10% | CSS class match rate |
| Structure | 10% | Node/edge count consistency |
| ViewBox | 5% | ViewBox dimension alignment |
| Markers | 5% | Arrow marker match rate |

**Sample Output:**

```
Case                           │  Total │  viewBox │    nodes │    edges │      css │  markers │ Status
───────────────────────────────────────────────────────────────────────────────────────────────────────
mermaid_flowchart_1            │ 0.9986 │   0.9714 │   1.0000 │   1.0000 │   1.0000 │   1.0000 │     🏆
mermaid_flowchart_2            │ 0.9982 │   0.9636 │   1.0000 │   1.0000 │   1.0000 │   1.0000 │     🏆
───────────────────────────────────────────────────────────────────────────────────────────────────────
📊 Summary:  Total: 15  Passed: 15 ✅  Failed: 0 ❌  Avg: 0.9870
```

### Running the Example App

```bash
# Desktop
./gradlew :example:desktopApp:run

# Android (requires connected device or emulator)
./gradlew :example:androidApp:installDebug

# Web (JS)
./gradlew :example:webApp:jsBrowserDevelopmentRun

# Web (WasmJS)
./gradlew :example:webApp:wasmJsBrowserDevelopmentRun

# iOS (open in Xcode)
open example/iosApp/iosApp.xcodeproj
```

### Running Tests

```bash
# Run tests on all platforms
./gradlew :mermaid-core:allTests

# JVM tests only
./gradlew :mermaid-core:jvmTest
```

## Architecture

### Rendering Pipeline

```
                          ┌─────────────────────────────────────────────────┐
 Mermaid Text              │               mermaid-core                      │
     │                    │                                                 │
     ▼                    │  ┌──────────┐   ┌──────────┐   ┌────────────┐  │
 Preprocessor ──────────▶ │  │ Detector │──▶│  Parser  │──▶│ DiagramDB  │  │
 (frontmatter/directive/  │  │ Registry │   │(recursive│   │ (data      │  │
  comment handling)       │  └──────────┘   │ descent) │   │  model)    │  │
                          │                 └──────────┘   └─────┬──────┘  │
                          │                                      │         │
                          │                               ┌──────▼──────┐  │
                          │                               │   Layout    │  │
                          │                               │  (Dagre)    │  │
                          │                               └──────┬──────┘  │
                          │                                      │         │
                          │  ┌──────────┐   ┌──────────┐   ┌────▼───────┐  │
 SVG XML String ◀──────── │  │Serializer│◀──│ SvgElement│◀──│  Renderer  │  │
                          │  │          │   │   (IR)    │   │(shapes/    │  │
                          │  └──────────┘   └──────────┘   │edges/css)  │  │
                          │                                └────────────┘  │
                          └─────────────────────────────────────────────────┘
```

### Core Design Pattern

Each diagram type is assembled via `DiagramDefinition`, consisting of three core components:

| Component | Interface | Responsibility |
|:----------|:----------|:---------------|
| **DiagramDB** | `DiagramDB` | Stores parsed diagram data |
| **DiagramParser** | `DiagramParser` | Parses Mermaid text into DB |
| **DiagramRenderer** | `DiagramRenderer` | Generates SVG IR from DB |

```kotlin
data class DiagramDefinition(
    val id: String,
    val detector: DiagramDetector,
    val dbFactory: () -> DiagramDB,
    val parser: DiagramParser,
    val renderer: DiagramRenderer,
    val styles: ((ThemeVariables) -> String)? = null,
    val init: ((MermaidConfig) -> Unit)? = null
)
```

### Rendering Modes

| Mode | Used By | Pipeline |
|:-----|:--------|:---------|
| **Unified (Dagre)** | Flowchart, Class, State, ER, etc. | DB → LayoutData → DagreLayout → Shapes + Edges → SVG |
| **Custom** | Pie, Sequence, Gantt, etc. | DB → Direct SVG element construction → SVG |

### SVG IR System

A pure Kotlin SVG intermediate representation replaces all DOM/d3 operations:

- `SvgElement` (sealed class) — Base for all SVG elements
- 20+ concrete subtypes: `SvgRoot`, `SvgGroup`, `SvgRect`, `SvgCircle`, `SvgPath`, `SvgText`, etc.
- `SvgBuilder` — DSL-style construction API
- `SvgPathBuilder` — Equivalent to d3.arc/d3.line path builders
- `SvgSerializer` — SVG IR → XML string serialization

## Tech Stack

| Technology | Version | Purpose |
|:-----------|:--------|:--------|
| Kotlin | 2.3.10 | Programming language |
| Kotlin Multiplatform | — | Cross-platform framework |
| Compose Multiplatform | 1.10.0 | Cross-platform UI (example app) |
| kotlinx.serialization | 1.8.1 | JSON serialization |
| kotlinx.coroutines | 1.10.2 | Coroutine support |
| Coil 3 | 3.4.0 | SVG image rendering (example app) |
| Gradle | 8.14.3 | Build tool |

## Contributing

Please see [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines on how to add new diagram types and contribute to the project.

### Development Workflow

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-diagram-type`)
3. Follow the [diagram implementation guide](CONTRIBUTING.md)
4. Write tests and evaluation cases
5. Submit a pull request

## Roadmap

- [x] Core engine: SVG IR, Dagre layout, theme system, preprocessing
- [x] Flowchart — pixel-level fidelity (v2 + legacy)
- [x] Pie Chart — basic implementation
- [x] All 28 diagram types — parsing & rendering implemented
- [ ] Pixel-level fidelity for all diagram types (target: March 2026)
- [ ] Publish to Maven Central
- [ ] Compose Multiplatform rendering component
- [ ] Gradle plugin for build-time SVG generation

## References

- [Mermaid.js Documentation](https://mermaid.js.org/)
- [Mermaid.js GitHub](https://github.com/mermaid-js/mermaid)
- [Dagre.js (layout algorithm reference)](https://github.com/dagrejs/dagre)
- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform Documentation](https://www.jetbrains.com/compose-multiplatform/)

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
