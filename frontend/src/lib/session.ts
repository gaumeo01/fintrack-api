export const SESSION_CHANGED = "finance-session-changed";
export function clearSession() {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("user");
  window.dispatchEvent(new Event(SESSION_CHANGED));
}
export function handleUnauthorized(status: number | undefined, code: string | undefined, requestToken: string | undefined) {
  if (status === 401 && code !== "INVALID_CURRENT_PASSWORD" &&
      requestToken === `Bearer ${localStorage.getItem("accessToken")}`) clearSession();
}
