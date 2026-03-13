<p align="center">
  <h1 align="center">Mermaid-KMP</h1>
  <p align="center">
    <strong>Mermaid 图表引擎的 Kotlin Multiplatform 重新实现</strong>
  </p>
  <p align="center">
    纯 Kotlin 实现 Mermaid 图表的解析与 SVG 渲染 — 无需浏览器、DOM 或 JavaScript 运行时。
  </p>
  <p align="center">
    <a href="README.md">English</a>
  </p>
</p>

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.0-4285F4?logo=jetbrains&logoColor=white)](https://www.jetbrains.com/compose-multiplatform/)
[![Platforms](https://img.shields.io/badge/平台-JVM%20|%20Android%20|%20iOS%20|%20JS%20|%20WasmJS-blue)]()
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> **⚠️ 迭代开发中：** 本项目正处于积极的迭代开发阶段。所有图表类型的像素级还原预计将于 **2026 年 3 月**完成。请参阅[图表支持状态](#支持的图表类型)了解当前进度。

## 概述

Mermaid-KMP 将 [Mermaid.js](https://mermaid.js.org/) 的图表解析和 SVG 渲染能力完整移植到 Kotlin Multiplatform 生态中。它提供了一个完全自包含的图表引擎，能够解析 Mermaid 语法并生成 SVG 输出 — 完全使用纯 Kotlin 实现，不依赖浏览器环境、DOM 或 d3.js。

核心库支持 **6 个目标平台**，可在 Android、iOS、Desktop (JVM)、Web (JS/WasmJS) 以及服务端等场景中使用。

### 核心亮点

- **纯 Kotlin 跨平台** — 通过自研 SVG IR 系统（`SvgElement` sealed class 体系）完全脱离 DOM/d3 依赖
- **忠实对标 Mermaid.js** — 代码结构和渲染管线紧密对标 mermaid-js 原版实现
- **完整的 Dagre 布局引擎** — ~100KB 纯 Kotlin 实现的 Sugiyama 分层有向图布局算法
- **内置评估框架** — `mermaid-eval` 模块自动对比 KMP 与 JS 的 SVG 输出，用于质量保证
- **多平台示例应用** — 覆盖 Android、iOS、Desktop、Web 四端的完整演示应用

## 支持的图表类型

Mermaid-KMP 支持 Mermaid.js 生态中的全部 **28 种图表类型**。下表展示了每种类型的当前实现状态：

| 图表类型 | 状态 | 渲染精度 | 说明 |
|:---------|:----:|:--------:|:-----|
| **Flowchart（流程图）** | ✅ 已实现 | 🏆 像素级 (~0.99) | 流程图（v2 + legacy），支持方向、形状、边类型、子图、classDef |
| **Flowchart-ELK** | ✅ 已实现 | ⚙️ 迭代中 | 使用 ELK 布局的流程图变体（复用流程图引擎） |
| **Pie Chart（饼图）** | ✅ 已实现 | ⚙️ 迭代中 | 饼图，支持 showData、title、扇区标签 |
| **Sequence Diagram（时序图）** | ✅ 已实现 | ⚙️ 迭代中 | 消息、激活、注释、循环、alt/opt/par |
| **Class Diagram（类图）** | ✅ 已实现 | ⚙️ 迭代中 | 类、接口、关系、方法、成员 |
| **State Diagram（状态图）** | ✅ 已实现 | ⚙️ 迭代中 | 状态、转换、复合状态、分叉、汇合 |
| **ER Diagram（实体关系图）** | ✅ 已实现 | ⚙️ 迭代中 | 实体关系图 |
| **Gantt Chart（甘特图）** | ✅ 已实现 | ⚙️ 迭代中 | 任务、分区、里程碑、依赖关系 |
| **Git Graph（Git 图）** | ✅ 已实现 | ⚙️ 迭代中 | 提交、分支、合并、标签 |
| **C4 Diagram（C4 图）** | ✅ 已实现 | ⚙️ 迭代中 | C4 模型（上下文、容器、组件、代码） |
| **Mindmap（思维导图）** | ✅ 已实现 | ⚙️ 迭代中 | 层级思维导图 |
| **Timeline（时间线）** | ✅ 已实现 | ⚙️ 迭代中 | 时间线/年代图 |
| **Journey（用户旅程图）** | ✅ 已实现 | ⚙️ 迭代中 | 用户旅程图 |
| **Quadrant Chart（象限图）** | ✅ 已实现 | ⚙️ 迭代中 | 四象限分类图 |
| **XY Chart（XY 图表）** | ✅ 已实现 | ⚙️ 迭代中 | 条形图和折线图 |
| **Sankey Diagram（桑基图）** | ✅ 已实现 | ⚙️ 迭代中 | 流量/能量图 |
| **Radar Chart（雷达图）** | ✅ 已实现 | ⚙️ 迭代中 | 蛛网图/雷达图 |
| **Requirement Diagram（需求图）** | ✅ 已实现 | ⚙️ 迭代中 | 需求及关系图 |
| **Block Diagram（块图）** | ✅ 已实现 | ⚙️ 迭代中 | 基于块的布局图 |
| **Packet Diagram（数据包图）** | ✅ 已实现 | ⚙️ 迭代中 | 网络数据包结构 |
| **Kanban Board（看板）** | ✅ 已实现 | ⚙️ 迭代中 | 看板任务板 |
| **Architecture Diagram（架构图）** | ✅ 已实现 | ⚙️ 迭代中 | 系统架构图 |
| **Ishikawa Diagram（鱼骨图）** | ✅ 已实现 | ⚙️ 迭代中 | 鱼骨图/因果图 |
| **Venn Diagram（韦恩图）** | ✅ 已实现 | ⚙️ 迭代中 | 集合图，支持交集 |
| **Treemap（树形图）** | ✅ 已实现 | ⚙️ 迭代中 | 层级面积占比图 |
| **Info（信息图）** | ✅ 已实现 | ⚙️ 迭代中 | Mermaid 版本信息 |
| **Error（错误图）** | ✅ 内置 | — | 回退用错误图表 |

**图例说明：**
- 🏆 **像素级** — SVG 输出与 mermaid-js 高度一致（评估分数 ≥ 0.95）
- ⚙️ **迭代中** — 解析和渲染已实现，像素级精度仍在优化中

> 目前，**Flowchart（流程图）** 是唯一达到像素级渲染精度的图表类型（评估分数 ~0.99）。其他所有类型均已实现完整的解析和渲染功能，但仍在持续迭代优化以达到像素级精度。预计 **2026 年 3 月**完成所有类型的全面对齐。

## 平台支持

| 平台 | mermaid-core | mermaid-cli | mermaid-eval | 示例应用 |
|:-----|:---:|:---:|:---:|:---:|
| **JVM (Desktop/Server)** | ✅ | ✅ | ✅ | ✅ |
| **Android** | ✅ | — | — | ✅ |
| **iOS (arm64)** | ✅ | — | — | ✅ |
| **iOS Simulator (arm64)** | ✅ | — | — | ✅ |
| **JS (IR)** | ✅ | — | — | ✅ |
| **WasmJS** | ✅ | — | — | ✅ |

## 项目结构

```
mermaid-kmp/
├── mermaid-core/        # 核心库（多平台）— 解析 + 布局 + 渲染
├── mermaid-cli/         # 命令行工具（仅 JVM）— .mmd → .svg 转换
├── mermaid-eval/        # 评估工具（仅 JVM）— KMP vs JS SVG 差异评分
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

### 安装

#### 作为 Gradle 依赖使用

```kotlin
// settings.gradle.kts — 将 mermaid-kmp 作为 composite build 或 submodule 引入
includeBuild("path/to/mermaid-kmp")

// build.gradle.kts
dependencies {
    implementation(project(":mermaid-core"))
}
```

#### 从源码构建

```bash
git clone https://github.com/user/mermaid-kmp.git
cd mermaid-kmp
./gradlew :mermaid-core:build
```

### 基本用法

```kotlin
import io.lugf027.github.mermaid.core.MermaidApi
import io.lugf027.github.mermaid.core.rendering.svg.SvgSerializer

// 初始化（在应用启动时调用一次）
MermaidApi.initialize()

// 一步渲染：Mermaid 文本 → SVG 字符串
val svg = MermaidApi.renderToSvg("""
    flowchart TD
        A[开始] --> B{判断}
        B -->|是| C[确认]
        B -->|否| D[取消]
""")

// 分步操作，获得更多控制
val diagram = MermaidApi.parse(mermaidText)     // 解析
val svgRoot = MermaidApi.render(diagram)         // 渲染为 SVG IR
val svgXml = SvgSerializer.serialize(svgRoot)    // 序列化为 XML 字符串
```

#### 主题支持

```kotlin
import io.lugf027.github.mermaid.core.config.MermaidConfig

// 使用指定主题初始化
MermaidApi.initialize(MermaidConfig(theme = "dark"))

// 可用主题：default, dark, forest, neutral, base
```

#### 用于 HTML 嵌入的渲染

```kotlin
// 获取不含 XML 声明的 SVG 内容（适合嵌入 HTML）
val svgContent = MermaidApi.renderToSvgContent("""
    pie title 宠物偏好
        "狗" : 45
        "猫" : 30
        "鸟" : 25
""")
```

### 命令行工具 (mermaid-cli)

从命令行将 `.mmd` 文件转换为 `.svg`：

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
|:-----|:----:|:-----|:-------|
| `--input <file>` | `-i` | 输入 .mmd 文件（必需） | — |
| `--output <file>` | `-o` | 输出 .svg 文件 | `<input>.svg` |
| `--theme <name>` | `-t` | 主题 (default/dark/forest/neutral) | default |
| `--indent` | — | 格式化 SVG 输出 | false |
| `--verbose` | `-v` | 显示详细错误信息 | false |
| `--help` | `-h` | 显示帮助 | — |
| `--version` | — | 显示版本 | — |

### 评估工具 (mermaid-eval)

自动对比 mermaid-kmp 与 mermaid-js (mmdc) 的 SVG 输出差异：

```bash
# 运行评估（使用绝对路径）
./gradlew :mermaid-eval:jvmRun --args="-d /absolute/path/to/mmd/files"

# 强制重生成 + JSON 报告 + 自定义阈值
./gradlew :mermaid-eval:jvmRun --args="-d /path/to/mmd -f --json report.json -t 0.98"
```

> ⚠️ **注意**：`jvmRun` 的工作目录可能与 shell 的 `cwd` 不同，`-d` 参数请使用**绝对路径**。

**评估维度与权重：**

| 维度 | 权重 | 说明 |
|:-----|:----:|:-----|
| 节点 (nodes) | 35% | 节点位置精度 |
| 边 (edges) | 35% | 边路径精度 |
| CSS | 10% | CSS 类匹配率 |
| 结构 (structure) | 10% | 节点/边数量一致性 |
| viewBox | 5% | viewBox 宽高对齐度 |
| 标记 (markers) | 5% | 箭头标记匹配率 |

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
 预处理器 ────────────────▶ │  │  检测器  │──▶│  解析器  │──▶│ DiagramDB  │  │
 (frontmatter/directive/  │  │ Registry │   │(手写递归) │   │ (数据模型)  │  │
  注释处理)               │  └──────────┘   └──────────┘   └─────┬──────┘  │
                          │                                      │         │
                          │                               ┌──────▼──────┐  │
                          │                               │    布局      │  │
                          │                               │  (Dagre)    │  │
                          │                               └──────┬──────┘  │
                          │                                      │         │
                          │  ┌──────────┐   ┌──────────┐   ┌────▼───────┐  │
 SVG XML 字符串 ◀──────── │  │ 序列化器 │◀──│ SvgElement│◀──│   渲染器   │  │
                          │  │          │   │   (IR)    │   │(shapes/    │  │
                          │  └──────────┘   └──────────┘   │edges/css)  │  │
                          │                                └────────────┘  │
                          └─────────────────────────────────────────────────┘
```

### 核心设计模式

每种图表类型由 `DiagramDefinition` 统一组装，包含三个核心组件：

| 组件 | 接口 | 职责 |
|:-----|:-----|:-----|
| **DiagramDB** | `DiagramDB` | 存储解析结果的数据库 |
| **DiagramParser** | `DiagramParser` | 将 Mermaid 文本解析并写入 DB |
| **DiagramRenderer** | `DiagramRenderer` | 从 DB 读取数据生成 SVG IR |

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

### 渲染模式

| 模式 | 适用图表 | 流程 |
|:-----|:---------|:-----|
| **统一渲染 (Dagre)** | Flowchart, Class, State, ER 等 | DB → LayoutData → DagreLayout → Shapes + Edges → SVG |
| **自定义渲染** | Pie, Sequence, Gantt 等 | DB → 直接构建 SVG 元素 → SVG |

### SVG IR 系统

项目构建了一套纯 Kotlin 的 SVG 中间表示系统，替代 DOM/d3 操作：

- `SvgElement`（sealed class）— 所有 SVG 元素的基类
- 20+ 具体子类：`SvgRoot`, `SvgGroup`, `SvgRect`, `SvgCircle`, `SvgPath`, `SvgText` 等
- `SvgBuilder` — DSL 风格的构建 API
- `SvgPathBuilder` — 等价于 d3.arc/d3.line 的路径构建器
- `SvgSerializer` — SVG IR → XML 字符串序列化

## 技术栈

| 技术 | 版本 | 用途 |
|:-----|:-----|:-----|
| Kotlin | 2.3.10 | 编程语言 |
| Kotlin Multiplatform | — | 跨平台框架 |
| Compose Multiplatform | 1.10.0 | 跨平台 UI（示例应用） |
| kotlinx.serialization | 1.8.1 | JSON 序列化 |
| kotlinx.coroutines | 1.10.2 | 协程支持 |
| Coil 3 | 3.4.0 | SVG 图片渲染（示例应用） |
| Gradle | 8.14.3 | 构建工具 |

## 参与贡献

请参阅 [CONTRIBUTING.md](CONTRIBUTING.md) 了解如何新增图表类型以及参与项目贡献的详细指南。

### 开发工作流

1. Fork 本仓库
2. 创建功能分支（`git checkout -b feature/my-diagram-type`）
3. 遵循[图表实现指南](CONTRIBUTING.md)
4. 编写测试和评估用例
5. 提交 Pull Request

## 路线图

- [x] 核心引擎：SVG IR、Dagre 布局、主题系统、预处理
- [x] Flowchart — 像素级精度（v2 + legacy）
- [x] Pie Chart — 基础实现
- [x] 全部 28 种图表类型 — 解析与渲染已实现
- [ ] 所有图表类型达到像素级精度（目标：2026 年 3 月）
- [ ] 发布到 Maven Central
- [ ] Compose Multiplatform 渲染组件
- [ ] Gradle 插件支持构建时 SVG 生成

## 参考资源

- [Mermaid.js 官方文档](https://mermaid.js.org/)
- [Mermaid.js GitHub](https://github.com/mermaid-js/mermaid)
- [Dagre.js（布局算法参考）](https://github.com/dagrejs/dagre)
- [Kotlin Multiplatform 文档](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform 文档](https://www.jetbrains.com/compose-multiplatform/)

## 许可证

本项目基于 MIT 许可证开源 — 详见 [LICENSE](LICENSE) 文件。
