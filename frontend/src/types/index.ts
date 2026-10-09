export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';

export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export type TaskView = 'ALL' | 'INBOX' | 'TODAY' | 'OVERDUE' | 'UPCOMING';

export type EisenhowerQuadrant = 'DO' | 'SCHEDULE' | 'DELEGATE' | 'ELIMINATE';

export type Role = 'USER' | 'ADMIN';

export interface Label {
  id: number;
  name: string;
  color: string;
}

export interface Project {
  id: number;
  name: string;
  color: string;
  archived: boolean;
}

export interface Task {
  id: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate: string | null;
  urgent: boolean;
  important: boolean;
  quadrant: EisenhowerQuadrant;
  startTime: string | null;
  endTime: string | null;
  alertEnabled: boolean;
  projectId: number | null;
  projectName: string | null;
  labels: Label[];
  userId: number;
  createdAt: string;
  updatedAt: string;
}

export interface TaskRequest {
  title: string;
  description?: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate?: string | null;
  urgent?: boolean;
  important?: boolean;
  startTime?: string | null;
  endTime?: string | null;
  alertEnabled?: boolean;
  projectId?: number | null;
  labelIds?: number[];
}

export interface ProjectRequest {
  name: string;
  color: string;
}

export type LabelRequest = ProjectRequest;

export interface DailyProgress {
  date: string;
  total: number;
  completed: number;
  percentage: number;
}

export interface User {
  id: number;
  username: string;
  email: string;
  displayName: string;
  role: Role;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

/** Shape returned by the backend's ApiExceptionHandler for every failure. */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors: Record<string, string>;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface TaskQuery {
  view?: TaskView;
  status?: TaskStatus;
  priority?: TaskPriority;
  projectId?: number;
  labelId?: number;
  urgent?: boolean;
  important?: boolean;
  from?: string;
  to?: string;
  search?: string;
  sortBy?: string;
  direction?: 'ASC' | 'DESC';
  page?: number;
  size?: number;
}
