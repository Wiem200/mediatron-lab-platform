import { useEffect } from 'react';

const toneClasses = {
  success: 'border-emerald-200 bg-white text-emerald-700 shadow-[0_18px_45px_rgba(16,185,129,0.15)]',
  error: 'border-rose-200 bg-white text-rose-700 shadow-[0_18px_45px_rgba(244,63,94,0.14)]',
  info: 'border-sky-200 bg-white text-sky-700 shadow-[0_18px_45px_rgba(14,165,233,0.14)]',
};

export default function ToastNotice({ toast, onClose }) {
  useEffect(() => {
    if (!toast) {
      return undefined;
    }

    const timeoutId = window.setTimeout(() => {
      onClose?.();
    }, 4200);

    return () => window.clearTimeout(timeoutId);
  }, [onClose, toast]);

  if (!toast) {
    return null;
  }

  return (
    <div className="pointer-events-none fixed right-4 top-4 z-[80] w-full max-w-sm sm:right-6 sm:top-6">
      <div
        className={`pointer-events-auto rounded-[1.6rem] border px-5 py-4 ${toneClasses[toast.type] || toneClasses.info}`}
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-[11px] font-black uppercase tracking-[0.3em] opacity-70">Notification</p>
            <p className="mt-2 text-sm font-semibold leading-6">{toast.message}</p>
          </div>
          <button
            type="button"
            onClick={() => onClose?.()}
            className="rounded-full border border-current/15 px-2 py-1 text-xs font-bold uppercase tracking-[0.2em] opacity-70 transition hover:opacity-100"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
