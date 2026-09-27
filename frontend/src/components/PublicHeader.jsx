import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useLanguage } from '../context/LanguageContext';

function getStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null');
  } catch {
    return null;
  }
}

export default function PublicHeader() {
  const location = useLocation();
  const { language } = useLanguage();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const user = getStoredUser();

  const copy = {
    en: {
      portal: 'Research Portal',
      home: 'Home',
      about: 'About',
      publications: 'Publications',
      events: 'Events',
      projects: 'Research Projects',
      statistics: 'Statistics',
      members: 'Members',
      labMembers: 'Lab Members',
      phdStudents: 'PhD Students',
      login: 'Login',
      dashboard: 'Dashboard',
      menu: 'Menu',
      close: 'Close menu',
      open: 'Open menu',
    },
    fr: {
      portal: 'Portail de Recherche',
      home: 'Accueil',
      about: 'A Propos',
      publications: 'Publications',
      events: 'Evenements',
      projects: 'Projets de Recherche',
      statistics: 'Statistiques',
      members: 'Membres',
      labMembers: 'Membres du Laboratoire',
      phdStudents: 'Doctorants',
      login: 'Connexion',
      dashboard: 'Dashboard',
      menu: 'Menu',
      close: 'Fermer le menu',
      open: 'Ouvrir le menu',
    },
  }[language] || {};

  useEffect(() => {
    setMobileMenuOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!mobileMenuOpen) {
      return undefined;
    }

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        setMobileMenuOpen(false);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [mobileMenuOpen]);

  const isActive = (path) => {
    if (path === '/') {
      return location.pathname === '/';
    }

    if (path === '/members') {
      return location.pathname === '/members'
        || location.pathname.startsWith('/members/')
        || location.pathname === '/phd-directory';
    }

    return location.pathname === path || location.pathname.startsWith(`${path}/`);
  };

  const navItems = useMemo(() => [
    { to: '/', label: copy.home },
    { to: '/about-mediatron', label: copy.about },
    { to: '/publications', label: copy.publications },
    { to: '/lab-events', label: copy.events },
    { to: '/research-projects', label: copy.projects },
    { to: '/statistics', label: copy.statistics },
    { to: '/members', label: copy.labMembers },
    { to: '/phd-directory', label: copy.phdStudents },
    user?.token
      ? { to: '/dashboard', label: copy.dashboard }
      : { to: '/login', label: copy.login },
  ], [copy.about, copy.dashboard, copy.events, copy.home, copy.labMembers, copy.login, copy.phdStudents, copy.projects, copy.publications, copy.statistics, user?.token]);

  const navLinkClass = (path) =>
    `whitespace-nowrap rounded-full px-3 py-2 text-xs font-black transition-all 2xl:px-4 2xl:text-sm ${
      isActive(path)
        ? 'bg-[#c8a864] text-[#1b2d52] shadow-sm'
        : 'text-[#1b2d52] hover:bg-white/80'
    }`;

  const mobileLinkClass = (path) =>
    `block rounded-2xl px-4 py-3 text-sm font-black transition ${
      isActive(path)
        ? 'bg-[#f1dfb8] text-[#1b2d52]'
        : 'text-[#52627e] hover:bg-[#f8f4ec] hover:text-[#1b2d52]'
    }`;

  return (
    <header className="sticky top-0 z-40 border-b border-[#e7decf] bg-[#f5f2ec]/95 backdrop-blur">
      <div className="mx-auto flex max-w-[96rem] items-center justify-between gap-3 px-4 py-3 sm:px-5 lg:px-8 xl:gap-6 xl:py-4">
        <Link to="/" className="flex min-w-0 shrink-0 items-center gap-3">
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-[#1b2d52] text-sm font-black text-[#c8a864] shadow-lg sm:h-10 sm:w-10">
            M
          </div>

          <div className="min-w-0">
            <p className="font-brand-serif text-base leading-none text-[#1b2d52] sm:text-lg">MEDIATRON</p>
            <p className="mt-1 max-w-[11rem] truncate text-[9px] uppercase tracking-[0.24em] text-[#8f7a4d] sm:max-w-none sm:text-[10px] sm:tracking-[0.28em]">
              {copy.portal}
            </p>
          </div>
        </Link>

        <nav className="hidden min-w-0 items-center justify-end gap-1 xl:flex 2xl:gap-2">
          <Link to="/" className={navLinkClass('/')}>
            {copy.home}
          </Link>

          <Link to="/about-mediatron" className={navLinkClass('/about-mediatron')}>
            {copy.about}
          </Link>

          <Link to="/publications" className={navLinkClass('/publications')}>
            {copy.publications}
          </Link>

          <Link to="/lab-events" className={navLinkClass('/lab-events')}>
            {copy.events}
          </Link>

          <Link to="/research-projects" className={navLinkClass('/research-projects')}>
            {copy.projects}
          </Link>

          <Link to="/statistics" className={navLinkClass('/statistics')}>
            {copy.statistics}
          </Link>

          <div className="group relative">
            <Link
              to="/members"
              className={`${navLinkClass('/members')} inline-flex items-center gap-1.5`}
            >
              {copy.members}
              <span className="text-xs leading-none">⌄</span>
            </Link>

            <div className="invisible absolute right-0 top-full z-50 mt-3 w-64 translate-y-1 rounded-[1.5rem] border border-[#e3d7c4] bg-white p-2 opacity-0 shadow-[0_20px_45px_rgba(27,45,82,0.16)] transition-all group-hover:visible group-hover:translate-y-0 group-hover:opacity-100">
              <Link to="/members" className={mobileLinkClass('/members')}>
                {copy.labMembers}
              </Link>
              <Link to="/phd-directory" className={mobileLinkClass('/phd-directory')}>
                {copy.phdStudents}
              </Link>
            </div>
          </div>

          {user?.token ? (
            <Link to="/dashboard" className={navLinkClass('/dashboard')}>
              {copy.dashboard}
            </Link>
          ) : (
            <Link to="/login" className={navLinkClass('/login')}>
              {copy.login}
            </Link>
          )}
        </nav>

        <button
          type="button"
          onClick={() => setMobileMenuOpen((current) => !current)}
          aria-label={mobileMenuOpen ? copy.close : copy.open}
          aria-expanded={mobileMenuOpen}
          className="inline-flex h-10 items-center justify-center rounded-full border border-[#e3d7c4] bg-white px-4 text-xs font-black uppercase tracking-[0.18em] text-[#1b2d52] shadow-sm transition hover:bg-[#f8f4ec] xl:hidden"
        >
          {mobileMenuOpen ? '×' : copy.menu}
        </button>
      </div>

      {mobileMenuOpen && (
        <div className="border-t border-[#e7decf] bg-[#f5f2ec]/98 px-4 pb-4 shadow-xl xl:hidden">
          <nav className="mx-auto grid max-w-[44rem] gap-2 rounded-b-[1.5rem] bg-white/70 p-3 shadow-sm sm:grid-cols-2">
            {navItems.map((item) => (
              <Link key={item.to} to={item.to} className={mobileLinkClass(item.to)}>
                {item.label}
              </Link>
            ))}
          </nav>
        </div>
      )}
    </header>
  );
}
