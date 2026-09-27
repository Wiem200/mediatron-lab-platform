import Sidebar from '../components/Sidebar';
import { Link } from 'react-router-dom';
import { getPrimaryRole, hasAdminPrivileges, useStoredUser } from '../utils/authStore';

export default function Dashboard() {
  const user = useStoredUser();
  const role = getPrimaryRole(user);
  const canAccessAdminArea = hasAdminPrivileges(user);

  return (
    <div className="flex bg-[#f5f2ec] min-h-screen">
      <Sidebar />

      <main className="w-full px-4 pb-10 pt-24 sm:px-6 lg:ml-64 lg:p-10">
        <header className="mb-8">
          <h1 className="text-3xl font-serif text-[#1b2d52]">
            Welcome  {user?.email.split('@')[0]}
          </h1>
          <p className="text-gray-500 italic">Space {user?.roles[0].replace('ROLE_', '')}</p>
        </header>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
            <h3 className="text-gray-400 text-xs font-bold uppercase tracking-wider mb-2">Statut Session</h3>
            <p className="text-[#166534] font-medium">
              {role === 'ROLE_LAB_HEAD' && user?.delegationActive ? 'Admin Mode Active' : 'Succesfuly Connected'}
            </p>
          </div>
          {canAccessAdminArea && (
            <Link
              to="/manage-projects"
              className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 transition hover:border-[#c8a864]"
            >
              <h3 className="text-gray-400 text-xs font-bold uppercase tracking-wider mb-2">Projects</h3>
              <p className="text-[#1b2d52] font-medium">Open project management</p>
            </Link>
          )}
          {/* On ajoutera ici les compteurs de publications au Sprint 2 */}
        </div>
      </main>
    </div>
  );
}
