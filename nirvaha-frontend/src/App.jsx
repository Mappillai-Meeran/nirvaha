import React from 'react';
import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth, ROLES } from './context/AuthContext';
import { LanguageProvider } from './context/LanguageContext';
import { Header } from './components/layout/Header';
import { LandingPage } from './pages/LandingPage';
import { Login } from './pages/auth/Login';
import { ForgotPassword } from './pages/auth/ForgotPassword';
import { SignUp } from './pages/auth/SignUp';
import { CitizenReport } from './pages/citizen/CitizenReport';
import { MyReports } from './pages/citizen/MyReports';
import { CommandCenter } from './pages/government/CommandCenter';
import { PriorityQueue } from './pages/government/PriorityQueue';
import { IntegrityRadar } from './pages/government/IntegrityRadar';
import { IncidentDetail } from './pages/government/IncidentDetail';

const ProtectedRoute = ({ children, allowedRoles }) => {
  const { currentUser, isDemoMode } = useAuth();
  if (!currentUser && isDemoMode) {
    return (
      <div className="flex items-center justify-center min-h-[50vh] text-xs text-[#66716E] font-medium">
        Synchronizing municipal session...
      </div>
    );
  }
  return allowedRoles.includes(currentUser?.role) ? children : <Navigate to="/login" replace />;
};

const AppRoutes = () => {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<Login />} />
      <Route path="/signup" element={<SignUp />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/citizen/report" element={<ProtectedRoute allowedRoles={[ROLES.CITIZEN]}><CitizenReport /></ProtectedRoute>} />
      <Route path="/citizen/my-reports" element={<ProtectedRoute allowedRoles={[ROLES.CITIZEN]}><MyReports /></ProtectedRoute>} />
      <Route path="/command-center" element={<ProtectedRoute allowedRoles={[ROLES.OFFICER, ROLES.ADMIN]}><CommandCenter /></ProtectedRoute>} />
      <Route path="/command-center/queue" element={<ProtectedRoute allowedRoles={[ROLES.OFFICER, ROLES.ADMIN]}><PriorityQueue /></ProtectedRoute>} />
      <Route path="/command-center/anomalies" element={<ProtectedRoute allowedRoles={[ROLES.OFFICER, ROLES.ADMIN]}><IntegrityRadar /></ProtectedRoute>} />
      <Route path="/incidents/:id" element={<ProtectedRoute allowedRoles={[ROLES.CITIZEN, ROLES.OFFICER, ROLES.ADMIN]}><IncidentDetail /></ProtectedRoute>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

const AppFooter = () => {
  const location = useLocation();
  // Hide footer on landing page and on the full-viewport command center shell
  if (location.pathname === '/' || location.pathname.startsWith('/command-center')) return null;
  return (
    <footer className="app-footer">
      Nirvaha Civic Intelligence Workspace • Directorate of Municipal Administration, Puducherry
    </footer>
  );
};

export default function App() {
  return (
    <AuthProvider>
      <LanguageProvider>
        <BrowserRouter>
          <div className="app-shell">
            <Header />
            <main className="flex-1"><AppRoutes /></main>
            <AppFooter />
          </div>
        </BrowserRouter>
      </LanguageProvider>
    </AuthProvider>
  );
}
