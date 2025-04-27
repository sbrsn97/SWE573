import { useState, useEffect } from 'react';
import { API_ENDPOINTS } from '../../config/config';
import { Tag } from './TagSelector';
import { HexColorPicker } from 'react-colorful';
import TagComponent from './Tag';

interface WikidataEntity {
  id: string;
  label: string;
  description: string;
}

interface CreateTagModalProps {
  onClose: () => void;
  onTagCreated: (tag: Tag) => void;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  code?: string;
}

const CreateTagModal = ({ onClose, onTagCreated }: CreateTagModalProps) => {
  const [label, setLabel] = useState('');
  const [description, setDescription] = useState('');
  const [colorCodeString, setColorCodeString] = useState('#3B82F6'); // Default blue color
  const [showColorPicker, setShowColorPicker] = useState(false);
  const [wikidataSearch, setWikidataSearch] = useState('');
  const [wikidataResults, setWikidataResults] = useState<WikidataEntity[]>([]);
  const [selectedEntity, setSelectedEntity] = useState<WikidataEntity | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const searchWikidata = async () => {
      if (!wikidataSearch.trim()) {
        setWikidataResults([]);
        return;
      }

      setIsLoading(true);
      try {
        const token = localStorage.getItem('token');
        if (!token) throw new Error('No authentication token found');

        const response = await fetch(`${API_ENDPOINTS.wikidata.searchEntities}?query=${encodeURIComponent(wikidataSearch)}`, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });

        if (!response.ok) {
          throw new Error('Failed to search Wikidata');
        }

        const data: ApiResponse<WikidataEntity[]> = await response.json();
        if (data.success) {
          setWikidataResults(data.data);
        } else {
          throw new Error(data.message);
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to search Wikidata');
        setWikidataResults([]);
      } finally {
        setIsLoading(false);
      }
    };

    const debounceTimer = setTimeout(searchWikidata, 300);
    return () => clearTimeout(debounceTimer);
  }, [wikidataSearch]);

  const handleSubmit = async (e?: React.MouseEvent) => {
    // If event is provided, prevent default behavior
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    
    // Validate inputs manually
    if (!label.trim()) {
      setError('Label is required');
      return;
    }

    if (colorCodeString && !/^#[0-9A-Fa-f]{6}$/.test(colorCodeString)) {
      setError('Please enter a valid hex color code (e.g., #FF0000)');
      return;
    }
    
    setIsLoading(true);
    setError(null);

    try {
      const token = localStorage.getItem('token');
      if (!token) throw new Error('No authentication token found');

      // Create the tag payload
      const tagPayload = {
        label: label.trim(),
        description: description.trim() || null,
        colorCodeString: colorCodeString,
        wikidataEntityId: selectedEntity?.id || 'Q35120' // Use "entity" (Q35120) as default if no entity selected
      };

      console.log('Sending tag creation request:', tagPayload);

      // Log the request details
      console.log('Making API request to:', API_ENDPOINTS.tags.create);
      console.log('Request method:', 'POST');
      console.log('Request headers:', {
        'Authorization': 'Bearer [TOKEN]', // Not showing actual token for security
        'Content-Type': 'application/json'
      });
      
      const response = await fetch(API_ENDPOINTS.tags.create, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(tagPayload)
      });

      console.log('API response status:', response.status, response.statusText);
      const responseData = await response.json();
      console.log('Tag creation response data:', responseData);

      if (!response.ok) {
        console.error('API error:', responseData);
        if (responseData.message) {
          throw new Error(`API Error: ${responseData.message}`);
        } else {
          throw new Error(`HTTP Error: ${response.status} ${response.statusText}`);
        }
      }

      if (!responseData.success) {
        console.error('API returned success=false:', responseData);
        throw new Error(responseData.message || 'Failed to create tag');
      }

      onTagCreated(responseData.data);
      onClose();
    } catch (err) {
      console.error('Error creating tag:', err);
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Failed to create tag: Unknown error');
      }
    } finally {
      setIsLoading(false);
    }
  };

  // Handle cancel button click
  const handleCancel = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    onClose();
  };

  // Create a preview tag object
  const previewTag: Tag = {
    id: 0,
    label: label || 'Tag Preview',
    description: description || 'Tag description will appear here',
    colorCodeString: colorCodeString,
    wikidataEntityId: selectedEntity?.id || ''
  };

  return (
    <div className="fixed inset-0 backdrop-blur-[2px] bg-white/30 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-xl shadow-lg p-6 w-full max-w-md" onClick={(e) => e.stopPropagation()}>
        <h2 className="text-2xl font-semibold mb-4">Create New Tag</h2>
        
        {error && (
          <div className="bg-red-50 text-red-600 p-4 rounded-lg mb-4 whitespace-pre-wrap">
            {error}
          </div>
        )}

        {/* Use onSubmit on a div instead of a form to avoid potential browser form behaviors */}
        <div>
          <div className="mb-4">
            <label htmlFor="label" className="block text-sm font-medium text-gray-700 mb-1">
              Label
            </label>
            <input
              type="text"
              id="label"
              value={label}
              onChange={(e) => setLabel(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>

          <div className="mb-4">
            <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
              Description
            </label>
            <input
              type="text"
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="mb-4">
            <label htmlFor="color" className="block text-sm font-medium text-gray-700 mb-1">
              Color
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                id="color"
                value={colorCodeString}
                onChange={(e) => setColorCodeString(e.target.value)}
                className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                pattern="^#[0-9A-Fa-f]{6}$"
                title="Please enter a valid hex color code (e.g., #FF0000)"
                required
              />
              <button
                type="button"
                onClick={(e) => {
                  e.preventDefault();
                  setShowColorPicker(!showColorPicker);
                }}
                className="px-3 py-2 bg-gray-100 rounded-lg hover:bg-gray-200"
                style={{ backgroundColor: colorCodeString }}
              >
                &nbsp;&nbsp;&nbsp;
              </button>
            </div>
            {showColorPicker && (
              <div className="absolute mt-2 bg-white p-2 rounded-lg shadow-lg z-50">
                <HexColorPicker color={colorCodeString} onChange={setColorCodeString} />
              </div>
            )}
          </div>

          {/* Tag Preview */}
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Preview
            </label>
            <div className="p-3 bg-gray-50 rounded-lg flex justify-center">
              <TagComponent tag={previewTag} />
            </div>
          </div>

          <div className="mb-6">
            <label htmlFor="wikidata" className="block text-sm font-medium text-gray-700 mb-1">
              Wikidata Entity (optional)
            </label>
            <input
              type="text"
              id="wikidata"
              value={wikidataSearch}
              onChange={(e) => setWikidataSearch(e.target.value)}
              placeholder="Search Wikidata entities..."
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {wikidataSearch && !isLoading && (
              <div className="absolute z-50 w-full mt-1 bg-white border border-gray-300 rounded-lg shadow-lg max-h-60 overflow-auto">
                {wikidataResults.map(entity => (
                  <div
                    key={entity.id}
                    className="px-4 py-2 hover:bg-gray-100 cursor-pointer"
                    onClick={(e) => {
                      e.preventDefault();
                      setSelectedEntity(entity);
                      setWikidataSearch('');
                    }}
                  >
                    <div className="font-medium">{entity.label}</div>
                    {entity.description && (
                      <div className="text-sm text-gray-500">{entity.description}</div>
                    )}
                  </div>
                ))}
                {wikidataResults.length === 0 && (
                  <div className="px-4 py-2 text-gray-500">
                    No matching entities found
                  </div>
                )}
              </div>
            )}
            {selectedEntity && (
              <div className="mt-2 p-2 bg-gray-50 rounded-lg">
                <div className="font-medium">{selectedEntity.label}</div>
                <div className="text-sm text-gray-500">{selectedEntity.description}</div>
                <button
                  type="button"
                  onClick={(e) => {
                    e.preventDefault();
                    setSelectedEntity(null);
                  }}
                  className="text-xs text-red-600 hover:text-red-700 mt-1"
                >
                  Remove
                </button>
              </div>
            )}
          </div>

          <div className="flex justify-end gap-3">
            <button
              type="button"
              onClick={handleCancel}
              className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-gray-500"
              disabled={isLoading}
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={handleSubmit}
              className="px-4 py-2 text-white bg-blue-600 rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
              disabled={isLoading}
            >
              {isLoading ? 'Creating...' : 'Create Tag'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CreateTagModal; 