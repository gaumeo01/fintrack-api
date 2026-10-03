import { expect, it } from "vitest";
import { today, monthRange } from "./dates";
it("preserves leap days and calendar boundaries", () => {
  for (const date of ["2024-02-29", "2025-01-01", "2025-12-31"]) {
    const [y, m, d] = date.split("-").map(Number);
    expect(today(new Date(y, m - 1, d, 0, 1))).toBe(date);
  }
  for (const [month, end] of [["2024-02",29],["2100-02",28],["2000-02",29],["2025-12",31],["2025-01",31]])
    expect(monthRange(String(month))).toEqual({startDate: `${month}-01`, endDate: `${month}-${end}`});
  expect(() => monthRange("2025-13")).toThrow();
});
it("uses the local calendar at UTC year boundary", () => {
  const expected: Record<string, string> = {UTC: "2025-12-31", "Asia/Bangkok": "2026-01-01", "America/Los_Angeles": "2025-12-31"};
  if (process.env.TZ && expected[process.env.TZ])
    expect(today(new Date("2025-12-31T18:00:00Z"))).toBe(expected[process.env.TZ]);
});
