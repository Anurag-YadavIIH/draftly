export interface AuthTokenResponse {
  token: string;
  tokenType: string;
  email: string;
}

export interface EmailDto {
  id: number;
  gmailMessageId: string;
  threadId: string;
  sender: string;
  subject: string;
  body: string;
  receivedAt: string;
  unread: boolean;
}

export type DraftStatus = 'SUGGESTED' | 'APPROVED' | 'EDITED' | 'REJECTED' | 'SENT' | 'FAILED';

export interface DraftDto {
  id: number;
  emailId: number;
  threadId: string;
  tone: string;
  content: string;
  status: DraftStatus;
  createdAt: string;
  updatedAt: string;
}

export interface SendResultDto {
  draftId: number;
  status: DraftStatus;
  sentGmailMessageId: string | null;
  attempts: number;
  message: string;
}

export interface PreferenceDto {
  userEmail: string;
  signature: string;
  defaultTone: string;
}

export interface GmailStatus {
  gmailMode: string;
  connected: boolean;
  note: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

export const TONES = ['formal', 'friendly', 'concise'] as const;
export type Tone = (typeof TONES)[number];
