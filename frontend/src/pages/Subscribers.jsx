import { useDeferredValue, useEffect, useRef, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { Mail, MailSearch, Power, Search, Trash2, Users } from 'lucide-react';
import Sidebar from '../components/Sidebar';
import ToastNotice from '../components/ToastNotice';
import { subscriberService } from '../api/subscriberService';
import { hasAdminPrivileges, useStoredUser } from '../utils/authStore';

const statusBadgeClasses = {
  ACTIVE: 'border-emerald-200 bg-emerald-100 text-emerald-700',
  INACTIVE: 'border-amber-200 bg-amber-100 text-amber-800',
};

function formatDate(value) {
  if (!value) {
    return 'Unknown date';
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleString();
}

function getErrorMessage(err, fallback) {
  return (
    err?.response?.data?.message ||
    err?.response?.data?.error ||
    (typeof err?.response?.data === 'string' ? err.response.data : null) ||
    err?.message ||
    fallback
  );
}

export default function Subscribers() {
  const user = useStoredUser();
  const canManageSubscribers = hasAdminPrivileges(user);
  const [allSubscribers, setAllSubscribers] = useState([]);
  const [subscribers, setSubscribers] = useState([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState('');
  const [toast, setToast] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [actionTargetId, setActionTargetId] = useState(null);
  const hasLoadedRef = useRef(false);

  const deferredSearch = useDeferredValue(search);

  useEffect(() => {
    if (!user?.token || !canManageSubscribers) {
      return undefined;
    }

    let cancelled = false;

    const loadSubscribers = async () => {
      const query = deferredSearch.trim();

      if (!hasLoadedRef.current && !query) {
        setLoading(true);
      } else {
        setSearching(true);
      }

      try {
        const response = query
          ? await subscriberService.searchSubscribers(query)
          : await subscriberService.getAllSubscribers();
        if (cancelled) {
          return;
        }

        const records = Array.isArray(response.data) ? response.data : [];
        setSubscribers(records);
        if (!query) {
          setAllSubscribers(records);
        }
        setError('');
        hasLoadedRef.current = true;
      } catch (err) {
        if (!cancelled) {
          setError(getErrorMessage(err, 'Unable to load newsletter subscribers.'));
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
          setSearching(false);
        }
      }
    };

    loadSubscribers();
    return () => {
      cancelled = true;
    };
  }, [canManageSubscribers, deferredSearch, user?.token]);

  if (!user?.token) {
    return <Navigate to="/login" replace />;
  }

  if (!canManageSubscribers) {
    return <Navigate to="/dashboard" replace />;
  }

  const activeCount = allSubscribers.filter((subscriber) => subscriber.status === 'ACTIVE').length;
  const inactiveCount = allSubscribers.filter((subscriber) => subscriber.status === 'INACTIVE').length;

  const updateLocalSubscriber = (updatedSubscriber) => {
    setAllSubscribers((current) => current.map((item) => (item.id === updatedSubscriber.id ? updatedSubscriber : item)));
    setSubscribers((current) => current.map((item) => (item.id === updatedSubscriber.id ? updatedSubscriber : item)));
  };

  const removeLocalSubscriber = (subscriberId) => {
    setAllSubscribers((current) => current.filter((item) => item.id !== subscriberId));
    setSubscribers((current) => current.filter((item) => item.id !== subscriberId));
  };

  const handleToggleStatus = async (subscriber) => {
    const newStatus = subscriber.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    setActionTargetId(subscriber.id);

    try {
      const response = await subscriberService.toggleStatus(subscriber.id, newStatus);
      const updatedSubscriber = response.data?.data;
      if (updatedSubscriber) {
        updateLocalSubscriber(updatedSubscriber);
      }
      setToast({
        type: 'success',
        message:
          response.data?.message ||
          (newStatus === 'ACTIVE' ? 'Subscriber reactivated.' : 'Subscriber moved to inactive status.'),
      });
    } catch (err) {
      setToast({ type: 'error', message: getErrorMessage(err, 'Unable to update subscriber status.') });
    } finally {
      setActionTargetId(null);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) {
      return;
    }

    setActionTargetId(deleteTarget.id);
    try {
      const response = await subscriberService.deleteSubscriber(deleteTarget.id);
      removeLocalSubscriber(deleteTarget.id);
      setToast({
        type: 'success',
        message: response.data?.message || 'Subscriber removed successfully.',
      });
      setDeleteTarget(null);
    } catch (err) {
      setToast({ type: 'error', message: getErrorMessage(err, 'Unable to remove subscriber.') });
    } finally {
      setActionTargetId(null);
    }
  };

  return (
    <div className="flex min-h-screen bg-[#f5f2ec] font-sans text-[#1b2d52]">
      <Sidebar />
      <ToastNotice toast={toast} onClose={() => setToast(null)} />

      <main className="w-full px-4 pb-10 pt-24 sm:px-6 lg:ml-64 lg:p-10">
        <div className="mb-8 flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">
          <div>
                        <h1 className="text-3xl font-serif uppercase tracking-tight sm:text-4xl">Newsletter Audience</h1>
            <p className="mt-3 max-w-3xl text-sm text-[#637390]">
              Review, search, and curate the list of visitors receiving MEDIATRON laboratory updates.
            </p>
          </div>

          <div className="rounded-[1.8rem] border border-[#eadfc9] bg-white px-6 py-5 shadow-sm">
            <p className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#97a4bc]">Active Subscribers</p>
            <p className="mt-3 text-4xl font-serif">{activeCount}</p>
            <p className="mt-2 text-sm text-[#6b7b95]">Currently eligible to receive newsletter communications.</p>
          </div>
        </div>

        {error && (
          <div className="mb-6 rounded-2xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">
            {error}
          </div>
        )}

        <div className="mb-8 grid grid-cols-1 gap-4 md:grid-cols-3">
          <div className="rounded-3xl border border-[#eadfc9] bg-white p-6 shadow-sm">
            <div className="flex items-center justify-between">
              <p className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#97a4bc]">Audience Size</p>
              <Users className="h-5 w-5 text-[#c8a864]" />
            </div>
            <p className="mt-3 text-4xl font-serif">{allSubscribers.length}</p>
            <p className="mt-2 text-sm text-[#6b7b95]">Total stored newsletter subscriber records.</p>
          </div>

          <div className="rounded-3xl border border-[#eadfc9] bg-white p-6 shadow-sm">
            <div className="flex items-center justify-between">
              <p className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#97a4bc]">Active</p>
              <Mail className="h-5 w-5 text-emerald-500" />
            </div>
            <p className="mt-3 text-4xl font-serif">{activeCount}</p>
            <p className="mt-2 text-sm text-[#6b7b95]">Subscribers who can receive upcoming updates.</p>
          </div>

          <div className="rounded-3xl border border-[#eadfc9] bg-white p-6 shadow-sm">
            <div className="flex items-center justify-between">
              <p className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#97a4bc]">Inactive</p>
              <Power className="h-5 w-5 text-amber-500" />
            </div>
            <p className="mt-3 text-4xl font-serif">{inactiveCount}</p>
            <p className="mt-2 text-sm text-[#6b7b95]">Muted records kept for traceability and future reactivation.</p>
          </div>
        </div>

        <section className="rounded-[2rem] border border-[#e3dac9] bg-white p-6 shadow-lg">
          <div className="mb-6 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <h2 className="text-3xl font-serif">Subscriber Directory</h2>
              <p className="mt-2 text-sm text-[#677791]">
                Search by email in real time, mute inactive subscribers, or permanently remove records after confirmation.
              </p>
            </div>

            <label className="block w-full max-w-xl">
              <span className="text-[11px] font-black uppercase tracking-[0.2em] sm:tracking-[0.35em] text-[#9aa6bf]">Search</span>
              <div className="mt-2 flex items-center rounded-[1.75rem] border border-[#e5e9f0] bg-[#fbfcfe] px-4 py-3">
                <Search className="h-5 w-5 text-[#95a4bc]" />
                <input
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  placeholder="Search subscribers by email"
                  className="ml-3 w-full bg-transparent text-sm outline-none"
                />
                {searching && (
                  <span className="ml-4 text-[11px] font-black uppercase tracking-[0.24em] text-[#c8a864]">
                    Searching
                  </span>
                )}
              </div>
            </label>
          </div>

          {loading ? (
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {Array.from({ length: 6 }).map((_, index) => (
                <div key={index} className="animate-pulse rounded-[1.8rem] border border-[#edf1f6] bg-[#faf7f1] p-6">
                  <div className="h-4 w-24 rounded-full bg-[#e9e2d6]" />
                  <div className="mt-4 h-6 w-3/4 rounded-full bg-[#ece6da]" />
                  <div className="mt-3 h-4 w-1/2 rounded-full bg-[#ece6da]" />
                  <div className="mt-6 h-10 rounded-2xl bg-[#ece6da]" />
                </div>
              ))}
            </div>
          ) : subscribers.length === 0 ? (
            <div className="rounded-[2rem] border border-dashed border-[#d8dfeb] bg-[#faf8f3] px-6 py-14 text-center">
              <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-[#fff1d5] text-[#c8a864]">
                <MailSearch className="h-8 w-8" />
              </div>
              <h3 className="mt-6 text-2xl font-serif text-[#1b2d52]">No subscribers found</h3>
              <p className="mx-auto mt-3 max-w-xl text-sm leading-7 text-[#697892]">
                {search.trim()
                  ? 'No newsletter subscribers match the current search query.'
                  : 'No subscribers exist yet. New public subscriptions will appear here automatically.'}
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto rounded-[1.8rem] border border-[#ecdfcf]">
              <div className="overflow-x-auto">
                <table className="min-w-[760px] w-full divide-y divide-[#efe6d8]">
                  <thead className="bg-[#fbf7ef]">
                    <tr>
                      <th className="px-6 py-4 text-left text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#97a4bc]">
                        Email
                      </th>
                      <th className="px-6 py-4 text-left text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#97a4bc]">
                        Subscription Date
                      </th>
                      <th className="px-6 py-4 text-left text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#97a4bc]">
                        Status
                      </th>
                      <th className="px-6 py-4 text-right text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#97a4bc]">
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#f1ebdf] bg-white">
                    {subscribers.map((subscriber) => {
                      const toggleTargetStatus = subscriber.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
                      return (
                        <tr key={subscriber.id} className="transition hover:bg-[#fcfaf5]">
                          <td className="px-6 py-5">
                            <div className="text-sm font-semibold text-[#1b2d52]">{subscriber.email}</div>
                          </td>
                          <td className="px-6 py-5 text-sm text-[#677791]">{formatDate(subscriber.subscribedAt)}</td>
                          <td className="px-6 py-5">
                            <span
                              className={`inline-flex rounded-full border px-3 py-1 text-[11px] font-black uppercase tracking-[0.18em] ${
                                statusBadgeClasses[subscriber.status] || 'border-slate-200 bg-slate-100 text-slate-700'
                              }`}
                            >
                              {subscriber.status}
                            </span>
                          </td>
                          <td className="px-6 py-5">
                            <div className="flex justify-end gap-3">
                              <button
                                type="button"
                                onClick={() => handleToggleStatus(subscriber)}
                                disabled={actionTargetId === subscriber.id}
                                className="inline-flex items-center gap-2 rounded-2xl border border-[#d8dfeb] px-4 py-2 text-xs font-black uppercase tracking-[0.18em] text-[#5f708b] transition hover:border-[#c8a864] hover:text-[#1b2d52] disabled:cursor-not-allowed disabled:opacity-60"
                              >
                                <Power className="h-4 w-4" />
                                {toggleTargetStatus === 'INACTIVE' ? 'Mute' : 'Activate'}
                              </button>
                              <button
                                type="button"
                                onClick={() => setDeleteTarget(subscriber)}
                                disabled={actionTargetId === subscriber.id}
                                className="inline-flex items-center gap-2 rounded-2xl border border-rose-200 px-4 py-2 text-xs font-black uppercase tracking-[0.18em] text-rose-600 transition hover:bg-rose-50 disabled:cursor-not-allowed disabled:opacity-60"
                              >
                                <Trash2 className="h-4 w-4" />
                                Remove
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </section>
      </main>

      {deleteTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#122243]/60 p-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-[2rem] bg-white p-8 shadow-2xl">
            <p className="text-xs font-black uppercase tracking-[0.2em] sm:tracking-[0.35em] text-rose-500">Remove Subscriber</p>
            <h2 className="mt-3 text-2xl font-serif text-[#1b2d52]">{deleteTarget.email}</h2>
            <p className="mt-4 text-sm leading-6 text-[#667791]">
              Are you sure you want to remove this subscriber? This permanently deletes the audience record.
            </p>
            <div className="mt-8 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
              <button
                type="button"
                onClick={() => setDeleteTarget(null)}
                className="rounded-2xl border border-[#d5dded] px-5 py-3 text-sm font-bold uppercase tracking-[0.2em] text-[#6c7d96]"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleDelete}
                disabled={actionTargetId === deleteTarget.id}
                className="rounded-2xl bg-rose-600 px-5 py-3 text-sm font-bold uppercase tracking-[0.2em] text-white hover:bg-rose-700 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {actionTargetId === deleteTarget.id ? 'Removing...' : 'Remove'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
