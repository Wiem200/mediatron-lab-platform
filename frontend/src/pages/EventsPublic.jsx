import { useEffect, useState } from 'react';
import PublicHeader from '../components/PublicHeader';
import { useLanguage } from '../context/LanguageContext';
import { eventService } from '../api/eventService';

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

export default function EventsPublic() {
  const { language } = useLanguage();
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const copy = {
    en: {
      loadError: 'Unable to load laboratory events.',
      eyebrow: 'Laboratory Events',
      title: 'Approved scientific events, seminars, and institutional activities from MEDIATRON.',
      subtitle: 'This public agenda only displays events that passed Lab Head validation.',
      sectionTitle: 'Approved Events',
      sectionSubtitle: 'Visible to visitors after scientific and institutional approval.',
      countSuffix: 'events',
      loading: 'Loading approved events...',
      empty: 'No approved events are available yet.',
      schedule: 'Schedule',
      responsible: 'Responsible Person',
    },
    fr: {
      loadError: "Impossible de charger les evenements du laboratoire.",
      eyebrow: 'Evenements du Laboratoire',
      title: 'Evenements scientifiques, seminaires et activites institutionnelles approuves par MEDIATRON.',
      subtitle: "Cet agenda public n'affiche que les evenements ayant passe la validation du chef de laboratoire.",
      sectionTitle: 'Evenements Approuves',
      sectionSubtitle: 'Visibles aux visiteurs apres validation scientifique et institutionnelle.',
      countSuffix: 'evenements',
      loading: 'Chargement des evenements approuves...',
      empty: "Aucun evenement approuve n'est disponible pour le moment.",
      schedule: 'Calendrier',
      responsible: 'Responsable',
    },
  }[language];

  useEffect(() => {
    const load = async () => {
      try {
        const response = await eventService.getPublic();
        setEvents(response.data || []);
      } catch (err) {
        const data = err?.response?.data;
        const message =
          data?.message ||
          data?.error ||
          (typeof data === 'string' && data) ||
          err?.message ||
          copy.loadError;
        setError(message);
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [copy.loadError]);

  return (
    <div className="min-h-screen bg-[#f5f2ec]">
      <PublicHeader />

      <main className="mx-auto max-w-7xl px-6 py-10 lg:px-10">
        <section className="rounded-[1.75rem] border border-[#e6dac6] bg-[radial-gradient(circle_at_top_right,_rgba(200,168,100,0.18),_transparent_32%),linear-gradient(135deg,_#1b2d52_0%,_#274071_100%)] p-8 text-white shadow-xl sm:rounded-[2.5rem] lg:p-12">
          <p className="mb-4 text-xs font-black uppercase tracking-[0.24em] text-[#f0d898] sm:tracking-[0.4em]">
            {copy.eyebrow}
          </p>

          <h1 className="max-w-3xl font-serif text-3xl leading-tight sm:text-4xl lg:text-5xl">
            {copy.title}
          </h1>

          <p className="mt-5 max-w-2xl text-sm leading-7 text-[#dbe3f1]">
            {copy.subtitle}
          </p>
        </section>

        <section className="mt-10">
          <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <h2 className="font-serif text-3xl text-[#1b2d52]">{copy.sectionTitle}</h2>
              <p className="mt-2 text-sm text-[#6a7891]">{copy.sectionSubtitle}</p>
            </div>

            <div className="w-fit rounded-full bg-white px-4 py-2 text-sm font-semibold text-[#1b2d52] shadow-sm">
              {events.length} {copy.countSuffix}
            </div>
          </div>

          {loading ? (
            <div className="rounded-[2rem] border border-[#e3dac9] bg-white px-6 py-12 text-center text-sm text-[#71809b] shadow-lg">
              {copy.loading}
            </div>
          ) : error ? (
            <div className="rounded-[2rem] border border-rose-200 bg-rose-50 px-6 py-10 text-sm text-rose-700">
              {error}
            </div>
          ) : events.length === 0 ? (
            <div className="rounded-[2rem] border border-dashed border-[#d8dfeb] bg-white/70 px-6 py-12 text-center text-sm text-[#71809b]">
              {copy.empty}
            </div>
          ) : (
            <div className="grid grid-cols-1 items-stretch gap-6 lg:grid-cols-2 xl:grid-cols-3">
              {events.map((event) => (
                <article
                  key={event.id}
                  className="flex h-full flex-col rounded-[2rem] border border-[#e3dac9] bg-white p-6 shadow-lg transition hover:-translate-y-1 hover:shadow-xl"
                >
                  {event.location ? (
                    <div className="mb-4 flex min-w-0 flex-wrap gap-2">
                      <span className="inline-flex max-w-full items-center rounded-full bg-[#fff3d8] px-3 py-1 text-[10px] font-black uppercase leading-snug tracking-[0.12em] text-[#a07a17] sm:text-[11px]">
                        {event.location}
                      </span>
                    </div>
                  ) : null}

                  <h3 className="font-serif text-2xl leading-tight text-[#1b2d52]">
                    {event.title}
                  </h3>

                  <p className="mt-4 text-sm leading-7 text-[#61718c]">
                    {event.description}
                  </p>

                  <div className="mt-auto pt-6">
                    <div className="grid grid-cols-1 gap-3 rounded-3xl bg-[#f8f9fc] p-4 text-sm text-[#51617d]">
                      <div>
                        <p className="text-[10px] font-black uppercase tracking-[0.16em] text-[#98a4bb] sm:text-[11px] sm:tracking-[0.22em]">
                          {copy.schedule}
                        </p>
                        <p className="mt-2 font-semibold text-[#1b2d52]">
                          {formatEventDate(event.eventDate)}
                        </p>
                      </div>

                      <div>
                        <p className="text-[10px] font-black uppercase tracking-[0.16em] text-[#98a4bb] sm:text-[11px] sm:tracking-[0.22em]">
                          {copy.responsible}
                        </p>
                        <p className="mt-2 font-semibold text-[#1b2d52]">
                          {event.responsiblePerson || '-'}
                        </p>
                      </div>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
