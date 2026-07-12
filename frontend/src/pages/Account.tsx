import { FormEvent, useState } from "react";
import { api, errorMessage } from "../api/client";

export default function Account() {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  async function submit(event: FormEvent) {
    event.preventDefault();
    setMessage("");
    setError("");

    if (newPassword !== confirmPassword) {
      setError("New passwords do not match");
      return;
    }

    try {
      await api.put("/api/auth/change-password", { currentPassword, newPassword });
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setMessage("Password changed successfully.");
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <section className="mx-auto max-w-xl space-y-6">
      <div>
        <h2 className="text-2xl font-bold">Account</h2>
        <p className="text-sm text-stone-500">Update your password.</p>
      </div>
      {error && <p className="rounded-md bg-coral/10 p-3 text-sm text-coral">{error}</p>}
      {message && <p className="rounded-md bg-mint/10 p-3 text-sm text-mint">{message}</p>}
      <form className="panel space-y-3" onSubmit={submit}>
        <input
          type="password"
          placeholder="Current password"
          value={currentPassword}
          onChange={(event) => setCurrentPassword(event.target.value)}
        />
        <input
          type="password"
          placeholder="New password"
          value={newPassword}
          onChange={(event) => setNewPassword(event.target.value)}
        />
        <input
          type="password"
          placeholder="Confirm new password"
          value={confirmPassword}
          onChange={(event) => setConfirmPassword(event.target.value)}
        />
        <button className="btn-primary" type="submit">
          Change password
        </button>
      </form>
    </section>
  );
}
