import { defineConfig } from '@playwright/test';

export default defineConfig({
  use: {
    // Base URL pointing to the webApp dev server (jsBrowserDevelopmentRun)
    baseURL: 'http://localhost:8080',
    headless: true,
    viewport: { width: 1600, height: 900 },
    screenshot: 'off',
  },
  timeout: 60_000,
});
