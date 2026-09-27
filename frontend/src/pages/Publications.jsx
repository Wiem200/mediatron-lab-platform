import { useEffect, useMemo, useState } from 'react';
import { Navigate } from 'react-router-dom';
import Sidebar from '../components/Sidebar';
import { useLanguage } from '../context/LanguageContext';
import { publicationService } from '../api/publicationService';

const STATUS_STYLES = {
  PENDING: 'bg-amber-100 text-amber-800 border border-amber-200',
  APPROVED: 'bg-emerald-100 text-emerald-800 border border-emerald-200',
  REJECTED: 'bg-rose-100 text-rose-800 border border-rose-200',
};

const DEFAULT_SORT_CONFIG = {
  sortBy: 'publicationYear',
  sortDirection: 'desc',
};

function buildSourceLink(publication) {
  const externalUrl = publication.externalUrl?.trim();

  if (externalUrl) {
    return externalUrl;
  }

  const doi = publication.doi?.trim();

  if (doi) {
    if (doi.startsWith('http://') || doi.startsWith('https://')) {
      return doi;
    }

    return `https://doi.org/${doi}`;
  }

  return '';
}

function normalizeText(value) {
  return String(value || '').trim().toLocaleLowerCase();
}

function compareTitles(firstPublication, secondPublication, direction) {
  const firstTitle = normalizeText(firstPublication.title);
  const secondTitle = normalizeText(secondPublication.title);

  if (!firstTitle && !secondTitle) {
    return 0;
  }

  if (!firstTitle) {
    return 1;
  }

  if (!secondTitle) {
    return -1;
  }

  const comparison = firstTitle.localeCompare(secondTitle, undefined, {
    sensitivity: 'base',
    numeric: true,
  });

  return direction === 'asc' ? comparison : -comparison;
}

function compareYears(firstPublication, secondPublication, direction) {
  const firstYear = Number(firstPublication.publicationYear);
  const secondYear = Number(secondPublication.publicationYear);

  const hasFirstYear = Number.isFinite(firstYear);
  const hasSecondYear = Number.isFinite(secondYear);

  if (!hasFirstYear && !hasSecondYear) {
    return compareTitles(firstPublication, secondPublication, 'asc');
  }

  if (!hasFirstYear) {
    return 1;
  }

  if (!hasSecondYear) {
    return -1;
  }

  if (firstYear === secondYear) {
    return compareTitles(firstPublication, secondPublication, 'asc');
  }

  return direction === 'asc' ? firstYear - secondYear : secondYear - firstYear;
}

function sortPublications(publications, sortConfig) {
  return [...publications].sort((firstPublication, secondPublication) => {
    if (sortConfig.sortBy === 'title') {
      const titleComparison = compareTitles(
        firstPublication,
        secondPublication,
        sortConfig.sortDirection
      );

      if (titleComparison !== 0) {
        return titleComparison;
      }

      return compareYears(firstPublication, secondPublication, 'desc');
    }

    return compareYears(firstPublication, secondPublication, sortConfig.sortDirection);
  });
}

function SortableHeader({ label, active, direction, onClick, alignRight = false }) {
  return (
    <th className={`p-4 ${alignRight ? 'text-right' : ''}`}>
      <button
        type="button"
        onClick={onClick}
        className={`inline-flex items-center gap-2 font-black uppercase transition hover:text-[#1b2d52] ${
          alignRight ? 'ml-auto justify-end' : ''
        }`}
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

export default function Publications() {
  const { language } = useLanguage();
  const user = JSON.parse(localStorage.getItem('user') || 'null');
  const role = user?.roles?.[0];

  const [items, setItems] = useState([]);
  const [sortConfig, setSortConfig] = useState(DEFAULT_SORT_CONFIG);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [actionLoadingId, setActionLoadingId] = useState(null);

  const canValidateOwnPublications = role === 'ROLE_MEMBER' || role === 'ROLE_LAB_HEAD';

  const copy = {
    en: {
      pageTitle: 'My Publications',
      subheading: 'Review imported publications linked to your profile and validate the records that actually concern you.',
      panelTitle: 'Member Validation Workflow',
      panelDescription: 'Approved publications become visible on your public profile. You can still change your decision later if a publication was linked by mistake.',
      loading: 'Loading publications...',
      noResults: 'No publications are linked to your profile.',
      title: 'Title',
      authors: 'Authors',
      year: 'Year',
      type: 'Type',
      source: 'Source',
      sourceLink: 'Link',
      status: 'Status',
      actions: 'Actions',
      approve: 'Approve',
      approveAgain: 'Approve Again',
      reject: 'Reject',
      removeApproval: 'Remove',
      pending: 'Pending',
      approved: 'Approved',
      rejected: 'Rejected',
      noSourceLink: 'No link',
      viewSource: 'Open source',
      viewDoi: 'Open DOI',
      unableLoad: 'Unable to load your publications.',
      unableUpdate: 'Unable to update publication status.',
      successApprove: 'Publication approved successfully.',
      successReject: 'Publication rejected successfully.',
      confirmApprove: 'Are you sure you want to approve this publication? It will become visible on your public profile.',
      confirmReject: 'Are you sure you want to reject/remove this publication from your approved profile publications?',
      signInPrompt: 'Only laboratory members can access this page.',
      emptyHint: 'Validated publications will stay visible here with their latest status.',
      unknown: 'Unknown',
      missingAuthors: 'Authors not available',
    },
    fr: {
      pageTitle: 'Mes Publications',
      subheading: 'Examinez les publications importees liees a votre profil et validez celles qui vous concernent reellement.',
      panelTitle: 'Workflow de Validation du Membre',
      panelDescription: 'Les publications approuvees deviennent visibles sur votre profil public. Vous pouvez modifier votre decision si une publication a ete liee par erreur.',
      loading: 'Chargement des publications...',
      noResults: 'Aucune publication n est liee a votre profil.',
      title: 'Titre',
      authors: 'Auteurs',
      year: 'Annee',
      type: 'Type',
      source: 'Source',
      sourceLink: 'Lien',
      status: 'Statut',
      actions: 'Actions',
      approve: 'Approuver',
      approveAgain: 'Re-approuver',
      reject: 'Rejeter',
      removeApproval: 'Retirer',
      pending: 'En attente',
      approved: 'Approuvee',
      rejected: 'Rejetee',
      noSourceLink: 'Aucun lien',
      viewSource: 'Ouvrir source',
      viewDoi: 'Ouvrir DOI',
      unableLoad: 'Impossible de charger vos publications.',
      unableUpdate: 'Impossible de mettre a jour le statut de la publication.',
      successApprove: 'Publication approuvee avec succes.',
      successReject: 'Publication rejetee avec succes.',
      confirmApprove: 'Voulez-vous vraiment approuver cette publication ? Elle sera visible sur votre profil public.',
      confirmReject: 'Voulez-vous vraiment rejeter/retirer cette publication de votre profil public ?',
      signInPrompt: 'Seuls les membres du laboratoire peuvent acceder a cette page.',
      emptyHint: 'Les publications validees resteront visibles ici avec leur statut courant.',
      unknown: 'Inconnu',
      missingAuthors: 'Auteurs indisponibles',
    },
  }[language];

  const sortedItems = useMemo(() => sortPublications(items, sortConfig), [items, sortConfig]);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError('');

      try {
        const res = await publicationService.getMyPublications();
        setItems(Array.isArray(res.data) ? res.data : []);
      } catch (err) {
        setItems([]);
        setError(err?.response?.data?.message || copy.unableLoad);
      } finally {
        setLoading(false);
      }
    };

    if (canValidateOwnPublications) {
      load();
    }
  }, [canValidateOwnPublications, copy.unableLoad]);

  const labelForStatus = (status) => {
    switch (status) {
      case 'APPROVED':
        return copy.approved;
      case 'REJECTED':
        return copy.rejected;
      default:
        return copy.pending;
    }
  };

  const handleSort = (sortBy) => {
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

  const handleAction = async (publicationId, action) => {
    const confirmationMessage = action === 'APPROVE'
      ? copy.confirmApprove
      : copy.confirmReject;

    const confirmed = window.confirm(confirmationMessage);

    if (!confirmed) {
      return;
    }

    setActionLoadingId(publicationId);
    setError('');
    setFeedback('');

    try {
      const response = action === 'APPROVE'
        ? await publicationService.approveMyPublication(publicationId)
        : await publicationService.rejectMyPublication(publicationId);

      const updatedPublication = response.data;

      setItems((current) => current.map((item) => (
        item.id === publicationId ? { ...item, ...updatedPublication } : item
      )));

      setFeedback(action === 'APPROVE' ? copy.successApprove : copy.successReject);
    } catch (err) {
      setError(err?.response?.data?.message || copy.unableUpdate);
    } finally {
      setActionLoadingId(null);
    }
  };

  if (!canValidateOwnPublications) {
    return <Navigate to="/dashboard" replace />;
  }

  return (
    <div className="flex min-h-screen bg-[#f5f2ec] font-sans text-[#1b2d52]">
      <Sidebar />

      <main className="w-full px-4 pb-10 pt-24 sm:px-6 lg:ml-64 lg:p-10">
        <div className="mb-8">
          <h1 className="text-3xl font-serif">{copy.pageTitle}</h1>
          <p className="mt-1 text-sm text-gray-500">{copy.subheading}</p>
        </div>

        <div className="mb-6 rounded-3xl border border-[#e7decf] bg-white p-7 shadow-md">
          <h2 className="text-4xl font-serif text-[#1b2d52]">{copy.panelTitle}</h2>
          <p className="mt-2 text-sm text-[#5d6f8f]">{copy.panelDescription}</p>
        </div>

        {feedback ? (
          <div className="mb-4 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
            {feedback}
          </div>
        ) : null}

        {error ? (
          <div className="mb-4 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
            {error}
          </div>
        ) : null}

        <div className="overflow-x-auto rounded-2xl border border-gray-100 bg-white shadow-xl">
          {loading ? (
            <div className="p-6 text-sm text-gray-500">{copy.loading}</div>
          ) : items.length === 0 ? (
            <div className="p-6 text-sm text-gray-500">
              <p>{copy.noResults}</p>
              <p className="mt-2">{copy.emptyHint}</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[1100px] text-left">
                <thead>
                  <tr className="border-b border-gray-100 bg-gray-50/60 text-xs font-black uppercase text-gray-500">
                    <SortableHeader
                      label={copy.title}
                      active={sortConfig.sortBy === 'title'}
                      direction={sortConfig.sortDirection}
                      onClick={() => handleSort('title')}
                    />

                    <th className="p-4">{copy.authors}</th>

                    <SortableHeader
                      label={copy.year}
                      active={sortConfig.sortBy === 'publicationYear'}
                      direction={sortConfig.sortDirection}
                      onClick={() => handleSort('publicationYear')}
                    />

                    <th className="p-4">{copy.type}</th>
                    <th className="p-4">{copy.source}</th>
                    <th className="p-4">{copy.sourceLink}</th>
                    <th className="p-4">{copy.status}</th>
                    <th className="p-4 text-right">{copy.actions}</th>
                  </tr>
                </thead>

                <tbody className="divide-y divide-gray-50">
                  {sortedItems.map((publication) => {
                    const status = publication.status || 'PENDING';
                    const statusClassName = STATUS_STYLES[status] || STATUS_STYLES.PENDING;
                    const isLoadingAction = actionLoadingId === publication.id;
                    const sourceLink = buildSourceLink(publication);
                    const sourceLabel = publication.externalUrl ? copy.viewSource : copy.viewDoi;

                    const approveDisabled = status === 'APPROVED' || isLoadingAction;
                    const rejectDisabled = status === 'REJECTED' || isLoadingAction;

                    const approveLabel = status === 'REJECTED' ? copy.approveAgain : copy.approve;
                    const rejectLabel = status === 'APPROVED' ? copy.removeApproval : copy.reject;

                    return (
                      <tr key={publication.id} className="align-top hover:bg-[#faf8f3]">
                        <td className="p-4 text-sm font-medium">
                          <div className="max-w-[320px]">
                            <p>{publication.title}</p>

                            {publication.doi ? (
                              <p className="mt-2 text-[11px] font-bold uppercase tracking-[0.12em] text-gray-400">
                                DOI: {publication.doi}
                              </p>
                            ) : null}
                          </div>
                        </td>

                        <td className="p-4 text-sm text-gray-600">
                          <div className="max-w-[320px]">
                            {publication.authorsText || copy.missingAuthors}
                          </div>
                        </td>

                        <td className="p-4 text-sm text-gray-600">
                          {publication.publicationYear || '-'}
                        </td>

                        <td className="p-4 text-sm text-gray-600">
                          {publication.type || copy.unknown}
                        </td>

                        <td className="p-4 text-sm text-gray-600">
                          {publication.source || '-'}
                        </td>

                        <td className="p-4 text-sm">
                          {sourceLink ? (
                            <a
                              href={sourceLink}
                              target="_blank"
                              rel="noreferrer"
                              className="inline-flex rounded-xl border border-[#d8ccb7] bg-white px-3 py-2 text-xs font-bold text-[#1b2d52] transition hover:border-[#c8a864] hover:bg-[#f5f2ec]"
                            >
                              {sourceLabel}
                            </a>
                          ) : (
                            <span className="text-xs text-gray-400">{copy.noSourceLink}</span>
                          )}
                        </td>

                        <td className="p-4 text-sm">
                          <span className={`rounded-full px-3 py-1 text-xs font-bold ${statusClassName}`}>
                            {labelForStatus(status)}
                          </span>
                        </td>

                        <td className="p-4 text-sm">
                          <div className="flex justify-end gap-2">
                            <button
                              type="button"
                              onClick={() => handleAction(publication.id, 'APPROVE')}
                              disabled={approveDisabled}
                              className="rounded-xl bg-emerald-600 px-3 py-2 text-xs font-bold text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:bg-emerald-300"
                            >
                              {isLoadingAction ? '...' : approveLabel}
                            </button>

                            <button
                              type="button"
                              onClick={() => handleAction(publication.id, 'REJECT')}
                              disabled={rejectDisabled}
                              className="rounded-xl bg-rose-600 px-3 py-2 text-xs font-bold text-white transition hover:bg-rose-700 disabled:cursor-not-allowed disabled:bg-rose-300"
                            >
                              {isLoadingAction ? '...' : rejectLabel}
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}