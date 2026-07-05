# Personal Finance Tracker Backend

Spring Boot backend for a personal finance tracker. The API supports user registration, login, JWT authentication, user-scoped categories, user-scoped transactions with filtering, pagination, and sorting, dashboard analytics, and budget tracking.

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Jakarta Validation
- PostgreSQL for local development
- H2 for tests
- Maven Wrapper
- Docker Compose

## Project Structure

- `src/main/java/com/gmeo/finance_tracker/auth`: register, login, JWT services, auth DTOs
- `src/main/java/com/gmeo/finance_tracker/security`: JWT filter and current-user helpers
- `src/main/java/com/gmeo/finance_tracker/category`: category CRUD
- `src/main/java/com/gmeo/finance_tracker/transaction`: transaction CRUD, filters, pagination
- `src/main/java/com/gmeo/finance_tracker/dashboard`: dashboard summary and analytics
- `src/main/java/com/gmeo/finance_tracker/budget`: budget CRUD and usage tracking
- `src/main/java/com/gmeo/finance_tracker/common`: shared responses and exception handling
- `src/main/resources/application.properties`: local PostgreSQL and JWT config
- `src/test`: unit and integration tests using H2
- `docs`: project roadmap and notes
- `docker-compose.yml`: local PostgreSQL service

## Run PostgreSQL

```powershell
docker compose up -d
```

The included Compose file starts PostgreSQL on host port `5433` with:

- Database: `fintrack_db`
- User: `fintrack_user`
- Password: `fintrack_password`

## Environment Variables

The backend reads JWT settings from environment variables:

- `APP_JWT_SECRET`: required secret used to sign JWTs. Use a long, non-public value.
- `APP_JWT_ACCESS_TOKEN_EXPIRATION_MS`: optional access token lifetime in milliseconds. Defaults to `3600000`.

PowerShell example:

```powershell
$env:APP_JWT_SECRET="replace-with-a-long-secret-at-least-32-characters"
$env:APP_JWT_ACCESS_TOKEN_EXPIRATION_MS="3600000"
```

## Run Backend

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS/Linux:

```sh
sh mvnw spring-boot:run
```

## Run Tests

Windows:

```powershell
.\mvnw.cmd test
```

macOS/Linux:

```sh
sh mvnw test
```

Tests use H2 with PostgreSQL compatibility mode. The most recent verified run passed `104` tests.

## Auth Flow

1. Register with `POST /api/auth/register`.
2. Login with `POST /api/auth/login`.
3. Store the returned `accessToken`.
4. Call protected endpoints with:

```http
Authorization: Bearer <accessToken>
```

The login response includes `accessToken`, `tokenType` set to `Bearer`, and a user DTO without password data.

## Endpoint Summary

| Endpoint | Status | Notes |
| --- | --- | --- |
| `GET /api/health` | Public | Health check |
| `POST /api/auth/register` | Public | Creates a user |
| `POST /api/auth/login` | Public | Returns Bearer access token |
| `POST /api/categories` | Protected | Create category for authenticated user |
| `GET /api/categories` | Protected | List authenticated user's categories |
| `GET /api/categories/{id}` | Protected | Get owned category |
| `PUT /api/categories/{id}` | Protected | Update owned category |
| `DELETE /api/categories/{id}` | Protected | Delete owned category |
| `POST /api/transactions` | Protected | Create transaction for authenticated user |
| `GET /api/transactions` | Protected | Filter, paginate, and sort authenticated user's transactions |
| `GET /api/transactions/{id}` | Protected | Get owned transaction |
| `PUT /api/transactions/{id}` | Protected | Update owned transaction |
| `DELETE /api/transactions/{id}` | Protected | Delete owned transaction |
| `GET /api/dashboard/summary` | Protected | Authenticated user's income, expense, balance, and transaction count |
| `GET /api/dashboard/category-breakdown` | Protected | Authenticated user's category totals for a date range and type |
| `GET /api/dashboard/trend` | Protected | Authenticated user's date-bucketed income and expense trend |
| `POST /api/budgets` | Protected | Create budget for authenticated user |
| `GET /api/budgets` | Protected | List authenticated user's budgets |
| `GET /api/budgets/{id}` | Protected | Get owned budget |
| `PUT /api/budgets/{id}` | Protected | Update owned budget |
| `DELETE /api/budgets/{id}` | Protected | Delete owned budget |
| `GET /api/budgets/{id}/usage` | Protected | Get usage for owned budget |

## Budget API

All `/api/budgets/**` endpoints require `Authorization: Bearer <accessToken>`. A missing or invalid JWT returns `401 Unauthorized`.

Budget request body:

```json
{
  "categoryId": 3,
  "amount": 500.00,
  "startDate": "2026-07-01",
  "endDate": "2026-07-31"
}
```

Budget response fields are `id`, `categoryId`, `categoryName`, `amount`, `startDate`, `endDate`, `createdAt`, and `updatedAt`.

Budget usage response fields are `budgetId`, `categoryId`, `categoryName`, `limitAmount`, `spentAmount`, `remainingAmount`, `usagePercentage`, `exceeded`, `startDate`, and `endDate`.

Business rules:

- Budgets belong to the authenticated user.
- The selected category must belong to the authenticated user.
- The selected category must be an `EXPENSE` category.
- Cross-user budget access returns `404 Not Found`.
- `categoryId`, `amount`, `startDate`, and `endDate` are required.
- `amount` must be at least `0.01`.
- `startDate` must be on or before `endDate`.
- Usage calculates `spentAmount` from authenticated-user `EXPENSE` transactions in the same category and inclusive date range.
- Usage calculates `remainingAmount` and `usagePercentage` with `BigDecimal`, not `double`.

Create budget:

```http
POST /api/budgets
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "categoryId": 3,
  "amount": 500.00,
  "startDate": "2026-07-01",
  "endDate": "2026-07-31"
}
```

```json
{
  "id": 10,
  "categoryId": 3,
  "categoryName": "Groceries",
  "amount": 500.00,
  "startDate": "2026-07-01",
  "endDate": "2026-07-31",
  "createdAt": "2026-07-05T10:15:30",
  "updatedAt": "2026-07-05T10:15:30"
}
```

List budgets:

```http
GET /api/budgets
Authorization: Bearer <accessToken>
```

```json
[
  {
    "id": 10,
    "categoryId": 3,
    "categoryName": "Groceries",
    "amount": 500.00,
    "startDate": "2026-07-01",
    "endDate": "2026-07-31",
    "createdAt": "2026-07-05T10:15:30",
    "updatedAt": "2026-07-05T10:15:30"
  }
]
```

Get budget usage:

```http
GET /api/budgets/10/usage
Authorization: Bearer <accessToken>
```

```json
{
  "budgetId": 10,
  "categoryId": 3,
  "categoryName": "Groceries",
  "limitAmount": 500.00,
  "spentAmount": 125.50,
  "remainingAmount": 374.50,
  "usagePercentage": 25.10,
  "exceeded": false,
  "startDate": "2026-07-01",
  "endDate": "2026-07-31"
}
```

## Security and Data Rules

- All `/api/**` endpoints require JWT except `POST /api/auth/register` and `POST /api/auth/login`.
- Missing or invalid JWT credentials return `401 Unauthorized`.
- Category, transaction, dashboard, and budget data is scoped to the authenticated user.
- A user cannot read, update, or delete another user's categories, transactions, or budgets.
- Transactions can only reference categories owned by the authenticated user.
- Transaction and category types are validated for consistency.
- Transaction descriptions are limited to `255` characters in both DTO validation and the entity column.
