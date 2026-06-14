import type { DraftStatus } from '../api/types';

const STYLES: Record<DraftStatus, string> = {
  SUGGESTED: 'bg-amber-100 text-amber-800 ring-amber-200',
  APPROVED: 'bg-emerald-100 text-emerald-800 ring-emerald-200',
  EDITED: 'bg-blue-100 text-blue-800 ring-blue-200',
  REJECTED: 'bg-slate-100 text-slate-600 ring-slate-200',
  SENT: 'bg-brand-100 text-brand-700 ring-brand-200',
  FAILED: 'bg-red-100 text-red-700 ring-red-200',
};

export default function StatusBadge({ status }: { status: DraftStatus }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${STYLES[status]}`}
    >
      {status}
    </span>
  );
}
