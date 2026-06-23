export default function StatCard({
  label,
  value,
  tone = "neutral",
}: {
  label: string;
  value: string | number;
  tone?: "neutral" | "income" | "expense";
}) {
  const color = tone === "income" ? "text-mint" : tone === "expense" ? "text-coral" : "text-ink";
  return (
    <div className="panel">
      <p className="text-sm text-stone-500">{label}</p>
      <p className={`mt-2 text-2xl font-bold ${color}`}>{value}</p>
    </div>
  );
}
