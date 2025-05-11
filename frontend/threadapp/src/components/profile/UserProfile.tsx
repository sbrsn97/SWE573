import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Avatar } from 'primereact/avatar';
import { API_ENDPOINTS } from '../../config/config';
import MainLayout from '../layout/MainLayout';
import { fetchWithAuth, handleAuthError } from '../../utils/authUtils';

interface User {
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
  followers?: any[];
  following?: any[];
}

const UserProfile = () => {
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState<User | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const formatDate = (dateString?: string) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('tr-TR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  };

  useEffect(() => {
    const fetchUser = async () => {
      setLoading(true);
      setError(null);
      
      try {
        // Fetch the user by ID
        const response = await fetchWithAuth(`${API_ENDPOINTS.users.all}/${id}`);
        
        if (!response.ok) {
          if (handleAuthError(response, navigate)) return;
          
          if (response.status === 404) {
            setError('User not found');
          } else {
            setError('Failed to load user profile');
          }
          setLoading(false);
          return;
        }
        
        const { data } = await response.json();
        setUser({
          ...data,
          initials: data.firstName.charAt(0) + data.lastName.charAt(0)
        });
      } catch (err) {
        setError('An error occurred while fetching the user profile');
        console.error('Error fetching user:', err);
      } finally {
        setLoading(false);
      }
    };
    
    if (id) {
      fetchUser();
    }
  }, [id, navigate]);

  const renderProfile = (user: User) => (
    <div className="bg-white rounded-xl shadow-sm p-8">
      {error && (
        <div className="bg-red-50 text-red-600 p-4 rounded-lg mb-4">
          {error}
        </div>
      )}
      
      <div className="flex flex-col gap-6">
        <div className="text-3xl font-bold text-gray-800 mb-4">User Profile</div>
        <div className="flex flex-col gap-6">
          <div className="flex items-center gap-8 mb-4">
            <div className="w-[5rem] h-[5rem] flex items-center justify-center">
              <Avatar label={user.initials} shape="circle" size="xlarge" />
            </div>
            
            <div className="flex items-center h-[5rem]">
              <span className="text-xl font-bold text-gray-800">
                {user.firstName} {user.lastName}
              </span>
            </div>
          </div>
          
          <div className="flex flex-col">
            <div className="py-2 flex items-center gap-4">
              <span className="font-semibold text-gray-700 min-w-[120px]">Username:</span>
              <span className="text-gray-800">{user.username}</span>
            </div>
            
            <div className="py-2 flex items-center gap-4">
              <span className="font-semibold text-gray-700 min-w-[120px]">Email:</span>
              <span className="text-gray-800">{user.email}</span>
            </div>
            
            <div className="py-2 flex items-center gap-4">
              <span className="font-semibold text-gray-700 min-w-[120px]">Birth Date:</span>
              <span className="text-gray-800">
                {user.birthDate ? formatDate(user.birthDate) : 'No birth date added'}
              </span>
            </div>
            
            <div className="py-2 flex items-start gap-4">
              <span className="font-semibold text-gray-700 min-w-[120px]">Bio:</span>
              <span className="text-gray-800">
                {user.bio || 'No bio added'}
              </span>
            </div>
            
            <div className="py-2 flex items-center gap-4">
              <span className="font-semibold text-gray-700 min-w-[120px]">Location:</span>
              <span className="text-gray-800">
                {user.location || 'No location added'}
              </span>
            </div>
            
            <div className="py-2 flex items-center gap-4">
              <span className="font-semibold text-gray-700 min-w-[120px]">Profession:</span>
              <span className="text-gray-800">
                {user.profession || 'No profession added'}
              </span>
            </div>
            
            <div className="py-2 flex items-center gap-4">
              <span className="font-semibold text-gray-700 min-w-[120px]">Following:</span>
              <span className="text-gray-800">{user.following?.length || 0}</span>
            </div>
            
            <div className="py-2 flex items-center gap-4">
              <span className="font-semibold text-gray-700 min-w-[120px]">Followers:</span>
              <span className="text-gray-800">{user.followers?.length || 0}</span>
            </div>
            
            {user.createdAt && (
              <div className="py-2 flex items-center gap-4">
                <span className="font-semibold text-gray-700 min-w-[120px]">Joined:</span>
                <span className="text-gray-800">{formatDate(user.createdAt)}</span>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );

  return (
    <MainLayout>
      {() => (
        loading ? (
          <div className="flex justify-center items-center py-6">
            <p className="text-gray-600">Loading profile...</p>
          </div>
        ) : user ? (
          renderProfile(user)
        ) : (
          <div className="bg-white rounded-xl shadow-sm p-8">
            <div className="text-center py-6 text-gray-500">
              {error || 'User not found'}
            </div>
          </div>
        )
      )}
    </MainLayout>
  );
};

export default UserProfile; 