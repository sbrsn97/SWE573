import React, { useState, useEffect } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { fetchWithAuth } from '../../utils/authUtils';
import { API_ENDPOINTS } from '../../config/config';

/**
 * A route wrapper that only allows access to admin users
 * Redirects to home page if user is not an admin
 */
const AdminRoute: React.FC = () => {
  const [isAdmin, setIsAdmin] = useState<boolean | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const checkAdminStatus = async () => {
      try {
        const token = localStorage.getItem('token');
        if (!token) {
          setIsAdmin(false);
          setLoading(false);
          return;
        }

        const response = await fetchWithAuth(API_ENDPOINTS.users.me);
        
        if (!response.ok) {
          setIsAdmin(false);
          setLoading(false);
          return;
        }

        const { data } = await response.json();
        setIsAdmin(data.role === 'ADMIN');
      } catch (error) {
        console.error('Error checking admin status:', error);
        setIsAdmin(false);
      } finally {
        setLoading(false);
      }
    };

    checkAdminStatus();
  }, []);

  if (loading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  return isAdmin ? <Outlet /> : <Navigate to="/" replace />;
};

export default AdminRoute; 