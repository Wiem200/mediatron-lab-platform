import { Link } from 'react-router-dom';

function buildPreview(text, fallback) {
  if (!text) {
    return fallback;
  }

  if (text.length <= 180) {
    return text;
  }

  return `${text.slice(0, 177).trim()}...`;
}

export default function ProjectsGrid({ projects, copy }) {
  return (
    <div className="grid grid-cols-1 items-stretch gap-6 lg:grid-cols-2 xl:grid-cols-3">
      {projects.map((project) => {
        const teamCount = (project.memberNames || []).length + (project.phdStudentNames || []).length;
        const leadName =
          project.responsibleName ||
          [project.responsibleMemberGrade, project.responsibleMemberName].filter(Boolean).join(' ');
        const fundingLabel = project.funding || copy.internal;

        return (
          <article
            key={project.id}
            className="flex h-full flex-col rounded-[2rem] border border-[#e3dac9] bg-white p-6 shadow-lg transition hover:-translate-y-1 hover:shadow-xl"
          >
            <div className="mb-4 flex min-w-0 flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
              <h3 className="min-w-0 flex-1 font-serif text-2xl leading-tight text-[#1b2d52]">
                {project.title}
              </h3>

              <span className="inline-flex max-w-full shrink-0 items-center justify-center rounded-full bg-[#fff4db] px-3 py-1 text-center text-[10px] font-black uppercase leading-snug tracking-[0.12em] text-[#9c7620] sm:max-w-[48%] sm:text-[11px]">
                {fundingLabel}
              </span>
            </div>

            <p className="text-sm leading-7 text-[#61718c]">
              {buildPreview(project.description, copy.emptyDescription)}
            </p>

            {(project.keywords || []).length > 0 ? (
              <div className="mt-5 flex flex-wrap gap-2">
                {(project.keywords || []).slice(0, 5).map((keyword) => (
                  <span
                    key={keyword}
                    className="inline-flex max-w-full rounded-full bg-[#eef3fb] px-3 py-1 text-xs font-semibold leading-snug text-[#34547f]"
                  >
                    {keyword}
                  </span>
                ))}
              </div>
            ) : null}

            <div className="mt-6 grid grid-cols-1 gap-3 rounded-3xl bg-[#f8f9fc] p-4 text-sm text-[#51617d] sm:grid-cols-2">
              <div className="min-w-0">
                <p className="text-[10px] font-black uppercase tracking-[0.16em] text-[#98a4bb] sm:text-[11px] sm:tracking-[0.22em]">
                  {copy.duration}
                </p>
                <p className="mt-2 font-semibold text-[#1b2d52]">
                  {project.duration || '-'}
                </p>
              </div>

              <div className="min-w-0">
                <p className="text-[10px] font-black uppercase tracking-[0.16em] text-[#98a4bb] sm:text-[11px] sm:tracking-[0.22em]">
                  {copy.team}
                </p>
                <p className="mt-2 font-semibold text-[#1b2d52]">
                  {teamCount} {copy.contributors}
                </p>
              </div>
            </div>

            <div className="mt-auto pt-6">
              <p className="text-[10px] font-black uppercase tracking-[0.16em] text-[#98a4bb] sm:text-[11px] sm:tracking-[0.22em]">
                {copy.lead}
              </p>
              <p className="mt-2 text-sm font-semibold text-[#1b2d52]">
                {leadName || copy.internal}
              </p>

              <Link
                to={`/research-projects/${project.id}`}
                className="mt-5 inline-flex min-h-[44px] items-center rounded-2xl bg-[#1b2d52] px-5 py-3 text-sm font-bold uppercase tracking-[0.16em] text-white transition hover:bg-[#274071]"
              >
                {copy.details}
              </Link>
            </div>
          </article>
        );
      })}
    </div>
  );
}
