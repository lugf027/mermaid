---
name: flowchart-rendering-optimization
overview: 参考 mermaid-js/mermaid 官方实现，优化 Kotlin MultiPlatform 版 mermaid 库的 flowchart 流程图布局和渲染效果，使其达到预期的层次布局效果。
todos:
  - id: refactor-rank-assignment
    content: 重构 FlowchartLayout.kt 中的 assignRanks() 方法，使用 Longest Path 算法正确处理节点层级分配和回边识别
    status: completed
  - id: optimize-node-ordering
    content: 优化 orderNodesInRanks() 方法，使用 barycenter 算法减少边交叉，实现同层节点合理排序
    status: completed
    dependencies:
      - refactor-rank-assignment
  - id: fix-position-calculation
    content: 修复 calculatePositions() 和 centerNodesInRanks() 方法，确保节点按正确方向（TB/BT/LR/RL）布局且同层居中对齐
    status: completed
    dependencies:
      - optimize-node-ordering
  - id: improve-edge-routing
    content: 改进 routeEdges() 和 getConnectionPoint() 方法，根据节点相对位置动态计算连接点，支持回边路由和多控制点
    status: completed
    dependencies:
      - fix-position-calculation
  - id: optimize-edge-rendering
    content: 优化 EdgeShapes.kt 中的边绘制逻辑，支持多控制点贝塞尔曲线渲染
    status: completed
    dependencies:
      - improve-edge-routing
---

## 用户需求

优化 Kotlin MultiPlatform mermaid 解析渲染库的 flowchart 流程图布局效果，使其能够像官方 mermaid-js 一样正确地按层次结构排列节点。

## 产品概述

这是一个 Kotlin MultiPlatform 的 mermaid 图表解析和渲染库，支持 Android、iOS、Desktop、Web 平台。当前 flowchart 流程图的布局效果不正确，节点横向排列在一起而非按依赖关系垂直分层。

## 核心问题

针对如下示例代码：

```
flowchart TD
    A[Start] --> B{Is it working?}
    B -->|Yes| C[Great!]
    B -->|No| D[Debug]
    D --> B
    C --> E[End]
```

- **预期效果**：节点按 TD（Top-Down）方向垂直分层排列，菱形决策框居中，连线平滑连接到节点边缘
- **实际效果**：所有节点横向排列在一起，没有按照依赖关系进行层次布局

## 核心改进

1. 重写层次布局算法，正确处理节点的层级分配和有环图（D --> B 回边）
2. 优化节点位置计算，实现同层节点居中对齐
3. 改进边与节点的连接点计算，根据实际连接方向确定连接点
4. 优化边的路由算法，支持平滑曲线和中间控制点

## 技术栈

- 语言：Kotlin (MultiPlatform)
- UI 框架：Compose Multiplatform
- 目标平台：Android、iOS、Desktop、Web

## 实现方案

### 1. 层次布局算法重构

#### 问题分析

当前 `FlowchartLayout.kt` 中的 `assignRanks()` 方法存在以下问题：

1. **回边处理不当**：示例中 `D --> B` 是回边（指向已访问节点），当前算法没有正确识别和处理
2. **层级计算错误**：使用 BFS 时，入度减为 0 才加入队列，导致有回边的节点可能无法正确处理
3. **位置计算逻辑混乱**：`calculatePositions()` 中的坐标分配逻辑在垂直/水平方向上有误

#### 解决方案

采用改进的 Longest Path 算法（类似 dagre 的 rank 分配）：

```
算法步骤：
1. 识别源节点（入度为0的节点），初始化 rank = 0
2. 使用拓扑排序遍历，每个节点的 rank = max(所有前驱节点的 rank) + 1
3. 识别回边（target.rank <= source.rank），暂时忽略回边参与 rank 计算
4. 处理循环：对于回边，不调整已计算的 rank
```

### 2. 节点位置计算优化

#### 当前问题

- 同层节点未按中心线对齐
- 节点间距计算不考虑实际布局方向

#### 解决方案

```
位置计算步骤：
1. 计算每层的最大宽度/高度
2. 计算总体布局尺寸，确定每层的基准位置
3. 同层节点按中心线居中对齐（使用 barycenter 方法减少边交叉）
4. 根据方向（TB/BT/LR/RL）正确映射坐标轴
```

### 3. 边路由算法优化

#### 当前问题

- 边只有起点和终点两个点，没有中间控制点
- 连接点计算过于简化，不考虑实际连接方向

#### 解决方案

```
边路由改进：
1. 根据源节点和目标节点的相对位置，动态计算连接点
2. 对于跨层边，添加中间控制点实现平滑曲线
3. 对于回边（如 D --> B），使用特殊路由避免穿过其他节点
```

### 4. 连接点计算优化

#### 当前问题

- 使用固定的 `isSource` 判断连接点位置
- 未考虑源/目标节点的实际相对位置

#### 解决方案

```
连接点计算：
1. 计算源中心到目标中心的向量
2. 根据向量方向确定从节点哪个边连出/进入
3. 对于菱形等特殊形状，计算实际与形状边界的交点
```

## 实现细节

### 性能考虑

- 当前层次布局算法复杂度 O(V+E)，优化后保持相同复杂度
- 边交叉减少算法（barycenter）单次迭代 O(V)，可配置迭代次数（默认 4 次）

### 关键配置参数（保持与现有 LayoutConfig 兼容）

- `nodeSpacingX: 50f` - 水平节点间距
- `nodeSpacingY: 60f` - 垂直节点间距（层间距）
- `diagramPadding: 30f` - 图表边距

### 回边处理策略

- 识别回边：target 的 rank <= source 的 rank
- 回边路由：使用折线或曲线绕过中间节点
- 回边不参与 rank 计算，避免循环依赖

## 目录结构

```
mermaid-core/src/commonMain/kotlin/io/github/lugf027/mermaid/
├── layout/
│   └── flowchart/
│       └── FlowchartLayout.kt     # [MODIFY] 重构层次布局算法
│                                   # - 重写 assignRanks(): 使用 Longest Path 算法
│                                   # - 重写 orderNodesInRanks(): 使用 barycenter 方法
│                                   # - 重写 calculatePositions(): 修复坐标计算逻辑
│                                   # - 重写 routeEdges(): 支持多控制点和回边路由
│                                   # - 重写 getConnectionPoint(): 根据实际方向计算连接点
├── render/
│   └── shapes/
│       └── EdgeShapes.kt          # [MODIFY] 优化边绘制
│                                   # - 修改 drawEdgeLine(): 支持贝塞尔曲线渲染
│                                   # - 优化 calculateBezierPath(): 支持多控制点曲线
└── model/
    └── flowchart/
        └── FlowEdge.kt            # [MODIFY] 可能需要扩展 points 类型支持曲线控制点
```

## 关键代码结构

### 改进的 Rank 分配算法接口

```
private fun assignRanks() {
    // 1. 初始化：找到所有源节点（入度为0）
    // 2. 拓扑排序：计算每个节点的 rank = max(predecessors.rank) + 1
    // 3. 识别回边：target.rank <= source.rank 的边
    // 4. 构建 rankNodes 映射
}
```

### 改进的位置计算算法接口

```
private fun calculatePositions() {
    val isHorizontal = data.direction.isHorizontal
    val isReversed = data.direction == Direction.BOTTOM_TO_TOP || 
                     data.direction == Direction.RIGHT_TO_LEFT
    
    // 1. 计算每层的尺寸
    // 2. 计算层的起始位置（考虑 isReversed）
    // 3. 计算同层节点的居中位置
    // 4. 应用坐标到节点
}
```

### 改进的连接点计算接口

```
private fun getConnectionPoint(
    vertex: FlowVertex,
    otherVertex: FlowVertex,
    isSource: Boolean
): Point {
    // 根据两个节点的相对位置确定连接方向
    // 返回节点边界上的连接点
}
```