import { useCallback, useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { eventService } from '../api/eventService';
import Sidebar from '../components/Sidebar';
import { getPrimaryRole, hasAdminPrivileges, useStoredUser } from '../utils/authStore';

const initialFormState = {
  title: '',
  description: '',
  eventDate: '',
  location: '',
  responsiblePerson: '',
};

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

function normalizeDateTime(value) {
  if (!value) {
    return '';
  }

  return value.slice(0, 16);
}

export default function EventsManagement() {
  const user = useStoredUser();
  const role = getPrimaryRole(user);

  const [events, setEvents] = useState([]);
  const [formData, setFormData] = useState(initialFormState);
  const [showModal, setShowModal] = useState(false);
  const [loading, setLoading] = useState(true);
  const [feedback, setFeedback] = useState(null);
  const canManageEvents = hasAdminPrivileges(user);

  const loadEvents = useCallback(async () => {
    setLoading(true);
    try {
      const response = await eventService.getAll();
      setEvents(response.data || []);
    } catch (err) {
      console.error('Event fetch error:', err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (canManageEvents) {
      loadEvents().catch(() => {});
    }
  }, [canManageEvents, loadEvents]);

  if (!user?.token) {
    return <Navigate to="/login" replace />;
  }

  if (!canManageEvents) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((current) => ({
      ...current,
      [name]: value,
    }));
  };

  const closeModal = () => {
    setFormData(initialFormState);
    setShowModal(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      await eventService.add({
        title: formData.title.trim(),
        description: formData.description.trim(),
        eventDate: formData.eventDate,
        location: formData.location.trim(),
        responsiblePerson: formData.responsiblePerson.trim(),
      });
      closeModal();
      await loadEvents();
      setFeedback({ type: 'success', message: 'Event saved successfully and submitted for Lab Head validation.' });
    } catch (err) {
      const data = err?.response?.data;
      const message =
        data?.message ||
        data?.error ||
        (typeof data === 'string' && data) ||
        err?.message ||
        'Event save failed.';
      setFeedback({ type: 'error', message });
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this event?')) {
      return;
    }

    try {
      await eventService.delete(id);
      await loadEvents();
      setFeedback({ type: 'success', message: 'Event deleted successfully.' });
    } catch (err) {
      const data = err?.response?.data;
      const message =
        data?.message ||
        data?.error ||
        (typeof data === 'string' && data) ||
        err?.message ||
        'Delete failed.';
      setFeedback({ type: 'error', message });
    }
  };

  return (
    <div className="flex min-h-screen bg-[#f5f2ec] font-sans text-[#1b2d52]">
      <Sidebar />
      <main className="w-full px-4 pb-10 pt-24 sm:px-6 lg:ml-64 lg:p-10">
        <div className="mb-10 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
                        <h1 className="text-3xl font-serif uppercase tracking-tight sm:text-4xl">Laboratory Events</h1>
            <p className="mt-2 text-sm text-gray-500">Create and monitor laboratory events before Lab Head validation.</p>
          </div>
          <button
            onClick={() => setShowModal(true)}
            className="rounded-xl bg-[#1b2d52] px-8 py-3 font-bold text-white shadow-lg transition-all hover:bg-[#2c4375]"
          >
            + ADD EVENT
          </button>
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

        <div className="mb-8 grid grid-cols-1 gap-6 md:grid-cols-3">
          <div className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
            <p className="mb-2 text-[10px] font-black uppercase tracking-[0.25em] text-gray-400">Queue</p>
            <p className="text-3xl font-serif">{events.length}</p>
            <p className="mt-2 text-sm text-gray-500">Total events under administration.</p>
          </div>
          <div className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
            <p className="mb-2 text-[10px] font-black uppercase tracking-[0.25em] text-gray-400">Pending</p>
            <p className="text-3xl font-serif">
              {events.filter((event) => event.status === 'PENDING_HEAD_VALIDATION').length}
            </p>
            <p className="mt-2 text-sm text-gray-500">Waiting for Lab Head review.</p>
          </div>
          <div className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
            <p className="mb-2 text-[10px] font-black uppercase tracking-[0.25em] text-gray-400">Status</p>
            <p className="text-lg font-semibold text-[#1b2d52]">Admin submission active</p>
            <p className="mt-2 text-sm text-gray-500">Every new event starts pending validation.</p>
          </div>
        </div>

        <div className="overflow-x-auto rounded-2xl border border-gray-100 bg-white shadow-xl">
          <table className="min-w-[760px] w-full text-left">
            <thead>
              <tr className="border-b border-gray-100 bg-gray-50/50 text-xs font-black uppercase text-gray-400">
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
                    Loading events...
                  </td>
                </tr>
              )}
              {!loading && events.length === 0 && (
                <tr>
                  <td colSpan="5" className="p-5 text-sm text-gray-400">
                    No laboratory events have been created yet.
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
                  <td className="p-5 text-right">
                    <button
                      onClick={() => handleDelete(event.id)}
                      className="text-xs font-bold text-red-500 hover:underline"
                    >
                      DELETE
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </main>

      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#1b2d52]/70 p-4 backdrop-blur-md">
          <div className="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-[1.75rem] sm:rounded-[2.5rem] bg-white p-10 shadow-2xl">
            <h2 className="mb-1 text-3xl font-serif text-[#1b2d52]">Create Laboratory Event</h2>
            <p className="mb-8 border-b pb-4 text-sm text-gray-400">
              New events are submitted with pending Lab Head validation status.
            </p>

            <form onSubmit={handleSubmit} className="space-y-8">
              <section>
                <h3 className="mb-4 text-xs font-black uppercase tracking-widest text-[#c8a864]">Event Details</h3>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 sm:gap-6">
                  <div className="col-span-2 flex flex-col gap-1">
                    <label className="text-[10px] font-bold uppercase text-gray-400">Title</label>
                    <input
                      name="title"
                      value={formData.title}
                      onChange={handleChange}
                      className="border-b-2 border-gray-100 p-2 outline-none focus:border-[#c8a864]"
                      required
                    />
                  </div>
                  <div className="col-span-2 flex flex-col gap-1">
                    <label className="text-[10px] font-bold uppercase text-gray-400">Description</label>
                    <textarea
                      name="description"
                      value={formData.description}
                      onChange={handleChange}
                      className="min-h-[120px] w-full rounded-2xl border-2 border-gray-50 bg-gray-50/30 p-4 outline-none focus:border-[#c8a864]"
                      required
                    />
                  </div>
                  <div className="flex flex-col gap-1">
                    <label className="text-[10px] font-bold uppercase text-gray-400">Event Date</label>
                    <input
                      type="datetime-local"
                      name="eventDate"
                      value={normalizeDateTime(formData.eventDate)}
                      onChange={handleChange}
                      className="border-b-2 border-gray-100 p-2 outline-none focus:border-[#c8a864]"
                      required
                    />
                  </div>
                  <div className="flex flex-col gap-1">
                    <label className="text-[10px] font-bold uppercase text-gray-400">Responsible Person</label>
                    <input
                      name="responsiblePerson"
                      value={formData.responsiblePerson}
                      onChange={handleChange}
                      className="border-b-2 border-gray-100 p-2 outline-none focus:border-[#c8a864]"
                      required
                    />
                  </div>
                  <div className="col-span-2 flex flex-col gap-1">
                    <label className="text-[10px] font-bold uppercase text-gray-400">Location</label>
                    <input
                      name="location"
                      value={formData.location}
                      onChange={handleChange}
                      className="border-b-2 border-gray-100 p-2 outline-none focus:border-[#c8a864]"
                    />
                  </div>
                </div>
              </section>

              <div className="flex justify-end gap-6 border-t pt-8">
                <button
                  type="button"
                  onClick={closeModal}
                  className="text-xs font-bold uppercase tracking-widest text-gray-400 transition-colors hover:text-gray-600"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="rounded-2xl bg-[#c8a864] px-12 py-4 text-xs font-bold uppercase tracking-widest text-white shadow-xl shadow-yellow-600/20 transition-all hover:scale-105"
                >
                  Save Event
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
