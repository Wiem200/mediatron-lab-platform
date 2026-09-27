import { useCallback, useEffect, useMemo, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { memberService } from '../api/memberService';
import { phdService } from '../api/phdService';
import Sidebar from '../components/Sidebar';
import ToastNotice from '../components/ToastNotice';
import { getPrimaryRole, hasAdminPrivileges, useStoredUser } from '../utils/authStore';

const MEMBER_INITIAL_FORM = {
  firstName: '',
  lastName: '',
  email: '',
  photoUrl: '',
  phoneNumber: '+216 ',
  grade: '',
  bio: '',
  researchAxes: '',
  orcidId: '',
  linkedinUrl: '',
  googleScholarUrl: '',
  personalSiteUrl: '',
};

const PHD_INITIAL_FORM = {
  firstName: '',
  lastName: '',
  photoUrl: '',
  thesisTopic: '',
  registrationYear: new Date().getFullYear(),
  researchTeam: '',
  supervisorId: '',
  coSupervisor: '',
  publicationIds: [],
};

function formatMemberName(member) {
  const fullName = `${member.firstName || ''} ${member.lastName || ''}`.trim();
  return fullName || member.email || `Member #${member.id}`;
}

function formatSupervisorLabel(member) {
  return [member.grade, formatMemberName(member)].filter(Boolean).join(' ');
}

function formatPhdName(student) {
  return student.fullName || `${student.firstName || ''} ${student.lastName || ''}`.trim() || `PhD Student #${student.id}`;
}

function getInitials(firstName, lastName, fallback = 'MED') {
  const initials = `${firstName?.[0] || ''}${lastName?.[0] || ''}`.trim();
  return initials || fallback;
}

function normalizeMemberForm(item) {
  return {
    ...MEMBER_INITIAL_FORM,
    ...item,
    email: item.email || '',
    phoneNumber: item.phone || '+216 ',
    researchAxes: Array.isArray(item.researchAxes) ? item.researchAxes.join(', ') : item.researchAxes || '',
  };
}

function normalizePhdForm(item) {
  return {
    ...PHD_INITIAL_FORM,
    ...item,
    publicationIds: Array.isArray(item.publicationIds) ? item.publicationIds : [],
    supervisorId: item.supervisorId ? String(item.supervisorId) : '',
    registrationYear: item.registrationYear || new Date().getFullYear(),
  };
}

function notifyPhdDirectoryUpdated() {
  const version = String(Date.now());
  window.localStorage.setItem('mediatron-phd-directory-version', version);
  window.dispatchEvent(new CustomEvent('mediatron-phd-directory-updated', { detail: version }));
}

function AvatarThumb({ item, fallback = 'MED', large = false }) {
  const [broken, setBroken] = useState(false);
  const sizeClass = large ? 'h-20 w-20 text-xl' : 'h-12 w-12 text-sm';
  const initials = getInitials(item.firstName, item.lastName, fallback);

  return (
    <div className={`overflow-hidden rounded-full border border-[#d9e1ee] bg-[#eef3f9] ${sizeClass}`}>
      {item.photoUrl && !broken ? (
        <img
          src={item.photoUrl}
          alt={formatPhdName(item)}
          className="h-full w-full object-cover"
          onError={() => setBroken(true)}
        />
      ) : (
        <div className="flex h-full w-full items-center justify-center font-serif text-[#1b2d52]">{initials}</div>
      )}
    </div>
  );
}

function PublicationSelector({ options, selectedIds, onToggle }) {
  return (
    <div className="max-h-72 space-y-2 overflow-y-auto pr-1">
      {options.length === 0 && (
        <div className="rounded-2xl border border-dashed border-[#d8dfeb] px-4 py-5 text-sm text-[#7b879d]">
          No approved publications are available for association yet.
        </div>
      )}

      {options.map((publication) => {
        const checked = selectedIds.includes(publication.id);
        return (
          <label
            key={publication.id}
            className={`flex cursor-pointer items-start gap-3 rounded-2xl border px-4 py-3 transition ${
              checked ? 'border-[#c8a864] bg-[#fff7e7]' : 'border-[#edf1f7] bg-[#fbfcfe] hover:border-[#d3dcea]'
            }`}
          >
            <input
              type="checkbox"
              checked={checked}
              onChange={() => onToggle(publication.id)}
              className="mt-1 h-4 w-4 accent-[#1b2d52]"
            />
            <div>
              <div className="text-sm font-semibold text-[#1b2d52]">{publication.title}</div>
              <div className="mt-1 text-xs text-[#71809b]">{publication.publicationYear || 'Year not specified'}</div>
            </div>
          </label>
        );
      })}
    </div>
  );
}

export default function Members() {
  const user = useStoredUser();
  const role = getPrimaryRole(user);
  const canManageMembers = hasAdminPrivileges(user);
  const [activeTab, setActiveTab] = useState('members');
  const [members, setMembers] = useState([]);
  const [phdStudents, setPhdStudents] = useState([]);
  const [publicationOptions, setPublicationOptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [saving, setSaving] = useState(false);
  const [promotingUserId, setPromotingUserId] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [toast, setToast] = useState(null);
  const [memberForm, setMemberForm] = useState(MEMBER_INITIAL_FORM);
  const [phdForm, setPhdForm] = useState(PHD_INITIAL_FORM);
  const [phdSearch, setPhdSearch] = useState('');
  const [teamFilter, setTeamFilter] = useState('');
  const [yearFilter, setYearFilter] = useState('');

  const loadData = useCallback(async () => {
    setLoading(true);
    setLoadError('');
    try {
      const [memberResponse, phdResponse, publicationResponse] = await Promise.all([
        memberService.getAll(),
        phdService.getAll(),
        phdService.getPublicationOptions(),
      ]);

      setMembers(memberResponse.data || []);
      setPhdStudents(phdResponse.data || []);
      setPublicationOptions(publicationResponse.data || []);
    } catch (err) {
      const data = err?.response?.data;
      const message =
        (typeof data === 'string' && data) ||
        data?.message ||
        data?.error ||
        err?.message ||
        'Unable to load directory.';
      setLoadError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (user?.token && canManageMembers) {
      loadData().catch(() => {});
    }
  }, [canManageMembers, loadData, user?.token]);

  const sortedMembers = useMemo(
    () => [...members].sort((left, right) => formatMemberName(left).localeCompare(formatMemberName(right))),
    [members]
  );

  const phdTeams = useMemo(() => {
    const values = new Set();
    phdStudents.forEach((student) => {
      if (student.researchTeam) {
        values.add(student.researchTeam);
      }
    });
    return Array.from(values).sort((left, right) => left.localeCompare(right));
  }, [phdStudents]);

  const phdYears = useMemo(() => {
    const values = new Set();
    phdStudents.forEach((student) => {
      if (student.registrationYear) {
        values.add(String(student.registrationYear));
      }
    });
    return Array.from(values).sort((left, right) => Number(right) - Number(left));
  }, [phdStudents]);

  const filteredPhds = useMemo(() => {
    const query = phdSearch.trim().toLowerCase();
    return phdStudents.filter((student) => {
      const matchesSearch =
        !query ||
        [
          student.fullName,
          student.thesisTopic,
          student.researchTeam,
          student.supervisorName,
          student.coSupervisor,
          ...(student.publications || []).map((publication) => publication.title),
        ]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(query));

      const matchesTeam = !teamFilter || student.researchTeam === teamFilter;
      const matchesYear = !yearFilter || String(student.registrationYear || '') === yearFilter;
      return matchesSearch && matchesTeam && matchesYear;
    });
  }, [phdSearch, phdStudents, teamFilter, yearFilter]);

  const activeRows = activeTab === 'members' ? members : filteredPhds;

  if (!user?.token) {
    return <Navigate to="/login" replace />;
  }

  if (!canManageMembers) {
    return <Navigate to="/dashboard" replace />;
  }

  const openCreateModal = () => {
    setEditingId(null);
    if (activeTab === 'members') {
      setMemberForm(MEMBER_INITIAL_FORM);
    } else {
      setPhdForm(PHD_INITIAL_FORM);
    }
    setShowModal(true);
  };

  const openEditModal = (item) => {
    setEditingId(item.id);
    if (activeTab === 'members') {
      setMemberForm(normalizeMemberForm(item));
    } else {
      setPhdForm(normalizePhdForm(item));
    }
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setEditingId(null);
  };

  const togglePublication = (publicationId) => {
    setPhdForm((current) => ({
      ...current,
      publicationIds: current.publicationIds.includes(publicationId)
        ? current.publicationIds.filter((id) => id !== publicationId)
        : [...current.publicationIds, publicationId],
    }));
  };

  const handleMemberChange = (event) => {
    const { name, value } = event.target;
    setMemberForm((current) => ({ ...current, [name]: value }));
  };

  const handlePhdChange = (event) => {
    const { name, value } = event.target;
    setPhdForm((current) => ({ ...current, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSaving(true);
    try {
      if (activeTab === 'members') {
        const normalizedEmail = memberForm.email?.trim() || '';

        const payload = {
          firstName: memberForm.firstName?.trim() || '',
          lastName: memberForm.lastName?.trim() || '',
          grade: memberForm.grade?.trim() || '',
          bio: memberForm.bio?.trim() || '',
          photoUrl: memberForm.photoUrl?.trim() || '',
          phone: memberForm.phoneNumber?.trim() || '',
          phoneNumber: memberForm.phoneNumber?.trim() || '',
          linkedinUrl: memberForm.linkedinUrl?.trim() || '',
          googleScholarUrl: memberForm.googleScholarUrl?.trim() || '',
          personalSiteUrl: memberForm.personalSiteUrl?.trim() || '',
          orcidId: memberForm.orcidId?.trim() || '',

          // Important: backend needs contactEmail directly.
          email: normalizedEmail,
          contactEmail: normalizedEmail,

          researchAxes: (memberForm.researchAxes || '')
            .split(',')
            .map((item) => item.trim())
            .filter(Boolean),

          // Keep this too in case another backend method still reads user.email.
          user: { email: normalizedEmail },
        };

        if (editingId) {
          await memberService.update(editingId, payload);
          setToast({ type: 'success', message: 'Laboratory member updated successfully.' });
        } else {
          await memberService.add(payload);
          setToast({ type: 'success', message: 'Laboratory member registered successfully.' });
        }
      } else {
        const payload = {
          firstName: phdForm.firstName.trim(),
          lastName: phdForm.lastName.trim(),
          photoUrl: phdForm.photoUrl.trim(),
          thesisTopic: phdForm.thesisTopic.trim(),
          registrationYear: Number(phdForm.registrationYear),
          researchTeam: phdForm.researchTeam.trim(),
          supervisorId: Number(phdForm.supervisorId),
          coSupervisor: phdForm.coSupervisor.trim(),
          publicationIds: Array.from(new Set(phdForm.publicationIds)),
        };

        if (editingId) {
          await phdService.update(editingId, payload);
          setToast({ type: 'success', message: 'PhD student updated and the public directory was refreshed.' });
        } else {
          await phdService.add(payload);
          setToast({ type: 'success', message: 'PhD student registered and published in the directory.' });
        }
        notifyPhdDirectoryUpdated();
      }

      closeModal();
      await loadData();
    } catch (err) {
      const data = err?.response?.data;
      const message =
        (typeof data === 'string' && data) ||
        data?.message ||
        data?.error ||
        err?.message ||
        'Unable to save this record.';
      setToast({ type: 'error', message });
    } finally {
      setSaving(false);
    }
  };

  const requestDelete = (item) => {
    setDeleteTarget({
      id: item.id,
      type: activeTab,
      title: activeTab === 'members' ? formatMemberName(item) : formatPhdName(item),
    });
  };

  const confirmDelete = async () => {
    if (!deleteTarget) {
      return;
    }

    try {
      if (deleteTarget.type === 'members') {
        await memberService.delete(deleteTarget.id, true);
        setToast({ type: 'success', message: 'Member deleted successfully.' });
      } else {
        await phdService.delete(deleteTarget.id, true);
        notifyPhdDirectoryUpdated();
        setToast({ type: 'success', message: 'PhD student deleted and removed from the public directory.' });
      }
      setDeleteTarget(null);
      await loadData();
    } catch (err) {
      const data = err?.response?.data;
      const message =
        (typeof data === 'string' && data) ||
        data?.message ||
        data?.error ||
        err?.message ||
        'Delete failed.';
      setToast({ type: 'error', message });
    }
  };

  const handleRoleChange = async (member, nextRole) => {
  if (!member.userId) {
    setToast({ type: 'error', message: 'This member is not linked to a user account.' });
    return;
  }

  setPromotingUserId(member.userId);

  try {
    await memberService.assignRole(member.userId, nextRole);
    await loadData();

    setToast({
      type: 'success',
      message:
        nextRole === 'ROLE_LAB_HEAD'
          ? 'Member promoted to Lab Head successfully.'
          : 'Lab Head demoted to Member successfully.',
    });
  } catch (err) {
    const data = err?.response?.data;
    const message =
      (typeof data === 'string' && data) ||
      data?.message ||
      data?.error ||
      err?.message ||
      'Role update failed.';

    setToast({ type: 'error', message });
  } finally {
    setPromotingUserId(null);
  }
};

  return (
    <div className="flex min-h-screen bg-[#f5f2ec] font-sans text-[#1b2d52]">
      <Sidebar />
      <ToastNotice toast={toast} onClose={() => setToast(null)} />

      <main className="w-full px-4 pb-10 pt-24 sm:px-6 lg:ml-64 lg:p-10">
        <div className="mb-8 flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">
          <div>
            <p className="mb-3 text-xs font-black uppercase tracking-[0.2em] sm:tracking-[0.35em] text-[#c8a864]">
              {activeTab === 'members' ? 'LAB MEMBERS' : 'PHD STUDENTS'}
            </p>
            <h1 className="text-3xl font-serif uppercase tracking-tight sm:text-4xl">
              {activeTab === 'members' ? 'Member Management' : 'Administrative PhD Portal'}
            </h1>
            <p className="mt-3 max-w-3xl text-sm text-[#637390]">
              {activeTab === 'members'
                ? 'Manage laboratory members, update professional records, and assign scientific leadership roles.'
                : 'Register doctoral researchers, connect them to supervisors and publications, and keep the public card directory current.'}
            </p>
            <div className="mt-6 flex gap-6 border-b border-[#ddd4c2]">
              <button
                type="button"
                onClick={() => setActiveTab('members')}
                className={`pb-3 text-sm font-black uppercase tracking-[0.24em] transition ${
                  activeTab === 'members' ? 'border-b-2 border-[#c8a864] text-[#1b2d52]' : 'text-[#8694aa]'
                }`}
              >
                Lab Members
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('phds')}
                className={`pb-3 text-sm font-black uppercase tracking-[0.24em] transition ${
                  activeTab === 'phds' ? 'border-b-2 border-[#c8a864] text-[#1b2d52]' : 'text-[#8694aa]'
                }`}
              >
                PhD Students
              </button>
            </div>
          </div>

          <button
            type="button"
            onClick={openCreateModal}
            className="rounded-2xl bg-[#1b2d52] px-8 py-3 text-sm font-bold text-white shadow-lg transition hover:bg-[#2c4375]"
          >
            + ADD {activeTab === 'members' ? 'MEMBER' : 'PHD STUDENT'}
          </button>
        </div>

        {activeTab === 'phds' && (
          <>
            <div className="mb-8 grid grid-cols-1 gap-4 md:grid-cols-3">
              <div className="rounded-3xl border border-[#eadfc9] bg-white p-6 shadow-sm">
                <p className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#97a4bc]">PhD Students</p>
                <p className="mt-3 text-4xl font-serif">{phdStudents.length}</p>
                <p className="mt-2 text-sm text-[#6b7b95]">Doctoral profiles currently visible in the MEDIATRON directory.</p>
              </div>
              <div className="rounded-3xl border border-[#eadfc9] bg-white p-6 shadow-sm">
                <p className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#97a4bc]">Research Teams</p>
                <p className="mt-3 text-4xl font-serif">{phdTeams.length}</p>
                <p className="mt-2 text-sm text-[#6b7b95]">Distinct doctoral teams available for client-side filtering.</p>
              </div>
              <div className="rounded-3xl border border-[#eadfc9] bg-white p-6 shadow-sm">
                <p className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#97a4bc]">Linked Publications</p>
                <p className="mt-3 text-4xl font-serif">
                  {phdStudents.reduce((count, student) => count + (student.publications?.length || 0), 0)}
                </p>
                <p className="mt-2 text-sm text-[#6b7b95]">Scientific papers already attached to doctoral records.</p>
              </div>
            </div>

            <section className="mb-8 rounded-[2rem] border border-[#e3dac9] bg-white p-6 shadow-lg">
              <div className="mb-6">
                <h2 className="text-3xl font-serif">Filter Doctoral Records</h2>
                <p className="mt-2 text-sm text-[#677791]">
                  Filter by research team, registration year, supervisor name, thesis keywords, or linked publications.
                </p>
              </div>

              <div className="grid grid-cols-1 gap-4 lg:grid-cols-[2fr_1fr_1fr]">
                <label className="block">
                  <span className="text-[11px] font-black uppercase tracking-[0.2em] sm:tracking-[0.35em] text-[#9aa6bf]">Search</span>
                  <input
                    value={phdSearch}
                    onChange={(event) => setPhdSearch(event.target.value)}
                    placeholder="Doctoral name, thesis topic, supervisor, publication..."
                    className="mt-2 w-full rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
                  />
                </label>

                <label className="block">
                  <span className="text-[11px] font-black uppercase tracking-[0.2em] sm:tracking-[0.35em] text-[#9aa6bf]">Research Team</span>
                  <select
                    value={teamFilter}
                    onChange={(event) => setTeamFilter(event.target.value)}
                    className="mt-2 w-full rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
                  >
                    <option value="">All teams</option>
                    {phdTeams.map((team) => (
                      <option key={team} value={team}>
                        {team}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="block">
                  <span className="text-[11px] font-black uppercase tracking-[0.2em] sm:tracking-[0.35em] text-[#9aa6bf]">Registration Year</span>
                  <select
                    value={yearFilter}
                    onChange={(event) => setYearFilter(event.target.value)}
                    className="mt-2 w-full rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
                  >
                    <option value="">All years</option>
                    {phdYears.map((year) => (
                      <option key={year} value={year}>
                        {year}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
            </section>
          </>
        )}

        <section className="overflow-x-auto rounded-[2rem] border border-[#e3dac9] bg-white shadow-lg">
          <table className="min-w-[760px] w-full text-left">
            <thead>
              <tr className="bg-gray-50/60 text-xs font-black uppercase text-gray-400">
                <th className="p-5 text-[#1b2d52]">Profile</th>
                <th className="p-5">{activeTab === 'members' ? 'Grade' : 'Thesis Topic'}</th>
                <th className="p-5">{activeTab === 'members' ? 'Contact Info' : 'Team And Supervision'}</th>
                <th className="p-5">{activeTab === 'members' ? 'Role' : 'Publications'}</th>
                <th className="p-5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {loadError && (
                <tr>
                  <td colSpan="5" className="p-5 text-sm text-rose-600">
                    {loadError}
                  </td>
                </tr>
              )}

              {loading && (
                <tr>
                  <td colSpan="5" className="p-5 text-sm text-gray-400">
                    Loading directory...
                  </td>
                </tr>
              )}

              {!loading && !loadError && activeRows.length === 0 && (
                <tr>
                  <td colSpan="5" className="p-5 text-sm text-gray-400">
                    {activeTab === 'members' ? 'No members are available.' : 'No doctoral students match the current filters.'}
                  </td>
                </tr>
              )}

              {!loading &&
                !loadError &&
                activeRows.map((item) => (
                  <tr key={item.id} className="align-top transition-colors hover:bg-gray-50/80">
                    <td className="p-5">
                      <div className="flex items-center gap-3">
                        <AvatarThumb item={item} />
                        <div>
                          <div className="font-semibold">
                            {activeTab === 'members' ? formatMemberName(item) : formatPhdName(item)}
                          </div>
                          {activeTab === 'members' ? (
                            <div className="mt-1 text-[11px] font-black uppercase tracking-[0.18em] text-[#97a4bc]">
                              {item.role ? item.role.replace('ROLE_', '').replaceAll('_', ' ') : 'Member'}
                            </div>
                          ) : (
                            <div className="mt-1 inline-flex rounded-full bg-[#eef2f8] px-3 py-1 text-[11px] font-black uppercase tracking-[0.18em] text-[#5b6c87]">
                              {item.registrationYear || 'Year n/a'}
                            </div>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className="p-5 text-sm leading-6 text-[#5e6c84]">
                      {activeTab === 'members' ? item.grade || '-' : item.thesisTopic}
                    </td>
                    <td className="p-5 text-sm text-[#5e6c84]">
                      {activeTab === 'members' ? (
                        item.email || item.phone || '-'
                      ) : (
                        <div className="space-y-2">
                          <div>
                            <span className="font-semibold text-[#1b2d52]">Team:</span> {item.researchTeam || '-'}
                          </div>
                          <div>
                            <span className="font-semibold text-[#1b2d52]">Supervisor:</span> {item.supervisorName || '-'}
                          </div>
                          {item.coSupervisor && (
                            <div>
                              <span className="font-semibold text-[#1b2d52]">Co-supervisor:</span> {item.coSupervisor}
                            </div>
                          )}
                        </div>
                      )}
                    </td>
                    <td className="p-5 text-sm text-[#5e6c84]">
                      {activeTab === 'members' ? (
                        item.role || '-'
                      ) : item.publications?.length > 0 ? (
                        <div className="space-y-2">
                          {item.publications.slice(0, 3).map((publication) => (
                            <div key={publication.id} className="rounded-2xl bg-[#f8f9fc] px-3 py-2">
                              <div className="font-semibold text-[#1b2d52]">{publication.title}</div>
                              <div className="text-xs text-[#7a879d]">{publication.publicationYear || 'Year n/a'}</div>
                            </div>
                          ))}
                          {item.publications.length > 3 && (
                            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-[#8b97ab]">
                              +{item.publications.length - 3} more publications
                            </div>
                          )}
                        </div>
                      ) : (
                        'No publications linked.'
                      )}
                    </td>
                    <td className="p-5">
                      <div className="flex justify-end gap-3">
                        <button
                          type="button"
                          onClick={() => openEditModal(item)}
                          className="text-xs font-bold uppercase tracking-[0.18em] text-indigo-600 hover:underline"
                        >
                          Edit
                        </button>
                      {activeTab === 'members' && item.role === 'ROLE_MEMBER' && (
                        <button
                          type="button"
                          onClick={() => handleRoleChange(item, 'ROLE_LAB_HEAD')}
                          disabled={promotingUserId === item.userId}
                          className="text-xs font-bold uppercase tracking-[0.18em] text-amber-600 hover:underline disabled:opacity-50 disabled:no-underline"
                        >
                          {promotingUserId === item.userId ? 'Updating...' : 'Make Lab Head'}
                        </button>
                      )}

                      {activeTab === 'members' && item.role === 'ROLE_LAB_HEAD' && (
                        <button
                          type="button"
                          onClick={() => handleRoleChange(item, 'ROLE_MEMBER')}
                          disabled={promotingUserId === item.userId}
                          className="text-xs font-bold uppercase tracking-[0.18em] text-orange-700 hover:underline disabled:opacity-50 disabled:no-underline"
                        >
                          {promotingUserId === item.userId ? 'Updating...' : 'Demote to Member'}
                        </button>
                      )}
                        <button
                          type="button"
                          onClick={() => requestDelete(item)}
                          className="text-xs font-bold uppercase tracking-[0.18em] text-rose-600 hover:underline"
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        </section>
      </main>

      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#122243]/70 p-4 backdrop-blur-sm">
          <div className="max-h-[92vh] w-full max-w-6xl overflow-y-auto rounded-[1.75rem] sm:rounded-[2.5rem] bg-[#fcfbf7] p-6 shadow-2xl sm:p-8 lg:p-10">
            <div className="mb-8 flex flex-col gap-3 border-b border-[#ece2d3] pb-5 lg:flex-row lg:items-end lg:justify-between">
              <div>
                <p className="text-xs font-black uppercase tracking-[0.2em] sm:tracking-[0.35em] text-[#c8a864]">
                  {editingId ? 'Update Record' : 'Register Record'}
                </p>
                <h2 className="mt-2 text-3xl font-serif text-[#1b2d52]">
                  {activeTab === 'members'
                    ? `${editingId ? 'Edit' : 'Register'} Lab Member`
                    : `${editingId ? 'Edit' : 'Register'} PhD Student`}
                </h2>
              </div>
              <p className="max-w-xl text-sm text-[#6d7b93]">
                {activeTab === 'members'
                  ? 'Maintain member identity, professional links, and research visibility.'
                  : 'Capture doctoral profile metadata, supervision, and publication links for the public card directory.'}
              </p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-8">
              {activeTab === 'members' ? (
                <>
                  <section className="rounded-[2rem] border border-[#ece2d3] bg-white p-6">
                    <h3 className="mb-5 text-sm font-black uppercase tracking-[0.28em] text-[#c8a864]">Personal Identity</h3>
                    <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
                      <label className="block">
                        <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">First Name</span>
                        <input
                          name="firstName"
                          value={memberForm.firstName}
                          onChange={handleMemberChange}
                          className="mt-2 w-full rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
                          required
                        />
                      </label>
                      <label className="block">
                        <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">Last Name</span>
                        <input
                          name="lastName"
                          value={memberForm.lastName}
                          onChange={handleMemberChange}
                          className="mt-2 w-full rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
                          required
                        />
                      </label>
                      <label className="block">
                        <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">Photo URL</span>
                        <input
                          name="photoUrl"
                          value={memberForm.photoUrl}
                          onChange={handleMemberChange}
                          placeholder="https://..."
                          className="mt-2 w-full rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
                        />
                      </label>
                      <label className="block">
                        <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">Email</span>
                        <input
                          name="email"
                          type="email"
                          value={memberForm.email}
                          onChange={handleMemberChange}
                          className="mt-2 w-full rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
                          required
                        />
                      </label>
                      <label className="block">
                        <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">Phone</span>
                        <input
                          name="phoneNumber"
                          value={memberForm.phoneNumber}
                          onChange={handleMemberChange}
                          className="mt-2 w-full rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
                        />
                      </label>
                      <label className="block">
                        <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">Grade</span>
                        <input
                          name="grade"
                          value={memberForm.grade}
                          onChange={handleMemberChange}
                          className="mt-2 w-full rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
                        />
                      </label>
                    </div>
                  </section>

                  <section className="rounded-[2rem] border border-[#ece2d3] bg-white p-6">
                    <h3 className="mb-5 text-sm font-black uppercase tracking-[0.28em] text-[#c8a864]">Professional Profile</h3>
                    <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
                      <input
                        name="orcidId"
                        value={memberForm.orcidId}
                        onChange={handleMemberChange}
                        placeholder="ORCID"
                        className="rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
                      />
                      <input
                        name="linkedinUrl"
                        value={memberForm.linkedinUrl}
                        onChange={handleMemberChange}
                        placeholder="LinkedIn URL"
                        className="rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
                      />
                      <input
                        name="googleScholarUrl"
                        value={memberForm.googleScholarUrl}
                        onChange={handleMemberChange}
                        placeholder="Google Scholar URL"
                        className="rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
                      />
                      <input
                        name="personalSiteUrl"
                        value={memberForm.personalSiteUrl}
                        onChange={handleMemberChange}
                        placeholder="Personal Website"
                        className="rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864] lg:col-span-2"
                      />
                      <input
                        name="researchAxes"
                        value={memberForm.researchAxes}
                        onChange={handleMemberChange}
                        placeholder="Research axes, separated by commas"
                        className="rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
                      />
                    </div>
                    <textarea
                      name="bio"
                      value={memberForm.bio}
                      onChange={handleMemberChange}
                      placeholder="Member biography"
                      className="mt-6 min-h-32 w-full rounded-[1.75rem] border border-[#e5e9f0] px-4 py-4 text-sm outline-none focus:border-[#c8a864]"
                    />
                  </section>
                </>
              ) : (
                <div className="grid grid-cols-1 gap-6 xl:grid-cols-[1.05fr_0.95fr]">
                  <section className="rounded-[2rem] border border-[#ece2d3] bg-white p-6">
                    <h3 className="mb-5 text-sm font-black uppercase tracking-[0.28em] text-[#c8a864]">Doctoral Identity</h3>
                    <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
                      <label className="block">
                        <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">First Name</span>
                        <input
                          name="firstName"
                          value={phdForm.firstName}
                          onChange={handlePhdChange}
                          className="mt-2 w-full rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
                          required
                        />
                      </label>
                      <label className="block">
                        <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">Last Name</span>
                        <input
                          name="lastName"
                          value={phdForm.lastName}
                          onChange={handlePhdChange}
                          className="mt-2 w-full rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
                          required
                        />
                      </label>
                      <label className="block lg:col-span-2">
                        <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">Photo URL</span>
                        <input
                          name="photoUrl"
                          value={phdForm.photoUrl}
                          onChange={handlePhdChange}
                          placeholder="https://..."
                          className="mt-2 w-full rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
                        />
                      </label>
                      <label className="block lg:col-span-2">
                        <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">Thesis Topic</span>
                        <textarea
                          name="thesisTopic"
                          value={phdForm.thesisTopic}
                          onChange={handlePhdChange}
                          className="mt-2 min-h-32 w-full rounded-[1.75rem] border border-[#e5e9f0] px-4 py-4 text-sm outline-none focus:border-[#c8a864]"
                          required
                        />
                      </label>
                      <label className="block">
                        <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">Registration Year</span>
                        <input
                          name="registrationYear"
                          type="number"
                          min="2000"
                          value={phdForm.registrationYear}
                          onChange={handlePhdChange}
                          className="mt-2 w-full rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
                          required
                        />
                      </label>
                      <label className="block">
                        <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">Research Team</span>
                        <input
                          name="researchTeam"
                          value={phdForm.researchTeam}
                          onChange={handlePhdChange}
                          className="mt-2 w-full rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
                          required
                        />
                      </label>
                      <label className="block">
                        <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">Primary Supervisor</span>
                        <select
                          name="supervisorId"
                          value={phdForm.supervisorId}
                          onChange={handlePhdChange}
                          className="mt-2 w-full rounded-2xl border border-[#e5e9f0] bg-white px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
                          required
                        >
                          <option value="">Select a laboratory supervisor</option>
                          {sortedMembers.map((member) => (
                            <option key={member.id} value={member.id}>
                              {formatSupervisorLabel(member)}
                            </option>
                          ))}
                        </select>
                      </label>
                      <label className="block">
                        <span className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#9aa6bf]">Co-supervisor</span>
                        <input
                          name="coSupervisor"
                          value={phdForm.coSupervisor}
                          onChange={handlePhdChange}
                          placeholder="Optional external or internal collaborator"
                          className="mt-2 w-full rounded-2xl border border-[#e5e9f0] px-4 py-3 text-sm outline-none focus:border-[#c8a864]"
                        />
                      </label>
                    </div>
                  </section>

                  <section className="rounded-[2rem] border border-[#ece2d3] bg-white p-6">
                    <div className="mb-5">
                      <h3 className="text-sm font-black uppercase tracking-[0.28em] text-[#c8a864]">Linked Publications</h3>
                      <p className="mt-2 text-sm text-[#6d7b93]">
                        Associate approved papers that should appear on the public doctoral card.
                      </p>
                    </div>

                    <PublicationSelector
                      options={publicationOptions}
                      selectedIds={phdForm.publicationIds}
                      onToggle={togglePublication}
                    />

                    <div className="mt-6 rounded-[1.75rem] bg-[#f8f9fc] p-5">
                      <p className="text-[11px] font-black uppercase tracking-[0.28em] text-[#97a4bc]">Public Card Preview</p>
                      <div className="mt-4 flex items-center gap-4">
                        <AvatarThumb item={phdForm} fallback="PHD" large />
                        <div>
                          <p className="text-xl font-serif text-[#1b2d52]">
                            {`${phdForm.firstName} ${phdForm.lastName}`.trim() || 'Doctoral Student'}
                          </p>
                          <p className="mt-1 text-sm italic text-[#6d7b93]">
                            {phdForm.thesisTopic || 'Thesis topic preview will appear here.'}
                          </p>
                        </div>
                      </div>
                      <div className="mt-4 grid grid-cols-1 gap-3 text-sm text-[#5c6c84]">
                        <div>
                          <span className="font-semibold text-[#1b2d52]">Year:</span> {phdForm.registrationYear || 'n/a'}
                        </div>
                        <div>
                          <span className="font-semibold text-[#1b2d52]">Team:</span> {phdForm.researchTeam || 'n/a'}
                        </div>
                        <div>
                          <span className="font-semibold text-[#1b2d52]">Publications:</span> {phdForm.publicationIds.length}
                        </div>
                      </div>
                    </div>
                  </section>
                </div>
              )}

              <div className="flex flex-col-reverse gap-3 border-t border-[#ece2d3] pt-6 sm:flex-row sm:justify-end">
                <button
                  type="button"
                  onClick={closeModal}
                  className="rounded-2xl border border-[#d5dded] px-6 py-3 text-sm font-bold uppercase tracking-[0.2em] text-[#6c7d96]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="rounded-2xl bg-[#c8a864] px-8 py-3 text-sm font-bold uppercase tracking-[0.2em] text-white shadow-lg transition hover:brightness-95 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {saving ? 'Saving...' : activeTab === 'members' ? 'Save Member' : 'Save PhD Profile'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {deleteTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#122243]/60 p-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-[2rem] bg-white p-8 shadow-2xl">
            <p className="text-xs font-black uppercase tracking-[0.2em] sm:tracking-[0.35em] text-rose-500">Confirm Deletion</p>
            <h2 className="mt-3 text-2xl font-serif text-[#1b2d52]">{deleteTarget.title}</h2>
            <p className="mt-4 text-sm leading-6 text-[#667791]">
              {deleteTarget.type === 'members'
                ? 'This action removes the member record from the management portal.'
                : 'This action removes the doctoral profile and its public card from the MEDIATRON directory.'}
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
                onClick={confirmDelete}
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
