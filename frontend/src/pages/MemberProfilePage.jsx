import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import PublicHeader from '../components/PublicHeader';
import { memberService } from '../api/memberService';
import { publicationService } from '../api/publicationService';

const PAGE_SIZE = 6;

function normalizeUrl(url) {
  if (!url || typeof url !== 'string') {
    return null;
  }

  const trimmed = url.trim();
  if (!trimmed) {
    return null;
  }

  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
    return trimmed;
  }

  return `https://${trimmed}`;
}

function safeText(value, fallback = 'Not provided') {
  if (typeof value !== 'string') {
    return fallback;
  }

  const cleaned = value.trim();
  return cleaned ? cleaned : fallback;
}

function getMemberGrade(member) {
  return member?.grade || member?.position || 'Research Member';
}

function buildFullName(member) {
  return `${member?.firstName || ''} ${member?.lastName || ''}`.trim() || 'Unnamed Member';
}

function PublicationTypeBadge({ type }) {
  return (
    <span className="inline-flex rounded-full bg-[#c8a864]/15 px-3 py-1 text-xs font-bold uppercase tracking-wide text-[#8a6f3c]">
      {type || 'Unknown'}
    </span>
  );
}

function InfoCard({ label, value }) {
  return (
    <div className="rounded-2xl border border-[#ece3d4] bg-[#fffdf9] p-4">
      <p className="mb-2 text-[10px] font-bold uppercase tracking-[0.25em] text-gray-400">{label}</p>
      <p className="text-sm leading-6 text-gray-600">{value}</p>
    </div>
  );
}

function ActionLink({ href, children }) {
  const normalized = normalizeUrl(href);

  if (!normalized) {
    return null;
  }

  return (
    <a
      href={normalized}
      target="_blank"
      rel="noreferrer"
      className="inline-flex items-center rounded-xl border border-[#d8ccb7] bg-white px-4 py-2 text-sm font-bold text-[#1b2d52] transition hover:border-[#c8a864]"
    >
      {children}
    </a>
  );
}

function PaginationButton({ children, disabled, onClick, active = false }) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className={`rounded-xl px-4 py-2 text-sm font-bold transition ${
        active
          ? 'bg-[#1b2d52] text-white'
          : 'bg-white text-[#1b2d52] border border-[#e6dccb] hover:border-[#c8a864]'
      } ${disabled ? 'cursor-not-allowed opacity-50' : ''}`}
    >
      {children}
    </button>
  );
}

export default function MemberProfilePage() {
  const { memberId } = useParams();

  const [member, setMember] = useState(null);
  const [memberLoading, setMemberLoading] = useState(true);
  const [memberError, setMemberError] = useState('');

  const [draftFilters, setDraftFilters] = useState({
    search: '',
    author: '',
    year: '',
    type: '',
  });

  const [appliedFilters, setAppliedFilters] = useState({
    search: '',
    author: '',
    year: '',
    type: '',
  });

  const [publications, setPublications] = useState([]);
  const [years, setYears] = useState([]);
  const [types, setTypes] = useState([]);
  const [pubLoading, setPubLoading] = useState(true);
  const [pubError, setPubError] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  useEffect(() => {
    const loadMember = async () => {
      setMemberLoading(true);
      setMemberError('');

      try {
        const res = await memberService.getPublicById(memberId);
        setMember(res.data || null);
      } catch (err) {
        console.error('Failed to load member:', err);
        setMemberError('Unable to load this member profile.');
        setMember(null);
      } finally {
        setMemberLoading(false);
      }
    };

    loadMember();
  }, [memberId]);

  useEffect(() => {
    const loadMeta = async () => {
      try {
        const [yearsRes, typesRes] = await Promise.all([
          publicationService.getPublicationYears(),
          publicationService.getPublicationTypes(),
        ]);

        setYears(Array.isArray(yearsRes.data) ? yearsRes.data : []);
        setTypes(Array.isArray(typesRes.data) ? typesRes.data : []);
      } catch (err) {
        console.error('Failed to load publication metadata:', err);
      }
    };

    loadMeta();
  }, []);

  useEffect(() => {
    const loadPublications = async () => {
      setPubLoading(true);
      setPubError('');

      try {
        const params = {
          page,
          size: PAGE_SIZE,
          search: appliedFilters.search || undefined,
          author: appliedFilters.author || undefined,
          year: appliedFilters.year || undefined,
          type: appliedFilters.type || undefined,
        };

        const res = await publicationService.getPublicationsByMember(memberId, params);
        const data = res.data || {};

        setPublications(Array.isArray(data.content) ? data.content : []);
        setTotalPages(typeof data.totalPages === 'number' ? data.totalPages : 0);
        setTotalElements(typeof data.totalElements === 'number' ? data.totalElements : 0);
      } catch (err) {
        console.error('Failed to load member publications:', err);
        setPubError('Unable to load member publications right now.');
        setPublications([]);
        setTotalPages(0);
        setTotalElements(0);
      } finally {
        setPubLoading(false);
      }
    };

    loadPublications();
  }, [memberId, page, appliedFilters]);

  const handleDraftChange = (event) => {
    const { name, value } = event.target;
    setDraftFilters((prev) => ({ ...prev, [name]: value }));
  };

  const applyFilters = () => {
    setPage(0);
    setAppliedFilters({ ...draftFilters });
  };

  const resetFilters = () => {
    const empty = { search: '', author: '', year: '', type: '' };
    setDraftFilters(empty);
    setAppliedFilters(empty);
    setPage(0);
  };

  const openExport = (format) => {
    const params = {
      search: appliedFilters.search || undefined,
      author: appliedFilters.author || undefined,
      year: appliedFilters.year || undefined,
      type: appliedFilters.type || undefined,
    };

    const url =
      format === 'csv'
        ? publicationService.buildMemberCsvExportUrl(memberId, params)
        : publicationService.buildMemberPdfExportUrl(memberId, params);

    window.open(url, '_blank', 'noopener,noreferrer');
  };

  const paginationNumbers = useMemo(() => {
    if (totalPages <= 0) {
      return [];
    }

    const windowSize = 5;
    const start = Math.max(0, page - 2);
    const end = Math.min(totalPages, start + windowSize);

    return Array.from({ length: end - start }, (_, index) => start + index);
  }, [page, totalPages]);

  return (
    <div className="min-h-screen bg-[#f5f2ec] text-[#1b2d52]">
      <PublicHeader />

      <main className="mx-auto max-w-7xl px-6 py-10 lg:px-10">
        <div className="mb-6">
          <Link
            to="/members"
            className="inline-flex items-center rounded-full border border-[#d8ccb7] bg-white px-4 py-2 text-sm font-bold text-[#1b2d52] transition hover:border-[#c8a864]"
          >
            ← Back to Members
          </Link>
        </div>

        {memberError ? (
          <div className="rounded-2xl border border-red-200 bg-red-50 px-6 py-5 text-red-600">
            {memberError}
          </div>
        ) : memberLoading ? (
          <div className="rounded-[2rem] border border-[#e7decf] bg-white px-6 py-16 text-center shadow-xl">
            Loading member profile...
          </div>
        ) : !member ? (
          <div className="rounded-[2rem] border border-[#e7decf] bg-white px-6 py-16 text-center shadow-xl">
            Member not found.
          </div>
        ) : (
          <>
            <section className="mb-8 rounded-[2rem] bg-[#1b2d52] p-8 text-white shadow-2xl">
              <div className="flex flex-col gap-8 lg:flex-row lg:items-start">
                <div className="h-36 w-36 overflow-hidden rounded-[2rem] border border-white/10 bg-white/10">
                  {member.photoUrl ? (
                    <img
                      src={member.photoUrl}
                      alt={buildFullName(member)}
                      className="h-full w-full object-cover"
                    />
                  ) : (
                    <div className="flex h-full w-full items-center justify-center text-4xl font-bold text-[#c8a864]">
                      {buildFullName(member).charAt(0)}
                    </div>
                  )}
                </div>

                <div className="flex-1">
                  <p className="mb-4 text-xs font-bold uppercase tracking-[0.2em] sm:tracking-[0.35em] text-[#c8a864]">
                    Member Profile
                  </p>

                  <h1 className="font-brand-serif text-4xl leading-tight md:text-5xl">
                    {buildFullName(member)}
                  </h1>

                  <p className="mt-3 text-lg font-semibold text-[#e8d9b3]">
                    {getMemberGrade(member)}
                  </p>

                  <p className="mt-2 text-sm uppercase tracking-[0.2em] text-white/50">
                    {safeText(member.lab, 'MEDIATRON')}
                  </p>

                  <p className="mt-6 max-w-3xl text-sm leading-7 text-white/70 md:text-base">
                    {safeText(member.bio, 'No biography available yet.')}
                  </p>

                  <div className="mt-6 flex flex-wrap gap-3">
                    <ActionLink href={member.googleScholarUrl}>Google Scholar</ActionLink>
                    <ActionLink href={member.linkedinUrl}>LinkedIn</ActionLink>
                    <ActionLink href={member.personalSiteUrl || member.personalWebsite}>Website</ActionLink>
                    {member.orcidId ? (
                      <span className="inline-flex items-center rounded-xl bg-white/10 px-4 py-2 text-sm font-bold text-[#e8d9b3]">
                        ORCID: {member.orcidId}
                      </span>
                    ) : null}
                  </div>
                </div>
              </div>
            </section>

            <section className="mb-8 grid grid-cols-1 gap-6 lg:grid-cols-2">
              <InfoCard
                label="Research Axes"
                value={safeText(member.researchAxes, 'Not specified')}
              />
              <InfoCard
                label="Biography"
                value={safeText(member.bio, 'No biography available yet.')}
              />
            </section>

            <section className="rounded-[2rem] border border-[#e7decf] bg-white p-6 shadow-xl">
              <div className="mb-6 flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
                <div>
                  <h2 className="font-brand-serif text-2xl text-[#1b2d52]">Approved Publications</h2>
                  <p className="mt-1 text-sm text-gray-500">
                    {pubLoading ? 'Loading publications...' : `${totalElements} publication(s) found`}
                  </p>
                </div>

                <div className="flex flex-wrap gap-3">
                  <button
                    type="button"
                    onClick={() => openExport('csv')}
                    className="rounded-xl border border-[#d8ccb7] bg-[#f5f2ec] px-5 py-3 text-sm font-bold text-[#1b2d52] transition hover:border-[#c8a864]"
                  >
                    Export CSV
                  </button>

                  <button
                    type="button"
                    onClick={() => openExport('pdf')}
                    className="rounded-xl bg-[#c8a864] px-5 py-3 text-sm font-bold text-[#1b2d52] shadow-lg shadow-[#c8a864]/20 transition hover:scale-[1.02]"
                  >
                    Export PDF
                  </button>
                </div>
              </div>

              <div className="mb-6 grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
                <div className="flex flex-col gap-2">
                  <label className="text-[10px] font-bold uppercase tracking-[0.25em] text-gray-400">
                    Search
                  </label>
                  <input
                    name="search"
                    value={draftFilters.search}
                    onChange={handleDraftChange}
                    placeholder="wireless, security..."
                    className="rounded-xl border-2 border-gray-100 bg-gray-50/60 px-4 py-3 outline-none transition focus:border-[#c8a864]"
                  />
                </div>

                <div className="flex flex-col gap-2">
                  <label className="text-[10px] font-bold uppercase tracking-[0.25em] text-gray-400">
                    Author
                  </label>
                  <input
                    name="author"
                    value={draftFilters.author}
                    onChange={handleDraftChange}
                    placeholder="Letaifa"
                    className="rounded-xl border-2 border-gray-100 bg-gray-50/60 px-4 py-3 outline-none transition focus:border-[#c8a864]"
                  />
                </div>

                <div className="flex flex-col gap-2">
                  <label className="text-[10px] font-bold uppercase tracking-[0.25em] text-gray-400">
                    Year
                  </label>
                  <select
                    name="year"
                    value={draftFilters.year}
                    onChange={handleDraftChange}
                    className="rounded-xl border-2 border-gray-100 bg-gray-50/60 px-4 py-3 outline-none transition focus:border-[#c8a864]"
                  >
                    <option value="">All years</option>
                    {years.map((year) => (
                      <option key={year} value={year}>
                        {year}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="flex flex-col gap-2">
                  <label className="text-[10px] font-bold uppercase tracking-[0.25em] text-gray-400">
                    Type
                  </label>
                  <select
                    name="type"
                    value={draftFilters.type}
                    onChange={handleDraftChange}
                    className="rounded-xl border-2 border-gray-100 bg-gray-50/60 px-4 py-3 outline-none transition focus:border-[#c8a864]"
                  >
                    <option value="">All types</option>
                    {types.map((type) => (
                      <option key={type} value={type}>
                        {type}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="mb-8 flex flex-wrap gap-3">
                <button
                  type="button"
                  onClick={applyFilters}
                  className="rounded-xl bg-[#1b2d52] px-6 py-3 text-sm font-bold text-white transition hover:bg-[#2b4477]"
                >
                  Apply Filters
                </button>

                <button
                  type="button"
                  onClick={resetFilters}
                  className="rounded-xl border border-[#d8ccb7] bg-white px-6 py-3 text-sm font-bold text-[#1b2d52] transition hover:border-[#c8a864]"
                >
                  Reset
                </button>
              </div>

              {pubError ? (
                <div className="rounded-2xl border border-red-200 bg-red-50 px-5 py-4 text-sm text-red-600">
                  {pubError}
                </div>
              ) : pubLoading ? (
                <div className="py-16 text-center text-gray-500">Loading publications...</div>
              ) : publications.length === 0 ? (
                <div className="rounded-[1.5rem] border border-dashed border-[#d8ccb7] bg-[#faf8f3] px-6 py-16 text-center">
                  <h3 className="font-brand-serif text-2xl text-[#1b2d52]">No publications found</h3>
                  <p className="mt-3 text-sm text-gray-500">
                    Try changing the filters.
                  </p>
                </div>
              ) : (
                <>
                  <div className="grid grid-cols-1 gap-5 xl:grid-cols-2">
                    {publications.map((publication, index) => (
                      <article
                        key={`${publication.title}-${publication.publicationYear || 'na'}-${index}`}
                        className="rounded-[1.5rem] border border-[#ece3d4] bg-[#fffdf9] p-5 shadow-sm"
                      >
                        <div className="mb-4 flex items-start justify-between gap-4">
                          <div>
                            <h3 className="font-semibold leading-7 text-[#1b2d52]">
                              {publication.title}
                            </h3>
                            <p className="mt-2 text-sm text-gray-500">
                              {publication.publicationYear || '—'} • {publication.source || '—'}
                            </p>
                          </div>

                          <PublicationTypeBadge type={publication.type} />
                        </div>

                        <p className="mb-4 text-sm leading-6 text-gray-600">
                          {publication.authorsText || 'No authors information'}
                        </p>

                        <div className="flex flex-wrap gap-3">
                          {publication.externalUrl ? (
                            <a
                              href={publication.externalUrl}
                              target="_blank"
                              rel="noreferrer"
                              className="text-sm font-semibold text-[#1b2d52] underline decoration-[#c8a864] underline-offset-4"
                            >
                              External link
                            </a>
                          ) : null}

                          {publication.pdfUrl ? (
                            <a
                              href={publication.pdfUrl}
                              target="_blank"
                              rel="noreferrer"
                              className="text-sm font-semibold text-[#8a6f3c] underline decoration-[#c8a864] underline-offset-4"
                            >
                              PDF
                            </a>
                          ) : null}
                        </div>
                      </article>
                    ))}
                  </div>

                  <div className="mt-8 flex flex-wrap items-center justify-between gap-4">
                    <div className="text-sm text-gray-500">
                      Showing page {totalPages === 0 ? 0 : page + 1} of {totalPages}
                    </div>

                    <div className="flex flex-wrap items-center gap-2">
                      <PaginationButton
                        disabled={page === 0}
                        onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
                      >
                        Previous
                      </PaginationButton>

                      {paginationNumbers.map((pageNumber) => (
                        <PaginationButton
                          key={pageNumber}
                          active={pageNumber === page}
                          onClick={() => setPage(pageNumber)}
                        >
                          {pageNumber + 1}
                        </PaginationButton>
                      ))}

                      <PaginationButton
                        disabled={totalPages === 0 || page >= totalPages - 1}
                        onClick={() => setPage((prev) => Math.min(prev + 1, totalPages - 1))}
                      >
                        Next
                      </PaginationButton>
                    </div>
                  </div>
                </>
              )}
            </section>
          </>
        )}
      </main>
    </div>
  );
}