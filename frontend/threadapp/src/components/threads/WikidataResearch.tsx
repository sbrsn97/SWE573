import React, { useState, useEffect, useRef } from 'react';
import { FaSearch, FaSpinner, FaExternalLinkAlt, FaInfoCircle, FaQuestionCircle, FaLink, FaCalendarAlt, FaMapMarkerAlt, FaUser, FaBuilding, FaGlobe, FaTimes } from 'react-icons/fa';
import { nlpService } from '../../services/nlpService';
import { wikidataService, WikidataEntity } from '../../services/wikidataService';

// Common Wikidata property mapping for better display
const propertyCategories = {
  identity: ['instance of', 'subclass of', 'part of', 'has part', 'facet of', 'type'],
  temporal: ['inception', 'date of birth', 'date of death', 'publication date', 'start time', 'end time', 'point in time'],
  spatial: ['country', 'located in', 'location', 'coordinate location', 'headquarters location', 'country of origin', 'place of birth', 'place of death'],
  people: ['founder', 'founded by', 'created by', 'developer', 'author', 'director', 'producer', 'manufacturer', 'owned by', 'employer'],
  details: ['official website', 'website', 'url', 'described at url', 'title', 'industry']
};

// Get icon for property category
const getCategoryIcon = (category: string) => {
  switch (category) {
    case 'identity':
      return <FaInfoCircle className="inline mr-1 text-blue-500" />;
    case 'temporal':
      return <FaCalendarAlt className="inline mr-1 text-green-500" />;
    case 'spatial':
      return <FaMapMarkerAlt className="inline mr-1 text-red-500" />;
    case 'people':
      return <FaUser className="inline mr-1 text-purple-500" />;
    case 'details':
      return <FaLink className="inline mr-1 text-orange-500" />;
    default:
      return <FaGlobe className="inline mr-1 text-gray-500" />;
  }
};

// Determine which category a property belongs to
const getPropertyCategory = (key: string): string => {
  // First, remove any P-ID in brackets
  const { mainLabel } = parsePropertyLabel(key);
  const keyLower = mainLabel.toLowerCase();
  
  for (const [category, terms] of Object.entries(propertyCategories)) {
    if (terms.some(term => keyLower.includes(term))) {
      return category;
    }
  }
  return 'other';
};

// Check if a value is a URL
const isUrl = (value: string): boolean => {
  return value.startsWith('http://') || value.startsWith('https://');
};

// Format date strings nicely
const formatDateValue = (value: string): string => {
  if (/^\d{4}-\d{2}-\d{2}/.test(value)) {
    try {
      const date = new Date(value);
      if (!isNaN(date.getTime())) {
        return date.toLocaleDateString(undefined, { 
          year: 'numeric', 
          month: 'long', 
          day: 'numeric' 
        });
      }
    } catch (e) {
      // If date parsing fails, return the original value
    }
  }
  return value;
};

// Format property value
const formatPropertyValue = (value: string): string => {
  // Handle dates
  const formattedValue = formatDateValue(value);
  
  // Truncate long values
  if (formattedValue.length > 100) {
    return formattedValue.substring(0, 97) + '...';
  }
  
  return formattedValue;
};

// Parse property label to extract the P-ID in brackets if present
const parsePropertyLabel = (label: string): { mainLabel: string; pId: string | null } => {
  // Check for our new format with P-ID in parentheses at end of string
  let match = label.match(/^(.*?)\s*\(([P][0-9]+)\)$/);
  if (match) {
    return {
      mainLabel: match[1].trim(),
      pId: match[2]
    };
  }
  
  // Check for old format with P-ID in brackets at end of string
  match = label.match(/^(.*?)\s*\[([P][0-9]+)\]$/);
  if (match) {
    return {
      mainLabel: match[1].trim(),
      pId: match[2]
    };
  }
  
  // Check for P-IDs that might be embedded in the text (e.g., "Property P123")
  const embeddedMatch = label.match(/^(.*?)\s*([P][0-9]+)$/);
  if (embeddedMatch) {
    return {
      mainLabel: embeddedMatch[1].trim(),
      pId: embeddedMatch[2]
    };
  }
  
  // Check if the entire label is just a P-ID
  if (label.match(/^[P][0-9]+$/)) {
    return {
      mainLabel: "Property",
      pId: label
    };
  }
  
  return { 
    mainLabel: label,
    pId: null
  };
};

interface WikidataResearchProps {
  threadTitle: string;
  threadDescription: string;
}

const WikidataResearch: React.FC<WikidataResearchProps> = ({ threadTitle, threadDescription }) => {
  const [keywords, setKeywords] = useState<string[]>([]);
  const [selectedKeyword, setSelectedKeyword] = useState<string>('');
  const [customQuery, setCustomQuery] = useState<string>('');
  const [searchResults, setSearchResults] = useState<WikidataEntity[]>([]);
  const [selectedEntity, setSelectedEntity] = useState<WikidataEntity | null>(null);
  const [selectedProperty, setSelectedProperty] = useState<{ key: string; value: string } | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [keywordsLoading, setKeywordsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [isExpanded, setIsExpanded] = useState<boolean>(false);
  const componentRef = useRef<HTMLDivElement>(null);

  // Add observer to detect when component is scrolled to via menu
  useEffect(() => {
    // Check if component was targeted for highlight (from menu click)
    const checkForHighlight = () => {
      if (componentRef.current?.classList.contains('highlight-pulse')) {
        setIsExpanded(true);
      }
    };

    // Set up observer
    const observer = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        if (mutation.type === 'attributes' && mutation.attributeName === 'class') {
          checkForHighlight();
        }
      });
    });

    if (componentRef.current) {
      observer.observe(componentRef.current, { attributes: true });
    }

    return () => {
      observer.disconnect();
    };
  }, []);

  // Add CSS for highlight pulse animation
  useEffect(() => {
    const style = document.createElement('style');
    style.textContent = `
      @keyframes highlightPulse {
        0% { box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.5); }
        70% { box-shadow: 0 0 0 10px rgba(59, 130, 246, 0); }
        100% { box-shadow: 0 0 0 0 rgba(59, 130, 246, 0); }
      }
      
      .highlight-pulse {
        animation: highlightPulse 1s ease-out 2;
      }
    `;
    document.head.appendChild(style);
    
    return () => {
      document.head.removeChild(style);
    };
  }, []);

  // Extract keywords when component mounts
  useEffect(() => {
    const extractKeywords = async () => {
      try {
        setKeywordsLoading(true);
        setError(null);
        
        // Combine title and description for better keyword extraction
        const combinedText = `${threadTitle} ${threadDescription || ''}`.trim();
        if (combinedText) {
          const extractedKeywords = await nlpService.extractKeywords(combinedText);
          
          // Filter out duplicate keywords and sort alphabetically
          const uniqueKeywords = Array.from(new Set(extractedKeywords)).sort();
          setKeywords(uniqueKeywords);
        }
      } catch (err) {
        setError('Failed to extract keywords. Please try again.');
        console.error('Error extracting keywords:', err);
      } finally {
        setKeywordsLoading(false);
      }
    };

    extractKeywords();
  }, [threadTitle, threadDescription]);

  // Handle keyword selection
  const handleKeywordSelect = (keyword: string) => {
    setSelectedKeyword(keyword);
    setCustomQuery(keyword);
  };

  // Handle search
  const handleSearch = async () => {
    if (!customQuery.trim()) return;
    
    try {
      setLoading(true);
      setError(null);
      setSelectedEntity(null);
      
      const response = await wikidataService.searchTopics(customQuery, 0, 10);
      setSearchResults(response.items || []);
      
      if (response.items.length === 0) {
        setError('No results found. Try a different search term.');
      }
    } catch (err) {
      setError('Search failed. Please try again.');
      console.error('Error searching Wikidata:', err);
    } finally {
      setLoading(false);
    }
  };

  // Handle entity selection
  const handleEntitySelect = async (entity: WikidataEntity) => {
    try {
      setLoading(true);
      setError(null);
      
      const entityDetails = await wikidataService.getTopicDetails(entity.id);
      setSelectedEntity(entityDetails);
    } catch (err) {
      setError('Failed to load entity details. Please try again.');
      console.error('Error loading entity details:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div 
      id="wikidata-research-section" 
      ref={componentRef}
      className="bg-white rounded-xl shadow-sm p-6 mb-6"
    >
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-semibold text-gray-800 flex items-center">
          <FaSearch className="mr-2 text-blue-500" />
          Research on Wikidata
          <button 
            className="ml-2 text-gray-400 hover:text-gray-600" 
            title="Learn more about Wikidata research"
            onClick={() => window.open('https://www.wikidata.org/wiki/Wikidata:Main_Page', '_blank')}
          >
            <FaQuestionCircle size={16} />
          </button>
        </h2>
        <button 
          className="text-blue-500 hover:text-blue-700 text-sm font-medium"
          onClick={() => setIsExpanded(!isExpanded)}
        >
          {isExpanded ? 'Collapse' : 'Expand'}
        </button>
      </div>

      {!isExpanded ? (
        <p className="text-gray-600 text-sm mb-2">
          Expand to research concepts from this thread on Wikidata.
        </p>
      ) : (
        <>
          <p className="text-gray-600 text-sm mb-4">
            Search Wikidata for concepts mentioned in this thread to learn more about them.
          </p>

          {/* Keywords Section */}
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Keywords extracted from this thread:
            </label>
            <div className="flex flex-wrap gap-2 mb-2">
              {keywordsLoading ? (
                <div className="flex items-center text-gray-500">
                  <FaSpinner className="animate-spin mr-2" />
                  Extracting keywords...
                </div>
              ) : keywords.length > 0 ? (
                keywords.map((keyword, index) => (
                  <button
                    key={index}
                    onClick={() => handleKeywordSelect(keyword)}
                    className={`px-2 py-1 rounded-full text-xs font-medium transition-colors ${
                      selectedKeyword === keyword
                        ? 'bg-blue-500 text-white'
                        : 'bg-gray-100 text-gray-800 hover:bg-gray-200'
                    }`}
                  >
                    {keyword}
                  </button>
                ))
              ) : (
                <p className="text-gray-500 text-sm">No keywords found. Try entering a custom search term.</p>
              )}
            </div>
          </div>

          {/* Search Input */}
          <div className="mb-4">
            <div className="flex">
              <input
                type="text"
                value={customQuery}
                onChange={(e) => setCustomQuery(e.target.value)}
                placeholder="Enter search term..."
                className="flex-grow px-3 py-2 border border-gray-300 rounded-l-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
              <button
                onClick={handleSearch}
                disabled={loading || !customQuery.trim()}
                className="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-r-md transition-colors disabled:bg-blue-300"
              >
                {loading ? <FaSpinner className="animate-spin" /> : <FaSearch />}
              </button>
            </div>
            {error && <p className="text-red-500 text-sm mt-1">{error}</p>}
          </div>

          {/* Search Results */}
          {searchResults.length > 0 && !selectedEntity && (
            <div className="mb-4">
              <h3 className="text-md font-medium text-gray-700 mb-2">Search Results:</h3>
              <div className="border rounded-md divide-y max-h-60 overflow-y-auto">
                {searchResults.map((result) => (
                  <div 
                    key={result.id} 
                    className="p-3 hover:bg-gray-50 cursor-pointer"
                    onClick={() => handleEntitySelect(result)}
                  >
                    <div className="font-medium">{result.label}</div>
                    <div className="text-sm text-gray-600">{result.description || 'No description available'}</div>
                    <div className="text-xs text-gray-500 mt-1">ID: {result.id}</div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Entity Details */}
          {selectedEntity && (
            <div className="border rounded-md p-4 bg-gray-50">
              <div className="flex justify-between items-start mb-2">
                <h3 className="text-lg font-medium">{selectedEntity.label}</h3>
                <div className="flex space-x-2">
                  {selectedEntity.url && (
                    <a 
                      href={selectedEntity.url} 
                      target="_blank" 
                      rel="noopener noreferrer" 
                      className="text-blue-500 hover:text-blue-700"
                      title="View on Wikidata"
                    >
                      <FaExternalLinkAlt />
                    </a>
                  )}
                  <button 
                    className="text-gray-400 hover:text-gray-600"
                    onClick={() => setSelectedEntity(null)}
                    title="Back to search results"
                  >
                    &times;
                  </button>
                </div>
              </div>
              
              <p className="text-gray-600 mb-4">{selectedEntity.description || 'No description available'}</p>
              
              {selectedEntity.properties && Object.keys(selectedEntity.properties).length > 0 ? (
                <div>
                  <h4 className="text-md font-medium text-gray-700 mb-2">Properties:</h4>
                  
                  {/* Group properties by category */}
                  {(() => {
                    // Use all properties without filtering
                    const allProperties = Object.entries(selectedEntity.properties);
                    
                    if (allProperties.length === 0) {
                      return (
                        <div className="flex items-center text-gray-500">
                          <FaInfoCircle className="mr-2" />
                          No properties available
                        </div>
                      );
                    }
                    
                    // Group properties by category
                    const categorizedProperties = allProperties.reduce((acc, [key, value]) => {
                      const category = getPropertyCategory(key);
                      if (!acc[category]) acc[category] = [];
                      acc[category].push([key, value]);
                      return acc;
                    }, {} as Record<string, [string, string][]>);
                    
                    // Sort properties alphabetically within each category
                    Object.keys(categorizedProperties).forEach(category => {
                      categorizedProperties[category].sort((a, b) => {
                        const { mainLabel: labelA } = parsePropertyLabel(a[0]);
                        const { mainLabel: labelB } = parsePropertyLabel(b[0]);
                        return labelA.localeCompare(labelB);
                      });
                    });
                    
                    // Order categories by importance
                    const categoryOrder = ['identity', 'temporal', 'spatial', 'people', 'details', 'other'];
                    const sortedCategories = Object.keys(categorizedProperties).sort(
                      (a, b) => categoryOrder.indexOf(a) - categoryOrder.indexOf(b)
                    );
                    
                    return sortedCategories.map(category => (
                      <div key={category} className="mb-4">
                        <h5 className="text-sm font-medium text-gray-600 mb-2 capitalize flex items-center">
                          {getCategoryIcon(category)}
                          {category}
                        </h5>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                          {categorizedProperties[category].map(([key, value], index) => (
                            <div key={index} className="border rounded p-2 bg-white hover:bg-gray-50">
                              <div className="text-sm font-medium text-gray-700">
                                {(() => {
                                  const { mainLabel, pId } = parsePropertyLabel(key);
                                  const hasDescription = selectedEntity.propertyDescriptions && key in selectedEntity.propertyDescriptions;
                                  
                                  return (
                                    <>
                                      <span 
                                        title={selectedEntity.propertyDescriptions?.[key] || 'No description available'} 
                                        className={`cursor-help ${hasDescription ? 'border-b border-dotted border-gray-400' : ''}`}
                                        onClick={(e) => {
                                          e.stopPropagation();
                                          setSelectedProperty({ key, value });
                                        }}
                                      >
                                        {mainLabel}
                                        {hasDescription && (
                                          <FaInfoCircle 
                                            className="inline-block ml-1 text-gray-400" 
                                            size={12} 
                                          />
                                        )}
                                      </span>
                                      {pId && (
                                        <a 
                                          href={`https://www.wikidata.org/wiki/Property:${pId}`}
                                          target="_blank"
                                          rel="noopener noreferrer"
                                          className="ml-1 text-xs text-gray-400 font-normal rounded bg-gray-100 px-1.5 py-0.5 hover:bg-gray-200 hover:text-gray-600 cursor-pointer"
                                          onClick={(e) => e.stopPropagation()}
                                          title="View property on Wikidata"
                                        >
                                          {pId}
                                        </a>
                                      )}
                                    </>
                                  );
                                })()}
                              </div>
                              <div className="text-sm text-gray-600">
                                {isUrl(value) ? (
                                  <a 
                                    href={value} 
                                    target="_blank" 
                                    rel="noopener noreferrer"
                                    className="text-blue-500 hover:underline flex items-center"
                                  >
                                    <span className="truncate">{value.replace(/^https?:\/\//, '')}</span>
                                    <FaExternalLinkAlt className="ml-1 flex-shrink-0" size={10} />
                                  </a>
                                ) : (
                                  formatPropertyValue(value)
                                )}
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    ));
                  })()}
                </div>
              ) : (
                <div className="flex items-center text-gray-500">
                  <FaInfoCircle className="mr-2" />
                  No additional properties available
                </div>
              )}
            </div>
          )}
          
          {/* Property Detail Modal */}
          {selectedProperty && (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" onClick={() => setSelectedProperty(null)}>
              <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4" onClick={(e) => e.stopPropagation()}>
                <div className="flex justify-between items-start mb-4">
                  <h3 className="text-lg font-medium">
                    {(() => {
                      const { mainLabel, pId } = parsePropertyLabel(selectedProperty.key);
                      return (
                        <>
                          {mainLabel}
                          {pId && <span className="ml-2 text-sm text-gray-500">{pId}</span>}
                        </>
                      );
                    })()}
                  </h3>
                  <button 
                    onClick={() => setSelectedProperty(null)}
                    className="text-gray-400 hover:text-gray-600"
                  >
                    <FaTimes />
                  </button>
                </div>
                
                {selectedEntity?.propertyDescriptions?.[selectedProperty.key] && (
                  <p className="text-gray-600 mb-4 border-l-4 border-blue-500 pl-3 py-2 bg-blue-50">
                    {selectedEntity.propertyDescriptions[selectedProperty.key]}
                  </p>
                )}
                
                <div className="mb-4">
                  <h4 className="text-sm font-medium text-gray-700 mb-1">Value:</h4>
                  <div className="bg-gray-50 p-3 rounded border">
                    {isUrl(selectedProperty.value) ? (
                      <a 
                        href={selectedProperty.value} 
                        target="_blank" 
                        rel="noopener noreferrer"
                        className="text-blue-500 hover:underline flex items-center"
                      >
                        <span>{selectedProperty.value.replace(/^https?:\/\//, '')}</span>
                        <FaExternalLinkAlt className="ml-1 flex-shrink-0" size={10} />
                      </a>
                    ) : (
                      formatPropertyValue(selectedProperty.value)
                    )}
                  </div>
                </div>
                
                <div className="flex justify-end">
                  <a
                    href={`https://www.wikidata.org/wiki/Property:${parsePropertyLabel(selectedProperty.key).pId || ''}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="mr-2 px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded flex items-center text-sm"
                  >
                    <FaExternalLinkAlt className="mr-1" size={12} />
                    View on Wikidata
                  </a>
                  <button
                    onClick={() => setSelectedProperty(null)}
                    className="px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded text-sm"
                  >
                    Close
                  </button>
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default WikidataResearch; 