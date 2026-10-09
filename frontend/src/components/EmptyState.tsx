import { ClipboardList } from 'lucide-react';

export function EmptyState({ filtered }: { filtered: boolean }) {
  return (
    <div className="rounded-xl border border-dashed border-slate-300 py-16 text-center dark:border-slate-700">
      <ClipboardList className="mx-auto size-10 text-slate-300 dark:text-slate-600" aria-hidden />
      <p className="mt-3 text-sm font-medium">
        {filtered ? 'No tasks match these filters' : 'No tasks yet'}
      </p>
      <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
        {filtered ? 'Try changing the filters.' : 'Add your first task from the bar above.'}
      </p>
    </div>
  );
}
