import { useEffect, useRef, useState } from 'react';

export default function GrafanaPanelFrame({
  title,
  description,
  src,
  height = 360,
  loading = 'lazy',
  priority = false,
  headerAction = null,
}) {
  const timeoutRef = useRef(null);
  const [loaded, setLoaded] = useState(false);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    setLoaded(false);
    setFailed(false);

    timeoutRef.current = setTimeout(() => {
      setFailed(true);
    }, 15000);

    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
    };
  }, [src]);

  const handleLoad = () => {
    setLoaded(true);
    setFailed(false);

    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }
  };

  const handleError = () => {
    setFailed(true);

    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }
  };

  return (
    <article className="flex h-full flex-col overflow-hidden rounded-[1.75rem] border border-[#e3dac9] bg-white shadow-[0_18px_45px_rgba(27,45,82,0.08)] sm:rounded-[2rem]">
      <div className="flex min-h-[132px] flex-col gap-4 border-b border-[#eee5d6] px-5 py-5 sm:px-6 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0 flex-1">
          <h2 className="font-serif text-xl leading-snug text-[#1b2d52] sm:text-2xl">
            {title}
          </h2>

          <p className="mt-2 text-sm leading-7 text-[#60708d]">
            {description}
          </p>
        </div>

        {headerAction ? (
          <div className="shrink-0 lg:max-w-[260px]">
            {headerAction}
          </div>
        ) : null}
      </div>

      <div
        className="relative flex-1 overflow-hidden bg-white"
        style={{ minHeight: `${height}px` }}
      >
        {!loaded && !failed ? (
          <div className="absolute inset-0 z-10 animate-pulse bg-[linear-gradient(135deg,_rgba(255,255,255,0.95)_0%,_rgba(244,239,229,0.96)_100%)]">
            <div className="flex h-full flex-col gap-4 px-6 py-6">
              <div className="h-4 w-40 rounded-full bg-[#e0d6c3]" />
              <div className="h-4 w-64 rounded-full bg-[#ece4d5]" />
              <div className="mt-4 flex-1 rounded-[1.5rem] bg-white/80" />
            </div>
          </div>
        ) : null}

        {failed ? (
          <div className="absolute inset-0 z-20 flex items-center justify-center bg-[#fff8f5] px-6 text-center">
            <div>
              <p className="text-sm font-semibold text-rose-700">
                Unable to load the embedded Grafana panel.
              </p>

              <p className="mt-2 text-xs leading-6 text-[#687892]">
                Verify that Grafana is running and that anonymous embedding is enabled.
              </p>
            </div>
          </div>
        ) : null}

        <iframe
          title={title}
          src={src}
          loading={priority ? 'eager' : loading}
          onLoad={handleLoad}
          onError={handleError}
          className="relative z-0 w-full border-0 bg-white"
          style={{ height: `${height}px`, minHeight: `${height}px` }}
          referrerPolicy="strict-origin-when-cross-origin"
          allowFullScreen
        />
      </div>
    </article>
  );
}