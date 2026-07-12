import { FormEvent, useEffect, useMemo, useState } from "react";
import { api, errorMessage } from "../api/client";
import type { Category, RecurringFrequency, RecurringTransaction, TransactionType } from "../types/api";

const frequencies: RecurringFrequency[] = ["DAILY", "WEEKLY", "MONTHLY", "YEARLY"];

const initialForm = {
  type: "EXPENSE" as TransactionType,
  amount: "",
  categoryId: "",
  description: "",
  frequency: "MONTHLY" as RecurringFrequency,
  startDate: new Date().toISOString().slice(0, 10),
  endDate: "",
  active: true,
};

export default function RecurringTransactions() {
  const [items, setItems] = useState<RecurringTransaction[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [form, setForm] = useState(initialForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const matchingCategories = useMemo(
    () => categories.filter((category) => category.type === form.type),
    [categories, form.type],
  );

  async function load() {
    setError("");
    try {
      const [recurringRes, categoryRes] = await Promise.all([
        api.get<RecurringTransaction[]>("/api/recurring-transactions"),
        api.get<Category[]>("/api/categories"),
      ]);
      setItems(recurringRes.data);
      setCategories(categoryRes.data);
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    setMessage("");
    const payload = {
      ...form,
      amount: form.amount,
      categoryId: Number(form.categoryId),
      endDate: form.endDate || null,
    };
    try {
      if (editingId) {
        await api.put(`/api/recurring-transactions/${editingId}`, payload);
      } else {
        await api.post("/api/recurring-transactions", payload);
      }
      setForm(initialForm);
      setEditingId(null);
      await load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  async function remove(id: number) {
    if (!confirm("Delete this recurring transaction?")) {
      return;
    }
    await api.delete(`/api/recurring-transactions/${id}`);
    await load();
  }

  async function generate(id: number) {
    if (!confirm("Generate the next due transaction?")) {
      return;
    }
    setError("");
    setMessage("");
    try {
      const response = await api.post<{ nextRunDate: string }>(`/api/recurring-transactions/${id}/generate`);
      setMessage(`Generated transaction. Next run date: ${response.data.nextRunDate}`);
      await load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <section className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold">Recurring Transactions</h2>
        <p className="text-sm text-stone-500">Manage repeating income and expense schedules.</p>
      </div>
      {error && <p className="rounded-md bg-coral/10 p-3 text-sm text-coral">{error}</p>}
      {message && <p className="rounded-md bg-mint/10 p-3 text-sm text-mint">{message}</p>}
      <form className="panel grid gap-3 md:grid-cols-4" onSubmit={submit}>
        <select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value as TransactionType, categoryId: "" })}>
          <option value="EXPENSE">Expense</option>
          <option value="INCOME">Income</option>
        </select>
        <input placeholder="Amount" value={form.amount} onChange={(event) => setForm({ ...form, amount: event.target.value })} />
        <select value={form.categoryId} onChange={(event) => setForm({ ...form, categoryId: event.target.value })}>
          <option value="">Category</option>
          {matchingCategories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
        <select value={form.frequency} onChange={(event) => setForm({ ...form, frequency: event.target.value as RecurringFrequency })}>
          {frequencies.map((frequency) => (
            <option key={frequency} value={frequency}>
              {frequency}
            </option>
          ))}
        </select>
        <input type="date" value={form.startDate} onChange={(event) => setForm({ ...form, startDate: event.target.value })} />
        <input type="date" value={form.endDate} onChange={(event) => setForm({ ...form, endDate: event.target.value })} />
        <input placeholder="Description" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} />
        <label className="flex items-center gap-2 text-sm">
          <input className="w-auto" type="checkbox" checked={form.active} onChange={(event) => setForm({ ...form, active: event.target.checked })} />
          Active
        </label>
        <button className="btn-primary" type="submit">
          {editingId ? "Update schedule" : "Create schedule"}
        </button>
      </form>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Type</th>
              <th>Category</th>
              <th>Amount</th>
              <th>Frequency</th>
              <th>Next run</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-stone-100">
            {items.map((item) => (
              <tr key={item.id}>
                <td>{item.type}</td>
                <td>{item.categoryName}</td>
                <td>{item.amount}</td>
                <td>{item.frequency}</td>
                <td>{item.nextRunDate}</td>
                <td className="space-x-2">
                  <button className="btn-secondary" type="button" onClick={() => generate(item.id)}>
                    Generate
                  </button>
                  <button
                    className="btn-secondary"
                    type="button"
                    onClick={() => {
                      setEditingId(item.id);
                      setForm({
                        type: item.type,
                        amount: item.amount,
                        categoryId: String(item.categoryId),
                        description: item.description || "",
                        frequency: item.frequency,
                        startDate: item.startDate,
                        endDate: item.endDate || "",
                        active: item.active,
                      });
                    }}
                  >
                    Edit
                  </button>
                  <button className="btn-danger" type="button" onClick={() => remove(item.id)}>
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
