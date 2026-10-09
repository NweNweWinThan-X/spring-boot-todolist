import { useState } from 'react';
import { Plus } from 'lucide-react';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { FilterBar } from '../components/FilterBar';
import type { StatusFilter } from '../components/FilterBar';
import { MatrixView } from '../components/MatrixView';
import { Navbar } from '../components/Navbar';
import { ProgressCard } from '../components/ProgressCard';
import { QuickAddTaskBar } from '../components/QuickAddTaskBar';
import { Sidebar } from '../components/Sidebar';
import type { Scope } from '../components/Sidebar';
import { TaskList } from '../components/TaskList';
import { TaskModal } from '../components/TaskModal';
import { useTasks } from '../hooks/useTasks';
import { useTaxonomy } from '../hooks/useTaxonomy';
import { useAuth } from '../context/AuthContext';
import type { Task, TaskPriority, TaskRequest } from '../types';

export function DashboardPage() {
  const { user } = useAuth();
  const { projects, labels } = useTaxonomy();

  const [scope, setScope] = useState<Scope>({ view: 'ALL' });
  const [status, setStatus] = useState<StatusFilter>('ALL');
  const [priority, setPriority] = useState<TaskPriority | 'ALL'>('ALL');
  const [search, setSearch] = useState('');
  const [matrixOpen, setMatrixOpen] = useState(false);

  const { tasks, progress, loading, create, update, toggleStatus, remove } = useTasks({
    view: scope.view,
    projectId: scope.projectId,
    labelId: scope.labelId,
    status: status === 'ALL' ? undefined : status,
    priority: priority === 'ALL' ? undefined : priority,
    search,
  });

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Task | null>(null);
  const [pendingDelete, setPendingDelete] = useState<Task | null>(null);

  const filtered =
    scope.view !== 'ALL' ||
    scope.projectId !== undefined ||
    scope.labelId !== undefined ||
    status !== 'ALL' ||
    priority !== 'ALL' ||
    search.trim().length > 0;

  const remaining = tasks.filter((t) => t.status !== 'COMPLETED').length;

  function closeModal() {
    setModalOpen(false);
    setEditing(null);
  }

  async function handleModalSubmit(request: TaskRequest) {
    if (editing) {
      await update(editing.id, request);
    } else {
      await create(request);
    }
    closeModal();
  }

  return (
    <div className="min-h-screen">
      <Navbar />

      <main className="mx-auto max-w-6xl gap-8 px-4 py-6 lg:flex">
        <Sidebar
          scope={scope}
          onScopeChange={setScope}
          projects={projects}
          labels={labels}
          matrixOpen={matrixOpen}
          onToggleMatrix={() => setMatrixOpen((v) => !v)}
        />

        <div className="mt-6 min-w-0 flex-1 space-y-5 lg:mt-0">
          <header className="flex items-end justify-between gap-3">
            <div className="min-w-0">
              <h1 className="text-xl font-semibold tracking-tight">
                {loading
                  ? 'Loading…'
                  : remaining === 0
                    ? 'Nothing left to do '
                    : `You have ${remaining} task${remaining === 1 ? '' : 's'} to complete `}
                <span aria-hidden>✏️</span>
              </h1>
              <p
                data-testid="current-user"
                className="mt-0.5 truncate text-sm text-slate-500 dark:text-slate-400"
              >
                {user?.displayName ?? user?.username}
              </p>
            </div>
            <button
              type="button"
              data-testid="new-task"
              onClick={() => {
                setEditing(null);
                setModalOpen(true);
              }}
              className="accent-gradient inline-flex shrink-0 items-center gap-1.5 rounded-lg px-4 py-2 text-sm font-medium text-white transition hover:opacity-90"
            >
              <Plus className="size-4" aria-hidden />
              New task
            </button>
          </header>

          <ProgressCard progress={progress} />

          <QuickAddTaskBar onAdd={create} />

          <FilterBar
            search={search}
            onSearchChange={setSearch}
            status={status}
            onStatusChange={setStatus}
            priority={priority}
            onPriorityChange={setPriority}
          />

          {matrixOpen ? (
            <MatrixView tasks={tasks} />
          ) : (
            <TaskList
              tasks={tasks}
              loading={loading}
              filtered={filtered}
              onToggle={toggleStatus}
              onEdit={(task) => {
                setEditing(task);
                setModalOpen(true);
              }}
              onDelete={setPendingDelete}
            />
          )}
        </div>
      </main>

      {modalOpen && (
        <TaskModal
          key={editing?.id ?? 'new'}
          open
          task={editing}
          projects={projects}
          labels={labels}
          onClose={closeModal}
          onSubmit={handleModalSubmit}
        />
      )}

      <ConfirmDialog
        open={pendingDelete !== null}
        title="Delete this task?"
        message={pendingDelete ? `“${pendingDelete.title}” will be removed.` : ''}
        onCancel={() => setPendingDelete(null)}
        onConfirm={() => {
          if (pendingDelete) {
            void remove(pendingDelete);
          }
          setPendingDelete(null);
        }}
      />
    </div>
  );
}
