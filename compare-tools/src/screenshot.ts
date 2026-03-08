/**
 * Automated screenshot script using Playwright.
 *
 * Opens the compare.html page, iterates through all sample diagrams,
 * and captures screenshots of both mermaid-kmp and mermaid-js rendering panels.
 *
 * Usage: npx tsx src/screenshot.ts [--base-url http://localhost:8080]
 */

import { chromium, type Browser, type Page } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';
import type { DiagramSample } from './types.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const OUTPUT_DIR = path.resolve(__dirname, '..', 'output');
const SCREENSHOTS_DIR = path.join(OUTPUT_DIR, 'screenshots');

// Parse CLI args
function parseArgs(): { baseUrl: string } {
  const args = process.argv.slice(2);
  let baseUrl = 'http://localhost:8080';

  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--base-url' && args[i + 1]) {
      baseUrl = args[i + 1];
      i++;
    }
  }

  return { baseUrl };
}

async function ensureDirs() {
  fs.mkdirSync(SCREENSHOTS_DIR, { recursive: true });
}

async function waitForComparePageReady(page: Page, timeoutMs = 30_000) {
  console.log('  Waiting for compare page to be ready...');
  const start = Date.now();

  await page.waitForFunction(
    () => (window as any).__compareApi?.isReady() === true,
    {},
    { timeout: timeoutMs }
  );

  console.log(`  Compare page ready in ${Date.now() - start}ms`);
}

async function loadSampleAndCapture(
  page: Page,
  sampleIndex: number,
  sampleCount: number,
): Promise<{ kmpPath: string; jsPath: string; sample: DiagramSample }> {
  console.log(`  [${sampleIndex + 1}/${sampleCount}] Loading sample...`);

  // Load the sample via the exposed API
  const sampleInfo = await page.evaluate(async (idx: number) => {
    return await (window as any).__compareApi.loadSample(idx);
  }, sampleIndex);

  console.log(`  [${sampleIndex + 1}/${sampleCount}] "${sampleInfo.name}" — waiting for render...`);

  // Wait for KMP iframe to finish rendering (check __mermaidKmpReady)
  try {
    const kmpFrame = page.frameLocator('#kmpFrame');
    await kmpFrame.locator('canvas').waitFor({ state: 'visible', timeout: 15_000 });
    // Additional wait for rendering to stabilize
    await page.waitForTimeout(1500);
  } catch {
    console.warn(`  [${sampleIndex + 1}] KMP canvas not found, proceeding anyway...`);
    await page.waitForTimeout(3000);
  }

  // Screenshot KMP panel
  const safeName = sampleInfo.name.replace(/[^a-zA-Z0-9_-]/g, '_').toLowerCase();
  const kmpPath = path.join(SCREENSHOTS_DIR, `${safeName}_kmp.png`);
  const jsPath = path.join(SCREENSHOTS_DIR, `${safeName}_js.png`);

  const kmpPanel = page.locator('#kmpPanel .panel-body');
  await kmpPanel.screenshot({ path: kmpPath });

  // Screenshot JS panel
  const jsPanel = page.locator('#jsPanel .panel-body');
  await jsPanel.screenshot({ path: jsPath });

  // Get the full sample data for the report
  const sample = await page.evaluate((idx: number) => {
    return (window as any).__compareApi.getSamples()[idx];
  }, sampleIndex);

  console.log(`  [${sampleIndex + 1}/${sampleCount}] "${sampleInfo.name}" — screenshots saved`);

  return { kmpPath, jsPath, sample };
}

async function main() {
  const { baseUrl } = parseArgs();

  console.log('=== Mermaid KMP vs JS Screenshot Capture ===');
  console.log(`Base URL: ${baseUrl}`);

  await ensureDirs();

  let browser: Browser | null = null;

  try {
    browser = await chromium.launch({ headless: true });
    const context = await browser.newContext({
      viewport: { width: 1600, height: 900 },
    });
    const page = await context.newPage();

    // Navigate to compare page
    const compareUrl = `${baseUrl}/compare.html`;
    console.log(`Opening ${compareUrl}...`);
    await page.goto(compareUrl, { waitUntil: 'networkidle', timeout: 60_000 });

    // Wait for page to be ready
    await waitForComparePageReady(page);

    // Get sample count
    const sampleCount = await page.evaluate(() => {
      return (window as any).__compareApi.getSampleCount();
    });
    console.log(`Found ${sampleCount} samples to capture`);

    // Capture each sample
    const results: Array<{ kmpPath: string; jsPath: string; sample: DiagramSample }> = [];

    for (let i = 0; i < sampleCount; i++) {
      try {
        const result = await loadSampleAndCapture(page, i, sampleCount);
        results.push(result);
      } catch (err: any) {
        console.error(`  [${i + 1}/${sampleCount}] Error: ${err.message}`);
        results.push({
          kmpPath: '',
          jsPath: '',
          sample: { name: `Sample ${i}`, type: 'unknown', text: '' },
        });
      }
    }

    // Save results manifest
    const manifest = results.map(r => ({
      sample: { name: r.sample.name, type: r.sample.type, text: r.sample.text },
      kmpScreenshot: r.kmpPath,
      jsScreenshot: r.jsPath,
    }));

    const manifestPath = path.join(OUTPUT_DIR, 'screenshot-manifest.json');
    fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2));
    console.log(`\nManifest saved to ${manifestPath}`);
    console.log(`Screenshots saved to ${SCREENSHOTS_DIR}`);
    console.log(`Total: ${results.length} samples captured`);

  } finally {
    if (browser) await browser.close();
  }
}

main().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});
