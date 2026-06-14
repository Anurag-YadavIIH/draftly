import { Navigate, Route, BrowserRouter, Routes } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import InboxPage from './pages/InboxPage';
import DraftsPage from './pages/DraftsPage';
import DraftDetailPage from './pages/DraftDetailPage';
import PreferencesPage from './pages/PreferencesPage';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          <Route
            element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route path="/inbox" element={<InboxPage />} />
            <Route path="/drafts" element={<DraftsPage />} />
            <Route path="/drafts/:id" element={<DraftDetailPage />} />
            <Route path="/preferences" element={<PreferencesPage />} />
          </Route>

          <Route path="/" element={<Navigate to="/inbox" replace />} />
          <Route path="*" element={<Navigate to="/inbox" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
