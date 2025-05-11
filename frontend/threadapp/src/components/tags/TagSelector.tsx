import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { API_ENDPOINTS } from '../../config/config';
import CreateTagModal from './CreateTagModal';
import TagComponent from './Tag';
import { handleAuthError } from '../../utils/authUtils';

export interface Tag {
  id: number;
  label: string;
  description: string;
  colorCodeString: string;
  wikidataEntityId: string;
}

interface TagSelectorProps {
  selectedTags: Tag[];
  onTagsChange: (tags: Tag[]) => void;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  code?: string;
}

const TagSelector = ({ selectedTags, onTagsChange }: TagSelectorProps) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<Tag[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [allTags, setAllTags] = useState<Tag[]>([]);
  const selectorRef = useRef<HTMLDivElement>(null);
  const createButtonRef = useRef<HTMLButtonElement>(null);
  const [modalPosition, setModalPosition] = useState<{top?: number; left?: number; right?: number; bottom?: number} | undefined>(undefined);
  const navigate = useNavigate();

  // Handle clicks outside of component
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (selectorRef.current && !selectorRef.current.contains(event.target as Node)) {
        setIsDropdownOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  // Fetch all tags on component mount and when showCreateModal changes
  useEffect(() => {
    const fetchAllTags = async () => {
      setIsLoading(true);
      try {
        const token = localStorage.getItem('token');
        if (!token) {
          navigate('/auth');
          return;
        }

        const response = await fetch(API_ENDPOINTS.tags.getAll, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });

        if (!response.ok) {
          // Handle authentication errors
          if (handleAuthError(response, navigate)) {
            return;
          }
          throw new Error(`Failed to fetch tags: ${response.status} ${response.statusText}`);
        }

        const data: ApiResponse<Tag[]> = await response.json();
        
        if (data.success) {
          setAllTags(data.data);
          if (!searchQuery.trim()) {
            // Update search results with the newly fetched tags (filtered by selected)
            const filteredResults = data.data.filter(
              tag => !selectedTags.some(selected => selected.id === tag.id)
            );
            setSearchResults(filteredResults);
          }
        } else {
          throw new Error(data.message);
        }
      } catch (err) {
        console.error('Error fetching tags:', err);
        setError(err instanceof Error ? err.message : 'Failed to fetch tags');
      } finally {
        setIsLoading(false);
      }
    };

    fetchAllTags();
  }, [showCreateModal, selectedTags, navigate]);

  // Search tags when query changes
  useEffect(() => {
    const searchTags = async () => {
      if (!searchQuery.trim()) {
        // When search is empty, filter from all tags
        const filteredResults = allTags.filter(
          tag => !selectedTags.some(selected => selected.id === tag.id)
        );
        setSearchResults(filteredResults);
        return;
      }

      setIsLoading(true);
      try {
        const token = localStorage.getItem('token');
        if (!token) {
          navigate('/auth');
          return;
        }

        const response = await fetch(`${API_ENDPOINTS.tags.search}?keyword=${encodeURIComponent(searchQuery)}`, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });

        if (!response.ok) {
          // Handle authentication errors
          if (handleAuthError(response, navigate)) {
            return;
          }
          throw new Error(`Failed to search tags: ${response.status} ${response.statusText}`);
        }

        const data: ApiResponse<Tag[]> = await response.json();
        
        if (data.success) {
          // Filter out already selected tags
          const filteredResults = data.data.filter(
            tag => !selectedTags.some(selected => selected.id === tag.id)
          );
          setSearchResults(filteredResults);
        } else {
          throw new Error(data.message);
        }
      } catch (err) {
        console.error('Error searching tags:', err);
        setError(err instanceof Error ? err.message : 'Failed to search tags');
        setSearchResults([]);
      } finally {
        setIsLoading(false);
      }
    };

    const debounceTimer = setTimeout(searchTags, 300);
    return () => clearTimeout(debounceTimer);
  }, [searchQuery, selectedTags, allTags, navigate]);

  const handleTagSelect = (tag: Tag) => {
    onTagsChange([...selectedTags, tag]);
    setSearchQuery('');
    setIsDropdownOpen(false);
  };

  const handleTagRemove = (tag: Tag) => {
    onTagsChange(selectedTags.filter(t => t.id !== tag.id));
  };

  const handleTagCreated = (newTag: Tag) => {
    // First update allTags to include the new tag
    setAllTags(prev => {
      // Check if the tag already exists to avoid duplicates
      if (prev.some(tag => tag.id === newTag.id)) {
        return prev;
      }
      return [...prev, newTag];
    });
    
    // Then add to selected tags
    onTagsChange([...selectedTags, newTag]);
    
    // Reset search state
    setSearchQuery('');
    setIsDropdownOpen(false);
    
    // Close the modal
    setShowCreateModal(false);
  };

  const handleCreateTag = (e?: React.MouseEvent) => {
    // Prevent default behavior if event is provided
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    
    // Check if user is authenticated
    if (!localStorage.getItem('token')) {
      navigate('/auth');
      return;
    }
    
    // Position modal at the center of the screen
    setModalPosition(undefined);
    setShowCreateModal(true);
  };

  const handleFocus = () => {
    setIsDropdownOpen(true);
    // If empty query, show all available tags
    if (!searchQuery.trim()) {
      const filteredResults = allTags.filter(
        tag => !selectedTags.some(selected => selected.id === tag.id)
      );
      setSearchResults(filteredResults);
    }
  };

  return (
    <div className="relative w-full" ref={selectorRef}>
      <div className="flex flex-wrap gap-2 mb-2">
        {selectedTags.map((tag) => (
          <TagComponent
            key={tag.id}
            tag={tag}
            onRemove={() => handleTagRemove(tag)}
          />
        ))}
      </div>
      
      <div className="flex gap-2">
        <div className="relative flex-1">
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              setIsDropdownOpen(true);
            }}
            onFocus={handleFocus}
            placeholder="Search for tags..."
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          
          {isDropdownOpen && (
            <div className="absolute z-10 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-60 overflow-y-auto">
              {isLoading ? (
                <div className="p-3 text-gray-500 text-center">Loading...</div>
              ) : error ? (
                <div className="p-3 text-red-500">{error}</div>
              ) : searchResults.length > 0 ? (
                <ul className="py-1">
                  {searchResults.map((tag) => (
                    <li
                      key={tag.id}
                      onClick={() => handleTagSelect(tag)}
                      className="px-4 py-2 hover:bg-gray-100 cursor-pointer flex items-center"
                    >
                      <TagComponent tag={tag} className="cursor-pointer" />
                    </li>
                  ))}
                </ul>
              ) : (
                searchQuery.trim() ? (
                  <div
                    onClick={(e) => {
                      e.preventDefault();
                      e.stopPropagation();
                      handleCreateTag();
                    }}
                    className="px-4 py-3 hover:bg-gray-100 cursor-pointer text-blue-600"
                  >
                    Create tag: "{searchQuery}"
                  </div>
                ) : (
                  <div className="p-3 text-gray-500 text-center">No tags available</div>
                )
              )}
            </div>
          )}
        </div>
        
        <button
          ref={createButtonRef}
          type="button"
          onClick={(e) => handleCreateTag(e)}
          className="px-4 py-2 bg-blue-100 text-blue-700 rounded-lg hover:bg-blue-200 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          + New Tag
        </button>
      </div>

      {showCreateModal && (
        <CreateTagModal
          onClose={() => setShowCreateModal(false)}
          onTagCreated={handleTagCreated}
          position={modalPosition}
        />
      )}
    </div>
  );
};

export default TagSelector; 