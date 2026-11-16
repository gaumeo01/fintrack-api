import { describe, expect, it } from "vitest";
import { formatVnd } from "./currency";

describe("VND display", () => {
  it("formats amounts and negative balances with Vietnamese grouping", () => {
    expect(formatVnd(65000)).toBe("65.000\u00a0₫");
    expect(formatVnd("1200000.00")).toBe("1.200.000\u00a0₫");
    expect(formatVnd(-235725)).toBe("-235.725\u00a0₫");
    expect(formatVnd(0)).toBe("0\u00a0₫");
  });
  it("rounds display only and preserves large decimal string precision", () => {
    const original = "99999999999999999.49";
    expect(formatVnd(original)).toBe("99.999.999.999.999.999\u00a0₫");
    expect(original).toBe("99999999999999999.49");
    expect(formatVnd("12.50")).toBe("13\u00a0₫");
  });
});
