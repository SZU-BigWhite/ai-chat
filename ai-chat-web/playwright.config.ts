import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  use: {
    baseURL: 'http://127.0.0.1:15173',
    viewport: { width: 1440, height: 1000 },
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  reporter: [['list'], ['html', { open: 'never' }]],
  webServer: [
    {
      command: 'node tests/mock-provider.mjs',
      url: 'http://127.0.0.1:18090/health',
      reuseExistingServer: false,
      timeout: 15_000,
    },
    {
      command: 'powershell -NoProfile -ExecutionPolicy Bypass -File tests/start-backend.ps1',
      url: 'http://127.0.0.1:18081/api/health',
      reuseExistingServer: false,
      timeout: 180_000,
    },
    {
      command: 'npm run dev -- --port 15173',
      url: 'http://127.0.0.1:15173',
      env: { API_PROXY_TARGET: 'http://127.0.0.1:18081' },
      reuseExistingServer: false,
      timeout: 30_000,
    },
  ],
})
