import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { BrowserRouter, Route, Routes } from 'react-router'
import AuthLayout from './components/auth/layout/AuthLayout.tsx'
import Login from './components/auth/login/Login.tsx'
import Register from './components/auth/register/Register.tsx'
import { PrimeReactProvider } from 'primereact/api'
import Tailwind from "primereact/passthrough/tailwind";

createRoot(document.getElementById('root')!).render(
  <BrowserRouter>
    <PrimeReactProvider value={{ pt: Tailwind }}>
      <StrictMode>
        <Routes>
          <Route path="/" element={<App />} />
          <Route path="/auth" element={<AuthLayout />} />
        </Routes>
      </StrictMode>
    </PrimeReactProvider>
    
  </BrowserRouter>
  
)
