import { defineConfig, devices } from '@playwright/test';

const apiOrigin = process.env.E2E_API_ORIGIN ?? 'http://127.0.0.1:8080';
const webOrigin = process.env.E2E_WEB_ORIGIN ?? 'http://127.0.0.1:8090';
const browserExecutable =
  process.env.PLAYWRIGHT_BROWSER_PATH ??
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe';

export default defineConfig({
  testDir: './specs',
  fullyParallel: false,
  workers: 1,
  timeout: 180_000,
  expect: {
    timeout: 20_000,
  },
  retries: process.env.CI ? 1 : 0,
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['json', { outputFile: 'test-results/e2e-results.json' }],
  ],
  outputDir: 'test-results/artifacts',
  use: {
    baseURL: webOrigin,
    viewport: { width: 390, height: 844 },
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: {
        launchOptions: browserExecutable
          ? {
              executablePath: browserExecutable,
            }
          : undefined,
      },
    },
  ],
  globalSetup: './support/global-setup.ts',
});

export { apiOrigin, webOrigin };
