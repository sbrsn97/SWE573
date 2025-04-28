import { StrictMode, useEffect } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import { BrowserRouter, Route, Routes, useNavigate } from 'react-router-dom'
import AuthLayout from './components/auth/layout/AuthLayout.tsx'
import { PrimeReactProvider } from 'primereact/api'
import Tailwind from "primereact/passthrough/tailwind";
import Profile from './components/profile/Profile.tsx'
import Home from './components/home/Home.tsx'
import ThreadDetail from './components/threads/ThreadDetail.tsx'
import { setupAuthInterceptor } from './utils/authUtils'
import AuthRoute from './components/auth/AuthRoute.tsx'

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
      <Route path="/profile" element={
        <AuthRoute>
          <Profile />
        </AuthRoute>
      } />
      <Route path="/threads/:id" element={
        <AuthRoute>
          <ThreadDetail />
        </AuthRoute>
      } />
      
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
