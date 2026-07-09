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
- Account/wallet CRUD
- Cross-user account access blocking
- Cross-user category and transaction access blocking
- Category CRUD
- Transaction CRUD
- Transaction filtering, pagination, and sorting
- Transaction/category type consistency validation
- Transaction description limit aligned at `255` characters in DTO and entity
- Dashboard analytics backend
- Budget CRUD backend
- Budget usage backend
- Transaction CSV import/export support
- Monthly report API
- Recurring transaction CRUD and manual generation
- Recurring transaction date range validation
- Unit and integration test coverage for auth, validation, filtering, and user isolation
- README backend setup documentation

## Current API Modules

- Auth: `POST /api/auth/register`, `POST /api/auth/login`
- Accounts: `POST`, `GET`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` under `/api/accounts`
- Categories: `POST`, `GET`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` under `/api/categories`
- Transactions: `POST`, `GET`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`, `GET /export`, `POST /import` under `/api/transactions`
- Dashboard: `GET /summary`, `GET /category-breakdown`, `GET /trend` under `/api/dashboard`
- Budgets: `POST`, `GET`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`, `GET /{id}/usage` under `/api/budgets`
- Reports: `GET /monthly` under `/api/reports`
- Recurring transactions: `POST`, `GET`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`, `POST /{id}/generate` under `/api/recurring-transactions`
- Health: `GET /api/health`

## Planned / Not Present In Current Code Snapshot

- Frontend budget UI
- Frontend account UI
- Budget alerts/notifications
- Link transactions to accounts
- Scheduled recurring transaction generation

## Notes

- Protected endpoints require `Authorization: Bearer <accessToken>`.
- User data must stay scoped to the authenticated user.
- Accounts are owned by the authenticated user and are not linked to transactions yet.
- Transactions must use a category owned by the authenticated user.
- Budgets must use an `EXPENSE` category owned by the authenticated user.
- Recurring transactions must use a category owned by the authenticated user and `startDate` must be on or before `endDate` when provided.
- Budget usage is calculated from matching authenticated-user `EXPENSE` transactions in the inclusive budget date range.
- The latest verified full test run passed `166` tests.
