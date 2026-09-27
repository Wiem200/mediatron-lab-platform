import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import PublicHeader from '../components/PublicHeader';
import { memberService } from '../api/memberService';

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

function MemberLink({ href, children }) {
  if (!href) {
    return null;
  }

  return (
    <a
      href={normalizeUrl(href)}
      target="_blank"
      rel="noreferrer"
      className="inline-flex rounded-full border border-[#d8ccb7] bg-[#faf8f3] px-3 py-1 text-xs font-bold uppercase tracking-wide text-[#8a6f3c] transition hover:border-[#c8a864]"
    >
      {children}
    </a>
  );
}

export default function PublicMembersPage() {
  const [members, setMembers] = useState([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const loadMembers = async () => {
      setLoading(true);
      setError('');

      try {
        const res = await memberService.getPublicList();
        const data = Array.isArray(res.data) ? res.data : [];
        setMembers(data);
      } catch (err) {
        console.error('Failed to load members:', err);
        setError('Unable to load members right now.');
        setMembers([]);
      } finally {
        setLoading(false);
      }
    };

    loadMembers();
  }, []);

  const filteredMembers = useMemo(() => {
    const query = search.trim().toLowerCase();

    if (!query) {
      return members;
    }

    return members.filter((member) => {
      const haystack = [
        member.firstName,
        member.lastName,
        member.grade,
        member.position,
        member.lab,
        member.researchAxes,
        member.bio,
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();

      return haystack.includes(query);
    });
  }, [members, search]);

  return (
    <div className="min-h-screen bg-[#f5f2ec] text-[#1b2d52]">
      <PublicHeader />

      <main className="mx-auto max-w-7xl px-6 py-10 lg:px-10">
        <section className="mb-8 rounded-[2rem] bg-[#1b2d52] px-8 py-10 text-white shadow-2xl">
          <div className="max-w-3xl">
            <p className="mb-4 text-xs font-bold uppercase tracking-[0.2em] sm:tracking-[0.35em] text-[#c8a864]">
              Research Community
            </p>

            <h1 className="font-brand-serif text-4xl leading-tight md:text-5xl">
              Meet the researchers and contributors behind the MEDIATRON laboratory.
            </h1>

            <p className="mt-5 max-w-2xl text-sm leading-7 text-white/65 md:text-base">
              Explore member profiles, discover research interests, and open each member page to see
              their approved publications and professional links.
            </p>
          </div>
        </section>

        <section className="mb-8 rounded-[2rem] border border-[#e7decf] bg-white p-6 shadow-xl">
          <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
            <div>
              <h2 className="font-brand-serif text-2xl text-[#1b2d52]">Browse Members</h2>
              <p className="mt-1 text-sm text-gray-500">
                Search by name, grade, lab, research axes, or bio.
              </p>
            </div>

            <div className="w-full md:max-w-md">
              <label className="mb-2 block text-[10px] font-bold uppercase tracking-[0.25em] text-gray-400">
                Search Members
              </label>
              <input
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Asma, Letaifa, AI, IoT..."
                className="w-full rounded-xl border-2 border-gray-100 bg-gray-50/60 px-4 py-3 outline-none transition focus:border-[#c8a864]"
              />
            </div>
          </div>
        </section>

        <section className="rounded-[2rem] border border-[#e7decf] bg-white p-6 shadow-xl">
          <div className="mb-6 flex items-center justify-between">
            <div>
              <h2 className="font-brand-serif text-2xl text-[#1b2d52]">Laboratory Members</h2>
              <p className="mt-1 text-sm text-gray-500">
                {loading ? 'Loading members...' : `${filteredMembers.length} member(s) found`}
              </p>
            </div>
          </div>

          {error ? (
            <div className="rounded-2xl border border-red-200 bg-red-50 px-5 py-4 text-sm text-red-600">
              {error}
            </div>
          ) : loading ? (
            <div className="py-16 text-center text-gray-500">Loading members...</div>
          ) : filteredMembers.length === 0 ? (
            <div className="rounded-[1.5rem] border border-dashed border-[#d8ccb7] bg-[#faf8f3] px-6 py-16 text-center">
              <h3 className="font-brand-serif text-2xl text-[#1b2d52]">No members found</h3>
              <p className="mt-3 text-sm text-gray-500">
                Try a different search term.
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
              {filteredMembers.map((member) => (
                <article
                  key={member.id}
                  className="rounded-[1.75rem] border border-[#ece3d4] bg-[#fffdf9] p-6 shadow-lg transition hover:-translate-y-1 hover:shadow-xl"
                >
                  <div className="mb-5 flex items-center gap-4">
                    <div className="h-16 w-16 overflow-hidden rounded-2xl border border-[#e7decf] bg-gray-100">
                      {member.photoUrl ? (
                        <img
                          src={member.photoUrl}
                          alt={buildFullName(member)}
                          className="h-full w-full object-cover"
                        />
                      ) : (
                        <div className="flex h-full w-full items-center justify-center bg-[#1b2d52] text-lg font-bold text-[#c8a864]">
                          {buildFullName(member).charAt(0)}
                        </div>
                      )}
                    </div>

                    <div>
                      <h3 className="font-brand-serif text-2xl leading-tight text-[#1b2d52]">
                        {buildFullName(member)}
                      </h3>
                      <p className="mt-1 text-sm font-semibold text-[#8a6f3c]">
                        {getMemberGrade(member)}
                      </p>
                      <p className="mt-1 text-xs uppercase tracking-[0.2em] text-gray-400">
                        {safeText(member.lab, 'MEDIATRON')}
                      </p>
                    </div>
                  </div>

                  <div className="mb-5 min-h-[88px]">
                    <p className="text-sm leading-7 text-gray-600">
                      {safeText(member.bio, 'No biography available yet.')}
                    </p>
                  </div>

                  <div className="mb-5 flex flex-wrap gap-2">
                    <MemberLink href={member.googleScholarUrl}>Scholar</MemberLink>
                    <MemberLink href={member.linkedinUrl}>LinkedIn</MemberLink>
                    <MemberLink href={member.personalSiteUrl || member.personalWebsite}>Website</MemberLink>
                  </div>

                  <div className="rounded-2xl bg-[#faf8f3] p-4">
                    <p className="mb-2 text-[10px] font-bold uppercase tracking-[0.25em] text-gray-400">
                      Research Axes
                    </p>
                    <p className="text-sm leading-6 text-gray-600">
                      {safeText(member.researchAxes, 'Not specified')}
                    </p>
                  </div>

                  <div className="mt-6">
                    <Link
                      to={`/members/${member.id}`}
                      className="inline-flex items-center rounded-xl bg-[#1b2d52] px-5 py-3 text-sm font-bold text-white transition hover:bg-[#2b4477]"
                    >
                      See More
                    </Link>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}