import axios, { AxiosError } from 'axios';
import type { ApiError } from '../types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

export const TOKEN_STORAGE_KEY = 'todolist.accessToken';
const LOGIN_PATH = '/login';

export const tokenStorage = {
  read(): string | null {
    try {
      return window.localStorage.getItem(TOKEN_STORAGE_KEY);
    } catch {
      // private mode or blocked site data: treat as signed out
      return null;
    }
  },
  write(token: string): void {
    try {
      window.localStorage.setItem(TOKEN_STORAGE_KEY, token);
    } catch {
      // nothing to do: the session simply will not survive a reload
    }
  },
  clear(): void {
    try {
      window.localStorage.removeItem(TOKEN_STORAGE_KEY);
    } catch {
      // nothing to do
    }
  },
};

export const apiClient = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use((config) => {
  const token = tokenStorage.read();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    const status = error.response?.status;
    const isAuthCall = error.config?.url?.startsWith('/auth') ?? false;

    // A 401 on a normal call means the stored token is gone or expired. A 401 from the sign-in
    // endpoints is just wrong credentials, so it must reach the form instead of redirecting.
    if (status === 401 && !isAuthCall) {
      tokenStorage.clear();
      if (window.location.pathname !== LOGIN_PATH) {
        window.location.assign(LOGIN_PATH);
      }
    }
    return Promise.reject(error);
  },
);

/** Pulls the backend message out of an axios failure, with a sensible fallback. */
export function toErrorMessage(error: unknown, fallback = '処理に失敗しました。'): string {
  if (axios.isAxiosError<ApiError>(error)) {
    const data = error.response?.data;
    const fieldMessage = data?.fieldErrors && Object.values(data.fieldErrors)[0];
    return fieldMessage ?? data?.message ?? fallback;
  }
  return fallback;
}
