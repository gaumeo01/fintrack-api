// @vitest-environment jsdom
import React from "react";
import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { AuthProvider, useAuth } from "./AuthContext";
import { handleUnauthorized } from "../lib/session";
vi.mock("../api/client", () => ({ api: { post: vi.fn() } }));
function State() {
  const auth = useAuth();
  return <><span>{auth.isAuthenticated ? auth.user?.email : "signed out"}</span><button onClick={auth.logout}>Logout</button></>;
}
function mount() {
  localStorage.setItem("accessToken", "token");
  localStorage.setItem("user", JSON.stringify({ email: "test@example.com" }));
  render(<AuthProvider><State /></AuthProvider>);
}
afterEach(() => { cleanup(); localStorage.clear(); });
it.each(["expired", "malformed"])("clears UI and storage for %s JWT", () => {
  mount(); act(() => handleUnauthorized(401, undefined, "Bearer token"));
  expect(screen.getByText("signed out")).toBeTruthy();
  expect(localStorage.length).toBe(0);
});
it("keeps session for invalid current password and ignores stale failures", () => {
  mount();
  act(() => handleUnauthorized(401, "INVALID_CURRENT_PASSWORD", "Bearer token"));
  act(() => handleUnauthorized(401, undefined, "Bearer old-token"));
  expect(screen.getByText("test@example.com")).toBeTruthy();
});
it("logout clears UI and storage", () => {
  mount(); fireEvent.click(screen.getByText("Logout"));
  expect(screen.getByText("signed out")).toBeTruthy(); expect(localStorage.length).toBe(0);
});
it("handles missing token and corrupt user storage", () => {
  localStorage.setItem("user", "broken"); render(<AuthProvider><State /></AuthProvider>);
  expect(screen.getByText("signed out")).toBeTruthy();
});

it("synchronizes logout from another tab and removes stale user data", () => {
  mount();
  act(() => { localStorage.removeItem("accessToken"); window.dispatchEvent(new Event("storage")); });
  expect(screen.getByText("signed out")).toBeTruthy(); expect(localStorage.getItem("user")).toBeNull();
});
