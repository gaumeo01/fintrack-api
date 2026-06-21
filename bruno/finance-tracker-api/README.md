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
- `month` is used by budget usage and **Reports / Get monthly report**.
- `recurringTransactionId` is used by recurring transaction detail, update, delete, and generate requests.
- `currentPassword` and `newPassword` are placeholders for **Account / Change password**.

## New Requests

- **Transactions / Search transactions by keyword** searches transaction descriptions and category names.
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
5. Create an income category and an expense category.
6. Update `categoryId` with the expense category id.
7. Create income and expense transactions.
8. Update `transactionId` with a transaction id you want to inspect or modify.
9. Run **Transactions / Search transactions by keyword**.
10. Run **Transactions / Export transactions CSV**.
11. Run **Import / Import transactions CSV** with a local CSV file if needed.
12. Create a recurring transaction and update `recurringTransactionId`.
13. Run recurring list, detail, update, generate, and delete requests.
14. Run dashboard requests.
15. Run **Reports / Get monthly report**.
16. Create a budget for the expense category.
17. Update `budgetId` with the created budget id.
18. Run budget list, detail, update, usage, and delete requests.
19. Run **Account / Change password** only after setting placeholder password variables intentionally.
