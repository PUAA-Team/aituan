import { defineConfig, devices } from '@playwright/test';

export const apiOrigin = process.env.E2E_API_ORIGIN ?? 'http://127.0.0.1:8080';
export const webOrigin = process.env.E2E_WEB_ORIGIN ?? 'http://127.0.0.1:8090';
export const apiBaseUrl = apiOrigin;
export const merchantBaseUrl = process.env.E2E_MERCHANT_ORIGIN ?? `${webOrigin}/merchant/`;
export const adminBaseUrl = process.env.E2E_ADMIN_ORIGIN ?? `${webOrigin}/admin/`;

const browserExecutable = process.env.PLAYWRIGHT_BROWSER_PATH;

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
    ['junit', { outputFile: 'test-results/junit.xml' }],
  ],
  outputDir: 'test-results/artifacts',
  use: {
    ...devices['Desktop Chrome'],
    baseURL: webOrigin,
    viewport: { width: 390, height: 844 },
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        viewport: { width: 390, height: 844 },
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
