import type { BudgetUsageStatus } from "../types/api";

const styles: Record<BudgetUsageStatus, string> = {
  SAFE: "bg-mint/10 text-mint",
  WARNING: "bg-amber/10 text-amber",
  OVER_BUDGET: "bg-coral/10 text-coral",
};

export default function StatusBadge({ status }: { status: BudgetUsageStatus }) {
  return (
    <span className={`inline-flex rounded-full px-2 py-1 text-xs font-semibold ${styles[status]}`}>
      {status.replace("_", " ")}
    </span>
  );
}
