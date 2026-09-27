import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { adminDelegationService } from '../api/adminDelegationService';
import { authService } from '../api/authService';
import ToastNotice from './ToastNotice';
import { getPrimaryRole, hasAdminPrivileges, setStoredUser, useStoredUser } from '../utils/authStore';

const SIDEBAR_STORAGE_KEY = 'mediatron-sidebar-collapsed';

function useDesktopBreakpoint() {
  const [isDesktop, setIsDesktop] = useState(() => {
    if (typeof window === 'undefined') {
      return true;
    }

    return window.matchMedia('(min-width: 1024px)').matches;
  });

  useEffect(() => {
    if (typeof window === 'undefined') {
      return undefined;
    }

    const mediaQuery = window.matchMedia('(min-width: 1024px)');
    const update = () => setIsDesktop(mediaQuery.matches);

    update();
    mediaQuery.addEventListener('change', update);
    return () => mediaQuery.removeEventListener('change', update);
  }, []);

  return isDesktop;
}

export default function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const user = useStoredUser();
  const role = getPrimaryRole(user);
  const delegationActive = role === 'ROLE_LAB_HEAD' && Boolean(user?.delegationActive);
  const canAccessAdminArea = hasAdminPrivileges(user);
  const isDesktop = useDesktopBreakpoint();
  const [delegationLoading, setDelegationLoading] = useState(false);
  const [toast, setToast] = useState(null);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(() => {
    if (typeof window === 'undefined') {
      return false;
    }

    return window.localStorage.getItem(SIDEBAR_STORAGE_KEY) === 'true';
  });

  const compact = isDesktop && collapsed;
  const adminRoutes = ['/manage-members', '/manage-projects', '/events', '/newsletter-audience'];

  useEffect(() => {
    const sidebarOffset = compact ? '5rem' : '16rem';
    document.documentElement.style.setProperty('--admin-sidebar-offset', sidebarOffset);
    document.documentElement.setAttribute('data-admin-sidebar', compact ? 'collapsed' : 'expanded');
    window.localStorage.setItem(SIDEBAR_STORAGE_KEY, collapsed ? 'true' : 'false');
  }, [collapsed, compact]);

  useEffect(() => {
    setMobileOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!mobileOpen) {
      return undefined;
    }

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        setMobileOpen(false);
      }
    };

    document.body.style.overflow = 'hidden';
    window.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.overflow = '';
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [mobileOpen]);

  const handleLogout = () => {
    authService.logout();
    navigate('/login');
  };

  const syncDelegationStatus = useCallback(async () => {
    if (!user?.token || role !== 'ROLE_LAB_HEAD') {
      return;
    }

    try {
      const response = await adminDelegationService.getStatus();
      const active = Boolean(response.data?.active);

      if (active === delegationActive) {
        return;
      }

      setStoredUser({ ...user, delegationActive: active });

      if (!active && delegationActive) {
        setToast({ type: 'info', message: 'Admin Mode was revoked because the primary Admin logged in.' });
        if (adminRoutes.includes(location.pathname)) {
          navigate('/dashboard', { replace: true });
        }
      }
    } catch {
      // Keep the last known state until the next successful poll.
    }
  }, [delegationActive, location.pathname, navigate, role, user]);

  useEffect(() => {
    if (!user?.token || role !== 'ROLE_LAB_HEAD') {
      return undefined;
    }

    syncDelegationStatus();
    const intervalId = window.setInterval(syncDelegationStatus, 5000);
    return () => window.clearInterval(intervalId);
  }, [role, syncDelegationStatus, user?.token]);

  const handleDelegationToggle = async () => {
    setDelegationLoading(true);
    try {
      const response = delegationActive
        ? await adminDelegationService.deactivate()
        : await adminDelegationService.activate();
      const active = Boolean(response.data?.active);
      setStoredUser({ ...user, delegationActive: active });
      setToast({
        type: 'success',
        message: active
          ? 'Admin Mode activated. All actions are now being logged.'
          : 'Admin Mode deactivated.',
      });

      if (!active && adminRoutes.includes(location.pathname)) {
        navigate('/dashboard', { replace: true });
      }
    } catch (err) {
      const data = err?.response?.data;
      const message =
        data?.message ||
        data?.error ||
        (typeof data === 'string' && data) ||
        err?.message ||
        'Unable to change delegation mode.';
      setToast({ type: 'error', message });
    } finally {
      setDelegationLoading(false);
    }
  };

  const navItems = useMemo(() => {
    const items = [
      {
        to: '/dashboard',
        paths: ['/dashboard'],
        label: 'Dashboard',
        shortLabel: 'D',
      },
    ];

    if (canAccessAdminArea) {
      items.push(
        {
          to: '/manage-members',
          paths: ['/manage-members'],
          label: 'Manage Members',
          shortLabel: 'M',
        },
        {
          to: '/manage-projects',
          paths: ['/manage-projects'],
          label: 'Manage Projects',
          shortLabel: 'P',
        },
        {
          to: '/events',
          paths: ['/events'],
          label: 'Manage Events',
          shortLabel: 'E',
        },
        {
          to: '/newsletter-audience',
          paths: ['/newsletter-audience'],
          label: 'Newsletter Audience',
          shortLabel: 'N',
        },
      );
    }

    if (role === 'ROLE_LAB_HEAD') {
      items.push(
        {
          to: '/validate-events',
          paths: ['/validate-events', '/validate'],
          label: 'Event Validation',
          shortLabel: 'EV',
        },
        {
          to: '/validate-projects',
          paths: ['/validate-projects'],
          label: 'Project Validation',
          shortLabel: 'PV',
        },
      );
    }

    if (role === 'ROLE_MEMBER' || role === 'ROLE_LAB_HEAD') {
      items.push({
        to: '/my-publications',
        paths: ['/my-publications'],
        label: 'My Publications',
        shortLabel: 'MP',
      });
    }

    return items;
  }, [canAccessAdminArea, role]);

  const isActive = (paths) => paths.includes(location.pathname);

  const navLinkClass = (active) => `
    group flex min-h-[44px] w-full items-center rounded-xl text-sm font-semibold transition
    ${compact ? 'justify-center px-2' : 'gap-3 px-3'}
    ${active ? 'bg-[#c8a864] text-[#1b2d52] shadow-lg shadow-black/10' : 'text-gray-300 hover:bg-white/10 hover:text-white'}
  `;

  return (
    <>
      <style>{`
        @media (min-width: 1024px) {
          .lg\\:ml-64 {
            margin-left: var(--admin-sidebar-offset, 16rem) !important;
            transition: margin-left 220ms ease;
          }

          .lg\\:pl-\\[18rem\\] {
            padding-left: calc(var(--admin-sidebar-offset, 16rem) + 2rem) !important;
            transition: padding-left 220ms ease;
          }
        }
      `}</style>

      <ToastNotice toast={toast} onClose={() => setToast(null)} />

      <button
        type="button"
        onClick={() => setMobileOpen(true)}
        aria-label="Open dashboard menu"
        className="fixed left-4 top-4 z-[70] flex h-11 w-11 items-center justify-center rounded-2xl bg-[#1b2d52] text-sm font-black text-[#c8a864] shadow-2xl ring-4 ring-white/60 lg:hidden"
      >
        M
      </button>

      {mobileOpen && (
        <button
          type="button"
          aria-label="Close dashboard menu"
          onClick={() => setMobileOpen(false)}
          className="fixed inset-0 z-[55] bg-[#101827]/45 backdrop-blur-sm lg:hidden"
        />
      )}

      {delegationActive && (
        <div className="fixed inset-x-0 top-0 z-[70] border-b border-amber-300 bg-amber-100/95 text-amber-950 shadow-sm backdrop-blur">
          <div className="px-4 py-3 text-sm font-semibold lg:pl-[18rem]">
            Admin Mode Active - All actions are being logged.
          </div>
        </div>
      )}

      <aside
        className={`fixed left-0 top-0 z-[60] flex h-[100dvh] flex-col bg-[#1b2d52] text-white shadow-2xl transition-all duration-300 ease-in-out lg:translate-x-0 ${
          mobileOpen ? 'translate-x-0' : '-translate-x-full'
        } ${compact ? 'lg:w-20 lg:px-3 lg:py-6' : 'lg:w-64 lg:p-6'} w-[18rem] max-w-[calc(100vw-1.25rem)] p-5`}
      >
        <button
          type="button"
          onClick={() => setCollapsed((current) => !current)}
          aria-label={compact ? 'Open sidebar' : 'Close sidebar'}
          title={compact ? 'Open sidebar' : 'Close sidebar'}
          className="absolute -right-4 top-6 hidden h-9 w-9 items-center justify-center rounded-full border border-white/10 bg-[#c8a864] text-lg font-black text-[#1b2d52] shadow-xl transition hover:bg-[#d7b77a] lg:flex"
        >
          {compact ? '›' : '‹'}
        </button>

        <button
          type="button"
          onClick={() => setMobileOpen(false)}
          aria-label="Close dashboard menu"
          className="absolute right-3 top-3 flex h-9 w-9 items-center justify-center rounded-full border border-white/10 bg-white/10 text-lg font-black text-white lg:hidden"
        >
          ×
        </button>

        <div className={`mb-8 flex items-center ${compact ? 'justify-center' : 'gap-3'}`}>
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-md bg-[#c8a864] font-bold text-[#1b2d52]">
            M
          </div>
          {!compact && (
            <div className="min-w-0 pr-8 lg:pr-0">
              <h2 className="text-lg font-bold tracking-tight text-[#c8a864]">MEDIATRON</h2>
              <p className="text-[9px] uppercase tracking-[2px] text-gray-400">Portal Management</p>
            </div>
          )}
        </div>

        {role === 'ROLE_LAB_HEAD' && !compact && (
          <div className="mb-6 rounded-2xl border border-white/10 bg-white/5 p-4">
            <p className="text-[10px] font-black uppercase tracking-[0.24em] text-[#c8a864]">Delegation Mode</p>
            <p className="mt-2 text-xs leading-5 text-gray-300">
              Temporarily unlock administrator CRUD actions when the primary Admin is unavailable.
            </p>
            <button
              type="button"
              onClick={handleDelegationToggle}
              disabled={delegationLoading}
              className={`mt-4 w-full rounded-xl px-4 py-3 text-xs font-black uppercase tracking-[0.18em] transition ${
                delegationActive
                  ? 'bg-amber-100 text-amber-900 hover:bg-amber-200'
                  : 'bg-[#c8a864] text-[#1b2d52] hover:bg-[#d7b77a]'
              } disabled:cursor-not-allowed disabled:opacity-60`}
            >
              {delegationLoading
                ? 'Updating...'
                : delegationActive
                  ? 'Exit Admin Mode'
                  : 'Switch to Admin Mode'}
            </button>
          </div>
        )}

        {role === 'ROLE_LAB_HEAD' && compact && (
          <button
            type="button"
            onClick={handleDelegationToggle}
            disabled={delegationLoading}
            title={delegationActive ? 'Exit Admin Mode' : 'Switch to Admin Mode'}
            className={`mb-5 flex h-12 w-full items-center justify-center rounded-2xl text-[10px] font-black uppercase tracking-[0.16em] transition ${
              delegationActive
                ? 'bg-amber-100 text-amber-900 hover:bg-amber-200'
                : 'bg-white/10 text-[#c8a864] hover:bg-white/15'
            } disabled:cursor-not-allowed disabled:opacity-60`}
          >
            ADM
          </button>
        )}

        <nav className="min-h-0 flex-1 space-y-2 overflow-y-auto overflow-x-hidden pr-1">
          {navItems.map((item) => {
            const active = isActive(item.paths);

            return (
              <Link
                key={item.to}
                to={item.to}
                title={compact ? item.label : undefined}
                className={navLinkClass(active)}
              >
                <span
                  className={`flex h-8 min-w-8 items-center justify-center rounded-lg text-[11px] font-black uppercase tracking-tight transition ${
                    active ? 'bg-[#1b2d52]/10 text-[#1b2d52]' : 'bg-white/10 text-[#c8a864] group-hover:bg-white/15'
                  }`}
                >
                  {item.shortLabel}
                </span>
                {!compact && <span className="truncate">{item.label}</span>}
              </Link>
            );
          })}
        </nav>

        <div className="border-t border-white/10 pt-5">
          {!compact ? (
            <>
              <div className="mb-4">
                <p className="text-[10px] font-bold uppercase tracking-wider text-gray-500">Signed in as</p>
                <p className="truncate text-xs font-medium text-[#c8a864]">{user?.email}</p>
                {delegationActive && (
                  <p className="mt-2 text-[10px] font-black uppercase tracking-[0.22em] text-amber-300">
                    Delegated Admin Session
                  </p>
                )}
              </div>

              <button
                type="button"
                onClick={handleLogout}
                className="flex w-full items-center justify-center gap-2 rounded-lg border border-red-500/20 bg-red-500/10 p-3 text-sm font-bold text-red-400 transition hover:bg-red-500/20"
              >
                Sign Out
              </button>
            </>
          ) : (
            <button
              type="button"
              onClick={handleLogout}
              title={`Sign out${user?.email ? ` - ${user.email}` : ''}`}
              className="flex h-12 w-full items-center justify-center rounded-2xl border border-red-500/20 bg-red-500/10 text-xs font-black uppercase tracking-[0.18em] text-red-300 transition hover:bg-red-500/20"
            >
              OUT
            </button>
          )}
        </div>
      </aside>
    </>
  );
}
