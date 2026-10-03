# Correctness and compatibility contracts

## Dates and amounts

Backend LocalDate values are calendar dates (`YYYY-MM-DD`), never timestamps. The frontend keeps them as strings, derives today from local calendar fields, and computes month lengths without converting local midnight to UTC. Date ranges include both endpoints.

Money remains BigDecimal / PostgreSQL NUMERIC(19,2): at most 17 integer digits and 2 decimal places, maximum `99999999999999999.99`. Transactions, recurring amounts and budgets must be at least `0.01`; account balances may be zero or negative. Excess scale/precision is rejected in JSON, direct service calls and CSV rather than rounded by PostgreSQL. Existing aggregate calculations use BigDecimal; percentage calculations retain HALF_UP at two decimal places. JSON monetary values remain numeric for compatibility. The frontend parses decimal tokens losslessly into strings, preserving full precision on display and edit. Other clients should use decimal-aware parsers.

## Authentication

Registration and new-password changes retain the 8-character minimum and enforce BCrypt's 72 UTF-8 byte maximum before hashing. Multibyte characters consume multiple bytes. **Behavior change:** previously accepted passwords beyond this limit now return 400. Existing password hashes are not changed.

Invalid current password still returns HTTP 401, now with `code: INVALID_CURRENT_PASSWORD`. This is an operation failure, not a session failure. Other protected-request 401 responses clear the matching active token, cached user, React context and protected route state. Late failures from requests made with an old token do not invalidate a newer login. Logout and cross-tab storage changes synchronize the UI. Password changes do not revoke already-issued JWTs; they expire according to their original lifetime.

## Categories and schedules

**Behavior change:** a category's type is immutable after creation, including unused categories. Rename it freely or create a new category of the desired type. This avoids concurrent type changes invalidating transactions, budgets or schedules. Ownership remains checked in services; cross-user resource requests return 404. Budgets require an owned EXPENSE category.

Changing a recurring start date or frequency resets the next occurrence to the requested start date. If any occurrence was generated, that new start must be strictly after `lastGeneratedDate`. Changing only amount, category, description, active state or end date preserves the cursor. End dates are inclusive; an end date before the next cursor stops generation. Deactivation pauses without advancing; reactivation resumes the cursor. Historical generated transactions do not change with template updates. Monthly/yearly advancement uses Java calendar clamping (for example, February 29 plus one year becomes February 28); it is not an end-of-month anchoring rule. Due checks use the backend's local date (container default UTC).

`POST /api/recurring-transactions/{id}/generate?occurrenceDate=YYYY-MM-DD` locks the owned template and inserts the transaction plus advances the cursor in one transaction. `(recurring_id, occurrence_date)` is unique in PostgreSQL. Repeat requests for an existing identity return the same transaction; the returned nextRunDate reflects the current cursor. The frontend always supplies its displayed nextRunDate. Omitting occurrenceDate preserves the old API behavior: consume the next due occurrence on every call. Unkeyed calls are **not** retry-idempotent while a schedule has a backlog.

Transaction responses include nullable `recurringId` and `occurrenceDate`; that identity survives template deletion and remains unchanged when a generated transaction is edited. Deleting a generated transaction does not rewind its template; retrying its old occurrence returns 400 rather than recreating it. Occurrence identity is not inferred for pre-migration history.

## CSV

Import header (exact order):

```csv
type,amount,categoryId,description,transactionDate
EXPENSE,12.34,1,Lunch,2024-02-29
```

Uploads are UTF-8 (optional BOM), at most 2 MB. Quoted commas, escaped quotes and multiline fields are supported. Malformed quoting/encoding/header rejects the whole file before writes. Logical record numbers include the header as row 1; blank records are skipped. Row validation uses the same DTO constraints as JSON plus service ownership/type checks.

Valid rows commit independently. The response contains `totalRows`, `successfulRows`, `failedRows`, and errors with `rowNumber`, `code: INVALID_ROW`, and `message`. successfulRows includes rows already committed by an identical upload. A SHA-256 digest of exact file bytes plus logical row number forms the identity, unique per user. Concurrent/repeated uploads cannot duplicate a retained row. Changed bytes are a different import; identical files cannot intentionally be imported twice while their rows remain. Deleting an imported transaction permits that row to be imported again. This is deliberate user deletion, not durable import history.

Infrastructure failures return an error, not a fabricated partial success; earlier rows may already have committed. Retry the **exact same bytes** to resume safely. Invalid rows are reevaluated on retries, while successful rows are not updated. Do not automatically retry a changed file. Pre-upgrade imports have no identity (see operations guide).

Exports remain reporting CSV, not round-trip import templates: they have additional IDs/category names/timestamps. User text that starts with spreadsheet formula markers `=`, `+`, `-`, `@` (including after leading whitespace), or tab/newline control prefixes, gets a leading apostrophe, followed by normal CSV quoting. Monetary cells stay numeric. **Behavior change:** spreadsheet-safe text exports contain this extra apostrophe.

## API additions and stricter behavior

- Additive: error `code`; CSV row error `code`; transaction `recurringId`/`occurrenceDate`; optional generation `occurrenceDate` query parameter.
- Stricter behavior: category type mutation, schedule rewinds, oversized/malformed CSV, oversized UTF-8 passwords and excess monetary precision are rejected.
- CSV exact-file retries deduplicate instead of creating duplicates.
- Database adoption is explicit; existing installations must follow the migration guide. No automatic Hibernate schema updates remain.
- Production OpenAPI/Swagger is disabled; development `/v3/api-docs` remains generated from controllers/DTOs.
