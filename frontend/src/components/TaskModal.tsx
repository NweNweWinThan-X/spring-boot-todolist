import { useEffect, useState } from 'react';
import { X } from 'lucide-react';
import type { Label, Project, Task, TaskPriority, TaskRequest, TaskStatus } from '../types';

interface TaskModalProps {
  open: boolean;
  task: Task | null;
  projects: Project[];
  labels: Label[];
  onClose: () => void;
  onSubmit: (request: TaskRequest) => Promise<void>;
}

interface FormState {
  title: string;
  description: string;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate: string;
  startTime: string;
  endTime: string;
  alertEnabled: boolean;
  urgent: boolean;
  important: boolean;
  projectId: string;
  labelIds: number[];
}

function toForm(task: Task | null): FormState {
  if (!task) {
    return {
      title: '',
      description: '',
      status: 'PENDING',
      priority: 'MEDIUM',
      dueDate: '',
      startTime: '',
      endTime: '',
      alertEnabled: false,
      urgent: false,
      important: false,
      projectId: '',
      labelIds: [],
    };
  }
  return {
    title: task.title,
    description: task.description ?? '',
    status: task.status,
    priority: task.priority,
    dueDate: task.dueDate ?? '',
    startTime: task.startTime?.slice(0, 5) ?? '',
    endTime: task.endTime?.slice(0, 5) ?? '',
    alertEnabled: task.alertEnabled,
    urgent: task.urgent,
    important: task.important,
    projectId: task.projectId ? String(task.projectId) : '',
    labelIds: task.labels.map((l) => l.id),
  };
}

const FIELD =
  'w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none transition focus:border-violet-400 dark:border-slate-700 dark:bg-slate-900';

export function TaskModal({ open, task, projects, labels, onClose, onSubmit }: TaskModalProps) {
  const [form, setForm] = useState<FormState>(() => toForm(task));
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!open) {
      return;
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose();
      }
    }
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [open, onClose]);

  if (!open) {
    return null;
  }

  function toggleLabel(id: number) {
    setForm((current) => ({
      ...current,
      labelIds: current.labelIds.includes(id)
        ? current.labelIds.filter((x) => x !== id)
        : [...current.labelIds, id],
    }));
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (submitting) {
      return;
    }
    setSubmitting(true);
    try {
      await onSubmit({
        title: form.title.trim(),
        description: form.description.trim() || null,
        status: form.status,
        priority: form.priority,
        dueDate: form.dueDate || null,
        startTime: form.startTime || null,
        endTime: form.endTime || null,
        alertEnabled: form.alertEnabled,
        urgent: form.urgent,
        important: form.important,
        projectId: form.projectId ? Number(form.projectId) : null,
        labelIds: form.labelIds,
      });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-30 flex items-end justify-center bg-slate-950/60 p-0 sm:items-center sm:p-4"
      onClick={onClose}
      role="presentation"
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="task-modal-title"
        data-testid="task-modal"
        onClick={(event) => event.stopPropagation()}
        className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-t-2xl bg-white p-5 shadow-xl sm:rounded-2xl dark:bg-slate-900"
      >
        <div className="flex items-center justify-between">
          <h2 id="task-modal-title" className="text-base font-semibold">
            {task ? 'Edit task' : 'Create task'}
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close"
            className="rounded-lg p-1.5 text-slate-500 transition hover:bg-slate-100 dark:hover:bg-slate-800"
          >
            <X className="size-5" aria-hidden />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-4 space-y-3">
          <label className="block space-y-1">
            <span className="text-sm font-medium">Title</span>
            <input
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              required
              maxLength={200}
              className={FIELD}
            />
          </label>

          <label className="block space-y-1">
            <span className="text-sm font-medium">Description</span>
            <textarea
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              rows={3}
              maxLength={2000}
              className={FIELD}
            />
          </label>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <label className="block space-y-1">
              <span className="text-sm font-medium">Status</span>
              <select
                value={form.status}
                onChange={(e) => setForm({ ...form, status: e.target.value as TaskStatus })}
                className={FIELD}
              >
                <option value="PENDING">Pending</option>
                <option value="IN_PROGRESS">In progress</option>
                <option value="COMPLETED">Completed</option>
              </select>
            </label>

            <label className="block space-y-1">
              <span className="text-sm font-medium">Priority</span>
              <select
                value={form.priority}
                onChange={(e) => setForm({ ...form, priority: e.target.value as TaskPriority })}
                className={FIELD}
              >
                <option value="HIGH">High</option>
                <option value="MEDIUM">Medium</option>
                <option value="LOW">Low</option>
              </select>
            </label>

            <label className="block space-y-1">
              <span className="text-sm font-medium">Due date</span>
              <input
                type="date"
                value={form.dueDate}
                onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
                className={FIELD}
              />
            </label>
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <label className="block space-y-1">
              <span className="text-sm font-medium">Start time</span>
              <input
                type="time"
                value={form.startTime}
                onChange={(e) => setForm({ ...form, startTime: e.target.value })}
                className={FIELD}
              />
            </label>
            <label className="block space-y-1">
              <span className="text-sm font-medium">End time</span>
              <input
                type="time"
                value={form.endTime}
                onChange={(e) => setForm({ ...form, endTime: e.target.value })}
                className={FIELD}
              />
            </label>
            <label className="block space-y-1">
              <span className="text-sm font-medium">Project</span>
              <select
                value={form.projectId}
                onChange={(e) => setForm({ ...form, projectId: e.target.value })}
                className={FIELD}
              >
                <option value="">Inbox</option>
                {projects
                  .filter((p) => !p.archived)
                  .map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}
                    </option>
                  ))}
              </select>
            </label>
          </div>

          {labels.length > 0 && (
            <fieldset className="space-y-1">
              <legend className="text-sm font-medium">Labels</legend>
              <div className="flex flex-wrap gap-1.5 pt-1">
                {labels.map((label) => {
                  const on = form.labelIds.includes(label.id);
                  return (
                    <button
                      key={label.id}
                      type="button"
                      onClick={() => toggleLabel(label.id)}
                      aria-pressed={on}
                      className={[
                        'rounded-full border px-2.5 py-1 text-xs transition',
                        on
                          ? 'border-transparent bg-violet-500 text-white'
                          : 'border-slate-200 text-slate-600 dark:border-slate-700 dark:text-slate-300',
                      ].join(' ')}
                    >
                      {label.name}
                    </button>
                  );
                })}
              </div>
            </fieldset>
          )}

          <div className="flex flex-wrap gap-4 pt-1">
            {(
              [
                ['urgent', 'Urgent'],
                ['important', 'Important'],
                ['alertEnabled', 'Alert me'],
              ] as const
            ).map(([key, text]) => (
              <label key={key} className="inline-flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={form[key]}
                  onChange={(e) => setForm({ ...form, [key]: e.target.checked })}
                  className="size-4 accent-violet-500"
                />
                {text}
              </label>
            ))}
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg px-4 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting || !form.title.trim()}
              className="accent-gradient rounded-lg px-4 py-2 text-sm font-medium text-white transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {task ? 'Update' : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
