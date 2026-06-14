import { api } from './client';
import type { DraftDto, SendResultDto } from './types';

export function listDrafts() {
  return api.get<DraftDto[]>('/drafts').then((r) => r.data);
}

export function getDraft(id: number) {
  return api.get<DraftDto>(`/drafts/${id}`).then((r) => r.data);
}

export function generateDraft(emailId: number, tone?: string) {
  return api.post<DraftDto>('/drafts', { emailId, tone }).then((r) => r.data);
}

export function approveDraft(id: number) {
  return api.post<DraftDto>(`/drafts/${id}/approve`).then((r) => r.data);
}

export function editDraft(id: number, content: string) {
  return api.put<DraftDto>(`/drafts/${id}/edit`, { content }).then((r) => r.data);
}

export function rejectDraft(id: number) {
  return api.post<DraftDto>(`/drafts/${id}/reject`).then((r) => r.data);
}

export function sendDraft(id: number) {
  return api.post<SendResultDto>(`/drafts/${id}/send`).then((r) => r.data);
}
