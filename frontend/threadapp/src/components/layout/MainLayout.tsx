import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from './Navbar';
import Sidebar from './Sidebar';
import { API_ENDPOINTS } from '../../config/config';
import { setCurrentUserId } from '../../utils/authUtils';

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  role: string;
  initials?: string;
  bio?: string;
  location?: string;
  profession?: string;
  birthDate?: string;
  createdAt?: string;
  updatedAt?: string;
  followerIds?: number[];
  followingIds?: number[];
}

interface MainLayoutProps {
  children: (user: User) => React.ReactNode;
}

function MainLayout({ children }: MainLayoutProps) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/auth');
      return;
    }

    const fetchUser = async () => {
      try {
        const response = await fetch(API_ENDPOINTS.users.me, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });

        if (!response.ok) {
          throw new Error('Failed to fetch user data');
        }

        const { data } = await response.json();
        
        // Store the user ID for user-specific local storage
        if (data.id) {
          setCurrentUserId(data.id);
        }
        
        setUser(data);
      } catch (error) {
        console.error('Error fetching user:', error);
        localStorage.removeItem('token');
        navigate('/auth');
      } finally {
        setLoading(false);
      }
    };

    fetchUser();
  }, [navigate]);

  if (loading) {
    return <div>Loading...</div>;
  }

  if (!user) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar user={user} />
      <div className="flex pt-16">
        <Sidebar />
        <main className="flex-1 p-8 ml-64">
          {children(user)}
        </main>
      </div>
    </div>
  );
}

export default MainLayout; 