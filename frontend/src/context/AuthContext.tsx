import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import * as authApi from '../api/auth';
import { EMAIL_STORAGE_KEY, TOKEN_STORAGE_KEY } from '../api/client';

interface AuthContextValue {
  email: string | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_STORAGE_KEY));
  const [email, setEmail] = useState<string | null>(() => localStorage.getItem(EMAIL_STORAGE_KEY));

  const applySession = useCallback((nextToken: string, nextEmail: string) => {
    localStorage.setItem(TOKEN_STORAGE_KEY, nextToken);
    localStorage.setItem(EMAIL_STORAGE_KEY, nextEmail);
    setToken(nextToken);
    setEmail(nextEmail);
  }, []);

  const login = useCallback(
    async (loginEmail: string, password: string) => {
      const res = await authApi.login(loginEmail, password);
      applySession(res.token, res.email);
    },
    [applySession],
  );

  const register = useCallback(
    async (registerEmail: string, password: string) => {
      const res = await authApi.register(registerEmail, password);
      applySession(res.token, res.email);
    },
    [applySession],
  );

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(EMAIL_STORAGE_KEY);
    setToken(null);
    setEmail(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ email, token, isAuthenticated: !!token, login, register, logout }),
    [email, token, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
