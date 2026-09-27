import { Search, X } from 'lucide-react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import PublicHeader from '../components/PublicHeader';
import PhdCard from '../components/PhdCard';
import { useLanguage } from '../context/LanguageContext';
import { phdService } from '../api/phdService';

function SkeletonCard() {
  return (
    <div className="animate-pulse rounded-2xl bg-white p-6 shadow-[0_12px_30px_rgba(27,45,82,0.08)]">
      <div className="flex items-start gap-4">
        <div className="h-24 w-24 rounded-full bg-[#e9edf5]" />
        <div className="flex-1 space-y-3">
          <div className="h-6 w-3/4 rounded-full bg-[#e9edf5]" />
          <div className="h-4 w-1/3 rounded-full bg-[#eef2f8]" />
        </div>
      </div>
      <div className="mt-6 h-24 rounded-[1.5rem] bg-[#f2f4f8]" />
      <div className="mt-6 space-y-4">
        <div className="h-12 rounded-2xl bg-[#f2f4f8]" />
        <div className="h-12 rounded-2xl bg-[#f2f4f8]" />
        <div className="h-12 rounded-2xl bg-[#f2f4f8]" />
      </div>
      <div className="mt-6 h-12 rounded-full bg-[#f2f4f8]" />
    </div>
  );
}

function PublicationsModal({ student, copy, onClose }) {
  if (!student) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#122243]/60 p-4 backdrop-blur-sm">
      <div className="w-full max-w-2xl rounded-[2rem] bg-white p-8 shadow-2xl">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-xs font-black uppercase tracking-[0.2em] sm:tracking-[0.35em] text-[#c8a864]">{copy.modalEyebrow}</p>
            <h2 className="mt-3 font-serif text-3xl text-[#1b2d52]">{student.fullName || 'Doctoral Student'}</h2>
            <p className="mt-2 text-sm italic leading-7 text-[#61718c]">{student.thesisTopic}</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-full border border-[#d7ccb8] p-2 text-[#1b2d52] transition hover:bg-[#f8f6f1]"
            aria-label={copy.close}
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="mt-8">
          <p className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#97a4bc]">{copy.publicationsTitle}</p>
          {student.publications?.length > 0 ? (
            <div className="mt-4 space-y-3">
              {student.publications.map((publication) => (
                <div key={publication.id} className="rounded-[1.4rem] bg-[#f8f9fc] px-5 py-4">
                  <p className="text-base font-semibold text-[#1b2d52]">{publication.title}</p>
                  <p className="mt-1 text-xs font-black uppercase tracking-[0.18em] text-[#8794a9]">
                    {publication.publicationYear || copy.yearUnavailable}
                  </p>
                </div>
              ))}
            </div>
          ) : (
            <div className="mt-4 rounded-[1.4rem] border border-dashed border-[#d8deea] px-5 py-6 text-sm text-[#667791]">
              {copy.noPublications}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default function PhDDirectory() {
  const { language } = useLanguage();
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [selectedStudent, setSelectedStudent] = useState(null);

  const copy = {
    en: {
      eyebrow: 'Doctoral Research',
      title: 'PhD Student Directory',
      subtitle:
        'Browse MEDIATRON doctoral researchers through a public gallery focused on thesis topics, supervision, and research teams.',
      searchPlaceholder: 'Search by student name or research team',
      loading: 'Loading doctoral directory...',
      loadError: 'Unable to load doctoral student profiles.',
      empty: 'No students found matching your search.',
      totalSuffix: 'doctoral cards',
      activeBadge: 'Active Researcher',
      supervisorPrefix: 'Directed by',
      yearPrefix: 'Registered in',
      teamPrefix: 'Team:',
      notAvailable: 'Not available',
      viewPublications: 'View Publications',
      modalEyebrow: 'Sprint 2 Integration',
      publicationsTitle: 'Associated Publications',
      noPublications: 'No publications are linked to this doctoral profile yet.',
      yearUnavailable: 'Year unavailable',
      close: 'Close',
    },
    fr: {
      eyebrow: 'Recherche Doctorale',
      title: 'Annuaire des Doctorants',
      subtitle:
        'Parcourez les doctorants du MEDIATRON dans une galerie publique axee sur les sujets de these, l encadrement et les equipes de recherche.',
      searchPlaceholder: 'Rechercher par nom ou equipe de recherche',
      loading: 'Chargement de l annuaire doctoral...',
      loadError: 'Impossible de charger les profils des doctorants.',
      empty: 'Aucun doctorant ne correspond a votre recherche.',
      totalSuffix: 'cartes doctorales',
      activeBadge: 'Chercheur Actif',
      supervisorPrefix: 'Dirige par',
      yearPrefix: 'Inscrit en',
      teamPrefix: 'Equipe :',
      notAvailable: 'Non disponible',
      viewPublications: 'Voir les Publications',
      modalEyebrow: 'Integration Sprint 2',
      publicationsTitle: 'Publications Associees',
      noPublications: 'Aucune publication n est encore liee a ce profil doctoral.',
      yearUnavailable: 'Annee indisponible',
      close: 'Fermer',
    },
  }[language];

  const loadStudents = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await phdService.getAll();
      setStudents(response.data || []);
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
  }, [copy.loadError]);

  useEffect(() => {
    loadStudents().catch(() => {});
  }, [loadStudents]);

  useEffect(() => {
    const refresh = () => {
      loadStudents().catch(() => {});
    };

    const onStorage = (event) => {
      if (event.key === 'mediatron-phd-directory-version') {
        refresh();
      }
    };

    window.addEventListener('storage', onStorage);
    window.addEventListener('focus', refresh);
    window.addEventListener('mediatron-phd-directory-updated', refresh);

    return () => {
      window.removeEventListener('storage', onStorage);
      window.removeEventListener('focus', refresh);
      window.removeEventListener('mediatron-phd-directory-updated', refresh);
    };
  }, [loadStudents]);

  const filteredStudents = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) {
      return students;
    }

    return students.filter((student) =>
      [student.fullName, student.researchTeam].filter(Boolean).some((value) => String(value).toLowerCase().includes(query))
    );
  }, [search, students]);

  return (
    <div className="min-h-screen bg-[#f5f2ec] text-[#1b2d52]">
      <PublicHeader />
      <PublicationsModal student={selectedStudent} copy={copy} onClose={() => setSelectedStudent(null)} />

      <main className="mx-auto max-w-7xl px-6 py-10 sm:px-8 lg:px-12 xl:px-16">
        <section className="relative overflow-hidden rounded-[1.75rem] sm:rounded-[2.5rem] border border-[#e3dac9] bg-[radial-gradient(circle_at_top_right,_rgba(200,168,100,0.16),_transparent_34%),linear-gradient(145deg,#fefcf7_0%,#f0ebdf_100%)] px-6 py-8 shadow-[0_25px_70px_rgba(27,45,82,0.08)] sm:px-8 lg:px-12">
          <div className="absolute bottom-0 right-12 h-40 w-40 rounded-full bg-[#1b2d52]/7 blur-3xl" />
          <div className="relative flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
            <div className="max-w-3xl">
              <p className="text-xs font-black uppercase tracking-[0.22em] sm:tracking-[0.38em] text-[#b08a37]">{copy.eyebrow}</p>
              <h1 className="mt-4 font-serif text-4xl leading-tight sm:text-5xl">{copy.title}</h1>
              <p className="mt-4 max-w-2xl text-sm leading-7 text-[#5c6a84] sm:text-base">{copy.subtitle}</p>
            </div>
            <div className="rounded-full bg-white px-5 py-3 text-sm font-semibold text-[#1b2d52] shadow-sm">
              {filteredStudents.length} {copy.totalSuffix}
            </div>
          </div>
        </section>

        <section className="mt-8 rounded-[2rem] border border-[#e3dac9] bg-white p-6 shadow-lg">
          <label className="block">
            <span className="text-[11px] font-black uppercase tracking-[0.2em] sm:tracking-[0.35em] text-[#9aa6bf]">Search</span>
            <div className="mt-2 flex items-center gap-3 rounded-2xl border border-[#e5e9f0] bg-[#fbfcfe] px-4 py-3">
              <Search className="h-5 w-5 text-[#8b97ab]" />
              <input
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder={copy.searchPlaceholder}
                className="w-full bg-transparent text-sm outline-none placeholder:text-[#9aa6bf]"
              />
            </div>
          </label>
        </section>

        <section className="mt-10">
          {loading ? (
            <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
              {Array.from({ length: 6 }).map((_, index) => (
                <SkeletonCard key={`phd-skeleton-${index}`} />
              ))}
            </div>
          ) : error ? (
            <div className="rounded-[2rem] border border-rose-200 bg-rose-50 px-6 py-8 text-sm text-rose-700 shadow-lg">
              {error}
            </div>
          ) : filteredStudents.length === 0 ? (
            <div className="rounded-[2rem] border border-dashed border-[#d8deea] bg-white px-6 py-12 text-center text-sm text-[#667791] shadow-lg">
              {copy.empty}
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
              {filteredStudents.map((student) => (
                <PhdCard
                  key={student.id}
                  student={student}
                  copy={copy}
                  onViewPublications={setSelectedStudent}
                />
              ))}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
