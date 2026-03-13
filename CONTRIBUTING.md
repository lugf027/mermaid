# 贡献指南：新增 Mermaid 图表类型

本文档描述如何为 mermaid-kmp 新增一种 Mermaid 图表类型的完整流程。

## 图表实现优先级

### 第二批（推荐优先实现）
| 图表类型 | ID | 渲染模式 | 复杂度 | 说明 |
|----------|-----|----------|--------|------|
| Sequence Diagram | `sequence` | 自定义渲染 | ⭐⭐⭐ | 时序图，需要专用布局 |
| Class Diagram | `classDiagram` | 统一渲染 (Dagre) | ⭐⭐⭐ | 类图，可复用 Dagre 布局 |
| State Diagram | `stateDiagram` | 统一渲染 (Dagre) | ⭐⭐⭐ | 状态图，可复用 Dagre 布局 |
| ER Diagram | `erDiagram` | 统一渲染 (Dagre) | ⭐⭐⭐ | 实体关系图 |
| Gantt | `gantt` | 自定义渲染 | ⭐⭐ | 甘特图，时间轴布局 |

### 第三批
| 图表类型 | ID | 渲染模式 | 复杂度 |
|----------|-----|----------|--------|
| Git Graph | `gitGraph` | 自定义渲染 | ⭐⭐⭐ |
| Journey | `journey` | 自定义渲染 | ⭐⭐ |
| Mindmap | `mindmap` | 自定义渲染 | ⭐⭐ |
| Timeline | `timeline` | 自定义渲染 | ⭐⭐ |
| C4 Diagram | `c4` | 统一渲染 (Dagre) | ⭐⭐⭐ |
| Sankey | `sankey` | 自定义渲染 | ⭐⭐⭐ |
| Quadrant Chart | `quadrantChart` | 自定义渲染 | ⭐⭐ |
| XY Chart | `xychart` | 自定义渲染 | ⭐⭐ |
| Kanban | `kanban` | 自定义渲染 | ⭐⭐ |
| Block | `block` | 统一渲染 (Dagre) | ⭐⭐ |
| 其他 | ... | ... | ... |

## 新增图表类型：完整步骤

以新增 **Sequence Diagram (时序图)** 为例，说明完整开发流程。

### 步骤 1：创建图表包

在 `mermaid-core/src/commonMain/kotlin/io/lugf027/github/mermaid/core/diagram/` 下创建新包：

```
diagram/
├── flowchart/     # 已有
├── pie/           # 已有
├── error/         # 已有
└── sequence/      # ← 新增
    ├── SequenceDb.kt
    ├── SequenceParser.kt
    ├── SequenceRenderer.kt
    └── SequenceDiagram.kt
```

### 步骤 2：实现 DiagramDB — 数据模型

`SequenceDb.kt` — 存储解析结果的数据库。

参考 `FlowchartDb.kt` 和 `PieDb.kt` 的实现模式：

```kotlin
package io.lugf027.github.mermaid.core.diagram.sequence

import io.lugf027.github.mermaid.core.diagram.DiagramDB

class SequenceDb : DiagramDB {
    // 1. 定义数据模型
    data class Actor(val id: String, val name: String, val type: ActorType)
    data class Message(val from: String, val to: String, val text: String, val type: MessageType)
    // ... 其他数据结构

    // 2. 内部存储
    private val actors = mutableListOf<Actor>()
    private val messages = mutableListOf<Message>()
    private var diagramTitle: String? = null
    // ...

    // 3. 写入方法（供 Parser 调用）
    fun addActor(actor: Actor) { actors.add(actor) }
    fun addMessage(message: Message) { messages.add(message) }

    // 4. 读取方法（供 Renderer 调用）
    fun getActors(): List<Actor> = actors.toList()
    fun getMessages(): List<Message> = messages.toList()

    // 5. 实现 DiagramDB 接口方法
    override fun clear() {
        actors.clear()
        messages.clear()
        diagramTitle = null
    }
    override fun setDiagramTitle(title: String) { diagramTitle = title }
    override fun getDiagramTitle(): String? = diagramTitle
    override fun getDirection(): String = "TB"  // 时序图默认从上到下
}
```

**关键原则：**
- 数据模型精确对标 mermaid-js 中对应的 DB 结构
- 参考 mermaid-js 源码：`packages/mermaid/src/diagrams/sequence/sequenceDb.ts`
- DB 负责存储数据，不负责解析逻辑

### 步骤 3：实现 DiagramParser — 解析器

`SequenceParser.kt` — 将 Mermaid 文本解析并写入 DB。

本项目使用**手写递归下降解析器**，不依赖 PEG/Yacc：

```kotlin
package io.lugf027.github.mermaid.core.diagram.sequence

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser

class SequenceParser : DiagramParser {

    override fun parse(text: String, db: DiagramDB) {
        val seqDb = db as SequenceDb
        seqDb.clear()

        val lines = text.lines()
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() || trimmed.startsWith("%%") -> continue
                trimmed.startsWith("sequenceDiagram") -> continue
                // 逐行匹配语法规则...
                ACTOR_REGEX.matches(trimmed) -> parseActor(trimmed, seqDb)
                MESSAGE_REGEX.matches(trimmed) -> parseMessage(trimmed, seqDb)
                // ...
            }
        }
    }

    companion object {
        // 定义语法正则
        private val ACTOR_REGEX = Regex("""^(?:participant|actor)\s+(.+?)(?:\s+as\s+(.+))?$""")
        private val MESSAGE_REGEX = Regex("""^(.+?)\s*(->>|-->>|->>-|-->>-|->>?\+?|-->>?\+?)\s*(.+?)\s*:\s*(.+)$""")
        // ...
    }

    private fun parseActor(line: String, db: SequenceDb) { /* ... */ }
    private fun parseMessage(line: String, db: SequenceDb) { /* ... */ }
}
```

**解析器实现要点：**
- 逐行扫描，用正则匹配语法元素
- 参考 mermaid-js 源码：`packages/mermaid/src/diagrams/sequence/sequenceParser.ts` (或对应的 .jison 文件)
- 保持与 mermaid-js 相同的语法兼容性
- 处理好边界情况：空行、注释行、多行文本等

### 步骤 4：实现 DiagramRenderer — 渲染器

`SequenceRenderer.kt` — 从 DB 读取数据生成 SVG IR。

渲染器分两种模式：

#### 模式 A：统一渲染（适用于 Dagre 布局的图表）

如果图表使用 Dagre 布局（如 class/state/er），需要：
1. 将 DB 数据转换为 `LayoutData`
2. 调用 `DagreLayout.layout()` 计算坐标
3. 使用 `ShapeRegistry`/`EdgeRenderer` 渲染节点和边

```kotlin
class ClassRenderer : DiagramRenderer {
    override fun draw(db: DiagramDB, config: MermaidConfig,
                      themeVariables: ThemeVariables, diagramId: String): SvgRoot {
        val classDb = db as ClassDb

        // 1. 构建 LayoutData
        val layoutData = classDb.getData()

        // 2. Dagre 布局
        DagreLayout.layout(layoutData, config)

        // 3. 复用 ShapeRegistry 和 EdgeRenderer 渲染
        return buildSvg {
            // ... 使用 layoutData 中的坐标信息渲染
        }
    }
}
```

#### 模式 B：自定义渲染（适用于非 Dagre 布局的图表）

如果图表有专用布局（如 sequence/pie/gantt），直接构建 SVG 元素：

```kotlin
class SequenceRenderer : DiagramRenderer {
    override fun draw(db: DiagramDB, config: MermaidConfig,
                      themeVariables: ThemeVariables, diagramId: String): SvgRoot {
        val seqDb = db as SequenceDb
        val actors = seqDb.getActors()
        val messages = seqDb.getMessages()

        // 1. 计算布局（自定义算法）
        val actorPositions = calculateActorPositions(actors, config)
        val messageYPositions = calculateMessageYPositions(messages, config)

        // 2. 构建 SVG IR
        return buildSvg {
            width = totalWidth
            height = totalHeight
            viewBox = "0 0 $totalWidth $totalHeight"

            // 渲染参与者
            for ((actor, pos) in actorPositions) {
                group {
                    rect { /* 参与者方框 */ }
                    text { /* 参与者名称 */ }
                    line { /* 生命线 */ }
                }
            }

            // 渲染消息
            for ((msg, y) in messageYPositions) {
                group {
                    line { /* 消息线 */ }
                    text { /* 消息文本 */ }
                }
            }
        }
    }
}
```

**渲染器实现要点：**
- 参考 mermaid-js 源码的对应 renderer 文件
- 使用 `SvgBuilder` DSL 或直接构造 `SvgElement` 子类
- 使用 `TextUtils.estimateTextWidth()` 估算文本宽度
- 使用 `ThemeVariables` 获取主题颜色
- 使用 `IdGenerator` 生成唯一 ID

### 步骤 5：组装 DiagramDefinition

`SequenceDiagram.kt` — 将三个组件组装为 `DiagramDefinition`：

```kotlin
package io.lugf027.github.mermaid.core.diagram.sequence

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

object SequenceDiagram {
    fun createDefinition(): DiagramDefinition {
        return DiagramDefinition(
            id = "sequence",
            detector = { text ->
                text.trimStart().startsWith("sequenceDiagram", ignoreCase = true)
            },
            dbFactory = { SequenceDb() },
            parser = SequenceParser(),
            renderer = SequenceRenderer(),
            styles = { themeVars ->
                // 可选：生成图表专用 CSS
                """
                .actor { fill: ${themeVars.actorBkg}; stroke: ${themeVars.actorBorder}; }
                .messageLine { stroke: ${themeVars.signalColor}; }
                """.trimIndent()
            }
        )
    }
}
```

### 步骤 6：注册到 DiagramOrchestration

在 `DiagramOrchestration.kt` 中注册新图表：

```kotlin
// 文件: mermaid-core/src/commonMain/kotlin/.../diagram/DiagramOrchestration.kt

fun registerBuiltinDiagrams() {
    // 已有注册
    DiagramRegistry.register(FlowchartDiagram.createFlowchartV2Definition())
    DiagramRegistry.register(FlowchartDiagram.createFlowchartLegacyDefinition())
    DiagramRegistry.register(PieDiagram.createDefinition())
    DiagramRegistry.register(ErrorDiagram.createDefinition())

    // ← 新增注册
    DiagramRegistry.register(SequenceDiagram.createDefinition())
}
```

### 步骤 7：添加图表配置（如需要）

如果新图表有专用配置项，在 `DiagramConfigs.kt` 中添加：

```kotlin
// 文件: mermaid-core/src/commonMain/kotlin/.../config/DiagramConfigs.kt

@Serializable
data class SequenceConfig(
    val actorMargin: Int = 50,
    val boxMargin: Int = 10,
    val noteMargin: Int = 10,
    val messageMargin: Int = 35,
    val mirrorActors: Boolean = true,
    // ...
)
```

并在 `MermaidConfig.kt` 中添加字段：

```kotlin
@Serializable
data class MermaidConfig(
    // ... 已有字段
    val sequence: SequenceConfig? = null,  // ← 新增
)
```

### 步骤 8：编写测试

#### 8.1 解析器单元测试

在 `mermaid-core/src/commonTest/` 下创建 `SequenceParserTest.kt`：

```kotlin
class SequenceParserTest {
    @Test
    fun testBasicSequence() {
        MermaidApi.initialize()
        val diagram = MermaidApi.parse("""
            sequenceDiagram
                Alice->>Bob: Hello Bob
                Bob-->>Alice: Hi Alice
        """.trimIndent())
        assertEquals("sequence", diagram.type)
    }

    @Test
    fun testWithParticipant() { /* ... */ }

    @Test
    fun testWithActivation() { /* ... */ }
}
```

#### 8.2 端到端渲染测试

```kotlin
@Test
fun testSequenceRendering() {
    MermaidApi.initialize()
    val svg = MermaidApi.renderToSvg("""
        sequenceDiagram
            Alice->>Bob: Hello
    """.trimIndent())
    assertTrue(svg.contains("<svg"))
    assertTrue(svg.contains("Alice"))
    assertTrue(svg.contains("Bob"))
}
```

#### 8.3 评估用例

在 `mermaid-eval/src/jvmMain/resources/files/mmd/` 下创建新的测试目录：

```
resources/files/mmd/
├── flowchart/              # 已有
│   ├── mermaid_flowchart_1.mmd
│   └── ...
└── sequence/               # ← 新增
    ├── mermaid_sequence_1.mmd
    ├── mermaid_sequence_2.mmd
    └── ...
```

然后运行评估：

```bash
./gradlew :mermaid-eval:jvmRun --args="-d /absolute/path/to/mmd/sequence -f"
```

### 步骤 9：更新示例应用

在 `example/shared/.../App.kt` 中添加新图表类型的 Tab 或预设示例。

## 开发参考

### mermaid-js 源码对照

每种图表在 mermaid-js 中的源码位置：

```
mermaid-js/packages/mermaid/src/diagrams/
├── flowchart/
│   ├── flowDb.ts          → FlowchartDb.kt
│   ├── parser/flowParser.ts → FlowchartParser.kt
│   └── flowRenderer-v2.ts → FlowchartRenderer.kt
├── pie/
│   ├── pieDb.ts           → PieDb.kt
│   ├── pieParser.ts       → PieParser.kt
│   └── pieRenderer.ts     → PieRenderer.kt
├── sequence/              ← 参考这里实现 SequenceDiagram
│   ├── sequenceDb.ts
│   ├── sequenceParser.ts
│   └── sequenceRenderer.ts
└── ...
```

### 关键工具类

| 工具类 | 说明 | 使用场景 |
|--------|------|----------|
| `TextUtils.estimateTextWidth()` | 估算文本像素宽度 | 计算节点/标签尺寸 |
| `TextUtils.estimateTextHeight()` | 估算文本行高 | 计算节点/标签尺寸 |
| `ColorUtils` | 颜色格式转换 (hex/rgb/hsl) | 主题颜色处理 |
| `IdGenerator` | 确定性 ID 生成 | SVG 元素 ID |
| `SvgPathBuilder` | SVG Path 数据构建 | 复杂路径（弧线、曲线） |
| `SvgBuilder.buildSvg {}` | SVG DSL 构建 | 生成 SVG IR 树 |
| `ThemeVariables` | 主题变量（120+ 属性） | 获取颜色/字体/边框配置 |

### SVG IR 常用元素

```kotlin
// 可用的 SvgElement 子类
SvgRoot       // <svg> 根元素
SvgGroup      // <g> 分组
SvgRect       // <rect> 矩形
SvgCircle     // <circle> 圆形
SvgEllipse    // <ellipse> 椭圆
SvgLine       // <line> 线段
SvgPath       // <path> 路径
SvgPolyline   // <polyline> 折线
SvgPolygon    // <polygon> 多边形
SvgText       // <text> 文本
SvgTspan      // <tspan> 文本片段
SvgDefs       // <defs> 定义块
SvgStyle      // <style> CSS 样式
SvgMarker     // <marker> 标记（箭头等）
SvgUse        // <use> 引用
SvgForeignObject // <foreignObject>
SvgClipPath   // <clipPath>
SvgTitle      // <title>
SvgDesc       // <desc>
SvgRawHtml    // 原始 HTML
```

### 主题系统

5 种内置主题，每种定义了 120+ 个颜色变量：

| 主题 | 类名 | 适用场景 |
|------|------|----------|
| Default | `DefaultTheme` | 默认主题（浅蓝色调） |
| Dark | `DarkTheme` | 深色背景 |
| Forest | `ForestTheme` | 绿色自然风格 |
| Neutral | `NeutralTheme` | 灰度中性风格 |
| Base | `BaseTheme` | 基础无色彩主题 |

渲染器通过 `themeVariables` 参数获取颜色值。每种图表类型可通过 `DiagramDefinition.styles` 生成专用 CSS。

## Checklist：新增图表类型

完成新图表类型后，请确认以下清单：

- [ ] `XxxDb.kt` — 数据模型，精确对标 mermaid-js
- [ ] `XxxParser.kt` — 手写递归下降解析器
- [ ] `XxxRenderer.kt` — SVG IR 渲染器
- [ ] `XxxDiagram.kt` — DiagramDefinition 组装
- [ ] `DiagramOrchestration.kt` — 注册新定义
- [ ] `DiagramConfigs.kt` — 添加专用配置（如需要）
- [ ] `MermaidConfig.kt` — 添加配置字段（如需要）
- [ ] `XxxParserTest.kt` — 解析器单元测试
- [ ] `MermaidApiTest.kt` — 端到端渲染测试
- [ ] `mermaid-eval/resources/files/mmd/xxx/` — 评估用例 (.mmd 文件)
- [ ] `README.md` — 更新支持的图表类型表格
- [ ] 运行 `./gradlew :mermaid-core:allTests` 确认所有测试通过
- [ ] 运行 `mermaid-eval` 确认评估分数 >= 0.95
