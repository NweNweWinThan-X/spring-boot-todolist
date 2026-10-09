import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import * as authApi from '../api/auth';
import { tokenStorage } from '../api/client';
import type { User } from '../types';

interface AuthContextValue {
  user: User | null;
  isAuthenticated: boolean;
  /** True until the stored token has been checked, so guards do not redirect too early. */
  isLoading: boolean;
  login: (usernameOrEmail: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    async function restoreSession() {
      if (!tokenStorage.read()) {
        setIsLoading(false);
        return;
      }
      try {
        const current = await authApi.fetchCurrentUser();
        if (!cancelled) {
          setUser(current);
        }
      } catch {
        // stale or revoked token; the response interceptor has already cleared it
        tokenStorage.clear();
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void restoreSession();
    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (usernameOrEmail: string, password: string) => {
    const response = await authApi.login(usernameOrEmail, password);
    tokenStorage.write(response.accessToken);
    setUser(response.user);
  }, []);

  const register = useCallback(async (username: string, email: string, password: string) => {
    const response = await authApi.register(username, email, password);
    tokenStorage.write(response.accessToken);
    setUser(response.user);
  }, []);

  const logout = useCallback(() => {
    tokenStorage.clear();
    setUser(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ user, isAuthenticated: user !== null, isLoading, login, register, logout }),
    [user, isLoading, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return context;
}
