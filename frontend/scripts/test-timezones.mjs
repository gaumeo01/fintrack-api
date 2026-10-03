import { spawnSync } from "node:child_process";
for (const TZ of ["UTC", "Asia/Bangkok", "America/Los_Angeles"]) {
  const result = spawnSync(process.execPath, ["node_modules/vitest/vitest.mjs", "run", "src/lib/dates.test.ts"], {
    stdio: "inherit", env: { ...process.env, TZ },
  });
  if (result.status !== 0) process.exit(result.status || 1);
}
