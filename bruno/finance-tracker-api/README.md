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

## Suggested Manual Test Order

1. Start PostgreSQL with `docker compose up -d`.
2. Start the API with `APP_JWT_SECRET` set.
3. Run **Auth / Register user**.
4. Run **Auth / Login user** and paste the returned token into `accessToken`.
5. Create an income category and an expense category.
6. Update `categoryId` with the expense category id.
7. Create income and expense transactions.
8. Update `transactionId` with a transaction id you want to inspect or modify.
9. Run dashboard requests.
10. Create a budget for the expense category.
11. Update `budgetId` with the created budget id.
12. Run budget list, detail, update, usage, and delete requests.
