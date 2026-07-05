import { useEffect, useState } from "react";
import { api, errorMessage } from "../api/client";
import StatCard from "../components/StatCard";
import type {
  CategoryBreakdownItem,
  DashboardSummary,
  MonthlyReport,
  TrendItem,
} from "../types/api";

function currentMonth() {
  return new Date().toISOString().slice(0, 7);
}

function monthStart(month: string) {
  return `${month}-01`;
}

function monthEnd(month: string) {
  const [year, monthNumber] = month.split("-").map(Number);
  return new Date(year, monthNumber, 0).toISOString().slice(0, 10);
}

export default function Dashboard() {
  const [month, setMonth] = useState(currentMonth());
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [trend, setTrend] = useState<TrendItem[]>([]);
  const [breakdown, setBreakdown] = useState<CategoryBreakdownItem[]>([]);
  const [report, setReport] = useState<MonthlyReport | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    async function load() {
      setError("");
      try {
        const fromDate = monthStart(month);
        const toDate = monthEnd(month);
        const [summaryRes, trendRes, breakdownRes, reportRes] = await Promise.all([
          api.get<DashboardSummary>("/api/dashboard/summary", { params: { fromDate, toDate } }),
          api.get<{ items: TrendItem[] }>("/api/dashboard/trend", {
            params: { fromDate, toDate, groupBy: "DAY" },
          }),
          api.get<{ items: CategoryBreakdownItem[] }>("/api/dashboard/category-breakdown", {
            params: { fromDate, toDate, type: "EXPENSE" },
          }),
          api.get<MonthlyReport>("/api/reports/monthly", { params: { month } }),
        ]);
        setSummary(summaryRes.data);
        setTrend(trendRes.data.items);
        setBreakdown(breakdownRes.data.items);
        setReport(reportRes.data);
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
          <h2 className="text-2xl font-bold">Dashboard</h2>
          <p className="text-sm text-stone-500">Summary, trend, categories, and monthly report.</p>
        </div>
        <label className="w-full text-sm font-medium md:w-48">
          Month
          <input className="mt-1" type="month" value={month} onChange={(event) => setMonth(event.target.value)} />
        </label>
      </div>
      {error && <p className="rounded-md bg-coral/10 p-3 text-sm text-coral">{error}</p>}
      <div className="grid gap-4 md:grid-cols-4">
        <StatCard label="Income" value={summary?.totalIncome ?? "0"} tone="income" />
        <StatCard label="Expense" value={summary?.totalExpense ?? "0"} tone="expense" />
        <StatCard label="Balance" value={summary?.balance ?? "0"} />
        <StatCard label="Transactions" value={summary?.transactionCount ?? 0} />
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <div className="panel">
          <h3 className="font-semibold">Daily trend</h3>
          <div className="mt-3 space-y-2">
            {trend.slice(0, 10).map((item) => (
              <div key={item.period} className="grid grid-cols-4 gap-2 text-sm">
                <span>{item.period}</span>
                <span className="text-mint">{item.incomeAmount}</span>
                <span className="text-coral">{item.expenseAmount}</span>
                <span>{item.balance}</span>
              </div>
            ))}
            {!trend.length && <p className="text-sm text-stone-500">No trend data.</p>}
          </div>
        </div>
        <div className="panel">
          <h3 className="font-semibold">Expense breakdown</h3>
          <div className="mt-3 space-y-2">
            {breakdown.map((item) => (
              <div key={item.categoryId} className="flex justify-between gap-3 text-sm">
                <span>{item.categoryName}</span>
                <span className="font-semibold">{item.totalAmount}</span>
              </div>
            ))}
            {!breakdown.length && <p className="text-sm text-stone-500">No category data.</p>}
          </div>
        </div>
      </div>
      <div className="panel">
        <h3 className="font-semibold">Monthly report</h3>
        <div className="mt-3 grid gap-4 md:grid-cols-3">
          <StatCard label="Report income" value={report?.totalIncome ?? "0"} tone="income" />
          <StatCard label="Report expense" value={report?.totalExpense ?? "0"} tone="expense" />
          <StatCard label="Report balance" value={report?.balance ?? "0"} />
        </div>
      </div>
    </section>
  );
}
