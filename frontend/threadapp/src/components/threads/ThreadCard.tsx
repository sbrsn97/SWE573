import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { FaThumbsUp, FaRegClock, FaUserPlus, FaTag, FaUser } from 'react-icons/fa';
import Tag from '../tags/Tag';
import { API_ENDPOINTS } from '../../config/config';
import { fetchWithAuth } from '../../utils/authUtils';

interface Author {
  id: number;
  firstName: string;
  lastName: string;
  username: string;
}

interface ThreadCardProps {
  thread: {
    id: number;
    title: string;
    description: string | null;
    authorId: number;
    upvoteCount: number;
    downvoteCount: number;
    createdAt: string;
    active: boolean;
    deactivatedByRole: string | null;
    tags: Array<{
      id: number;
      label: string;
      description: string;
      colorCodeString: string;
      wikidataEntityId: string;
    }>;
  };
}

const ThreadCard: React.FC<ThreadCardProps> = ({ thread }) => {
  const [author, setAuthor] = useState<Author | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Fetch author details
  useEffect(() => {
    const fetchAuthor = async () => {
      if (!thread.authorId) return;
      
      setLoading(true);
      try {
        const response = await fetchWithAuth(`${API_ENDPOINTS.users.all}/${thread.authorId}`);
        
        if (response.ok) {
          const result = await response.json();
          if (result && result.data) {
            setAuthor({
              id: result.data.id,
              firstName: result.data.firstName,
              lastName: result.data.lastName,
              username: result.data.username
            });
          }
        } else {
          setError('Failed to fetch author');
        }
      } catch (err) {
        console.error('Error fetching author:', err);
        setError('Error loading author data');
      } finally {
        setLoading(false);
      }
    };
    
    fetchAuthor();
  }, [thread.authorId]);

  // Format date to a readable string
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  // Truncate description to a specific length
  const truncateDescription = (text: string | null, maxLength: number = 150) => {
    if (!text) return '';
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
  };

  return (
    <div className={`bg-white rounded-lg shadow-sm hover:shadow-md transition-shadow p-4 ${!thread.active ? 'border border-red-200 bg-red-50' : ''}`}>
      <Link to={`/threads/${thread.id}`} className="block">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-semibold text-blue-800 hover:text-blue-600 mb-2 transition-colors">
            {thread.title}
          </h3>
          {!thread.active && (
            <span className="bg-red-100 text-red-800 text-xs px-2 py-1 rounded-full flex items-center">
              <span>Inactive</span>
              {thread.deactivatedByRole && (
                <span className="ml-1 text-xs opacity-75">
                  (by {thread.deactivatedByRole.toLowerCase()})
                </span>
              )}
            </span>
          )}
        </div>
      </Link>
      
      {thread.description && (
        <p className="text-gray-600 text-sm mb-3">
          {truncateDescription(thread.description)}
        </p>
      )}
      
      <div className="flex flex-wrap gap-2 mb-2">
        {thread.tags.slice(0, 3).map(tag => (
          <Tag key={tag.id} tag={tag} />
        ))}
        {thread.tags.length > 3 && (
          <span className="inline-flex items-center bg-gray-100 text-gray-600 text-xs px-2 py-1 rounded">
            <FaTag className="mr-1" size={10} />
            +{thread.tags.length - 3} more
          </span>
        )}
      </div>
      
      <div className="flex items-center justify-between text-xs text-gray-500 mt-2">
        <div className="flex items-center gap-2">
          <span className="flex items-center">
            <FaThumbsUp className="mr-1" />
            {thread.upvoteCount - thread.downvoteCount}
          </span>
          <span className="flex items-center">
            <FaRegClock className="mr-1" />
            {formatDate(thread.createdAt)}
          </span>
          {author ? (
            <Link to={`/users/${author.id}`} className="flex items-center text-gray-500 hover:text-gray-700">
              <FaUser className="mr-1 text-xs" />
              @{author.username}
            </Link>
          ) : loading ? (
            <span className="flex items-center text-gray-400">
              <FaUser className="mr-1 text-xs" />
              Loading...
            </span>
          ) : (
            <span className="flex items-center text-gray-400">
              <FaUser className="mr-1 text-xs" />
              Unknown
            </span>
          )}
        </div>
        <Link 
          to={`/threads/${thread.id}`} 
          className="text-blue-600 hover:text-blue-800 font-medium"
        >
          Read more →
        </Link>
      </div>
    </div>
  );
};

export default ThreadCard; 