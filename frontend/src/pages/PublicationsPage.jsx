import { useEffect, useMemo, useState } from 'react';
import PublicHeader from '../components/PublicHeader';
import { publicationService } from '../api/publicationService';

const PAGE_SIZE = 10;

const emptyFilters = {
  search: '',
  author: '',
  year: '',
  type: '',
};

const DEFAULT_SORT_CONFIG = {
  sortBy: 'publicationYear',
  sortDirection: 'desc',
};

function PublicationTypeBadge({ type }) {
  return (
    <span className="inline-flex rounded-full bg-[#c8a864]/15 px-3 py-1 text-xs font-bold uppercase tracking-wide text-[#8a6f3c]">
      {type || 'Unknown'}
    </span>
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
          : 'border border-[#e6dccb] bg-white text-[#1b2d52] hover:border-[#c8a864]'
      } ${disabled ? 'cursor-not-allowed opacity-50' : ''}`}
    >
      {children}
    </button>
  );
}

function SortableHeader({ label, active, direction, onClick }) {
  return (
    <th className="px-5 py-4">
      <button
        type="button"
        onClick={onClick}
        className="inline-flex items-center gap-2 text-left font-black uppercase tracking-[0.15em] text-[#1b2d52] transition hover:text-[#8a6f3c]"
      >
        <span>{label}</span>
        <span className="flex flex-col text-[9px] leading-[8px]">
          <span className={active && direction === 'asc' ? 'opacity-100' : 'opacity-35'}>
            ▲
          </span>
          <span className={active && direction === 'desc' ? 'opacity-100' : 'opacity-35'}>
            ▼
          </span>
        </span>
      </button>
    </th>
  );
}

function getPublicationLink(publication) {
  return publication?.externalUrl || publication?.pdfUrl || '';
}

export default function PublicationsPage() {
  const [draftFilters, setDraftFilters] = useState(emptyFilters);
  const [appliedFilters, setAppliedFilters] = useState(emptyFilters);
  const [sortConfig, setSortConfig] = useState(DEFAULT_SORT_CONFIG);

  const [publications, setPublications] = useState([]);
  const [years, setYears] = useState([]);
  const [types, setTypes] = useState([]);

  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [loading, setLoading] = useState(true);
  const [metaLoading, setMetaLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const loadMeta = async () => {
      setMetaLoading(true);

      try {
        const [yearsRes, typesRes] = await Promise.all([
          publicationService.getPublicationYears(),
          publicationService.getPublicationTypes(),
        ]);

        setYears(Array.isArray(yearsRes.data) ? yearsRes.data : []);
        setTypes(Array.isArray(typesRes.data) ? typesRes.data : []);
      } catch (err) {
        console.error('Failed to load publication metadata:', err);
      } finally {
        setMetaLoading(false);
      }
    };

    loadMeta();
  }, []);

  useEffect(() => {
    const loadPublications = async () => {
      setLoading(true);
      setError('');

      try {
        const params = {
          page,
          size: PAGE_SIZE,
          search: appliedFilters.search || undefined,
          author: appliedFilters.author || undefined,
          year: appliedFilters.year || undefined,
          type: appliedFilters.type || undefined,
          sortBy: sortConfig.sortBy,
          sortDirection: sortConfig.sortDirection,
        };

        const res = await publicationService.getPublications(params);
        const data = res.data || {};

        setPublications(Array.isArray(data.content) ? data.content : []);
        setTotalPages(typeof data.totalPages === 'number' ? data.totalPages : 0);
        setTotalElements(typeof data.totalElements === 'number' ? data.totalElements : 0);
      } catch (err) {
        console.error('Failed to load publications:', err);
        setError('Unable to load publications right now.');
        setPublications([]);
        setTotalPages(0);
        setTotalElements(0);
      } finally {
        setLoading(false);
      }
    };

    loadPublications();
  }, [page, appliedFilters, sortConfig]);

  const handleDraftChange = (event) => {
    const { name, value } = event.target;
    setDraftFilters((prev) => ({ ...prev, [name]: value }));
  };

  const applyFilters = () => {
    setPage(0);
    setAppliedFilters({ ...draftFilters });
  };

  const resetFilters = () => {
    setDraftFilters(emptyFilters);
    setAppliedFilters(emptyFilters);
    setPage(0);
  };

  const handleSort = (sortBy) => {
    setPage(0);

    setSortConfig((current) => {
      if (current.sortBy === sortBy) {
        return {
          sortBy,
          sortDirection: current.sortDirection === 'asc' ? 'desc' : 'asc',
        };
      }

      return {
        sortBy,
        sortDirection: sortBy === 'publicationYear' ? 'desc' : 'asc',
      };
    });
  };

  const openExport = (format) => {
    const params = {
      search: appliedFilters.search || undefined,
      author: appliedFilters.author || undefined,
      year: appliedFilters.year || undefined,
      type: appliedFilters.type || undefined,
    };

    const url = format === 'csv'
      ? publicationService.buildPublicCsvExportUrl(params)
      : publicationService.buildPublicPdfExportUrl(params);

    window.open(url, '_blank', 'noopener,noreferrer');
  };

  const paginationNumbers = useMemo(() => {
    if (totalPages <= 0) {
      return [];
    }

    const windowSize = 5;
    const start = Math.max(0, Math.min(page - 2, Math.max(totalPages - windowSize, 0)));
    const end = Math.min(totalPages, start + windowSize);

    return Array.from({ length: end - start }, (_, index) => start + index);
  }, [page, totalPages]);

  return (
    <div className="min-h-screen bg-[#f5f2ec] text-[#1b2d52]">
      <PublicHeader />

      <main className="mx-auto max-w-7xl px-6 py-10 lg:px-10">
        <section className="mb-8 rounded-[2rem] bg-[#1b2d52] px-8 py-10 text-white shadow-2xl">
          <div className="max-w-3xl">
            <p className="mb-4 text-xs font-bold uppercase tracking-[0.2em] text-[#c8a864] sm:tracking-[0.35em]">
              Publications Archive
            </p>

            <h1 className="font-brand-serif text-4xl leading-tight md:text-5xl">
              Explore validated research published by the MEDIATRON laboratory.
            </h1>

            <p className="mt-5 max-w-2xl text-sm leading-7 text-white/65 md:text-base">
              Browse approved publications, filter by topic, author, year, and type, then export the
              current result set in CSV or PDF format.
            </p>
          </div>
        </section>

        <section className="mb-8 rounded-[2rem] border border-[#e7decf] bg-white p-6 shadow-xl">
          <div className="mb-6 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div>
              <h2 className="font-brand-serif text-2xl text-[#1b2d52]">Filter Publications</h2>
              <p className="mt-1 text-sm text-gray-500">
                Use one or more filters to narrow the visible research records.
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

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
            <div className="flex flex-col gap-2">
              <label className="text-[10px] font-bold uppercase tracking-[0.25em] text-gray-400">
                Search
              </label>
              <input
                name="search"
                value={draftFilters.search}
                onChange={handleDraftChange}
                placeholder="wireless, security, IoT..."
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
                placeholder=""
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
                disabled={metaLoading}
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
                disabled={metaLoading}
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

          <div className="mt-6 flex flex-wrap gap-3">
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
        </section>

        <section className="rounded-[2rem] border border-[#e7decf] bg-white p-6 shadow-xl">
          <div className="mb-6 flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
            <div>
              <h2 className="font-brand-serif text-2xl text-[#1b2d52]">Approved Publications</h2>
              <p className="mt-1 text-sm text-gray-500">
                {loading ? 'Loading results...' : `${totalElements} publication(s) found`}
              </p>
            </div>

            <div className="rounded-full bg-[#f5f2ec] px-4 py-2 text-xs font-bold uppercase tracking-[0.2em] text-[#8a6f3c]">
              Page {totalPages === 0 ? 0 : page + 1} / {totalPages}
            </div>
          </div>

          {error ? (
            <div className="rounded-2xl border border-red-200 bg-red-50 px-5 py-4 text-sm text-red-600">
              {error}
            </div>
          ) : loading ? (
            <div className="py-16 text-center text-gray-500">Loading publications...</div>
          ) : publications.length === 0 ? (
            <div className="rounded-[1.5rem] border border-dashed border-[#d8ccb7] bg-[#faf8f3] px-6 py-16 text-center">
              <h3 className="font-brand-serif text-2xl text-[#1b2d52]">No publications found</h3>
              <p className="mt-3 text-sm text-gray-500">
                Try changing the search text or removing one of the filters.
              </p>
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[680px] text-left">
                  <thead>
                    <tr className="border-b border-gray-100 bg-gray-50/70 text-[11px] font-black uppercase tracking-[0.15em] text-[#1b2d52]">
                      <SortableHeader
                        label="Title"
                        active={sortConfig.sortBy === 'title'}
                        direction={sortConfig.sortDirection}
                        onClick={() => handleSort('title')}
                      />

                      <SortableHeader
                        label="Year"
                        active={sortConfig.sortBy === 'publicationYear'}
                        direction={sortConfig.sortDirection}
                        onClick={() => handleSort('publicationYear')}
                      />

                      <th className="px-5 py-4">Authors</th>
                      <th className="px-5 py-4">Type</th>
                    </tr>
                  </thead>

                  <tbody className="divide-y divide-gray-50">
                    {publications.map((publication, index) => {
                      const publicationLink = getPublicationLink(publication);
                      const publicationTitle = publication.title || 'Untitled publication';

                      return (
                        <tr
                          key={`${publicationTitle}-${publication.publicationYear || 'na'}-${index}`}
                          className="align-top transition hover:bg-[#faf8f3]"
                        >
                          <td className="px-5 py-5">
                            <div className="max-w-[430px]">
                              {publicationLink ? (
                                <a
                                  href={publicationLink}
                                  target="_blank"
                                  rel="noreferrer"
                                  className="font-semibold leading-6 text-[#1b2d52] underline decoration-[#c8a864] decoration-2 underline-offset-4 transition hover:text-[#8a6f3c]"
                                >
                                  {publicationTitle}
                                </a>
                              ) : (
                                <p className="font-semibold leading-6 text-[#1b2d52]">
                                  {publicationTitle}
                                </p>
                              )}
                            </div>
                          </td>

                          <td className="px-5 py-5 text-sm text-gray-600">
                            {publication.publicationYear || '—'}
                          </td>

                          <td className="px-5 py-5">
                            <p className="max-w-[360px] text-sm leading-6 text-gray-600">
                              {publication.authorsText || '—'}
                            </p>
                          </td>

                          <td className="px-5 py-5">
                            <PublicationTypeBadge type={publication.type} />
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
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
      </main>
    </div>
  );
}