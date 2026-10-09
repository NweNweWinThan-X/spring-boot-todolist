import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { CheckCircle2, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { toErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';

export function LoginPage() {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [usernameOrEmail, setUsernameOrEmail] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    try {
      await login(usernameOrEmail, password);
      navigate('/', { replace: true });
    } catch (error) {
      toast.error(toErrorMessage(error, 'Could not sign in.'));
    } finally {
      setSubmitting(false);
    }
  }

  const field =
    'w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none transition focus:border-violet-400 dark:border-slate-700 dark:bg-slate-900';

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <div className="mb-6 flex items-center justify-center gap-2">
          <CheckCircle2 className="size-7 text-violet-500" aria-hidden />
          <span className="text-lg font-semibold tracking-tight">ToDo</span>
        </div>

        <form
          onSubmit={handleSubmit}
          className="space-y-3 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900"
        >
          <h1 className="text-base font-semibold">Log in</h1>

          <label className="block space-y-1">
            <span className="text-sm font-medium">Username or email address</span>
            <input
              value={usernameOrEmail}
              onChange={(event) => setUsernameOrEmail(event.target.value)}
              autoComplete="username"
              required
              className={field}
            />
          </label>

          <label className="block space-y-1">
            <span className="text-sm font-medium">Password</span>
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="current-password"
              required
              className={field}
            />
          </label>

          <button
            type="submit"
            data-testid="submit"
            disabled={submitting}
            className="inline-flex w-full items-center justify-center gap-2 rounded-lg accent-gradient px-4 py-2 text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-50"
          >
            {submitting && <Loader2 className="size-4 animate-spin" aria-hidden />}
            Log in
          </button>

          <p className="pt-1 text-center text-sm text-slate-500 dark:text-slate-400">
            No account yet?{' '}
            <Link to="/register" className="font-medium text-violet-500 hover:underline">
              Register
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}
