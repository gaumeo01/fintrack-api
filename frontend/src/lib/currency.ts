const vnd = new Intl.NumberFormat("vi-VN", {
  style: "currency",
  currency: "VND",
  maximumFractionDigits: 0,
});

/** Display only. Pass decimal strings directly to Intl to preserve API precision. */
export function formatVnd(value: string | number): string {
  if (value === "" || (typeof value === "number" && !Number.isFinite(value))) return "—";
  // Modern Intl accepts decimal strings exactly; TypeScript's lib still lists number/bigint.
  return (vnd.format as (value: string | number) => string)(value);
}
