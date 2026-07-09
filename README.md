# Finance Tracker API

Finance Tracker API is a Spring Boot REST API for tracking personal income, expenses, accounts, categories, dashboard analytics, recurring transactions, monthly reports, imports/exports, and budgets. It uses JWT authentication and keeps each user's data isolated from every other user.

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Java |
| Framework | Spring Boot |
| Web API | Spring Web |
| Persistence | Spring Data JPA |
| Security | Spring Security, JWT |
| Database | PostgreSQL |
| Local services | Docker Compose |
| Build tool | Maven |
| Testing | JUnit, Spring Boot Test, H2 for tests |

## Main Features

- User registration
- Login with JWT
- Authenticated account/wallet CRUD
- Authenticated category CRUD
- Authenticated transaction CRUD
- Transaction filtering and pagination
- Transaction CSV import and export
- Dashboard summary
- Dashboard category breakdown
- Dashboard trend
- Budget CRUD
- Budget usage/progress API
- Recurring transaction CRUD and manual generation
- Monthly report API
- User-owned data isolation
- Cross-user access protection

## Project Structure

```text
src/main/java/com/gmeo/finance_tracker
|-- auth/          # Registration, login, password changes, JWT issuing
|-- account/       # User-owned account and wallet CRUD
|-- budget/        # Budget CRUD and budget usage/progress
|-- category/      # User-owned income/expense categories
|-- common/        # Shared DTOs, responses, exceptions, utilities
|-- config/        # Spring Security configuration
|-- dashboard/     # Dashboard analytics endpoints
|-- recurring/     # Recurring transaction schedules and generation
|-- report/        # Monthly report endpoints
|-- security/      # JWT filter and current-user lookup
|-- transaction/   # Transaction CRUD, filtering, import/export, specifications
`-- user/          # User entity, repository, and user service
```

Tests live under `src/test/java/com/gmeo/finance_tracker`.

## Requirements

- Java 21
- Docker and Docker Compose
- Maven wrapper from this repository (`mvnw`)

## Environment Variables

| Variable | Required | Default | Description |
| --- | --- | --- | --- |
| `APP_JWT_SECRET` | Yes, outside tests | None | Secret key used to sign JWT access tokens. Use a long random value. |
| `APP_JWT_ACCESS_TOKEN_EXPIRATION_MS` | No | `3600000` | Access token lifetime in milliseconds. |

The local PostgreSQL settings are currently configured in `src/main/resources/application.properties`:

| Property | Value |
| --- | --- |
| Database URL | `jdbc:postgresql://localhost:5433/fintrack_db` |
| Username | `fintrack_user` |
| Password | `fintrack_password` |

Tests use `src/test/resources/application.properties` and run against an in-memory H2 database, so `APP_JWT_SECRET` is not required for tests.

## Run With Docker Compose

The included `docker-compose.yml` starts PostgreSQL only. Run the database first, then start the Spring Boot app from your machine.

```bash
docker compose up -d
```

Set the JWT secret:

```bash
export APP_JWT_SECRET="replace-with-a-long-random-secret-at-least-32-characters"
```

Start the API:

```bash
sh mvnw spring-boot:run
```

The API will use PostgreSQL on `localhost:5433`.

To stop the database:

```bash
docker compose down
```

## Run Tests

```bash
sh mvnw test
```

## Frontend

The React frontend lives in `frontend/`.

```bash
cd frontend
npm install
cp .env.example .env
npm run dev
```

Set `VITE_API_BASE_URL` in `frontend/.env` to the backend origin, for example:

```text
VITE_API_BASE_URL=http://localhost:8080
```

Create a production build with:

```bash
cd frontend
npm run build
```

## Authentication Flow

1. Register a user with `POST /api/auth/register`.
2. Log in with `POST /api/auth/login`.
3. Copy the `accessToken` from the login response.
4. Call protected endpoints with this header:

```http
Authorization: Bearer <accessToken>
```

Only registration and login are public. Endpoints under `/api/**` are protected unless explicitly public.

## API Endpoint Summary

### Auth

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Public | Create a new user account. |
| `POST` | `/api/auth/login` | Public | Log in and receive a JWT access token. |

### Accounts

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/api/accounts` | JWT | Create an account or wallet. |
| `GET` | `/api/accounts` | JWT | List current user's accounts. |
| `GET` | `/api/accounts/{id}` | JWT | Get one owned account. |
| `PUT` | `/api/accounts/{id}` | JWT | Update one owned account. |
| `DELETE` | `/api/accounts/{id}` | JWT | Delete one owned account. |

Account types are `CASH`, `BANK`, `E_WALLET`, `SAVINGS`, `CREDIT_CARD`, and `OTHER`.

Account rules:

- Accounts belong to the authenticated user.
- Cross-user account access returns `404 Not Found`.
- `name`, `type`, and `initialBalance` are required.
- `currentBalance` is optional and defaults to `initialBalance`.
- `active` is optional and defaults to `true`.
- Transactions are not linked to accounts yet; the transaction API is unchanged.

### Categories

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/api/categories` | JWT | Create a category. |
| `GET` | `/api/categories` | JWT | List current user's categories. |
| `GET` | `/api/categories/{id}` | JWT | Get one owned category. |
| `PUT` | `/api/categories/{id}` | JWT | Update one owned category. |
| `DELETE` | `/api/categories/{id}` | JWT | Delete one owned category. |

Category types are `INCOME` and `EXPENSE`.

### Transactions

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/api/transactions` | JWT | Create a transaction. |
| `GET` | `/api/transactions` | JWT | List transactions with filters and pagination. |
| `GET` | `/api/transactions/{id}` | JWT | Get one owned transaction. |
| `PUT` | `/api/transactions/{id}` | JWT | Update one owned transaction. |
| `DELETE` | `/api/transactions/{id}` | JWT | Delete one owned transaction. |
| `GET` | `/api/transactions/export` | JWT | Export filtered transactions as CSV. |
| `POST` | `/api/transactions/import` | JWT | Import transactions from CSV. |

Supported transaction query parameters:

| Parameter | Example | Notes |
| --- | --- | --- |
| `type` | `EXPENSE` | Optional. `INCOME` or `EXPENSE`. |
| `categoryId` | `1` | Optional. |
| `fromDate` | `2026-06-01` | Optional ISO date. |
| `toDate` | `2026-06-30` | Optional ISO date. |
| `minAmount` | `10.00` | Optional. |
| `maxAmount` | `1000.00` | Optional. |
| `page` | `0` | Optional pagination page. |
| `size` | `20` | Optional page size. |
| `sort` | `transactionDate,desc` | Optional Spring pageable sort. |

### Dashboard

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `GET` | `/api/dashboard/summary` | JWT | Income, expense, and balance summary. |
| `GET` | `/api/dashboard/category-breakdown` | JWT | Totals grouped by category. |
| `GET` | `/api/dashboard/trend` | JWT | Time-series income/expense trend. |

Dashboard query parameters:

| Endpoint | Parameters |
| --- | --- |
| `/api/dashboard/summary` | Optional `fromDate`, `toDate` |
| `/api/dashboard/category-breakdown` | Required `fromDate`, `toDate`, `type` |
| `/api/dashboard/trend` | Required `fromDate`, `toDate`; optional `groupBy` defaulting to `MONTH` |

### Reports

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `GET` | `/api/reports/monthly` | JWT | Monthly totals, category rankings, and daily trend. |

Report query parameters:

| Endpoint | Parameters |
| --- | --- |
| `/api/reports/monthly` | Required `month` in `YYYY-MM` format |

### Budgets

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/api/budgets` | JWT | Create a budget for an owned expense category. |
| `GET` | `/api/budgets` | JWT | List current user's budgets. |
| `GET` | `/api/budgets/{id}` | JWT | Get one owned budget. |
| `PUT` | `/api/budgets/{id}` | JWT | Update one owned budget. |
| `DELETE` | `/api/budgets/{id}` | JWT | Delete one owned budget. |
| `GET` | `/api/budgets/{id}/usage` | JWT | Show usage for one owned budget. |

Budget rules:

- Budgets belong to the authenticated user.
- Budgets must use categories owned by the authenticated user.
- Only `EXPENSE` categories can have budgets.
- Cross-user budget access returns `404 Not Found`.
- `categoryId`, `amount`, `startDate`, and `endDate` are required.
- `amount` must be at least `0.01`.
- `startDate` must be on or before `endDate`.
- Budget CRUD responses use `amount`.
- Budget usage responses use `limitAmount`.
- Budget usage counts authenticated-user `EXPENSE` transactions in the same category and inclusive budget date range.
- Budget usage calculates `remainingAmount` and `usagePercentage` with `BigDecimal`.

### Recurring Transactions

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/api/recurring-transactions` | JWT | Create a recurring transaction schedule. |
| `GET` | `/api/recurring-transactions` | JWT | List current user's recurring transactions. |
| `GET` | `/api/recurring-transactions/{id}` | JWT | Get one owned recurring transaction. |
| `PUT` | `/api/recurring-transactions/{id}` | JWT | Update one owned recurring transaction. |
| `DELETE` | `/api/recurring-transactions/{id}` | JWT | Delete one owned recurring transaction. |
| `POST` | `/api/recurring-transactions/{id}/generate` | JWT | Generate the next due transaction and advance the schedule. |

Recurring transaction rules:

- Recurring transactions belong to the authenticated user.
- Recurring transactions must use categories owned by the authenticated user.
- The recurring transaction type must match the category type.
- Cross-user recurring transaction access returns `404 Not Found`.
- `type`, `amount`, `categoryId`, `frequency`, and `startDate` are required.
- `amount` must be at least `0.01`.
- `startDate` must be on or before `endDate` when `endDate` is provided.
- Supported frequencies are `DAILY`, `WEEKLY`, `MONTHLY`, and `YEARLY`.

## Example Requests and Responses

### Register

Request:

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "email": "alex@example.com",
  "password": "password123",
  "fullName": "Alex Nguyen"
}
```

Response:

```json
{
  "id": 1,
  "email": "alex@example.com",
  "fullName": "Alex Nguyen",
  "role": "USER",
  "createdAt": "2026-06-18T09:00:00"
}
```

### Login

Request:

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "alex@example.com",
  "password": "password123"
}
```

Response:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": 1,
    "email": "alex@example.com",
    "fullName": "Alex Nguyen",
    "role": "USER",
    "createdAt": "2026-06-18T09:00:00"
  }
}
```

### Create Category

Request:

```http
POST /api/categories
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "name": "Food",
  "type": "EXPENSE"
}
```

Response:

```json
{
  "id": 1,
  "name": "Food",
  "type": "EXPENSE",
  "createdAt": "2026-06-18T09:05:00",
  "updatedAt": "2026-06-18T09:05:00"
}
```

### Create Account

Request:

```http
POST /api/accounts
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "name": "Cash Wallet",
  "type": "CASH",
  "initialBalance": 1000000.00
}
```

Response:

```json
{
  "id": 3,
  "name": "Cash Wallet",
  "type": "CASH",
  "initialBalance": 1000000.00,
  "currentBalance": 1000000.00,
  "active": true,
  "createdAt": "2026-06-18T09:07:00",
  "updatedAt": "2026-06-18T09:07:00"
}
```

### Create Transaction

Request:

```http
POST /api/transactions
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "type": "EXPENSE",
  "amount": 25.50,
  "categoryId": 1,
  "description": "Lunch",
  "transactionDate": "2026-06-18"
}
```

Response:

```json
{
  "id": 10,
  "type": "EXPENSE",
  "amount": 25.50,
  "categoryId": 1,
  "categoryName": "Food",
  "categoryType": "EXPENSE",
  "description": "Lunch",
  "transactionDate": "2026-06-18",
  "createdAt": "2026-06-18T09:10:00",
  "updatedAt": "2026-06-18T09:10:00"
}
```

### Dashboard Summary

Request:

```http
GET /api/dashboard/summary?fromDate=2026-06-01&toDate=2026-06-30
Authorization: Bearer <accessToken>
```

Response:

```json
{
  "totalIncome": 5000000.00,
  "totalExpense": 2400000.00,
  "balance": 2600000.00,
  "transactionCount": 12
}
```

### Create Budget

Request:

```http
POST /api/budgets
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "categoryId": 1,
  "amount": 3000000.00,
  "startDate": "2026-06-01",
  "endDate": "2026-06-30"
}
```

Response:

```json
{
  "id": 5,
  "categoryId": 1,
  "categoryName": "Food",
  "amount": 3000000.00,
  "startDate": "2026-06-01",
  "endDate": "2026-06-30",
  "createdAt": "2026-06-18T09:15:00",
  "updatedAt": "2026-06-18T09:15:00"
}
```

### Budget Usage

Request:

```http
GET /api/budgets/5/usage
Authorization: Bearer <accessToken>
```

Response:

```json
{
  "budgetId": 5,
  "categoryId": 1,
  "categoryName": "Food",
  "limitAmount": 3000000.00,
  "spentAmount": 2400000.00,
  "remainingAmount": 600000.00,
  "usagePercentage": 80.00,
  "exceeded": false,
  "startDate": "2026-06-01",
  "endDate": "2026-06-30"
}
```

## JWT Security and User Data Isolation

- Protected endpoints require `Authorization: Bearer <accessToken>`.
- JWTs are signed with `APP_JWT_SECRET`; keep this value private and do not commit real secrets.
- The API resolves the current user from the JWT and scopes reads/writes to that user.
- Accounts, categories, transactions, budgets, recurring transactions, reports, dashboard analytics, and budget usage only use data owned by the authenticated user.
- Attempts to access another user's account, category, transaction, budget, or recurring transaction are rejected, typically with `404 Not Found` so resource existence is not leaked.
- Budget creation rejects categories owned by another user.
- Budget usage only counts the authenticated user's `EXPENSE` transactions in the budget category and inclusive budget date range.
