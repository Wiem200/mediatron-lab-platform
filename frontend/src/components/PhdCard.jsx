import { BriefcaseBusiness, CalendarDays, GraduationCap, UserRound } from 'lucide-react';
import { useMemo, useState } from 'react';

function AvatarPlaceholder({ student }) {
  const [broken, setBroken] = useState(false);
  const initials = useMemo(() => {
    const first = student.fullName?.[0] || student.firstName?.[0] || '';
    const last = student.lastName?.[0] || '';
    return `${first}${last}`.trim() || 'P';
  }, [student.firstName, student.fullName, student.lastName]);

  return (
    <div className="relative h-24 w-24 overflow-hidden rounded-full border-4 border-[#f5f2ec] bg-[#eaf0f8] shadow-md">
      {student.photoUrl && !broken ? (
        <img
          src={student.photoUrl}
          alt={student.fullName || 'PhD student'}
          className="h-full w-full object-cover"
          onError={() => setBroken(true)}
        />
      ) : (
        <div className="flex h-full w-full items-center justify-center bg-[#edf3fb] text-[#1b2d52]">
          <UserRound className="h-10 w-10" />
          <span className="sr-only">{initials}</span>
        </div>
      )}
    </div>
  );
}

function InfoRow({ icon: Icon, children }) {
  return (
    <div className="flex items-start gap-3 text-sm text-[#55657f]">
      <span className="mt-0.5 rounded-full bg-[#f3efe5] p-2 text-[#b3872f]">
        <Icon className="h-4 w-4" />
      </span>
      <span className="leading-6">{children}</span>
    </div>
  );
}

export default function PhdCard({ student, copy, onViewPublications }) {
  return (
    <article className="group flex h-full flex-col rounded-2xl bg-white p-6 shadow-[0_12px_30px_rgba(27,45,82,0.08)] transition-all duration-300 hover:-translate-y-1 hover:shadow-xl">
      <div className="flex items-start gap-4">
        <AvatarPlaceholder student={student} />
        <div className="min-w-0 flex-1">
          <h2 className="font-serif text-3xl leading-tight text-[#1b2d52]">{student.fullName || 'Doctoral Student'}</h2>
          <span className="mt-3 inline-flex rounded-full bg-[#eef5ec] px-3 py-1 text-[11px] font-black uppercase tracking-[0.22em] text-[#2f6d49]">
            {copy.activeBadge}
          </span>
        </div>
      </div>

      <div className="mt-6 rounded-[1.5rem] bg-[#f8f6f1] p-5">
        <p className="text-lg font-semibold italic leading-8 text-[#4f5f79]">{student.thesisTopic}</p>
      </div>

      <div className="mt-6 space-y-4">
        <InfoRow icon={GraduationCap}>
          <strong className="mr-1 font-semibold text-[#1b2d52]">{copy.supervisorPrefix}</strong>
          {student.supervisorName || copy.notAvailable}
        </InfoRow>
        <InfoRow icon={CalendarDays}>
          <strong className="mr-1 font-semibold text-[#1b2d52]">{copy.yearPrefix}</strong>
          {student.registrationYear || copy.notAvailable}
        </InfoRow>
        <InfoRow icon={BriefcaseBusiness}>
          <strong className="mr-1 font-semibold text-[#1b2d52]">{copy.teamPrefix}</strong>
          {student.researchTeam || copy.notAvailable}
        </InfoRow>
      </div>

      <div className="mt-auto pt-6">
        <button
          type="button"
          onClick={() => onViewPublications(student)}
          className="inline-flex w-full items-center justify-center rounded-full border border-[#d9c9a5] bg-[#fffaf0] px-5 py-3 text-sm font-bold uppercase tracking-[0.18em] text-[#1b2d52] transition-all duration-300 hover:border-[#c8a864] hover:bg-[#fff4db]"
        >
          {copy.viewPublications}
        </button>
      </div>
    </article>
  );
}
