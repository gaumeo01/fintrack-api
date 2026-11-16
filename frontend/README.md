# Finance Tracker Frontend

React + TypeScript + Vite app for the Finance Tracker API.

## Run Locally

```bash
cd frontend
npm install
cp .env.example .env
npm run dev
```

Set `VITE_API_BASE_URL` to the backend origin, for example:

```text
VITE_API_BASE_URL=http://localhost:8080
```

If port 5173 is unavailable on Windows, use:

```powershell
npm.cmd run dev -- --host 127.0.0.1 --port 3001 --strictPort
```

Open `http://127.0.0.1:3001/login`. The root `.env` must include this exact
origin in `APP_CORS_ALLOWED_ORIGINS` (the current `.env.example` includes it).
An existing `.env` value overrides the fallback in `docker-compose.yml`.
After changing it, run the following from the repository root to apply it
without restarting PostgreSQL or removing its data:

```powershell
docker compose up -d --no-deps --force-recreate app
```

Build for production:

```bash
npm run build
```

## Verification and production

`npm test` runs date/session/precision regression tests. `npm run typecheck` checks TypeScript and `npm run build` produces the static application. `npm run test:e2e` exercises a disposable real backend at `http://localhost:18080` (override `E2E_API_URL`), with CORS permitting `http://localhost:5174`. Install Chromium with `npx playwright install chromium`, or set `PLAYWRIGHT_CHANNEL=chrome` to use installed Chrome. The E2E suite creates test accounts and finance records; never target production.

`Dockerfile` serves the build from unprivileged nginx with same-origin `/api/` proxying. Use the root production Compose file. Calendar dates remain strings. The API client's lossless JSON parser preserves decimal monetary values. See [contracts](../docs/contracts.md) and [operations](../docs/operations.md).
