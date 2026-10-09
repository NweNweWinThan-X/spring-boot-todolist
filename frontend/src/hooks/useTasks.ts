import { useCallback, useEffect, useRef, useState } from 'react';
import { toast } from 'sonner';
import * as taskApi from '../api/tasks';
import { toErrorMessage } from '../api/client';
import type { DailyProgress, Task, TaskQuery, TaskRequest, TaskStatus } from '../types';

const PAGE_SIZE = 100;
const SEARCH_DEBOUNCE_MS = 300;

export function useTasks(query: Omit<TaskQuery, 'page' | 'size' | 'search'> & { search: string }) {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [progress, setProgress] = useState<DailyProgress | null>(null);
  const [loading, setLoading] = useState(true);
  const [debouncedSearch, setDebouncedSearch] = useState(query.search);
  const initialised = useRef(false);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedSearch(query.search), SEARCH_DEBOUNCE_MS);
    return () => window.clearTimeout(timer);
  }, [query.search]);

  const { view, status, priority, projectId, labelId } = query;

  const load = useCallback(async () => {
    // Only the first load shows skeletons; later refreshes keep the list on screen.
    if (!initialised.current) {
      setLoading(true);
    }
    try {
      const [page, daily] = await Promise.all([
        taskApi.fetchTasks({
          view,
          status,
          priority,
          projectId,
          labelId,
          search: debouncedSearch.trim() || undefined,
          sortBy: 'createdAt',
          direction: 'DESC',
          page: 0,
          size: PAGE_SIZE,
        }),
        taskApi.fetchDailyProgress(),
      ]);
      setTasks(page.content);
      setProgress(daily);
    } catch (error) {
      toast.error(toErrorMessage(error, 'Could not load tasks.'));
    } finally {
      initialised.current = true;
      setLoading(false);
    }
  }, [view, status, priority, projectId, labelId, debouncedSearch]);

  useEffect(() => {
    void load();
  }, [load]);

  const create = useCallback(
    async (request: TaskRequest) => {
      try {
        await taskApi.createTask(request);
        toast.success('Task added');
        await load();
      } catch (error) {
        toast.error(toErrorMessage(error, 'Could not add the task.'));
        throw error;
      }
    },
    [load],
  );

  const update = useCallback(
    async (id: number, request: TaskRequest) => {
      try {
        await taskApi.updateTask(id, request);
        toast.success('Task updated');
        await load();
      } catch (error) {
        toast.error(toErrorMessage(error, 'Could not update the task.'));
        throw error;
      }
    },
    [load],
  );

  const toggleStatus = useCallback(
    async (task: Task) => {
      const next: TaskStatus = task.status === 'COMPLETED' ? 'PENDING' : 'COMPLETED';
      setTasks((current) => current.map((t) => (t.id === task.id ? { ...t, status: next } : t)));
      try {
        await taskApi.updateTaskStatus(task.id, next);
        setProgress(await taskApi.fetchDailyProgress());
      } catch (error) {
        setTasks((current) =>
          current.map((t) => (t.id === task.id ? { ...t, status: task.status } : t)),
        );
        toast.error(toErrorMessage(error, 'Could not change the status.'));
      }
    },
    [],
  );

  const remove = useCallback(
    async (task: Task) => {
      setTasks((current) => current.filter((t) => t.id !== task.id));
      try {
        await taskApi.deleteTask(task.id);
        toast.success('Task deleted');
        setProgress(await taskApi.fetchDailyProgress());
      } catch (error) {
        toast.error(toErrorMessage(error, 'Could not delete the task.'));
        await load();
      }
    },
    [load],
  );

  return { tasks, progress, loading, create, update, toggleStatus, remove, reload: load };
}
