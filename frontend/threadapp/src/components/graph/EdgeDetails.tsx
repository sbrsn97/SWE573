import React, { useState, useRef, useEffect, useCallback } from 'react';
import { FaTimes, FaTrash } from 'react-icons/fa';
import { API_ENDPOINTS, API_BASE_URL } from '../../config/config';
import { fetchWithAuth } from '../../utils/authUtils';
import { HexColorPicker } from 'react-colorful';

// Static initial properties data
const initialProperties = [
  { id: 'P31', label: 'instance of', description: 'that class of which this subject is a particular example and member' },
  { id: 'P279', label: 'subclass of', description: 'all instances of these items are instances of those items; this item is a class/type/kind/group of those items; this item is narrower than that item' },
  { id: 'P361', label: 'part of', description: 'object of which the subject is a part (if this subject is already part of object A which is a part of object B, then do not also state that the subject is a part of object B)' },
  { id: 'P527', label: 'has part', description: 'part of this subject; inverse property of "part of" (P361)' },
  { id: 'P1269', label: 'facet of', description: 'aspect, appearance, or different view of a related concept' },
  { id: 'P2283', label: 'uses', description: 'the object or resource that is or was used by the subject' },
  { id: 'P2670', label: 'has parts of the class', description: 'subject has parts that are instances of this class' },
  { id: 'P921', label: 'main subject', description: 'primary topic of a work (use P180 for works of art)' },
  { id: 'P4733', label: 'related category', description: 'a category related to this topic, but not a parent category or a subcategory' },
  { id: 'P1431', label: 'executive producer', description: 'producer who is not involved in technical aspects but oversees financial, legal and administrative aspects' },
  { id: 'P175', label: 'performer', description: 'performer involved in the performance or execution of this item or event' },
  { id: 'P162', label: 'producer', description: 'producer(s) of this film or music work' },
  { id: 'P272', label: 'production company', description: 'companies that produced the film, audio or performing arts work' },
  { id: 'P495', label: 'country of origin', description: 'country of origin of the creative work or subject item' },
  { id: 'P577', label: 'publication date', description: 'date or point in time when the work was first published or released' },
  { id: 'P1433', label: 'published in', description: 'published in this periodical, series or other type of journal' },
  { id: 'P452', label: 'industry', description: 'industry of company or organization' },
  { id: 'P112', label: 'founded by', description: 'founder or co-founder of this organization, religion or place' },
  { id: 'P571', label: 'inception', description: 'time when an entity begins to exist' },
  { id: 'P6375', label: 'located at street address', description: 'street address, for items that have a street address' },
  { id: 'P276', label: 'location', description: 'location where the item is or has been located' },
  { id: 'P131', label: 'located in the administrative territorial entity', description: 'the item is located on the territory of the following administrative entity' }
];

interface WikidataProperty {
  id: string;
  label: string;
  description?: string;
}

interface GraphEdge {
  id: number;
  sourceNodeId: number;
  targetNodeId: number;
  label: string;
  type: string;
  weight: number;
  color: string;
  wikidataPropertyId?: string;
  threadId: number;
}

interface EdgeDetailsProps {
  edge: GraphEdge;
  onClose: () => void;
  onUpdate: () => void;
  onDelete?: (edgeId: number) => void;
}

const EdgeDetails: React.FC<EdgeDetailsProps> = ({ edge, onClose, onUpdate, onDelete }) => {
  const [label, setLabel] = useState(edge.label || '');
  const [type, setType] = useState(edge.type || 'default');
  const [weight, setWeight] = useState(edge.weight || 1);
  const [color, setColor] = useState(edge.color || '#555555');
  const [wikidataPropertyId, setWikidataPropertyId] = useState(edge.wikidataPropertyId || '');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  
  // Wikidata property search state
  const [propertySearch, setPropertySearch] = useState(edge.label || '');
  const [propertyResults, setPropertyResults] = useState<WikidataProperty[]>([]);
  const [selectedProperty, setSelectedProperty] = useState<WikidataProperty | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);
  const searchResultsRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);
  const formRef = useRef<HTMLFormElement>(null);
  
  const [dropdownPosition, setDropdownPosition] = useState({
    top: 0,
    left: 0,
    width: 0
  });

  // Add state for pagination
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [hasMoreResults, setHasMoreResults] = useState(true);
  const [isFetchingMore, setIsFetchingMore] = useState(false);

  // Add a ref for the dropdown scroll container
  const dropdownContainerRef = useRef<HTMLDivElement>(null);

  // Update dropdown position based on input element
  const updateDropdownPosition = useCallback(() => {
    if (searchInputRef.current) {
      const rect = searchInputRef.current.getBoundingClientRect();
      setDropdownPosition({
        top: rect.bottom + window.scrollY,
        left: rect.left,
        width: searchInputRef.current.offsetWidth
      });
    }
  }, []);

  // Add window event listeners
  useEffect(() => {
    if (showDropdown) {
      updateDropdownPosition();
      
      // Update position on scroll or resize
      window.addEventListener('scroll', updateDropdownPosition);
      window.addEventListener('resize', updateDropdownPosition);
      
      return () => {
        window.removeEventListener('scroll', updateDropdownPosition);
        window.removeEventListener('resize', updateDropdownPosition);
      };
    }
  }, [showDropdown, updateDropdownPosition]);

  // Update position when input changes
  useEffect(() => {
    if (searchInputRef.current) {
      updateDropdownPosition();
    }
  }, [propertySearch, updateDropdownPosition]);

  // Initialize propertySearch and check for matching property
  useEffect(() => {
    if (edge.wikidataPropertyId) {
      // If the edge already has a wikidataPropertyId, find the matching property
      const matchedProperty = initialProperties.find(
        prop => prop.id === edge.wikidataPropertyId
      );
      
      if (matchedProperty) {
        setSelectedProperty(matchedProperty);
        setPropertySearch(matchedProperty.label);
        setLabel(matchedProperty.label);
        setWikidataPropertyId(matchedProperty.id);
      } else {
        // If we can't find a match locally, but have a wikidataPropertyId,
        // we'll use the label and id as they are
        setWikidataPropertyId(edge.wikidataPropertyId);
        
        // If label is empty but we have a wikidataPropertyId, we'll try to fetch it
        if (!edge.label && edge.wikidataPropertyId) {
          // Try to fetch the property details by ID
          fetchPropertyById(edge.wikidataPropertyId);
        }
      }
    } else if (edge.label) {
      // Otherwise, try to match by label
      const matchedProperty = initialProperties.find(
        prop => prop.label.toLowerCase() === edge.label.toLowerCase()
      );
      
      if (matchedProperty) {
        setSelectedProperty(matchedProperty);
        setWikidataPropertyId(matchedProperty.id);
      }
    }
  }, [edge.label, edge.wikidataPropertyId]);

  // Function to fetch property details by ID
  const fetchPropertyById = async (propertyId: string) => {
    try {
      const response = await fetchWithAuth(
        API_ENDPOINTS.wikidata.properties.getById(propertyId)
      );
      
      if (!response.ok) {
        throw new Error(`API request failed with status ${response.status}`);
      }
      
      const responseData = await response.json();
      
      if (!responseData.success) {
        throw new Error(responseData.message || 'Failed to fetch property details');
      }
      
      const propertyData = responseData.data;
      if (propertyData && propertyData.label) {
        // We found the property, update state
        const property: WikidataProperty = {
          id: propertyId,
          label: propertyData.label,
          description: propertyData.description || ''
        };
        
        setSelectedProperty(property);
        setPropertySearch(property.label);
        setLabel(property.label);
      }
    } catch (error) {
      // Error handling silently fails
    }
  };

  // Add click outside listener to close dropdown
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      // If click is outside both the input and dropdown, close the dropdown
      if (!(searchInputRef.current?.contains(event.target as Node) || 
            searchResultsRef.current?.contains(event.target as Node))) {
        setShowDropdown(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  // When the component mounts, add escape key handler to close dropdown
  useEffect(() => {
    const handleEscapeKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setShowDropdown(false);
      }
    };

    document.addEventListener('keydown', handleEscapeKey);
    return () => {
      document.removeEventListener('keydown', handleEscapeKey);
    };
  }, []);

  // Reset pagination when search query changes
  useEffect(() => {
    setPage(0);
    setHasMoreResults(true);
    setPropertyResults([]);
  }, [propertySearch]);

  // Add dropdown scroll handler for pagination
  useEffect(() => {
    const handleScroll = () => {
      const container = dropdownContainerRef.current;
      if (!container || !hasMoreResults || isFetchingMore) return;
      
      const { scrollTop, scrollHeight, clientHeight } = container;
      // When user scrolls to bottom (with a small threshold), load more
      if (scrollHeight - scrollTop - clientHeight < 50) {
        fetchMoreProperties();
      }
    };

    const dropdown = dropdownContainerRef.current;
    if (dropdown && showDropdown) {
      dropdown.addEventListener('scroll', handleScroll);
      return () => dropdown.removeEventListener('scroll', handleScroll);
    }
  }, [showDropdown, hasMoreResults, isFetchingMore, page, propertySearch]);

  // Effect to search for wikidata properties when the search query changes
  useEffect(() => {
    // Clear the debounce timer
    const debounceTimer = setTimeout(() => {
      if (!propertySearch.trim()) {
        // If search is empty, show initial properties
        fetchInitialProperties();
      } else {
        // If there's a search term, fetch from API
        searchProperties();
      }
    }, 300);
    
    return () => {
      clearTimeout(debounceTimer);
    };
  }, [propertySearch]);

  const fetchInitialProperties = async () => {
    setIsLoading(true);
    setPage(0);
    
    // Use static data for initial properties
    setPropertyResults(initialProperties.slice(0, size));
    setHasMoreResults(initialProperties.length > size);
    setShowDropdown(true);
    
    setIsLoading(false);
  };

  const fetchMoreProperties = async () => {
    if (!hasMoreResults || isFetchingMore) return;
    
    try {
      setIsFetchingMore(true);
      const nextPage = page + 1;
      
      if (propertySearch.trim() === '') {
        // For initial properties (static data), just slice more
        const startIdx = nextPage * size;
        const newProperties = initialProperties.slice(startIdx, startIdx + size);
        
        if (newProperties.length > 0) {
          setPropertyResults(prev => [...prev, ...newProperties]);
          setPage(nextPage);
          setHasMoreResults(startIdx + size < initialProperties.length);
        } else {
          setHasMoreResults(false);
        }
      } else {
        // For search query, call the API with the next page
        const apiResponse = await fetchPropertiesFromAPI(propertySearch, nextPage);
        
        if (apiResponse.data.length > 0) {
          setPropertyResults(prev => [...prev, ...apiResponse.data]);
          setPage(nextPage);
          setHasMoreResults(apiResponse.data.length === size && apiResponse.totalPages > nextPage + 1);
        } else {
          setHasMoreResults(false);
        }
      }
    } catch (error) {
      // Error handling silently fails
    } finally {
      setIsFetchingMore(false);
    }
  };

  const fetchPropertiesFromAPI = async (query: string, pageNum: number = 0): Promise<{data: WikidataProperty[], totalPages: number}> => {
    try {
      const response = await fetchWithAuth(
        API_ENDPOINTS.wikidata.properties.search(query, pageNum, size)
      );
      
      if (!response.ok) {
        throw new Error(`API request failed with status ${response.status}`);
      }
      
      // Get the raw response text
      const responseText = await response.text();
      
      // Extract properties from the raw response
      let properties: WikidataProperty[] = [];
      let totalPages = 1;
      
      try {
        // Parse the response
        let parsedResponse;
        try {
          parsedResponse = JSON.parse(responseText);
        } catch (e) {
          return { data: [], totalPages: 0 };
        }
        
        if (parsedResponse && typeof parsedResponse === 'object') {
          if (parsedResponse.items && Array.isArray(parsedResponse.items)) {
            // Direct format: { items: [...], ... }
            properties = parsedResponse.items;
            totalPages = parsedResponse.totalPages || 1;
          } else if (parsedResponse.data && parsedResponse.data.items && Array.isArray(parsedResponse.data.items)) {
            // Nested format: { data: { items: [...], ... } }
            properties = parsedResponse.data.items;
            totalPages = parsedResponse.data.totalPages || 1;
          } else if (Array.isArray(parsedResponse)) {
            // Direct array format
            properties = parsedResponse;
          } else {
            // Try manual regex extraction as a last resort if the response contains property data
            if (responseText.includes('"id"') && responseText.includes('"label"')) {
              const matches = responseText.match(/"items":\s*\[(.*?)\]/s);
              if (matches && matches[1]) {
                try {
                  // Try to parse the extracted items array
                  const itemsJson = `[${matches[1]}]`;
                  const extractedItems = JSON.parse(itemsJson);
                  if (Array.isArray(extractedItems) && extractedItems.length > 0) {
                    properties = extractedItems;
                  }
                } catch (e) {
                  // Extraction failed silently
                }
              }
            }
          }
        }
      } catch (error) {
        // Error processing properties
      }
      
      // Convert and validate properties
      const validProperties = properties.map((item: any) => ({
        id: item.id || '',
        label: item.label || item.id || '',
        description: item.description || ''
      })).filter(p => p.id && p.label);
      
      return {
        data: validProperties,
        totalPages: totalPages
      };
    } catch (error) {
      return { data: [], totalPages: 0 };
    }
  };

  const searchProperties = async () => {
    if (!propertySearch.trim()) {
      fetchInitialProperties();
      return;
    }
    
    try {
      setIsLoading(true);
      setPage(0);
      
      // Always send API request for any non-empty search
      const apiResponse = await fetchPropertiesFromAPI(propertySearch);
      
      // Set properties and ensure dropdown is visible 
      setShowDropdown(true);
      
      if (apiResponse.data.length > 0) {
        // We have results from the API - use them
        setPropertyResults(apiResponse.data);
        setHasMoreResults(apiResponse.data.length === size && apiResponse.totalPages > 1);
      } else {
        // If no results from API, try a more lenient local search as fallback
        const filteredProperties = initialProperties.filter(
          prop => prop.label.toLowerCase().includes(propertySearch.toLowerCase()) || 
                  prop.id.toLowerCase().includes(propertySearch.toLowerCase())
        );
        
        if (filteredProperties.length > 0) {
          setPropertyResults(filteredProperties.slice(0, size));
          setHasMoreResults(filteredProperties.length > size);
        } else {
          // No results at all
          setPropertyResults([]);
          setHasMoreResults(false);
        }
      }
    } catch (err) {
      // On error, show no results
      setPropertyResults([]);
      setHasMoreResults(false);
    } finally {
      setIsLoading(false);
    }
  };

  // Handle input focus to ensure dropdown shows
  const handleInputFocus = () => {
    setShowDropdown(true);
    
    // If we don't have any results yet, load initial properties
    if (propertyResults.length === 0) {
      fetchInitialProperties();
    }
  };

  // Handle property input change
  const handlePropertyInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setPropertySearch(value);
    setLabel(value); // Update label as user types
    
    // Always show dropdown when typing
    setShowDropdown(true);
    
    if (!value.trim()) {
      // When input is cleared, clear the selected property and wikidataPropertyId
      setSelectedProperty(null);
      setWikidataPropertyId('');
      // Show initial properties immediately when search is cleared
      fetchInitialProperties();
    } else if (selectedProperty && value !== selectedProperty.label) {
      // If user edits a previously selected property, clear it
      setSelectedProperty(null);
      // Keep the existing wikidataPropertyId until they select a new property
      // The search debounce effect will handle the actual search
    }
  };

  const handleSelectProperty = (property: WikidataProperty) => {
    setSelectedProperty(property);
    setLabel(property.label);
    setPropertySearch(property.label);
    setWikidataPropertyId(property.id);
    setShowDropdown(false);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError(null);

    // Create the update payload
    const updatePayload = {
      label,
      type,
      weight,
      color,
      // If label is empty, also clear the wikidataPropertyId
      wikidataPropertyId: label.trim() === '' ? '' : (selectedProperty ? selectedProperty.id : wikidataPropertyId)
    };

    try {
      const response = await fetchWithAuth(
        API_ENDPOINTS.graph.edges.update(edge.id),
        {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(updatePayload)
        }
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || `Error ${response.status}: ${response.statusText}`);
      }

      onUpdate();
      onClose();
    } catch (err) {
      setError('Failed to update edge');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async () => {
    // Confirm deletion
    if (!window.confirm('Are you sure you want to delete this connection?')) {
      return;
    }

    setIsDeleting(true);
    setError(null);

    try {
      const response = await fetchWithAuth(
        API_ENDPOINTS.graph.edges.delete(edge.id),
        {
          method: 'DELETE'
        }
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || `Error ${response.status}: ${response.statusText}`);
      }

      if (onDelete) {
        onDelete(edge.id);
      } else {
        onUpdate();
      }
      onClose();
    } catch (err) {
      setError('Failed to delete connection');
      setIsDeleting(false);
    }
  };

  // Make sure dropdown appears when we have results
  useEffect(() => {
    if (propertyResults.length > 0) {
      setShowDropdown(true);
    }
  }, [propertyResults]);

  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
      <div className="bg-white/95 backdrop-blur-sm rounded-lg p-6 max-w-md w-full shadow-xl">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-xl font-semibold">Edit Connection</h3>
          <button 
            onClick={onClose}
            className="text-gray-500 hover:text-gray-700"
          >
            <FaTimes />
          </button>
        </div>

        <div className="flex justify-between items-center mb-4">
          <div className="text-sm text-gray-600">
            Connection ID: {edge.id}
          </div>
          <button
            onClick={handleDelete}
            disabled={isDeleting}
            className="flex items-center gap-1 text-red-500 hover:text-red-700 px-3 py-1 rounded-md hover:bg-red-50 transition-colors"
          >
            <FaTrash size={14} />
            <span>{isDeleting ? 'Deleting...' : 'Delete'}</span>
          </button>
        </div>

        <form ref={formRef} onSubmit={handleSubmit} className="space-y-4">
          <div className="mb-6 relative">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Edge Label
            </label>
            <div className="relative">
              <input
                ref={searchInputRef}
                type="text"
                value={propertySearch}
                onChange={handlePropertyInputChange}
                onFocus={handleInputFocus}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Search Wikidata properties or type custom label"
              />
              
              {/* Property search dropdown */}
              <div 
                className={`absolute z-[60] bg-white border border-gray-300 rounded-lg shadow-lg overflow-auto mt-1 max-h-[180px] w-full ${!showDropdown ? 'hidden' : ''}`}
                ref={(node) => {
                  // Set both refs to the same element
                  searchResultsRef.current = node;
                  dropdownContainerRef.current = node;
                }}
              >
                {/* Main content based on loading state */}
                {isLoading ? (
                  <div className="p-2 text-center text-sm text-gray-500">
                    <div className="inline-block animate-spin h-4 w-4 border-2 border-blue-500 rounded-full border-t-transparent mr-2"></div>
                    Searching...
                  </div>
                ) : propertyResults.length > 0 ? (
                  // Render the property list when we have results
                  <>
                    {/* Map through the properties */}
                    {propertyResults.map((property, index) => (
                      <div
                        key={`${property.id}-${index}`}
                        className="p-2 hover:bg-gray-100 cursor-pointer border-b border-gray-200"
                        onClick={() => handleSelectProperty(property)}
                      >
                        <p className="font-medium">{property.label} <span className="text-gray-500">({property.id})</span></p>
                        {property.description && (
                          <p className="text-sm text-gray-600 truncate">{property.description}</p>
                        )}
                      </div>
                    ))}
                    
                    {/* Loading more indicator */}
                    {isFetchingMore && (
                      <div className="p-2 text-center text-sm text-gray-500">
                        <div className="inline-block animate-spin h-4 w-4 border-2 border-blue-500 rounded-full border-t-transparent mr-2"></div>
                        Loading more...
                      </div>
                    )}
                    
                    {/* No more results indicator */}
                    {!hasMoreResults && (
                      <div className="p-2 text-center text-sm text-gray-500">
                        No more properties to load
                      </div>
                    )}
                  </>
                ) : (
                  // No results found message
                  <div className="p-2 text-center text-sm text-gray-500">
                    No properties found
                  </div>
                )}
              </div>
              
              {/* Loading indicator inside the input */}
              {isLoading && (
                <div className="absolute right-3 top-1/2 transform -translate-y-1/2">
                  <div className="animate-spin h-4 w-4 border-2 border-blue-500 rounded-full border-t-transparent"></div>
                </div>
              )}
            </div>
            
            {selectedProperty && (
              <div className="text-xs text-blue-600 mt-1">
                Using Wikidata property: {selectedProperty.id} ({selectedProperty.label})
              </div>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Edge Type
            </label>
            <select
              value={type}
              onChange={(e) => setType(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="default">Default</option>
              <option value="straight">Straight</option>
              <option value="step">Step</option>
              <option value="smoothstep">Smooth Step</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Edge Weight: {weight}
            </label>
            <input
              type="range"
              min="1"
              max="10"
              value={weight}
              onChange={(e) => setWeight(parseInt(e.target.value))}
              className="w-full accent-green-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Edge Color
            </label>
            <div className="flex flex-col md:flex-row gap-4">
              <div className="w-full md:w-1/2">
                <HexColorPicker 
                  color={color} 
                  onChange={setColor} 
                  style={{ width: '100%', height: '150px' }}
                />
              </div>
              <div className="flex-1 flex flex-col">
                <input
                  type="text"
                  value={color}
                  onChange={(e) => setColor(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md mb-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <div className="h-12 w-full rounded-md mt-2 border border-gray-200" style={{ backgroundColor: color }}></div>
              </div>
            </div>
          </div>

          {error && (
            <div className="text-red-500 text-sm text-center">
              {error}
            </div>
          )}

          <div className="flex justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-50 transition-colors"
              disabled={isSubmitting}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 transition-colors"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default EdgeDetails; 