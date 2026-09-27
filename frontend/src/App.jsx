import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import AboutMediatron from './pages/AboutMediatron';
import ForgotPassword from './pages/ForgotPassword';
import LoginPage from './pages/LoginPage';
import ResetPassword from './pages/ResetPassword';
import Dashboard from './pages/Dashboard';
import PrivateRoute from './components/PrivateRoute';
import Homepage from './pages/Homepage';
import Members from './pages/Members';
import Publications from './pages/Publications';
import PublicationsPage from './pages/PublicationsPage';
import MemberListPublic from './pages/MemberListPublic';
import MemberProfilePublic from './pages/MemberProfilePublic';
import PhDDirectory from './pages/PhDDirectory';
import EventsManagement from './pages/EventsManagement';
import Projects from './pages/Projects';
import ValidationDashboard from './pages/ValidationDashboard';
import ProjectValidation from './pages/ProjectValidation';
import ProjectsList from './pages/ProjectsList';
import ProjectDetailPublic from './pages/ProjectDetailPublic';
import EventsPublic from './pages/EventsPublic';
import Statistics from './pages/Statistics';
import Subscribers from './pages/Subscribers';
import ChatbotWidget from './components/ChatbotWidget';
import { LanguageProvider } from './context/LanguageContext';

function App() {
  return (
    <LanguageProvider>
      <Router>
        <ChatbotWidget />
        <Routes>
        {/* Public routes */}
        <Route path="/" element={<Homepage />} />
        <Route path="/about-mediatron" element={<AboutMediatron />} />
        <Route path="/publications" element={<PublicationsPage />} />
        <Route path="/members" element={<MemberListPublic />} />
        <Route path="/members/:id" element={<MemberProfilePublic />} />
        <Route path="/phd-directory" element={<PhDDirectory />} />
        <Route path="/lab-events" element={<EventsPublic />} />
        <Route path="/research-projects" element={<ProjectsList />} />
        <Route path="/research-projects/:id" element={<ProjectDetailPublic />} />
        <Route path="/statistics" element={<Statistics />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />

        {/* Protected routes */}
        <Route
          path="/dashboard"
          element={
            <PrivateRoute>
              <Dashboard />
            </PrivateRoute>
          }
        />

        <Route
          path="/manage-members"
          element={
            <PrivateRoute>
              <Members />
            </PrivateRoute>
          }
        />

        <Route
          path="/events"
          element={
            <PrivateRoute>
              <EventsManagement />
            </PrivateRoute>
          }
        />

        <Route
          path="/manage-projects"
          element={
            <PrivateRoute>
              <Projects />
            </PrivateRoute>
          }
        />

        <Route
          path="/validate"
          element={
            <PrivateRoute>
              <ValidationDashboard />
            </PrivateRoute>
          }
        />

        <Route
          path="/newsletter-audience"
          element={
            <PrivateRoute>
              <Subscribers />
            </PrivateRoute>
          }
        />

        <Route
          path="/validate-events"
          element={
            <PrivateRoute>
              <ValidationDashboard />
            </PrivateRoute>
          }
        />

        <Route
          path="/validate-projects"
          element={
            <PrivateRoute>
              <ProjectValidation />
            </PrivateRoute>
          }
        />

        <Route
          path="/my-publications"
          element={
            <PrivateRoute>
              <Publications />
            </PrivateRoute>
          }
        />

        {/* Unknown routes go back to the public homepage */}
        <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Router>
    </LanguageProvider>
  );
}

export default App;
