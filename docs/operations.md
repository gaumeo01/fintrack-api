# Deployment, migrations and recovery

## Production deployment

Use the standalone `compose.prod.yml`; `docker-compose.yml` is for local development and contains development-only defaults.
Provide `POSTGRES_USER`, `POSTGRES_PASSWORD`, `APP_JWT_SECRET`, and `APP_CORS_ALLOWED_ORIGINS` through the host environment or an untracked deployment environment file. Use a cryptographically random JWT secret of at least 32 UTF-8 bytes. Compose rejects missing values. Never reuse the example development values.

```sh
docker compose -f compose.prod.yml up -d --build
```

The frontend binds to loopback port 8081. Put an HTTPS reverse proxy in front of it, forwarding to `127.0.0.1:8081`. Nginx serves SPA routes and proxies `/api/` to the backend. PostgreSQL and the backend have no public host ports in this configuration. Both app runtime images use non-root users. The backend image contains a JRE and curl, not Maven/JDK tooling. `JAVA_TOOL_OPTIONS` may be supplied for JVM tuning; default maximum heap is 75% of container memory.

The `prod` profile requires the three `SPRING_DATASOURCE_*` values, JWT secret and allowed browser origins. `DB_POOL_SIZE` defaults to 10. Hibernate validates the schema; Flyway owns changes. SQL logging and public OpenAPI/Swagger are disabled in production. Only actuator health is exposed. `/actuator/health/liveness` checks the process; `/actuator/health/readiness` includes database availability. `/api/health` remains the original authenticated API endpoint and is not a readiness probe.

Unexpected errors log exception type and HTTP method without payloads, tokens, passwords or SQL parameters. Collect stdout/stderr centrally. Alert on repeated failures and unhealthy readiness. Do not enable request body or SQL parameter logging in production.

## Existing database adoption (read before upgrading)

Stop old application writers and take a verified backup first. Never run a new and old version together during schema adoption. No existing database is automatically baselined.

1. Restore a backup into an isolated PostgreSQL database and rehearse the upgrade there.
2. Check that `users`, `categories`, `accounts`, `transactions`, `budgets`, and `recurring_transactions` match the entities at starting revision `22ddd90`, except that budgets may have the older `budget_month` date instead of `start_date`/`end_date`. Compare with `V1__baseline.sql`. Unrecognized historical schemas require their own reviewed migration; do not assume V1 describes them.
3. Inspect invalid money, orphaned ownership/category relationships and invalid date ranges. Repair domain inconsistencies deliberately before deployment. New check constraints reject nonpositive amounts or reversed ranges instead of silently changing them.
4. For this verified existing schema only, start once with `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` and `SPRING_FLYWAY_BASELINE_VERSION=1`. This records V1 as the baseline and applies V2/V3. Remove both overrides immediately afterward. Empty databases run V1/V2/V3 automatically with no overrides.
5. Confirm Flyway history contains successful migrations, Hibernate validation passes, readiness is healthy, and balances/budget dates match the pre-upgrade snapshot.

V2 adds budget ranges if absent. A legacy `budget_month` is copied to the first and last day of that month (including leap years); existing non-null ranges are preserved. The old column and values remain for reference, become nullable, and are no longer written by the app. Its existing unique constraint can remain because new rows have null legacy months. Rolling back to the old application is not supported after new range budgets are created; restore the backup and matching application version instead.

V2 also adds recurring occurrence identity, a uniqueness constraint, and indexes. Existing generated transactions cannot be reliably linked to templates and remain unlinked. For existing advanced schedules, `last_generated_date` is conservatively seeded to `next_run_date - 1 day`, preventing rewinds before the old cursor. This is a migration guard, not a reconstructed occurrence history.

V3 adds per-user CSV import row identity. Previously imported transactions have no import identity: importing an old file for the first time after upgrade can duplicate pre-upgrade rows. Reconcile old imports before retrying them.

## Backup and restore

Use PostgreSQL's tools outside the app. The following commands assume the production Compose service and an operator-controlled host shell. Keep backups encrypted with restricted access and off-host retention; schedule them according to the acceptable recovery point. Database credentials stay inside the container environment.

```sh
# Create a custom-format archive inside the database container, then copy it to the host.
docker compose -f compose.prod.yml exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d fintrack_db -Fc -f /tmp/fintrack.dump'
docker compose -f compose.prod.yml cp postgres:/tmp/fintrack.dump ./fintrack.dump
```

Restore into a **new isolated database**, never over a live database as a first test:

```sh
docker compose -f compose.prod.yml cp ./fintrack.dump postgres:/tmp/restore.dump
docker compose -f compose.prod.yml exec -T postgres sh -c 'createdb -U "$POSTGRES_USER" fintrack_restore'
docker compose -f compose.prod.yml exec -T postgres sh -c 'pg_restore --exit-on-error --no-owner -U "$POSTGRES_USER" -d fintrack_restore /tmp/restore.dump'
```

Verify table counts, monetary totals, dates, Flyway history and application smoke tests against the restored database. Record restore duration. Only then plan a controlled cutover. Keep the original database and backup until recovery is confirmed; `docker compose down -v` destroys database volumes and is not a recovery procedure.

## Verification

```sh
sh mvnw verify
cd frontend
npm ci
npm test
npm run typecheck
npm run build
npx playwright install chromium
# Start a disposable backend at localhost:18080 with CORS http://localhost:5174 first.
npm run test:e2e
```

There is no configured ESLint task. TypeScript checking is the frontend static check.

PostgreSQL tests are enabled only when `FINTRACK_TEST_DB_URL` ends in `/finance_tracker_it`. They truncate that explicitly disposable database and must never target application data. Set `FINTRACK_TEST_DB_PASSWORD` and use database user `fintrack_test`. H2 tests remain fast by default; CI supplies a PostgreSQL 16 service and runs all tests plus image builds and browser journeys. Windows may use `PLAYWRIGHT_CHANNEL=chrome` for an installed Chrome instead of downloading Chromium.
