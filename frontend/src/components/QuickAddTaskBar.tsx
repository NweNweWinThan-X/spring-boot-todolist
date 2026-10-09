import { useState } from 'react';
import { Plus } from 'lucide-react';
import type { TaskPriority, TaskRequest } from '../types';

interface QuickAddTaskBarProps {
  onAdd: (request: TaskRequest) => Promise<void>;
}

export function QuickAddTaskBar({ onAdd }: QuickAddTaskBarProps) {
  const [title, setTitle] = useState('');
  const [priority, setPriority] = useState<TaskPriority>('MEDIUM');
  const [submitting, setSubmitting] = useState(false);

  const trimmed = title.trim();

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!trimmed || submitting) {
      return;
    }
    setSubmitting(true);
    try {
      await onAdd({ title: trimmed, status: 'PENDING', priority, description: null, dueDate: null });
      setTitle('');
      setPriority('MEDIUM');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="flex flex-col gap-2 rounded-xl border border-slate-200 bg-white p-2 shadow-sm sm:flex-row sm:items-center dark:border-slate-800 dark:bg-slate-900"
    >
      <input
        value={title}
        onChange={(event) => setTitle(event.target.value)}
        placeholder="Add a task"
        data-testid="quick-add-input"
        aria-label="New task title"
        maxLength={200}
        className="flex-1 rounded-lg bg-transparent px-3 py-2 text-sm outline-none placeholder:text-slate-400"
      />
      <select
        value={priority}
        onChange={(event) => setPriority(event.target.value as TaskPriority)}
        aria-label="Priority"
        className="rounded-lg border border-slate-200 bg-white px-2.5 py-2 text-sm outline-none dark:border-slate-700 dark:bg-slate-900"
      >
        <option value="HIGH">High</option>
        <option value="MEDIUM">Medium</option>
        <option value="LOW">Low</option>
      </select>
      <button
        type="submit"
        data-testid="quick-add-submit"
        disabled={!trimmed || submitting}
        className="inline-flex items-center justify-center gap-1.5 rounded-lg accent-gradient px-4 py-2 text-sm font-medium text-white transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
      >
        <Plus className="size-4" aria-hidden />
        Add
      </button>
    </form>
  );
}
