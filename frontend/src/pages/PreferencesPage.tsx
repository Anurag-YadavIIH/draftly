import { useEffect, useState, type FormEvent } from 'react';
import * as preferencesApi from '../api/preferences';
import { apiErrorMessage } from '../api/client';
import { TONES } from '../api/types';
import Alert from '../components/Alert';
import Button from '../components/Button';
import Spinner from '../components/Spinner';

export default function PreferencesPage() {
  const [signature, setSignature] = useState('');
  const [defaultTone, setDefaultTone] = useState<string>('formal');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const prefs = await preferencesApi.getPreferences();
        setSignature(prefs.signature ?? '');
        setDefaultTone(prefs.defaultTone ?? 'formal');
      } catch (err) {
        setError(apiErrorMessage(err, 'Could not load preferences.'));
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    setNotice(null);
    try {
      await preferencesApi.updatePreferences(signature, defaultTone);
      setNotice('Preferences saved.');
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not save preferences.'));
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center py-16 text-slate-400">
        <Spinner className="h-6 w-6" />
      </div>
    );
  }

  return (
    <div className="max-w-2xl">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-slate-900">Preferences</h1>
        <p className="mt-1 text-sm text-slate-500">
          Your signature and default tone are used to personalize every generated draft.
        </p>
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

      <form onSubmit={handleSubmit} className="space-y-5 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <div>
          <label htmlFor="signature" className="block text-sm font-medium text-slate-700">
            Email signature
          </label>
          <textarea
            id="signature"
            rows={4}
            value={signature}
            onChange={(e) => setSignature(e.target.value)}
            placeholder={'Best regards,\nYour Name'}
            className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
          <p className="mt-1 text-xs text-slate-400">Appended to the end of every generated reply.</p>
        </div>

        <div>
          <label htmlFor="defaultTone" className="block text-sm font-medium text-slate-700">
            Default tone
          </label>
          <select
            id="defaultTone"
            value={defaultTone}
            onChange={(e) => setDefaultTone(e.target.value)}
            className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm capitalize shadow-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          >
            {TONES.map((tone) => (
              <option key={tone} value={tone} className="capitalize">
                {tone}
              </option>
            ))}
          </select>
          <p className="mt-1 text-xs text-slate-400">
            Used whenever a draft is generated without an explicit tone.
          </p>
        </div>

        <Button type="submit" loading={saving}>
          Save preferences
        </Button>
      </form>
    </div>
  );
}
