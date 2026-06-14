import { api } from './client';
import type { EmailDto } from './types';

export function listEmails() {
  return api.get<EmailDto[]>('/emails').then((r) => r.data);
}

export function getEmail(id: number) {
  return api.get<EmailDto>(`/emails/${id}`).then((r) => r.data);
}

export function fetchEmails(max = 10) {
  return api.post<EmailDto[]>(`/emails/fetch`, null, { params: { max } }).then((r) => r.data);
}
