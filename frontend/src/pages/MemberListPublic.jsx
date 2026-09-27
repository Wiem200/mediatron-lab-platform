import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
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

function shortenText(value, maxLength = 155) {
  const cleaned = cleanText(value);

  if (!cleaned) {
    return '';
  }

  if (cleaned.length <= maxLength) {
    return cleaned;
  }

  return `${cleaned.slice(0, maxLength).replace(/[\s,.;:]+$/g, '')}...`;
}

function shortenBiography(value, maxLength = 125) {
  const cleaned = cleanText(value);

  if (!cleaned || /^speciality\s*:/i.test(cleaned) || /scopus documents/i.test(cleaned)) {
    return '';
  }

  const sentences = cleaned
    .match(/[^.!?]+[.!?]+|[^.!?]+$/g)
    ?.map((sentence) => cleanText(sentence))
    .filter(Boolean) || [];

  let preview = sentences.slice(0, 2).join(' ');
  const wasLimitedBySentenceCount = sentences.length > 2;

  if (preview.length > maxLength) {
    preview = `${preview.slice(0, maxLength).replace(/[\s,.;:]+$/g, '')}...`;
  } else if (wasLimitedBySentenceCount) {
    preview = `${preview.replace(/[\s,.;:]+$/g, '')}...`;
  }

  return preview;
}

function normalizeSearchString(value) {
  return cleanText(value)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();
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

function buildMemberSearchText(member) {
  const contact = parseContact(member);

  return normalizeSearchString([
    getFullName(member),
    member?.firstName,
    member?.lastName,
    member?.grade,
    member?.position,
    member?.lab,
    member?.bio,
    member?.contact,
    member?.email,
    member?.phone,
    member?.phoneNumber,
    member?.linkedinUrl,
    member?.googleScholarUrl,
    member?.personalSiteUrl,
    member?.personalWebsite,
    ...getResearchAxes(member),
    ...contact.emails,
    ...contact.phones,
    ...contact.notes,
  ].filter(Boolean).join(' '));
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

function mergeMemberSummary(summary, details) {
  if (!details) {
    return summary;
  }

  return {
    ...summary,
    ...details,
    contact: cleanText(details.contact) || cleanText(summary.contact),
    phone: cleanText(details.phone) || cleanText(summary.phone),
    email: cleanText(details.email) || cleanText(summary.email),
    bio: cleanText(details.bio) || cleanText(summary.bio),
    researchAxes: details.researchAxes?.length ? details.researchAxes : summary.researchAxes,
    linkedinUrl: cleanText(details.linkedinUrl) || cleanText(summary.linkedinUrl),
    googleScholarUrl: cleanText(details.googleScholarUrl) || cleanText(summary.googleScholarUrl),
    personalSiteUrl: cleanText(details.personalSiteUrl) || cleanText(summary.personalSiteUrl),
    personalWebsite: cleanText(details.personalWebsite) || cleanText(summary.personalWebsite),
    publications: details.publications || summary.publications,
    currentProjects: details.currentProjects || summary.currentProjects,
    supervisions: details.supervisions || summary.supervisions,
  };
}

function MemberAvatar({ member }) {
  const initials = `${member.firstName?.[0] || ''}${member.lastName?.[0] || ''}`.trim() || 'M';

  return (
    <div className="relative h-16 w-16 shrink-0 overflow-hidden rounded-[1.25rem] border border-[#d8deea] bg-[#eaf0f8] shadow-[0_10px_24px_rgba(27,45,82,0.10)]">
      {member.photoUrl ? (
        <img
          src={member.photoUrl}
          alt={getFullName(member)}
          className="h-full w-full object-cover"
        />
      ) : (
        <div className="flex h-full w-full items-center justify-center bg-[linear-gradient(135deg,#edf3fb_0%,#dbe5f3_100%)] font-serif text-xl text-[#1b2d52]">
          {initials}
        </div>
      )}
    </div>
  );
}

function ContactPill({ href, children }) {
  if (!children) {
    return null;
  }

  return (
    <a
      href={href}
      title={String(children)}
      target={href?.startsWith('mailto:') || href?.startsWith('tel:') ? undefined : '_blank'}
      rel={href?.startsWith('mailto:') || href?.startsWith('tel:') ? undefined : 'noreferrer'}
      className="inline-flex h-8 max-w-full items-center rounded-full border border-[#dbe2ee] bg-white px-3 text-[11px] font-bold text-[#1b2d52] transition hover:border-[#c8a864] hover:bg-[#fffaf0]"
    >
      <span className="block max-w-[145px] truncate">{children}</span>
    </a>
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
      title={String(children)}
      target="_blank"
      rel="noreferrer"
      className="inline-flex h-8 items-center rounded-full border border-[#d9c79d] bg-[#fff8e6] px-3 text-[11px] font-black uppercase tracking-[0.12em] text-[#7d631d] transition hover:border-[#c8a864] hover:bg-[#fff1c2]"
    >
      {children}
    </a>
  );
}

function ClampText({ children, lines = 2, className = '' }) {
  return (
    <p
      className={`overflow-hidden ${className}`}
      style={{
        display: '-webkit-box',
        WebkitLineClamp: lines,
        WebkitBoxOrient: 'vertical',
      }}
    >
      {children}
    </p>
  );
}

function MemberCard({ member, copy }) {
  const axes = getResearchAxes(member);
  const contact = parseContact(member);
  const bioPreview = shortenBiography(member.bio, 125);
  const hasProfiles = Boolean(member.linkedinUrl || member.googleScholarUrl || member.personalSiteUrl || member.personalWebsite);
  const hasContact = contact.emails.length > 0 || contact.phones.length > 0;
  const visibleProfileLinks = [
    { label: 'LinkedIn', url: member.linkedinUrl },
    { label: 'Scholar', url: member.googleScholarUrl },
    { label: 'Website', url: member.personalSiteUrl || member.personalWebsite },
  ].filter((item) => normalizeUrl(item.url)).slice(0, hasContact ? 1 : 2);

  return (
    <article className="group flex h-[405px] flex-col rounded-[2rem] border border-[#e3dac9] bg-white p-5 shadow-[0_18px_45px_rgba(27,45,82,0.08)] transition duration-300 hover:-translate-y-1 hover:border-[#d2bf9f] hover:shadow-[0_26px_70px_rgba(27,45,82,0.14)]">
      <div className="flex min-h-[78px] items-start gap-4">
        <MemberAvatar member={member} />
        <div className="min-w-0 flex-1">
          <h2 className="line-clamp-2 font-serif text-[1.35rem] leading-tight text-[#1b2d52]">
            {getFullName(member)}
          </h2>
          <p className="mt-2 line-clamp-2 text-[10px] font-black uppercase tracking-[0.22em] text-[#9a7b3a]">
            {member.grade || member.position || copy.gradeFallback}
          </p>
        </div>
      </div>

      <div className="mt-3 h-[66px] overflow-hidden">
        {axes.length > 0 ? (
          <div className="flex flex-wrap gap-2">
            {axes.slice(0, 2).map((axis) => (
              <span
                key={axis}
                title={axis}
                className="inline-flex h-7 max-w-[205px] items-center rounded-full border border-[#ead9ac] bg-[#fff8e6] px-3 text-[11px] font-bold text-[#7d631d]"
              >
                <span className="truncate">{axis}</span>
              </span>
            ))}
            {axes.length > 2 ? (
              <span
                title={axes.slice(2).join(', ')}
                className="inline-flex h-7 items-center rounded-full border border-[#dbe2ee] bg-[#f7f9fc] px-3 text-[11px] font-bold text-[#667791]"
              >
                +{axes.length - 2}
              </span>
            ) : null}
          </div>
        ) : null}
      </div>

      <div className={bioPreview ? 'mt-2 h-[72px] overflow-hidden rounded-[1.25rem] border border-[#edf0f6] bg-[#fafbfe] p-3.5' : 'mt-2 h-[72px] overflow-hidden'}>
        {bioPreview ? (
          <ClampText lines={2} className="text-[13px] leading-6 text-[#5d6c85]">
            {bioPreview}
          </ClampText>
        ) : null}
      </div>

      <div className="mt-auto pt-4">
        <div className="mb-3 flex h-[36px] flex-wrap items-start gap-2 overflow-hidden">
          {contact.emails.slice(0, 1).map((email) => (
            <ContactPill key={email} href={`mailto:${email}`}>
              {email}
            </ContactPill>
          ))}
          {contact.phones.slice(0, 1).map((phone) => (
            <ContactPill key={phone} href={`tel:${phone.replace(/[^\d+]/g, '')}`}>
              {phone}
            </ContactPill>
          ))}
          {visibleProfileLinks.map((link) => (
            <ProfileLink key={link.label} url={link.url}>{link.label}</ProfileLink>
          ))}
          {!hasContact && !hasProfiles ? (
            <span className="inline-flex h-8 items-center rounded-full border border-[#dbe2ee] bg-[#f7f9fc] px-3 text-[11px] font-semibold text-[#667791]">
              {copy.noContact}
            </span>
          ) : null}
        </div>

        <Link
          to={`/members/${member.id}`}
          className="inline-flex h-12 w-full items-center justify-center rounded-full bg-[#1b2d52] px-5 text-sm font-black uppercase tracking-[0.18em] text-white transition hover:bg-[#2d4677]"
        >
          {copy.seeMore}
        </Link>
      </div>
    </article>
  );
}

export default function MemberListPublic() {
  const { language } = useLanguage();
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  const copy = useMemo(() => ({
    en: {
      eyebrow: 'People',
      title: 'Laboratory Members',
      subtitle: 'Discover the researchers and scientific contributors shaping the MEDIATRON laboratory.',
      loading: 'Loading members...',
      error: 'Unable to load members.',
      empty: 'No member profiles are available yet.',
      noSearchResults: 'No member matches your search.',
      searchLabel: 'Search members',
      searchPlaceholder: 'Search by name, grade, research axis, email...',
      clearSearch: 'Clear',
      showingLabel: 'Showing',
      ofLabel: 'of',
      seeMore: 'See more',
      gradeFallback: 'Laboratory Member',
      noContact: 'Contact not provided yet',
      bioFallback: 'Scientific biography not provided yet.',
      pubs: 'Pubs',
      projects: 'Projects',
      supervisions: 'PhDs',
      countLabel: 'validated public profiles',
    },
    fr: {
      eyebrow: 'Equipe',
      title: 'Membres du Laboratoire',
      subtitle: 'Decouvrez les chercheurs et acteurs scientifiques qui font vivre le laboratoire MEDIATRON.',
      loading: 'Chargement des membres...',
      error: 'Impossible de charger les membres.',
      empty: 'Aucun profil membre n est disponible pour le moment.',
      noSearchResults: 'Aucun membre ne correspond a votre recherche.',
      searchLabel: 'Rechercher un membre',
      searchPlaceholder: 'Rechercher par nom, grade, axe, email...',
      clearSearch: 'Effacer',
      showingLabel: 'Affichage',
      ofLabel: 'sur',
      seeMore: 'Voir plus',
      gradeFallback: 'Membre du laboratoire',
      noContact: 'Contact non renseigne',
      bioFallback: 'Biographie scientifique non renseignee.',
      pubs: 'Pubs',
      projects: 'Projets',
      supervisions: 'Docs',
      countLabel: 'profils publics valides',
    },
  })[language], [language]);

  const normalizedSearchQuery = normalizeSearchString(searchQuery);

  const filteredMembers = useMemo(() => {
    if (!normalizedSearchQuery) {
      return members;
    }

    const tokens = normalizedSearchQuery.split(/\s+/).filter(Boolean);

    return members.filter((member) => {
      const searchableText = buildMemberSearchText(member);
      return tokens.every((token) => searchableText.includes(token));
    });
  }, [members, normalizedSearchQuery]);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError('');

      try {
        const response = await memberPublicService.getMembers();
        const baseMembers = Array.isArray(response.data) ? response.data : [];

        if (cancelled) {
          return;
        }

        setMembers(baseMembers);
        setLoading(false);

        const detailResponses = await Promise.allSettled(
          baseMembers.map((member) => memberPublicService.getMemberById(member.id))
        );

        if (cancelled) {
          return;
        }

        const detailsById = new Map();
        detailResponses.forEach((result, index) => {
          if (result.status === 'fulfilled' && result.value?.data) {
            detailsById.set(baseMembers[index].id, result.value.data);
          }
        });

        setMembers((currentMembers) => currentMembers.map((member) => mergeMemberSummary(member, detailsById.get(member.id))));
      } catch (err) {
        if (!cancelled) {
          setError(err?.response?.data?.message || copy.error);
          setLoading(false);
        }
      }
    };

    load();

    return () => {
      cancelled = true;
    };
  }, [copy.error]);

  return (
    <div className="min-h-screen bg-[#f5f2ec] text-[#1b2d52]">
      <PublicHeader />

      <main className="mx-auto max-w-7xl px-6 py-10 sm:px-8 lg:px-12 xl:px-16">
        <section className="relative overflow-hidden rounded-[1.75rem] sm:rounded-[2.5rem] border border-[#e3dac9] bg-[linear-gradient(135deg,#fcfbf7_0%,#f3ecde_100%)] px-6 py-8 shadow-[0_25px_70px_rgba(27,45,82,0.08)] sm:px-8 lg:px-12">
          <div className="absolute right-0 top-0 h-48 w-48 rounded-full bg-[#d8b56a]/15 blur-3xl" />
          <div className="absolute bottom-0 left-1/3 h-40 w-40 rounded-full bg-[#1b2d52]/8 blur-3xl" />

          <div className="relative flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
            <div className="max-w-3xl">
              <p className="text-xs font-black uppercase tracking-[0.22em] sm:tracking-[0.38em] text-[#b08a37]">{copy.eyebrow}</p>
              <h1 className="mt-4 font-serif text-4xl leading-tight sm:text-5xl">{copy.title}</h1>
              <p className="mt-4 max-w-2xl text-sm leading-7 text-[#5c6a84] sm:text-base">
                {copy.subtitle}
              </p>
            </div>

            {!loading && !error ? (
              <div className="rounded-[1.5rem] border border-[#e3dac9] bg-white/70 px-5 py-4 shadow-sm backdrop-blur">
                <p className="font-serif text-3xl text-[#1b2d52]">{filteredMembers.length}</p>
                <p className="mt-1 text-[10px] font-black uppercase tracking-[0.2em] text-[#8f9cb2]">
                  {copy.countLabel}
                </p>
                {members.length !== filteredMembers.length ? (
                  <p className="mt-2 text-[11px] font-bold text-[#667791]">
                    {copy.showingLabel} {filteredMembers.length} {copy.ofLabel} {members.length}
                  </p>
                ) : null}
              </div>
            ) : null}
          </div>
        </section>

        {!loading && !error && members.length > 0 ? (
          <section className="mt-8 rounded-[2rem] border border-[#e3dac9] bg-white p-4 shadow-[0_16px_45px_rgba(27,45,82,0.07)] sm:p-5">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
              <label className="block flex-1">
                <span className="mb-2 block text-[11px] font-black uppercase tracking-[0.28em] text-[#9a7b3a]">
                  {copy.searchLabel}
                </span>
                <div className="relative">
                  <input
                    type="search"
                    value={searchQuery}
                    onChange={(event) => setSearchQuery(event.target.value)}
                    placeholder={copy.searchPlaceholder}
                    className="w-full rounded-full border border-[#dbe2ee] bg-[#fafbfe] px-5 py-3 pr-12 text-sm font-semibold text-[#1b2d52] outline-none transition placeholder:text-[#9aa6bb] focus:border-[#c8a864] focus:bg-white focus:shadow-[0_0_0_4px_rgba(200,168,100,0.14)]"
                  />
                  <span className="pointer-events-none absolute right-5 top-1/2 -translate-y-1/2 text-[#9aa6bb]">
                    ⌕
                  </span>
                </div>
              </label>

              <div className="flex flex-wrap items-center gap-3">
                <span className="rounded-full border border-[#edf0f6] bg-[#f8f6f1] px-4 py-2 text-xs font-bold text-[#667791]">
                  {copy.showingLabel} {filteredMembers.length} {copy.ofLabel} {members.length}
                </span>
                {searchQuery ? (
                  <button
                    type="button"
                    onClick={() => setSearchQuery('')}
                    className="rounded-full border border-[#d9c79d] bg-[#fff8e6] px-4 py-2 text-xs font-black uppercase tracking-[0.12em] text-[#7d631d] transition hover:border-[#c8a864] hover:bg-[#fff1c2]"
                  >
                    {copy.clearSearch}
                  </button>
                ) : null}
              </div>
            </div>
          </section>
        ) : null}

        <section className="mt-10">
          {loading ? (
            <div className="rounded-[2rem] border border-[#e3dac9] bg-white px-6 py-12 text-center text-sm text-[#667791] shadow-lg">
              {copy.loading}
            </div>
          ) : error ? (
            <div className="rounded-[2rem] border border-rose-200 bg-rose-50 px-6 py-8 text-sm text-rose-700 shadow-lg">
              {error}
            </div>
          ) : members.length === 0 ? (
            <div className="rounded-[2rem] border border-dashed border-[#d8deea] bg-white px-6 py-12 text-center text-sm text-[#667791] shadow-lg">
              {copy.empty}
            </div>
          ) : filteredMembers.length === 0 ? (
            <div className="rounded-[2rem] border border-dashed border-[#d8deea] bg-white px-6 py-12 text-center shadow-lg">
              <p className="text-sm font-semibold text-[#667791]">{copy.noSearchResults}</p>
              <button
                type="button"
                onClick={() => setSearchQuery('')}
                className="mt-5 rounded-full border border-[#d9c79d] bg-[#fff8e6] px-5 py-2 text-xs font-black uppercase tracking-[0.12em] text-[#7d631d] transition hover:border-[#c8a864] hover:bg-[#fff1c2]"
              >
                {copy.clearSearch}
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
              {filteredMembers.map((member) => (
                <MemberCard key={member.id} member={member} copy={copy} />
              ))}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
