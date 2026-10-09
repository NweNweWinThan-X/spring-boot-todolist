import { Search } from 'lucide-react';
import type { TaskPriority, TaskStatus } from '../types';

export type StatusFilter = TaskStatus | 'ALL';

const STATUS_TABS: ReadonlyArray<{ value: StatusFilter; label: string }> = [
  { value: 'ALL', label: 'All' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'IN_PROGRESS', label: 'In progress' },
  { value: 'COMPLETED', label: 'Completed' },
];

const PRIORITY_OPTIONS: ReadonlyArray<{ value: TaskPriority | 'ALL'; label: string }> = [
  { value: 'ALL', label: 'All priorities' },
  { value: 'HIGH', label: 'High' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'LOW', label: 'Low' },
];

interface FilterBarProps {
  search: string;
  onSearchChange: (value: string) => void;
  status: StatusFilter;
  onStatusChange: (value: StatusFilter) => void;
  priority: TaskPriority | 'ALL';
  onPriorityChange: (value: TaskPriority | 'ALL') => void;
}

export function FilterBar({
  search,
  onSearchChange,
  status,
  onStatusChange,
  priority,
  onPriorityChange,
}: FilterBarProps) {
  return (
    <section className="space-y-3" aria-label="Filters">
      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search
            className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400"
            aria-hidden
          />
          <input
            type="search"
            data-testid="search"
            value={search}
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder="Search tasks"
            aria-label="Search tasks"
            className="w-full rounded-lg border border-slate-200 bg-white py-2 pr-3 pl-9 text-sm outline-none transition focus:border-violet-400 focus:ring-2 focus:ring-violet-100 dark:border-slate-700 dark:bg-slate-900 dark:focus:ring-violet-900/40"
          />
        </div>

        <select
          value={priority}
          onChange={(event) => onPriorityChange(event.target.value as TaskPriority | 'ALL')}
          aria-label="Filter by priority"
          className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none transition focus:border-violet-400 dark:border-slate-700 dark:bg-slate-900"
        >
          {PRIORITY_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      <div role="tablist" aria-label="Status" className="flex flex-wrap gap-1">
        {STATUS_TABS.map((tab) => {
          const active = tab.value === status;
          return (
            <button
              key={tab.value}
              type="button"
              role="tab"
              aria-selected={active}
              onClick={() => onStatusChange(tab.value)}
              className={[
                'rounded-full px-3.5 py-1.5 text-sm font-medium transition',
                active
                  ? 'bg-violet-500 text-white shadow-sm'
                  : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800',
              ].join(' ')}
            >
              {tab.label}
            </button>
          );
        })}
      </div>
    </section>
  );
}
