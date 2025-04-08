import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { API_ENDPOINTS } from '../../config/config';

interface Thread {
  id: number;
  title: string;
  description: string;
}

interface User {
  id: number;
  username: string;
  firstName: string;
  lastName: string;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  code?: string;
}

interface SearchResultsProps {
  query: string;
  onClose: () => void;
}

const SearchResults = ({ query, onClose }: SearchResultsProps) => {
  const [activeTab, setActiveTab] = useState<'threads' | 'users'>('threads');
  const [threads, setThreads] = useState<Thread[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  const handleApiError = async (response: Response) => {
    const errorData: ApiResponse<any> = await response.json();
    if (response.status === 401 && errorData.code === 'TOKEN_EXPIRED') {
      localStorage.removeItem('token');
      navigate('/auth');
      return true;
    }
    setError(errorData.message || 'An error occurred while searching');
    return false;
  };

  useEffect(() => {
    const search = async () => {
      if (!query.trim()) {
        setThreads([]);
        setUsers([]);
        setError(null);
        return;
      }

      setLoading(true);
      setError(null);
      try {
        const token = localStorage.getItem('token');
        if (!token) {
          navigate('/auth');
          return;
        }

        if (activeTab === 'threads') {
          // Search threads
          const threadsResponse = await fetch(`${API_ENDPOINTS.threads.search}?keyword=${encodeURIComponent(query.toLowerCase())}`, {
            headers: {
              'Authorization': `Bearer ${token}`
            }
          });
          
          if (!threadsResponse.ok) {
            const shouldRedirect = await handleApiError(threadsResponse);
            if (shouldRedirect) return;
          } else {
            const threadsData: ApiResponse<Thread[]> = await threadsResponse.json();
            if (threadsData.success) {
              setThreads(threadsData.data);
            } else {
              setError(threadsData.message);
            }
          }
        } else {
          // Search users
          const usersResponse = await fetch(`${API_ENDPOINTS.users.search}?keyword=${encodeURIComponent(query.toLowerCase())}`, {
            headers: {
              'Authorization': `Bearer ${token}`
            }
          });

          if (!usersResponse.ok) {
            const shouldRedirect = await handleApiError(usersResponse);
            if (shouldRedirect) return;
          } else {
            const usersData: ApiResponse<User[]> = await usersResponse.json();
            if (usersData.success) {
              setUsers(usersData.data);
            } else {
              setError(usersData.message);
            }
          }
        }
      } catch (error) {
        console.error('Search failed:', error);
        setError('An error occurred while searching');
      } finally {
        setLoading(false);
      }
    };

    const debounceTimer = setTimeout(search, 300);
    return () => clearTimeout(debounceTimer);
  }, [query, activeTab, navigate]);

  return (
    <div className="absolute top-full left-0 right-0 mt-2 bg-white rounded-lg shadow-lg max-h-[400px] overflow-hidden">
      <div className="flex border-b">
        <button
          className={`flex-1 py-2 px-4 text-sm font-medium ${
            activeTab === 'threads'
              ? 'text-blue-600 border-b-2 border-blue-600'
              : 'text-gray-500 hover:text-gray-700'
          }`}
          onClick={() => setActiveTab('threads')}
        >
          Threads
        </button>
        <button
          className={`flex-1 py-2 px-4 text-sm font-medium ${
            activeTab === 'users'
              ? 'text-blue-600 border-b-2 border-blue-600'
              : 'text-gray-500 hover:text-gray-700'
          }`}
          onClick={() => setActiveTab('users')}
        >
          Users
        </button>
      </div>

      <div className="overflow-y-auto max-h-[350px]">
        {error ? (
          <div className="p-4 text-center text-red-500">{error}</div>
        ) : loading ? (
          <div className="p-4 text-center text-gray-500">Searching...</div>
        ) : activeTab === 'threads' ? (
          threads.length > 0 ? (
            threads.map((thread) => (
              <Link
                key={thread.id}
                to={`/threads/${thread.id}`}
                className="block p-4 hover:bg-gray-50 border-b last:border-b-0"
                onClick={onClose}
              >
                <h3 className="font-medium text-gray-900">{thread.title}</h3>
                <p className="text-sm text-gray-500 truncate">{thread.description}</p>
              </Link>
            ))
          ) : (
            <div className="p-4 text-center text-gray-500">No threads found</div>
          )
        ) : (
          users.length > 0 ? (
            users.map((user) => (
              <Link
                key={user.id}
                to={`/profile/${user.id}`}
                className="block p-4 hover:bg-gray-50 border-b last:border-b-0"
                onClick={onClose}
              >
                <h3 className="font-medium text-gray-900">
                  {user.firstName} {user.lastName}
                </h3>
                <p className="text-sm text-gray-500">@{user.username}</p>
              </Link>
            ))
          ) : (
            <div className="p-4 text-center text-gray-500">No users found</div>
          )
        )}
      </div>
    </div>
  );
};

export default SearchResults; 