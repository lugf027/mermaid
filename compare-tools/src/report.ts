/**
 * HTML report generator for screenshot comparison results.
 *
 * Reads compare-report.json and generates a visual HTML report with:
 * - Summary statistics
 * - Per-sample comparison view (KMP screenshot, JS screenshot, diff image)
 * - Sorted by diff percentage (worst first)
 *
 * Usage: npx tsx src/report.ts
 */

import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';
import type { CompareReport, CompareResult } from './types.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const OUTPUT_DIR = path.resolve(__dirname, '..', 'output');
const REPORT_JSON_PATH = path.join(OUTPUT_DIR, 'compare-report.json');
const REPORT_HTML_PATH = path.join(OUTPUT_DIR, 'report.html');

function imgToBase64(filePath: string): string {
  if (!filePath || !fs.existsSync(filePath)) return '';
  const buffer = fs.readFileSync(filePath);
  return `data:image/png;base64,${buffer.toString('base64')}`;
}

function statusBadge(status: string): string {
  const colors: Record<string, string> = {
    pass: '#4caf50',
    fail: '#f44336',
    error: '#ff9800',
  };
  const color = colors[status] || '#999';
  return `<span style="display:inline-block;padding:2px 8px;border-radius:3px;background:${color};color:#fff;font-size:11px;font-weight:600;text-transform:uppercase;">${status}</span>`;
}

function generateHtml(report: CompareReport): string {
  // Sort by diff percentage descending (worst first)
  const sorted = [...report.results].sort((a, b) => b.diffPercentage - a.diffPercentage);

  const rows = sorted.map((r, i) => {
    const kmpImg = imgToBase64(r.kmpScreenshot);
    const jsImg = imgToBase64(r.jsScreenshot);
    const diffImg = imgToBase64(r.diffImage);

    return `
    <div class="sample-card" data-status="${r.status}">
      <div class="card-header">
        <span class="card-index">#${i + 1}</span>
        <span class="card-name">${escapeHtml(r.sample.name)}</span>
        <span class="card-type">${escapeHtml(r.sample.type)}</span>
        ${statusBadge(r.status)}
        <span class="card-diff ${r.status === 'pass' ? 'pass' : 'fail'}">${r.diffPercentage.toFixed(2)}%</span>
        <span class="card-pixels">${r.diffPixels.toLocaleString()} / ${r.totalPixels.toLocaleString()} px</span>
        ${r.errorMessage ? `<span class="card-error">${escapeHtml(r.errorMessage)}</span>` : ''}
      </div>
      <div class="card-images">
        <div class="img-col">
          <div class="img-label kmp">Mermaid-KMP</div>
          ${kmpImg ? `<img src="${kmpImg}" alt="KMP rendering"/>` : '<div class="no-img">No image</div>'}
        </div>
        <div class="img-col">
          <div class="img-label js">Mermaid-JS</div>
          ${jsImg ? `<img src="${jsImg}" alt="JS rendering"/>` : '<div class="no-img">No image</div>'}
        </div>
        <div class="img-col">
          <div class="img-label diff">Diff</div>
          ${diffImg ? `<img src="${diffImg}" alt="Pixel diff"/>` : '<div class="no-img">No image</div>'}
        </div>
      </div>
    </div>`;
  }).join('\n');

  return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Mermaid KMP vs JS — Comparison Report</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    background: #f5f5f5;
    color: #333;
    padding: 24px;
  }

  h1 { font-size: 24px; margin-bottom: 8px; }
  .meta { color: #888; font-size: 13px; margin-bottom: 20px; }

  /* Summary */
  .summary {
    display: flex;
    gap: 16px;
    margin-bottom: 24px;
    flex-wrap: wrap;
  }
  .stat-card {
    background: #fff;
    border: 1px solid #ddd;
    border-radius: 8px;
    padding: 16px 24px;
    text-align: center;
    min-width: 120px;
  }
  .stat-card .stat-value {
    font-size: 32px;
    font-weight: 700;
    line-height: 1.2;
  }
  .stat-card .stat-label {
    font-size: 12px;
    color: #888;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
  .stat-card.pass .stat-value { color: #4caf50; }
  .stat-card.fail .stat-value { color: #f44336; }
  .stat-card.error .stat-value { color: #ff9800; }
  .stat-card.avg .stat-value { color: #1565c0; }

  /* Filter buttons */
  .filters {
    margin-bottom: 16px;
    display: flex;
    gap: 8px;
  }
  .filters button {
    padding: 4px 12px;
    border: 1px solid #ddd;
    border-radius: 4px;
    background: #fff;
    cursor: pointer;
    font-size: 13px;
  }
  .filters button.active { background: #1a1a2e; color: #fff; border-color: #1a1a2e; }

  /* Sample cards */
  .sample-card {
    background: #fff;
    border: 1px solid #ddd;
    border-radius: 8px;
    margin-bottom: 16px;
    overflow: hidden;
  }
  .sample-card[data-status="pass"] { border-left: 4px solid #4caf50; }
  .sample-card[data-status="fail"] { border-left: 4px solid #f44336; }
  .sample-card[data-status="error"] { border-left: 4px solid #ff9800; }

  .card-header {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 16px;
    background: #fafafa;
    border-bottom: 1px solid #eee;
    flex-wrap: wrap;
  }
  .card-index { font-weight: 700; color: #888; font-size: 13px; }
  .card-name { font-weight: 600; font-size: 15px; }
  .card-type { color: #888; font-size: 12px; background: #f0f0f0; padding: 1px 6px; border-radius: 3px; }
  .card-diff { font-weight: 700; font-size: 14px; }
  .card-diff.pass { color: #4caf50; }
  .card-diff.fail { color: #f44336; }
  .card-pixels { color: #888; font-size: 11px; }
  .card-error { color: #ff9800; font-size: 12px; font-style: italic; }

  .card-images {
    display: flex;
    gap: 0;
  }
  .img-col {
    flex: 1;
    text-align: center;
    border-right: 1px solid #eee;
    overflow: hidden;
  }
  .img-col:last-child { border-right: none; }
  .img-label {
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    padding: 4px 0;
  }
  .img-label.kmp { background: #e8f5e9; color: #2e7d32; }
  .img-label.js { background: #e3f2fd; color: #1565c0; }
  .img-label.diff { background: #fce4ec; color: #c62828; }

  .img-col img {
    max-width: 100%;
    max-height: 400px;
    object-fit: contain;
    display: block;
    margin: 8px auto;
  }
  .no-img {
    padding: 40px;
    color: #ccc;
    font-style: italic;
  }
</style>
</head>
<body>
<h1>Mermaid KMP vs JS — Comparison Report</h1>
<div class="meta">Generated: ${report.timestamp}</div>

<div class="summary">
  <div class="stat-card">
    <div class="stat-value">${report.summary.total}</div>
    <div class="stat-label">Total</div>
  </div>
  <div class="stat-card pass">
    <div class="stat-value">${report.summary.passed}</div>
    <div class="stat-label">Passed</div>
  </div>
  <div class="stat-card fail">
    <div class="stat-value">${report.summary.failed}</div>
    <div class="stat-label">Failed</div>
  </div>
  <div class="stat-card error">
    <div class="stat-value">${report.summary.errors}</div>
    <div class="stat-label">Errors</div>
  </div>
  <div class="stat-card avg">
    <div class="stat-value">${report.summary.avgDiffPercentage}%</div>
    <div class="stat-label">Avg Diff</div>
  </div>
</div>

<div class="filters">
  <button class="active" onclick="filterCards('all')">All</button>
  <button onclick="filterCards('fail')">Failed</button>
  <button onclick="filterCards('pass')">Passed</button>
  <button onclick="filterCards('error')">Errors</button>
</div>

${rows}

<script>
function filterCards(status) {
  document.querySelectorAll('.filters button').forEach(b => b.classList.remove('active'));
  event.target.classList.add('active');
  document.querySelectorAll('.sample-card').forEach(card => {
    if (status === 'all' || card.dataset.status === status) {
      card.style.display = '';
    } else {
      card.style.display = 'none';
    }
  });
}
</script>
</body>
</html>`;
}

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

async function main() {
  console.log('=== Generating HTML Report ===');

  if (!fs.existsSync(REPORT_JSON_PATH)) {
    console.error(`Report JSON not found: ${REPORT_JSON_PATH}`);
    console.error('Run diff.ts first.');
    process.exit(1);
  }

  const report: CompareReport = JSON.parse(fs.readFileSync(REPORT_JSON_PATH, 'utf-8'));
  const html = generateHtml(report);
  fs.writeFileSync(REPORT_HTML_PATH, html);

  console.log(`Report generated: ${REPORT_HTML_PATH}`);
  console.log(`Summary: ${report.summary.passed} pass, ${report.summary.failed} fail, ${report.summary.errors} errors (avg diff: ${report.summary.avgDiffPercentage}%)`);
}

main().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});
