import type { Task } from '../types';
import { EmptyState } from './EmptyState';
import { TaskCard } from './TaskCard';
import { TaskSkeleton } from './TaskSkeleton';

interface TaskListProps {
  tasks: Task[];
  loading: boolean;
  filtered: boolean;
  onToggle: (task: Task) => void;
  onEdit: (task: Task) => void;
  onDelete: (task: Task) => void;
}

export function TaskList({ tasks, loading, filtered, onToggle, onEdit, onDelete }: TaskListProps) {
  if (loading) {
    return <TaskSkeleton />;
  }
  if (tasks.length === 0) {
    return <EmptyState filtered={filtered} />;
  }
  return (
    <ul className="space-y-3">
      {tasks.map((task) => (
        <TaskCard
          key={task.id}
          task={task}
          onToggle={onToggle}
          onEdit={onEdit}
          onDelete={onDelete}
        />
      ))}
    </ul>
  );
}
