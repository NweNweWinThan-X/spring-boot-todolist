import { apiClient } from './client';
import type {
  DailyProgress,
  PagedResponse,
  Task,
  TaskQuery,
  TaskRequest,
  TaskStatus,
} from '../types';

export async function fetchTasks(query: TaskQuery): Promise<PagedResponse<Task>> {
  const { data } = await apiClient.get<PagedResponse<Task>>('/tasks', { params: query });
  return data;
}

export async function fetchDailyProgress(date?: string): Promise<DailyProgress> {
  const { data } = await apiClient.get<DailyProgress>('/tasks/progress', { params: { date } });
  return data;
}

export async function createTask(request: TaskRequest): Promise<Task> {
  const { data } = await apiClient.post<Task>('/tasks', request);
  return data;
}

export async function updateTask(id: number, request: TaskRequest): Promise<Task> {
  const { data } = await apiClient.put<Task>(`/tasks/${id}`, request);
  return data;
}

export async function updateTaskStatus(id: number, status: TaskStatus): Promise<Task> {
  const { data } = await apiClient.patch<Task>(`/tasks/${id}/status`, { status });
  return data;
}

export async function deleteTask(id: number): Promise<void> {
  await apiClient.delete(`/tasks/${id}`);
}
