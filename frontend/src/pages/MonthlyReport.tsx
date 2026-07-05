import { useEffect, useState } from "react";
import { api, errorMessage } from "../api/client";
import StatCard from "../components/StatCard";
import type { MonthlyReport as MonthlyReportData } from "../types/api";

function currentMonth() {
  return new Date().toISOString().slice(0, 7);
}

export default function MonthlyReport() {
  const [month, setMonth] = useState(currentMonth());
  const [report, setReport] = useState<MonthlyReportData | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    async function load() {
      setError("");
      try {
        const { data } = await api.get<MonthlyReportData>("/api/reports/monthly", { params: { month } });
        setReport(data);
      } catch (err) {
        setError(errorMessage(err));
      }
    }
    void load();
  }, [month]);

  return (
    <section className="space-y-6">
      <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
        <div>
          <h2 className="text-2xl font-bold">Monthly Report</h2>
          <p className="text-sm text-stone-500">Income, expenses, categories, and daily movement.</p>
        </div>
        <label className="w-full text-sm font-medium md:w-48">
          Month
          <input className="mt-1" type="month" value={month} onChange={(event) => setMonth(event.target.value)} />
        </label>
      </div>
      {error && <p className="rounded-md bg-coral/10 p-3 text-sm text-coral">{error}</p>}
      <div className="grid gap-4 md:grid-cols-4">
        <StatCard label="Income" value={report?.totalIncome ?? "0"} tone="income" />
        <StatCard label="Expense" value={report?.totalExpense ?? "0"} tone="expense" />
        <StatCard label="Balance" value={report?.balance ?? "0"} />
        <StatCard label="Transactions" value={report?.transactionCount ?? 0} />
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <div className="panel">
          <h3 className="font-semibold">Top expense categories</h3>
          <div className="mt-3 space-y-2">
            {report?.topExpenseCategories.map((item) => (
              <div key={item.categoryId} className="flex justify-between gap-3 text-sm">
                <span>{item.categoryName}</span>
                <span className="font-semibold">{item.amount}</span>
              </div>
            ))}
            {!report?.topExpenseCategories.length && <p className="text-sm text-stone-500">No expenses.</p>}
          </div>
        </div>
        <div className="panel">
          <h3 className="font-semibold">Top income categories</h3>
          <div className="mt-3 space-y-2">
            {report?.topIncomeCategories.map((item) => (
              <div key={item.categoryId} className="flex justify-between gap-3 text-sm">
                <span>{item.categoryName}</span>
                <span className="font-semibold">{item.amount}</span>
              </div>
            ))}
            {!report?.topIncomeCategories.length && <p className="text-sm text-stone-500">No income.</p>}
          </div>
        </div>
      </div>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Income</th>
              <th>Expense</th>
              <th>Balance</th>
              <th>Transactions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-stone-100">
            {report?.dailyTrend.map((item) => (
              <tr key={item.date}>
                <td>{item.date}</td>
                <td className="text-mint">{item.income}</td>
                <td className="text-coral">{item.expense}</td>
                <td>{item.balance}</td>
                <td>{item.transactionCount}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
