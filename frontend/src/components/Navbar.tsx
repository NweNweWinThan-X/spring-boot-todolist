import { CheckCircle2, LogOut, Moon, Sun } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';

export function Navbar() {
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();

  return (
    <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/80 backdrop-blur dark:border-slate-800 dark:bg-slate-900/80">
      <div className="mx-auto flex max-w-5xl items-center gap-3 px-4 py-3">
        <CheckCircle2 className="size-6 shrink-0 text-violet-500" aria-hidden />
        <span className="text-base font-semibold tracking-tight">ToDo</span>

        <div className="flex-1" />

        <span className="hidden max-w-[16rem] truncate text-sm text-slate-500 sm:block dark:text-slate-400">
          {user?.email}
        </span>

        <button
          type="button"
          onClick={toggleTheme}
          aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
          className="rounded-lg p-2 text-slate-500 transition hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100"
        >
          {theme === 'dark' ? <Sun className="size-5" /> : <Moon className="size-5" />}
        </button>

        <button
          type="button"
          onClick={logout}
          data-testid="logout"
          className="inline-flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-slate-100"
        >
          <LogOut className="size-4" aria-hidden />
          <span className="hidden sm:inline">Log out</span>
        </button>
      </div>
    </header>
  );
}
