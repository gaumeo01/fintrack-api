import { FormEvent, useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { errorMessage } from "../api/client";
import { useAuth } from "../context/AuthContext";

export default function Register() {
  const { register, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError("");
    try {
      await register(fullName, email, password);
      navigate("/dashboard");
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center px-4">
      <form className="panel w-full max-w-md space-y-4" onSubmit={submit}>
        <div>
          <h1 className="text-2xl font-bold">Create account</h1>
          <p className="text-sm text-stone-500">Start tracking income, expenses, and budgets.</p>
        </div>
        {error && <p className="rounded-md bg-coral/10 p-3 text-sm text-coral">{error}</p>}
        <label className="block text-sm font-medium">
          Full name
          <input className="mt-1" value={fullName} onChange={(event) => setFullName(event.target.value)} />
        </label>
        <label className="block text-sm font-medium">
          Email
          <input className="mt-1" value={email} onChange={(event) => setEmail(event.target.value)} />
        </label>
        <label className="block text-sm font-medium">
          Password
          <input
            className="mt-1"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </label>
        <button className="btn-primary w-full" disabled={loading} type="submit">
          {loading ? "Creating..." : "Create account"}
        </button>
        <p className="text-center text-sm text-stone-500">
          Already registered?{" "}
          <Link className="font-semibold text-mint" to="/login">
            Sign in
          </Link>
        </p>
      </form>
    </main>
  );
}
