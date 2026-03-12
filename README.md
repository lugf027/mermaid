# Mermaid-KMP

**Kotlin Multiplatform 实现的 Mermaid 图表引擎** — 纯 Kotlin 实现 Mermaid 图表的解析与 SVG 渲染，不依赖浏览器、DOM 或 JavaScript 运行时。

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-blue.svg)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.0-green.svg)](https://www.jetbrains.com/compose-multiplatform/)
[![License](https://img.shields.io/badge/license-MIT-lightgrey.svg)]()

## 概述

Mermaid-KMP 将 [Mermaid.js](https://mermaid.js.org/) 的图表解析和 SVG 渲染能力移植到 Kotlin Multiplatform 环境中，实现了完全脱离浏览器/DOM/d3.js 的纯 Kotlin SVG 生成。核心库支持 6 个目标平台，可在 Android、iOS、Desktop、Web 和服务端等场景中使用。

### 核心亮点

- **纯 Kotlin 跨平台**：通过自研 SVG IR 系统（`SvgElement` sealed class）完全脱离 DOM/d3 依赖
- **精确对标 mermaid-js**：代码结构和渲染结果精确对标 mermaid-js 原版
- **完整的 Dagre 布局引擎**：~100KB 纯 Kotlin 实现的 Sugiyama 分层有向图布局算法
- **自动化评估体系**：内置 `mermaid-eval` 工具，自动对比 KMP 与 JS 的 SVG 输出差异
- **跨平台示例应用**：覆盖 Android / iOS / Desktop / Web 四端

## 支持的图表类型

| 图表类型 | 状态 | 说明 |
|----------|------|------|
| **Flowchart** | ✅ 已实现 | 流程图（v2 + legacy），支持方向、形状、边类型、子图、classDef |
| **Pie Chart** | ✅ 已实现 | 饼图，支持 showData、title、扇区标签 |
| Sequence Diagram | 🔲 计划中 | 时序图 |
| Class Diagram | 🔲 计划中 | 类图 |
| State Diagram | 🔲 计划中 | 状态图 |
| ER Diagram | 🔲 计划中 | 实体关系图 |
| Gantt | 🔲 计划中 | 甘特图 |
| 其他 20+ 种 | 🔲 计划中 | gitGraph, mindmap, timeline, c4, sankey 等 |

> 所有 30+ 种 mermaid 图表类型已内置**类型检测器**，可正确识别图表类型，仅解析和渲染待实现。

## 平台支持

| 平台 | mermaid-core | mermaid-cli | mermaid-eval | example |
|------|:---:|:---:|:---:|:---:|
| **JVM** | ✅ | ✅ | ✅ | ✅ (Desktop) |
| **Android** | ✅ | - | - | ✅ |
| **iOS (arm64)** | ✅ | - | - | ✅ |
| **iOS Simulator (arm64)** | ✅ | - | - | ✅ |
| **JS (IR)** | ✅ | - | - | ✅ (Web) |
| **WasmJS** | ✅ | - | - | ✅ (Web) |

## 项目结构

```
mermaid-kmp/
├── mermaid-core/        # 核心库（多平台）— 解析 + 布局 + 渲染
├── mermaid-cli/         # 命令行工具（JVM）— .mmd → .svg 转换
├── mermaid-eval/        # 评估工具（JVM）— KMP vs JS SVG 差异评分
└── example/             # 示例应用
    ├── shared/          # 共享 UI（Compose Multiplatform）
    ├── androidApp/      # Android 入口
    ├── desktopApp/      # Desktop 入口
    ├── webApp/          # Web 入口 (JS + WasmJS)
    └── iosApp/          # iOS 入口 (SwiftUI + Compose)
```

### 模块依赖关系

```
mermaid-core ← mermaid-cli
             ← mermaid-eval
             ← example/shared ← androidApp
                               ← desktopApp
                               ← webApp
                               ← iosApp
```

## 快速开始

### 环境要求

- **JDK** 11+
- **Gradle** 8.14+（已通过 Gradle Wrapper 内置）
- **mmdc**（可选，`mermaid-eval` 需要，用于生成 JS 参考 SVG）
  ```bash
  npm install -g @mermaid-js/mermaid-cli
  ```

### 作为库使用

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":mermaid-core"))
}
```

```kotlin
import io.lugf027.github.mermaid.core.MermaidApi

// 初始化
MermaidApi.initialize()

// 一步渲染：Mermaid 文本 → SVG 字符串
val svg = MermaidApi.renderToSvg("""
    flowchart TD
        A[Start] --> B{Decision}
        B -->|Yes| C[OK]
        B -->|No| D[Cancel]
""")

// 分步操作
val diagram = MermaidApi.parse(mermaidText)     // 解析
val svgRoot = MermaidApi.render(diagram)         // 渲染为 SVG IR
val svgXml = SvgSerializer.serialize(svgRoot)    // 序列化为 XML
```

### 命令行工具 (mermaid-cli)

```bash
# 构建 fat JAR
./gradlew :mermaid-cli:fatJar

# 转换 .mmd 文件为 .svg
java -jar mermaid-cli/build/libs/mermaid-cli-all.jar -i diagram.mmd -o diagram.svg

# 使用暗色主题
java -jar mermaid-cli/build/libs/mermaid-cli-all.jar -i diagram.mmd --theme dark

# 或使用 Gradle 直接运行
./gradlew :mermaid-cli:jvmRun --args="-i diagram.mmd -o diagram.svg"
```

**CLI 参数：**

| 参数 | 简写 | 说明 | 默认值 |
|------|------|------|--------|
| `--input <file>` | `-i` | 输入 .mmd 文件（必需） | - |
| `--output <file>` | `-o` | 输出 .svg 文件 | `<input>.svg` |
| `--theme <name>` | `-t` | 主题 (default/dark/forest/neutral) | default |
| `--indent` | - | 格式化 SVG 输出 | false |
| `--verbose` | `-v` | 显示详细错误信息 | false |
| `--help` | `-h` | 显示帮助 | - |
| `--version` | - | 显示版本 | - |

### 评估工具 (mermaid-eval)

自动对比 mermaid-kmp 与 mermaid-js (mmdc) 的 SVG 输出差异：

```bash
# 运行评估（使用绝对路径）
./gradlew :mermaid-eval:jvmRun --args="-d /absolute/path/to/mmd/files"

# 强制重生成 + JSON 报告 + 自定义阈值
./gradlew :mermaid-eval:jvmRun --args="-d /path/to/mmd -f --json report.json -t 0.98"

# 构建 fat JAR 运行
./gradlew :mermaid-eval:fatJar
java -jar mermaid-eval/build/libs/mermaid-eval-all.jar -d /path/to/mmd -f
```

> ⚠️ **注意**：`jvmRun` 的工作目录可能与 shell 的 `cwd` 不同，`-d` 参数请使用**绝对路径**。

**评估维度与权重：**

| 维度 | 权重 | 说明 |
|------|------|------|
| nodes | 35% | 节点位置精度 |
| edges | 35% | 边路径精度 |
| css | 10% | CSS 类匹配率 |
| structure | 10% | 节点/边数量一致性 |
| viewBox | 5% | viewBox 宽高对齐度 |
| markers | 5% | 箭头标记匹配率 |

**输出示例：**

```
Case                           │  Total │  viewBox │    nodes │    edges │      css │  markers │ Status
───────────────────────────────────────────────────────────────────────────────────────────────────────
mermaid_flowchart_1            │ 0.9986 │   0.9714 │   1.0000 │   1.0000 │   1.0000 │   1.0000 │     🏆
mermaid_flowchart_2            │ 0.9982 │   0.9636 │   1.0000 │   1.0000 │   1.0000 │   1.0000 │     🏆
───────────────────────────────────────────────────────────────────────────────────────────────────────
📊 Summary:  Total: 15  Passed: 15 ✅  Failed: 0 ❌  Avg: 0.9870
```

### 运行示例应用

```bash
# Desktop 应用
./gradlew :example:desktopApp:run

# Android 应用（需要连接设备或模拟器）
./gradlew :example:androidApp:installDebug

# Web 应用 (JS)
./gradlew :example:webApp:jsBrowserDevelopmentRun

# Web 应用 (WasmJS)
./gradlew :example:webApp:wasmJsBrowserDevelopmentRun

# iOS 应用（使用 Xcode 打开）
open example/iosApp/iosApp.xcodeproj
```

### 运行测试

```bash
# 运行所有平台的测试
./gradlew :mermaid-core:allTests

# 仅运行 JVM 测试
./gradlew :mermaid-core:jvmTest
```

## 架构设计

### 渲染管线

```
                          ┌─────────────────────────────────────────────────┐
 Mermaid 文本              │               mermaid-core                      │
     │                    │                                                 │
     ▼                    │  ┌──────────┐   ┌──────────┐   ┌────────────┐  │
 Preprocessor ──────────▶ │  │ Detector │──▶│  Parser  │──▶│ DiagramDB  │  │
 (frontmatter/directive/  │  │ Registry │   │(手写递归) │   │ (数据模型)  │  │
  comment 处理)           │  └──────────┘   └──────────┘   └─────┬──────┘  │
                          │                                      │         │
                          │                               ┌──────▼──────┐  │
                          │                               │   Layout    │  │
                          │                               │  (Dagre)    │  │
                          │                               └──────┬──────┘  │
                          │                                      │         │
                          │  ┌──────────┐   ┌──────────┐   ┌────▼───────┐  │
 SVG XML 字符串 ◀──────── │  │Serializer│◀──│ SvgElement│◀──│  Renderer  │  │
                          │  │          │   │   (IR)    │   │(shapes/    │  │
                          │  └──────────┘   └──────────┘   │edges/css)  │  │
                          │                                └────────────┘  │
                          └─────────────────────────────────────────────────┘
```

### 核心设计模式

每种图表类型由 `DiagramDefinition` 统一组装，包含三个核心组件：

| 组件 | 接口 | 职责 |
|------|------|------|
| **DiagramDB** | `DiagramDB` | 存储解析结果的数据库 |
| **DiagramParser** | `DiagramParser` | 将 Mermaid 文本解析并写入 DB |
| **DiagramRenderer** | `DiagramRenderer` | 从 DB 读取数据生成 SVG IR |

```kotlin
data class DiagramDefinition(
    val id: String,
    val detector: DiagramDetector,          // 文本 → 是否匹配此类型
    val dbFactory: () -> DiagramDB,         // 每次解析创建新实例
    val parser: DiagramParser,              // 解析器
    val renderer: DiagramRenderer,          // 渲染器
    val styles: ((ThemeVariables) -> String)? = null,
    val init: ((MermaidConfig) -> Unit)? = null
)
```

### 渲染模式

| 模式 | 适用图表 | 流程 |
|------|---------|------|
| **统一渲染** | Flowchart, Class, State 等 | DB → LayoutData → DagreLayout → Shapes + Edges → SVG |
| **自定义渲染** | Pie, Gantt, Sequence 等 | DB → 直接构建 SVG 元素 → SVG |

### SVG IR 系统

项目构建了一套纯 Kotlin 的 SVG 中间表示系统，替代 DOM/d3 操作：

- `SvgElement`（sealed class）— 所有 SVG 元素的基类
- 20+ 具体子类：`SvgRoot`, `SvgGroup`, `SvgRect`, `SvgCircle`, `SvgPath`, `SvgText` 等
- `SvgBuilder` — DSL 风格的构建 API
- `SvgPathBuilder` — 等价于 d3.arc/d3.line 的路径构建器
- `SvgSerializer` — SVG IR → XML 字符串序列化

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Kotlin | 2.3.10 | 编程语言 |
| Kotlin Multiplatform | - | 跨平台框架 |
| Compose Multiplatform | 1.10.0 | 跨平台 UI (示例应用) |
| kotlinx.serialization | 1.8.1 | JSON 序列化 |
| kotlinx.coroutines | 1.10.2 | 协程支持 |
| Coil 3 | 3.4.0 | SVG 图片渲染 (示例应用) |
| Gradle | 8.14.3 | 构建工具 |

## 目录约定

```
mermaid-core/src/
└── commonMain/kotlin/io/lugf027/github/mermaid/core/
    ├── config/          # 配置管理
    ├── detect/          # 图表类型检测
    ├── diagram/         # 图表定义（每种图表一个子包）
    │   ├── flowchart/   #   流程图 (DB + Parser + Renderer)
    │   ├── pie/         #   饼图 (DB + Parser + Renderer)
    │   └── error/       #   错误回退图
    ├── layout/          # 布局算法
    │   └── dagre/       #   Dagre Sugiyama 布局引擎
    ├── preprocess/      # 预处理 (frontmatter/directive/comment)
    ├── rendering/       # 渲染组件
    │   ├── clusters/    #   子图渲染
    │   ├── edges/       #   边渲染
    │   ├── markers/     #   箭头标记
    │   ├── shapes/      #   节点形状（20 种）
    │   └── svg/         #   SVG IR 系统
    ├── themes/          # 主题系统（5 种内置主题）
    └── util/            # 工具类
```

## 参考资源

- [Mermaid.js 官方文档](https://mermaid.js.org/)
- [Mermaid.js GitHub](https://github.com/mermaid-js/mermaid)
- [Dagre.js (布局算法参考)](https://github.com/dagrejs/dagre)
- [Kotlin Multiplatform 文档](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform 文档](https://www.jetbrains.com/compose-multiplatform/)

## License

MIT
