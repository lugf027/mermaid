/**
 * measure_shapes.js — 通用 mermaid 形状尺寸采集工具
 * 
 * 使用 Puppeteer 在浏览器中渲染 mermaid 图表，采集每个节点的实际尺寸信息：
 *   - 形状元素 (rect/circle/polygon/path) 的 getBBox
 *   - label-container 的 getBBox（dagre 使用此值作为节点尺寸输入）
 *   - 文本标签的 clientRect 宽高
 *   - 节点的 transform 位置
 * 
 * 采集的数据用于：
 *   1. 验证 mermaid-kmp 各形状的尺寸计算公式
 *   2. 确认 dagre 接收的实际节点尺寸
 *   3. 调试布局差异的根因
 * 
 * 用法:
 *   # 渲染指定 .mmd 文件并采集尺寸
 *   node measure_shapes.js --input flowchart.mmd
 * 
 *   # 渲染内联 mermaid 代码
 *   node measure_shapes.js --code "flowchart TD\n    A[Start] --> B{Decision}"
 * 
 *   # 使用内置示例（覆盖所有形状类型）
 *   node measure_shapes.js --example all-shapes
 * 
 *   # 输出为 JSON
 *   node measure_shapes.js --input flowchart.mmd --json --output shapes.json
 * 
 *   # 指定 mermaid 版本
 *   node measure_shapes.js --example all-shapes --mermaid-version 11.4.1
 * 
 * 依赖: puppeteer (npm install puppeteer)
 * 
 * 来源: 从归档中的 measure_hex.js、debug_fc7_measure.mjs 整合而来。
 */

const fs = require('fs');
const path = require('path');

// ============================================================
// 内置示例
// ============================================================

const EXAMPLES = {
    // 覆盖所有 flowchart 形状
    'all-shapes': `flowchart TD
    A([Stadium]) --> B((Circle))
    B --> C{Diamond}
    C --> D{{Hexagon}}
    C --> E[(Cylinder)]
    D --> F[Rectangle]
    E --> F
    F --> G>Asymmetric]
    G --> H[/Parallelogram/]
    H --> I[\\Parallelogram Alt\\]`,
    
    // 基本流程图
    'basic': `flowchart LR
    A[Start] --> B{Is it?}
    B -->|Yes| C[OK]
    C --> D[Rethink]
    D --> B
    B ---->|No| E[End]`,
    
    // 子图
    'subgraph': `flowchart TB
    subgraph one
        a1[Node 1] --> a2[Node 2]
    end
    subgraph two
        b1[Node 3] --> b2[Node 4]
    end
    one --> two`,
    
    // 序列图（非 dagre 渲染）
    'sequence': `sequenceDiagram
    Alice->>John: Hello John
    John-->>Alice: Great!
    Alice-)John: See you later!`,
    
    // 饼图
    'pie': `pie title Pets
    "Dogs" : 386
    "Cats" : 85
    "Rats" : 15`,
};

// ============================================================
// 参数解析
// ============================================================

function parseArgs() {
    const args = process.argv.slice(2);
    const config = {
        input: null,
        code: null,
        example: null,
        mermaidVersion: '11.12.0',
        json: false,
        output: null,
        timeout: 15000,
    };
    
    for (let i = 0; i < args.length; i++) {
        switch (args[i]) {
            case '--input':
            case '-i':
                config.input = args[++i];
                break;
            case '--code':
            case '-c':
                config.code = args[++i].replace(/\\n/g, '\n');
                break;
            case '--example':
            case '-e':
                config.example = args[++i];
                break;
            case '--mermaid-version':
                config.mermaidVersion = args[++i];
                break;
            case '--json':
            case '-j':
                config.json = true;
                break;
            case '--output':
            case '-o':
                config.output = args[++i];
                break;
            case '--timeout':
                config.timeout = parseInt(args[++i]);
                break;
            case '--list':
                console.log('可用示例:');
                for (const name of Object.keys(EXAMPLES)) {
                    const firstLine = EXAMPLES[name].split('\n')[0].trim();
                    console.log(`  ${name}: ${firstLine}`);
                }
                process.exit(0);
            case '--help':
            case '-h':
                console.log(`
用法: node measure_shapes.js [选项]

选项:
  -i, --input <file>           渲染指定 .mmd 文件
  -c, --code <code>            渲染内联 mermaid 代码 (用 \\n 分行)
  -e, --example <name>         使用内置示例 (all-shapes, basic, subgraph, sequence, pie)
  --mermaid-version <ver>      mermaid CDN 版本 (默认: 11.12.0)
  -j, --json                   以 JSON 格式输出
  -o, --output <file>          将结果写入文件
  --timeout <ms>               渲染超时 (默认: 15000)
  --list                       列出所有可用示例
  -h, --help                   显示帮助
                `);
                process.exit(0);
        }
    }
    
    return config;
}

// ============================================================
// 主逻辑
// ============================================================

async function main() {
    const config = parseArgs();
    
    // 确定 mermaid 代码来源
    let mermaidCode = '';
    if (config.input) {
        mermaidCode = fs.readFileSync(config.input, 'utf8');
    } else if (config.code) {
        mermaidCode = config.code;
    } else if (config.example) {
        mermaidCode = EXAMPLES[config.example];
        if (!mermaidCode) {
            console.error(`未知示例: ${config.example}。可用: ${Object.keys(EXAMPLES).join(', ')}`);
            process.exit(1);
        }
    } else {
        mermaidCode = EXAMPLES['all-shapes'];
        console.log('未指定输入，使用 all-shapes 示例\n');
    }
    
    // 加载 puppeteer
    let puppeteer;
    try {
        puppeteer = require('puppeteer');
    } catch {
        try {
            puppeteer = require('/opt/homebrew/lib/node_modules/@mermaid-js/mermaid-cli/node_modules/puppeteer');
        } catch {
            console.error('错误: 找不到 puppeteer。请运行 npm install puppeteer');
            process.exit(1);
        }
    }
    
    console.log(`🎨 渲染 mermaid 图表 (v${config.mermaidVersion})...`);
    
    const browser = await puppeteer.launch({ headless: 'new' });
    const page = await browser.newPage();
    
    // 渲染 mermaid 图表
    await page.setContent(`
        <!DOCTYPE html>
        <html>
        <head>
            <script src="https://cdn.jsdelivr.net/npm/mermaid@${config.mermaidVersion}/dist/mermaid.min.js"></script>
        </head>
        <body>
            <div id="diagram">
                <pre class="mermaid">${mermaidCode}</pre>
            </div>
            <script>
                mermaid.initialize({ startOnLoad: true });
            </script>
        </body>
        </html>
    `, { waitUntil: 'networkidle0' });
    
    // 等待 SVG 渲染完成
    try {
        await page.waitForSelector('svg', { timeout: config.timeout });
        await new Promise(r => setTimeout(r, 2000)); // 额外等待渲染稳定
    } catch {
        console.error('❌ SVG 渲染超时');
        await browser.close();
        process.exit(1);
    }
    
    // 采集节点尺寸
    const results = await page.evaluate(() => {
        const svg = document.querySelector('svg');
        if (!svg) return { error: 'No SVG found' };
        
        const data = {
            svgInfo: {
                viewBox: svg.getAttribute('viewBox'),
                width: svg.getAttribute('width'),
                height: svg.getAttribute('height'),
                style: svg.getAttribute('style'),
            },
            nodes: [],
            edges: [],
        };
        
        // 采集节点信息
        const nodes = svg.querySelectorAll('g.node');
        for (const node of nodes) {
            const nodeInfo = {
                id: node.id,
                transform: node.getAttribute('transform'),
            };
            
            // 文本标签
            const labelP = node.querySelector('.nodeLabel p') || node.querySelector('.nodeLabel');
            if (labelP) {
                nodeInfo.labelText = labelP.textContent;
            }
            
            // label-container 的 getBBox (dagre 使用此值)
            const labelContainer = node.querySelector('.label-container, .basic.label-container');
            if (labelContainer) {
                const bbox = labelContainer.getBBox();
                nodeInfo.labelContainerBBox = {
                    x: bbox.x, y: bbox.y,
                    width: bbox.width, height: bbox.height,
                };
                nodeInfo.labelContainerTag = labelContainer.tagName;
            }
            
            // 形状元素 getBBox
            const shapeEl = node.querySelector('rect, circle, polygon, path:not([class*="text"])');
            if (shapeEl) {
                const bbox = shapeEl.getBBox();
                nodeInfo.shapeBBox = {
                    x: bbox.x, y: bbox.y,
                    width: bbox.width, height: bbox.height,
                };
                nodeInfo.shapeTag = shapeEl.tagName;
            }
            
            // foreignObject 宽高（文本容器）
            const fo = node.querySelector('foreignObject');
            if (fo) {
                nodeInfo.foreignObject = {
                    width: parseFloat(fo.getAttribute('width')),
                    height: parseFloat(fo.getAttribute('height')),
                };
                const div = fo.querySelector('div');
                if (div) {
                    const rect = div.getBoundingClientRect();
                    nodeInfo.textClientRect = { width: rect.width, height: rect.height };
                }
            }
            
            // 节点整体 getBBox
            const nodeBbox = node.getBBox();
            nodeInfo.nodeBBox = {
                x: nodeBbox.x, y: nodeBbox.y,
                width: nodeBbox.width, height: nodeBbox.height,
            };
            
            data.nodes.push(nodeInfo);
        }
        
        // 采集边信息
        const edgeGroups = svg.querySelectorAll('g.edgePath, g.edge');
        for (const eg of edgeGroups) {
            const edgeInfo = { id: eg.id };
            const pathEl = eg.querySelector('path');
            if (pathEl) {
                edgeInfo.d = pathEl.getAttribute('d');
                edgeInfo.className = pathEl.getAttribute('class');
                edgeInfo.markerEnd = pathEl.getAttribute('marker-end');
            }
            data.edges.push(edgeInfo);
        }
        
        // 采集边标签
        const edgeLabels = svg.querySelectorAll('g.edgeLabel');
        data.edgeLabels = [];
        for (const el of edgeLabels) {
            const info = {
                transform: el.getAttribute('transform'),
            };
            const p = el.querySelector('p');
            if (p) info.text = p.textContent;
            const fo = el.querySelector('foreignObject');
            if (fo) {
                info.foWidth = parseFloat(fo.getAttribute('width'));
                info.foHeight = parseFloat(fo.getAttribute('height'));
            }
            data.edgeLabels.push(info);
        }
        
        return data;
    });
    
    // 输出结果
    if (config.json) {
        const jsonStr = JSON.stringify(results, null, 2);
        if (config.output) {
            fs.writeFileSync(config.output, jsonStr);
            console.log(`📄 结果已写入 ${config.output}`);
        } else {
            console.log(jsonStr);
        }
    } else {
        // 友好格式输出
        console.log('\n=== SVG 信息 ===');
        console.log(`  viewBox: ${results.svgInfo.viewBox}`);
        console.log(`  style: ${results.svgInfo.style}`);
        
        console.log(`\n=== 节点 (${results.nodes.length} 个) ===`);
        for (const node of results.nodes) {
            console.log(`\n  📦 ${node.id} (${node.labelText || '?'})`);
            console.log(`     transform: ${node.transform}`);
            if (node.labelContainerBBox) {
                const b = node.labelContainerBBox;
                console.log(`     labelContainer (${node.labelContainerTag}): getBBox=(${b.width.toFixed(3)} × ${b.height.toFixed(3)}) at (${b.x.toFixed(3)}, ${b.y.toFixed(3)})`);
            }
            if (node.shapeBBox) {
                const b = node.shapeBBox;
                console.log(`     shape (${node.shapeTag}): getBBox=(${b.width.toFixed(3)} × ${b.height.toFixed(3)}) at (${b.x.toFixed(3)}, ${b.y.toFixed(3)})`);
            }
            if (node.textClientRect) {
                console.log(`     textRect: ${node.textClientRect.width.toFixed(3)} × ${node.textClientRect.height.toFixed(3)}`);
            }
            if (node.foreignObject) {
                console.log(`     foreignObject: ${node.foreignObject.width} × ${node.foreignObject.height}`);
            }
            const nb = node.nodeBBox;
            console.log(`     nodeBBox: ${nb.width.toFixed(3)} × ${nb.height.toFixed(3)} at (${nb.x.toFixed(3)}, ${nb.y.toFixed(3)})`);
        }
        
        if (results.edges.length > 0) {
            console.log(`\n=== 边 (${results.edges.length} 条) ===`);
            for (const edge of results.edges) {
                console.log(`  ${edge.id}: class="${edge.className}" marker="${edge.markerEnd || 'none'}"`);
            }
        }
        
        if (results.edgeLabels && results.edgeLabels.length > 0) {
            console.log(`\n=== 边标签 (${results.edgeLabels.length} 个) ===`);
            for (const el of results.edgeLabels) {
                console.log(`  "${el.text || '?'}": transform=${el.transform}, fo=${el.foWidth}×${el.foHeight}`);
            }
        }
    }
    
    await browser.close();
    console.log('\n✨ 完成！');
}

main().catch(err => {
    console.error(err);
    process.exit(1);
});
