import type { EisenhowerQuadrant, Task } from '../types';

const QUADRANTS: ReadonlyArray<{
  key: EisenhowerQuadrant;
  title: string;
  hint: string;
  accent: string;
}> = [
  { key: 'DO', title: 'Do', hint: 'Urgent · Important', accent: 'border-rose-400/60' },
  {
    key: 'SCHEDULE',
    title: 'Schedule',
    hint: 'Not urgent · Important',
    accent: 'border-violet-400/60',
  },
  {
    key: 'DELEGATE',
    title: 'Delegate',
    hint: 'Urgent · Not important',
    accent: 'border-amber-400/60',
  },
  {
    key: 'ELIMINATE',
    title: 'Eliminate',
    hint: 'Neither',
    accent: 'border-slate-400/40',
  },
];

export function MatrixView({ tasks }: { tasks: Task[] }) {
  return (
    <section aria-label="Eisenhower matrix" className="grid gap-3 sm:grid-cols-2">
      {QUADRANTS.map((quadrant) => {
        const inQuadrant = tasks.filter((task) => task.quadrant === quadrant.key);
        return (
          <div
            key={quadrant.key}
            data-testid={`quadrant-${quadrant.key}`}
            className={`rounded-xl border-2 border-dashed bg-white p-4 dark:bg-slate-900 ${quadrant.accent}`}
          >
            <div className="flex items-baseline justify-between">
              <h3 className="text-sm font-semibold">{quadrant.title}</h3>
              <span className="text-xs text-slate-400 tabular-nums">{inQuadrant.length}</span>
            </div>
            <p className="mt-0.5 text-xs text-slate-400">{quadrant.hint}</p>
            <ul className="mt-3 space-y-1.5">
              {inQuadrant.length === 0 && (
                <li className="text-xs text-slate-400">Empty</li>
              )}
              {inQuadrant.map((task) => (
                <li
                  key={task.id}
                  className="truncate rounded-lg bg-slate-100 px-2.5 py-1.5 text-xs dark:bg-slate-800"
                >
                  {task.title}
                </li>
              ))}
            </ul>
          </div>
        );
      })}
    </section>
  );
}
