# Local November 2026 demo

Start the development backend on port 8080, then run from the repository root:

```powershell
node scripts/demo-vnd.mjs --local-demo
```

The script is intentionally fixed to the loopback development API. Never run it
against a production instance or a tunnel to production. It uses normal authenticated
APIs and creates a separate account. It saves that account's credentials in
`.local/demo-vnd-credentials.json` (gitignored). Open this file locally to obtain
the login details; do not share it, paste it into logs, or commit it. Retain the
file to reuse the same account on subsequent runs. Deleting it creates a new
dedicated account on the next run. A lock prevents concurrent runs; after an
interrupted process, confirm it has stopped before removing its stale lock.

The seed checks existing categories, transactions and budgets. It adds missing
records, refuses to overwrite conflicting demo records, and never deletes data.
Run it twice to verify the second run reports zero new transactions. Descriptions
carry a stable `[DEMO-VND-202611]` marker. Existing users' data is not inspected
or changed through this account.

Expected data: 8 categories, 28 transactions (3 income, 25 expense), and 5 budgets.
November income is 18,500,000 VND, expense 6,500,000 VND, balance 12,000,000 VND.
The script verifies actual API summaries and monthly report against stored rows.
Budget statuses come from the backend: food/transport SAFE, study/entertainment
WARNING, shopping OVER_BUDGET.

Start the frontend with:

```powershell
cd frontend
npm.cmd run dev -- --host 127.0.0.1 --port 3001 --strictPort
```

Sign in at http://127.0.0.1:3001/login with the saved credentials. Select November
2026 on Dashboard, Budgets and Monthly Report. Screenshot routes: `/dashboard`,
`/transactions`, `/budgets`, `/reports/monthly`.

VND formatting is presentation-only. Inputs and CSV keep unlocalized decimal
values; database precision and calculations are unchanged. Fractional amounts
are rounded only for display. Old small amounts are not automatically converted.
