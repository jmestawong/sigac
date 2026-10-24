import { defineConfig, devices } from '@playwright/test';
import path from 'node:path';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: 'list',
  use: {
    baseURL: 'http://localhost:4200',
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: [
    {
      command: 'node scripts/levantar-servicios.js e2e --ready-port=8790',
      cwd: path.resolve(__dirname, '../backend'),
      url: 'http://localhost:8790',
      reuseExistingServer: !process.env.CI,
      timeout: 360_000,
    },
    {
      command: 'npm run start',
      url: 'http://localhost:4200',
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
  ],
});
