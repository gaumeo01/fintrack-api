# Finance Tracker API Bruno Collection

This Bruno collection contains example requests for manually testing the Finance Tracker API.

## Open The Collection

1. Open Bruno.
2. Choose **Open Collection**.
3. Select the `bruno/finance-tracker-api` folder.
4. Select the `local` environment.

## Configure Variables

- `baseUrl` defaults to `http://localhost:8080`.
- Change `baseUrl` in the `local` environment if your API runs on another host or port.
- After calling **Auth / Login user**, copy the `accessToken` value from the response.
- Paste it into the `accessToken` environment variable.
- Protected requests already include `Authorization: Bearer {{accessToken}}`.
- `keyword` is used by **Transactions / Search transactions by keyword**.
- `month` is used by **Reports / Get monthly report**.
- `fromDate` and `toDate` are used by dashboard, transaction filters, and budget date ranges.
- `accountId` is used by account detail, update, and delete requests.
- `recurringTransactionId` is used by recurring transaction detail, update, delete, and generate requests.
- `currentPassword` and `newPassword` are placeholders for **Account / Change password**.

## Budget API Note

Budgets use the date-range API: `GET /api/budgets`, `POST /api/budgets`, `GET /api/budgets/{id}`, `PUT /api/budgets/{id}`, `DELETE /api/budgets/{id}`, and `GET /api/budgets/{id}/usage`. The older month query endpoints are not used by this collection. Usage responses include `limitAmount`, `usagePercentage`, and `status` values of `SAFE`, `WARNING`, or `OVER_BUDGET`.

## New Requests

- **Transactions / Search transactions by keyword** searches transaction descriptions and category names.
- **Accounts** contains account/wallet CRUD requests.
- **Transactions / Export transactions CSV** downloads the authenticated user's transactions as CSV.
- **Import / Import transactions CSV** uploads transactions from a CSV file.
- **Reports / Get monthly report** returns monthly totals, top categories, and daily trend.
- **Recurring Transactions** contains CRUD requests and manual generation.
- **Account / Change password** updates the authenticated user's password.

## Suggested Manual Test Order

1. Start PostgreSQL with `docker compose up -d`.
2. Start the API with `APP_JWT_SECRET` set.
3. Run **Auth / Register user**.
4. Run **Auth / Login user** and paste the returned token into `accessToken`.
5. Create an account and update `accountId`.
6. Run account list, detail, update, and delete requests.
7. Create an income category and an expense category.
8. Update `categoryId` with the expense category id.
9. Create income and expense transactions.
10. Update `transactionId` with a transaction id you want to inspect or modify.
11. Run **Transactions / Search transactions by keyword**.
12. Run **Transactions / Export transactions CSV**.
13. Run **Import / Import transactions CSV** with a local CSV file if needed.
14. Create a recurring transaction and update `recurringTransactionId`.
15. Run recurring list, detail, update, generate, and delete requests.
16. Run dashboard requests.
17. Run **Reports / Get monthly report**.
18. Create a budget for the expense category.
19. Update `budgetId` with the created budget id.
20. Run budget list, detail, update, usage, and delete requests.
21. Run **Account / Change password** only after setting placeholder password variables intentionally.
