/**
 * Pixel-level diff script using pixelmatch.
 *
 * Reads screenshot pairs from the manifest, compares them pixel by pixel,
 * and generates diff images and a structured JSON result.
 *
 * Usage: npx tsx src/diff.ts [--threshold 0.1]
 */

import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';
import { PNG } from 'pngjs';
import pixelmatch from 'pixelmatch';
import type { CompareResult, CompareReport } from './types.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const OUTPUT_DIR = path.resolve(__dirname, '..', 'output');
const DIFFS_DIR = path.join(OUTPUT_DIR, 'diffs');
const MANIFEST_PATH = path.join(OUTPUT_DIR, 'screenshot-manifest.json');
const REPORT_JSON_PATH = path.join(OUTPUT_DIR, 'compare-report.json');

// Diff threshold percentage: samples with diff% >= this are considered "fail"
const FAIL_THRESHOLD = 5.0; // 5%

// Parse CLI args
function parseArgs(): { threshold: number } {
  const args = process.argv.slice(2);
  let threshold = 0.1; // pixelmatch threshold (0-1)

  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--threshold' && args[i + 1]) {
      threshold = parseFloat(args[i + 1]);
      i++;
    }
  }

  return { threshold };
}

function readPng(filePath: string): PNG {
  const buffer = fs.readFileSync(filePath);
  return PNG.sync.read(buffer);
}

/**
 * Resize a PNG image to the target dimensions by creating a new blank (white)
 * image and copying pixels from the source.
 */
function resizePng(img: PNG, width: number, height: number): PNG {
  if (img.width === width && img.height === height) return img;

  const resized = new PNG({ width, height });
  // Fill with white
  for (let i = 0; i < resized.data.length; i += 4) {
    resized.data[i] = 255;     // R
    resized.data[i + 1] = 255; // G
    resized.data[i + 2] = 255; // B
    resized.data[i + 3] = 255; // A
  }

  // Copy source pixels
  const copyW = Math.min(img.width, width);
  const copyH = Math.min(img.height, height);
  for (let y = 0; y < copyH; y++) {
    for (let x = 0; x < copyW; x++) {
      const srcIdx = (y * img.width + x) * 4;
      const dstIdx = (y * width + x) * 4;
      resized.data[dstIdx] = img.data[srcIdx];
      resized.data[dstIdx + 1] = img.data[srcIdx + 1];
      resized.data[dstIdx + 2] = img.data[srcIdx + 2];
      resized.data[dstIdx + 3] = img.data[srcIdx + 3];
    }
  }

  return resized;
}

function compareImages(
  kmpPath: string,
  jsPath: string,
  diffPath: string,
  threshold: number,
): { diffPixels: number; totalPixels: number; diffPercentage: number } {
  let kmpImg = readPng(kmpPath);
  let jsImg = readPng(jsPath);

  // Ensure same dimensions (use the larger of the two)
  const width = Math.max(kmpImg.width, jsImg.width);
  const height = Math.max(kmpImg.height, jsImg.height);

  kmpImg = resizePng(kmpImg, width, height);
  jsImg = resizePng(jsImg, width, height);

  const diff = new PNG({ width, height });
  const diffPixels = pixelmatch(
    kmpImg.data,
    jsImg.data,
    diff.data,
    width,
    height,
    { threshold, includeAA: true }
  );

  // Save diff image
  fs.writeFileSync(diffPath, PNG.sync.write(diff));

  const totalPixels = width * height;
  const diffPercentage = totalPixels > 0 ? (diffPixels / totalPixels) * 100 : 0;

  return { diffPixels, totalPixels, diffPercentage };
}

async function main() {
  const { threshold } = parseArgs();

  console.log('=== Mermaid KMP vs JS Pixel Diff ===');
  console.log(`Pixelmatch threshold: ${threshold}`);
  console.log(`Fail threshold: ${FAIL_THRESHOLD}%`);

  // Read manifest
  if (!fs.existsSync(MANIFEST_PATH)) {
    console.error(`Manifest not found: ${MANIFEST_PATH}`);
    console.error('Run screenshot.ts first.');
    process.exit(1);
  }

  const manifest = JSON.parse(fs.readFileSync(MANIFEST_PATH, 'utf-8'));
  fs.mkdirSync(DIFFS_DIR, { recursive: true });

  const results: CompareResult[] = [];

  for (let i = 0; i < manifest.length; i++) {
    const entry = manifest[i];
    const safeName = entry.sample.name.replace(/[^a-zA-Z0-9_-]/g, '_').toLowerCase();
    const diffPath = path.join(DIFFS_DIR, `${safeName}_diff.png`);

    console.log(`[${i + 1}/${manifest.length}] "${entry.sample.name}"...`);

    if (!entry.kmpScreenshot || !entry.jsScreenshot) {
      results.push({
        sample: entry.sample,
        kmpScreenshot: entry.kmpScreenshot || '',
        jsScreenshot: entry.jsScreenshot || '',
        diffImage: '',
        diffPixels: 0,
        totalPixels: 0,
        diffPercentage: 100,
        status: 'error',
        errorMessage: 'Missing screenshot(s)',
      });
      continue;
    }

    if (!fs.existsSync(entry.kmpScreenshot) || !fs.existsSync(entry.jsScreenshot)) {
      results.push({
        sample: entry.sample,
        kmpScreenshot: entry.kmpScreenshot,
        jsScreenshot: entry.jsScreenshot,
        diffImage: '',
        diffPixels: 0,
        totalPixels: 0,
        diffPercentage: 100,
        status: 'error',
        errorMessage: 'Screenshot file not found',
      });
      continue;
    }

    try {
      const { diffPixels, totalPixels, diffPercentage } = compareImages(
        entry.kmpScreenshot,
        entry.jsScreenshot,
        diffPath,
        threshold,
      );

      const status = diffPercentage >= FAIL_THRESHOLD ? 'fail' : 'pass';

      results.push({
        sample: entry.sample,
        kmpScreenshot: entry.kmpScreenshot,
        jsScreenshot: entry.jsScreenshot,
        diffImage: diffPath,
        diffPixels,
        totalPixels,
        diffPercentage: Math.round(diffPercentage * 100) / 100,
        status,
      });

      const icon = status === 'pass' ? '✓' : '✗';
      console.log(`  ${icon} diff: ${diffPercentage.toFixed(2)}% (${diffPixels}/${totalPixels} pixels)`);
    } catch (err: any) {
      console.error(`  Error: ${err.message}`);
      results.push({
        sample: entry.sample,
        kmpScreenshot: entry.kmpScreenshot,
        jsScreenshot: entry.jsScreenshot,
        diffImage: '',
        diffPixels: 0,
        totalPixels: 0,
        diffPercentage: 100,
        status: 'error',
        errorMessage: err.message,
      });
    }
  }

  // Build report
  const report: CompareReport = {
    timestamp: new Date().toISOString(),
    baseUrl: '',
    results,
    summary: {
      total: results.length,
      passed: results.filter(r => r.status === 'pass').length,
      failed: results.filter(r => r.status === 'fail').length,
      errors: results.filter(r => r.status === 'error').length,
      avgDiffPercentage: results.length > 0
        ? Math.round(
            (results.reduce((sum, r) => sum + r.diffPercentage, 0) / results.length) * 100
          ) / 100
        : 0,
    },
  };

  fs.writeFileSync(REPORT_JSON_PATH, JSON.stringify(report, null, 2));

  console.log('\n=== Summary ===');
  console.log(`Total: ${report.summary.total}`);
  console.log(`Passed: ${report.summary.passed}`);
  console.log(`Failed: ${report.summary.failed}`);
  console.log(`Errors: ${report.summary.errors}`);
  console.log(`Avg Diff: ${report.summary.avgDiffPercentage}%`);
  console.log(`\nReport saved to ${REPORT_JSON_PATH}`);
}

main().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});
