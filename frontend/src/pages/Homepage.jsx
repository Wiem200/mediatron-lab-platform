import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import heroNetworkBackground from '../assets/home-hero-network.png';
import { eventService } from '../api/eventService';
import { projectService } from '../api/projectService';
import PublicHeader from '../components/PublicHeader';
import SubscribeSection from '../components/SubscribeSection';
import { useLanguage } from '../context/LanguageContext';
import { statisticsService } from '../api/statisticsService';

function formatEventDate(value, language) {
  if (!value) {
    return '-';
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleString(language === 'fr' ? 'fr-FR' : 'en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default function Homepage() {
  const { language } = useLanguage();
  const [events, setEvents] = useState([]);
  const [eventsLoading, setEventsLoading] = useState(true);
  const [projects, setProjects] = useState([]);
  const [projectsLoading, setProjectsLoading] = useState(true);
  const [statisticsSummary, setStatisticsSummary] = useState({
    publicationCount: 0,
    journalAndConferenceCount: 0,
    classifiedConferenceCount: 0
  });
  const [statsLoading, setStatsLoading] = useState(true);
  const numberFormatter = new Intl.NumberFormat(language === 'fr' ? 'fr-FR' : 'en-US');

  const copy = {
    en: {
      heroEyebrow: 'MEDIATRON Laboratory',
      heroTitle: 'Welcome to MEDIATRON',
      heroSubtitle:
        'A cutting-edge research laboratory dedicated to advancing media technology, digital innovation, and scientific excellence.',
      heroPanelLabel: 'Research Vision',
      heroPanelTitle: 'Cybersecurity research for today and tomorrow.',
      heroPanelDescription:
        'MEDIATRON brings together scientific rigor, publication activity, and applied digital research across communication systems and intelligent platforms.',
      learnMore: 'Learn More',
      eventsEyebrow: 'Approved Events',
      eventsTitle: 'Discover the latest validated MEDIATRON events right after the welcome overview.',
      eventsDescription:
        'This preview highlights approved seminars, conferences, and laboratory activities currently visible to visitors.',
      eventsButton: 'View All Events',
      eventsLoading: 'Loading approved events...',
      eventsEmpty: 'No approved events are available yet.',
      eventsLocationFallback: 'Location not specified',
      eventsResponsiblePrefix: 'Led by',
      projectsEyebrow: 'Research Projects',
      projectsTitle: 'Browse the MEDIATRON projects currently approved for public visibility.',
      projectsDescription:
        'This section highlights a selection of validated research projects, with quick access to their scientific scope, duration, and leadership.',
      projectsButton: 'View All Projects',
      projectsLoading: 'Loading approved projects...',
      projectsEmpty: 'No approved projects are available yet.',
      projectsLeadPrefix: 'Project Lead',
      projectsFundingPrefix: 'Funding',
      projectsDescriptionFallback: 'No project description is available yet.',
      projectsDetails: 'Open Project',
      footerContactTitle: 'Contact Information',
      footerAddressLabel: 'Address',
      footerAddressLine1: "SUP'COM - Ecole Superieure des Communications de Tunis",
      footerAddressLine2: 'Route de Raoued, Km 3.5',
      footerAddressLine3: '2083 Ariana, Tunisia',
      footerEmailLabel: 'Email',
      footerPhoneLabel: 'Phone',
      footerQuickLinksTitle: 'Quick Links',
      footerLocationTitle: 'Location',
      footerQuickLinks: [
        { to: '/about-mediatron', label: 'About Laboratory' },
        { to: '/statistics', label: 'Publications' },
        { to: '/members', label: 'Team Members' },
      ],
      footerSupcomLabel: "SUP'COM Website",
      footerMapTitle: "SUP'COM on Google Maps",
      footerCopyright: "© 2026 MEDIATRON Research Laboratory - SUP'COM. All rights reserved.",
      statisticsEyebrow: 'Dynamic Statistics',
      statisticsTitle: 'Track MEDIATRON publication activity through live analytics.',
      statisticsDescription:
        'Explore the public Grafana dashboards for publication trends, indexed journal versus conference distribution, and conference class breakdowns.',
      statisticsButton: 'Open Statistics Dashboard',
      statisticsCards: [
        {
          key: 'publicationCount',
          description: 'Total approved publications.'
        },
        {
          key: 'journalAndConferenceCount',
          description: 'Approved journal and conference publications.'
        },
        {
          key: 'classifiedConferenceCount',
          description: 'Approved classified conference publications.'
        }
      ]
    },
    fr: {
      heroEyebrow: 'Laboratoire MEDIATRON',
      heroTitle: 'Bienvenue a MEDIATRON',
      heroSubtitle:
        "Un laboratoire de recherche de pointe consacre aux technologies des medias, a l'innovation numerique et a l'excellence scientifique.",
      heroPanelLabel: 'Vision Scientifique',
      heroPanelTitle: 'Une recherche en cybersecurite pour aujourd hui et demain.',
      heroPanelDescription:
        'MEDIATRON rassemble rigueur scientifique, activite de publication et recherche numerique appliquee autour des systemes de communication et des plateformes intelligentes.',
      learnMore: 'En Savoir Plus',
      eventsEyebrow: 'Evenements Approuves',
      eventsTitle: 'Decouvrez les derniers evenements MEDIATRON valides juste apres la presentation du laboratoire.',
      eventsDescription:
        'Cet apercu met en avant les seminaires, conferences et activites du laboratoire deja approuves pour les visiteurs.',
      eventsButton: 'Voir Tous Les Evenements',
      eventsLoading: 'Chargement des evenements approuves...',
      eventsEmpty: 'Aucun evenement approuve n est disponible pour le moment.',
      eventsLocationFallback: 'Lieu non precise',
      eventsResponsiblePrefix: 'Anime par',
      projectsEyebrow: 'Projets de Recherche',
      projectsTitle: 'Parcourez les projets MEDIATRON actuellement approuves pour la visibilite publique.',
      projectsDescription:
        'Cette section met en avant une selection de projets de recherche valides avec un acces rapide a leur portee scientifique, leur duree et leur pilotage.',
      projectsButton: 'Voir Tous Les Projets',
      projectsLoading: 'Chargement des projets approuves...',
      projectsEmpty: 'Aucun projet approuve n est disponible pour le moment.',
      projectsLeadPrefix: 'Responsable du Projet',
      projectsFundingPrefix: 'Financement',
      projectsDescriptionFallback: 'Aucune description de projet n est disponible pour le moment.',
      projectsDetails: 'Ouvrir le Projet',
      footerContactTitle: 'Informations de Contact',
      footerAddressLabel: 'Adresse',
      footerAddressLine1: "SUP'COM - Ecole Superieure des Communications de Tunis",
      footerAddressLine2: 'Route de Raoued, Km 3.5',
      footerAddressLine3: '2083 Ariana, Tunisie',
      footerEmailLabel: 'Email',
      footerPhoneLabel: 'Telephone',
      footerQuickLinksTitle: 'Liens Rapides',
      footerLocationTitle: 'Localisation',
      footerQuickLinks: [
        { to: '/about-mediatron', label: 'A Propos du Laboratoire' },
        { to: '/statistics', label: 'Publications' },
        { to: '/members', label: "Membres de l Equipe" },
      ],
      footerSupcomLabel: "Site Web de SUP'COM",
      footerMapTitle: 'SUP COM sur Google Maps',
      footerCopyright: "© 2026 Laboratoire de Recherche MEDIATRON - SUP'COM. Tous droits reserves.",
      statisticsEyebrow: 'Statistiques Dynamiques',
      statisticsTitle: 'Suivez l activite de publication de MEDIATRON via des analyses en direct.',
      statisticsDescription:
        'Accedez aux tableaux publics Grafana pour consulter les tendances annuelles, la repartition journaux indexes versus conferences et la classification des conferences.',
      statisticsButton: 'Voir Les Statistiques',
      statisticsCards: [
        {
          key: 'publicationCount',
          description: 'Total des publications approuvees.'
        },
        {
          key: 'journalAndConferenceCount',
          description: 'Publications approuvees en journal et conference.'
        },
        {
          key: 'classifiedConferenceCount',
          description: 'Conferences approuvees avec classification.'
        }
      ]
    },
  }[language];
  const footerCopyrightText = (copy.footerCopyright || '').replace('Â©', '(c)').replace('©', '(c)');

  useEffect(() => {
    const loadEvents = async () => {
      try {
        const response = await eventService.getPublic();
        setEvents(response.data || []);
      } catch {
        setEvents([]);
      } finally {
        setEventsLoading(false);
      }
    };

    loadEvents();
  }, []);

  useEffect(() => {
    const loadSummary = async () => {
      try {
        const response = await statisticsService.getSummary();
        setStatisticsSummary(response.data || {
          publicationCount: 0,
          journalAndConferenceCount: 0,
          classifiedConferenceCount: 0
        });
      } catch {
        setStatisticsSummary({
          publicationCount: 0,
          journalAndConferenceCount: 0,
          classifiedConferenceCount: 0
        });
      } finally {
        setStatsLoading(false);
      }
    };

    loadSummary();
  }, []);

  useEffect(() => {
    const loadProjects = async () => {
      try {
        const response = await projectService.getPublic();
        setProjects(response.data || []);
      } catch {
        setProjects([]);
      } finally {
        setProjectsLoading(false);
      }
    };

    loadProjects();
  }, []);

  return (
    <div className="min-h-screen bg-[#f5f2ec] text-[#1b2d52]">
      <PublicHeader />

      <main>
        <section className="px-0 pb-6 pt-0">
          <div className="relative min-h-[calc(100vh-4.5rem)] overflow-hidden border-y border-[#e6dac6] px-8 py-16 text-white shadow-xl sm:px-10 lg:px-16 lg:py-24">
            <div
              className="absolute inset-0 bg-cover bg-center bg-no-repeat"
              style={{ backgroundImage: `url(${heroNetworkBackground})` }}
            />
            <div className="absolute inset-0 bg-[linear-gradient(135deg,_rgba(12,23,45,0.84)_0%,_rgba(20,39,72,0.7)_48%,_rgba(22,45,79,0.62)_100%)]" />
            <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,_rgba(255,255,255,0.08),_transparent_26%)]" />

            <div className="mx-auto flex min-h-[calc(100vh-10rem)] max-w-7xl items-center justify-center">
              <div className="relative z-10 max-w-5xl text-center">
                <p className="text-xs font-black uppercase tracking-[0.42em] text-[#f0d898]">{copy.heroEyebrow}</p>
                <h1 className="mt-5 font-serif text-4xl leading-tight sm:text-5xl lg:text-7xl">
                  {copy.heroTitle}
                </h1>
                <p className="mx-auto mt-6 max-w-4xl text-base leading-8 text-[#dbe3f1] sm:text-lg lg:text-xl">
                  {copy.heroSubtitle}
                </p>

                <div className="mx-auto mt-8 max-w-3xl rounded-[2rem] border border-white/12 bg-white/10 p-6 text-left shadow-[0_18px_40px_rgba(7,19,42,0.22)] backdrop-blur">
                  <p className="text-[11px] font-black uppercase tracking-[0.26em] text-[#f0d898]">
                    {copy.heroPanelLabel}
                  </p>
                  <h2 className="mt-3 font-serif text-2xl leading-tight text-white sm:text-3xl">
                    {copy.heroPanelTitle}
                  </h2>
                  <p className="mt-3 text-sm leading-7 text-[#dce8f7] sm:text-base">
                    {copy.heroPanelDescription}
                  </p>
                </div>

                <div className="mt-8 flex justify-center">
                  <Link
                    to="/about-mediatron"
                    className="inline-flex items-center rounded-2xl bg-[#f0d898] px-5 py-3 text-sm font-bold uppercase tracking-[0.18em] text-[#1b2d52] transition hover:bg-[#f4dfaa]"
                  >
                    {copy.learnMore}
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="mx-auto max-w-7xl px-6 py-6 lg:px-10" aria-labelledby="homepage-events-title">
          <div className="relative overflow-hidden rounded-[1.75rem] sm:rounded-[2.5rem] border border-[#e3dac9] bg-white px-8 py-10 shadow-[0_18px_45px_rgba(27,45,82,0.08)] lg:px-10 lg:py-12">
            <div className="absolute right-0 top-0 h-36 w-36 rounded-full bg-[#d8b56a]/10 blur-3xl" />
            <div className="relative">
              <p className="text-xs font-black uppercase tracking-[0.36em] text-[#b08a37]">{copy.eventsEyebrow}</p>
              <div className="mt-4 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
                <div>
                  <h2 id="homepage-events-title" className="max-w-3xl font-serif text-3xl leading-tight text-[#1b2d52] lg:text-4xl">
                    {copy.eventsTitle}
                  </h2>
                  <p className="mt-4 max-w-4xl text-sm leading-8 text-[#5f7397] sm:text-base">
                    {copy.eventsDescription}
                  </p>
                </div>
                <Link
                  to="/lab-events"
                  className="inline-flex items-center gap-3 rounded-full bg-[#1b2d52] px-6 py-3 text-sm font-black uppercase tracking-[0.16em] text-white transition hover:bg-[#2d4677]"
                >
                  {copy.eventsButton}
                  <svg viewBox="0 0 24 24" className="h-5 w-5 fill-none stroke-current stroke-[2]">
                    <path d="M5 12h14" />
                    <path d="m13 5 7 7-7 7" />
                  </svg>
                </Link>
              </div>
            </div>

            <div className="relative mt-10">
              {eventsLoading ? (
                <div className="rounded-[2rem] border border-[#e9edf4] bg-[#fbfcfe] px-6 py-10 text-center text-sm text-[#71809b]">
                  {copy.eventsLoading}
                </div>
              ) : events.length === 0 ? (
                <div className="rounded-[2rem] border border-dashed border-[#d8deea] bg-[#fbfcfe] px-6 py-10 text-center text-sm text-[#71809b]">
                  {copy.eventsEmpty}
                </div>
              ) : (
                <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
                  {events.slice(0, 3).map((event) => (
                    <article
                      key={event.id}
                      className="rounded-[2rem] border border-[#e6ebf2] bg-[linear-gradient(180deg,_#ffffff_0%,_#f8fbff_100%)] p-6 shadow-sm transition hover:-translate-y-1 hover:shadow-xl"
                    >
                      <div className="inline-flex rounded-full bg-[#eef5ff] px-3 py-1 text-[11px] font-black uppercase tracking-[0.2em] text-[#4875a6]">
                        {formatEventDate(event.eventDate, language)}
                      </div>
                      <h3 className="mt-4 font-serif text-2xl leading-tight text-[#1b2d52]">{event.title}</h3>
                      <p className="mt-3 text-sm leading-7 text-[#61718c]">{event.description}</p>
                      <div className="mt-5 space-y-2 text-sm text-[#5d6c84]">
                        <p>
                          <span className="font-semibold text-[#1b2d52]">{copy.eventsResponsiblePrefix}:</span>{' '}
                          {event.responsiblePerson || '-'}
                        </p>
                        <p>
                          <span className="font-semibold text-[#1b2d52]">Location:</span>{' '}
                          {event.location || copy.eventsLocationFallback}
                        </p>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </div>
          </div>
        </section>

        <section className="mx-auto max-w-7xl px-6 py-6 lg:px-10" aria-labelledby="statistics-preview-title">
          <div className="relative overflow-hidden rounded-[1.75rem] sm:rounded-[2.5rem] border border-[#d9e4f0] bg-[linear-gradient(180deg,_#f8fbff_0%,_#eef4fb_55%,_#f8fbff_100%)] px-8 py-10 shadow-[0_18px_45px_rgba(27,45,82,0.08)] lg:px-10 lg:py-12">
            <div className="absolute inset-x-0 top-[28%] h-px bg-[linear-gradient(90deg,_transparent,_rgba(21,93,162,0.18),_transparent)]" />
            <div className="absolute inset-x-0 top-[64%] h-px bg-[linear-gradient(90deg,_transparent,_rgba(21,93,162,0.12),_transparent)]" />
            <div className="absolute inset-y-0 left-[24%] w-px bg-[linear-gradient(180deg,_transparent,_rgba(21,93,162,0.08),_transparent)]" />
            <div className="absolute inset-y-0 left-[50%] w-px bg-[linear-gradient(180deg,_transparent,_rgba(21,93,162,0.08),_transparent)]" />
            <div className="absolute inset-y-0 left-[76%] w-px bg-[linear-gradient(180deg,_transparent,_rgba(21,93,162,0.08),_transparent)]" />

            <div className="relative">
              <p className="text-xs font-black uppercase tracking-[0.36em] text-[#4c7cb5]">
                {copy.statisticsEyebrow}
              </p>
              <h2 id="statistics-preview-title" className="mt-4 max-w-3xl font-serif text-3xl leading-tight text-[#1b2d52] lg:text-4xl">
                {copy.statisticsTitle}
              </h2>
              <p className="mt-5 max-w-4xl text-sm leading-8 text-[#5f7397] sm:text-base">
                {copy.statisticsDescription}
              </p>
            </div>

            <div className="relative mt-12 grid gap-10 lg:grid-cols-3">
              {copy.statisticsCards.map((item, index) => (
                <article key={item.key} className="flex flex-col items-center text-center">
                  <div className="flex h-56 w-56 items-center justify-center rounded-full border border-[#9fc8ee] bg-[radial-gradient(circle_at_top,_#ffffff_0%,_#f6fbff_48%,_#e4f0fb_100%)] p-8 shadow-[0_18px_35px_rgba(32,95,156,0.12)]">
                    <div>
                      <p className="text-[11px] font-black uppercase tracking-[0.28em] text-[#7a91b3]">
                        Stat {String(index + 1).padStart(2, '0')}
                      </p>
                      <p className="mt-4 font-serif text-5xl leading-none text-[#155da2] drop-shadow-[0_4px_10px_rgba(64,185,255,0.35)] sm:text-6xl">
                        {statsLoading ? '...' : numberFormatter.format(statisticsSummary[item.key] || 0)}
                      </p>
                    </div>
                  </div>
                  <div className="mt-6 w-full max-w-xs border-t border-[#40b9ff] pt-4">
                    <p className="text-sm leading-7 text-[#465c7d]">
                      {item.description}
                    </p>
                  </div>
                </article>
              ))}
            </div>

            <div className="relative mt-12 flex justify-center lg:justify-end">
              <Link
                to="/statistics"
                className="inline-flex items-center gap-3 rounded-full bg-[#155da2] px-6 py-3 text-sm font-black uppercase tracking-[0.16em] text-white shadow-[0_14px_32px_rgba(21,93,162,0.22)] transition hover:bg-[#0f4f8d]"
              >
                {copy.statisticsButton}
                <svg viewBox="0 0 24 24" className="h-5 w-5 fill-none stroke-current stroke-[2]">
                  <path d="M5 12h14" />
                  <path d="m13 5 7 7-7 7" />
                </svg>
              </Link>
            </div>
          </div>
        </section>

        <section className="mx-auto max-w-7xl px-6 py-6 lg:px-10" aria-labelledby="homepage-projects-title">
          <div className="relative overflow-hidden rounded-[1.75rem] sm:rounded-[2.5rem] border border-[#dfe5ee] bg-[linear-gradient(180deg,_#ffffff_0%,_#f8fbff_100%)] px-8 py-10 shadow-[0_18px_45px_rgba(27,45,82,0.08)] lg:px-10 lg:py-12">
            <div className="absolute left-0 top-0 h-40 w-40 rounded-full bg-[#1b2d52]/6 blur-3xl" />
            <div className="absolute bottom-0 right-0 h-40 w-40 rounded-full bg-[#d8b56a]/10 blur-3xl" />

            <div className="relative">
              <p className="text-xs font-black uppercase tracking-[0.36em] text-[#4c7cb5]">{copy.projectsEyebrow}</p>
              <div className="mt-4 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
                <div>
                  <h2 id="homepage-projects-title" className="max-w-3xl font-serif text-3xl leading-tight text-[#1b2d52] lg:text-4xl">
                    {copy.projectsTitle}
                  </h2>
                  <p className="mt-4 max-w-4xl text-sm leading-8 text-[#5f7397] sm:text-base">
                    {copy.projectsDescription}
                  </p>
                </div>
                <Link
                  to="/research-projects"
                  className="inline-flex items-center gap-3 rounded-full bg-[#1b2d52] px-6 py-3 text-sm font-black uppercase tracking-[0.16em] text-white transition hover:bg-[#2d4677]"
                >
                  {copy.projectsButton}
                  <svg viewBox="0 0 24 24" className="h-5 w-5 fill-none stroke-current stroke-[2]">
                    <path d="M5 12h14" />
                    <path d="m13 5 7 7-7 7" />
                  </svg>
                </Link>
              </div>
            </div>

            <div className="relative mt-10">
              {projectsLoading ? (
                <div className="rounded-[2rem] border border-[#e9edf4] bg-[#fbfcfe] px-6 py-10 text-center text-sm text-[#71809b]">
                  {copy.projectsLoading}
                </div>
              ) : projects.length === 0 ? (
                <div className="rounded-[2rem] border border-dashed border-[#d8deea] bg-[#fbfcfe] px-6 py-10 text-center text-sm text-[#71809b]">
                  {copy.projectsEmpty}
                </div>
              ) : (
                <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
                  {projects.slice(0, 3).map((project) => (
                    <article
                      key={project.id}
                      className="rounded-[2rem] border border-[#e6ebf2] bg-white p-6 shadow-sm transition hover:-translate-y-1 hover:shadow-xl"
                    >
                      <div className="inline-flex rounded-full bg-[#eef5ff] px-3 py-1 text-[11px] font-black uppercase tracking-[0.2em] text-[#4875a6]">
                        {project.duration || '-'}
                      </div>
                      <h3 className="mt-4 font-serif text-2xl leading-tight text-[#1b2d52]">{project.title}</h3>
                      <p className="mt-3 text-sm leading-7 text-[#61718c]">
                        {project.description?.trim()
                          ? `${project.description.slice(0, 160)}${project.description.length > 160 ? '...' : ''}`
                          : copy.projectsDescriptionFallback}
                      </p>
                      <div className="mt-5 space-y-2 text-sm text-[#5d6c84]">
                        <p>
                          <span className="font-semibold text-[#1b2d52]">{copy.projectsLeadPrefix}:</span>{' '}
                          {project.responsibleName || project.responsibleMemberName || '-'}
                        </p>
                        <p>
                          <span className="font-semibold text-[#1b2d52]">{copy.projectsFundingPrefix}:</span>{' '}
                          {project.funding || '-'}
                        </p>
                      </div>
                      <div className="mt-6">
                        <Link
                          to={`/research-projects/${project.id}`}
                          className="inline-flex items-center gap-2 text-sm font-bold uppercase tracking-[0.14em] text-[#155da2] transition hover:text-[#0f4f8d]"
                        >
                          {copy.projectsDetails}
                          <svg viewBox="0 0 24 24" className="h-4 w-4 fill-none stroke-current stroke-[2]">
                            <path d="M5 12h14" />
                            <path d="m13 5 7 7-7 7" />
                          </svg>
                        </Link>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </div>
          </div>
        </section>

        <SubscribeSection />

        <section className="px-0 pb-0 pt-6" aria-labelledby="homepage-footer-title">
          <div className="relative overflow-hidden border-y border-[#243868] bg-[linear-gradient(135deg,_#152646_0%,_#1b2d52_55%,_#233a68_100%)] px-8 py-10 text-white shadow-[0_24px_60px_rgba(16,25,46,0.28)] sm:px-10 lg:px-16 lg:py-12">
            <div className="absolute -right-12 -top-16 h-56 w-56 rounded-full bg-[#c8a864]/12 blur-3xl" />
            <div className="absolute -bottom-20 -left-10 h-64 w-64 rounded-full bg-[#c8a864]/10 blur-3xl" />
            <div className="absolute inset-x-0 top-28 h-px bg-[linear-gradient(90deg,_transparent,_rgba(200,168,100,0.28),_transparent)]" />

            <div className="relative mx-auto max-w-7xl">
              <div className="mb-10 max-w-3xl">
                <div className="inline-flex items-center gap-3 rounded-full border border-[#c8a864]/25 bg-white/8 px-4 py-2 text-[11px] font-black uppercase tracking-[0.28em] text-[#e8cd90] backdrop-blur">
                  <span className="h-2 w-2 rounded-full bg-[#c8a864]" />
                  MEDIATRON Contact Desk
                </div>
                <h2 id="homepage-footer-title" className="mt-5 font-serif text-3xl leading-tight text-white lg:text-5xl">
                  Stay connected with the laboratory.
                </h2>
                <p className="mt-4 max-w-2xl text-sm leading-8 text-[#cad4e8] sm:text-base">
                  Inspired by the login page, this closing block uses MEDIATRON&apos;s secure-access palette with dark navy surfaces, warm gold accents, and softer premium cards.
                </p>
              </div>

              <div className="grid gap-8 xl:grid-cols-[1.15fr_0.8fr_1.2fr]">
              <div className="rounded-[2rem] border border-white/10 bg-white/6 p-6 backdrop-blur-sm">
                <div className="flex items-center gap-3">
                  <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-[linear-gradient(135deg,_#c8a864,_#e8cd90)] text-[#1b2d52] shadow-lg">
                    <svg viewBox="0 0 24 24" className="h-6 w-6 fill-none stroke-current stroke-[2]">
                      <path d="M12 21s7-4.35 7-11a7 7 0 1 0-14 0c0 6.65 7 11 7 11Z" />
                      <circle cx="12" cy="10" r="2.5" />
                    </svg>
                  </div>
                  <h3 className="text-2xl font-serif text-white lg:text-3xl">{copy.footerContactTitle}</h3>
                </div>

                <div className="mt-6 space-y-8">
                  <div>
                    <p className="text-[11px] font-black uppercase tracking-[0.24em] text-[#e8cd90]">{copy.footerAddressLabel}</p>
                    <div className="mt-4 flex gap-4 text-base leading-8 text-[#d7deed]">
                      <svg viewBox="0 0 24 24" className="mt-1 h-5 w-5 shrink-0 fill-none stroke-[#c8a864] stroke-[2]">
                        <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
                        <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 0 4 24V4.5A2.5 2.5 0 0 1 6.5 2Z" />
                        <path d="M8 7h8" />
                        <path d="M8 11h8" />
                      </svg>
                      <div>
                        <p>{copy.footerAddressLine1}</p>
                        <p>{copy.footerAddressLine2}</p>
                        <p>{copy.footerAddressLine3}</p>
                      </div>
                    </div>
                  </div>

                  <div>
                    <p className="text-[11px] font-black uppercase tracking-[0.24em] text-[#e8cd90]">{copy.footerEmailLabel}</p>
                    <div className="mt-4 flex items-center gap-4 text-base text-[#d7deed]">
                      <svg viewBox="0 0 24 24" className="h-5 w-5 shrink-0 fill-none stroke-[#c8a864] stroke-[2]">
                        <rect x="3" y="5" width="18" height="14" rx="2" />
                        <path d="m3 7 9 6 9-6" />
                      </svg>
                      <a href="mailto:mediatron.contact@supcom.tn" className="transition hover:text-[#e8cd90]">
                        mediatron.contact@supcom.tn
                      </a>
                    </div>
                  </div>

                  <div>
                    <p className="text-[11px] font-black uppercase tracking-[0.24em] text-[#e8cd90]">{copy.footerPhoneLabel}</p>
                    <div className="mt-4 space-y-4 text-base text-[#d7deed]">
                      <div className="flex items-center gap-4">
                        <svg viewBox="0 0 24 24" className="h-5 w-5 shrink-0 fill-none stroke-[#c8a864] stroke-[2]">
                          <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.8 19.8 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.12 4.18 2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.7 12.7 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.7 12.7 0 0 0 2.81.7A2 2 0 0 1 22 16.92Z" />
                        </svg>
                        <a href="tel:+21671857000" className="transition hover:text-[#e8cd90]">+216 71 857 000</a>
                      </div>
                      <div className="flex items-center gap-4">
                        <svg viewBox="0 0 24 24" className="h-5 w-5 shrink-0 fill-none stroke-[#c8a864] stroke-[2]">
                          <rect x="7" y="2" width="10" height="20" rx="2" />
                          <path d="M11 18h2" />
                        </svg>
                        <span>+216 71 857 001 (Fax)</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div className="rounded-[2rem] border border-white/10 bg-white/6 p-6 backdrop-blur-sm">
                <div className="flex items-center gap-3">
                  <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-[linear-gradient(135deg,_#c8a864,_#e8cd90)] text-[#1b2d52] shadow-lg">
                    <svg viewBox="0 0 24 24" className="h-6 w-6 fill-none stroke-current stroke-[2]">
                      <path d="M10 13a5 5 0 0 0 7.54.46l2.5-2.5a5 5 0 0 0-7.07-7.07L11.53 5.3" />
                      <path d="M14 11a5 5 0 0 0-7.54-.46l-2.5 2.5a5 5 0 0 0 7.07 7.07l1.43-1.43" />
                    </svg>
                  </div>
                  <h3 className="text-2xl font-serif text-white lg:text-3xl">{copy.footerQuickLinksTitle}</h3>
                </div>

                <div className="mt-8 space-y-4 text-lg text-[#f4f0e6]">
                  {copy.footerQuickLinks.map((item) => (
                    <Link
                      key={item.to}
                      to={item.to}
                      className="flex items-center gap-3 rounded-2xl border border-transparent px-3 py-3 transition hover:border-white/10 hover:bg-white/8 hover:text-[#e8cd90]"
                    >
                      <svg viewBox="0 0 24 24" className="h-5 w-5 fill-none stroke-current stroke-[2]">
                        <path d="m9 18 6-6-6-6" />
                      </svg>
                      <span>{item.label}</span>
                    </Link>
                  ))}
                  <a
                    href="https://www.supcom.tn/"
                    target="_blank"
                    rel="noreferrer"
                    className="flex items-center gap-3 rounded-2xl border border-transparent px-3 py-3 transition hover:border-white/10 hover:bg-white/8 hover:text-[#e8cd90]"
                  >
                    <svg viewBox="0 0 24 24" className="h-5 w-5 fill-none stroke-current stroke-[2]">
                      <path d="m9 18 6-6-6-6" />
                    </svg>
                    <span>{copy.footerSupcomLabel}</span>
                  </a>
                </div>
              </div>

              <div className="rounded-[2rem] border border-white/10 bg-white/6 p-6 backdrop-blur-sm">
                <div className="flex items-center gap-3">
                  <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-[linear-gradient(135deg,_#c8a864,_#e8cd90)] text-[#1b2d52] shadow-lg">
                    <svg viewBox="0 0 24 24" className="h-6 w-6 fill-none stroke-current stroke-[2]">
                      <path d="M3 6l6-2 6 2 6-2v14l-6 2-6-2-6 2V6Z" />
                      <path d="M9 4v14" />
                      <path d="M15 6v14" />
                    </svg>
                  </div>
                  <h3 className="text-2xl font-serif text-white lg:text-3xl">{copy.footerLocationTitle}</h3>
                </div>

                <div className="mt-6 overflow-hidden rounded-[1.8rem] border border-white/15 bg-white shadow-[0_18px_40px_rgba(7,19,42,0.22)]">
                  <iframe
                    title={copy.footerMapTitle}
                    src="https://www.google.com/maps?q=SUPCOM%20Tunis&z=15&output=embed"
                    className="h-[380px] w-full border-0"
                    loading="lazy"
                    referrerPolicy="no-referrer-when-downgrade"
                  />
                </div>
              </div>
            </div>

            <div className="relative mt-10 border-t border-white/10 pt-8 text-center text-sm font-medium tracking-[0.08em] text-[#d7deed]">
              {footerCopyrightText}
            </div>
          </div>
          </div>
        </section>
      </main>
    </div>
  );
}
