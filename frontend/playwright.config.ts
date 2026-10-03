import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./e2e",
  workers: 1,
  use: { baseURL: "http://localhost:5174", timezoneId: "Asia/Bangkok",
    channel: process.env.PLAYWRIGHT_CHANNEL || undefined, trace: "retain-on-failure" },
  webServer: { command: "npm run dev -- --host 127.0.0.1 --port 5174 --strictPort",
    url: "http://localhost:5174", reuseExistingServer: false,
    env: { VITE_API_BASE_URL: process.env.E2E_API_URL || "http://localhost:18080" } },
});
