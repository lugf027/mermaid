# mermaid-kmp 开发辅助工具集

在 mermaid-kmp 开发过程中，为了调试布局差异、测量字体宽度、验证 dagre 算法移植等，积累了一系列辅助脚本。本目录将这些脚本整合为通用化的工具，可在后续迭代支持更多图表类型时复用。

## 工具概览

| 工具 | 语言 | 用途 | 适用场景 |
|------|------|------|----------|
| `compare_svg.py` | Python | SVG 结构化差异对比 | 验证任意图表类型的渲染精度 |
| `measure_font_widths.js` | Node.js | 字体宽度表 + kerning 采集 | 更新/扩展 TextUtils 的字体数据 |
| `test_dagre_layout.mjs` | Node.js | Dagre 布局算法验证 | 验证 dagre 移植对 flowchart/class/state/er 等图表的布局 |
| `measure_shapes.js` | Node.js | mermaid 形状尺寸采集 | 验证新增形状的尺寸计算公式 |
| `decode_svg_data.py` | Python | SVG data-points 解码 | 调试 dagre 的 edge points 坐标 |

## 安装依赖

```bash
cd tools/
npm install
```

Python 工具无额外依赖（仅使用标准库）。

## 工具详情

### 1. compare_svg.py — SVG 对比工具

**功能**: 全面对比 mermaid-js 和 mermaid-kmp 生成的 SVG 文件差异。

**对比维度** (8 个):
- viewBox / max-width 对齐
- 节点位置 (transform 坐标)
- 边路径坐标 (path d 属性)
- 边 CSS 类
- 箭头标记 (marker-end/start)
- 结构完整性 (节点数/边数)
- 边标签文本和位置

**用法**:
```bash
# 对比两个 SVG
python3 compare_svg.py output_js.svg output_kmp.svg

# 批量对比目录
python3 compare_svg.py --dir tests/ --pattern "mermaid_flowchart_*"

# 详细模式 + JSON 输出
python3 compare_svg.py output_js.svg output_kmp.svg --verbose --json

# 自定义容差
python3 compare_svg.py a.svg b.svg --node-threshold 0.5 --edge-threshold 1.0
```

**在新图表类型开发中的用途**:
1. 实现新图表类型后，用 mmdc 生成 JS 版 SVG
2. 用 mermaid-kmp CLI 生成 KMP 版 SVG
3. 运行 compare_svg.py 检查差异
4. 根据差异报告定位和修复问题

---

### 2. measure_font_widths.js — 字体宽度采集工具

**功能**: 在真实浏览器中测量 mermaid 字体的字符宽度和 kerning 值，生成可供 `TextUtils.estimateTextWidth()` 使用的数据表。

**输出文件**:
- `char_widths.json` — 所有可打印 ASCII 字符的宽度
- `kerning_pairs.json` — 非零 kerning 字符对
- `validation.json` — 文本宽度验证结果

**用法**:
```bash
# 标准采集（mermaid 默认字体）
node measure_font_widths.js

# 指定字体和字号
node measure_font_widths.js --font "arial, sans-serif" --size 14

# 验证特定文本
node measure_font_widths.js --validate "Hello World,Process A,Decision"

# 对比 DOM 和 Canvas 测量方式
node measure_font_widths.js --compare-canvas

# 输出到指定目录
node measure_font_widths.js --output-dir ./data/fonts
```

**在新图表类型开发中的用途**:
- 如果新图表类型使用不同字体/字号，需要采集对应的字体数据
- 验证 TextUtils 的宽度估算是否足够精确

---

### 3. test_dagre_layout.mjs — Dagre 布局验证工具

**功能**: 使用 dagre-d3-es 运行 dagre 布局，输出节点位置和边控制点，用于验证 mermaid-kmp 中 dagre 移植的正确性。

**工作模式**:
- **标准模式**: 运行布局，输出最终结果
- **调试模式 (`--debug`)**: 逐步执行 dagre 内部 pipeline（acyclic → rank → normalize → order），输出每步中间状态

**内置示例**:
| 名称 | 说明 |
|------|------|
| `basic` | 5 节点 LR 流程图（含菱形判断） |
| `shapes` | 6 种形状 TB 流程图 |
| `cycle` | 含循环边的 TB 流程图 |
| `multi_branch` | 多出边判断节点 |

**用法**:
```bash
# 列出示例
node test_dagre_layout.mjs --list

# 运行示例
node test_dagre_layout.mjs --example basic

# 调试模式
node test_dagre_layout.mjs --example cycle --debug

# 自定义输入
node test_dagre_layout.mjs --input my_graph.json

# 与预期坐标对比
node test_dagre_layout.mjs --input graph.json --expected expected.json
```

**JSON 输入格式**:
```json
{
  "graph": { "rankdir": "TB", "nodesep": 50, "ranksep": 50, "marginx": 8, "marginy": 8 },
  "nodes": [
    { "id": "A", "width": 100, "height": 50, "label": "Node A" }
  ],
  "edges": [
    { "v": "A", "w": "B", "label": "edge", "width": 30, "height": 24 }
  ]
}
```

**在新图表类型开发中的用途**:
- 使用 Dagre 渲染的图表类型（flowchart, class, state, er）都需要验证布局
- 构造对应图表类型的 graph JSON 输入，验证 KMP 实现与 JS 版本的一致性

---

### 4. measure_shapes.js — 形状尺寸采集工具

**功能**: 在浏览器中渲染 mermaid 图表，采集每个节点的实际尺寸信息（getBBox、clientRect、foreignObject），确认 dagre 接收的实际节点尺寸。

**采集数据**:
- `labelContainerBBox` — dagre 使用的节点尺寸
- `shapeBBox` — 形状元素 (rect/circle/polygon/path) 的 getBBox
- `textClientRect` — 文本标签的 clientRect
- `nodeBBox` — 节点整体 getBBox

**用法**:
```bash
# 内置示例（覆盖所有形状）
node measure_shapes.js --example all-shapes

# 自定义 .mmd 文件
node measure_shapes.js --input my_diagram.mmd

# 内联 mermaid 代码
node measure_shapes.js --code "flowchart TD\n    A[Start] --> B{Decision}"

# JSON 输出
node measure_shapes.js --example all-shapes --json --output shapes.json

# 指定 mermaid 版本
node measure_shapes.js --example basic --mermaid-version 11.4.1
```

**在新图表类型开发中的用途**:
- 验证新图表类型中节点形状的尺寸计算公式
- 当出现布局偏差时，使用此工具获取 JS 版本的实际 getBBox 值进行逆向分析

---

### 5. decode_svg_data.py — SVG Data-Points 解码工具

**功能**: 解码 mermaid SVG 中 base64 编码的 `data-points` 属性，提取 dagre 布局的原始 edge points 坐标。

**用法**:
```bash
# 解码单个 SVG
python3 decode_svg_data.py output.svg

# 对比两个 SVG 的 data-points
python3 decode_svg_data.py output_js.svg output_kmp.svg

# JSON 输出
python3 decode_svg_data.py output.svg --json
```

---

## 开发新图表类型的工作流

以实现 `sequenceDiagram` 为例：

```bash
# 1. 编写 .mmd 测试用例
echo 'sequenceDiagram
    Alice->>John: Hello John
    John-->>Alice: Great!' > test_seq.mmd

# 2. 生成 JS 版 SVG（参考基准）
mmdc -i test_seq.mmd -o test_seq_js.svg

# 3. 采集 JS 版的节点尺寸（如果需要理解渲染细节）
node tools/measure_shapes.js --input test_seq.mmd --json --output seq_shapes.json

# 4. 实现 KMP 版本（修改代码）...

# 5. 生成 KMP 版 SVG
cd mermaid-kmp && ./gradlew :mermaid-cli:jvmRun --args="-i test_seq.mmd -o test_seq_kmp.svg"

# 6. 对比差异
python3 tools/compare_svg.py test_seq_js.svg test_seq_kmp.svg --verbose

# 7. 如果是 dagre 布局类型，验证布局算法
node tools/test_dagre_layout.mjs --input seq_graph.json --expected seq_expected.json

# 8. 加入 mermaid-eval 评估套件
cp test_seq.mmd mermaid-eval/src/jvmMain/resources/files/mmd/sequence/
./gradlew :mermaid-eval:jvmRun --args="-d /path/to/mmd/files"
```

## 与 mermaid-eval 的关系

- **mermaid-eval** 是集成到 Gradle 的自动化评估工具（Kotlin），用于 CI/回归测试
- **tools/** 是独立的辅助脚本（Python/Node.js），用于开发调试和数据采集
- 开发时使用 tools/ 调试定位问题，修复后用 mermaid-eval 跑回归确认

## 来源

这些工具从 `归档/` 目录下的以下脚本整合而来：

| 工具 | 来源脚本 |
|------|----------|
| `compare_svg.py` | `compare_all.py`, `compare_svgs.py`, `compare_edges.py`, `compare_3cases.py` |
| `measure_font_widths.js` | `measure_dom.js`, `measure_font.js`, `measure_kern.js`, `measure_compare.js`, `calc_widths.py` |
| `test_dagre_layout.mjs` | `test_dagre.mjs`, `test_dagre3.mjs`, `test_dagre3_debug.mjs`, `test_dagre4.mjs`, `debug_fc7_dagre.mjs` |
| `measure_shapes.js` | `measure_hex.js`, `debug_fc7_measure.mjs` |
| `decode_svg_data.py` | `decode_points.py`, `debug_dagre.js` |
