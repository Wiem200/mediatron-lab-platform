import { useCallback, useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import Sidebar from '../components/Sidebar';
import { eventService } from '../api/eventService';

const statusClasses = {
  PENDING_HEAD_VALIDATION: 'bg-amber-100 text-amber-800 border border-amber-200',
  APPROVED: 'bg-emerald-100 text-emerald-800 border border-emerald-200',
  REJECTED: 'bg-rose-100 text-rose-800 border border-rose-200',
};

function formatEventDate(value) {
  if (!value) {
    return '-';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString();
}

export default function ValidationDashboard() {
  const user = JSON.parse(localStorage.getItem('user') || 'null');
  const role = user?.roles?.[0];
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [feedback, setFeedback] = useState(null);
  const [error, setError] = useState('');

  const loadEvents = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await eventService.getPendingValidation();
      setEvents(response.data || []);
    } catch (err) {
      const data = err?.response?.data;
      const message =
        data?.message ||
        data?.error ||
        (typeof data === 'string' && data) ||
        err?.message ||
        'Unable to load the event validation queue.';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (user?.token && role === 'ROLE_LAB_HEAD') {
      loadEvents().catch(() => {});
    }
  }, [loadEvents, role, user?.token]);

  if (!user?.token) {
    return <Navigate to="/login" replace />;
  }

  if (role !== 'ROLE_LAB_HEAD') {
    return <Navigate to="/dashboard" replace />;
  }

  const handleValidation = async (id, action) => {
    setActionLoadingId(id);
    setFeedback(null);
    setError('');
    try {
      const response = await eventService.validate(id, action);
      const validatedEvent = response.data;
      setEvents((current) => current.filter((event) => event.id !== id));
      setFeedback({
        type: 'success',
        message: `"${validatedEvent.title}" was ${action === 'APPROVE' ? 'approved' : 'rejected'} successfully.`,
      });
    } catch (err) {
      const data = err?.response?.data;
      const message =
        data?.message ||
        data?.error ||
        (typeof data === 'string' && data) ||
        err?.message ||
        'Validation failed.';
      setFeedback({ type: 'error', message });
    } finally {
      setActionLoadingId(null);
    }
  };

  return (
    <div className="flex min-h-screen bg-[#f5f2ec] font-sans text-[#1b2d52]">
      <Sidebar />
      <main className="w-full px-4 pb-10 pt-24 sm:px-6 lg:ml-64 lg:p-10">
        <div className="mb-8 flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
          <div>
                        <h1 className="text-3xl font-serif uppercase tracking-tight sm:text-4xl">Event Validation Dashboard</h1>
            <p className="mt-3 max-w-3xl text-sm text-[#637390]">
              Review administrator-submitted events. Only approved events become visible in the public laboratory agenda.
            </p>
          </div>
          <div className="rounded-3xl border border-[#eadfc9] bg-white px-6 py-5 shadow-sm">
            <p className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#97a4bc]">Pending Queue</p>
            <p className="mt-2 text-4xl font-serif">{events.length}</p>
          </div>
        </div>

        {feedback && (
          <div
            className={`mb-6 rounded-2xl border px-5 py-4 text-sm ${
              feedback.type === 'success'
                ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
                : 'border-rose-200 bg-rose-50 text-rose-700'
            }`}
          >
            {feedback.message}
          </div>
        )}

        {error && (
          <div className="mb-6 rounded-2xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">
            {error}
          </div>
        )}

        <div className="overflow-x-auto rounded-[2rem] border border-[#e3dac9] bg-white shadow-lg">
          <table className="min-w-[760px] w-full text-left">
            <thead>
              <tr className="bg-gray-50/50 text-xs font-black uppercase text-gray-400">
                <th className="p-5 text-[#1b2d52]">Event</th>
                <th className="p-5">Schedule</th>
                <th className="p-5">Responsible</th>
                <th className="p-5">Status</th>
                <th className="p-5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {loading && (
                <tr>
                  <td colSpan="5" className="p-5 text-sm text-gray-400">
                    Loading validation queue...
                  </td>
                </tr>
              )}
              {!loading && events.length === 0 && (
                <tr>
                  <td colSpan="5" className="p-5 text-sm text-gray-400">
                    No events are currently waiting for Lab Head validation.
                  </td>
                </tr>
              )}
              {events.map((event) => (
                <tr key={event.id} className="align-top transition-colors hover:bg-gray-50/80">
                  <td className="p-5">
                    <div className="font-semibold">{event.title}</div>
                    <div className="mt-1 max-w-md text-sm text-gray-500">{event.description}</div>
                    <div className="mt-2 text-xs text-gray-400">
                      {event.location ? `Location: ${event.location}` : 'Location not specified'}
                    </div>
                  </td>
                  <td className="p-5 text-sm text-gray-600">{formatEventDate(event.eventDate)}</td>
                  <td className="p-5 text-sm text-gray-600">{event.responsiblePerson}</td>
                  <td className="p-5">
                    <span
                      className={`inline-flex rounded-full px-3 py-1 text-xs font-bold ${
                        statusClasses[event.status] || 'border border-slate-200 bg-slate-100 text-slate-700'
                      }`}
                    >
                      {event.status?.replaceAll('_', ' ')}
                    </span>
                  </td>
                  <td className="p-5">
                    <div className="flex justify-end gap-2">
                      <button
                        type="button"
                        onClick={() => handleValidation(event.id, 'APPROVE')}
                        disabled={actionLoadingId === event.id}
                        className="rounded-xl bg-emerald-600 px-3 py-2 text-xs font-bold text-white hover:bg-emerald-700 disabled:opacity-50"
                      >
                        {actionLoadingId === event.id ? '...' : 'Approve'}
                      </button>
                      <button
                        type="button"
                        onClick={() => handleValidation(event.id, 'REJECT')}
                        disabled={actionLoadingId === event.id}
                        className="rounded-xl bg-rose-600 px-3 py-2 text-xs font-bold text-white hover:bg-rose-700 disabled:opacity-50"
                      >
                        Reject
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </main>
    </div>
  );
}
