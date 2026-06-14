import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Inbox as InboxIcon, RefreshCw, Sparkles } from 'lucide-react';
import * as emailsApi from '../api/emails';
import * as draftsApi from '../api/drafts';
import { apiErrorMessage } from '../api/client';
import type { EmailDto } from '../api/types';
import Button from '../components/Button';
import Alert from '../components/Alert';
import Spinner from '../components/Spinner';

function formatDate(iso: string) {
  return new Date(iso).toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

export default function InboxPage() {
  const navigate = useNavigate();
  const [emails, setEmails] = useState<EmailDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [fetching, setFetching] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [generatingId, setGeneratingId] = useState<number | null>(null);

  async function loadEmails() {
    setLoading(true);
    setError(null);
    try {
      setEmails(await emailsApi.listEmails());
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not load emails.'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadEmails();
  }, []);

  async function handleFetchNew() {
    setFetching(true);
    setError(null);
    try {
      await emailsApi.fetchEmails(10);
      await loadEmails();
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not fetch new emails.'));
    } finally {
      setFetching(false);
    }
  }

  async function handleGenerateDraft(emailId: number) {
    setGeneratingId(emailId);
    setError(null);
    try {
      const draft = await draftsApi.generateDraft(emailId);
      navigate(`/drafts/${draft.id}`);
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not generate a draft for this email.'));
    } finally {
      setGeneratingId(null);
    }
  }

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Inbox</h1>
          <p className="mt-1 text-sm text-slate-500">Recent emails pulled from your connected Gmail account.</p>
        </div>
        <Button variant="secondary" onClick={handleFetchNew} loading={fetching}>
          <RefreshCw className="h-4 w-4" />
          Fetch new emails
        </Button>
      </div>

      {error && (
        <div className="mb-4">
          <Alert variant="error">{error}</Alert>
        </div>
      )}

      {loading ? (
        <div className="flex justify-center py-16 text-slate-400">
          <Spinner className="h-6 w-6" />
        </div>
      ) : emails.length === 0 ? (
        <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-slate-300 bg-white py-16 text-center">
          <InboxIcon className="h-8 w-8 text-slate-300" />
          <p className="text-sm text-slate-500">No emails yet. Fetch new emails to get started.</p>
        </div>
      ) : (
        <ul className="space-y-3">
          {emails.map((email) => (
            <li key={email.id} className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
              <div className="flex items-start justify-between gap-4">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    {email.unread && <span className="h-2 w-2 flex-shrink-0 rounded-full bg-brand-500" />}
                    <p className="truncate text-sm font-medium text-slate-900">{email.sender}</p>
                    <span className="flex-shrink-0 text-xs text-slate-400">{formatDate(email.receivedAt)}</span>
                  </div>
                  <p className="mt-1 truncate text-sm font-semibold text-slate-800">{email.subject}</p>
                  <p className="mt-1 line-clamp-2 text-sm text-slate-500">{email.body}</p>
                </div>
                <Button
                  variant="secondary"
                  className="flex-shrink-0"
                  onClick={() => handleGenerateDraft(email.id)}
                  loading={generatingId === email.id}
                >
                  <Sparkles className="h-4 w-4" />
                  Generate draft
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
