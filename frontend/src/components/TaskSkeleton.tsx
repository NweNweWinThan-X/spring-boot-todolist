export function TaskSkeleton({ rows = 4 }: { rows?: number }) {
  return (
    <ul className="space-y-3" aria-hidden>
      {Array.from({ length: rows }, (_, index) => (
        <li
          key={index}
          className="flex items-start gap-3 rounded-xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900"
        >
          <div className="mt-0.5 size-5 shrink-0 animate-pulse rounded-md bg-slate-200 dark:bg-slate-700" />
          <div className="flex-1 space-y-2">
            <div className="h-4 w-1/3 animate-pulse rounded bg-slate-200 dark:bg-slate-700" />
            <div className="h-3 w-2/3 animate-pulse rounded bg-slate-100 dark:bg-slate-800" />
          </div>
        </li>
      ))}
    </ul>
  );
}
