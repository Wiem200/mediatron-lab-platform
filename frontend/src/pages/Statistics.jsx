import { useEffect, useMemo, useState } from 'react';
import PublicHeader from '../components/PublicHeader';
import GrafanaPanelFrame from '../components/GrafanaPanelFrame';
import { useLanguage } from '../context/LanguageContext';
import { memberPublicService } from '../api/memberPublicService';

const grafanaBaseUrl = (import.meta.env.VITE_GRAFANA_PUBLIC_URL || 'http://localhost:3000').replace(/\/$/, '');
const dashboardUid = import.meta.env.VITE_GRAFANA_DASHBOARD_UID || 'mediatron-publication-statistics';
const dashboardSlug = 'mediatron-publication-statistics';

const REGULAR_PANEL_HEIGHT = 360;
const HEATMAP_PANEL_HEIGHT = 420;
const ALL_MEMBERS_VALUE = '%';

function buildPanelUrl(panelId, selectedMember = ALL_MEMBERS_VALUE) {
  const url = new URL(`${grafanaBaseUrl}/d-solo/${dashboardUid}/${dashboardSlug}`);

  url.searchParams.set('orgId', '1');
  url.searchParams.set('panelId', String(panelId));
  url.searchParams.set('theme', 'light');
  url.searchParams.set('from', 'now-10y');
  url.searchParams.set('to', 'now');
  url.searchParams.set('refresh', '24h');
  url.searchParams.set('kiosk', '');

  if (Number(panelId) === 4) {
    url.searchParams.set('var-member', selectedMember || ALL_MEMBERS_VALUE);
  }

  return url.toString();
}

function normalizeMemberName(member) {
  const firstName = member?.firstName || member?.first_name || '';
  const lastName = member?.lastName || member?.last_name || '';
  const fullName = `${firstName} ${lastName}`.replace(/\s+/g, ' ').trim();

  return fullName || member?.name || member?.fullName || member?.full_name || '';
}

export default function Statistics() {
  const { language } = useLanguage();

  const [members, setMembers] = useState([]);
  const [selectedMember, setSelectedMember] = useState(ALL_MEMBERS_VALUE);
  const [membersLoading, setMembersLoading] = useState(true);

  const copy = {
    en: {
      eyebrow: 'Statistics',
      title: 'MEDIATRON publication statistics',
      subtitle:
        'Explore clean public analytics about the laboratory research production, powered by live Grafana panels connected to PostgreSQL.',
      introTitle: 'Public analytics overview',
      introDescription:
        'The charts below show publication evolution, journal and conference distribution, conference classification, member evolution, and yearly density without exposing the full Grafana editing interface.',
      allMembers: 'All members',
      memberFilterLabel: 'Member filter',
      loadingMembers: 'Loading members...',
      panelDescriptions: {
        yearly: 'Tracks the yearly volume of approved MEDIATRON publications.',
        distribution: 'Shows the relative distribution between journal publications and conference publications.',
        classification: 'Breaks conference publications into A, B, C, and National categories.',
        evolution: 'Compares publication evolution by laboratory member over time.',
        heatmap: 'Highlights publication density by member and by year.',
      },
    },
    fr: {
      eyebrow: 'Statistiques',
      title: 'Statistiques des publications MEDIATRON',
      subtitle:
        'Explorez des analyses publiques propres sur la production scientifique du laboratoire, avec des panels Grafana connectes a PostgreSQL.',
      introTitle: 'Vue analytique publique',
      introDescription:
        "Les graphiques ci-dessous presentent l'evolution des publications, la repartition journaux/conferences, la classification des conferences, l'evolution par membre et la densite annuelle sans afficher l'interface complete de Grafana.",
      allMembers: 'Tous les membres',
      memberFilterLabel: 'Filtre membre',
      loadingMembers: 'Chargement des membres...',
      panelDescriptions: {
        yearly: 'Suit le volume annuel des publications approuvees de MEDIATRON.',
        distribution: 'Affiche la repartition entre publications de journaux et publications de conferences.',
        classification: 'Classe les conferences selon les categories A, B, C et Nationale.',
        evolution: "Compare l'evolution des publications par membre du laboratoire.",
        heatmap: 'Met en evidence la densite des publications par membre et par annee.',
      },
    },
  }[language];

  useEffect(() => {
    const loadMembers = async () => {
      setMembersLoading(true);

      try {
        const response = await memberPublicService.getMembers();
        const rawMembers = Array.isArray(response.data) ? response.data : [];

        const normalizedMembers = rawMembers
          .map((member) => ({
            id: member.id || normalizeMemberName(member),
            name: normalizeMemberName(member),
          }))
          .filter((member) => member.name)
          .sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }));

        setMembers(normalizedMembers);
      } catch (error) {
        console.error('Failed to load public members for statistics filter:', error);
        setMembers([]);
      } finally {
        setMembersLoading(false);
      }
    };

    loadMembers();
  }, []);

  const panels = useMemo(() => [
    {
      id: 1,
      title: 'Publication Count Per Year',
      description: copy.panelDescriptions.yearly,
      height: REGULAR_PANEL_HEIGHT,
      span: 'xl:col-span-1',
    },
    {
      id: 2,
      title: 'Journal Vs Conference Distribution',
      description: copy.panelDescriptions.distribution,
      height: REGULAR_PANEL_HEIGHT,
      span: 'xl:col-span-1',
    },
    {
      id: 3,
      title: 'Conference Classification Breakdown',
      description: copy.panelDescriptions.classification,
      height: REGULAR_PANEL_HEIGHT,
      span: 'xl:col-span-1',
    },
    {
      id: 4,
      title: 'Evolution By Member',
      description: copy.panelDescriptions.evolution,
      height: REGULAR_PANEL_HEIGHT,
      span: 'xl:col-span-1',
    },
    {
      id: 5,
      title: 'Publication Heatmap By Member And Year',
      description: copy.panelDescriptions.heatmap,
      height: HEATMAP_PANEL_HEIGHT,
      span: 'xl:col-span-2',
    },
  ], [copy.panelDescriptions]);

  const memberFilterControl = (
    <div className="w-full">
      <label
        htmlFor="statistics-member-filter"
        className="mb-2 block text-[10px] font-black uppercase tracking-[0.2em] text-[#8a6f3c]"
      >
        {copy.memberFilterLabel}
      </label>

      <select
        id="statistics-member-filter"
        value={selectedMember}
        onChange={(event) => setSelectedMember(event.target.value)}
        disabled={membersLoading}
        className="min-h-[42px] w-full rounded-xl border border-[#d8ccb7] bg-[#faf8f3] px-3 py-2 text-xs font-bold text-[#1b2d52] outline-none transition focus:border-[#c8a864] focus:bg-white"
      >
        <option value={ALL_MEMBERS_VALUE}>
          {membersLoading ? copy.loadingMembers : copy.allMembers}
        </option>

        {members.map((member) => (
          <option key={member.id} value={member.name}>
            {member.name}
          </option>
        ))}
      </select>
    </div>
  );

  return (
    <div className="min-h-screen bg-[#f5f2ec] text-[#1b2d52]">
      <PublicHeader />

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-10 xl:px-12">
        <section className="relative overflow-hidden rounded-[1.75rem] border border-[#e3dac9] bg-[radial-gradient(circle_at_top_right,_rgba(200,168,100,0.18),_transparent_30%),linear-gradient(135deg,_#1b2d52_0%,_#274071_100%)] px-6 py-8 text-white shadow-xl sm:rounded-[2.5rem] sm:px-8 lg:px-12">
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_bottom_left,_rgba(124,214,255,0.18),_transparent_28%)]" />

          <div className="relative max-w-4xl">
            <p className="text-xs font-black uppercase tracking-[0.24em] text-[#f0d898] sm:tracking-[0.4em]">
              {copy.eyebrow}
            </p>

            <h1 className="mt-4 font-serif text-3xl leading-tight sm:text-5xl">
              {copy.title}
            </h1>

            <p className="mt-5 max-w-3xl text-sm leading-8 text-[#dbe3f1] sm:text-base">
              {copy.subtitle}
            </p>
          </div>
        </section>

        <section className="mt-8 rounded-[2rem] border border-[#e3dac9] bg-white px-6 py-6 shadow-[0_18px_45px_rgba(27,45,82,0.08)] sm:px-8">
          <h2 className="font-serif text-2xl text-[#1b2d52] sm:text-3xl">
            {copy.introTitle}
          </h2>

          <p className="mt-3 max-w-4xl text-sm leading-7 text-[#60708d] sm:text-base">
            {copy.introDescription}
          </p>
        </section>

        <section className="mt-8 grid grid-cols-1 items-stretch gap-6 xl:grid-cols-2">
          {panels.map((panel, index) => {
            const isEvolutionPanel = Number(panel.id) === 4;

            return (
              <div key={panel.id} className={`h-full ${panel.span}`}>
                <GrafanaPanelFrame
                  title={panel.title}
                  description={panel.description}
                  src={buildPanelUrl(panel.id, selectedMember)}
                  height={panel.height}
                  priority={index < 2}
                  headerAction={isEvolutionPanel ? memberFilterControl : null}
                />
              </div>
            );
          })}
        </section>
      </main>
    </div>
  );
}