import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import { BrowserRouter, Route, Routes } from 'react-router'
import AuthLayout from './components/auth/layout/AuthLayout.tsx'
import { PrimeReactProvider } from 'primereact/api'
import Tailwind from "primereact/passthrough/tailwind";
import Profile from './components/profile/Profile.tsx'
import Home from './components/home/Home.tsx'

createRoot(document.getElementById('root')!).render(
  <BrowserRouter>
    <PrimeReactProvider value={{ pt: Tailwind }}>
      <StrictMode>
        <Routes>
          <Route path="/home" element={<Home />} />
          <Route path="/auth" element={<AuthLayout />} />
          <Route path="/profile" element={<Profile />} />
        </Routes>
      </StrictMode>
    </PrimeReactProvider>
    
  </BrowserRouter>
  
)
