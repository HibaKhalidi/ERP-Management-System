import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { api, tokenStore } from '../api/client';

interface AuthInfo { username: string; roles: string[]; permissions: string[] }

interface AuthCtx {
  user: AuthInfo | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  can: (permission: string) => boolean;
}

const Ctx = createContext<AuthCtx>(null as unknown as AuthCtx);
export const useAuth = () => useContext(Ctx);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthInfo | null>(null);
  const [loading, setLoading] = useState(!!tokenStore.get());

  useEffect(() => {
    if (!tokenStore.get()) return;
    api.get<AuthInfo>('/auth/me')
      .then((r) => setUser(r.data))
      .catch(() => tokenStore.clear())
      .finally(() => setLoading(false));
  }, []);

  const login = async (username: string, password: string) => {
    const r = await api.post<AuthInfo & { token: string }>('/auth/login', { username, password });
    tokenStore.set(r.data.token);
    setUser({ username: r.data.username, roles: r.data.roles, permissions: r.data.permissions });
  };

  const logout = () => {
    tokenStore.clear();
    setUser(null);
  };

  const can = (p: string) => !!user?.permissions.includes(p);

  return <Ctx.Provider value={{ user, loading, login, logout, can }}>{children}</Ctx.Provider>;
}
