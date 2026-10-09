import { AlertTriangle, CalendarDays, Hash, Inbox, Layers, ListTodo, Sun } from 'lucide-react';
import type { Label, Project, TaskView } from '../types';

export interface Scope {
  view: TaskView;
  projectId?: number;
  labelId?: number;
}

const VIEWS: ReadonlyArray<{ view: TaskView; label: string; icon: typeof Inbox }> = [
  { view: 'ALL', label: 'All tasks', icon: ListTodo },
  { view: 'INBOX', label: 'Inbox', icon: Inbox },
  { view: 'TODAY', label: 'Today', icon: Sun },
  { view: 'UPCOMING', label: 'Upcoming', icon: CalendarDays },
  { view: 'OVERDUE', label: 'Overdue', icon: AlertTriangle },
];

interface SidebarProps {
  scope: Scope;
  onScopeChange: (scope: Scope) => void;
  projects: Project[];
  labels: Label[];
  matrixOpen: boolean;
  onToggleMatrix: () => void;
}

function rowClass(active: boolean): string {
  return [
    'flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-sm transition',
    active
      ? 'bg-violet-500/15 font-medium text-violet-600 dark:text-violet-300'
      : 'text-slate-600 hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-800/60',
  ].join(' ');
}

export function Sidebar({
  scope,
  onScopeChange,
  projects,
  labels,
  matrixOpen,
  onToggleMatrix,
}: SidebarProps) {
  const activeProjects = projects.filter((p) => !p.archived);

  return (
    <nav aria-label="Views" className="w-full shrink-0 space-y-6 lg:w-56">
      <ul className="space-y-0.5">
        {VIEWS.map(({ view, label, icon: Icon }) => (
          <li key={view}>
            <button
              type="button"
              onClick={() => onScopeChange({ view })}
              aria-current={scope.view === view && !scope.projectId && !scope.labelId}
              className={rowClass(
                scope.view === view && !scope.projectId && !scope.labelId,
              )}
            >
              <Icon className="size-4 shrink-0" aria-hidden />
              {label}
            </button>
          </li>
        ))}
        <li>
          <button type="button" onClick={onToggleMatrix} className={rowClass(matrixOpen)}>
            <Layers className="size-4 shrink-0" aria-hidden />
            Eisenhower matrix
          </button>
        </li>
      </ul>

      {activeProjects.length > 0 && (
        <section>
          <h2 className="px-3 pb-1 text-xs font-semibold tracking-wide text-slate-400 uppercase">
            Projects
          </h2>
          <ul className="space-y-0.5">
            {activeProjects.map((project) => (
              <li key={project.id}>
                <button
                  type="button"
                  onClick={() => onScopeChange({ view: 'ALL', projectId: project.id })}
                  className={rowClass(scope.projectId === project.id)}
                >
                  <span
                    className="size-2.5 shrink-0 rounded-full"
                    style={{ backgroundColor: project.color }}
                    aria-hidden
                  />
                  <span className="truncate">{project.name}</span>
                </button>
              </li>
            ))}
          </ul>
        </section>
      )}

      {labels.length > 0 && (
        <section>
          <h2 className="px-3 pb-1 text-xs font-semibold tracking-wide text-slate-400 uppercase">
            Labels
          </h2>
          <ul className="space-y-0.5">
            {labels.map((label) => (
              <li key={label.id}>
                <button
                  type="button"
                  onClick={() => onScopeChange({ view: 'ALL', labelId: label.id })}
                  className={rowClass(scope.labelId === label.id)}
                >
                  <Hash className="size-4 shrink-0" style={{ color: label.color }} aria-hidden />
                  <span className="truncate">{label.name}</span>
                </button>
              </li>
            ))}
          </ul>
        </section>
      )}
    </nav>
  );
}
