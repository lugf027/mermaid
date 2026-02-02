---
name: mermaid-flowchart-rendering-optimization
overview: 优化 Mermaid 流程图渲染效果，解决菱形节点文本溢出、连接线长度、连接点重叠、节点对齐和箭头位置等5个问题，参考 mermaid-js/mermaid 的实现方案进行改进。
todos:
  - id: fix-diamond-size
    content: 修改 calculateNodeSizes() 方法，优化菱形节点尺寸计算公式，确保文本完全容纳在菱形边界内
    status: completed
  - id: add-rank-spacing
    content: 在 LayoutConfig 中新增 rankSpacing 参数，并修改 calculatePositions() 使用该参数控制层级间距
    status: completed
  - id: implement-port-allocator
    content: 实现 EdgePortAllocator 端口分配机制，为同一节点的多条边分配不同连接点位置，避免重叠
    status: completed
  - id: align-single-child
    content: 优化节点对齐算法，新增 alignSingleChildNodes() 方法，确保单一子节点与父节点垂直对齐
    status: completed
    dependencies:
      - add-rank-spacing
  - id: optimize-back-edge
    content: 优化 createBackEdgePath() 回边路由算法，增加偏移量和箭头终点 margin，避免箭头位置紧凑
    status: completed
    dependencies:
      - implement-port-allocator
---

## 产品概述

针对 Mermaid 流程图渲染库的布局和渲染问题进行修复优化，解决菱形节点文本溢出、连接线长度不足、连接点重叠、节点对齐异常和箭头位置紧凑等5个渲染问题。

## 核心问题

### 问题1: 菱形节点文本溢出

菱形节点 `B{Is it working?}` 的尺寸计算使用简单的 1.5 倍缩放，导致长文本超出菱形边界。

### 问题2: 连接线长度太短

`B -->|Yes| C` 和 `B -->|No| D` 的连接线长度不足，导致边标签 (Yes/No) 显示过大、拥挤。

### 问题3: 连接点重叠

`B -->|No| D` 的起点与 `D --> B` 的终点在菱形节点 B 上的连接点重合，造成视觉混乱。

### 问题4: 节点对齐问题

`C --> E[End]` 中，E 节点应该在 C 节点正下方，而非与 A、B 节点对齐。

### 问题5: 箭头位置紧凑

`D --> B` 的回边箭头终点位置太紧凑，与节点边界距离不足。

## 技术栈

- 语言: Kotlin (Multiplatform)
- UI框架: Compose Multiplatform
- 构建工具: Gradle with Kotlin DSL

## 实现方案

### 问题1解决方案: 优化菱形尺寸计算

**当前实现问题**: `FlowchartLayout.kt` 第284-286行使用 `textSize.width * 1.5f` 和 `textSize.height * 1.5f` 计算菱形尺寸，这种简单缩放无法保证文本完全容纳在菱形内部。

**解决方案**: 参考 mermaid-js 的 `question.ts` 实现，菱形的对角线长度应为 `s = w + h + 2*padding`，其中 `w` 是文本宽度，`h` 是文本高度。菱形是旋转45度的正方形，文本需要放置在菱形的内切矩形中，因此：

- 菱形宽度 = `(textWidth + textHeight) * sqrt(2) / 2 + padding * 2`
- 菱形高度 = 与宽度相同（正菱形）或根据文本比例调整

修改 `calculateNodeSizes()` 方法，将菱形尺寸计算改为：

```
NodeShape.DIAMOND -> {
    val padding = config.textPadding
    val size = textSize.width + textSize.height + padding * 2
    size to size
}
```

### 问题2解决方案: 增加层级间距 (rankSpacing)

**当前实现问题**: `LayoutConfig` 中只有 `nodeSpacingX` 和 `nodeSpacingY`，用于同一层级内节点间距。缺少 `rankSpacing` 参数控制不同层级之间的间距。

**解决方案**:

1. 在 `LayoutConfig` 中新增 `rankSpacing` 参数（默认值 80f），用于控制层级间的最小距离
2. 修改 `calculatePositions()` 方法，使用 `rankSpacing` 替代原有的 `nodeSpacingX/Y` 来计算层级位置
3. 这样可以确保有足够空间显示边标签

### 问题3解决方案: 实现端口分配机制

**当前实现问题**: `getConnectionPointByDirection()` 方法根据方向计算单一连接点，导致多条边连接到同一位置。

**解决方案**:

1. 新增 `EdgePortAllocator` 内部类，负责为节点的每条边分配不同的端口位置
2. 在 `routeEdges()` 之前，先统计每个节点各方向的边数量
3. 对于同一方向有多条边的情况，在该方向的边界上均匀分配连接点
4. 修改 `getDiamondConnectionPoint()` 和 `getRectangleConnectionPoint()`，支持端口偏移参数

### 问题4解决方案: 优化重心启发式算法

**当前实现问题**: `orderNodesInRanks()` 使用重心启发式算法，但子节点没有继承父节点的对齐偏好。对于只有单一父节点的子节点（如 E 只被 C 连接），应该优先与父节点垂直对齐。

**解决方案**:

1. 新增 `alignSingleChildNodes()` 方法，在 `orderNodesInRanks()` 后执行
2. 遍历所有节点，对于只有单一入边且父节点在上一层级的节点，调整其位置使其与父节点对齐
3. 修改 `calculatePositions()` 中的节点定位逻辑，对单一子节点进行特殊处理，优先继承父节点的横坐标位置

### 问题5解决方案: 优化回边路由算法

**当前实现问题**: `createBackEdgePath()` 中 `offset` 值为 `nodeSpacing * 0.6f`，对于箭头终点来说偏移不足。

**解决方案**:

1. 增加回边路由的偏移量，将系数从 0.6f 调整为 0.8f
2. 为箭头终点额外增加偏移量（考虑箭头大小 ARROW_SIZE = 10f）
3. 修改 `getDiamondConnectionPoint()` 和相关方法，为回边计算时增加额外的 margin

## 实现注意事项

### 性能考虑

- 端口分配机制在 `routeEdges()` 前一次性计算，避免重复遍历
- 单一子节点对齐在布局流程中顺序执行，时间复杂度 O(n)

### 向后兼容

- 新增的 `rankSpacing` 参数有合理默认值，不影响现有调用
- 所有修改都在现有方法内部进行，不改变公开 API

### 代码风格

- 遵循项目现有的 Kotlin 代码风格
- 新增方法添加 KDoc 注释
- 保持与 mermaid-js 实现的逻辑一致性

## 目录结构

```
mermaid-core/src/commonMain/kotlin/io/github/lugf027/mermaid/
├── layout/
│   ├── LayoutEngine.kt              # [MODIFY] 新增 rankSpacing 配置参数
│   └── flowchart/
│       └── FlowchartLayout.kt       # [MODIFY] 核心布局引擎修改
│                                    #   - 修改 calculateNodeSizes() 优化菱形尺寸计算
│                                    #   - 修改 calculatePositions() 使用 rankSpacing
│                                    #   - 新增 EdgePortAllocator 类实现端口分配
│                                    #   - 新增 alignSingleChildNodes() 单一子节点对齐
│                                    #   - 修改 createBackEdgePath() 优化回边偏移
│                                    #   - 修改连接点计算方法支持端口偏移
└── render/
    └── shapes/
        └── EdgeShapes.kt            # [MODIFY] 调整箭头偏移量，优化回边箭头位置
```

## 关键代码结构

### LayoutConfig 新增参数

```
public data class LayoutConfig(
    // ... 现有参数 ...
    
    /**
     * 层级之间的间距（用于不同 rank 之间的距离）
     */
    val rankSpacing: Float = 80f,
)
```

### EdgePortAllocator 端口分配器

```
/**
 * 为节点边分配不同的连接端口，避免重叠
 */
private class EdgePortAllocator {
    // nodeId -> direction -> List<edgeIndex>
    private val portAssignments: MutableMap<String, MutableMap<ConnectionDirection, MutableList<Int>>>
    
    fun allocate(edges: List<FlowEdge>, vertices: Map<String, FlowVertex>)
    fun getPortOffset(nodeId: String, direction: ConnectionDirection, edgeIndex: Int): Float
}
```