/**
 * Mermaid KMP vs JS — Comparison Page Logic
 *
 * Responsibilities:
 * 1. Load sample data from mermaid-kmp (?mode=samples endpoint)
 * 2. Sync editor text to both renderers
 * 3. Render mermaid-js diagrams via CDN API
 * 4. Update mermaid-kmp iframe via postMessage / URL
 * 5. Gallery mode: batch-render all samples side by side
 */

// ── Globals ──
let samples = [];
let mermaidJsLoaded = false;
let currentMode = 'editor'; // 'editor' | 'gallery'
let renderCounter = 0;

// ── DOM refs ──
const sampleSelector = document.getElementById('sampleSelector');
const codeEditor = document.getElementById('codeEditor');
const kmpFrame = document.getElementById('kmpFrame');
const jsOutput = document.getElementById('jsOutput');
const editorMode = document.getElementById('editorMode');
const galleryMode = document.getElementById('galleryMode');
const galleryGrid = document.getElementById('galleryGrid');
const btnEditor = document.getElementById('btnEditor');
const btnGallery = document.getElementById('btnGallery');
const statusBar = document.getElementById('statusBar');

// ── Load mermaid-js from CDN ──
async function loadMermaidJs() {
    try {
        setStatus('Loading mermaid-js from CDN...');
        const mod = await import('https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs');
        window.mermaid = mod.default;
        window.mermaid.initialize({
            startOnLoad: false,
            theme: 'default',
            securityLevel: 'loose',
            logLevel: 4, // warn
        });
        mermaidJsLoaded = true;
        setStatus('mermaid-js loaded successfully');
    } catch (err) {
        console.error('Failed to load mermaid-js:', err);
        setStatus('Error loading mermaid-js: ' + err.message);
    }
}

// ── Load sample data ──
async function loadSamples() {
    try {
        setStatus('Loading sample data...');

        // Try fetching from the ?mode=samples endpoint
        const resp = await fetch('index.html?mode=samples');
        const html = await resp.text();

        // Parse the JSON from the <pre> tag
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, 'text/html');
        const pre = doc.getElementById('samples-json');

        if (pre) {
            samples = JSON.parse(pre.textContent);
        } else {
            // Fallback: try parsing the response directly as JSON
            samples = JSON.parse(html);
        }

        populateSampleSelector();
        setStatus(`Loaded ${samples.length} samples`);
    } catch (err) {
        console.error('Failed to load samples:', err);
        setStatus('Error loading samples: ' + err.message);

        // Fallback: use a minimal built-in sample
        samples = [{
            name: 'Flowchart',
            type: 'flowchart',
            text: 'flowchart TD\n    A[Start] --> B{Decision}\n    B -->|Yes| C[OK]\n    B -->|No| D[Not OK]'
        }];
        populateSampleSelector();
    }
}

function populateSampleSelector() {
    // Clear existing options (keep the first placeholder)
    while (sampleSelector.options.length > 1) {
        sampleSelector.remove(1);
    }

    samples.forEach((sample, index) => {
        const opt = document.createElement('option');
        opt.value = index.toString();
        opt.textContent = `${sample.name} (${sample.type})`;
        sampleSelector.appendChild(opt);
    });
}

// ── Rendering ──

/**
 * Render a diagram using mermaid-js and return SVG HTML.
 */
async function renderMermaidJs(text, containerId) {
    if (!mermaidJsLoaded || !text.trim()) return '';

    try {
        renderCounter++;
        const id = containerId || `mermaid-js-render-${renderCounter}`;
        const { svg } = await window.mermaid.render(id, text.trim());
        return svg;
    } catch (err) {
        console.warn('mermaid-js render error:', err);
        return `<div style="color:red;padding:16px;font-size:12px;">
            <strong>mermaid-js Error</strong><br>${err.message || err}
        </div>`;
    }
}

/**
 * Update the mermaid-kmp iframe to render the given text.
 */
function updateKmpFrame(text, iframe) {
    const frame = iframe || kmpFrame;
    try {
        // Try postMessage first (faster, no reload)
        if (frame.contentWindow && frame.contentWindow.updateMermaidKmpText) {
            frame.contentWindow.updateMermaidKmpText(text);
            return;
        }
    } catch (e) {
        // Cross-origin or not loaded yet
    }

    // Fallback: update iframe src
    const encoded = encodeURIComponent(text);
    const newSrc = `index.html?mode=render&text=${encoded}`;
    if (frame.src !== newSrc) {
        frame.src = newSrc;
    }
}

// ── Editor sync ──

let editorDebounceTimer = null;

function onEditorChange() {
    clearTimeout(editorDebounceTimer);
    editorDebounceTimer = setTimeout(async () => {
        const text = codeEditor.value;
        setStatus('Rendering...');

        // Update both renderers
        updateKmpFrame(text);

        const svg = await renderMermaidJs(text);
        jsOutput.innerHTML = svg;

        setStatus('Rendered');
    }, 500);
}

// ── Sample selection ──

function onSampleSelected() {
    const idx = parseInt(sampleSelector.value);
    if (isNaN(idx) || idx < 0 || idx >= samples.length) return;

    const sample = samples[idx];
    codeEditor.value = sample.text;
    onEditorChange();
}

// ── Mode switching ──

function switchToEditor() {
    currentMode = 'editor';
    editorMode.classList.remove('hidden');
    galleryMode.classList.remove('active');
    btnEditor.classList.add('active');
    btnGallery.classList.remove('active');
}

function switchToGallery() {
    currentMode = 'gallery';
    editorMode.classList.add('hidden');
    galleryMode.classList.add('active');
    btnEditor.classList.remove('active');
    btnGallery.classList.add('active');
    buildGallery();
}

// ── Gallery ──

async function buildGallery() {
    galleryGrid.innerHTML = '';
    setStatus('Building gallery...');

    for (let i = 0; i < samples.length; i++) {
        const sample = samples[i];
        const item = document.createElement('div');
        item.className = 'gallery-item';
        item.setAttribute('data-sample-index', i.toString());

        // Header
        const header = document.createElement('div');
        header.className = 'gallery-item-header';
        header.textContent = `${sample.name} (${sample.type})`;
        item.appendChild(header);

        // Body with two panels
        const body = document.createElement('div');
        body.className = 'gallery-item-body';

        // KMP panel
        const kmpPanel = document.createElement('div');
        kmpPanel.className = 'gallery-panel kmp';
        const kmpLabel = document.createElement('div');
        kmpLabel.className = 'gallery-panel-label';
        kmpLabel.textContent = 'KMP';
        kmpPanel.appendChild(kmpLabel);

        const kmpIframe = document.createElement('iframe');
        const encodedText = encodeURIComponent(sample.text);
        kmpIframe.src = `index.html?mode=render&text=${encodedText}`;
        kmpIframe.setAttribute('loading', 'lazy');
        kmpPanel.appendChild(kmpIframe);
        body.appendChild(kmpPanel);

        // JS panel
        const jsPanel = document.createElement('div');
        jsPanel.className = 'gallery-panel js';
        const jsLabel = document.createElement('div');
        jsLabel.className = 'gallery-panel-label';
        jsLabel.textContent = 'JS';
        jsPanel.appendChild(jsLabel);

        const jsContainer = document.createElement('div');
        jsContainer.className = 'mermaid-js-gallery';
        jsPanel.appendChild(jsContainer);
        body.appendChild(jsPanel);

        item.appendChild(body);
        galleryGrid.appendChild(item);

        // Render mermaid-js for this sample
        const svg = await renderMermaidJs(sample.text, `gallery-js-${i}`);
        jsContainer.innerHTML = svg;

        setStatus(`Gallery: rendered ${i + 1}/${samples.length}`);
    }

    setStatus(`Gallery complete — ${samples.length} samples`);
}

// ── Status ──

function setStatus(msg) {
    statusBar.textContent = msg;
}

// ── Expose API for Playwright ──

window.__compareApi = {
    getSamples: () => samples,
    getSampleCount: () => samples.length,
    isReady: () => mermaidJsLoaded && samples.length > 0,

    /** Load a sample by index and wait for rendering */
    async loadSample(index) {
        if (index < 0 || index >= samples.length) {
            throw new Error(`Invalid sample index: ${index}`);
        }

        const sample = samples[index];
        codeEditor.value = sample.text;

        // Ensure editor mode
        if (currentMode !== 'editor') switchToEditor();

        // Update both renderers
        updateKmpFrame(sample.text);
        const svg = await renderMermaidJs(sample.text);
        jsOutput.innerHTML = svg;

        // Wait a bit for KMP to render
        await new Promise(r => setTimeout(r, 2000));

        return {
            name: sample.name,
            type: sample.type,
        };
    },

    /** Get the KMP panel element for screenshot */
    getKmpPanel: () => document.querySelector('#kmpPanel .panel-body'),
    /** Get the JS panel element for screenshot */
    getJsPanel: () => document.querySelector('#jsPanel .panel-body'),
};

// ── Init ──

async function init() {
    setStatus('Initializing...');

    // Wire up events
    codeEditor.addEventListener('input', onEditorChange);
    sampleSelector.addEventListener('change', onSampleSelected);
    btnEditor.addEventListener('click', switchToEditor);
    btnGallery.addEventListener('click', switchToGallery);

    // Load dependencies in parallel
    await Promise.all([loadMermaidJs(), loadSamples()]);

    // Load default sample
    if (samples.length > 0) {
        sampleSelector.value = '0';
        onSampleSelected();
    }

    setStatus('Ready');
}

init();
