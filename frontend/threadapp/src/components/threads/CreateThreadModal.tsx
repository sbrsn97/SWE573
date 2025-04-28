import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { API_ENDPOINTS } from '../../config/config';
import { Tag } from '../tags/TagSelector';
import TagSelector from '../tags/TagSelector';

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

interface CreateThreadModalProps {
  isOpen: boolean;
  onClose: () => void;
  onThreadCreated: (threadId: number) => void;
}

const CreateThreadModal = ({ isOpen, onClose, onThreadCreated }: CreateThreadModalProps) => {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [selectedTags, setSelectedTags] = useState<Tag[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const navigate = useNavigate();

  // Reset form state when modal is closed
  useEffect(() => {
    if (!isOpen) {
      setTitle('');
      setDescription('');
      setSelectedTags([]);
      setError(null);
    }
  }, [isOpen]);

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
    const fetchUser = async () => {
      if (!isOpen) return;

      try {
        const token = localStorage.getItem('token');
        if (!token) {
          navigate('/auth');
          return;
        }

        const response = await fetch(API_ENDPOINTS.users.me, {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Accept': 'application/json'
          }
        });

        if (!response.ok) {
          try {
            const errorData = await response.json();
            console.error('Error fetching user:', errorData);
            if (response.status === 401 && errorData.code === 'TOKEN_EXPIRED') {
              localStorage.removeItem('token');
              navigate('/auth');
              return;
            }
            setError(errorData.message || 'An error occurred');
          } catch (jsonError) {
            setError(`Error: ${response.status} ${response.statusText}`);
          }
          return;
        }

        const { data } = await response.json();
        console.log('Fetched current user:', data);
        setUser(data);
      } catch (err) {
        console.error('Error fetching user:', err);
        setError(err instanceof Error ? err.message : 'An error occurred');
      }
    };

    fetchUser();
  }, [isOpen, navigate]);

  // Log selected tags whenever they change
  useEffect(() => {
    console.log('Selected tags updated:', selectedTags);
  }, [selectedTags]);
  
  const handleTagsChange = (newTags: Tag[]) => {
    console.log('Tags changed to:', newTags);
    setSelectedTags(newTags);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const token = localStorage.getItem('token');
      if (!token) {
        navigate('/auth');
        return;
      }

      if (!user || !user.id) {
        setError('User information not available. Please try again or log out and log back in.');
        setLoading(false);
        return;
      }

      const response = await fetch(API_ENDPOINTS.threads.create, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          title,
          description: description || null,
          authorId: user.id,
          tags: selectedTags
        })
      });

      console.log('Thread creation request with tags:', selectedTags);

      if (!response.ok) {
        try {
          const errorData = await response.json();
          console.error('Error response:', errorData);
          setError(errorData.message || `Error: ${response.status} ${response.statusText}`);
        } catch (jsonError) {
          console.error('Could not parse error response:', jsonError);
          setError(`Error: ${response.status} ${response.statusText}`);
        }
        setLoading(false);
        return;
      }

      try {
        const { data } = await response.json();
        console.log('Thread created successfully with ID:', data.id);
        onThreadCreated(data.id);
        onClose();
      } catch (jsonError) {
        console.error('Error parsing success response:', jsonError);
        setError('Error parsing server response');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 backdrop-blur-[2px] bg-white/30 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-xl shadow-lg p-6 w-full max-w-2xl">
        <h2 className="text-2xl font-semibold mb-4">Create New Thread</h2>
        
        {error && (
          <div className="bg-red-50 text-red-600 p-4 rounded-lg mb-4">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="mb-4">
            <label htmlFor="title" className="block text-sm font-medium text-gray-700 mb-1">
              Title
            </label>
            <input
              type="text"
              id="title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>

          <div className="mb-4">
            <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
              Description (optional)
            </label>
            <textarea
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              rows={4}
            />
          </div>

          <div className="mb-6">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Tags
            </label>
            <div className="text-xs text-gray-500 mb-2">
              {selectedTags.length > 0 
                ? `${selectedTags.length} tag${selectedTags.length > 1 ? 's' : ''} selected` 
                : 'No tags selected'}
            </div>
            <TagSelector
              key={`tag-selector-${isOpen}`}
              selectedTags={selectedTags}
              onTagsChange={handleTagsChange}
            />
          </div>

          <div className="flex justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-gray-500"
              disabled={loading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-white bg-blue-600 rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
              disabled={loading}
            >
              {loading ? 'Creating...' : 'Create Thread'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateThreadModal; 