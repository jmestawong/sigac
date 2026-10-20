import { defineConfig, devices } from '@playwright/test';
import path from 'node:path';

// Maven Wrapper segun el sistema operativo; el prefijo "./" es necesario porque
// el directorio actual no esta en el PATH al lanzar el comando desde Playwright.
const mavenWrapper = process.platform === 'win32' ? '.\\mvnw.cmd' : './mvnw';

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
      command: `${mavenWrapper} -q spring-boot:run -Dspring-boot.run.profiles=e2e`,
      cwd: path.resolve(__dirname, '../backend'),
      port: 8081,
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
    {
      command: 'npm run start',
      url: 'http://localhost:4200',
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
  ],
});
