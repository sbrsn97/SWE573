import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { API_ENDPOINTS } from '../../config/config';
import MainLayout from '../layout/MainLayout';
import { FaThumbsUp, FaThumbsDown, FaRegThumbsUp, FaRegThumbsDown } from 'react-icons/fa';

interface Tag {
  id: number;
  label: string;
  description: string;
  colorCodeString: string;
  wikidataEntityId: string;
}

interface Thread {
  id: number;
  title: string;
  description: string | null;
  authorId: number;
  tags: Tag[];
  upvoteCount: number;
  downvoteCount: number;
  createdAt: string;
  updatedAt: string;
}

interface User {
  id: number;
  username: string;
  email: string;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  code?: string;
}

const ThreadDetail = () => {
  const { id } = useParams<{ id: string }>();
  const [thread, setThread] = useState<Thread | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [userVote, setUserVote] = useState<'up' | 'down' | null>(null);
  const navigate = useNavigate();

  const handleApiError = async (response: Response) => {
    const errorData: ApiResponse<any> = await response.json();
    if (response.status === 401 && errorData.code === 'TOKEN_EXPIRED') {
      localStorage.removeItem('token');
      navigate('/auth');
      return true;
    }
    setError(errorData.message || 'An error occurred');
    return false;
  };

  useEffect(() => {
    const fetchThread = async () => {
      try {
        const token = localStorage.getItem('token');
        if (!token) {
          navigate('/auth');
          return;
        }

        const response = await fetch(API_ENDPOINTS.threads.get(Number(id)), {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Accept': 'application/json'
          }
        });

        if (!response.ok) {
          const shouldRedirect = await handleApiError(response);
          if (shouldRedirect) return;
        }

        const { data } = await response.json();
        setThread(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'An error occurred');
      } finally {
        setLoading(false);
      }
    };

    fetchThread();
  }, [id, navigate]);

  const handleVote = async (isUpvote: boolean) => {
    if (!thread) return;

    try {
      const token = localStorage.getItem('token');
      if (!token) {
        navigate('/auth');
        return;
      }

      const response = await fetch(API_ENDPOINTS.threads.vote(thread.id), {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ isUpvote })
      });

      if (!response.ok) {
        const shouldRedirect = await handleApiError(response);
        if (shouldRedirect) return;
      }

      const { data } = await response.json();
      setThread(data);
      setUserVote(isUpvote ? 'up' : 'down');
    } catch (err) {
      console.error('Error voting:', err);
      setError('Failed to vote. Please try again.');
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getContrastColor = (hexColor: string): string => {
    const color = hexColor.replace('#', '');
    const r = parseInt(color.substr(0, 2), 16);
    const g = parseInt(color.substr(2, 2), 16);
    const b = parseInt(color.substr(4, 2), 16);
    
    const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
    return luminance > 0.5 ? '#000000' : '#FFFFFF';
  };

  const renderTag = (tag: Tag) => {
    const textColor = tag.colorCodeString ? getContrastColor(tag.colorCodeString) : 'text-gray-700';
    return (
      <span
        key={tag.id}
        className={`px-3 py-1 rounded-full text-sm border border-gray-200`}
        style={{
          backgroundColor: tag.colorCodeString || '#E5E7EB',
          color: textColor
        }}
        title={tag.description || tag.label}
      >
        {tag.label}
      </span>
    );
  };

  const renderContent = (user: User) => {
    if (loading) {
      return (
        <div className="flex justify-center items-center h-[calc(100vh-80px)]">
          <p className="text-gray-600">Loading...</p>
        </div>
      );
    }

    if (error) {
      return (
        <div className="bg-red-50 text-red-600 p-4 rounded-lg">
          {error}
        </div>
      );
    }

    if (!thread) {
      return null;
    }

    return (
      <div className="bg-white rounded-xl shadow-sm p-8">
        <h1 className="text-3xl font-semibold text-gray-900 mb-4">
          {thread.title}
        </h1>

        <div className="flex items-center gap-4 text-sm text-gray-500 mb-6">
          <span>Posted {formatDate(thread.createdAt)}</span>
          {thread.updatedAt !== thread.createdAt && (
            <span>(Edited {formatDate(thread.updatedAt)})</span>
          )}
        </div>

        {thread.description && (
          <div className="prose max-w-none mb-8">
            {thread.description}
          </div>
        )}

        {thread.tags.length > 0 && (
          <div className="flex flex-wrap gap-2 mb-8">
            {thread.tags.map(renderTag)}
          </div>
        )}

        <div className="flex items-center gap-6">
          <div className="flex items-center gap-2">
            <button
              onClick={() => handleVote(true)}
              className={`p-2 rounded-full hover:bg-gray-100 transition-colors ${
                userVote === 'up' ? 'text-blue-600' : 'text-gray-600'
              }`}
            >
              {userVote === 'up' ? <FaThumbsUp size={20} /> : <FaRegThumbsUp size={20} />}
            </button>
            <span className="text-gray-600 font-medium">{thread.upvoteCount}</span>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => handleVote(false)}
              className={`p-2 rounded-full hover:bg-gray-100 transition-colors ${
                userVote === 'down' ? 'text-red-600' : 'text-gray-600'
              }`}
            >
              {userVote === 'down' ? <FaThumbsDown size={20} /> : <FaRegThumbsDown size={20} />}
            </button>
            <span className="text-gray-600 font-medium">{thread.downvoteCount}</span>
          </div>
        </div>
      </div>
    );
  };

  return <MainLayout>{renderContent}</MainLayout>;
};

export default ThreadDetail; 