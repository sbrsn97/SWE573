import { StrictMode, useEffect } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import { BrowserRouter, Route, Routes, useNavigate } from 'react-router-dom'
import AuthLayout from './components/auth/layout/AuthLayout.tsx'
import { PrimeReactProvider } from 'primereact/api'
import Tailwind from "primereact/passthrough/tailwind";
import UserProfile from './components/profile/UserProfile.tsx'
import Home from './components/home/Home.tsx'
import ThreadDetail from './components/threads/ThreadDetail.tsx'
import ThreadsList from './components/threads/ThreadsList.tsx'
import ThreadHistory from './components/threads/ThreadHistory.tsx'
import ThreadEdit from './components/threads/ThreadEdit.tsx'
import { setupAuthInterceptor } from './utils/authUtils'
import AuthRoute from './components/auth/AuthRoute.tsx'
import AdminRoute from './components/routes/AdminRoute.tsx'
import AdminDashboard from './components/admin/AdminDashboard.tsx'
import ProfanityManagement from './components/admin/ProfanityManagement.tsx'
import TagManagement from './components/admin/TagManagement.tsx'
import CreateThreadPage from './components/threads/CreateThreadPage.tsx'
import NotificationsPage from './pages/NotificationsPage'

const App = () => {
  const navigate = useNavigate();
  
  useEffect(() => {
    // Setup authentication interceptor
    setupAuthInterceptor(navigate);
  }, [navigate]);
  
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/auth" element={<AuthLayout />} />
      
      {/* Protected routes */}
      <Route path="/home" element={
        <AuthRoute>
          <Home />
        </AuthRoute>
      } />
      <Route path="/users/:id" element={
        <AuthRoute>
          <UserProfile />
        </AuthRoute>
      } />
      <Route path="/threads" element={
        <AuthRoute>
          <ThreadsList />
        </AuthRoute>
      } />
      
      <Route path="/threads/new" element={
        <AuthRoute>
          <CreateThreadPage />
        </AuthRoute>
      } />
      
      <Route path="/threads/:id" element={
        <AuthRoute>
          <ThreadDetail />
        </AuthRoute>
      } />
      <Route path="/threads/:id/history" element={
        <AuthRoute>
          <ThreadHistory />
        </AuthRoute>
      } />
      <Route path="/threads/:id/edit" element={
        <AuthRoute>
          <ThreadEdit />
        </AuthRoute>
      } />
      
      <Route path="/notifications" element={
        <AuthRoute>
          <NotificationsPage />
        </AuthRoute>
      } />

      {/* Admin routes */}
      <Route path="/admin" element={<AdminRoute />}>
        <Route index element={<AdminDashboard />} />
        <Route path="profanity" element={<ProfanityManagement />} />
        <Route path="tags" element={<TagManagement />} />
      </Route>
      
      {/* Default route */}
      <Route path="*" element={
        <AuthRoute>
          <Home />
        </AuthRoute>
      } />
    </Routes>
  );
};

createRoot(document.getElementById('root')!).render(
  <BrowserRouter>
    <PrimeReactProvider value={{ pt: Tailwind }}>
      <StrictMode>
        <App />
      </StrictMode>
    </PrimeReactProvider>
  </BrowserRouter>
)
