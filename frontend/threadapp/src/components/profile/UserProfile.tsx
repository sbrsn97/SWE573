import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Avatar } from 'primereact/avatar';
import { Button } from 'primereact/button';
import { API_ENDPOINTS } from '../../config/config';
import MainLayout from '../layout/MainLayout';
import { fetchWithAuth, handleAuthError } from '../../utils/authUtils';
import { FaUserPlus, FaUserMinus } from 'react-icons/fa';
import eventBus, { EVENTS } from '../../utils/eventBus';

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
  followerIds?: number[];
  followingIds?: number[];
}

const UserProfile = () => {
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState<User | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [isFollowing, setIsFollowing] = useState(false);
  const [followLoading, setFollowLoading] = useState(false);
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
    const fetchCurrentUser = async () => {
      try {
        const response = await fetchWithAuth(API_ENDPOINTS.users.me);
        
        if (!response.ok) {
          if (handleAuthError(response, navigate)) return;
          return;
        }
        
        const { data } = await response.json();
        setCurrentUser(data);
      } catch (err) {
        console.error('Error fetching current user:', err);
      }
    };
    
    fetchCurrentUser();
  }, [navigate]);

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

  // Check if current user is following the profile user
  useEffect(() => {
    if (currentUser && user) {
      const isAlreadyFollowing = currentUser.followingIds?.includes(user.id);
      setIsFollowing(!!isAlreadyFollowing);
    }
  }, [currentUser, user]);

  const handleFollow = async () => {
    if (!currentUser || !user) return;
    
    setFollowLoading(true);
    try {
      const isUnfollowing = isFollowing;
      const endpoint = isUnfollowing 
        ? API_ENDPOINTS.users.unfollow(currentUser.id, Number(id))
        : API_ENDPOINTS.users.follow(currentUser.id, Number(id));
      
      const response = await fetchWithAuth(endpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        }
      });
      
      if (!response.ok) {
        if (handleAuthError(response, navigate)) return;
        throw new Error('Failed to follow/unfollow user');
      }
      
      // Update the following status and refetch the user to update counts
      setIsFollowing(!isFollowing);
      
      // Refetch user data to update follower counts
      const userResponse = await fetchWithAuth(`${API_ENDPOINTS.users.all}/${id}`);
      if (userResponse.ok) {
        const { data } = await userResponse.json();
        setUser({
          ...data,
          initials: data.firstName.charAt(0) + data.lastName.charAt(0)
        });
      }
      
      // Also refetch current user to update following list
      const currentUserResponse = await fetchWithAuth(API_ENDPOINTS.users.me);
      if (currentUserResponse.ok) {
        const { data } = await currentUserResponse.json();
        setCurrentUser(data);
      }
    } catch (err) {
      console.error('Error while following/unfollowing:', err);
    } finally {
      setFollowLoading(false);
    }
  };

  const renderProfile = (user: User) => (
    <div className="bg-white rounded-xl shadow-sm p-8">
      {error && (
        <div className="bg-red-50 text-red-600 p-4 rounded-lg mb-4">
          {error}
        </div>
      )}
      
      <div className="flex flex-col gap-6">
        <div className="flex justify-between items-center mb-4">
          <div className="text-3xl font-bold text-gray-800">User Profile</div>
          {currentUser && currentUser.id !== user.id && (
            <Button 
              className={isFollowing ? 'p-button-danger' : 'p-button-primary'}
              icon={isFollowing ? <FaUserMinus className="mr-2" /> : <FaUserPlus className="mr-2" />}
              label={isFollowing ? 'Unfollow' : 'Follow'}
              onClick={handleFollow}
              loading={followLoading}
            />
          )}
        </div>
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
              <div className="flex items-center">
                <span className="text-gray-800 font-medium">{user.followingIds?.length || 0}</span>
                <span className="text-gray-500 text-sm ml-2">users</span>
              </div>
            </div>
            
            <div className="py-2 flex items-center gap-4">
              <span className="font-semibold text-gray-700 min-w-[120px]">Followers:</span>
              <div className="flex items-center">
                <span className="text-gray-800 font-medium">{user.followerIds?.length || 0}</span>
                <span className="text-gray-500 text-sm ml-2">users</span>
              </div>
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