# Personal Finance Tracker Backend

Spring Boot backend for a personal finance tracker. The API supports user registration, login, JWT authentication, user-scoped categories, and user-scoped transactions with filtering, pagination, and sorting.

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

Tests use H2 with PostgreSQL compatibility mode. The most recent verified run passed `62` tests.

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
| `/api/dashboard/summary` | Not implemented in current code snapshot | No dashboard controller found |
| `/api/dashboard/category-breakdown` | Not implemented in current code snapshot | No dashboard controller found |
| `/api/dashboard/trend` | Not implemented in current code snapshot | No dashboard controller found |

## Security and Data Rules

- All `/api/**` endpoints require JWT except `POST /api/auth/register` and `POST /api/auth/login`.
- Category and transaction data is scoped to the authenticated user.
- A user cannot read, update, or delete another user's categories or transactions.
- Transactions can only reference categories owned by the authenticated user.
- Transaction and category types are validated for consistency.
- Transaction descriptions are limited to `255` characters in both DTO validation and the entity column.
