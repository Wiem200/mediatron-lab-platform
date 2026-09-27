import { useEffect, useMemo, useState } from 'react';
import ProjectsGrid from '../components/ProjectsGrid';
import PublicHeader from '../components/PublicHeader';
import { useLanguage } from '../context/LanguageContext';
import { projectService } from '../api/projectService';

export default function ProjectsList() {
  const { language } = useLanguage();
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const copy = {
    en: {
      emptyDescription: 'No description available.',
      loadError: 'Unable to load research projects.',
      eyebrow: 'Research Projects',
      title: "Explore MEDIATRON's scientific portfolio, from active grants to completed laboratory initiatives.",
      subtitle:
        'Each project card opens a detailed view with leadership, contributing teams, doctoral researchers, and resource links for deeper exploration.',
      directory: 'Project Directory',
      directorySubtitle: "Public overview of the laboratory's research activities.",
      countSuffix: 'projects',
      loading: 'Loading research projects...',
      empty: 'No research projects are available in the public directory yet.',
      internal: 'Internal',
      duration: 'Duration',
      team: 'Team',
      contributors: 'contributors',
      lead: 'Project Lead',
      details: 'View Details',
    },
    fr: {
      emptyDescription: 'Aucune description disponible.',
      loadError: "Impossible de charger les projets de recherche.",
      eyebrow: 'Projets de Recherche',
      title: 'Explorez le portefeuille scientifique de MEDIATRON, des projets actifs aux initiatives deja achevees du laboratoire.',
      subtitle:
        'Chaque carte de projet ouvre une vue detaillee avec la direction scientifique, les equipes contributrices, les doctorants et les ressources associees.',
      directory: 'Repertoire des Projets',
      directorySubtitle: "Vue publique des activites de recherche du laboratoire.",
      countSuffix: 'projets',
      loading: 'Chargement des projets de recherche...',
      empty: "Aucun projet de recherche n'est encore disponible dans le repertoire public.",
      internal: 'Interne',
      duration: 'Duree',
      team: 'Equipe',
      contributors: 'contributeurs',
      lead: 'Responsable du Projet',
      details: 'Voir les Details',
    },
  }[language];

  useEffect(() => {
    const load = async () => {
      try {
        const response = await projectService.getPublic();
        setProjects(response.data || []);
      } catch (err) {
        setError(err?.response?.data?.message || copy.loadError);
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [copy.loadError]);

  const featuredKeywords = useMemo(() => {
    const values = new Set();
    projects.forEach((project) => {
      (project.keywords || []).forEach((keyword) => values.add(keyword));
    });
    return Array.from(values).slice(0, 8);
  }, [projects]);

  return (
    <div className="min-h-screen bg-[#f5f2ec]">
      <PublicHeader />
      <main className="mx-auto max-w-7xl px-6 py-10 lg:px-10">
        <section className="relative overflow-hidden rounded-[1.75rem] sm:rounded-[2.5rem] border border-[#e6dac6] bg-[radial-gradient(circle_at_top_right,_rgba(200,168,100,0.18),_transparent_32%),linear-gradient(135deg,_#1b2d52_0%,_#274071_100%)] p-8 text-white shadow-xl lg:p-12">
          <div className="max-w-3xl">
            <p className="mb-4 text-xs font-black uppercase tracking-[0.24em] sm:tracking-[0.4em] text-[#f0d898]">{copy.eyebrow}</p>
            <h1 className="max-w-2xl font-serif text-4xl leading-tight lg:text-5xl">{copy.title}</h1>
            <p className="mt-5 max-w-2xl text-sm leading-7 text-[#dbe3f1]">{copy.subtitle}</p>
          </div>

          {featuredKeywords.length > 0 && (
            <div className="mt-8 flex flex-wrap gap-2">
              {featuredKeywords.map((keyword) => (
                <span
                  key={keyword}
                  className="rounded-full border border-white/15 bg-white/10 px-4 py-2 text-xs font-semibold uppercase tracking-[0.18em] text-[#f7f1e4]"
                >
                  {keyword}
                </span>
              ))}
            </div>
          )}
        </section>

        <section className="mt-10">
          <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <h2 className="font-serif text-3xl text-[#1b2d52]">{copy.directory}</h2>
              <p className="mt-2 text-sm text-[#6a7891]">{copy.directorySubtitle}</p>
            </div>
            <div className="rounded-full bg-white px-4 py-2 text-sm font-semibold text-[#1b2d52] shadow-sm">
              {projects.length} {copy.countSuffix}
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
          ) : projects.length === 0 ? (
            <div className="rounded-[2rem] border border-dashed border-[#d8dfeb] bg-white/70 px-6 py-12 text-center text-sm text-[#71809b]">
              {copy.empty}
            </div>
          ) : (
            <ProjectsGrid projects={projects} copy={copy} />
          )}
        </section>
      </main>
    </div>
  );
}
