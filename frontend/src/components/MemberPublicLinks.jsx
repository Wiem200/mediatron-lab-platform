function ExternalLink({ href, label, accent = false }) {
  if (!href) {
    return null;
  }

  return (
    <a
      href={href}
      target="_blank"
      rel="noreferrer"
      className={`inline-flex items-center rounded-full border px-3 py-1.5 text-xs font-semibold transition ${
        accent
          ? 'border-[#c8a864] bg-[#fff6df] text-[#7f6117] hover:bg-[#fcefc8]'
          : 'border-[#dde5f1] bg-white text-[#1b2d52] hover:border-[#c8a864]'
      }`}
    >
      {label}
    </a>
  );
}

export default function MemberPublicLinks({
  contact,
  personalSiteUrl,
  linkedinUrl,
  googleScholarUrl,
  accent = false,
}) {
  const hasLinks = contact || personalSiteUrl || linkedinUrl || googleScholarUrl;

  if (!hasLinks) {
    return null;
  }

  return (
    <div className="flex flex-wrap gap-2">
      {contact ? (
        <span
          className={`inline-flex items-center rounded-full border px-3 py-1.5 text-xs font-semibold ${
            accent
              ? 'border-[#d7c594] bg-[#fff7e6] text-[#6f5820]'
              : 'border-[#dde5f1] bg-[#f8fafc] text-[#4f627f]'
          }`}
        >
          {contact}
        </span>
      ) : null}

      <ExternalLink href={personalSiteUrl} label="Website" accent={accent} />
      <ExternalLink href={linkedinUrl} label="LinkedIn" accent={accent} />
      <ExternalLink href={googleScholarUrl} label="Google Scholar" accent={accent} />
    </div>
  );
}
