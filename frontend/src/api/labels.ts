import { apiClient } from './client';
import type { Label, LabelRequest } from '../types';

export async function fetchLabels(): Promise<Label[]> {
  const { data } = await apiClient.get<Label[]>('/labels');
  return data;
}

export async function createLabel(request: LabelRequest): Promise<Label> {
  const { data } = await apiClient.post<Label>('/labels', request);
  return data;
}

export async function updateLabel(id: number, request: LabelRequest): Promise<Label> {
  const { data } = await apiClient.put<Label>(`/labels/${id}`, request);
  return data;
}

export async function deleteLabel(id: number): Promise<void> {
  await apiClient.delete(`/labels/${id}`);
}
