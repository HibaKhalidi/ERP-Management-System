import axios from 'axios';
import type { Page } from '../types';

const TOKEN_KEY = 'erp_token';

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t: string) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

export const api = axios.create({ baseURL: '/api' });

api.interceptors.request.use((config) => {
  const t = tokenStore.get();
  if (t) config.headers.Authorization = `Bearer ${t}`;
  return config;
});

api.interceptors.response.use(
  (r) => r,
  (error) => {
    if (error.response?.status === 401 && !error.config?.url?.includes('/auth/login')) {
      tokenStore.clear();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  },
);

/** Extrait un message lisible d'une erreur API. */
export function errorMessage(e: unknown): string {
  const err = e as { response?: { data?: { message?: string; details?: Record<string, string> } }; message?: string };
  const d = err.response?.data;
  if (d?.details) return Object.entries(d.details).map(([k, v]) => `${k} : ${v}`).join(' · ');
  return d?.message ?? err.message ?? 'Erreur inconnue';
}

export async function fetchPage<T>(path: string, params: Record<string, unknown>): Promise<Page<T>> {
  const clean = Object.fromEntries(Object.entries(params).filter(([, v]) => v !== '' && v != null));
  return (await api.get<Page<T>>(path, { params: clean })).data;
}

/** Charge tous les éléments d'une ressource (pour les listes déroulantes). */
export async function fetchAll<T>(path: string): Promise<T[]> {
  return (await fetchPage<T>(path, { size: 200, sort: 'id,asc' })).content;
}
