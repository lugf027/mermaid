---
name: mermaid-flowchart-rendering-optimization
overview: 优化 Kotlin Multiplatform Mermaid 库的 flowchart 渲染，解决子图尺寸、节点位置、边连接关系、文字溢出和曲线样式等 6 个核心问题。
todos:
  - id: fix-layout-config
    content: 修改 LayoutEngine.kt 中的 LayoutConfig，增加子图标题边距配置并调整 charWidth 和 textPadding 参数
    status: completed
  - id: refactor-subgraph-layout
    content: 重构 FlowchartLayout.kt 的子图布局逻辑，在节点位置计算时考虑子图标题偏移，扩大子图边界尺寸
    status: completed
    dependencies:
      - fix-layout-config
  - id: optimize-edge-curves
    content: 优化 EdgeShapes.kt 中的曲线绘制，实现 generateRoundedPath() 圆角路径算法使箭头更圆润
    status: completed
  - id: update-renderer
    content: 更新 FlowchartRenderer.kt 中子图标题绘制位置，使用配置的 titleMargin 参数
    status: completed
    dependencies:
      - fix-layout-config
  - id: verify-edge-connections
    content: 使用 [subagent:code-explorer] 验证边连接逻辑，确保 A-->C 和 B-->C 等跨子图边正确渲染
    status: completed
    dependencies:
      - refactor-subgraph-layout
---

## 产品概述

优化 Kotlin Multiplatform mermaid 解析渲染库中 flowchart 图表的布局和渲染效果，解决子图尺寸、节点位置、边连接和曲线样式等问题。

## 核心功能

1. **子图尺寸与标题优化** - 扩大子图整体尺寸，为标题预留足够空间，避免节点与标题文字重叠
2. **节点文字溢出修复** - 增大节点尺寸估算中的字符宽度和内边距，确保文字完全容纳在节点内
3. **边连接正确性修复** - 确保边的连接关系与 mermaid 定义一致，修复缺失或错误的箭头指向
4. **曲线圆角优化** - 采用类似 mermaid-js 的圆角路径算法，使箭头连线更加圆润美观

## 技术栈

- 语言: Kotlin (Multiplatform)
- UI框架: Compose Multiplatform
- 图形绘制: androidx.compose.ui.graphics

## 实现方案

### 整体策略

参考 mermaid-js/mermaid 开源库的实现，对当前项目的布局引擎和渲染器进行优化。主要修改集中在：

1. `LayoutConfig` - 增加子图标题边距配置
2. `FlowchartLayout` - 优化子图布局算法，增加标题预留空间
3. `TextMeasurer` - 调整文字尺寸估算参数
4. `EdgeShapes` - 实现圆角路径算法

### 关键技术决策

**1. 子图标题边距设计**
参考 mermaid-js 的 `subGraphTitleMargins.ts`，在 `LayoutConfig` 中新增 `subgraphTitleTopMargin` 和 `subgraphTitleBottomMargin` 配置。在布局阶段：

- 子图内的节点位置需要向下偏移 `titleMargin` 高度
- 子图边界需要在顶部额外增加标题高度

**2. 节点尺寸计算优化**
当前 `charWidth = 8f` 偏小，导致文字溢出。参考 mermaid-js 的 `labelHelper()` 函数，将 `charWidth` 调整为 10f，同时增加 `textPadding` 从 12f 到 16f，确保文字有足够边距。

**3. 子图布局流程重构**
当前 `layoutSubgraphs()` 在节点布局之后执行，仅计算包围盒。需要调整为：

- 先识别子图内节点
- 在 `calculatePositions()` 时为子图内节点添加标题偏移
- 最后计算子图边界时加入标题高度

**4. 圆角曲线算法**
参考 mermaid-js 的 `generateRoundedPath()` 函数，实现二次贝塞尔曲线圆角：

- 在转角处计算切入点和切出点
- 使用 `Q` (quadraticBezierTo) 绘制圆角
- 圆角半径可配置，默认 5f

### 性能考虑

- 布局计算为一次性操作，复杂度 O(n) 其中 n 为节点数
- 曲线路径计算在渲染时进行，避免重复计算可缓存 Path 对象

## 实现细节

### 核心目录结构

```
mermaid-core/src/commonMain/kotlin/io/github/lugf027/mermaid/
├── layout/
│   ├── LayoutEngine.kt          # [MODIFY] 在 LayoutConfig 中添加子图标题边距配置 (subgraphTitleTopMargin, subgraphTitleBottomMargin)；调整 charWidth 从 8f 到 10f，textPadding 从 12f 到 16f
│   └── flowchart/
│       └── FlowchartLayout.kt   # [MODIFY] 重构 layoutSubgraphs() 方法，在节点位置计算前预处理子图，为子图内节点添加标题偏移；优化 calculatePositions() 支持子图内节点位置调整
├── render/
│   ├── flowchart/
│   │   └── FlowchartRenderer.kt # [MODIFY] 优化 drawSubgraphWithTextRecursive() 中标题绘制位置，使用配置的 titleMargin
│   └── shapes/
│       └── EdgeShapes.kt        # [MODIFY] 新增 generateRoundedPath() 方法实现圆角路径；修改 calculateSmoothBezierPath() 支持圆角模式
└── model/
    └── flowchart/
        └── FlowSubgraph.kt      # [MODIFY] 添加 titleHeight 属性用于存储标题高度
```

## 关键代码结构

### LayoutConfig 扩展

```
public data class LayoutConfig(
    // ... existing fields ...
    
    /**
     * Top margin for subgraph title.
     */
    val subgraphTitleTopMargin: Float = 8f,
    
    /**
     * Bottom margin for subgraph title (space between title and content).
     */
    val subgraphTitleBottomMargin: Float = 16f,
    
    /**
     * Estimated line height for subgraph title.
     */
    val subgraphTitleHeight: Float = 24f,
    
    // Adjusted values
    val charWidth: Float = 10f,  // Increased from 8f
    val textPadding: Float = 16f  // Increased from 12f
)
```

### 圆角路径算法接口

```
/**
 * Generates SVG-like path with rounded corners.
 * @param points Array of points
 * @param radius Corner radius
 * @return Path with rounded corners
 */
public fun generateRoundedPath(points: List<Point>, radius: Float = 5f): Path
```

## Agent Extensions

### SubAgent

- **code-explorer**
- 用途: 在修改过程中探索相关代码依赖和调用链，确保修改不会破坏现有功能
- 预期结果: 确认所有受影响的文件和接口，验证修改的完整性