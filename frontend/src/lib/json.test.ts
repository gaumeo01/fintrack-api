import { expect, it } from "vitest";
import { parseApiJson } from "./json";
it("preserves maximum money and decimal precision with numeric IDs", () => {
 expect(parseApiJson('{"amount":99999999999999999.99,"id":42,"total":0.10}'))
   .toEqual({amount: "99999999999999999.99", id: 42, total: "0.10"});
});
