/**
 * measure_font_widths.js — 通用字体宽度表采集工具
 * 
 * 使用 Puppeteer 在真实浏览器中测量 mermaid 使用的字体的字符宽度和 kerning 值。
 * 采集的数据用于在 mermaid-kmp 中实现离线文本宽度估算 (TextUtils.estimateTextWidth)。
 * 
 * 功能:
 *   1. 测量所有可打印 ASCII 字符的 DOM foreignObject 宽度（与 mermaid 渲染环境一致）
 *   2. 测量所有字母对的 kerning 值（字距调整）
 *   3. 对比 DOM 和 Canvas 两种测量方式的差异
 *   4. 验证给定文本的累加宽度与浏览器实测宽度
 * 
 * 输出:
 *   - char_widths.json: 字符宽度表
 *   - kerning_pairs.json: 非零 kerning 对
 *   - validation.json: 文本宽度验证结果
 * 
 * 用法:
 *   node measure_font_widths.js [--font "trebuchet ms, verdana, arial, sans-serif"] [--size 16]
 *   node measure_font_widths.js --validate "Process A,Decision,Start,End"
 *   node measure_font_widths.js --output-dir ./data
 * 
 * 依赖: puppeteer (npm install puppeteer)
 * 
 * 来源: 从归档中的 measure_dom.js、measure_font.js、measure_kern.js、
 *        measure_compare.js、calc_widths.py 整合而来。
 */

const fs = require('fs');
const path = require('path');

// ============================================================
// 配置
// ============================================================

const DEFAULT_FONT = '"trebuchet ms", verdana, arial, sans-serif';
const DEFAULT_SIZE = 16;
const PRINTABLE_ASCII = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
const SPECIAL_CHARS = ' !"#$%&\'()*+,-./:;<=>?@[\\]^_`{|}~';

// mermaid 默认的 foreignObject 样式
const MERMAID_LABEL_STYLE = {
    fontFamily: '"trebuchet ms", verdana, arial, sans-serif',
    fontSize: '16px',
    display: 'table-cell',
    whiteSpace: 'nowrap',
    lineHeight: '1.5',
    maxWidth: '200px',
    textAlign: 'center',
};

// ============================================================
// 参数解析
// ============================================================

function parseArgs() {
    const args = process.argv.slice(2);
    const config = {
        font: DEFAULT_FONT,
        size: DEFAULT_SIZE,
        validate: [],
        outputDir: '.',
        kerning: true,
        compareCanvas: false,
    };
    
    for (let i = 0; i < args.length; i++) {
        switch (args[i]) {
            case '--font':
                config.font = args[++i];
                break;
            case '--size':
                config.size = parseInt(args[++i]);
                break;
            case '--validate':
                config.validate = args[++i].split(',').map(s => s.trim());
                break;
            case '--output-dir':
                config.outputDir = args[++i];
                break;
            case '--no-kerning':
                config.kerning = false;
                break;
            case '--compare-canvas':
                config.compareCanvas = true;
                break;
            case '--help':
            case '-h':
                console.log(`
用法: node measure_font_widths.js [选项]

选项:
  --font <font>          字体名称 (默认: ${DEFAULT_FONT})
  --size <px>            字号 (默认: ${DEFAULT_SIZE})
  --validate <texts>     要验证的文本列表 (逗号分隔)
  --output-dir <dir>     输出目录 (默认: 当前目录)
  --no-kerning           跳过 kerning 测量 (加速)
  --compare-canvas       同时使用 Canvas 测量并对比
  -h, --help             显示帮助
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
    
    // 尝试多种方式加载 puppeteer
    let puppeteer;
    try {
        puppeteer = require('puppeteer');
    } catch {
        try {
            // mmdc 安装路径
            puppeteer = require('/opt/homebrew/lib/node_modules/@mermaid-js/mermaid-cli/node_modules/puppeteer');
        } catch {
            console.error('错误: 找不到 puppeteer。请运行 npm install puppeteer');
            process.exit(1);
        }
    }
    
    console.log(`🔧 字体: ${config.font}, 字号: ${config.size}px`);
    console.log(`📂 输出目录: ${config.outputDir}`);
    
    const browser = await puppeteer.launch({ headless: true });
    const page = await browser.newPage();
    
    // ---- 步骤1: 测量单字符宽度 ----
    console.log('\n📏 步骤1: 测量字符宽度...');
    
    const charWidths = await page.evaluate(({ font, size, chars, specials, style }) => {
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute('width', '800');
        svg.setAttribute('height', '200');
        document.body.appendChild(svg);
        
        function measureDom(text) {
            const g = document.createElementNS('http://www.w3.org/2000/svg', 'g');
            const fo = document.createElementNS('http://www.w3.org/2000/svg', 'foreignObject');
            fo.setAttribute('width', '1000');
            fo.setAttribute('height', '100');
            const div = document.createElement('div');
            Object.assign(div.style, style);
            const span = document.createElement('span');
            span.className = 'nodeLabel';
            const p = document.createElement('p');
            p.style.margin = '0';
            p.textContent = text;
            span.appendChild(p);
            div.appendChild(span);
            fo.appendChild(div);
            g.appendChild(fo);
            svg.appendChild(g);
            const bbox = span.getBoundingClientRect();
            svg.removeChild(g);
            return bbox.width;
        }
        
        const result = {};
        // 字母和数字
        for (const ch of chars) {
            result[ch] = measureDom(ch);
        }
        // 特殊字符（使用 charCode 作为 key 以避免 JSON 键冲突）
        for (const ch of specials) {
            result[`_${ch.charCodeAt(0)}`] = measureDom(ch);
        }
        
        document.body.removeChild(svg);
        return result;
    }, {
        font: config.font,
        size: config.size,
        chars: PRINTABLE_ASCII,
        specials: SPECIAL_CHARS,
        style: MERMAID_LABEL_STYLE,
    });
    
    console.log(`  ✅ 测量了 ${Object.keys(charWidths).length} 个字符`);
    
    // ---- 步骤2: 测量 kerning ----
    let kerningPairs = {};
    if (config.kerning) {
        console.log('\n📐 步骤2: 测量 kerning 对 (52×52=2704 对)...');
        
        kerningPairs = await page.evaluate(({ chars, style }) => {
            const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
            svg.setAttribute('width', '800');
            svg.setAttribute('height', '200');
            document.body.appendChild(svg);
            
            function measureDom(text) {
                const g = document.createElementNS('http://www.w3.org/2000/svg', 'g');
                const fo = document.createElementNS('http://www.w3.org/2000/svg', 'foreignObject');
                fo.setAttribute('width', '1000');
                fo.setAttribute('height', '100');
                const div = document.createElement('div');
                Object.assign(div.style, style);
                const span = document.createElement('span');
                span.className = 'nodeLabel';
                const p = document.createElement('p');
                p.style.margin = '0';
                p.textContent = text;
                span.appendChild(p);
                div.appendChild(span);
                fo.appendChild(div);
                g.appendChild(fo);
                svg.appendChild(g);
                const bbox = span.getBoundingClientRect();
                svg.removeChild(g);
                return bbox.width;
            }
            
            const kern = {};
            for (let i = 0; i < chars.length; i++) {
                for (let j = 0; j < chars.length; j++) {
                    const c1 = chars[i], c2 = chars[j];
                    const pair = c1 + c2;
                    const pairW = measureDom(pair);
                    const c1W = measureDom(c1);
                    const c2W = measureDom(c2);
                    const k = pairW - c1W - c2W;
                    if (Math.abs(k) > 0.001) {
                        kern[pair] = Math.round(k * 1000000) / 1000000; // 精度到 6 位小数
                    }
                }
            }
            
            document.body.removeChild(svg);
            return kern;
        }, {
            chars: 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz',
            style: MERMAID_LABEL_STYLE,
        });
        
        console.log(`  ✅ 发现 ${Object.keys(kerningPairs).length} 个非零 kerning 对`);
    }
    
    // ---- 步骤3: 可选 Canvas 对比 ----
    let canvasComparison = {};
    if (config.compareCanvas) {
        console.log('\n🎨 步骤3: Canvas 对比测量...');
        
        canvasComparison = await page.evaluate(({ font, size, chars, style }) => {
            const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
            svg.setAttribute('width', '800');
            svg.setAttribute('height', '200');
            document.body.appendChild(svg);
            
            function measureDom(text) {
                const g = document.createElementNS('http://www.w3.org/2000/svg', 'g');
                const fo = document.createElementNS('http://www.w3.org/2000/svg', 'foreignObject');
                fo.setAttribute('width', '1000');
                fo.setAttribute('height', '100');
                const div = document.createElement('div');
                Object.assign(div.style, style);
                const span = document.createElement('span');
                span.className = 'nodeLabel';
                const p = document.createElement('p');
                p.style.margin = '0';
                p.textContent = text;
                span.appendChild(p);
                div.appendChild(span);
                fo.appendChild(div);
                g.appendChild(fo);
                svg.appendChild(g);
                const bbox = span.getBoundingClientRect();
                svg.removeChild(g);
                return bbox.width;
            }
            
            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d');
            ctx.font = `${size}px ${font}`;
            
            const result = {};
            for (const ch of chars) {
                const dom = measureDom(ch);
                const cv = ctx.measureText(ch).width;
                result[ch] = { dom, canvas: cv, diff: Math.round((dom - cv) * 1000000) / 1000000 };
            }
            
            document.body.removeChild(svg);
            return result;
        }, {
            font: config.font,
            size: config.size,
            chars: PRINTABLE_ASCII,
            style: MERMAID_LABEL_STYLE,
        });
        
        const diffs = Object.values(canvasComparison).filter(v => Math.abs(v.diff) > 0.001);
        console.log(`  ✅ DOM vs Canvas 差异 > 0.001 的字符: ${diffs.length} 个`);
    }
    
    // ---- 步骤4: 文本宽度验证 ----
    let validation = {};
    const textsToValidate = config.validate.length > 0
        ? config.validate
        : ['Start', 'End', 'Decision', 'Process A', 'Process B', 'Yes', 'No'];
    
    console.log('\n🧪 步骤4: 验证文本宽度...');
    
    validation = await page.evaluate(({ texts, style }) => {
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute('width', '800');
        svg.setAttribute('height', '200');
        document.body.appendChild(svg);
        
        function measureDom(text) {
            const g = document.createElementNS('http://www.w3.org/2000/svg', 'g');
            const fo = document.createElementNS('http://www.w3.org/2000/svg', 'foreignObject');
            fo.setAttribute('width', '1000');
            fo.setAttribute('height', '100');
            const div = document.createElement('div');
            Object.assign(div.style, style);
            const span = document.createElement('span');
            span.className = 'nodeLabel';
            const p = document.createElement('p');
            p.style.margin = '0';
            p.textContent = text;
            span.appendChild(p);
            div.appendChild(span);
            fo.appendChild(div);
            g.appendChild(fo);
            svg.appendChild(g);
            const bbox = span.getBoundingClientRect();
            svg.removeChild(g);
            return bbox.width;
        }
        
        const result = {};
        for (const text of texts) {
            result[text] = measureDom(text);
        }
        
        document.body.removeChild(svg);
        return result;
    }, {
        texts: textsToValidate,
        style: MERMAID_LABEL_STYLE,
    });
    
    // 使用 charWidths + kerningPairs 计算并对比
    console.log('\n  文本宽度验证结果:');
    for (const [text, browserWidth] of Object.entries(validation)) {
        let calculated = 0;
        for (let i = 0; i < text.length; i++) {
            const ch = text[i];
            const charKey = ch.match(/[A-Za-z0-9]/) ? ch : `_${ch.charCodeAt(0)}`;
            calculated += charWidths[charKey] || 0;
            
            // 加上 kerning
            if (i > 0) {
                const pair = text[i-1] + ch;
                if (kerningPairs[pair]) {
                    calculated += kerningPairs[pair];
                }
            }
        }
        const diff = browserWidth - calculated;
        const ok = Math.abs(diff) < 0.1 ? '✅' : (Math.abs(diff) < 1.0 ? '⚠️' : '❌');
        console.log(`  ${ok} "${text}": browser=${browserWidth.toFixed(6)}, calc=${calculated.toFixed(6)}, diff=${diff.toFixed(6)}`);
    }
    
    // ---- 写入文件 ----
    if (!fs.existsSync(config.outputDir)) {
        fs.mkdirSync(config.outputDir, { recursive: true });
    }
    
    const charWidthsFile = path.join(config.outputDir, 'char_widths.json');
    fs.writeFileSync(charWidthsFile, JSON.stringify(charWidths, null, 2));
    console.log(`\n📄 字符宽度表 → ${charWidthsFile}`);
    
    if (config.kerning) {
        const kerningFile = path.join(config.outputDir, 'kerning_pairs.json');
        fs.writeFileSync(kerningFile, JSON.stringify(kerningPairs, null, 2));
        console.log(`📄 Kerning 对 → ${kerningFile}`);
    }
    
    const validationFile = path.join(config.outputDir, 'validation.json');
    fs.writeFileSync(validationFile, JSON.stringify(validation, null, 2));
    console.log(`📄 验证结果 → ${validationFile}`);
    
    if (config.compareCanvas) {
        const compareFile = path.join(config.outputDir, 'dom_vs_canvas.json');
        fs.writeFileSync(compareFile, JSON.stringify(canvasComparison, null, 2));
        console.log(`📄 DOM vs Canvas → ${compareFile}`);
    }
    
    await browser.close();
    console.log('\n✨ 完成！');
}

main().catch(err => {
    console.error(err);
    process.exit(1);
});
