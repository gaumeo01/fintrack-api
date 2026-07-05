import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const links = [
  ["Dashboard", "/dashboard"],
  ["Transactions", "/transactions"],
  ["Categories", "/categories"],
  ["Budgets", "/budgets"],
  ["Monthly Report", "/reports/monthly"],
];

export default function AppLayout() {
  const { user, logout } = useAuth();

  return (
    <div className="min-h-screen">
      <header className="border-b border-stone-200 bg-white">
        <div className="mx-auto flex max-w-7xl flex-col gap-3 px-4 py-4 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 className="text-xl font-bold">Finance Tracker</h1>
            <p className="text-sm text-stone-500">{user?.email}</p>
          </div>
          <nav className="flex flex-wrap gap-2">
            {links.map(([label, to]) => (
              <NavLink
                key={to}
                to={to}
                className={({ isActive }) =>
                  `rounded-md px-3 py-2 text-sm font-medium ${
                    isActive ? "bg-mint text-white" : "text-stone-700 hover:bg-stone-100"
                  }`
                }
              >
                {label}
              </NavLink>
            ))}
            <button className="btn-secondary" type="button" onClick={logout}>
              Sign out
            </button>
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-7xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  );
}
