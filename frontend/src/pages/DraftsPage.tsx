import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { FileEdit } from 'lucide-react';
import * as draftsApi from '../api/drafts';
import * as emailsApi from '../api/emails';
import { apiErrorMessage } from '../api/client';
import type { DraftDto, EmailDto } from '../api/types';
import StatusBadge from '../components/StatusBadge';
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

export default function DraftsPage() {
  const [drafts, setDrafts] = useState<DraftDto[]>([]);
  const [emailsById, setEmailsById] = useState<Map<number, EmailDto>>(new Map());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const [draftList, emailList] = await Promise.all([draftsApi.listDrafts(), emailsApi.listEmails()]);
        setDrafts(draftList);
        setEmailsById(new Map(emailList.map((e) => [e.id, e])));
      } catch (err) {
        setError(apiErrorMessage(err, 'Could not load drafts.'));
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-slate-900">Drafts</h1>
        <p className="mt-1 text-sm text-slate-500">Review, edit, approve and send AI-generated replies.</p>
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
      ) : drafts.length === 0 ? (
        <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-slate-300 bg-white py-16 text-center">
          <FileEdit className="h-8 w-8 text-slate-300" />
          <p className="text-sm text-slate-500">
            No drafts yet. Go to the Inbox and generate a reply for an email.
          </p>
        </div>
      ) : (
        <ul className="space-y-3">
          {drafts.map((draft) => {
            const email = emailsById.get(draft.emailId);
            return (
              <li key={draft.id}>
                <Link
                  to={`/drafts/${draft.id}`}
                  className="block rounded-xl border border-slate-200 bg-white p-4 shadow-sm transition-colors hover:border-brand-300 hover:shadow"
                >
                  <div className="flex items-start justify-between gap-4">
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <p className="truncate text-sm font-semibold text-slate-900">
                          {email ? `Re: ${email.subject}` : `Draft #${draft.id}`}
                        </p>
                        <StatusBadge status={draft.status} />
                      </div>
                      {email && <p className="mt-0.5 truncate text-xs text-slate-400">To: {email.sender}</p>}
                      <p className="mt-2 line-clamp-2 text-sm text-slate-500">{draft.content}</p>
                    </div>
                    <div className="flex-shrink-0 text-right">
                      <span className="inline-flex items-center rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-600">
                        {draft.tone}
                      </span>
                      <p className="mt-2 text-xs text-slate-400">{formatDate(draft.updatedAt)}</p>
                    </div>
                  </div>
                </Link>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
