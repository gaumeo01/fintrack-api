import { FormEvent, useEffect, useMemo, useState } from "react";
import { api, errorMessage } from "../api/client";
import StatusBadge from "../components/StatusBadge";
import type { Budget, BudgetUsageItem, Category } from "../types/api";

function currentMonth() {
  return new Date().toISOString().slice(0, 7);
}

export default function Budgets() {
  const [month, setMonth] = useState(currentMonth());
  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [usage, setUsage] = useState<BudgetUsageItem[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [categoryId, setCategoryId] = useState("");
  const [amount, setAmount] = useState("");
  const [editingId, setEditingId] = useState<number | null>(null);
  const [error, setError] = useState("");

  const expenseCategories = useMemo(
    () => categories.filter((category) => category.type === "EXPENSE"),
    [categories],
  );

  async function load() {
    setError("");
    try {
      const [budgetRes, usageRes, categoryRes] = await Promise.all([
        api.get<Budget[]>("/api/budgets", { params: { month } }),
        api.get<{ items: BudgetUsageItem[] }>("/api/budgets/usage", { params: { month } }),
        api.get<Category[]>("/api/categories"),
      ]);
      setBudgets(budgetRes.data);
      setUsage(usageRes.data.items);
      setCategories(categoryRes.data);
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  useEffect(() => {
    void load();
  }, [month]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    try {
      const payload = { categoryId: Number(categoryId), amount, month };
      if (editingId) {
        await api.put(`/api/budgets/${editingId}`, payload);
      } else {
        await api.post("/api/budgets", payload);
      }
      setCategoryId("");
      setAmount("");
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
          <p className="text-sm text-stone-500">Track monthly limits and usage status.</p>
        </div>
        <label className="w-full text-sm font-medium md:w-48">
          Month
          <input className="mt-1" type="month" value={month} onChange={(event) => setMonth(event.target.value)} />
        </label>
      </div>
      {error && <p className="rounded-md bg-coral/10 p-3 text-sm text-coral">{error}</p>}
      <form className="panel grid gap-3 md:grid-cols-4" onSubmit={submit}>
        <select value={categoryId} onChange={(event) => setCategoryId(event.target.value)}>
          <option value="">Expense category</option>
          {expenseCategories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
        <input placeholder="Amount" value={amount} onChange={(event) => setAmount(event.target.value)} />
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
                <th>Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-100">
              {budgets.map((budget) => (
                <tr key={budget.id}>
                  <td>{budget.categoryName}</td>
                  <td>{budget.amount}</td>
                  <td className="space-x-2">
                    <button
                      className="btn-secondary"
                      type="button"
                      onClick={() => {
                        setEditingId(budget.id);
                        setCategoryId(String(budget.categoryId));
                        setAmount(budget.amount);
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
                <tr key={item.categoryId}>
                  <td>{item.categoryName}</td>
                  <td>{item.spentAmount}</td>
                  <td>{item.usagePercent}%</td>
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
