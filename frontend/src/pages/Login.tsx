import { FormEvent, useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { errorMessage } from "../api/client";
import { useAuth } from "../context/AuthContext";

export default function Login() {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
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
      await login(email, password);
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
          <h1 className="text-2xl font-bold">Sign in</h1>
          <p className="text-sm text-stone-500">Use your Finance Tracker account.</p>
        </div>
        {error && <p className="rounded-md bg-coral/10 p-3 text-sm text-coral">{error}</p>}
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
          {loading ? "Signing in..." : "Sign in"}
        </button>
        <p className="text-center text-sm text-stone-500">
          No account?{" "}
          <Link className="font-semibold text-mint" to="/register">
            Register
          </Link>
        </p>
      </form>
    </main>
  );
}
