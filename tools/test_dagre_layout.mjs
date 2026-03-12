/**
 * test_dagre_layout.mjs — 通用 Dagre 布局验证工具
 * 
 * 使用 dagre-d3-es 库运行 dagre 布局，输出节点位置和边的控制点坐标。
 * 用于验证 mermaid-kmp 中 Dagre 布局算法移植的正确性。
 * 
 * 两种工作模式:
 *   1. 标准模式: 输入图定义 JSON，运行布局，输出结果
 *   2. 调试模式 (--debug): 逐步执行 dagre 内部流程，输出每步中间状态
 * 
 * 用法:
 *   # 使用内置示例
 *   node test_dagre_layout.mjs --example basic
 *   node test_dagre_layout.mjs --example diamond
 *   node test_dagre_layout.mjs --example cycle
 * 
 *   # 使用 JSON 输入文件
 *   node test_dagre_layout.mjs --input graph.json
 * 
 *   # 调试模式 — 输出每个阶段的中间状态
 *   node test_dagre_layout.mjs --example basic --debug
 * 
 *   # 与预期坐标对比
 *   node test_dagre_layout.mjs --input graph.json --expected expected.json
 * 
 * 依赖: dagre-d3-es (npm install dagre-d3-es)
 * 
 * 来源: 从归档中的 test_dagre.mjs、test_dagre3.mjs、test_dagre3_debug.mjs、
 *        test_dagre4.mjs、debug_fc7_dagre.mjs 整合而来。
 */

import { layout as dagreLayout } from 'dagre-d3-es/src/dagre/index.js';
import * as graphlib from 'dagre-d3-es/src/graphlib/index.js';
import fs from 'fs';

// ============================================================
// 内置示例图
// ============================================================

const EXAMPLES = {
    // 基础 LR 流程图 (5节点, 含菱形判断)
    basic: {
        graph: { rankdir: 'LR', nodesep: 50, ranksep: 50, marginx: 8, marginy: 8 },
        nodes: [
            { id: 'A', width: 95.016, height: 54, label: 'Start' },
            { id: 'B', width: 113.391, height: 113.391, shape: 'question', label: 'Decision' },
            { id: 'C', width: 125.953, height: 54, label: 'Process A' },
            { id: 'D', width: 126.453, height: 54, label: 'Process B' },
            { id: 'E', width: 86.234, height: 54, label: 'End' },
        ],
        edges: [
            { v: 'A', w: 'B' },
            { v: 'B', w: 'C', label: 'Yes', width: 22.234, height: 24 },
            { v: 'B', w: 'D', label: 'No', width: 16.453, height: 24 },
            { v: 'C', w: 'E' },
            { v: 'D', w: 'E' },
        ],
    },
    
    // 多形状 TB 流程图 (6节点, stadium/circle/diamond/hexagon/cylinder/rect)
    shapes: {
        graph: { rankdir: 'TB', nodesep: 50, ranksep: 50, marginx: 8, marginy: 8 },
        nodes: [
            { id: 'A', width: 82.683, height: 39.0, label: 'Stadium' },
            { id: 'B', width: 56.719, height: 56.719, label: 'Circle' },
            { id: 'C', width: 116.312, height: 116.312, label: 'Diamond' },
            { id: 'D', width: 114.898, height: 39.0, label: 'Hexagon' },
            { id: 'E', width: 74.344, height: 57.647, label: 'Cylinder' },
            { id: 'F', width: 130.281, height: 54.0, label: 'Rectangle' },
        ],
        edges: [
            { v: 'A', w: 'B' },
            { v: 'B', w: 'C' },
            { v: 'C', w: 'D' },
            { v: 'C', w: 'E' },
            { v: 'D', w: 'F' },
            { v: 'E', w: 'F' },
        ],
    },
    
    // 含循环边的 TB 流程图 (6节点, F→A 回边)
    cycle: {
        graph: { rankdir: 'TB', nodesep: 50, ranksep: 50, edgesep: 20, marginx: 8, marginy: 8 },
        nodes: [
            { id: 'A', width: 95.016, height: 54, label: 'Start' },
            { id: 'B', width: 131.953, height: 131.953, shape: 'question', label: 'Is it sunny?' },
            { id: 'C', width: 164.219, height: 54, label: 'Go to the park' },
            { id: 'D', width: 134.5, height: 54, label: 'Stay home' },
            { id: 'E', width: 129.531, height: 54, label: 'Have fun!' },
            { id: 'F', width: 86.234, height: 54, label: 'End' },
        ],
        edges: [
            { v: 'A', w: 'B' },
            { v: 'B', w: 'C', label: 'Yes', width: 22.656, height: 24 },
            { v: 'B', w: 'D', label: 'No', width: 18.797, height: 24 },
            { v: 'C', w: 'E' },
            { v: 'D', w: 'E' },
            { v: 'E', w: 'F' },
            { v: 'F', w: 'A' },  // 循环边
        ],
    },
    
    // 多出边判断节点 (Christmas shopping)
    multi_branch: {
        graph: { rankdir: 'TB', nodesep: 50, ranksep: 50, marginx: 8, marginy: 8 },
        nodes: [
            { id: 'A', width: 130, height: 54, label: 'Christmas' },
            { id: 'B', width: 140, height: 54, label: 'Go shopping' },
            { id: 'C', width: 150, height: 150, shape: 'question', label: 'Let me think' },
            { id: 'D', width: 110, height: 54, label: 'Laptop' },
            { id: 'E', width: 100, height: 54, label: 'iPhone' },
            { id: 'F', width: 80, height: 54, label: 'Car' },
        ],
        edges: [
            { v: 'A', w: 'B', label: 'Get money', width: 66, height: 24 },
            { v: 'B', w: 'C' },
            { v: 'C', w: 'D', label: 'One', width: 27, height: 24 },
            { v: 'C', w: 'E', label: 'Two', width: 27, height: 24 },
            { v: 'C', w: 'F', label: 'Three', width: 36, height: 24 },
            { v: 'E', w: 'A' },  // 循环边
        ],
    },
};

// ============================================================
// 图构建
// ============================================================

function buildGraph(spec) {
    const graph = new graphlib.Graph({ multigraph: true, compound: true })
        .setGraph(spec.graph)
        .setDefaultEdgeLabel(() => ({}));
    
    for (const node of spec.nodes) {
        graph.setNode(node.id, {
            id: node.id,
            width: node.width,
            height: node.height,
            shape: node.shape || 'squareRect',
            label: node.label || node.id,
        });
    }
    
    for (const edge of spec.edges) {
        const edgeData = {
            id: `L_${edge.v}_${edge.w}_0`,
            width: edge.width || 0,
            height: edge.height || 0,
        };
        if (edge.label) {
            edgeData.label = edge.label;
            edgeData.labelpos = edge.labelpos || 'c';
            edgeData.labeloffset = edge.labeloffset || 10;
        }
        edgeData.weight = edge.weight || 1;
        edgeData.minlen = edge.minlen || 1;
        graph.setEdge(edge.v, edge.w, edgeData, edgeData.id);
    }
    
    return graph;
}

// ============================================================
// 标准模式: 运行布局并输出结果
// ============================================================

function runLayout(spec) {
    const graph = buildGraph(spec);
    dagreLayout(graph);
    
    console.log('=== Dagre Layout Results ===');
    console.log(`Graph: rankdir=${spec.graph.rankdir}, ranksep=${spec.graph.ranksep}, nodesep=${spec.graph.nodesep}`);
    
    console.log('\n--- Node Positions ---');
    const nodeResults = {};
    for (const nodeId of spec.nodes.map(n => n.id)) {
        const n = graph.node(nodeId);
        console.log(`  ${nodeId} (${n.label || ''}): x=${n.x.toFixed(3)}, y=${n.y.toFixed(3)}, w=${n.width}, h=${n.height}`);
        nodeResults[nodeId] = { x: n.x, y: n.y, width: n.width, height: n.height };
    }
    
    console.log('\n--- Edge Points ---');
    const edgeResults = {};
    graph.edges().forEach(e => {
        const edge = graph.edge(e);
        const key = `${e.v}->${e.w}`;
        console.log(`  ${key} (${e.name || ''}):`);
        if (edge.x !== undefined) {
            console.log(`    label: x=${edge.x.toFixed(3)}, y=${edge.y.toFixed(3)}`);
        }
        console.log(`    points: ${JSON.stringify(edge.points.map(p => ({ x: +p.x.toFixed(3), y: +p.y.toFixed(3) })))}`);
        edgeResults[key] = {
            points: edge.points,
            x: edge.x,
            y: edge.y,
        };
    });
    
    return { nodes: nodeResults, edges: edgeResults };
}

// ============================================================
// 调试模式: 逐步执行 dagre 内部流程
// ============================================================

async function runDebugLayout(spec) {
    console.log('=== Dagre Debug Mode ===');
    console.log('逐步执行 dagre 布局的各个阶段\n');
    
    const graph = buildGraph(spec);
    
    // 步骤 1: makeSpaceForEdgeLabels
    console.log('--- Step 1: makeSpaceForEdgeLabels ---');
    const graphOpts = graph.graph();
    const origRanksep = graphOpts.ranksep;
    graphOpts.ranksep = graphOpts.ranksep / 2;
    console.log(`  ranksep: ${origRanksep} → ${graphOpts.ranksep}`);
    
    graph.edges().forEach(e => {
        const edge = graph.edge(e);
        edge.minlen = (edge.minlen || 1) * 2;
        if (edge.labelpos && edge.labelpos.toLowerCase() !== 'c') {
            const rd = graphOpts.rankdir;
            if (rd === 'TB' || rd === 'BT') {
                edge.width += edge.labeloffset || 10;
            } else {
                edge.height += edge.labeloffset || 10;
            }
        }
        console.log(`  Edge ${e.v}->${e.w}: minlen=${edge.minlen}, w=${edge.width}, h=${edge.height}`);
    });
    
    // 步骤 2: acyclic
    console.log('\n--- Step 2: Acyclic (检测反转边) ---');
    try {
        const { run: acyclicRun } = await import('dagre-d3-es/src/dagre/acyclic.js');
        acyclicRun(graph);
        graph.edges().forEach(e => {
            const edge = graph.edge(e);
            if (edge.reversed) {
                console.log(`  ⚠️ Edge ${e.v}->${e.w}: REVERSED`);
            }
        });
        console.log('  ✅ 完成');
    } catch (err) {
        console.log(`  ⚠️ 无法导入 acyclic 模块: ${err.message}`);
    }
    
    // 步骤 3: rank
    console.log('\n--- Step 3: Rank (分配层级) ---');
    try {
        const { rank: rankFn } = await import('dagre-d3-es/src/dagre/rank/index.js');
        rankFn(graph);
        graph.nodes().forEach(v => {
            const node = graph.node(v);
            console.log(`  ${v}: rank=${node.rank}`);
        });
    } catch (err) {
        console.log(`  ⚠️ 无法导入 rank 模块: ${err.message}`);
    }
    
    // 步骤 4: injectEdgeLabelProxies
    console.log('\n--- Step 4: injectEdgeLabelProxies ---');
    let proxyCount = 0;
    graph.edges().forEach(e => {
        const edge = graph.edge(e);
        if (edge.width && edge.height) {
            const v = graph.node(e.v), w = graph.node(e.w);
            if (v && w) {
                const rank = Math.floor((w.rank - v.rank) / 2 + v.rank);
                const proxyId = `_ep${proxyCount++}`;
                graph.setNode(proxyId, { width: 0, height: 0, dummy: 'edge-proxy', rank: rank, e: { v: e.v, w: e.w } });
                console.log(`  Proxy ${proxyId} for ${e.v}->${e.w}: rank=${rank}`);
            }
        }
    });
    
    // 步骤 5: normalizeRanks
    console.log('\n--- Step 5: normalizeRanks ---');
    try {
        const { normalizeRanks } = await import('dagre-d3-es/src/dagre/util.js');
        normalizeRanks(graph);
        graph.nodes().forEach(v => {
            const node = graph.node(v);
            console.log(`  ${v}: rank=${node.rank} ${node.dummy ? `(dummy=${node.dummy})` : ''}`);
        });
    } catch (err) {
        console.log(`  ⚠️ 无法导入 util 模块: ${err.message}`);
    }
    
    // 步骤 6: removeEdgeLabelProxies
    console.log('\n--- Step 6: removeEdgeLabelProxies ---');
    graph.nodes().forEach(v => {
        const node = graph.node(v);
        if (node.dummy === 'edge-proxy') {
            const edge = graph.edge(node.e);
            if (edge) edge.labelRank = node.rank;
            graph.removeNode(v);
            console.log(`  Removed proxy ${v}, labelRank=${node.rank}`);
        }
    });
    
    // 步骤 7: normalize (长边拆分)
    console.log('\n--- Step 7: normalize.run (长边拆分为 dummy 节点) ---');
    try {
        const { run: normalizeRun } = await import('dagre-d3-es/src/dagre/normalize.js');
        normalizeRun(graph);
        graph.nodes().forEach(v => {
            const node = graph.node(v);
            console.log(`  ${v}: rank=${node.rank} ${node.dummy ? `(dummy=${node.dummy})` : ''}`);
        });
    } catch (err) {
        console.log(`  ⚠️ 无法导入 normalize 模块: ${err.message}`);
    }
    
    // 步骤 8: order (层内排序)
    console.log('\n--- Step 8: order (层内排序) ---');
    try {
        const { order } = await import('dagre-d3-es/src/dagre/order/index.js');
        order(graph);
        graph.nodes().forEach(v => {
            const node = graph.node(v);
            console.log(`  ${v}: rank=${node.rank}, order=${node.order} ${node.dummy ? `(dummy=${node.dummy})` : ''}`);
        });
    } catch (err) {
        console.log(`  ⚠️ 无法导入 order 模块: ${err.message}`);
    }
    
    // 输出 layer matrix
    console.log('\n--- Layer Matrix ---');
    try {
        const { buildLayerMatrix, maxRank } = await import('dagre-d3-es/src/dagre/util.js');
        const layers = buildLayerMatrix(graph);
        layers.forEach((layer, i) => {
            console.log(`  rank ${i}: [${layer.join(', ')}]`);
        });
    } catch (err) {
        console.log(`  ⚠️ 无法构建 layer matrix: ${err.message}`);
    }
    
    console.log('\n调试模式完成。要查看完整布局结果，请不带 --debug 标志重新运行。');
}

// ============================================================
// 结果对比
// ============================================================

function compareResults(actual, expected) {
    console.log('\n=== 结果对比 ===');
    let maxDiff = 0;
    
    for (const [nodeId, exp] of Object.entries(expected.nodes || {})) {
        const act = actual.nodes[nodeId];
        if (!act) {
            console.log(`  ❌ 节点 ${nodeId}: 缺失`);
            continue;
        }
        const dx = Math.abs(act.x - exp.x);
        const dy = Math.abs(act.y - exp.y);
        const d = Math.max(dx, dy);
        maxDiff = Math.max(maxDiff, d);
        const ok = d < 0.5 ? '✅' : (d < 2 ? '⚠️' : '❌');
        console.log(`  ${ok} ${nodeId}: actual=(${act.x.toFixed(3)}, ${act.y.toFixed(3)}) expected=(${exp.x.toFixed(3)}, ${exp.y.toFixed(3)}) diff=(${dx.toFixed(3)}, ${dy.toFixed(3)})`);
    }
    
    console.log(`\n  最大位置差异: ${maxDiff.toFixed(3)}px`);
    return maxDiff;
}

// ============================================================
// CLI 入口
// ============================================================

function main() {
    const args = process.argv.slice(2);
    let spec = null;
    let debug = false;
    let expectedFile = null;
    
    for (let i = 0; i < args.length; i++) {
        switch (args[i]) {
            case '--example':
            case '-e':
                const exName = args[++i];
                spec = EXAMPLES[exName];
                if (!spec) {
                    console.error(`未知示例: ${exName}。可选: ${Object.keys(EXAMPLES).join(', ')}`);
                    process.exit(1);
                }
                break;
            case '--input':
            case '-i':
                spec = JSON.parse(fs.readFileSync(args[++i], 'utf8'));
                break;
            case '--debug':
            case '-d':
                debug = true;
                break;
            case '--expected':
                expectedFile = args[++i];
                break;
            case '--list':
                console.log('可用示例:');
                for (const [name, ex] of Object.entries(EXAMPLES)) {
                    console.log(`  ${name}: ${ex.nodes.length} 节点, ${ex.edges.length} 条边, ${ex.graph.rankdir} 方向`);
                }
                process.exit(0);
            case '--help':
            case '-h':
                console.log(`
用法: node test_dagre_layout.mjs [选项]

选项:
  -e, --example <name>    使用内置示例 (basic, shapes, cycle, multi_branch)
  -i, --input <file>      从 JSON 文件加载图定义
  -d, --debug             调试模式 (逐步执行 dagre 内部流程)
  --expected <file>        与预期坐标对比
  --list                  列出所有可用示例
  -h, --help              显示帮助

JSON 输入格式:
  {
    "graph": { "rankdir": "TB", "nodesep": 50, "ranksep": 50, "marginx": 8, "marginy": 8 },
    "nodes": [{ "id": "A", "width": 100, "height": 50, "label": "Node A" }, ...],
    "edges": [{ "v": "A", "w": "B", "label": "edge", "width": 30, "height": 24 }, ...]
  }
                `);
                process.exit(0);
        }
    }
    
    if (!spec) {
        spec = EXAMPLES.basic;
        console.log('未指定输入，使用默认 basic 示例\n');
    }
    
    if (debug) {
        runDebugLayout(spec);
    } else {
        const result = runLayout(spec);
        
        if (expectedFile) {
            const expected = JSON.parse(fs.readFileSync(expectedFile, 'utf8'));
            compareResults(result, expected);
        }
    }
}

main();
