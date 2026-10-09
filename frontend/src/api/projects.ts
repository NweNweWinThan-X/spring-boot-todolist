import { apiClient } from './client';
import type { Project, ProjectRequest } from '../types';

export async function fetchProjects(): Promise<Project[]> {
  const { data } = await apiClient.get<Project[]>('/projects');
  return data;
}

export async function createProject(request: ProjectRequest): Promise<Project> {
  const { data } = await apiClient.post<Project>('/projects', request);
  return data;
}

export async function updateProject(id: number, request: ProjectRequest): Promise<Project> {
  const { data } = await apiClient.put<Project>(`/projects/${id}`, request);
  return data;
}

export async function setProjectArchived(id: number, archived: boolean): Promise<Project> {
  const { data } = await apiClient.patch<Project>(`/projects/${id}/archived`, null, {
    params: { archived },
  });
  return data;
}

export async function deleteProject(id: number): Promise<void> {
  await apiClient.delete(`/projects/${id}`);
}
