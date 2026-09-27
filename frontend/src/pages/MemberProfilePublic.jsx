import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import PublicHeader from '../components/PublicHeader';
import { useLanguage } from '../context/LanguageContext';
import { memberPublicService } from '../api/memberPublicService';

const EMPTY_MARKERS = new Set(['', 'n/a', 'na', 'none', 'aucun', 'aucune', '/', '-', 'pas encore']);

function cleanText(value) {
  if (value === null || value === undefined) {
    return '';
  }

  const cleaned = String(value).replace(/\s+/g, ' ').trim();
  return EMPTY_MARKERS.has(cleaned.toLowerCase()) ? '' : cleaned;
}

function normalizeSearchString(value) {
  return cleanText(value)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();
}

function normalizeUrl(url) {
  const cleaned = cleanText(url);

  if (!cleaned) {
    return '';
  }

  if (cleaned.startsWith('http://') || cleaned.startsWith('https://')) {
    return cleaned;
  }

  return `https://${cleaned}`;
}

function getFullName(member) {
  return cleanText(`${member.firstName || ''} ${member.lastName || ''}`) || 'MEDIATRON Member';
}

function getResearchAxes(member) {
  const rawAxes = member?.researchAxes;

  if (Array.isArray(rawAxes)) {
    return Array.from(new Set(
      rawAxes
        .flatMap((axis) => String(axis || '').split(/[;\n]+/))
        .map(cleanText)
        .filter(Boolean)
    ));
  }

  return Array.from(new Set(
    String(rawAxes || '')
      .split(/[;\n]+/)
      .map(cleanText)
      .filter(Boolean)
  ));
}

function parseContact(member) {
  const source = [member?.contact, member?.email, member?.phone, member?.phoneNumber]
    .map(cleanText)
    .filter(Boolean)
    .join(' | ');

  const emails = Array.from(new Set(source.match(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/gi) || []));
  const phones = Array.from(new Set(source.match(/(?:\+?\d[\d\s().-]{6,}\d)/g) || []))
    .map((phone) => phone.replace(/\s+/g, ' ').trim())
    .filter((phone) => !emails.some((email) => email.includes(phone)));

  let remainder = source;
  emails.forEach((email) => {
    remainder = remainder.replace(email, '');
  });
  phones.forEach((phone) => {
    remainder = remainder.replace(phone, '');
  });

  const notes = remainder
    .split(/[|;,]+/)
    .map((item) => cleanText(item.replace(/^[/:—-]+|[/:—-]+$/g, '')))
    .filter(Boolean);

  return { emails, phones, notes };
}

function MemberAvatar({ member }) {
  const initials = `${member.firstName?.[0] || ''}${member.lastName?.[0] || ''}`.trim() || 'M';

  return (
    <div className="h-28 w-28 shrink-0 overflow-hidden rounded-[2rem] border border-[#d7deea] bg-[#e8eef7] shadow-[0_18px_45px_rgba(27,45,82,0.16)] sm:h-36 sm:w-36">
      {member.photoUrl ? (
        <img
          src={member.photoUrl}
          alt={getFullName(member)}
          className="h-full w-full object-cover"
        />
      ) : (
        <div className="flex h-full w-full items-center justify-center bg-[linear-gradient(135deg,#edf3fb_0%,#dbe5f3_100%)] font-serif text-4xl text-[#1b2d52]">
          {initials}
        </div>
      )}
    </div>
  );
}

function SectionCard({ title, eyebrow, children, className = '' }) {
  return (
    <section className={`rounded-[2rem] border border-[#e3dac9] bg-white p-6 shadow-[0_18px_45px_rgba(27,45,82,0.08)] sm:p-8 ${className}`}>
      {eyebrow ? (
        <p className="mb-3 text-[10px] font-black uppercase tracking-[0.32em] text-[#b08a37]">{eyebrow}</p>
      ) : null}
      <h2 className="font-serif text-2xl text-[#1b2d52]">{title}</h2>
      <div className="mt-5">{children}</div>
    </section>
  );
}

function ProfileLink({ url, children }) {
  const normalized = normalizeUrl(url);

  if (!normalized) {
    return null;
  }

  return (
    <a
      href={normalized}
      target="_blank"
      rel="noreferrer"
      className="inline-flex items-center justify-center rounded-full border border-[#d9c79d] bg-[#fff8e6] px-4 py-2 text-xs font-black uppercase tracking-[0.14em] text-[#7d631d] transition hover:border-[#c8a864] hover:bg-[#fff1c2]"
    >
      {children}
    </a>
  );
}

function ContactRow({ label, value, href }) {
  if (!value) {
    return null;
  }

  const content = (
    <div className="rounded-[1.25rem] border border-[#e7ecf4] bg-[#fbfcfe] p-4 transition hover:border-[#d8c79f]">
      <p className="text-[10px] font-black uppercase tracking-[0.24em] text-[#8f9cb2]">{label}</p>
      <p className="mt-2 break-words text-sm font-semibold leading-6 text-[#1b2d52]">{value}</p>
    </div>
  );

  if (!href) {
    return content;
  }

  return (
    <a href={href} className="block">
      {content}
    </a>
  );
}

function ContactPanel({ member, copy }) {
  const contact = parseContact(member);
  const profileLinks = [
    { label: 'LinkedIn', url: member.linkedinUrl },
    { label: 'Google Scholar', url: member.googleScholarUrl },
    { label: 'Website', url: member.personalSiteUrl || member.personalWebsite },
  ].filter((item) => normalizeUrl(item.url));

  const hasContact = contact.emails.length || contact.phones.length || contact.notes.length || profileLinks.length;

  if (!hasContact) {
    return (
      <div className="rounded-[1.5rem] border border-dashed border-[#dbe2ee] bg-[#fafbfe] p-6 text-sm leading-7 text-[#667791]">
        {copy.noContact}
      </div>
    );
  }

  return (
    <div className="space-y-5">
      <div className="grid gap-3">
        {contact.emails.map((email) => (
          <ContactRow key={email} label={copy.emailLabel} value={email} href={`mailto:${email}`} />
        ))}
        {contact.phones.map((phone) => (
          <ContactRow key={phone} label={copy.phoneLabel} value={phone} href={`tel:${phone.replace(/[^\d+]/g, '')}`} />
        ))}
        {contact.notes.map((note) => (
          <ContactRow key={note} label={copy.contactNoteLabel} value={note} />
        ))}
      </div>

      {profileLinks.length ? (
        <div>
          <p className="mb-3 text-[10px] font-black uppercase tracking-[0.26em] text-[#8f9cb2]">
            {copy.profileLinksLabel}
          </p>
          <div className="flex flex-wrap gap-2">
            {profileLinks.map((link) => (
              <ProfileLink key={link.label} url={link.url}>
                {link.label}
              </ProfileLink>
            ))}
          </div>
        </div>
      ) : null}
    </div>
  );
}

function StatRow({ value, label }) {
  return (
    <div className="flex min-w-0 items-center gap-3 rounded-[1.35rem] border border-[#e3dac9] bg-white/80 px-4 py-3 shadow-sm backdrop-blur">
      <p className="shrink-0 font-serif text-3xl leading-none text-[#1b2d52]">{value}</p>
      <p className="min-w-0 text-[10px] font-black uppercase tracking-[0.16em] text-[#8f9cb2]">
        {label}
      </p>
    </div>
  );
}

function PublicationCard({ publication, copy, index }) {
  return (
    <article key={`${publication.title || 'publication'}-${index}`} className="rounded-[1.5rem] border border-[#e9edf4] bg-[#fbfcfe] p-5 transition hover:border-[#d8c79f] hover:bg-white">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <h3 className="text-lg font-semibold leading-7 text-[#1b2d52]">{publication.title || 'Untitled publication'}</h3>
          {publication.authorsText ? (
            <p className="mt-2 text-sm leading-6 text-[#607089]">{publication.authorsText}</p>
          ) : null}
        </div>
        {publication.type ? (
          <span className="w-fit shrink-0 rounded-full bg-[#c8a864]/15 px-3 py-1 text-[10px] font-black uppercase tracking-[0.15em] text-[#8a6f3c]">
            {publication.type}
          </span>
        ) : null}
      </div>

      <p className="mt-3 break-words text-xs font-semibold uppercase tracking-[0.14em] text-[#8f9cb2]">
        {publication.publicationYear || copy.unknownYear}
        {publication.source ? ` | ${publication.source}` : ''}
        {publication.doi ? ` | DOI: ${publication.doi}` : ''}
      </p>

      <div className="mt-4 flex flex-wrap gap-2">
        {publication.externalUrl ? (
          <a
            href={publication.externalUrl}
            target="_blank"
            rel="noreferrer"
            className="inline-flex rounded-full border border-[#d8dfeb] bg-white px-4 py-2 text-xs font-black uppercase tracking-[0.18em] text-[#1b2d52] transition hover:border-[#c8a864]"
          >
            {copy.openSource}
          </a>
        ) : null}
        {publication.pdfUrl ? (
          <a
            href={publication.pdfUrl}
            target="_blank"
            rel="noreferrer"
            className="inline-flex rounded-full bg-[#1b2d52] px-4 py-2 text-xs font-black uppercase tracking-[0.18em] text-white transition hover:bg-[#2d4677]"
          >
            PDF
          </a>
        ) : null}
      </div>
    </article>
  );
}

export default function MemberProfilePublic() {
  const { language } = useLanguage();
  const { id } = useParams();
  const [member, setMember] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [publicationFilters, setPublicationFilters] = useState({ search: '', year: '', type: '' });
  const [appliedPublicationFilters, setAppliedPublicationFilters] = useState({ search: '', year: '', type: '' });
  const [publicationPage, setPublicationPage] = useState(1);

  const copy = useMemo(() => ({
    en: {
      back: 'Back to members',
      loadError: 'Unable to load member profile.',
      loading: 'Loading profile...',
      notFound: 'Member not found.',
      gradeFallback: 'Laboratory Member',
      noBio: 'No biography is available for this member yet.',
      researchAreas: 'Research Areas',
      noResearchAreas: 'No research areas have been published yet.',
      publications: 'Associated Publications',
      noPublications: 'No synchronized publications are available yet.',
      publicationSyncPending: 'Publication synchronization is prepared for this profile but not yet connected to a live source.',
      projects: 'Current Projects',
      noProjects: 'No current projects are associated with this member.',
      supervisions: 'Supervisions',
      noSupervisions: 'No current supervision records are available.',
      contactSection: 'Contact And Profiles',
      bioSection: 'Biography',
      detailsSection: 'Scientific Profile',
      unknownYear: 'Year not specified',
      thesisTopicFallback: 'Thesis topic not specified.',
      viewProject: 'View project',
      emailLabel: 'Email',
      phoneLabel: 'Phone',
      contactNoteLabel: 'Contact note',
      profileLinksLabel: 'Academic profiles',
      noContact: 'No public contact or academic profile link has been provided yet.',
      publicationsStat: 'Publications',
      projectsStat: 'Projects',
      supervisionsStat: 'Supervisions',
      openSource: 'Open source',
      publicationFilterTitle: 'Filter member publications',
      publicationFilterSubtitle: "Search this researcher's publications by title, authors, year, source, DOI, or type.",
      searchLabel: 'Search',
      searchPlaceholder: 'wireless, security, DOI, author...',
      yearLabel: 'Year',
      allYears: 'All years',
      typeLabel: 'Type',
      allTypes: 'All types',
      applyFilters: 'Apply Filters',
      resetFilters: 'Reset',
      resultsCount: 'Showing {shown} of {total} publication(s)',
      noMatchingPublications: 'No publication matches the current filters.',
      pageLabel: 'Page {current} of {total}',
      previous: 'Previous',
      next: 'Next',
      labFallback: 'MEDIATRON Laboratory',
    },
    fr: {
      back: 'Retour aux membres',
      loadError: 'Impossible de charger le profil du membre.',
      loading: 'Chargement du profil...',
      notFound: 'Membre introuvable.',
      gradeFallback: 'Membre du laboratoire',
      noBio: 'Aucune biographie n est disponible pour ce membre.',
      researchAreas: 'Axes de Recherche',
      noResearchAreas: 'Aucun axe de recherche n a encore ete publie.',
      publications: 'Publications Associees',
      noPublications: 'Aucune publication synchronisee n est disponible pour le moment.',
      publicationSyncPending: 'La synchronisation des publications est preparee pour ce profil mais n est pas encore connectee a une source en direct.',
      projects: 'Projets en Cours',
      noProjects: 'Aucun projet en cours n est associe a ce membre.',
      supervisions: 'Encadrements',
      noSupervisions: 'Aucun encadrement en cours n est disponible.',
      contactSection: 'Coordonnees Et Profils',
      bioSection: 'Biographie',
      detailsSection: 'Profil Scientifique',
      unknownYear: 'Annee non precisee',
      thesisTopicFallback: 'Sujet de these non precise.',
      viewProject: 'Voir le projet',
      emailLabel: 'Email',
      phoneLabel: 'Telephone',
      contactNoteLabel: 'Note de contact',
      profileLinksLabel: 'Profils academiques',
      noContact: 'Aucun contact public ou lien de profil academique n a encore ete fourni.',
      publicationsStat: 'Publications',
      projectsStat: 'Projets',
      supervisionsStat: 'Encadrements',
      openSource: 'Ouvrir la source',
      publicationFilterTitle: 'Filtrer les publications du membre',
      publicationFilterSubtitle: 'Recherchez dans les publications de ce chercheur par titre, auteurs, annee, source, DOI ou type.',
      searchLabel: 'Recherche',
      searchPlaceholder: 'wireless, security, DOI, auteur...',
      yearLabel: 'Annee',
      allYears: 'Toutes les annees',
      typeLabel: 'Type',
      allTypes: 'Tous les types',
      applyFilters: 'Appliquer',
      resetFilters: 'Reinitialiser',
      resultsCount: 'Affichage de {shown} sur {total} publication(s)',
      noMatchingPublications: 'Aucune publication ne correspond aux filtres actuels.',
      pageLabel: 'Page {current} sur {total}',
      previous: 'Precedent',
      next: 'Suivant',
      labFallback: 'Laboratoire MEDIATRON',
    },
  })[language], [language]);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        const response = await memberPublicService.getMemberById(id);
        setMember(response.data);
      } catch (err) {
        if (err?.response?.status === 404) {
          setMember(null);
          setError(copy.notFound);
        } else {
          setError(err?.response?.data?.message || copy.loadError);
        }
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [copy.loadError, copy.notFound, id]);

  const researchAxes = getResearchAxes(member || {});
  const publicationCount = member?.publicationCount ?? member?.publications?.length ?? 0;
  const projectCount = member?.projectCount ?? member?.currentProjects?.length ?? 0;
  const supervisionCount = member?.supervisionCount ?? member?.supervisions?.length ?? 0;

  const publications = useMemo(() => (Array.isArray(member?.publications) ? member.publications : []), [member?.publications]);

  const publicationYears = useMemo(() => (
    Array.from(new Set(
      publications
        .map((publication) => publication.publicationYear || publication.year)
        .filter(Boolean)
        .map(String)
    )).sort((a, b) => Number(b) - Number(a))
  ), [publications]);

  const publicationTypes = useMemo(() => (
    Array.from(new Set(
      publications
        .map((publication) => cleanText(publication.type))
        .filter(Boolean)
    )).sort((a, b) => a.localeCompare(b))
  ), [publications]);

  const filteredPublications = useMemo(() => {
    const search = normalizeSearchString(appliedPublicationFilters.search);
    const selectedYear = cleanText(appliedPublicationFilters.year);
    const selectedType = cleanText(appliedPublicationFilters.type);

    return publications.filter((publication) => {
      const publicationYear = String(publication.publicationYear || publication.year || '');
      const publicationType = cleanText(publication.type);

      const matchesYear = !selectedYear || publicationYear === selectedYear;
      const matchesType = !selectedType || publicationType === selectedType;

      const searchableText = normalizeSearchString([
        publication.title,
        publication.authorsText,
        publication.source,
        publication.doi,
        publication.externalUrl,
        publication.pdfUrl,
        publicationType,
        publicationYear,
      ].filter(Boolean).join(' '));

      const matchesSearch = !search || searchableText.includes(search);

      return matchesSearch && matchesYear && matchesType;
    });
  }, [appliedPublicationFilters, publications]);

  const publicationPageSize = 5;
  const publicationTotalPages = Math.max(1, Math.ceil(filteredPublications.length / publicationPageSize));
  const safePublicationPage = Math.min(publicationPage, publicationTotalPages);
  const paginatedPublications = filteredPublications.slice(
    (safePublicationPage - 1) * publicationPageSize,
    safePublicationPage * publicationPageSize
  );

  const publicationPageNumbers = useMemo(() => {
    const maxButtons = 5;
    const total = publicationTotalPages;
    const current = safePublicationPage;
    let start = Math.max(1, current - 2);
    let end = Math.min(total, start + maxButtons - 1);

    if (end - start + 1 < maxButtons) {
      start = Math.max(1, end - maxButtons + 1);
    }

    return Array.from({ length: end - start + 1 }, (_, index) => start + index);
  }, [publicationTotalPages, safePublicationPage]);

  const handlePublicationFilterChange = (field, value) => {
    setPublicationFilters((current) => ({ ...current, [field]: value }));
  };

  const applyPublicationFilters = () => {
    setAppliedPublicationFilters(publicationFilters);
    setPublicationPage(1);
  };

  const resetPublicationFilters = () => {
    const emptyFilters = { search: '', year: '', type: '' };
    setPublicationFilters(emptyFilters);
    setAppliedPublicationFilters(emptyFilters);
    setPublicationPage(1);
  };

  return (
    <div className="min-h-screen bg-[#f5f2ec] text-[#1b2d52]">
      <PublicHeader />

      <main className="mx-auto max-w-6xl px-6 py-10 sm:px-8 lg:px-12">
        <Link
          to="/members"
          className="inline-flex items-center rounded-full border border-[#d8dfeb] bg-white px-4 py-2 text-xs font-bold uppercase tracking-[0.2em] text-[#1b2d52] shadow-sm transition hover:border-[#c8a864]"
        >
          {copy.back}
        </Link>

        {loading ? (
          <div className="mt-6 rounded-[2rem] border border-[#e3dac9] bg-white px-6 py-12 text-center text-sm text-[#667791] shadow-lg">
            {copy.loading}
          </div>
        ) : error ? (
          <div className="mt-6 rounded-[2rem] border border-rose-200 bg-rose-50 px-6 py-8 text-sm text-rose-700 shadow-lg">
            {error}
          </div>
        ) : !member ? (
          <div className="mt-6 rounded-[2rem] border border-[#e3dac9] bg-white px-6 py-12 text-center text-sm text-[#667791] shadow-lg">
            {copy.notFound}
          </div>
        ) : (
          <div className="mt-6 space-y-6">
            <section className="relative overflow-hidden rounded-[1.75rem] sm:rounded-[2.5rem] border border-[#e3dac9] bg-[linear-gradient(135deg,#fcfbf7_0%,#f3ecde_100%)] px-6 py-8 shadow-[0_25px_70px_rgba(27,45,82,0.08)] sm:px-8 lg:px-10">
              <div className="absolute right-0 top-0 h-56 w-56 rounded-full bg-[#d8b56a]/15 blur-3xl" />
              <div className="absolute bottom-0 left-1/4 h-44 w-44 rounded-full bg-[#1b2d52]/8 blur-3xl" />

              <div className="relative flex flex-col gap-7 lg:flex-row lg:items-start lg:justify-between">
                <div className="flex flex-col gap-6 sm:flex-row sm:items-center lg:flex-1">
                  <MemberAvatar member={member} />

                  <div className="min-w-0 flex-1">
                    <p className="text-xs font-black uppercase tracking-[0.22em] sm:tracking-[0.38em] text-[#b08a37]">{copy.detailsSection}</p>
                    <h1 className="mt-3 break-words font-serif text-4xl leading-tight sm:text-5xl">
                      {getFullName(member)}
                    </h1>
                    <p className="mt-3 text-sm font-black uppercase tracking-[0.22em] text-[#8c7551]">
                      {member.grade || member.position || copy.gradeFallback}
                    </p>
                    <p className="mt-2 text-xs font-bold uppercase tracking-[0.2em] text-[#8f9cb2]">
                      {member.lab || copy.labFallback}
                    </p>

                    <div className="mt-5 flex flex-wrap gap-3">
                      <ProfileLink url={member.linkedinUrl}>LinkedIn</ProfileLink>
                      <ProfileLink url={member.googleScholarUrl}>Google Scholar</ProfileLink>
                      <ProfileLink url={member.personalSiteUrl || member.personalWebsite}>Website</ProfileLink>
                    </div>
                  </div>
                </div>

                <div className="grid w-full gap-3 sm:grid-cols-3 lg:w-72 lg:grid-cols-1">
                  <StatRow value={publicationCount} label={copy.publicationsStat} />
                  <StatRow value={projectCount} label={copy.projectsStat} />
                  <StatRow value={supervisionCount} label={copy.supervisionsStat} />
                </div>
              </div>
            </section>

            <div className="grid grid-cols-1 gap-6 xl:grid-cols-[1.15fr_0.85fr]">
              <SectionCard title={copy.bioSection}>
                <p className="whitespace-pre-wrap text-sm leading-8 text-[#5f6e87]">
                  {member.bio || copy.noBio}
                </p>
              </SectionCard>

              <SectionCard title={copy.contactSection}>
                <ContactPanel member={member} copy={copy} />
              </SectionCard>
            </div>

            <SectionCard title={copy.researchAreas}>
              {researchAxes.length > 0 ? (
                <div className="flex flex-wrap gap-3">
                  {researchAxes.map((axis) => (
                    <span
                      key={axis}
                      className="rounded-full border border-[#d9c79d] bg-[#fff5db] px-4 py-2 text-sm font-semibold text-[#7d631d]"
                    >
                      {axis}
                    </span>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-[#667791]">{copy.noResearchAreas}</p>
              )}
            </SectionCard>

            <SectionCard title={copy.publications}>
              {publications.length > 0 ? (
                <div className="space-y-5">
                  <form
                    onSubmit={(event) => {
                      event.preventDefault();
                      applyPublicationFilters();
                    }}
                    className="rounded-[1.75rem] border border-[#e3dac9] bg-[#fcfbf7] p-5 shadow-[0_12px_30px_rgba(27,45,82,0.04)]"
                  >
                    <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                      <div>
                        <p className="text-[10px] font-black uppercase tracking-[0.32em] text-[#b08a37]">
                          {copy.publicationFilterTitle}
                        </p>
                        <p className="mt-2 text-sm leading-6 text-[#667791]">
                          {copy.publicationFilterSubtitle}
                        </p>
                      </div>
                      <div className="w-fit rounded-full bg-white px-4 py-2 text-xs font-bold text-[#667791] shadow-sm">
                        {copy.resultsCount
                          .replace('{shown}', filteredPublications.length)
                          .replace('{total}', publications.length)}
                      </div>
                    </div>

                    <div className="mt-5 grid grid-cols-1 gap-4 lg:grid-cols-[1.4fr_0.7fr_0.7fr]">
                      <label className="block">
                        <span className="mb-2 block text-[10px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa5b8]">
                          {copy.searchLabel}
                        </span>
                        <input
                          type="search"
                          value={publicationFilters.search}
                          onChange={(event) => handlePublicationFilterChange('search', event.target.value)}
                          placeholder={copy.searchPlaceholder}
                          className="w-full rounded-2xl border border-[#e0e5ee] bg-white px-4 py-3 text-sm font-semibold text-[#1b2d52] outline-none transition placeholder:text-[#9aa5b8] focus:border-[#c8a864] focus:ring-4 focus:ring-[#c8a864]/15"
                        />
                      </label>

                      <label className="block">
                        <span className="mb-2 block text-[10px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa5b8]">
                          {copy.yearLabel}
                        </span>
                        <select
                          value={publicationFilters.year}
                          onChange={(event) => handlePublicationFilterChange('year', event.target.value)}
                          className="w-full rounded-2xl border border-[#e0e5ee] bg-white px-4 py-3 text-sm font-semibold text-[#1b2d52] outline-none transition focus:border-[#c8a864] focus:ring-4 focus:ring-[#c8a864]/15"
                        >
                          <option value="">{copy.allYears}</option>
                          {publicationYears.map((year) => (
                            <option key={year} value={year}>{year}</option>
                          ))}
                        </select>
                      </label>

                      <label className="block">
                        <span className="mb-2 block text-[10px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa5b8]">
                          {copy.typeLabel}
                        </span>
                        <select
                          value={publicationFilters.type}
                          onChange={(event) => handlePublicationFilterChange('type', event.target.value)}
                          className="w-full rounded-2xl border border-[#e0e5ee] bg-white px-4 py-3 text-sm font-semibold text-[#1b2d52] outline-none transition focus:border-[#c8a864] focus:ring-4 focus:ring-[#c8a864]/15"
                        >
                          <option value="">{copy.allTypes}</option>
                          {publicationTypes.map((type) => (
                            <option key={type} value={type}>{type}</option>
                          ))}
                        </select>
                      </label>
                    </div>

                    <div className="mt-5 flex flex-wrap gap-3">
                      <button
                        type="submit"
                        className="rounded-2xl bg-[#1b2d52] px-5 py-3 text-xs font-black uppercase tracking-[0.16em] text-white transition hover:bg-[#2d4677]"
                      >
                        {copy.applyFilters}
                      </button>
                      <button
                        type="button"
                        onClick={resetPublicationFilters}
                        className="rounded-2xl border border-[#d8c8a7] bg-white px-5 py-3 text-xs font-black uppercase tracking-[0.16em] text-[#1b2d52] transition hover:border-[#c8a864]"
                      >
                        {copy.resetFilters}
                      </button>
                    </div>
                  </form>

                  {filteredPublications.length > 0 ? (
                    <>
                      <div className="space-y-4">
                        {paginatedPublications.map((publication, index) => (
                          <PublicationCard
                            key={`${publication.id || publication.doi || publication.title || 'publication'}-${index}`}
                            publication={publication}
                            copy={copy}
                            index={index}
                          />
                        ))}
                      </div>

                      {publicationTotalPages > 1 ? (
                        <div className="flex flex-col gap-4 rounded-[1.5rem] border border-[#e9edf4] bg-white px-4 py-4 sm:flex-row sm:items-center sm:justify-between">
                          <p className="text-sm font-semibold text-[#667791]">
                            {copy.pageLabel
                              .replace('{current}', safePublicationPage)
                              .replace('{total}', publicationTotalPages)}
                          </p>
                          <div className="flex flex-wrap gap-2">
                            <button
                              type="button"
                              disabled={safePublicationPage <= 1}
                              onClick={() => setPublicationPage((page) => Math.max(1, page - 1))}
                              className="rounded-xl border border-[#e3dac9] bg-white px-4 py-2 text-sm font-black text-[#1b2d52] transition hover:border-[#c8a864] disabled:cursor-not-allowed disabled:text-[#9aa5b8] disabled:opacity-60"
                            >
                              {copy.previous}
                            </button>
                            {publicationPageNumbers.map((pageNumber) => (
                              <button
                                key={pageNumber}
                                type="button"
                                onClick={() => setPublicationPage(pageNumber)}
                                className={`rounded-xl border px-4 py-2 text-sm font-black transition ${
                                  pageNumber === safePublicationPage
                                    ? 'border-[#1b2d52] bg-[#1b2d52] text-white'
                                    : 'border-[#e3dac9] bg-white text-[#1b2d52] hover:border-[#c8a864]'
                                }`}
                              >
                                {pageNumber}
                              </button>
                            ))}
                            <button
                              type="button"
                              disabled={safePublicationPage >= publicationTotalPages}
                              onClick={() => setPublicationPage((page) => Math.min(publicationTotalPages, page + 1))}
                              className="rounded-xl border border-[#e3dac9] bg-white px-4 py-2 text-sm font-black text-[#1b2d52] transition hover:border-[#c8a864] disabled:cursor-not-allowed disabled:text-[#9aa5b8] disabled:opacity-60"
                            >
                              {copy.next}
                            </button>
                          </div>
                        </div>
                      ) : null}
                    </>
                  ) : (
                    <div className="rounded-[1.5rem] border border-dashed border-[#dbe2ee] bg-[#fafbfe] p-6 text-sm leading-7 text-[#667791]">
                      {copy.noMatchingPublications}
                    </div>
                  )}
                </div>
              ) : (
                <div className="space-y-3 rounded-[1.5rem] border border-dashed border-[#dbe2ee] bg-[#fafbfe] p-6 text-sm leading-7 text-[#667791]">
                  <p>{copy.noPublications}</p>
                  {member.publicationSync?.synchronizationAvailable ? (
                    <p>{copy.publicationSyncPending}</p>
                  ) : null}
                </div>
              )}
            </SectionCard>

            <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
              <SectionCard title={copy.projects}>
                {member.currentProjects?.length > 0 ? (
                  <div className="space-y-4">
                    {member.currentProjects.map((project) => (
                      <article key={project.id} className="rounded-[1.5rem] border border-[#e9edf4] bg-[#fbfcfe] p-5 transition hover:border-[#d8c79f] hover:bg-white">
                        <h3 className="text-lg font-semibold text-[#1b2d52]">{project.title}</h3>
                        {project.duration ? (
                          <p className="mt-2 text-xs font-semibold uppercase tracking-[0.18em] text-[#8f9cb2]">
                            {project.duration}
                          </p>
                        ) : null}
                        {project.description ? (
                          <p className="mt-3 text-sm leading-7 text-[#607089]">{project.description}</p>
                        ) : null}
                        <Link
                          to={`/research-projects/${project.id}`}
                          className="mt-4 inline-flex rounded-full bg-[#1b2d52] px-4 py-2 text-xs font-bold uppercase tracking-[0.18em] text-white transition hover:bg-[#2d4677]"
                        >
                          {copy.viewProject}
                        </Link>
                      </article>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-[#667791]">{copy.noProjects}</p>
                )}
              </SectionCard>

              <SectionCard title={copy.supervisions}>
                {member.supervisions?.length > 0 ? (
                  <div className="space-y-4">
                    {member.supervisions.map((student) => (
                      <article key={student.id} className="rounded-[1.5rem] border border-[#e9edf4] bg-[#fbfcfe] p-5 transition hover:border-[#d8c79f] hover:bg-white">
                        <div className="flex items-start gap-4">
                          <div className="flex h-14 w-14 shrink-0 items-center justify-center overflow-hidden rounded-2xl border border-[#dce2ee] bg-[#eef2f8] font-serif text-lg text-[#1b2d52]">
                            {student.photoUrl ? (
                              <img
                                src={student.photoUrl}
                                alt={`${student.firstName || ''} ${student.lastName || ''}`.trim()}
                                className="h-full w-full object-cover"
                              />
                            ) : (
                              `${student.firstName?.[0] || ''}${student.lastName?.[0] || ''}`.trim() || 'D'
                            )}
                          </div>
                          <div className="min-w-0 flex-1">
                            <h3 className="text-lg font-semibold text-[#1b2d52]">
                              {student.firstName} {student.lastName}
                            </h3>
                            <p className="mt-2 text-sm leading-7 text-[#607089]">
                              {student.thesisTopic || copy.thesisTopicFallback}
                            </p>
                            <p className="mt-3 break-words text-xs font-semibold uppercase tracking-[0.18em] text-[#8f9cb2]">
                              {student.registrationYear || copy.unknownYear}
                              {student.researchTeam ? ` | ${student.researchTeam}` : ''}
                            </p>
                          </div>
                        </div>
                      </article>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-[#667791]">{copy.noSupervisions}</p>
                )}
              </SectionCard>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
