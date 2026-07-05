import { FormEvent, useEffect, useMemo, useState } from "react";
import { api, errorMessage } from "../api/client";
import type { Category, ImportResult, PageResponse, Transaction, TransactionType } from "../types/api";

const emptyForm = {
  type: "EXPENSE" as TransactionType,
  amount: "",
  categoryId: "",
  description: "",
  transactionDate: new Date().toISOString().slice(0, 10),
};

export default function Transactions() {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [filters, setFilters] = useState({
    type: "",
    categoryId: "",
    fromDate: "",
    toDate: "",
    minAmount: "",
    maxAmount: "",
    keyword: "",
  });
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [importResult, setImportResult] = useState<ImportResult | null>(null);

  const filteredCategories = useMemo(
    () => categories.filter((category) => category.type === form.type),
    [categories, form.type],
  );

  async function loadTransactions(nextPage = page) {
    setError("");
    try {
      const params = Object.fromEntries(
        Object.entries({ ...filters, page: nextPage, size: 20, sort: "transactionDate,desc" }).filter(
          ([, value]) => value !== "",
        ),
      );
      const { data } = await api.get<PageResponse<Transaction>>("/api/transactions", { params });
      setTransactions(data.content);
      setPage(data.page);
      setTotalPages(data.totalPages);
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  async function loadCategories() {
    const { data } = await api.get<Category[]>("/api/categories");
    setCategories(data);
  }

  useEffect(() => {
    void loadCategories();
    void loadTransactions(0);
  }, []);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    setMessage("");
    try {
      const payload = {
        type: form.type,
        amount: form.amount,
        categoryId: Number(form.categoryId),
        description: form.description || null,
        transactionDate: form.transactionDate,
      };
      if (editingId) {
        await api.put(`/api/transactions/${editingId}`, payload);
        setMessage("Transaction updated.");
      } else {
        await api.post("/api/transactions", payload);
        setMessage("Transaction created.");
      }
      setForm(emptyForm);
      setEditingId(null);
      await loadTransactions(0);
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  function edit(transaction: Transaction) {
    setEditingId(transaction.id);
    setForm({
      type: transaction.type,
      amount: transaction.amount,
      categoryId: String(transaction.categoryId),
      description: transaction.description ?? "",
      transactionDate: transaction.transactionDate,
    });
  }

  async function remove(id: number) {
    await api.delete(`/api/transactions/${id}`);
    await loadTransactions(page);
  }

  async function exportCsv() {
    const params = Object.fromEntries(Object.entries(filters).filter(([, value]) => value !== ""));
    const response = await api.get("/api/transactions/export", { params, responseType: "blob" });
    const url = URL.createObjectURL(response.data);
    const link = document.createElement("a");
    link.href = url;
    link.download = "transactions-export.csv";
    link.click();
    URL.revokeObjectURL(url);
  }

  async function importCsv(file: File | null) {
    if (!file) {
      return;
    }
    setError("");
    const body = new FormData();
    body.append("file", file);
    try {
      const { data } = await api.post<ImportResult>("/api/transactions/import", body);
      setImportResult(data);
      await loadTransactions(0);
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <section className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold">Transactions</h2>
        <p className="text-sm text-stone-500">Create, filter, search, import, and export transactions.</p>
      </div>
      {error && <p className="rounded-md bg-coral/10 p-3 text-sm text-coral">{error}</p>}
      {message && <p className="rounded-md bg-mint/10 p-3 text-sm text-mint">{message}</p>}
      <form className="panel grid gap-3 md:grid-cols-6" onSubmit={submit}>
        <select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value as TransactionType })}>
          <option value="EXPENSE">Expense</option>
          <option value="INCOME">Income</option>
        </select>
        <input placeholder="Amount" value={form.amount} onChange={(event) => setForm({ ...form, amount: event.target.value })} />
        <select value={form.categoryId} onChange={(event) => setForm({ ...form, categoryId: event.target.value })}>
          <option value="">Category</option>
          {filteredCategories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
        <input type="date" value={form.transactionDate} onChange={(event) => setForm({ ...form, transactionDate: event.target.value })} />
        <input placeholder="Description" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} />
        <button className="btn-primary" type="submit">
          {editingId ? "Update" : "Create"}
        </button>
      </form>
      <div className="panel grid gap-3 md:grid-cols-8">
        <select value={filters.type} onChange={(event) => setFilters({ ...filters, type: event.target.value })}>
          <option value="">All types</option>
          <option value="EXPENSE">Expense</option>
          <option value="INCOME">Income</option>
        </select>
        <select value={filters.categoryId} onChange={(event) => setFilters({ ...filters, categoryId: event.target.value })}>
          <option value="">All categories</option>
          {categories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
        <input type="date" value={filters.fromDate} onChange={(event) => setFilters({ ...filters, fromDate: event.target.value })} />
        <input type="date" value={filters.toDate} onChange={(event) => setFilters({ ...filters, toDate: event.target.value })} />
        <input placeholder="Min" value={filters.minAmount} onChange={(event) => setFilters({ ...filters, minAmount: event.target.value })} />
        <input placeholder="Max" value={filters.maxAmount} onChange={(event) => setFilters({ ...filters, maxAmount: event.target.value })} />
        <input placeholder="Keyword" value={filters.keyword} onChange={(event) => setFilters({ ...filters, keyword: event.target.value })} />
        <button className="btn-secondary" type="button" onClick={() => void loadTransactions(0)}>
          Filter
        </button>
      </div>
      <div className="flex flex-col gap-3 md:flex-row">
        <button className="btn-secondary" type="button" onClick={() => void exportCsv()}>
          Export CSV
        </button>
        <label className="btn-secondary inline-flex cursor-pointer">
          Import CSV
          <input className="hidden" type="file" accept=".csv,text/csv" onChange={(event) => void importCsv(event.target.files?.[0] ?? null)} />
        </label>
      </div>
      {importResult && (
        <div className="panel text-sm">
          Imported {importResult.successfulRows} of {importResult.totalRows} rows.
          {importResult.errors.map((item) => (
            <p key={`${item.rowNumber}-${item.message}`} className="text-coral">
              Row {item.rowNumber}: {item.message}
            </p>
          ))}
        </div>
      )}
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Type</th>
              <th>Category</th>
              <th>Amount</th>
              <th>Description</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-stone-100">
            {transactions.map((transaction) => (
              <tr key={transaction.id}>
                <td>{transaction.transactionDate}</td>
                <td>{transaction.type}</td>
                <td>{transaction.categoryName}</td>
                <td>{transaction.amount}</td>
                <td>{transaction.description}</td>
                <td className="space-x-2">
                  <button className="btn-secondary" type="button" onClick={() => edit(transaction)}>
                    Edit
                  </button>
                  <button className="btn-danger" type="button" onClick={() => void remove(transaction.id)}>
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="flex items-center justify-end gap-2">
        <button className="btn-secondary" disabled={page === 0} type="button" onClick={() => void loadTransactions(page - 1)}>
          Previous
        </button>
        <span className="text-sm">
          Page {page + 1} of {Math.max(totalPages, 1)}
        </span>
        <button className="btn-secondary" disabled={page + 1 >= totalPages} type="button" onClick={() => void loadTransactions(page + 1)}>
          Next
        </button>
      </div>
    </section>
  );
}
