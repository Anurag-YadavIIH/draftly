import { api } from './client';
import type { PreferenceDto } from './types';

export function getPreferences() {
  return api.get<PreferenceDto>('/preferences').then((r) => r.data);
}

export function updatePreferences(signature: string, defaultTone: string) {
  return api.put<PreferenceDto>('/preferences', { signature, defaultTone }).then((r) => r.data);
}
