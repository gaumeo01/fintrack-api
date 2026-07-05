import { FormEvent, useEffect, useState } from "react";
import { api, errorMessage } from "../api/client";
import type { Category, TransactionType } from "../types/api";

export default function Categories() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [name, setName] = useState("");
  const [type, setType] = useState<TransactionType>("EXPENSE");
  const [editingId, setEditingId] = useState<number | null>(null);
  const [error, setError] = useState("");

  async function load() {
    const { data } = await api.get<Category[]>("/api/categories");
    setCategories(data);
  }

  useEffect(() => {
    void load();
  }, []);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    try {
      if (editingId) {
        await api.put(`/api/categories/${editingId}`, { name, type });
      } else {
        await api.post("/api/categories", { name, type });
      }
      setName("");
      setType("EXPENSE");
      setEditingId(null);
      await load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  async function remove(id: number) {
    await api.delete(`/api/categories/${id}`);
    await load();
  }

  return (
    <section className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold">Categories</h2>
        <p className="text-sm text-stone-500">Manage income and expense categories.</p>
      </div>
      {error && <p className="rounded-md bg-coral/10 p-3 text-sm text-coral">{error}</p>}
      <form className="panel grid gap-3 md:grid-cols-4" onSubmit={submit}>
        <input placeholder="Category name" value={name} onChange={(event) => setName(event.target.value)} />
        <select value={type} onChange={(event) => setType(event.target.value as TransactionType)}>
          <option value="EXPENSE">Expense</option>
          <option value="INCOME">Income</option>
        </select>
        <button className="btn-primary" type="submit">
          {editingId ? "Update category" : "Create category"}
        </button>
        {editingId && (
          <button className="btn-secondary" type="button" onClick={() => setEditingId(null)}>
            Cancel
          </button>
        )}
      </form>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Type</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-stone-100">
            {categories.map((category) => (
              <tr key={category.id}>
                <td>{category.name}</td>
                <td>{category.type}</td>
                <td className="space-x-2">
                  <button
                    className="btn-secondary"
                    type="button"
                    onClick={() => {
                      setEditingId(category.id);
                      setName(category.name);
                      setType(category.type);
                    }}
                  >
                    Edit
                  </button>
                  <button className="btn-danger" type="button" onClick={() => void remove(category.id)}>
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
