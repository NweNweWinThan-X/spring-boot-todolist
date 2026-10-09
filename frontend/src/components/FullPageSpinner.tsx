import { Loader2 } from 'lucide-react';

export function FullPageSpinner() {
  return (
    <div className="flex min-h-screen items-center justify-center" role="status" aria-live="polite">
      <Loader2 className="size-8 animate-spin text-violet-500" aria-hidden />
      <span className="sr-only">Loading</span>
    </div>
  );
}
