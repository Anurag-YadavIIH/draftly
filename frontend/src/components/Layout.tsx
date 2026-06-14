import { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { Inbox, FileEdit, Settings, LogOut, Mail, Sparkles, RefreshCw } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import * as authApi from '../api/auth';
import type { GmailStatus } from '../api/types';

const NAV_ITEMS = [
  { to: '/inbox', label: 'Inbox', icon: Inbox },
  { to: '/drafts', label: 'Drafts', icon: FileEdit },
  { to: '/preferences', label: 'Preferences', icon: Settings },
];

export default function Layout() {
  const { email, logout } = useAuth();
  const navigate = useNavigate();
  const [gmail, setGmail] = useState<GmailStatus | null>(null);

  async function refreshGmailStatus() {
    try {
      setGmail(await authApi.gmailStatus());
    } catch {
      setGmail(null);
    }
  }

  useEffect(() => {
    refreshGmailStatus();
  }, []);

  async function handleConnectGmail() {
    const { authorizationUrl } = await authApi.gmailLoginUrl();
    window.open(authorizationUrl, '_blank', 'noopener,noreferrer');
  }

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <div className="flex min-h-screen bg-slate-50">
      <aside className="flex w-64 flex-col border-r border-slate-200 bg-white">
        <div className="flex items-center gap-2 px-6 py-5">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-600 text-white">
            <Sparkles className="h-5 w-5" />
          </div>
          <span className="text-lg font-semibold text-slate-900">Draftly</span>
        </div>

        <nav className="flex-1 space-y-1 px-3">
          {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                  isActive ? 'bg-brand-50 text-brand-700' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
                }`
              }
            >
              <Icon className="h-4 w-4" />
              {label}
            </NavLink>
          ))}
        </nav>

        <div className="border-t border-slate-200 p-3">
          <div className="rounded-md bg-slate-50 px-3 py-2.5 text-sm">
            <div className="flex items-center gap-2 text-slate-500">
              <Mail className="h-4 w-4" />
              <span className="text-xs font-medium uppercase tracking-wide">Gmail</span>
            </div>
            <div className="mt-1.5 flex items-center justify-between gap-2">
              <span
                className={`inline-flex items-center gap-1.5 text-xs font-medium ${
                  gmail?.connected ? 'text-emerald-600' : 'text-slate-500'
                }`}
              >
                <span className={`h-1.5 w-1.5 rounded-full ${gmail?.connected ? 'bg-emerald-500' : 'bg-slate-400'}`} />
                {gmail?.connected ? 'Connected' : 'Not connected'}
              </span>
              <button
                onClick={refreshGmailStatus}
                title="Refresh status"
                className="rounded p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-600"
              >
                <RefreshCw className="h-3.5 w-3.5" />
              </button>
            </div>
            {!gmail?.connected && gmail?.gmailMode !== 'mock' && (
              <button
                onClick={handleConnectGmail}
                className="mt-2 w-full rounded-md bg-brand-600 px-2 py-1.5 text-xs font-semibold text-white hover:bg-brand-700"
              >
                Connect Gmail
              </button>
            )}
          </div>
        </div>

        <div className="border-t border-slate-200 p-3">
          <div className="flex items-center justify-between gap-2 px-1">
            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-slate-900">{email}</p>
            </div>
            <button
              onClick={handleLogout}
              title="Log out"
              className="rounded p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
            >
              <LogOut className="h-4 w-4" />
            </button>
          </div>
        </div>
      </aside>

      <main className="flex-1 overflow-y-auto">
        <div className="mx-auto max-w-5xl px-6 py-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
