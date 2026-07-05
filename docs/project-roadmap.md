# Personal Finance Tracker Backend Roadmap

## Completed

- Health API
- PostgreSQL Docker setup
- H2 test configuration
- Global exception handler
- User registration
- User login
- JWT authentication
- Bearer-token protected API endpoints
- User ownership and data isolation
- Cross-user category and transaction access blocking
- Category CRUD
- Transaction CRUD
- Transaction filtering, pagination, and sorting
- Transaction/category type consistency validation
- Transaction description limit aligned at `255` characters in DTO and entity
- Dashboard analytics backend
- Budget CRUD backend
- Budget usage backend
- Unit and integration test coverage for auth, validation, filtering, and user isolation
- README backend setup documentation

## Current API Modules

- Auth: `POST /api/auth/register`, `POST /api/auth/login`
- Categories: `POST`, `GET`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` under `/api/categories`
- Transactions: `POST`, `GET`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` under `/api/transactions`
- Dashboard: `GET /summary`, `GET /category-breakdown`, `GET /trend` under `/api/dashboard`
- Budgets: `POST`, `GET`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`, `GET /{id}/usage` under `/api/budgets`
- Health: `GET /api/health`

## Planned / Not Present In Current Code Snapshot

- Frontend budget UI
- Budget alerts/notifications
- Report/statistics API
- Import/export support

## Notes

- Protected endpoints require `Authorization: Bearer <accessToken>`.
- User data must stay scoped to the authenticated user.
- Transactions must use a category owned by the authenticated user.
- Budgets must use an `EXPENSE` category owned by the authenticated user.
- Budget usage is calculated from matching authenticated-user `EXPENSE` transactions in the inclusive budget date range.
- The latest verified full test run passed `104` tests.
