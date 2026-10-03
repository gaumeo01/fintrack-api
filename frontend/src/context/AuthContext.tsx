import { clearSession, SESSION_CHANGED } from "../lib/session";
import type React from "react";
import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { api } from "../api/client";
import type { LoginResponse, User } from "../types/api";

interface AuthContextValue {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (fullName: string, email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function readUser(): User | null {
  const raw = localStorage.getItem("user");
  try {
    const parsed = raw ? JSON.parse(raw) : null;
    return localStorage.getItem("accessToken") && parsed && typeof parsed.email === "string" ? parsed as User : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState(() => {
    if (!readUser()) {
      localStorage.removeItem("accessToken");
      localStorage.removeItem("user");
      return null;
    }
    return localStorage.getItem("accessToken");
  });
  const [user, setUser] = useState<User | null>(() => readUser());

  useEffect(() => {
    const sync = () => {
      const nextUser = readUser();
      if (!nextUser) {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("user");
      }
      setToken(nextUser ? localStorage.getItem("accessToken") : null);
      setUser(nextUser);
    };
    window.addEventListener(SESSION_CHANGED, sync);
    window.addEventListener("storage", sync);
    return () => {
      window.removeEventListener(SESSION_CHANGED, sync);
      window.removeEventListener("storage", sync);
    };
  }, []);

  async function login(email: string, password: string) {
    const { data } = await api.post<LoginResponse>("/api/auth/login", { email, password });
    localStorage.setItem("accessToken", data.accessToken);
    localStorage.setItem("user", JSON.stringify(data.user));
    setToken(data.accessToken);
    setUser(data.user);
  }

  async function register(fullName: string, email: string, password: string) {
    await api.post("/api/auth/register", { fullName, email, password });
    await login(email, password);
  }

  function logout() {
    clearSession();
    setToken(null);
    setUser(null);
  }

  const value = useMemo(
    () => ({
      user,
      token,
      isAuthenticated: Boolean(token),
      login,
      register,
      logout,
    }),
    [token, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return value;
}
