import { useCallback, useEffect, useMemo, useState } from 'react';
import { Navigate } from 'react-router-dom';
import Sidebar from '../components/Sidebar';
import ToastNotice from '../components/ToastNotice';
import { projectService } from '../api/projectService';

const decisionStyles = {
  APPROVED: 'border-emerald-300 bg-emerald-50 text-emerald-700',
  REJECTED: 'border-rose-300 bg-rose-50 text-rose-700',
};

function formatStatusLabel(status) {
  return status ? status.replaceAll('_', ' ') : 'Unknown';
}

function formatDate(value) {
  if (!value) {
    return 'No validation recorded yet';
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleString();
}

function buildProjectLinks(project) {
  return [
    { label: 'GitHub Repository', url: project.githubLink },
    { label: 'Video Presentation', url: project.videoLink },
    { label: 'PDF Report', url: project.pdfLink },
    { label: 'Poster', url: project.posterLink },
  ].filter((item) => item.url);
}

export default function ProjectValidation() {
  const user = JSON.parse(localStorage.getItem('user') || 'null');
  const role = user?.roles?.[0];

  const [projects, setProjects] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState(null);
  const [decision, setDecision] = useState('APPROVED');
  const [comment, setComment] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [toast, setToast] = useState(null);

  const loadProjects = useCallback(async () => {
    setLoading(true);
    setError('');

    try {
      const response = await projectService.getPending();
      setProjects(response.data || []);
    } catch (err) {
      const data = err?.response?.data;
      const message =
        data?.message ||
        data?.error ||
        (typeof data === 'string' && data) ||
        err?.message ||
        'Unable to load the project validation queue.';

      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (user?.token && role === 'ROLE_LAB_HEAD') {
      loadProjects().catch(() => {});
    }
  }, [loadProjects, role, user?.token]);

  useEffect(() => {
    if (projects.length === 0) {
      setSelectedProjectId(null);
      return;
    }

    const selectedStillExists = projects.some((project) => project.id === selectedProjectId);

    if (!selectedStillExists) {
      setSelectedProjectId(projects[0].id);
    }
  }, [projects, selectedProjectId]);

  const selectedProject = useMemo(
    () => projects.find((project) => project.id === selectedProjectId) || null,
    [projects, selectedProjectId]
  );

  const selectedProjectLinks = useMemo(
    () => (selectedProject ? buildProjectLinks(selectedProject) : []),
    [selectedProject]
  );

  if (!user?.token) {
    return <Navigate to="/login" replace />;
  }

  if (role !== 'ROLE_LAB_HEAD') {
    return <Navigate to="/dashboard" replace />;
  }

  const isRejectingWithoutComment = decision === 'REJECTED' && !comment.trim();

  const handleSelectProject = (projectId) => {
    setSelectedProjectId(projectId);
    setDecision('APPROVED');
    setComment('');
    setError('');
    setToast(null);
  };

  const getErrorMessage = (err, fallback) => {
    const data = err?.response?.data;

    return (
      data?.message ||
      data?.error ||
      (typeof data === 'string' && data) ||
      err?.message ||
      fallback
    );
  };

  const handleValidate = async () => {
    if (submitting) {
      return;
    }

    if (!selectedProject) {
      setToast({ type: 'error', message: 'Please select a project first.' });
      return;
    }

    if (decision === 'REJECTED' && !comment.trim()) {
      setToast({
        type: 'error',
        message: 'Scientific feedback is required before rejecting a project.',
      });
      return;
    }

    setSubmitting(true);
    setError('');
    setToast(null);

    try {
      const payload = {
        decision: decision === 'APPROVED' ? 'APPROVE' : 'REJECT',
        comment: comment.trim(),
      };

      await projectService.validate(selectedProject.id, payload);

      setProjects((current) => current.filter((project) => project.id !== selectedProject.id));
      setDecision('APPROVED');
      setComment('');

      setToast({
        type: 'success',
        message:
          decision === 'APPROVED'
            ? 'Project successfully approved and published.'
            : 'Project rejected and feedback was returned to the administrator.',
      });

      await loadProjects();
    } catch (err) {
      setToast({
        type: 'error',
        message: getErrorMessage(err, 'Project validation failed.'),
      });
    } finally {
      setSubmitting(false);
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    await handleValidate();
  };

  return (
    <div className="flex min-h-screen bg-[#f5f2ec] font-sans text-[#1b2d52]">
      <Sidebar />
      <ToastNotice toast={toast} onClose={() => setToast(null)} />

      <main className="w-full px-4 pb-10 pt-24 sm:px-6 lg:ml-64 lg:p-10">
        <div className="mb-8 flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
          <div>
                        <h1 className="text-3xl font-serif uppercase tracking-tight sm:text-4xl">Project Validation Workspace</h1>
            <p className="mt-3 max-w-3xl text-sm text-[#637390]">
              Review administrator-submitted research projects before they become visible in the public directory.
            </p>
          </div>

          <div className="rounded-3xl border border-[#eadfc9] bg-white px-6 py-5 shadow-sm">
            <p className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#97a4bc]">Pending Queue</p>
            <p className="mt-2 text-4xl font-serif">{projects.length}</p>
          </div>
        </div>

        {error && (
          <div className="mb-6 rounded-2xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">
            {error}
          </div>
        )}

        <section className="mb-8 overflow-hidden rounded-[2rem] border border-[#e3dac9] bg-white shadow-lg">
          <table className="min-w-[760px] w-full text-left">
            <thead>
              <tr className="bg-gray-50/60 text-xs font-black uppercase text-gray-400">
                <th className="p-5 text-[#1b2d52]">Project</th>
                <th className="p-5">Lead</th>
                <th className="p-5">Team</th>
                <th className="p-5">Keywords</th>
                <th className="p-5">Status</th>
              </tr>
            </thead>

            <tbody className="divide-y divide-gray-50">
              {loading && (
                <tr>
                  <td colSpan="5" className="p-5 text-sm text-gray-400">
                    Loading validation queue...
                  </td>
                </tr>
              )}

              {!loading && projects.length === 0 && (
                <tr>
                  <td colSpan="5" className="p-5 text-sm text-gray-400">
                    No projects are waiting for Lab Head validation.
                  </td>
                </tr>
              )}

              {projects.map((project) => {
                const isSelected = project.id === selectedProjectId;
                const teamSize = (project.memberNames?.length || 0) + (project.phdStudentNames?.length || 0);

                return (
                  <tr
                    key={project.id}
                    onClick={() => handleSelectProject(project.id)}
                    className={`cursor-pointer align-top transition ${
                      isSelected ? 'bg-[#fff8e9]' : 'hover:bg-gray-50/80'
                    }`}
                  >
                    <td className="p-5">
                      <div className="font-semibold">{project.title}</div>
                      <div className="mt-1 max-w-md text-sm text-gray-500">{project.description}</div>
                    </td>
                    <td className="p-5 text-sm text-gray-600">{project.responsibleMemberName || 'Not assigned'}</td>
                    <td className="p-5 text-sm text-gray-600">{teamSize} linked researchers</td>
                    <td className="p-5 text-sm text-gray-600">
                      {(project.keywords || []).slice(0, 3).join(', ') || '-'}
                    </td>
                    <td className="p-5">
                      <span className="inline-flex rounded-full border border-amber-200 bg-amber-100 px-3 py-1 text-xs font-bold text-amber-800">
                        {formatStatusLabel(project.status)}
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </section>

        <section className="grid grid-cols-1 gap-6 xl:grid-cols-[1.3fr_0.9fr]">
          <article className="rounded-[2rem] border border-[#e3dac9] bg-white p-6 shadow-lg">
            <div className="flex flex-col gap-4 border-b border-[#ece2d3] pb-5 md:flex-row md:items-start md:justify-between">
              <div>
                <p className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#97a4bc]">Review Workspace</p>
                <h2 className="mt-2 text-3xl font-serif text-[#1b2d52]">
                  {selectedProject ? selectedProject.title : 'Select a pending project'}
                </h2>
                <p className="mt-2 text-sm text-[#65748f]">
                  {selectedProject
                    ? 'Inspect the scientific record, team composition, and linked resources before taking a decision.'
                    : 'Choose a project from the queue to inspect its scientific details.'}
                </p>
              </div>

              {selectedProject && (
                <span className="inline-flex rounded-full border border-amber-200 bg-amber-100 px-3 py-1 text-xs font-bold uppercase tracking-[0.18em] text-amber-800">
                  {formatStatusLabel(selectedProject.status)}
                </span>
              )}
            </div>

            {!selectedProject ? (
              <div className="rounded-[1.8rem] border border-dashed border-[#d8dfeb] bg-[#fbfcfe] px-6 py-12 text-center text-sm text-[#71809b]">
                No pending project is selected.
              </div>
            ) : (
              <div className="mt-6 space-y-6">
                <section className="rounded-[1.8rem] bg-[#f8f9fc] p-5">
                  <p className="text-[11px] font-black uppercase tracking-[0.28em] text-[#97a4bc]">Project Summary</p>
                  <p className="mt-3 text-sm leading-7 text-[#53637d]">{selectedProject.description}</p>
                </section>

                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                  <section className="rounded-[1.8rem] bg-[#f8f9fc] p-5">
                    <p className="text-[11px] font-black uppercase tracking-[0.28em] text-[#97a4bc]">Lead And Team</p>
                    <p className="mt-3 text-sm font-semibold text-[#1b2d52]">
                      Lead: {selectedProject.responsibleName || selectedProject.responsibleMemberName || 'Not assigned'}
                    </p>

                    <div className="mt-4">
                      <p className="text-xs font-black uppercase tracking-[0.22em] text-[#9aa6bf]">Members</p>
                      <div className="mt-2 flex flex-wrap gap-2">
                        {(selectedProject.memberNames || []).map((name) => (
                          <span key={name} className="rounded-full bg-[#fff7e8] px-3 py-1 text-xs font-semibold text-[#8f6a12]">
                            {name}
                          </span>
                        ))}

                        {(!selectedProject.memberNames || selectedProject.memberNames.length === 0) && (
                          <span className="text-sm text-[#7b879d]">No members linked.</span>
                        )}
                      </div>
                    </div>

                    <div className="mt-4">
                      <p className="text-xs font-black uppercase tracking-[0.22em] text-[#9aa6bf]">PhD Students</p>
                      <div className="mt-2 flex flex-wrap gap-2">
                        {(selectedProject.phdStudentNames || []).map((name) => (
                          <span key={name} className="rounded-full bg-[#eef8f2] px-3 py-1 text-xs font-semibold text-[#1f7452]">
                            {name}
                          </span>
                        ))}

                        {(!selectedProject.phdStudentNames || selectedProject.phdStudentNames.length === 0) && (
                          <span className="text-sm text-[#7b879d]">No PhD students linked.</span>
                        )}
                      </div>
                    </div>
                  </section>

                  <section className="rounded-[1.8rem] bg-[#f8f9fc] p-5">
                    <p className="text-[11px] font-black uppercase tracking-[0.28em] text-[#97a4bc]">Keywords And Links</p>

                    <div className="mt-3 flex flex-wrap gap-2">
                      {(selectedProject.keywords || []).map((keyword) => (
                        <span key={keyword} className="rounded-full bg-[#edf4ff] px-3 py-1 text-xs font-semibold text-[#30517d]">
                          {keyword}
                        </span>
                      ))}

                      {(!selectedProject.keywords || selectedProject.keywords.length === 0) && (
                        <span className="text-sm text-[#7b879d]">No keywords recorded.</span>
                      )}
                    </div>

                    <div className="mt-5 space-y-2 text-sm">
                      {selectedProjectLinks.map((link) => (
                        <a
                          key={link.label}
                          href={link.url}
                          target="_blank"
                          rel="noreferrer"
                          className="block text-[#1b2d52] underline decoration-[#c8a864]"
                        >
                          {link.label}
                        </a>
                      ))}

                      {selectedProjectLinks.length === 0 && (
                        <p className="text-[#7b879d]">No project resources were provided.</p>
                      )}
                    </div>

                    <div className="mt-5 grid grid-cols-1 gap-3 text-sm text-[#53637d]">
                      <div className="rounded-2xl bg-white px-4 py-3">
                        <span className="font-semibold text-[#1b2d52]">Duration:</span> {selectedProject.duration}
                      </div>
                      <div className="rounded-2xl bg-white px-4 py-3">
                        <span className="font-semibold text-[#1b2d52]">Funding:</span>{' '}
                        {selectedProject.funding || 'Funding details not specified'}
                      </div>
                    </div>
                  </section>
                </div>

                {selectedProject.latestValidationComment && (
                  <section className="rounded-[1.8rem] border border-[#eadfc9] bg-[#fffaf0] p-5">
                    <p className="text-[11px] font-black uppercase tracking-[0.28em] text-[#c8a864]">Latest Review Note</p>
                    <p className="mt-3 text-sm leading-7 text-[#5b6780]">{selectedProject.latestValidationComment}</p>
                    <p className="mt-3 text-xs text-[#8b96aa]">
                      {selectedProject.latestValidatorEmail || 'Lab Head'} • {formatDate(selectedProject.latestValidatedAt)}
                    </p>
                  </section>
                )}
              </div>
            )}
          </article>

          <aside className="rounded-[2rem] border border-[#e3dac9] bg-white p-6 shadow-lg">
            <p className="text-[11px] font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#97a4bc]">Validation Form</p>
            <h2 className="mt-2 text-3xl font-serif text-[#1b2d52]">Scientific Decision</h2>
            <p className="mt-2 text-sm text-[#65748f]">
              Approvals publish the project immediately. Rejections must include actionable scientific feedback.
            </p>

            <form onSubmit={handleSubmit} className="mt-8 space-y-6">
              <div>
                <p className="text-[11px] font-black uppercase tracking-[0.28em] text-[#9aa6bf]">Decision</p>

                <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2">
                  {['APPROVED', 'REJECTED'].map((value) => {
                    const isActive = decision === value;

                    return (
                      <button
                        key={value}
                        type="button"
                        onClick={() => {
                          setDecision(value);
                          setToast(null);
                        }}
                        disabled={submitting}
                        className={`rounded-[1.4rem] border px-4 py-4 text-sm font-bold uppercase tracking-[0.2em] transition disabled:cursor-not-allowed disabled:opacity-60 ${
                          isActive
                            ? decisionStyles[value]
                            : 'border-[#d7dfeb] bg-[#fbfcfe] text-[#6a7891] hover:border-[#c8a864]'
                        }`}
                      >
                        {value === 'APPROVED' ? 'Approve' : 'Reject'}
                      </button>
                    );
                  })}
                </div>
              </div>

              <label className="block">
                <span className="text-[11px] font-black uppercase tracking-[0.28em] text-[#9aa6bf]">Scientific Feedback</span>
                <textarea
                  value={comment}
                  onChange={(event) => setComment(event.target.value)}
                  placeholder="Provide scientific corrections, missing evidence, or publication concerns."
                  disabled={submitting}
                  className={`mt-3 min-h-48 w-full rounded-[1.8rem] border px-4 py-4 text-sm outline-none disabled:cursor-not-allowed disabled:opacity-60 ${
                    isRejectingWithoutComment ? 'border-rose-300 bg-rose-50' : 'border-[#e5e9f0] bg-[#fbfcfe]'
                  }`}
                />
                <p className="mt-2 text-xs text-[#7c889f]">
                  {decision === 'REJECTED'
                    ? 'Feedback is mandatory for rejections.'
                    : 'Optional approval note. Leave empty to publish without comment.'}
                </p>
              </label>

              <button
                type="button"
                onClick={handleValidate}
                disabled={!selectedProject || submitting}
                className="w-full rounded-[1.4rem] bg-[#1b2d52] px-5 py-4 text-sm font-bold uppercase tracking-[0.2em] text-white shadow-lg transition hover:bg-[#2c4375] disabled:cursor-not-allowed disabled:opacity-50"
              >
                {submitting ? 'Submitting...' : decision === 'APPROVED' ? 'Approve And Publish' : 'Reject Project'}
              </button>
            </form>
          </aside>
        </section>
      </main>
    </div>
  );
}