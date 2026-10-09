import type { DailyProgress } from '../types';

export function ProgressCard({ progress }: { progress: DailyProgress | null }) {
  if (!progress) {
    return (
      <div className="h-24 animate-pulse rounded-xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900" />
    );
  }

  const { completed, total, percentage } = progress;
  const message =
    total === 0
      ? 'Nothing due today.'
      : percentage === 100
        ? 'All done for today.'
        : 'You are almost done, go ahead.';

  return (
    <section
      aria-label="Daily progress"
      className="rounded-xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900"
    >
      <div className="flex items-baseline justify-between gap-3">
        <div className="min-w-0">
          <h2 className="text-sm font-semibold">Daily task</h2>
          <p className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">
            {completed}/{total} completed
          </p>
        </div>
        <span className="text-lg font-semibold tabular-nums">{percentage}%</span>
      </div>

      <p className="mt-1 text-xs text-slate-400">{message}</p>

      <div
        className="mt-3 h-2 overflow-hidden rounded-full bg-slate-200 dark:bg-slate-800"
        role="progressbar"
        aria-valuenow={percentage}
        aria-valuemin={0}
        aria-valuemax={100}
      >
        <div
          className="accent-gradient h-full rounded-full transition-[width] duration-500"
          style={{ width: `${percentage}%` }}
        />
      </div>
    </section>
  );
}
