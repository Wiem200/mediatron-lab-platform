import { useDeferredValue, useEffect, useMemo, useState } from 'react';
import { Navigate } from 'react-router-dom';
import Sidebar from '../components/Sidebar';
import ToastNotice from '../components/ToastNotice';
import { memberService } from '../api/memberService';
import { phdService } from '../api/phdService';
import { projectService } from '../api/projectService';
import { getPrimaryRole, hasAdminPrivileges, useStoredUser } from '../utils/authStore';

const emptyFormState = {
  title: '',
  description: '',
  duration: '',
  funding: '',
  githubLink: '',
  videoLink: '',
  pdfLink: '',
  posterLink: '',
  responsibleMemberId: '',
  memberIds: [],
  phdStudentIds: [],
  keywords: [],
};

function isValidUrl(value) {
  if (!value) {
    return true;
  }

  try {
    new URL(value);
    return true;
  } catch {
    return false;
  }
}

function formatMemberName(member) {
  const fullName = `${member.firstName || ''} ${member.lastName || ''}`.trim();
  return fullName || member.email || `Member #${member.id}`;
}

function formatPhdName(student) {
  return `${student.firstName || ''} ${student.lastName || ''}`.trim() || `PhD Student #${student.id}`;
}

const projectStatusClasses = {
  PENDING_HEAD_VALIDATION: 'border-amber-200 bg-amber-100 text-amber-800',
  APPROVED: 'border-emerald-200 bg-emerald-100 text-emerald-800',
  REJECTED: 'border-rose-200 bg-rose-100 text-rose-700',
};

function formatProjectStatus(status) {
  return status ? status.replaceAll('_', ' ') : 'Status unavailable';
}

function formatValidationDate(value) {
  if (!value) {
    return 'Validation date unavailable';
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleString();
}

function normalizeProjectForForm(project) {
  return {
    title: project.title || '',
    description: project.description || '',
    duration: project.duration || '',
    funding: project.funding || '',
    githubLink: project.githubLink || '',
    videoLink: project.videoLink || '',
    pdfLink: project.pdfLink || '',
    posterLink: project.posterLink || '',
    responsibleMemberId: project.responsibleMemberId ? String(project.responsibleMemberId) : '',
    memberIds: Array.isArray(project.memberIds) ? project.memberIds : [],
    phdStudentIds: Array.isArray(project.phdStudentIds) ? project.phdStudentIds : [],
    keywords: Array.isArray(project.keywords) ? project.keywords : [],
  };
}

function SelectionPanel({
  title,
  searchValue,
  onSearchChange,
  items,
  selectedIds,
  onToggle,
  getItemLabel,
  subtitleKey,
  emptyMessage,
}) {
  const visibleItems = useMemo(() => {
    const query = searchValue.trim().toLowerCase();
    if (!query) {
      return items;
    }

    return items.filter((item) => {
      const label = getItemLabel(item).toLowerCase();
      const subtitle = subtitleKey && item[subtitleKey] ? String(item[subtitleKey]).toLowerCase() : '';
      return label.includes(query) || subtitle.includes(query);
    });
  }, [getItemLabel, items, searchValue, subtitleKey]);

  return (
    <section className="rounded-3xl border border-[#e7decf] bg-white/80 p-5">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div>
          <h3 className="text-sm font-black uppercase tracking-[0.28em] text-[#c8a864]">{title}</h3>
          <p className="mt-1 text-xs text-[#667799]">{selectedIds.length} selected</p>
        </div>
        <input
          value={searchValue}
          onChange={(event) => onSearchChange(event.target.value)}
          placeholder={`Search ${title.toLowerCase()}`}
          className="w-full max-w-52 rounded-2xl border border-[#e6ebf2] bg-[#fbfcfe] px-4 py-2 text-sm outline-none focus:border-[#c8a864]"
        />
      </div>

      <div className="max-h-72 space-y-2 overflow-y-auto pr-1">
        {visibleItems.length === 0 && (
          <div className="rounded-2xl border border-dashed border-[#d8dfeb] px-4 py-5 text-sm text-[#7b879d]">
            {emptyMessage}
          </div>
        )}

        {visibleItems.map((item) => {
          const checked = selectedIds.includes(item.id);
          return (
            <label
              key={item.id}
              className={`flex cursor-pointer items-start gap-3 rounded-2xl border px-4 py-3 transition ${
                checked
                  ? 'border-[#c8a864] bg-[#fff7e7]'
                  : 'border-[#edf1f7] bg-[#fbfcfe] hover:border-[#d3dcea]'
              }`}
            >
              <input
                type="checkbox"
                checked={checked}
                onChange={() => onToggle(item.id)}
                className="mt-1 h-4 w-4 accent-[#1b2d52]"
              />
              <div>
                <div className="text-sm font-semibold text-[#1b2d52]">{getItemLabel(item)}</div>
                {subtitleKey && item[subtitleKey] && (
                  <div className="mt-1 text-xs text-[#71809b]">{item[subtitleKey]}</div>
                )}
              </div>
            </label>
          );
        })}
      </div>
    </section>
  );
}

export default function Projects() {
  const user = useStoredUser();
  const role = getPrimaryRole(user);
  const canManageProjects = hasAdminPrivileges(user);
  const [projects, setProjects] = useState([]);
  const [members, setMembers] = useState([]);
  const [phdStudents, setPhdStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [feedbackTarget, setFeedbackTarget] = useState(null);
  const [editingId, setEditingId] = useState(null);
  const [search, setSearch] = useState('');
  const [leaderFilter, setLeaderFilter] = useState('');
  const [keywordFilter, setKeywordFilter] = useState('');
  const [memberSearch, setMemberSearch] = useState('');
  const [phdSearch, setPhdSearch] = useState('');
  const [keywordInput, setKeywordInput] = useState('');
  const [formData, setFormData] = useState(emptyFormState);
  const [formErrors, setFormErrors] = useState({});

  const deferredSearch = useDeferredValue(search);

  useEffect(() => {
    if (!user?.token || !canManageProjects) {
      return;
    }

    const loadData = async () => {
      setLoading(true);
      setError('');
      try {
        const [projectsResponse, membersResponse, phdResponse] = await Promise.all([
          projectService.getAll(),
          memberService.getAll(),
          phdService.getAll(),
        ]);

        setProjects(projectsResponse.data || []);
        setMembers(membersResponse.data || []);
        setPhdStudents(phdResponse.data || []);
      } catch (err) {
        const message =
          err?.response?.data?.message ||
          err?.response?.data?.error ||
          (typeof err?.response?.data === 'string' ? err.response.data : null) ||
          err?.message ||
          'Unable to load research projects.';
        setError(message);
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [canManageProjects, user?.token]);

  const sortedMembers = useMemo(
    () => [...members].sort((left, right) => formatMemberName(left).localeCompare(formatMemberName(right))),
    [members]
  );
  const sortedPhdStudents = useMemo(
    () => [...phdStudents].sort((left, right) => formatPhdName(left).localeCompare(formatPhdName(right))),
    [phdStudents]
  );
  const keywordOptions = useMemo(() => {
    const uniqueKeywords = new Set();
    projects.forEach((project) => {
      (project.keywords || []).forEach((keyword) => uniqueKeywords.add(keyword));
    });
    return Array.from(uniqueKeywords).sort((left, right) => left.localeCompare(right));
  }, [projects]);

  const filteredProjects = useMemo(() => {
    const query = deferredSearch.trim().toLowerCase();
    return projects.filter((project) => {
      const matchesSearch =
        !query ||
        [
          project.title,
          project.description,
          project.funding,
          project.responsibleMemberName,
          ...(project.keywords || []),
          ...(project.memberNames || []),
          ...(project.phdStudentNames || []),
        ]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(query));

      const matchesLeader = !leaderFilter || String(project.responsibleMemberId || '') === String(leaderFilter);
      const matchesKeyword = !keywordFilter || (project.keywords || []).includes(keywordFilter);
      return matchesSearch && matchesLeader && matchesKeyword;
    });
  }, [deferredSearch, keywordFilter, leaderFilter, projects]);

  const linkedResearcherCount = useMemo(() => {
    const ids = new Set();
    projects.forEach((project) => (project.memberIds || []).forEach((id) => ids.add(id)));
    return ids.size;
  }, [projects]);
  const linkedPhdCount = useMemo(() => {
    const ids = new Set();
    projects.forEach((project) => (project.phdStudentIds || []).forEach((id) => ids.add(id)));
    return ids.size;
  }, [projects]);

  if (!user?.token) {
    return <Navigate to="/login" replace />;
  }

  if (!canManageProjects) {
    return <Navigate to="/dashboard" replace />;
  }

  const setMessage = (type, message) => setFeedback({ type, message });

  const refreshProjects = async () => {
    const response = await projectService.getAll();
    setProjects(response.data || []);
  };

  const openFeedbackModal = (project) => {
    if (project?.status === 'REJECTED' && project.latestValidationComment) {
      setFeedbackTarget(project);
    }
  };

  const renderStatusBadge = (project) => {
    if (project.status === 'APPROVED') {
      return null;
    }

    const classes = projectStatusClasses[project.status] || 'border-slate-200 bg-slate-100 text-slate-700';
    const baseClass = `inline-flex max-w-full items-center rounded-full border px-3 py-1 text-[10px] font-black uppercase leading-tight tracking-[0.14em] ${classes}`;

    if (project.status === 'REJECTED' && project.latestValidationComment) {
      return (
        <button
          type="button"
          onClick={() => openFeedbackModal(project)}
          className={`${baseClass} transition hover:brightness-95`}
          title="View Lab Head feedback"
        >
          <span className="min-w-0 break-words">{formatProjectStatus(project.status)}</span>
        </button>
      );
    }

    return (
      <span className={baseClass}>
        <span className="min-w-0 break-words">{formatProjectStatus(project.status)}</span>
      </span>
    );
  };

  const closeFormModal = () => {
    setShowForm(false);
    setEditingId(null);
    setFormErrors({});
  };

  const openCreateModal = () => {
    setEditingId(null);
    setFormData(emptyFormState);
    setKeywordInput('');
    setMemberSearch('');
    setPhdSearch('');
    setFormErrors({});
    setShowForm(true);
  };

  const openEditModal = (project) => {
    setEditingId(project.id);
    setFormData(normalizeProjectForForm(project));
    setKeywordInput('');
    setMemberSearch('');
    setPhdSearch('');
    setFormErrors({});
    setShowForm(true);
  };

  const handleFieldChange = (event) => {
    const { name, value } = event.target;
    setFormData((current) => ({ ...current, [name]: value }));
    setFormErrors((current) => ({ ...current, [name]: undefined }));
  };

  const toggleMember = (memberId) => {
    setFormData((current) => ({
      ...current,
      memberIds: current.memberIds.includes(memberId)
        ? current.memberIds.filter((id) => id !== memberId)
        : [...current.memberIds, memberId],
    }));
  };

  const togglePhdStudent = (studentId) => {
    setFormData((current) => ({
      ...current,
      phdStudentIds: current.phdStudentIds.includes(studentId)
        ? current.phdStudentIds.filter((id) => id !== studentId)
        : [...current.phdStudentIds, studentId],
    }));
  };

  const addKeyword = () => {
    const normalized = keywordInput.trim().replace(/,$/, '');
    if (!normalized) {
      return;
    }

    setFormData((current) => ({
      ...current,
      keywords: current.keywords.includes(normalized) ? current.keywords : [...current.keywords, normalized],
    }));
    setKeywordInput('');
  };

  const removeKeyword = (keywordToRemove) => {
    setFormData((current) => ({
      ...current,
      keywords: current.keywords.filter((keyword) => keyword !== keywordToRemove),
    }));
  };

  const handleKeywordKeyDown = (event) => {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      addKeyword();
    }
  };

  const validateForm = () => {
    const nextErrors = {};
    if (!formData.title.trim()) nextErrors.title = 'Title is required.';
    if (!formData.description.trim()) nextErrors.description = 'Detailed description is required.';
    if (!formData.duration.trim()) nextErrors.duration = 'Duration is required.';
    if (!formData.responsibleMemberId) nextErrors.responsibleMemberId = 'Select the responsible member.';
    if (formData.githubLink && !isValidUrl(formData.githubLink)) nextErrors.githubLink = 'Enter a valid GitHub URL.';
    if (formData.videoLink && !isValidUrl(formData.videoLink)) nextErrors.videoLink = 'Enter a valid video URL.';
    if (formData.pdfLink && !isValidUrl(formData.pdfLink)) nextErrors.pdfLink = 'Enter a valid PDF URL.';
    if (formData.posterLink && !isValidUrl(formData.posterLink)) nextErrors.posterLink = 'Enter a valid poster URL.';
    return nextErrors;
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const nextErrors = validateForm();
    setFormErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) {
      setMessage('error', 'Fix the highlighted project fields before saving.');
      return;
    }

    const payload = {
      title: formData.title.trim(),
      description: formData.description.trim(),
      duration: formData.duration.trim(),
      funding: formData.funding.trim(),
      githubLink: formData.githubLink.trim(),
      videoLink: formData.videoLink.trim(),
      pdfLink: formData.pdfLink.trim(),
      posterLink: formData.posterLink.trim(),
      responsibleMemberId: Number(formData.responsibleMemberId),
      memberIds: Array.from(new Set(formData.memberIds)),
      phdStudentIds: Array.from(new Set(formData.phdStudentIds)),
      keywords: formData.keywords,
    };

    setSaving(true);
    try {
      if (editingId) {
        await projectService.update(editingId, payload);
        setMessage('success', 'Research project updated and sent for Lab Head validation.');
      } else {
        await projectService.add(payload);
        setMessage('success', 'Research project created and sent for Lab Head validation.');
      }
      closeFormModal();
      await refreshProjects();
    } catch (err) {
      const message =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        (typeof err?.response?.data === 'string' ? err.response.data : null) ||
        err?.message ||
        'Unable to save this research project.';
      setMessage('error', message);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) {
      return;
    }

    try {
      await projectService.delete(deleteTarget.id, true);
      setDeleteTarget(null);
      await refreshProjects();
      setMessage('success', `"${deleteTarget.title}" was deleted.`);
    } catch (err) {
      const message =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        (typeof err?.response?.data === 'string' ? err.response.data : null) ||
        err?.message ||
        'Delete failed.';
      setMessage('error', message);
    }
  };

  return (
    <div className="flex min-h-screen bg-[#f5f2ec] font-sans text-[#1b2d52]">
      <Sidebar />
      <ToastNotice toast={feedback} onClose={() => setFeedback(null)} />

      <main className="w-full px-4 pb-10 pt-24 sm:px-6 lg:ml-64 lg:p-10">
        <div className="mb-8 flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">
          <div>
                        <h1 className="text-3xl font-serif uppercase tracking-tight sm:text-4xl">Research Projects</h1>
            <p className="mt-3 max-w-3xl text-sm text-[#637390]">
              Create, update, and retire scientific projects while keeping their researchers and PhD students linked to the laboratory directory.
            </p>
          </div>

          <button
            type="button"
            onClick={openCreateModal}
            className="rounded-2xl bg-[#1b2d52] px-8 py-3 text-sm font-bold text-white shadow-lg transition hover:bg-[#2c4375]"
          >
            + ADD PROJECT
          </button>
        </div>

        {error && (
          <div className="mb-6 rounded-2xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">
            {error}
          </div>
        )}

        <div className="mb-8 grid grid-cols-1 gap-4 md:grid-cols-3">
          <div className="rounded-3xl border border-[#eadfc9] bg-white p-6 shadow-sm">
            <p className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#97a4bc]">Projects</p>
            <p className="mt-3 text-4xl font-serif">{projects.length}</p>
            <p className="mt-2 text-sm text-[#6b7b95]">Active records available for the project directory.</p>
          </div>
          <div className="rounded-3xl border border-[#eadfc9] bg-white p-6 shadow-sm">
            <p className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#97a4bc]">Researchers</p>
            <p className="mt-3 text-4xl font-serif">{linkedResearcherCount}</p>
            <p className="mt-2 text-sm text-[#6b7b95]">Unique laboratory members linked across the current portfolio.</p>
          </div>
          <div className="rounded-3xl border border-[#eadfc9] bg-white p-6 shadow-sm">
            <p className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#97a4bc]">PhD Students</p>
            <p className="mt-3 text-4xl font-serif">{linkedPhdCount}</p>
            <p className="mt-2 text-sm text-[#6b7b95]">Doctoral researchers participating in tracked projects.</p>
          </div>
        </div>

        <section className="mb-8 rounded-[2rem] border border-[#e3dac9] bg-white p-6 shadow-lg">
          <div className="mb-6">
            <h2 className="text-3xl font-serif">Search And Filter</h2>
            <p className="mt-2 text-sm text-[#677791]">
              Narrow the project list by title, description, linked people, or project keywords.
            </p>
          </div>

          <div className="grid grid-cols-1 gap-4 lg:grid-cols-[2fr_1fr_1fr]">
            <label className="block">
              <span className="text-[11px] font-black uppercase tracking-[0.2em] sm:tracking-[0.35em] text-[#9aa6bf]">Search</span>
              <input
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="AI, cyber-physical systems, smart sensing..."
                className="mt-2 w-full rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
              />
            </label>

            <label className="block">
              <span className="text-[11px] font-black uppercase tracking-[0.2em] sm:tracking-[0.35em] text-[#9aa6bf]">Leader</span>
              <select
                value={leaderFilter}
                onChange={(event) => setLeaderFilter(event.target.value)}
                className="mt-2 w-full rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
              >
                <option value="">All responsible members</option>
                {sortedMembers.map((member) => (
                  <option key={member.id} value={member.id}>
                    {formatMemberName(member)}
                  </option>
                ))}
              </select>
            </label>

            <label className="block">
              <span className="text-[11px] font-black uppercase tracking-[0.2em] sm:tracking-[0.35em] text-[#9aa6bf]">Keyword</span>
              <select
                value={keywordFilter}
                onChange={(event) => setKeywordFilter(event.target.value)}
                className="mt-2 w-full rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
              >
                <option value="">All keywords</option>
                {keywordOptions.map((keyword) => (
                  <option key={keyword} value={keyword}>
                    {keyword}
                  </option>
                ))}
              </select>
            </label>
          </div>
        </section>

        <section>
          {loading ? (
            <div className="rounded-[2rem] border border-[#e3dac9] bg-white px-6 py-12 text-center text-sm text-[#71809b] shadow-lg">
              Loading research projects...
            </div>
          ) : filteredProjects.length === 0 ? (
            <div className="rounded-[2rem] border border-dashed border-[#d8dfeb] bg-white/70 px-6 py-12 text-center text-sm text-[#71809b]">
              No projects match the current search and filter selection.
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-5 xl:grid-cols-2">
              {filteredProjects.map((project) => (
                <article
                  key={project.id}
                  className="rounded-[2rem] border border-[#e3dac9] bg-white p-6 shadow-lg transition hover:-translate-y-1 hover:shadow-xl"
                >
                  <div className="mb-4 flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                    <div className="min-w-0 flex-1">
                      <div className="mb-3 flex max-w-full flex-wrap items-center gap-2">
                        {project.duration ? (
                          <span className="inline-flex max-w-full items-center rounded-full bg-[#eef2f8] px-3 py-1 text-[10px] font-black uppercase leading-tight tracking-[0.14em] text-[#5b6c87]">
                            <span className="min-w-0 break-words">{project.duration}</span>
                          </span>
                        ) : null}

                        {renderStatusBadge(project)}

                        <span className="inline-flex max-w-full items-center rounded-full bg-[#fff3d8] px-3 py-1 text-[10px] font-black uppercase leading-tight tracking-[0.14em] text-[#a07a17]">
                          <span className="mr-1 shrink-0">Lead:</span>
                          <span className="min-w-0 break-words">{project.responsibleMemberName || 'Not assigned'}</span>
                        </span>
                      </div>
                      <h2 className="text-2xl font-serif text-[#1b2d52]">{project.title}</h2>
                      <p className="mt-3 text-sm leading-6 text-[#61718c]">{project.description}</p>
                      {project.status === 'REJECTED' && project.latestValidationComment && (
                        <button
                          type="button"
                          onClick={() => openFeedbackModal(project)}
                          className="mt-3 text-xs font-bold uppercase tracking-[0.18em] text-rose-600 underline decoration-rose-300"
                        >
                          View Lab Head feedback
                        </button>
                      )}
                    </div>

                    <div className="flex shrink-0 flex-row flex-wrap gap-2 lg:justify-end">
                      <button
                        type="button"
                        onClick={() => openEditModal(project)}
                        className="inline-flex min-h-[40px] items-center justify-center whitespace-nowrap rounded-xl border border-[#d5dded] bg-white px-4 py-2 text-[11px] font-black uppercase tracking-[0.12em] text-[#1b2d52] transition hover:border-[#c8a864] hover:bg-[#f6f8fb]"
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        onClick={() => setDeleteTarget(project)}
                        className="inline-flex min-h-[40px] items-center justify-center whitespace-nowrap rounded-xl border border-rose-200 bg-white px-4 py-2 text-[11px] font-black uppercase tracking-[0.12em] text-rose-600 transition hover:border-rose-300 hover:bg-rose-50"
                      >
                        Delete
                      </button>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                    <div className="rounded-3xl bg-[#f8f9fc] p-4">
                      <p className="text-[11px] font-black uppercase tracking-[0.28em] text-[#97a4bc]">Funding</p>
                      <p className="mt-2 text-sm text-[#54647f]">{project.funding || 'Funding details not specified.'}</p>
                    </div>

                    <div className="rounded-3xl bg-[#f8f9fc] p-4">
                      <p className="text-[11px] font-black uppercase tracking-[0.28em] text-[#97a4bc]">Resources</p>
                      <div className="mt-2 space-y-2 text-sm">
                        {project.githubLink ? (
                          <a className="block text-[#1b2d52] underline decoration-[#c8a864]" href={project.githubLink} target="_blank" rel="noreferrer">
                            GitHub Repository
                          </a>
                        ) : (
                          <span className="block text-[#7b879d]">No GitHub link</span>
                        )}
                        {project.videoLink ? (
                          <a className="block text-[#1b2d52] underline decoration-[#c8a864]" href={project.videoLink} target="_blank" rel="noreferrer">
                            Video Overview
                          </a>
                        ) : (
                          <span className="block text-[#7b879d]">No video link</span>
                        )}
                        {project.pdfLink ? (
                          <a className="block text-[#1b2d52] underline decoration-[#c8a864]" href={project.pdfLink} target="_blank" rel="noreferrer">
                            PDF Report
                          </a>
                        ) : (
                          <span className="block text-[#7b879d]">No PDF link</span>
                        )}
                        {project.posterLink ? (
                          <a className="block text-[#1b2d52] underline decoration-[#c8a864]" href={project.posterLink} target="_blank" rel="noreferrer">
                            Poster
                          </a>
                        ) : (
                          <span className="block text-[#7b879d]">No poster link</span>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="mt-5 space-y-4">
                    <div>
                      <p className="text-[11px] font-black uppercase tracking-[0.28em] text-[#97a4bc]">Keywords</p>
                      <div className="mt-2 flex flex-wrap gap-2">
                        {(project.keywords || []).length > 0 ? (
                          project.keywords.map((keyword) => (
                            <span key={keyword} className="rounded-full bg-[#edf4ff] px-3 py-1 text-xs font-semibold text-[#30517d]">
                              {keyword}
                            </span>
                          ))
                        ) : (
                          <span className="text-sm text-[#7b879d]">No keywords recorded.</span>
                        )}
                      </div>
                    </div>

                    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
                      <div>
                        <p className="text-[11px] font-black uppercase tracking-[0.28em] text-[#97a4bc]">Associated Members</p>
                        <div className="mt-2 flex flex-wrap gap-2">
                          {(project.memberNames || []).map((name) => (
                            <span key={name} className="rounded-full bg-[#fff7e8] px-3 py-1 text-xs font-semibold text-[#8f6a12]">
                              {name}
                            </span>
                          ))}
                          {(!project.memberNames || project.memberNames.length === 0) && (
                            <span className="text-sm text-[#7b879d]">No members linked.</span>
                          )}
                        </div>
                      </div>

                      <div>
                        <p className="text-[11px] font-black uppercase tracking-[0.28em] text-[#97a4bc]">Associated PhD Students</p>
                        <div className="mt-2 flex flex-wrap gap-2">
                          {(project.phdStudentNames || []).map((name) => (
                            <span key={name} className="rounded-full bg-[#eef8f2] px-3 py-1 text-xs font-semibold text-[#1f7452]">
                              {name}
                            </span>
                          ))}
                          {(!project.phdStudentNames || project.phdStudentNames.length === 0) && (
                            <span className="text-sm text-[#7b879d]">No PhD students linked.</span>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </main>

      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#122243]/70 p-4 backdrop-blur-sm">
          <div className="max-h-[92vh] w-full max-w-6xl overflow-y-auto rounded-[1.75rem] sm:rounded-[2.5rem] bg-[#fcfbf7] p-6 shadow-2xl sm:p-8 lg:p-10">
            <div className="mb-8 flex flex-col gap-3 border-b border-[#ece2d3] pb-5 lg:flex-row lg:items-end lg:justify-between">
              <div>
                <p className="text-xs font-black uppercase tracking-[0.2em] sm:tracking-[0.35em] text-[#c8a864]">
                  {editingId ? 'Update Project' : 'Create Project'}
                </p>
                <h2 className="mt-2 text-3xl font-serif text-[#1b2d52]">
                  {editingId ? 'Edit Research Project' : 'Register Research Project'}
                </h2>
              </div>
              <p className="max-w-xl text-sm text-[#6d7b93]">
                Link the project to laboratory researchers and doctoral students, then capture discoverability metadata for the public directory.
              </p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-8">
              <section className="rounded-[2rem] border border-[#ece2d3] bg-white p-6">
                <h3 className="mb-5 text-sm font-black uppercase tracking-[0.28em] text-[#c8a864]">Project Core</h3>
                <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
                  <label className="block">
                    <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">Title</span>
                    <input
                      name="title"
                      value={formData.title}
                      onChange={handleFieldChange}
                      className={`mt-2 w-full rounded-2xl border px-4 py-3 text-sm outline-none ${
                        formErrors.title ? 'border-rose-300 bg-rose-50' : 'border-[#e5e9f0]'
                      }`}
                      required
                    />
                    {formErrors.title && <p className="mt-2 text-xs text-rose-600">{formErrors.title}</p>}
                  </label>

                  <label className="block">
                    <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">Duration</span>
                    <input
                      name="duration"
                      value={formData.duration}
                      onChange={handleFieldChange}
                      placeholder="36 months, 2025 - 2028..."
                      className={`mt-2 w-full rounded-2xl border px-4 py-3 text-sm outline-none ${
                        formErrors.duration ? 'border-rose-300 bg-rose-50' : 'border-[#e5e9f0]'
                      }`}
                      required
                    />
                    {formErrors.duration && <p className="mt-2 text-xs text-rose-600">{formErrors.duration}</p>}
                  </label>

                  <label className="block lg:col-span-2">
                    <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">Detailed Description</span>
                    <textarea
                      name="description"
                      value={formData.description}
                      onChange={handleFieldChange}
                      className={`mt-2 min-h-36 w-full rounded-[1.75rem] border px-4 py-4 text-sm outline-none ${
                        formErrors.description ? 'border-rose-300 bg-rose-50' : 'border-[#e5e9f0]'
                      }`}
                      required
                    />
                    {formErrors.description && <p className="mt-2 text-xs text-rose-600">{formErrors.description}</p>}
                  </label>

                  <label className="block">
                    <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">Funding</span>
                    <input
                      name="funding"
                      value={formData.funding}
                      onChange={handleFieldChange}
                      placeholder="National grant, Horizon Europe..."
                      className="mt-2 w-full rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
                    />
                  </label>

                  <label className="block">
                    <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">Responsible Member</span>
                    <select
                      name="responsibleMemberId"
                      value={formData.responsibleMemberId}
                      onChange={handleFieldChange}
                      className={`mt-2 w-full rounded-2xl border px-4 py-3 text-sm outline-none ${
                        formErrors.responsibleMemberId ? 'border-rose-300 bg-rose-50' : 'border-[#e5e9f0]'
                      }`}
                      required
                    >
                      <option value="">Select project leader</option>
                      {sortedMembers.map((member) => (
                        <option key={member.id} value={member.id}>
                          {formatMemberName(member)}
                        </option>
                      ))}
                    </select>
                    {formErrors.responsibleMemberId && (
                      <p className="mt-2 text-xs text-rose-600">{formErrors.responsibleMemberId}</p>
                    )}
                  </label>

                  <label className="block">
                    <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">GitHub Link</span>
                    <input
                      name="githubLink"
                      value={formData.githubLink}
                      onChange={handleFieldChange}
                      placeholder="https://github.com/..."
                      className={`mt-2 w-full rounded-2xl border px-4 py-3 text-sm outline-none ${
                        formErrors.githubLink ? 'border-rose-300 bg-rose-50' : 'border-[#e5e9f0]'
                      }`}
                    />
                    {formErrors.githubLink && <p className="mt-2 text-xs text-rose-600">{formErrors.githubLink}</p>}
                  </label>

                  <label className="block">
                    <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">Video Link</span>
                    <input
                      name="videoLink"
                      value={formData.videoLink}
                      onChange={handleFieldChange}
                      placeholder="https://youtube.com/..."
                      className={`mt-2 w-full rounded-2xl border px-4 py-3 text-sm outline-none ${
                        formErrors.videoLink ? 'border-rose-300 bg-rose-50' : 'border-[#e5e9f0]'
                      }`}
                    />
                    {formErrors.videoLink && <p className="mt-2 text-xs text-rose-600">{formErrors.videoLink}</p>}
                  </label>

                  <label className="block">
                    <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">PDF Link</span>
                    <input
                      name="pdfLink"
                      value={formData.pdfLink}
                      onChange={handleFieldChange}
                      placeholder="https://.../report.pdf"
                      className={`mt-2 w-full rounded-2xl border px-4 py-3 text-sm outline-none ${
                        formErrors.pdfLink ? 'border-rose-300 bg-rose-50' : 'border-[#e5e9f0]'
                      }`}
                    />
                    {formErrors.pdfLink && <p className="mt-2 text-xs text-rose-600">{formErrors.pdfLink}</p>}
                  </label>

                  <label className="block">
                    <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">Poster Link</span>
                    <input
                      name="posterLink"
                      value={formData.posterLink}
                      onChange={handleFieldChange}
                      placeholder="https://.../poster"
                      className={`mt-2 w-full rounded-2xl border px-4 py-3 text-sm outline-none ${
                        formErrors.posterLink ? 'border-rose-300 bg-rose-50' : 'border-[#e5e9f0]'
                      }`}
                    />
                    {formErrors.posterLink && <p className="mt-2 text-xs text-rose-600">{formErrors.posterLink}</p>}
                  </label>
                </div>
              </section>

              <section className="rounded-[2rem] border border-[#ece2d3] bg-white p-6">
                <div className="mb-5 flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
                  <div>
                    <h3 className="text-sm font-black uppercase tracking-[0.28em] text-[#c8a864]">Keywords</h3>
                    <p className="mt-2 text-sm text-[#6d7b93]">Press Enter or comma to add discoverability tags.</p>
                  </div>
                  <div className="w-full max-w-lg">
                    <input
                      value={keywordInput}
                      onChange={(event) => setKeywordInput(event.target.value)}
                      onKeyDown={handleKeywordKeyDown}
                      placeholder="networking, edge AI, healthcare IoT..."
                      className="w-full rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
                    />
                  </div>
                </div>

                <div className="flex flex-wrap gap-3">
                  {formData.keywords.map((keyword) => (
                    <button
                      type="button"
                      key={keyword}
                      onClick={() => removeKeyword(keyword)}
                      className="rounded-full bg-[#edf4ff] px-4 py-2 text-xs font-semibold text-[#30517d]"
                    >
                      {keyword} x
                    </button>
                  ))}
                  {formData.keywords.length === 0 && (
                    <p className="text-sm text-[#7b879d]">No keywords added yet.</p>
                  )}
                </div>
              </section>

              <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
                <SelectionPanel
                  title="Associated Members"
                  searchValue={memberSearch}
                  onSearchChange={setMemberSearch}
                  items={sortedMembers}
                  selectedIds={formData.memberIds}
                  onToggle={toggleMember}
                  getItemLabel={formatMemberName}
                  subtitleKey="grade"
                  emptyMessage="No members match the current search."
                />

                <SelectionPanel
                  title="Associated PhD Students"
                  searchValue={phdSearch}
                  onSearchChange={setPhdSearch}
                  items={sortedPhdStudents}
                  selectedIds={formData.phdStudentIds}
                  onToggle={togglePhdStudent}
                  getItemLabel={formatPhdName}
                  subtitleKey="thesisTopic"
                  emptyMessage="No PhD students match the current search."
                />
              </div>

              <div className="flex flex-col-reverse gap-3 border-t border-[#ece2d3] pt-6 sm:flex-row sm:justify-end">
                <button
                  type="button"
                  onClick={closeFormModal}
                  className="rounded-2xl border border-[#d5dded] px-6 py-3 text-sm font-bold uppercase tracking-[0.2em] text-[#6c7d96]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="rounded-2xl bg-[#c8a864] px-8 py-3 text-sm font-bold uppercase tracking-[0.2em] text-white shadow-lg transition hover:brightness-95 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {saving ? 'Saving...' : editingId ? 'Update Project' : 'Create Project'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {feedbackTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#122243]/60 p-4 backdrop-blur-sm">
          <div className="w-full max-w-2xl rounded-[2rem] bg-white p-8 shadow-2xl">
            <p className="text-xs font-black uppercase tracking-[0.2em] sm:tracking-[0.35em] text-rose-500">Lab Head Feedback</p>
            <h2 className="mt-3 text-2xl font-serif text-[#1b2d52]">{feedbackTarget.title}</h2>
            <div className="mt-4 flex flex-wrap gap-2">
              {renderStatusBadge(feedbackTarget)}
              <span className="rounded-full bg-[#f2f5fa] px-3 py-1 text-[11px] font-black uppercase tracking-[0.2em] text-[#6b7b95]">
                {formatValidationDate(feedbackTarget.latestValidatedAt)}
              </span>
            </div>
            <div className="mt-6 rounded-[1.7rem] border border-rose-100 bg-rose-50/70 px-5 py-5">
              <p className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-rose-500">Scientific Feedback</p>
              <p className="mt-3 text-sm leading-7 text-[#5f6c83]">
                {feedbackTarget.latestValidationComment || 'No feedback comment was recorded.'}
              </p>
            </div>
            <p className="mt-4 text-sm text-[#667791]">
              Reviewer: {feedbackTarget.latestValidatorEmail || 'Lab Head'}
            </p>
            <div className="mt-8 flex justify-end">
              <button
                type="button"
                onClick={() => setFeedbackTarget(null)}
                className="rounded-2xl border border-[#d5dded] px-5 py-3 text-sm font-bold uppercase tracking-[0.2em] text-[#6c7d96]"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {deleteTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#122243]/60 p-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-[2rem] bg-white p-8 shadow-2xl">
            <p className="text-xs font-black uppercase tracking-[0.2em] sm:tracking-[0.35em] text-rose-500">Delete Project</p>
            <h2 className="mt-3 text-2xl font-serif text-[#1b2d52]">{deleteTarget.title}</h2>
            <p className="mt-4 text-sm leading-6 text-[#667791]">
              This action removes the project record and its links to members and PhD students. It cannot be undone.
            </p>
            <div className="mt-8 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
              <button
                type="button"
                onClick={() => setDeleteTarget(null)}
                className="rounded-2xl border border-[#d5dded] px-5 py-3 text-sm font-bold uppercase tracking-[0.2em] text-[#6c7d96]"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleDelete}
                className="rounded-2xl bg-rose-600 px-5 py-3 text-sm font-bold uppercase tracking-[0.2em] text-white hover:bg-rose-700"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
