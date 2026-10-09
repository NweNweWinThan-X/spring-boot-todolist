import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end suite. It drives the real SPA against the real API, so both must be running:
 *   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local      (:8080)
 *   npm run dev -- --port 3000 --strictPort                      (:3000)
 */
export default defineConfig({
  testDir: './e2e',
  // Each spec registers its own account, so ordering between files does not matter, but
  // steps inside a file build on each other.
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { outputFolder: 'playwright-report', open: 'never' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:3000',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
