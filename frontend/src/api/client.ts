import { parseApiJson } from "../lib/json";
import { handleUnauthorized } from "../lib/session";
import axios from "axios";

export const api = axios.create({
  transformResponse: [(data) => typeof data === "string" && data.trim() ? parseApiJson(data) : data],
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    handleUnauthorized(error.response?.status, error.response?.data?.code, error.config?.headers?.Authorization);
    return Promise.reject(error);
  },
);

export function errorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    return (
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      "Request failed"
    );
  }
  return "Request failed";
}
