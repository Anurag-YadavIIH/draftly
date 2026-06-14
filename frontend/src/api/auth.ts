import { api } from './client';
import type { AuthTokenResponse, GmailStatus } from './types';

export function register(email: string, password: string) {
  return api.post<AuthTokenResponse>('/auth/register', { email, password }).then((r) => r.data);
}

export function login(email: string, password: string) {
  return api.post<AuthTokenResponse>('/auth/login', { email, password }).then((r) => r.data);
}

export function gmailStatus() {
  return api.get<GmailStatus>('/auth/status').then((r) => r.data);
}

export function gmailLoginUrl() {
  return api.get<{ authorizationUrl: string }>('/auth/gmail/login').then((r) => r.data);
}

export function logout() {
  return api.post<{ status: string }>('/auth/logout').then((r) => r.data);
}
