import { Bell, CalendarClock, Check, Clock, Hash, Pencil, Trash2 } from 'lucide-react';
import type { Task, TaskPriority } from '../types';

const PRIORITY_STYLES: Record<TaskPriority, { label: string; badge: string; bar: string }> = {
  LOW: {
    label: 'Low',
    badge: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300',
    bar: 'bg-emerald-400',
  },
  MEDIUM: {
    label: 'Medium',
    badge: 'bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300',
    bar: 'bg-amber-400',
  },
  HIGH: {
    label: 'High',
    badge: 'bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300',
    bar: 'bg-rose-400',
  },
};

const STATUS_LABELS: Record<Task['status'], string> = {
  PENDING: 'Pending',
  IN_PROGRESS: 'In progress',
  COMPLETED: 'Completed',
};

function isOverdue(task: Task): boolean {
  if (!task.dueDate || task.status === 'COMPLETED') {
    return false;
  }
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return new Date(task.dueDate) < today;
}

function formatTime(value: string | null): string | null {
  return value ? value.slice(0, 5) : null;
}

interface TaskCardProps {
  task: Task;
  onToggle: (task: Task) => void;
  onEdit: (task: Task) => void;
  onDelete: (task: Task) => void;
}

export function TaskCard({ task, onToggle, onEdit, onDelete }: TaskCardProps) {
  const completed = task.status === 'COMPLETED';
  const overdue = isOverdue(task);
  const priority = PRIORITY_STYLES[task.priority];
  const start = formatTime(task.startTime);
  const end = formatTime(task.endTime);

  return (
    <li
      data-testid="task-card"
      data-task-id={task.id}
      className="group relative flex items-start gap-3 overflow-hidden rounded-xl border border-slate-200 bg-white p-4 pl-5 shadow-sm transition duration-200 hover:shadow-md dark:border-slate-800 dark:bg-slate-900"
    >
      <span className={`absolute inset-y-0 left-0 w-1 ${priority.bar}`} aria-hidden />

      <button
        type="button"
        onClick={() => onToggle(task)}
        role="checkbox"
        aria-checked={completed}
        aria-label={completed ? `Mark ${task.title} as pending` : `Complete ${task.title}`}
        className={[
          'mt-0.5 grid size-5 shrink-0 place-items-center rounded-full border transition',
          completed
            ? 'border-violet-500 bg-violet-500 text-white'
            : 'border-slate-300 hover:border-violet-400 dark:border-slate-600',
        ].join(' ')}
      >
        {completed && <Check className="size-3" aria-hidden />}
      </button>

      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <h3
            className={[
              'text-sm font-medium transition',
              completed ? 'text-slate-400 line-through dark:text-slate-500' : '',
            ].join(' ')}
          >
            {task.title}
          </h3>
          <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${priority.badge}`}>
            {priority.label}
          </span>
          <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600 dark:bg-slate-800 dark:text-slate-300">
            {STATUS_LABELS[task.status]}
          </span>
          {task.projectName && (
            <span className="text-xs text-slate-400">{task.projectName}</span>
          )}
        </div>

        {task.description && (
          <p className="mt-1 line-clamp-2 text-sm text-slate-500 dark:text-slate-400">
            {task.description}
          </p>
        )}

        <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs">
          {task.dueDate && (
            <span
              className={[
                'inline-flex items-center gap-1',
                overdue ? 'font-medium text-rose-500 dark:text-rose-400' : 'text-slate-400',
              ].join(' ')}
            >
              <CalendarClock className="size-3.5" aria-hidden />
              {task.dueDate}
              {overdue && <span>· overdue</span>}
            </span>
          )}
          {start && (
            <span className="inline-flex items-center gap-1 text-slate-400">
              <Clock className="size-3.5" aria-hidden />
              {start}
              {end && `–${end}`}
            </span>
          )}
          {task.alertEnabled && (
            <span className="inline-flex items-center gap-1 text-slate-400">
              <Bell className="size-3.5" aria-hidden />
              Alert
            </span>
          )}
          {task.labels.map((label) => (
            <span
              key={label.id}
              className="inline-flex items-center gap-0.5"
              style={{ color: label.color }}
            >
              <Hash className="size-3" aria-hidden />
              {label.name}
            </span>
          ))}
        </div>
      </div>

      <div className="flex shrink-0 items-center gap-1 opacity-60 transition group-hover:opacity-100">
        <button
          type="button"
          onClick={() => onEdit(task)}
          aria-label={`Edit ${task.title}`}
          className="rounded-lg p-2 text-slate-500 transition hover:bg-slate-100 hover:text-slate-900 dark:hover:bg-slate-800 dark:hover:text-slate-100"
        >
          <Pencil className="size-4" aria-hidden />
        </button>
        <button
          type="button"
          onClick={() => onDelete(task)}
          aria-label={`Delete ${task.title}`}
          className="rounded-lg p-2 text-slate-500 transition hover:bg-rose-50 hover:text-rose-600 dark:hover:bg-rose-950/40 dark:hover:text-rose-400"
        >
          <Trash2 className="size-4" aria-hidden />
        </button>
      </div>
    </li>
  );
}
