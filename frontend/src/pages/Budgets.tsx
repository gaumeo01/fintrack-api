import { FormEvent, useEffect, useMemo, useState } from "react";
import { api, errorMessage } from "../api/client";
import StatusBadge from "../components/StatusBadge";
import type { Budget, BudgetUsageItem, Category } from "../types/api";

function monthRange(month: string) {
  const [year, monthNumber] = month.split("-").map(Number);
  const startDate = `${month}-01`;
  const endDate = new Date(year, monthNumber, 0).toISOString().slice(0, 10);
  return { startDate, endDate };
}

export default function Budgets() {
  const [month, setMonth] = useState(new Date().toISOString().slice(0, 7));
  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [usage, setUsage] = useState<BudgetUsageItem[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [categoryId, setCategoryId] = useState("");
  const [amount, setAmount] = useState("");
  const [startDate, setStartDate] = useState(monthRange(month).startDate);
  const [endDate, setEndDate] = useState(monthRange(month).endDate);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [error, setError] = useState("");

  const expenseCategories = useMemo(
    () => categories.filter((category) => category.type === "EXPENSE"),
    [categories],
  );

  async function load() {
    setError("");
    try {
      const [budgetRes, categoryRes] = await Promise.all([
        api.get<Budget[]>("/api/budgets"),
        api.get<Category[]>("/api/categories"),
      ]);
      const usageRes = await Promise.all(
        budgetRes.data.map((budget) => api.get<BudgetUsageItem>(`/api/budgets/${budget.id}/usage`)),
      );
      setBudgets(budgetRes.data);
      setUsage(usageRes.map((response) => response.data));
      setCategories(categoryRes.data);
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  useEffect(() => {
    void load();
  }, []);

  useEffect(() => {
    if (!editingId) {
      const range = monthRange(month);
      setStartDate(range.startDate);
      setEndDate(range.endDate);
    }
  }, [month, editingId]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    try {
      const payload = { categoryId: Number(categoryId), amount, startDate, endDate };
      if (editingId) {
        await api.put(`/api/budgets/${editingId}`, payload);
      } else {
        await api.post("/api/budgets", payload);
      }
      setCategoryId("");
      setAmount("");
      const range = monthRange(month);
      setStartDate(range.startDate);
      setEndDate(range.endDate);
      setEditingId(null);
      await load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  async function remove(id: number) {
    await api.delete(`/api/budgets/${id}`);
    await load();
  }

  return (
    <section className="space-y-6">
      <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
        <div>
          <h2 className="text-2xl font-bold">Budgets</h2>
          <p className="text-sm text-stone-500">Track limits and usage across each budget date range.</p>
        </div>
        <label className="w-full text-sm font-medium md:w-48">
          Quick month
          <input className="mt-1" type="month" value={month} onChange={(event) => setMonth(event.target.value)} />
        </label>
      </div>
      {error && <p className="rounded-md bg-coral/10 p-3 text-sm text-coral">{error}</p>}
      <form className="panel grid gap-3 md:grid-cols-5" onSubmit={submit}>
        <select value={categoryId} onChange={(event) => setCategoryId(event.target.value)}>
          <option value="">Expense category</option>
          {expenseCategories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
        <input placeholder="Amount" value={amount} onChange={(event) => setAmount(event.target.value)} />
        <input type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} />
        <input type="date" value={endDate} onChange={(event) => setEndDate(event.target.value)} />
        <button className="btn-primary" type="submit">
          {editingId ? "Update budget" : "Create budget"}
        </button>
        {editingId && (
          <button className="btn-secondary" type="button" onClick={() => setEditingId(null)}>
            Cancel
          </button>
        )}
      </form>
      <div className="grid gap-4 lg:grid-cols-2">
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Category</th>
                <th>Amount</th>
                <th>Dates</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-100">
              {budgets.map((budget) => (
                <tr key={budget.id}>
                  <td>{budget.categoryName}</td>
                  <td>{budget.amount}</td>
                  <td>
                    {budget.startDate} to {budget.endDate}
                  </td>
                  <td className="space-x-2">
                    <button
                      className="btn-secondary"
                      type="button"
                      onClick={() => {
                        setEditingId(budget.id);
                        setCategoryId(String(budget.categoryId));
                        setAmount(budget.amount);
                        setStartDate(budget.startDate);
                        setEndDate(budget.endDate);
                      }}
                    >
                      Edit
                    </button>
                    <button className="btn-danger" type="button" onClick={() => void remove(budget.id)}>
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Category</th>
                <th>Spent</th>
                <th>Usage</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-100">
              {usage.map((item) => (
                <tr key={item.budgetId}>
                  <td>{item.categoryName}</td>
                  <td>
                    {item.spentAmount} / {item.limitAmount}
                  </td>
                  <td>{item.usagePercentage}%</td>
                  <td>
                    <StatusBadge status={item.status} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
}
