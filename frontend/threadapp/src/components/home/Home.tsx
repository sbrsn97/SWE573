import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../layout/Navbar';
import { API_ENDPOINTS } from '../../config/config';

interface User {
  username: string;
  firstName: string;
  lastName: string;
  profilePicture?: string;
}

const Home = () => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const token = localStorage.getItem('token');
    
    if (!token) {
      navigate('/auth');
      return;
    }

    // Fetch user data
    const fetchUserData = async () => {
      try {
        const response = await fetch(API_ENDPOINTS.users.me, {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Accept': 'application/json'
          }
        });

        if (response.ok) {
          const { data } = await response.json();
          setUser(data);
        } else {
          navigate('/auth');
        }
      } catch (error) {
        console.error('Error fetching user data:', error);
        navigate('/auth');
      } finally {
        setLoading(false);
      }
    };

    fetchUserData();
  }, [navigate]);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50">
        <p className="text-gray-600">Loading...</p>
      </div>
    );
  }

  if (!user) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar user={user} />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-[80px]">
        <section className="bg-white rounded-xl shadow-sm p-8 mb-8">
          <h1 className="text-3xl font-semibold text-gray-900 mb-2">
            Welcome, {user.username}!
          </h1>
          <p className="text-lg text-gray-600">
            Start exploring and connecting with others.
          </p>
        </section>
      </main>
    </div>
  );
};

export default Home; 