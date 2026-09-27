function ResourceLink({ link, accent, children }) {
  if (!link?.url) {
    return null;
  }

  return (
    <a
      href={link.url}
      target="_blank"
      rel="noreferrer"
      className={`flex items-center gap-3 rounded-3xl border border-[#e4d9c7] bg-white px-5 py-4 text-sm font-semibold text-[#1b2d52] shadow-sm transition hover:-translate-y-0.5 hover:shadow-md ${accent}`}
    >
      <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#f4efe6]">{children}</span>
      <span>{link.label}</span>
    </a>
  );
}

function TeamBlock({ title, subtitle, items, emptyMessage, accentClass }) {
  return (
    <div className="rounded-[2rem] border border-[#e3dac9] bg-white p-8 shadow-lg">
      <h2 className="font-serif text-3xl text-[#1b2d52]">{title}</h2>
      <p className="mt-2 text-sm text-[#677791]">{subtitle}</p>
      <div className="mt-6 flex flex-wrap gap-3">
        {items.length > 0 ? (
          items.map((item) => (
            <span key={item} className={`rounded-full px-4 py-2 text-sm font-semibold ${accentClass}`}>
              {item}
            </span>
          ))
        ) : (
          <p className="text-sm text-[#71809b]">{emptyMessage}</p>
        )}
      </div>
    </div>
  );
}

export default function ProjectDetails({ project, copy }) {
  const resources = project.documentLinks || {};
  const leadName =
    project.responsibleName || [project.responsibleMemberGrade, project.responsibleMemberName].filter(Boolean).join(' ');

  return (
    <>
      <section className="overflow-hidden rounded-[2.5rem] border border-[#e6dac6] bg-[linear-gradient(125deg,_#1b2d52_0%,_#254474_48%,_#fcfaf4_48%,_#fcfaf4_100%)] shadow-xl">
        <div className="grid grid-cols-1 lg:grid-cols-[1.2fr_0.8fr]">
          <div className="p-8 text-white lg:p-12">
            <p className="text-xs font-black uppercase tracking-[0.38em] text-[#f0d898]">{copy.eyebrow}</p>
            <h1 className="mt-4 max-w-3xl font-serif text-4xl leading-tight lg:text-5xl">{project.title}</h1>
            <p className="mt-6 max-w-2xl text-sm leading-7 text-[#dce4f1]">{project.description}</p>

            <div className="mt-8 flex flex-wrap gap-2">
              {(project.keywords || []).map((keyword) => (
                <span
                  key={keyword}
                  className="rounded-full border border-white/12 bg-white/10 px-4 py-2 text-xs font-semibold uppercase tracking-[0.16em] text-[#f7f1e4]"
                >
                  {keyword}
                </span>
              ))}
            </div>
          </div>

          <div className="bg-[#fcfaf4] p-8 lg:p-12">
            <div className="rounded-[2rem] border border-[#e4d9c7] bg-white p-6 shadow-sm">
              <p className="text-[11px] font-black uppercase tracking-[0.28em] text-[#98a4bb]">{copy.lead}</p>
              <div className="mt-4">
                <p className="text-lg font-semibold text-[#1b2d52]">{leadName || copy.memberFallback}</p>
              </div>

              <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div className="rounded-2xl bg-[#f8f9fc] p-4">
                  <p className="text-[11px] font-black uppercase tracking-[0.22em] text-[#98a4bb]">{copy.funding}</p>
                  <p className="mt-2 text-sm font-semibold text-[#1b2d52]">{project.funding || copy.unspecified}</p>
                </div>
                <div className="rounded-2xl bg-[#f8f9fc] p-4">
                  <p className="text-[11px] font-black uppercase tracking-[0.22em] text-[#98a4bb]">{copy.duration}</p>
                  <p className="mt-2 text-sm font-semibold text-[#1b2d52]">{project.duration}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="mt-10 grid grid-cols-1 gap-6 xl:grid-cols-[1fr_0.95fr]">
        <div className="rounded-[2rem] border border-[#e3dac9] bg-white p-8 shadow-lg">
          <h2 className="font-serif text-3xl text-[#1b2d52]">{copy.description}</h2>
          <div className="mt-6 space-y-5 text-base leading-8 text-[#5f6f8a]">
            {project.description.split('\n').filter(Boolean).map((paragraph, index) => (
              <p key={`${project.id}-paragraph-${index}`}>{paragraph}</p>
            ))}
          </div>
        </div>

        <div className="rounded-[2rem] border border-[#e3dac9] bg-white p-8 shadow-lg">
          <h2 className="font-serif text-3xl text-[#1b2d52]">{copy.hub}</h2>
          <p className="mt-2 text-sm text-[#677791]">{copy.hubSubtitle}</p>

          <div className="mt-6 grid grid-cols-1 gap-4">
            <ResourceLink link={resources.pdf} accent="hover:border-[#b98f38]">
              <svg viewBox="0 0 24 24" className="h-5 w-5 fill-none stroke-[#8c6713] stroke-[1.8]">
                <path d="M7 3h7l5 5v13H7z" />
                <path d="M14 3v6h6" />
              </svg>
            </ResourceLink>
            <ResourceLink link={resources.poster} accent="hover:border-[#b98f38]">
              <svg viewBox="0 0 24 24" className="h-5 w-5 fill-none stroke-[#8c6713] stroke-[1.8]">
                <path d="M6 4h12v14H6z" />
                <path d="M9 20h6" />
              </svg>
            </ResourceLink>
            <ResourceLink link={resources.github} accent="hover:border-[#365a8e]">
              <svg viewBox="0 0 24 24" className="h-5 w-5 fill-none stroke-[#274071] stroke-[1.8]">
                <path d="M9 18c-4 1.5-4-2.5-6-3" />
                <path d="M15 21v-4a4 4 0 0 0-1-3c3.5-.4 7-1.7 7-7a5.4 5.4 0 0 0-1.5-3.7A5 5 0 0 0 19.4 0S18 .4 15 2.3a13 13 0 0 0-6 0C6 .4 4.6 0 4.6 0A5 5 0 0 0 4.5 3.3 5.4 5.4 0 0 0 3 7c0 5.3 3.5 6.6 7 7a4 4 0 0 0-1 3v4" />
              </svg>
            </ResourceLink>
            <ResourceLink link={resources.video} accent="hover:border-[#365a8e]">
              <svg viewBox="0 0 24 24" className="h-5 w-5 fill-none stroke-[#274071] stroke-[1.8]">
                <rect x="3" y="5" width="14" height="14" rx="2" />
                <path d="m10 9 5 3-5 3z" />
                <path d="m17 10 4-2v8l-4-2" />
              </svg>
            </ResourceLink>
          </div>
        </div>
      </section>

      <section className="mt-10 rounded-[2rem] border border-[#e3dac9] bg-white p-8 shadow-lg">
        <h2 className="font-serif text-3xl text-[#1b2d52]">{copy.teamTitle}</h2>
        <p className="mt-2 text-sm text-[#677791]">{copy.teamSubtitle}</p>

        <div className="mt-6 rounded-[1.75rem] bg-[#f8f9fc] p-5">
          <p className="text-[11px] font-black uppercase tracking-[0.22em] text-[#98a4bb]">{copy.lead}</p>
          <p className="mt-2 text-base font-semibold text-[#1b2d52]">{leadName || copy.memberFallback}</p>
        </div>
      </section>

      <section className="mt-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
        <TeamBlock
          title={copy.members}
          subtitle={copy.membersSubtitle}
          items={project.memberNames || []}
          emptyMessage={copy.membersEmpty}
          accentClass="bg-[#fff7e8] text-[#8f6a12]"
        />
        <TeamBlock
          title={copy.phd}
          subtitle={copy.phdSubtitle}
          items={project.phdStudentNames || []}
          emptyMessage={copy.phdEmpty}
          accentClass="bg-[#eef8f2] text-[#1f7452]"
        />
      </section>
    </>
  );
}
