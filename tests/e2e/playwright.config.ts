import { defineConfig, devices } from '@playwright/test';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const suiteRoot = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(suiteRoot, '../..');

export const apiBaseUrl = 'http://127.0.0.1:18080';
export const merchantBaseUrl = 'http://127.0.0.1:15174';
export const adminBaseUrl = 'http://127.0.0.1:15175';

export default defineConfig({
  testDir: './specs',
  outputDir: './artifacts/test-results',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 90_000,
  expect: {
    timeout: 10_000,
  },
  reporter: [
    ['list'],
    ['html', { outputFolder: 'artifacts/html-report', open: 'never' }],
    ['junit', { outputFile: 'artifacts/results/junit.xml' }],
  ],
  use: {
    ...devices['Desktop Chrome'],
    baseURL: apiBaseUrl,
    viewport: { width: 1440, height: 960 },
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'retain-on-failure',
  },
  webServer: [
    {
      command: 'mvn -B -f services/backend/pom.xml org.springframework.boot:spring-boot-maven-plugin:3.4.6:run',
      cwd: repositoryRoot,
      env: {
        ...process.env,
        SERVER_PORT: '18080',
        SPRING_PROFILES_ACTIVE: 'demo',
        AITUAN_AI_ENABLED: 'false',
        AITUAN_MAIL_ENABLED: 'false',
        AITUAN_MAIL_DEBUG_RETURN_CODE: 'false',
        AITUAN_UPLOAD_STRATEGY: 'local',
      },
      url: `${apiBaseUrl}/api/open/auth/token/check`,
      timeout: 180_000,
      reuseExistingServer: false,
      stdout: 'ignore',
      stderr: 'pipe',
    },
    {
      command: 'npm run dev --prefix apps/merchant_web -- --host 127.0.0.1 --port 15174 --strictPort',
      cwd: repositoryRoot,
      env: {
        ...process.env,
        VITE_API_BASE_URL: apiBaseUrl,
      },
      url: merchantBaseUrl,
      timeout: 60_000,
      reuseExistingServer: false,
      stdout: 'ignore',
      stderr: 'pipe',
    },
    {
      command: 'npm run dev --prefix apps/admin_web -- --host 127.0.0.1 --port 15175 --strictPort',
      cwd: repositoryRoot,
      env: {
        ...process.env,
        VITE_API_BASE_URL: apiBaseUrl,
      },
      url: adminBaseUrl,
      timeout: 60_000,
      reuseExistingServer: false,
      stdout: 'ignore',
      stderr: 'pipe',
    },
  ],
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
