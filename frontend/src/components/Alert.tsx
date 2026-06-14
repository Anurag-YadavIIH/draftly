import type { ReactNode } from 'react';

type Variant = 'error' | 'success' | 'info';

const STYLES: Record<Variant, string> = {
  error: 'bg-red-50 text-red-700 ring-red-200',
  success: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  info: 'bg-blue-50 text-blue-700 ring-blue-200',
};

export default function Alert({ variant = 'info', children }: { variant?: Variant; children: ReactNode }) {
  return (
    <div className={`rounded-lg px-4 py-3 text-sm ring-1 ring-inset ${STYLES[variant]}`} role="status">
      {children}
    </div>
  );
}
