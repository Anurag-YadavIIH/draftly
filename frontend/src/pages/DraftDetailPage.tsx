import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Check, Send, X } from 'lucide-react';
import * as draftsApi from '../api/drafts';
import * as emailsApi from '../api/emails';
import { apiErrorMessage } from '../api/client';
import type { DraftDto, EmailDto } from '../api/types';
import StatusBadge from '../components/StatusBadge';
import Alert from '../components/Alert';
import Button from '../components/Button';
import Spinner from '../components/Spinner';

function formatDate(iso: string) {
  return new Date(iso).toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  });
}

const REVIEWABLE_STATUSES: DraftDto['status'][] = ['SUGGESTED', 'APPROVED', 'EDITED', 'FAILED'];
const SENDABLE_STATUSES: DraftDto['status'][] = ['APPROVED', 'EDITED', 'FAILED'];

export default function DraftDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const draftId = Number(id);

  const [draft, setDraft] = useState<DraftDto | null>(null);
  const [email, setEmail] = useState<EmailDto | null>(null);
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState<'save' | 'approve' | 'reject' | 'send' | null>(null);

  useEffect(() => {
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const d = await draftsApi.getDraft(draftId);
        setDraft(d);
        setContent(d.content);
        setEmail(await emailsApi.getEmail(d.emailId));
      } catch (err) {
        setError(apiErrorMessage(err, 'Could not load this draft.'));
      } finally {
        setLoading(false);
      }
    })();
  }, [draftId]);

  const reviewable = draft ? REVIEWABLE_STATUSES.includes(draft.status) : false;
  const sendable = draft ? SENDABLE_STATUSES.includes(draft.status) : false;
  const dirty = draft ? content !== draft.content : false;

  async function handleSave() {
    if (!draft) return;
    setBusy('save');
    setError(null);
    setNotice(null);
    try {
      setDraft(await draftsApi.editDraft(draft.id, content));
      setNotice('Draft updated.');
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not save changes.'));
    } finally {
      setBusy(null);
    }
  }

  async function handleApprove() {
    if (!draft) return;
    setBusy('approve');
    setError(null);
    setNotice(null);
    try {
      setDraft(await draftsApi.approveDraft(draft.id));
      setNotice('Draft approved. It can now be sent.');
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not approve this draft.'));
    } finally {
      setBusy(null);
    }
  }

  async function handleReject() {
    if (!draft) return;
    setBusy('reject');
    setError(null);
    setNotice(null);
    try {
      setDraft(await draftsApi.rejectDraft(draft.id));
      setNotice('Draft rejected.');
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not reject this draft.'));
    } finally {
      setBusy(null);
    }
  }

  async function handleSend() {
    if (!draft) return;
    if (!window.confirm('Send this reply via Gmail now? This cannot be undone.')) {
      return;
    }
    setBusy('send');
    setError(null);
    setNotice(null);
    try {
      const result = await draftsApi.sendDraft(draft.id);
      setDraft(await draftsApi.getDraft(draft.id));
      if (result.status === 'SENT') {
        setNotice(result.message);
      } else {
        setError(result.message);
      }
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not send this draft.'));
    } finally {
      setBusy(null);
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center py-16 text-slate-400">
        <Spinner className="h-6 w-6" />
      </div>
    );
  }

  if (!draft) {
    return (
      <div>
        <Alert variant="error">{error ?? 'Draft not found.'}</Alert>
        <Link to="/drafts" className="mt-4 inline-flex items-center gap-1 text-sm text-brand-600 hover:text-brand-700">
          <ArrowLeft className="h-4 w-4" /> Back to drafts
        </Link>
      </div>
    );
  }

  return (
    <div>
      <button
        onClick={() => navigate('/drafts')}
        className="mb-4 inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-700"
      >
        <ArrowLeft className="h-4 w-4" /> Back to drafts
      </button>

      <div className="mb-4 flex items-center justify-between gap-4">
        <h1 className="text-2xl font-semibold text-slate-900">
          {email ? `Re: ${email.subject}` : `Draft #${draft.id}`}
        </h1>
        <StatusBadge status={draft.status} />
      </div>

      {error && (
        <div className="mb-4">
          <Alert variant="error">{error}</Alert>
        </div>
      )}
      {notice && (
        <div className="mb-4">
          <Alert variant="success">{notice}</Alert>
        </div>
      )}

      {email && (
        <div className="mb-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
          <p className="text-xs font-medium uppercase tracking-wide text-slate-400">Original email</p>
          <div className="mt-2 flex items-center justify-between gap-2">
            <p className="text-sm font-medium text-slate-900">{email.sender}</p>
            <p className="text-xs text-slate-400">{formatDate(email.receivedAt)}</p>
          </div>
          <p className="mt-1 text-sm font-semibold text-slate-800">{email.subject}</p>
          <p className="mt-2 whitespace-pre-wrap text-sm text-slate-600">{email.body}</p>
        </div>
      )}

      <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="mb-2 flex items-center justify-between">
          <p className="text-xs font-medium uppercase tracking-wide text-slate-400">Reply draft</p>
          <span className="inline-flex items-center rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-600">
            {draft.tone} tone
          </span>
        </div>
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          readOnly={!reviewable}
          rows={10}
          className="block w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-800 shadow-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 disabled:bg-slate-50"
        />

        <div className="mt-4 flex flex-wrap items-center gap-2">
          {reviewable && (
            <Button variant="secondary" onClick={handleSave} loading={busy === 'save'} disabled={!dirty}>
              Save changes
            </Button>
          )}
          {reviewable && (
            <Button variant="secondary" onClick={handleApprove} loading={busy === 'approve'}>
              <Check className="h-4 w-4" />
              Approve
            </Button>
          )}
          {reviewable && (
            <Button variant="danger" onClick={handleReject} loading={busy === 'reject'}>
              <X className="h-4 w-4" />
              Reject
            </Button>
          )}
          {sendable && (
            <Button onClick={handleSend} loading={busy === 'send'} className="ml-auto">
              <Send className="h-4 w-4" />
              Send via Gmail
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
