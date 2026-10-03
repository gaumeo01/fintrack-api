import { parse } from "lossless-json";
/** Preserve decimal tokens before JavaScript can round them; IDs/counts remain safe integers. */
export function parseApiJson(text: string): unknown {
  try {
    return parse(text, undefined, (token) => {
      if (/[.eE]/.test(token)) return token;
      const value = Number(token);
      return Number.isSafeInteger(value) ? value : token;
    });
  } catch { return text; }
}
