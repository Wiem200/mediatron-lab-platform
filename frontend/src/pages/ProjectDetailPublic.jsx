import { useEffect, useState } from 'react';
import { Link, Navigate, useParams } from 'react-router-dom';
import ProjectDetails from '../components/ProjectDetails';
import PublicHeader from '../components/PublicHeader';
import { useLanguage } from '../context/LanguageContext';
import { projectService } from '../api/projectService';

export default function ProjectDetailPublic() {
  const { language } = useLanguage();
  const { id } = useParams();
  const [project, setProject] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const copy = {
    en: {
      loadError: 'Unable to load this research project.',
      loading: 'Loading project details...',
      back: 'Back to Research Projects',
      eyebrow: 'Research Project',
      lead: 'Project Lead',
      memberFallback: 'Laboratory Member',
      funding: 'Funding',
      duration: 'Duration',
      unspecified: 'Not specified',
      description: 'Detailed Description',
      hub: 'Resource Hub',
      hubSubtitle: 'Reports, codebases, posters, and video presentations linked to this project.',
      teamTitle: 'Project Team',
      teamSubtitle: 'Lead researcher, participating members, and doctoral contributors involved in this work.',
      members: 'Laboratory Members',
      membersSubtitle: 'Participating researchers linked to this project.',
      membersEmpty: 'No laboratory members are listed for this project.',
      phd: 'PhD Students',
      phdSubtitle: 'Doctoral researchers involved in the project activities.',
      phdEmpty: 'No PhD students are listed for this project.',
    },
    fr: {
      loadError: "Impossible de charger ce projet de recherche.",
      loading: 'Chargement des details du projet...',
      back: 'Retour aux Projets de Recherche',
      eyebrow: 'Projet de Recherche',
      lead: 'Responsable du Projet',
      memberFallback: 'Membre du Laboratoire',
      funding: 'Financement',
      duration: 'Duree',
      unspecified: 'Non precise',
      description: 'Description Detaillee',
      hub: 'Centre de Ressources',
      hubSubtitle: 'Rapports, codes sources, posters et presentations video lies a ce projet.',
      teamTitle: 'Equipe du Projet',
      teamSubtitle: 'Responsable scientifique, membres participants et doctorants impliques dans ce travail.',
      members: 'Membres du Laboratoire',
      membersSubtitle: 'Chercheurs participants rattaches a ce projet.',
      membersEmpty: "Aucun membre du laboratoire n'est liste pour ce projet.",
      phd: 'Doctorants',
      phdSubtitle: 'Chercheurs doctorants impliques dans les activites du projet.',
      phdEmpty: "Aucun doctorant n'est liste pour ce projet.",
    },
  }[language];

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        const response = await projectService.getPublicById(id);
        setProject(response.data);
      } catch (err) {
        const data = err?.response?.data;
        const message =
          data?.message ||
          data?.error ||
          (typeof data === 'string' && data) ||
          err?.message ||
          copy.loadError;
        setError(message);
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [copy.loadError, id]);

  if (!loading && !error && !project) {
    return <Navigate to="/research-projects" replace />;
  }

  return (
    <div className="min-h-screen bg-[#f5f2ec]">
      <PublicHeader />
      <main className="mx-auto max-w-7xl px-6 py-10 lg:px-10">
        {loading ? (
          <div className="rounded-[2rem] border border-[#e3dac9] bg-white px-6 py-12 text-center text-sm text-[#71809b] shadow-lg">
            {copy.loading}
          </div>
        ) : error ? (
          <div className="rounded-[2rem] border border-rose-200 bg-rose-50 px-6 py-10 text-sm text-rose-700">
            {error}
          </div>
        ) : (
          <>
            <Link
              to="/research-projects"
              className="mb-6 inline-flex items-center rounded-full border border-[#d7ccb8] bg-white px-4 py-2 text-sm font-semibold text-[#1b2d52] shadow-sm"
            >
              {copy.back}
            </Link>

            <ProjectDetails project={project} copy={copy} />
          </>
        )}
      </main>
    </div>
  );
}
