# Finance Tracker implementation report

## 1. Starting State

- Branch: `main`; starting HEAD: `22ddd9041ab451cfeb6b46f2238bb22edb092855`.
- Working tree was clean and matched the audit. Recent history included the local CORS fix, release merge and account management work. No user changes were overwritten and no branch switch occurred.
- Backend baseline: 169 tests passed, zero failures/errors/skips.
- Frontend baseline: TypeScript + Vite production build passed. No frontend tests or lint command existed.
- Maven initially lacked permission to write its user cache; the approved Maven invocation resolved it. PowerShell blocked npm.ps1, so npm.cmd was used. Docker access was approved and worked. No environment blocker remains.
- Architecture: Spring Boot 4.0.6/Java 21/JPA/PostgreSQL, React/Vite/TypeScript, H2 tests, manual recurring generation, custom CSV parser, Hibernate schema updates with no migrations.

## 2. Problems Confirmed

| Audit item | Confirmation / root cause | Severity | Implemented solution |
| --- | --- | --- | --- |
| Calendar dates | Confirmed: UTC serialization of local month-end and UTC-based today defaults | High | Calendar helpers, local today, string dates, timezone regression runs |
| Session consistency | Confirmed: interceptor removed storage but left React state; all 401s treated alike | High | Synchronized invalidation/logout/storage events, stale-request guard, INVALID_CURRENT_PASSWORD code |
| Password and money limits | Confirmed: password character cap exceeded BCrypt byte cap; DTOs lacked precision; CSV bypassed length/precision rules | High | UTF-8 byte constraint, shared service validation, Digits(17,2), lossless frontend JSON parsing |
| Domain invariants | Confirmed: mutable category types and unchanged schedule cursor after schedule edits | High | Immutable category type, explicit non-rewinding rescheduling; existing ownership/type checks preserved |
| Migrations | Confirmed: ddl-auto=update; legacy month schema required an adoption path | High | Flyway V1-V3, explicit baseline adoption, legacy ranges backfill, Hibernate validate |
| Recurring atomicity | Confirmed: separate saves without transaction, lock or durable occurrence identity | High | Service transaction, owned row lock, unique occurrence identity, keyed retry |
| CSV | Confirmed: incomplete validation, formula exposure, no retry identity; partial results already existed | High | Shared constraints, strict parsing, structured error code, exact-file row identity, safe exports |
| Tests | Confirmed: H2-only database coverage and no frontend behavior suite | High | PostgreSQL tests, Vitest behavior tests and Playwright core journeys |
| Production | Partial: multi-stage image already existed but ran as root; no frontend runtime/profile/readiness/recovery instructions | Medium | Non-root images, production Compose/profile, required secrets, health, safe logging, operations guide |
| Docs / CI | Confirmed: limited CI and stale Compose description; OpenAPI dependency targeted another Boot major | Medium | Updated README/Bruno/OpenAPI contracts, CI tests/package/browser/images |

Already-correct behavior was preserved: BigDecimal calculations, NUMERIC(19,2) storage, owned resource lookups/404 isolation, budget EXPENSE restriction, inclusive ranges, percentage HALF_UP rounding, and the basic multi-stage backend architecture.

The existing OpenAPI dependency was moved from 2.8.9 to 3.0.3 because [springdoc documents Boot 4 compatibility in its 3.x line](https://springdoc.org/). Runtime smoke testing also found the generic exception handler incorrectly returned 500 for disabled/missing public endpoints; a specific 404 handler and regression test now cover it.

## 3. Changes Implemented

Key paths, relative to the repository root:

- Frontend: `frontend/src/lib/dates.ts`, `session.ts`, `json.ts`; `frontend/src/api/client.ts`; `frontend/src/context/AuthContext.tsx`; date defaults and ranges in Transactions, Budgets, Dashboard, MonthlyReport and RecurringTransactions pages; immutable type control in Categories.
- Authentication: `src/main/java/com/gmeo/finance_tracker/auth/AuthService.java`, `JwtService.java`, auth request DTOs; `common/exception/InvalidCredentialsException.java`, `GlobalExceptionHandler.java`, `common/response/ApiErrorResponse.java`.
- Validation: `src/main/java/com/gmeo/finance_tracker/common/validation/RequestValidation.java`, `PasswordBytes.java`, `PasswordBytesValidator.java`; transaction/budget/recurring/account DTOs and service entry points.
- Domain and recurring: `category/CategoryService.java`; `recurring/RecurringTransactionService.java`, repository, entity and controller; transaction identity fields and response mapping.
- CSV: `transaction/TransactionService.java`, repository/entity, controller documentation and `dto/TransactionImportError.java`.
- Database: `src/main/resources/db/migration/V1__baseline.sql`, `V2__budget_ranges_and_occurrence_identity.sql`, `V3__csv_import_identity.sql`; application properties and `pom.xml`.
- Testing: `src/test/java/com/gmeo/finance_tracker/database/PostgresIntegrationTests.java`, `common/ValidationPolicyTests.java`, existing auth/category/CSV tests; frontend tests, `frontend/scripts/test-timezones.mjs`, `frontend/e2e/core.spec.ts`, Playwright config.
- Production: `Dockerfile`, `.dockerignore`, `frontend/Dockerfile`, `frontend/.dockerignore`, `frontend/nginx.conf`, `compose.prod.yml`, `src/main/resources/application-prod.properties`, `config/SecurityConfig.java`.
- CI: `.github/workflows/ci.yml`.
- Documentation: `README.md`, `frontend/README.md`, `docs/contracts.md`, `docs/operations.md`, roadmap, Bruno README/generation request/environment. A complete file inventory is below.

## 4. Important Design Decisions

- Dates remain ISO calendar strings. Local clock fields determine today; month-end uses calendar arithmetic without timestamp conversion.
- A 401 with INVALID_CURRENT_PASSWORD preserves the session. Other matching-token 401s clear storage and React state. Old in-flight requests cannot invalidate a newer token. Missing/corrupt storage and cross-tab logout clear stale identity.
- New passwords retain the existing minimum length and enforce BCrypt's 72 UTF-8 byte cap before hashing; no arbitrary reduced character maximum.
- Monetary inputs allow 17 integer digits and 2 fractional digits; excess scale is rejected, not rounded. Account balances retain signed values. Numeric JSON remains compatible; the frontend preserves decimal tokens as strings before JavaScript can lose precision.
- Category type is immutable even without references. This deliberate stronger invariant avoids check-then-insert races involving future references and preserves historical meaning.
- Changing frequency/start resets the cursor only to a start strictly after the latest generated occurrence. Other template changes preserve the cursor; generated history is unchanged.
- Recurring occurrence identity is template ID plus scheduled date. It remains a scalar identity after template deletion. A pessimistic owned-row lock serializes generation/update/delete; one transaction commits the generated row and cursor together. Database uniqueness protects the identity independently of application checks.
- The optional occurrenceDate query parameter makes retries explicit without silently breaking the legacy next-due endpoint. The frontend supplies it. Unkeyed backlog generation is intentionally not idempotent.
- CSV remains partial-success. Exact bytes SHA-256 plus logical row number, scoped to user, deduplicates retries at the database. Valid rows commit independently; already-committed rows count as successful. Infrastructure errors propagate and exact-file retry resumes safely. Changing bytes means a new import.

## 5. Database Changes

- V1 creates the current six-table baseline on empty PostgreSQL databases.
- V2 adds/backfills budget ranges, positive-amount/date checks, recurring identity/cursor history, unique (recurring_id, occurrence_date), paired-null identity check and indexes on transaction user/date and category plus budget/recurring/category/account ownership/reference access.
- V3 adds import_key and unique (user_id, import_key).
- Legacy budget_month values are retained and made nullable; no data column is dropped. Existing installations must explicitly baseline at V1 after schema verification and backup rehearsal. Automatic baseline is disabled.
- Pre-upgrade generated/imported rows cannot have trustworthy provenance reconstructed. They remain unlinked. A conservative cursor watermark prevents legacy schedule rewinds.
- The unmanaged historical month schema adoption, empty schema migrations and Hibernate validation were tested in disposable PostgreSQL 16. No live user database was migrated.

## 6. API Contract Changes

**BREAKING behavior tightening:** category type updates now reject with 400; unsafe schedule rewinds reject; over-limit passwords, monetary scale/precision, invalid/oversized CSV reject; identical CSV retries deduplicate instead of inserting again; exported formula-like text gains a safety apostrophe. Existing clients relying on those old behaviors must adjust.

Additive: error code field, INVALID_CURRENT_PASSWORD, CSV INVALID_ROW code, optional generation occurrenceDate, transaction recurringId/occurrenceDate. HTTP 401 for wrong current password is preserved. Missing public endpoints correctly return 404. Production Swagger/OpenAPI is disabled; health/readiness endpoints are available without details. Full contracts and migration implications are in `docs/contracts.md` and `docs/operations.md`.

## 7. Tests Added

- Calendar leap years (including 2000/2100), month/year boundaries and local today at a UTC boundary; separate processes for UTC, Asia/Bangkok and America/Los_Angeles.
- Matching-token invalidation, old-token response protection, wrong-password preservation, logout, corrupt/missing storage and cross-tab cleanup; exact maximum monetary JSON parsing.
- BCrypt ASCII/Thai/emoji byte boundaries; monetary minimum/maximum, overflow, excess fraction and negatives.
- Category type rejection; actual expired JWT; wrong current password code and continued token validity; missing public endpoint 404.
- CSV partial precision/date/description failures, retained successes on retry, spreadsheet formula escaping.
- PostgreSQL migrations/legacy adoption, exact money, database uniqueness, simultaneous occurrence requests, repeat retry, failure after insert with full rollback, non-rewinding rescheduling/history preservation and concurrent CSV upload deduplication.
- Two real-browser journeys cover login, transaction create/edit/filter on leap day, budget dates/create, recurring create/generate, wrong and successful password change, logout and protected-route invalid-session redirection.

## 8. Verification Results

| Check | Final result |
| --- | --- |
| Full Maven verify (tests + package) | 184 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS |
| PostgreSQL integration subset | All 7 passed, included in the full total |
| Frontend behavior tests | 9 passed |
| Separate timezone runs | 2 date tests passed in each of UTC/Bangkok/Los Angeles |
| TypeScript check | Passed |
| Frontend production build | Passed |
| Real Chrome browser E2E | 2 passed against final backend image |
| Backend production Docker build | Passed, fintrack-api:hardening |
| Frontend production Docker build | Passed, fintrack-web:hardening |
| Runtime smoke | Readiness 200, protected API 401, disabled production docs 404, non-root runtimes |
| Production Compose | Valid with dummy injected values; missing JWT secret fails as expected |
| git diff --check | Passed |
| Secret review | No production secrets introduced; changed-file private-key/token scan clean; only explicit test credentials and existing development defaults |
| TODO/FIXME review | No matches in modified runtime/frontend/documentation areas |

No ESLint configuration existed, so no lint result is claimed. CI YAML was updated and its substantive commands were run locally; GitHub-hosted execution itself has not run. Build/test logs are in ignored `target/` files. Both initial environmental access issues were resolved; no verification was omitted for lack of Docker or Maven access.

## 9. Remaining Risks / Technical Debt

- **Medium:** npm audit reports five high-severity dependency entries along the Tailwind 3 build-time glob chain and one low-severity esbuild development-server advisory. Compatible fixes were applied, reducing the initial 13 advisories to 6. The remaining Tailwind remediation offered by npm is a major-version migration; it was not forced into this correctness change. These Node tools are absent from the final nginx runtime image. Do not expose development servers or process untrusted build globs.
- **Medium:** stateless JWTs remain valid until expiration after password change. This is existing behavior, explicitly documented; immediate global session revocation would require token-version/revocation design.
- **Medium deployment prerequisite:** the real existing database has not been upgraded. Rehearse adoption on its backup, verify schema/data compatibility and reconcile pre-upgrade CSV imports before production rollout. Synthetic legacy/empty database tests do not substitute for that data-specific rehearsal.

No known critical issue remains from the implemented scope. No production deployment/push was performed.

## 10. Git Summary

- Final branch: `main`; final HEAD: `22ddd9041ab451cfeb6b46f2238bb22edb092855`.
- Commits created: none. Push: none. No history rewrite or destructive Git operation.
- Final tree contains the implementation as uncommitted changes: 52 tracked files modified and 26 new files (including this report); no tracked files deleted.
- Temporary test containers/network are removed after verification; locally built images remain available. User application containers/volumes were not modified.

## 11. Recommended Next Actions

1. Review the changes and breaking behavior notes, then create logical commits.
2. Let the updated GitHub CI run on the reviewed branch/PR.
3. Rehearse migration and backup restoration using a copy of the actual database before deployment; configure HTTPS and injected production secrets.
4. Plan the remaining frontend build-tool advisory remediation and, if required by the deployment threat model, immediate JWT revocation after password changes.

## Complete File Inventory

### Modified

- `.github/workflows/ci.yml`
- `.gitignore`
- `Dockerfile`
- `README.md`
- `bruno/finance-tracker-api/README.md`
- `bruno/finance-tracker-api/Recurring Transactions/Generate recurring transaction.bru`
- `bruno/finance-tracker-api/environments/local.bru`
- `docs/project-roadmap.md`
- `frontend/README.md`
- `frontend/package-lock.json`
- `frontend/package.json`
- `frontend/src/api/client.ts`
- `frontend/src/context/AuthContext.tsx`
- `frontend/src/pages/Budgets.tsx`
- `frontend/src/pages/Categories.tsx`
- `frontend/src/pages/Dashboard.tsx`
- `frontend/src/pages/MonthlyReport.tsx`
- `frontend/src/pages/RecurringTransactions.tsx`
- `frontend/src/pages/Transactions.tsx`
- `pom.xml`
- `src/main/java/com/gmeo/finance_tracker/account/AccountService.java`
- `src/main/java/com/gmeo/finance_tracker/account/dto/AccountRequest.java`
- `src/main/java/com/gmeo/finance_tracker/auth/AuthController.java`
- `src/main/java/com/gmeo/finance_tracker/auth/AuthService.java`
- `src/main/java/com/gmeo/finance_tracker/auth/JwtService.java`
- `src/main/java/com/gmeo/finance_tracker/auth/dto/ChangePasswordRequest.java`
- `src/main/java/com/gmeo/finance_tracker/auth/dto/RegisterRequest.java`
- `src/main/java/com/gmeo/finance_tracker/budget/BudgetService.java`
- `src/main/java/com/gmeo/finance_tracker/budget/dto/BudgetRequest.java`
- `src/main/java/com/gmeo/finance_tracker/category/CategoryService.java`
- `src/main/java/com/gmeo/finance_tracker/common/exception/GlobalExceptionHandler.java`
- `src/main/java/com/gmeo/finance_tracker/common/exception/InvalidCredentialsException.java`
- `src/main/java/com/gmeo/finance_tracker/common/response/ApiErrorResponse.java`
- `src/main/java/com/gmeo/finance_tracker/config/SecurityConfig.java`
- `src/main/java/com/gmeo/finance_tracker/recurring/RecurringTransaction.java`
- `src/main/java/com/gmeo/finance_tracker/recurring/RecurringTransactionController.java`
- `src/main/java/com/gmeo/finance_tracker/recurring/RecurringTransactionRepository.java`
- `src/main/java/com/gmeo/finance_tracker/recurring/RecurringTransactionService.java`
- `src/main/java/com/gmeo/finance_tracker/recurring/dto/RecurringTransactionRequest.java`
- `src/main/java/com/gmeo/finance_tracker/transaction/Transaction.java`
- `src/main/java/com/gmeo/finance_tracker/transaction/TransactionController.java`
- `src/main/java/com/gmeo/finance_tracker/transaction/TransactionRepository.java`
- `src/main/java/com/gmeo/finance_tracker/transaction/TransactionService.java`
- `src/main/java/com/gmeo/finance_tracker/transaction/dto/TransactionImportError.java`
- `src/main/java/com/gmeo/finance_tracker/transaction/dto/TransactionRequest.java`
- `src/main/java/com/gmeo/finance_tracker/transaction/dto/TransactionResponse.java`
- `src/main/resources/application.properties`
- `src/test/java/com/gmeo/finance_tracker/category/CategoryServiceTests.java`
- `src/test/java/com/gmeo/finance_tracker/security/JwtSecurityIntegrationTests.java`
- `src/test/java/com/gmeo/finance_tracker/transaction/TransactionImportFlowTests.java`
- `src/test/java/com/gmeo/finance_tracker/transaction/TransactionServiceTests.java`
- `src/test/resources/application.properties`

### Created

- `.dockerignore`
- `compose.prod.yml`
- `docs/contracts.md`
- `docs/implementation-report.md`
- `docs/operations.md`
- `frontend/.dockerignore`
- `frontend/Dockerfile`
- `frontend/e2e/core.spec.ts`
- `frontend/nginx.conf`
- `frontend/playwright.config.ts`
- `frontend/scripts/test-timezones.mjs`
- `frontend/src/context/AuthContext.test.tsx`
- `frontend/src/lib/dates.test.ts`
- `frontend/src/lib/dates.ts`
- `frontend/src/lib/json.test.ts`
- `frontend/src/lib/json.ts`
- `frontend/src/lib/session.ts`
- `src/main/java/com/gmeo/finance_tracker/common/validation/PasswordBytes.java`
- `src/main/java/com/gmeo/finance_tracker/common/validation/PasswordBytesValidator.java`
- `src/main/java/com/gmeo/finance_tracker/common/validation/RequestValidation.java`
- `src/main/resources/application-prod.properties`
- `src/main/resources/db/migration/V1__baseline.sql`
- `src/main/resources/db/migration/V2__budget_ranges_and_occurrence_identity.sql`
- `src/main/resources/db/migration/V3__csv_import_identity.sql`
- `src/test/java/com/gmeo/finance_tracker/common/ValidationPolicyTests.java`
- `src/test/java/com/gmeo/finance_tracker/database/PostgresIntegrationTests.java`
