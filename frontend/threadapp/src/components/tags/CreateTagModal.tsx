import React, { useState, useEffect, useRef, useCallback } from 'react';
import { API_ENDPOINTS, API_BASE_URL } from '../../config/config';
import Tag from './Tag';
import { HexColorPicker } from 'react-colorful';

// Static initial entities data
const initialEntities = [
  // Core concepts
  { id: 'Q5',       label: 'Human', description: 'Common name of Homo sapiens, the only extant human species' },
  { id: 'Q2',       label: 'Earth', description: 'Third planet from the Sun in the Solar System' },
  { id: 'Q1',       label: 'Universe', description: 'All of space and time and their contents' },

  // Countries
  { id: 'Q30',      label: 'United States of America', description: 'Federal republic in North America' },
  { id: 'Q183',     label: 'Germany', description: 'Country in Central Europe' },
  { id: 'Q142',     label: 'France', description: 'Country in Western Europe' },
  { id: 'Q145',     label: 'United Kingdom', description: 'Country in Northwestern Europe' },
  { id: 'Q668',     label: 'India', description: 'Country in South Asia' },
  { id: 'Q148',     label: 'China', description: 'Country in East Asia' },
  { id: 'Q17',      label: 'Japan', description: 'Island country in East Asia' },
  { id: 'Q155',     label: 'Brazil', description: 'Country in South America' },
  { id: 'Q159',     label: 'Russia', description: 'Country spanning Eastern Europe and Northern Asia' },
  { id: 'Q43',      label: 'Turkey', description: 'Country in Western Asia and Southeastern Europe' },

  // Cities
  { id: 'Q60',      label: 'New York City', description: 'Most populous city in the United States' },
  { id: 'Q84',      label: 'London', description: 'Capital and largest city of England and the United Kingdom' },
  { id: 'Q90',      label: 'Paris', description: 'Capital and largest city of France' },
  { id: 'Q64',      label: 'Berlin', description: 'Capital and largest city of Germany' },
  { id: 'Q1490',    label: 'Tokyo', description: 'Capital and largest city of Japan' },
  { id: 'Q649',     label: 'Moscow', description: 'Capital and largest city of Russia' },
  { id: 'Q406',     label: 'Istanbul', description: 'Largest city in Turkey' },
  { id: 'Q8686',    label: 'São Paulo', description: 'Largest city in Brazil' },
  { id: 'Q4006',    label: 'Mumbai', description: 'Financial capital and largest city of India' },
  { id: 'Q52',      label: 'Cairo', description: 'Capital and largest city of Egypt' },

  // Big companies / brands
  { id: 'Q312',     label: 'Apple Inc.', description: 'American multinational technology company' },
  { id: 'Q95',      label: 'Google', description: 'American multinational technology company specializing in Internet-related services' },
  { id: 'Q2283',    label: 'Microsoft', description: 'American multinational technology corporation' },
  { id: 'Q3884',    label: 'Amazon.com', description: 'American multinational technology company focusing on e-commerce' },
  { id: 'Q355',     label: 'Facebook', description: 'American online social media and social networking service' },
  { id: 'Q37152',   label: 'IBM', description: 'American multinational technology corporation' },
  { id: 'Q478214',  label: 'Tesla, Inc.', description: 'American electric vehicle and clean energy company' },
  { id: 'Q483',     label: 'Coca-Cola', description: 'American multinational beverage corporation' },
  { id: 'Q1503162', label: 'Nike', description: 'American multinational athletic footwear and apparel corporation' },
  { id: 'Q905763',  label: 'Samsung', description: 'South Korean multinational manufacturing conglomerate' },

  // Programming & tech
  { id: 'Q28865',   label: 'Python (programming language)', description: 'High-level, general-purpose programming language' },
  { id: 'Q2005',    label: 'JavaScript', description: 'High-level, interpreted programming language' },
  { id: 'Q251',     label: 'Java (programming language)', description: 'Object-oriented programming language' },
  { id: 'Q210439',  label: 'C++', description: 'General-purpose programming language' },
  { id: 'Q3564568', label: 'React', description: 'JavaScript library for building user interfaces' },
  { id: 'Q453253',  label: 'Angular', description: 'TypeScript-based open-source web application framework' },
  { id: 'Q201641',  label: 'Node.js', description: 'JavaScript runtime environment' },
  { id: 'Q559623',  label: 'Docker', description: 'Set of platform as a service products' },
  { id: 'Q21563378',label: 'Kubernetes', description: 'Open-source container orchestration platform' },
  { id: 'Q297',     label: 'Git', description: 'Distributed version control system' },

  // Science & nature
  { id: 'Q7191',    label: 'DNA', description: 'Molecule that carries genetic instructions' },
  { id: 'Q7192',    label: 'Atom', description: 'Smallest unit of ordinary matter' },
  { id: 'Q11421',   label: 'Gravity', description: 'Natural phenomenon by which all things with mass are attracted to one another' },
  { id: 'Q12389',   label: 'Evolution', description: 'Change in the heritable characteristics of biological populations over successive generations' },
  { id: 'Q6540',    label: 'Black hole', description: 'Region of spacetime where gravity is so strong that nothing can escape' },
  { id: 'Q808',     label: 'Virus', description: 'Infectious agent that replicates only inside the living cells of an organism' },
  { id: 'Q10876',   label: 'Bacteria', description: 'Domain of single-celled prokaryotic microorganisms' },
  { id: 'Q6858',    label: 'Cancer', description: 'Group of diseases involving abnormal cell growth' },
  { id: 'Q174',     label: 'Gold', description: 'Chemical element with symbol Au and atomic number 79' },
  { id: 'Q11465',   label: 'Iron', description: 'Chemical element with symbol Fe and atomic number 26' },
  { id: 'Q184',     label: 'Lithium', description: 'Chemical element with symbol Li and atomic number 3' },

  // Crime & mystery
  { id: 'Q83267',     label: 'Crime', description: 'Unlawful act punishable by a state or other authority' },
  { id: 'Q1360677',   label: 'Crime scene', description: 'Location where an illegal act took place' },
  { id: 'Q120396',    label: 'Detective', description: 'Investigator, usually a member of a law enforcement agency' },
  { id: 'Q1053583',   label: 'Evidence', description: 'Material that is presented to a court of law to prove or disprove a fact' },
  { id: 'Q210642',    label: 'Fingerprint', description: 'Impression left by the friction ridges of a human finger' },
  { id: 'Q219831',    label: 'DNA profiling', description: 'Forensic technique used to identify individuals by characteristics of their DNA' },
  { id: 'Q101493',    label: 'Murder', description: 'Unlawful killing of another human being' },
  { id: 'Q13406463',  label: 'Conspiracy theory', description: 'Explanation for an event or situation that invokes a conspiracy' },
  { id: 'Q2074349',   label: 'Mystery', description: 'Genre of fiction that deals with the solution of a crime or puzzle' },
  { id: 'Q58023',     label: 'Forensic science', description: 'Application of science to criminal and civil laws' },
  { id: 'Q137759',    label: 'Autopsy', description: 'Surgical procedure that consists of a thorough examination of a corpse' },
  { id: 'Q988524',    label: 'Coroner', description: 'Official who investigates deaths that occur under unusual or suspicious circumstances' },
  { id: 'Q35163',     label: 'Ballistics', description: 'Science of mechanics that deals with the launching, flight, behavior, and effects of projectiles' },

  // Academia & media
  { id: 'Q3918',     label: 'University', description: 'Institution of higher education and research' },
  { id: 'Q23442',    label: 'Library', description: 'Collection of materials, books or media that are accessible for use' },
  { id: 'Q13442814', label: 'Scholarly article', description: 'Academic work published in an academic journal' },
  { id: 'Q571',      label: 'Book', description: 'Medium for recording information in the form of writing or images' },

  // Historical & literary figures
  { id: 'Q7186',     label: 'Marie Curie', description: 'Polish and naturalized-French physicist and chemist who conducted pioneering research on radioactivity' },
  { id: 'Q937',      label: 'Albert Einstein', description: 'German-born theoretical physicist who developed the theory of relativity' },
  { id: 'Q7251',     label: 'Ada Lovelace', description: 'English mathematician and writer, chiefly known for her work on Charles Babbage\'s proposed mechanical general-purpose computer' },
  { id: 'Q725',      label: 'Alan Turing', description: 'English mathematician, computer scientist, logician, cryptanalyst, philosopher, and theoretical biologist' },
  { id: 'Q692',      label: 'Sherlock Holmes', description: 'Fictional detective created by British author Sir Arthur Conan Doyle' },
  { id: 'Q23419',    label: 'Zeus', description: 'Sky and thunder god in ancient Greek religion, who rules as king of the gods of Mount Olympus' },
  { id: 'Q181779',   label: 'Hercules', description: 'Roman hero and god, equivalent to the Greek divine hero Heracles' },

  // Institutions & organizations
  { id: 'Q1065',     label: 'United Nations', description: 'Intergovernmental organization whose stated purposes are to maintain international peace and security' },
  { id: 'Q15852',    label: 'World Health Organization', description: 'Specialized agency of the United Nations responsible for international public health' },
  { id: 'Q84567',    label: 'Interpol', description: 'International organization that facilitates worldwide police cooperation and crime control' },
  { id: 'Q7588',     label: 'Federal Bureau of Investigation (FBI)', description: 'Domestic intelligence and security service of the United States' },
  { id: 'Q271076',   label: 'Central Intelligence Agency (CIA)', description: 'Foreign intelligence service of the federal government of the United States' },

  // Lab & astronomy
  { id: 'Q179443',   label: 'Microscope', description: 'Laboratory instrument used to examine objects that are too small to be seen by the naked eye' },
  { id: 'Q8022',     label: 'Telescope', description: 'Optical instrument using lenses, curved mirrors, or a combination of both to observe distant objects' },
  { id: 'Q525',      label: 'Sun', description: 'Star at the center of the Solar System' },
  { id: 'Q405',      label: 'Moon', description: 'Earth\'s only natural satellite' },
  { id: 'Q111',      label: 'Mars', description: 'Fourth planet from the Sun and the second-smallest planet in the Solar System' },
  { id: 'Q318',      label: 'Galaxy', description: 'System of stars, stellar remnants, interstellar gas, dust, and dark matter' },

  // AI & modern tech
  { id: 'Q11660',    label: 'Artificial intelligence', description: 'Intelligence demonstrated by machines, as opposed to natural intelligence displayed by animals including humans' }
];

interface WikidataEntity {
  id: string;
  label: string;
  description?: string;
  url?: string;
  type?: string;
}

interface WikidataSearchResponse {
  items: WikidataEntity[];
  totalItems: number;
  currentPage: number;
  totalPages: number;
  pageSize: number;
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

// Tag interface matching TagSelector.tsx
interface Tag {
  id: number;
  label: string;
  description: string;
  colorCodeString: string;
  wikidataEntityId: string;
}

const CreateTagModal = ({ onClose, onTagCreated }: CreateTagModalProps) => {
  const [label, setLabel] = useState('');
  const [description, setDescription] = useState('');
  const [colorCodeString, setColorCodeString] = useState('#3B82F6'); // Default to blue
  const [wikidataSearch, setWikidataSearch] = useState('');
  const [wikidataResults, setWikidataResults] = useState<WikidataEntity[]>([]);
  const [selectedEntity, setSelectedEntity] = useState<WikidataEntity | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const [hasMoreEntities, setHasMoreEntities] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);
  const [showColorPicker, setShowColorPicker] = useState(false);
  const searchResultsRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);
  const colorPickerRef = useRef<HTMLDivElement>(null);

  const [dropdownPosition, setDropdownPosition] = useState({
    top: 0,
    left: 0,
    width: 0
  });

  // Update dropdown position based on input element
  const updateDropdownPosition = useCallback(() => {
    if (searchInputRef.current) {
      const rect = searchInputRef.current.getBoundingClientRect();
      setDropdownPosition({
        top: rect.bottom + window.scrollY + 4,
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
  }, [wikidataSearch, updateDropdownPosition]);

  // Preview of how the tag will look
  const previewTag = {
    id: 0, // Placeholder, will be assigned by backend
    label,
    description,
    colorCodeString,
    wikidataEntityId: selectedEntity?.id || '',
  };

  // Effect to fetch initial entities
  useEffect(() => {
    fetchInitialEntities();
  }, []);

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
        setShowColorPicker(false);
      }
    };

    document.addEventListener('keydown', handleEscapeKey);
    return () => {
      document.removeEventListener('keydown', handleEscapeKey);
    };
  }, []);

  // Effect to search for wikidata entities when the search query changes
  useEffect(() => {
    if (wikidataSearch.trim()) {
      setShowDropdown(true); // Show dropdown as soon as user starts typing
      const debounceTimer = setTimeout(() => {
        setPage(1); // Reset pagination when search query changes
        setWikidataResults([]); // Clear previous results
        searchEntities();
      }, 300);
      
      return () => clearTimeout(debounceTimer);
    } else {
      // If search is empty, show initial entities
      fetchInitialEntities();
    }
  }, [wikidataSearch]);

  const fetchInitialEntities = async () => {
    // Use static data instead of API call
    setWikidataResults(initialEntities);
    setShowDropdown(true);
    setIsLoading(false);
  };

  const searchEntities = async () => {
    if (!wikidataSearch.trim()) {
      fetchInitialEntities();
      return;
    }
    
    try {
      setIsLoading(true);
      const token = localStorage.getItem('token');
      if (!token) {
        return;
      }
      
      const searchTerm = encodeURIComponent(wikidataSearch.trim());
      // Fetch more items initially (50 instead of 10)
      const directUrl = `${API_BASE_URL}/wikidata/entities/search?query=${searchTerm}&page=0&size=50`;
      
      const response = await fetch(directUrl, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (!response.ok) {
        return;
      }

      const data = await response.json();
      
      let newEntities: WikidataEntity[] = [];
      
      if (data && Array.isArray(data)) {
        newEntities = data;
      } else if (data && data.data && Array.isArray(data.data.items)) {
        newEntities = data.data.items;
      } else if (data && Array.isArray(data.items)) {
        newEntities = data.items;
      }
      
      setWikidataResults(newEntities);
      setShowDropdown(true);
    } catch (err) {
      // Silent error handling
    } finally {
      setIsLoading(false);
    }
  };

  const handleResultsScroll = () => {
    if (!searchResultsRef.current) return;
    
    const { scrollTop, scrollHeight, clientHeight } = searchResultsRef.current;
    
    // If scrolled near the bottom (within 50px), show more items
    if (scrollTop + clientHeight >= scrollHeight - 50) {
      // Show next 10 items
      const currentCount = wikidataResults.length;
      const nextCount = Math.min(currentCount + 10, wikidataResults.length);
      setWikidataResults(prev => prev.slice(0, nextCount));
    }
  };

  const handleSubmit = async (e?: React.MouseEvent) => {
    if (e) {
      e.preventDefault();
    }
    
    if (!label.trim()) {
      return;
    }
    
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        return;
      }

      // Prepare payload with proper wikidata entity ID
      // Use Q35120 as the default entity ID when none is selected
      const payload = {
        label: label.trim(),
        description: description.trim(),
        colorCodeString: colorCodeString,
        wikidataEntityId: selectedEntity?.id || "Q35120" // Default to Q35120 when no entity is selected
      };

      const response = await fetch(API_ENDPOINTS.tags.create, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(payload)
      });

      const responseText = await response.text();

      if (!response.ok) {
        return;
      }

      let data;
      try {
        data = JSON.parse(responseText) as ApiResponse<Tag>;
      } catch (parseErr) {
        return;
      }
      
      if (data.success) {
        // Call the parent's callback with the newly created tag
        onTagCreated(data.data);
        onClose();
      }
    } catch (err) {
      // Silent error handling
    }
  };

  const handleFormSubmit = (e: React.FormEvent) => {
    e.preventDefault(); // This is critical to prevent the form from refreshing the page
    e.stopPropagation(); // Stop event propagation
    handleSubmit();
    return false; // Ensure no default behavior happens
  };

  // Simplified input change handler without console logs
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setWikidataSearch(e.target.value);
    if (e.target.value.trim()) {
      setShowDropdown(true); // Show dropdown when user is typing
    }
  };

  // Track color changes
  const handleColorChange = (newColor: string) => {
    setColorCodeString(newColor);
  };

  // Close color picker when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (colorPickerRef.current && !colorPickerRef.current.contains(event.target as Node)) {
        setShowColorPicker(false);
      }
    };

    if (showColorPicker) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [showColorPicker]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center backdrop-blur-sm bg-black/30 overflow-y-auto">
      <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full max-h-[90vh] overflow-y-auto p-6">
        <div className="mb-4">
          <h2 className="text-xl font-semibold">Create New Tag</h2>
          
          <form 
              onSubmit={handleFormSubmit} 
              className="tag-creation-form"
              noValidate
            >
            <div className="mb-4">
              <label className="block text-gray-700 font-medium mb-2">
                Label <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={label}
                onChange={(e) => setLabel(e.target.value)}
                className="w-full p-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Enter tag label"
                required
              />
            </div>
            
            <div className="mb-4">
              <label className="block text-gray-700 font-medium mb-2">
                Description
              </label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full p-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Enter tag description"
                rows={3}
              />
            </div>
            
            <div className="mb-4">
              <label className="block text-gray-700 font-medium mb-2">
                Color
              </label>
              <div className="flex items-center space-x-2">
                <div
                  ref={colorPickerRef}
                  className="w-10 h-10 cursor-pointer rounded border border-gray-300"
                  style={{
                    backgroundColor: colorCodeString,
                  }}
                  onClick={() => setShowColorPicker(true)}
                />
                <input
                  type="text"
                  value={colorCodeString}
                  onChange={(e) => setColorCodeString(e.target.value)}
                  className="w-32 p-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="#000000"
                  pattern="^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$"
                  title="Hex color code (e.g. #FF5733)"
                />
                <div className="ml-3">
                  <Tag
                    tag={{
                    id: 0,
                    label,
                    description,
                    colorCodeString,
                    wikidataEntityId: selectedEntity?.id || ''
                  }}
                  />
                </div>
              </div>
            </div>
            
            <div className="mb-6">
              <label className="block text-gray-700 font-medium mb-2">
                Wikidata Entity
              </label>
              
              {selectedEntity ? (
                <div className="p-3 border border-gray-300 rounded-lg mb-2">
                  <div className="flex justify-between">
                    <div>
                      <p className="font-medium">{selectedEntity.label} <span className="text-gray-500">({selectedEntity.id})</span></p>
                      {selectedEntity.description && (
                        <p className="text-sm text-gray-500 mt-1">{selectedEntity.description}</p>
                      )}
                    </div>
                    <button
                      type="button"
                      onClick={() => setSelectedEntity(null)}
                      className="text-red-500 hover:text-red-700"
                    >
                      ✕
                    </button>
                  </div>
                </div>
              ) : (
                <div className="relative">
                  <input
                    ref={searchInputRef}
                    type="text"
                    value={wikidataSearch}
                    onChange={handleInputChange}
                    onFocus={() => {
                      setShowDropdown(true);
                      fetchInitialEntities();
                    }}
                    className="w-full p-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Search Wikidata entities"
                  />
                  {isLoading && <p className="text-gray-500 text-sm mt-1">Loading...</p>}
                  
                  {/* Search Results Dropdown - Position it as a floating layer */}
                  {showDropdown && (
                    <div 
                      className="fixed z-[100] bg-white border border-gray-300 rounded-lg shadow-lg overflow-auto"
                      ref={searchResultsRef} 
                      onScroll={handleResultsScroll}
                      style={{ 
                        maxHeight: '300px',
                        top: `${dropdownPosition.top}px`,
                        left: `${dropdownPosition.left}px`,
                        width: `${dropdownPosition.width}px`
                      }}
                    >
                      {wikidataResults.length > 0 ? (
                        <>
                          {wikidataResults.map(entity => (
                            <div
                              key={entity.id}
                              className="p-2 hover:bg-gray-100 cursor-pointer border-b border-gray-200"
                              onClick={() => {
                                setSelectedEntity(entity);
                                setWikidataSearch('');
                                setShowDropdown(false);
                              }}
                            >
                              <p className="font-medium">{entity.label} <span className="text-gray-500">({entity.id})</span></p>
                              {entity.description && (
                                <p className="text-sm text-gray-600 truncate">{entity.description}</p>
                              )}
                            </div>
                          ))}
                          {hasMoreEntities && wikidataSearch.trim() && (
                            <div className="p-2 text-center text-gray-500 text-sm">
                              {isLoading ? "Loading more..." : "Scroll to load more"}
                            </div>
                          )}
                        </>
                      ) : (
                        <div className="p-2 text-gray-500">No results found</div>
                      )}
                    </div>
                  )}
                </div>
              )}
            </div>
            
            <div className="flex justify-end">
              <button
                type="button"
                onClick={onClose}
                className="mr-2 px-4 py-2 text-gray-700 bg-gray-200 rounded-lg hover:bg-gray-300 focus:outline-none"
              >
                Cancel
              </button>
              <button
                type="button" 
                onClick={handleSubmit}
                className="px-4 py-2 text-white bg-blue-500 rounded-lg hover:bg-blue-600 focus:outline-none"
              >
                Create Tag
              </button>
            </div>
          </form>
        </div>
      </div>
      
      {/* Color Picker Popup */}
      {showColorPicker && (
        <div 
          className="fixed z-[200] bg-white rounded-lg shadow-xl p-4"
          ref={colorPickerRef}
          style={{
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)'
          }}
        >
          <div className="mb-3">
            <h3 className="text-lg font-medium mb-2">Select Color</h3>
            <HexColorPicker color={colorCodeString} onChange={handleColorChange} />
            <div className="mt-2 flex items-center">
              <div 
                className="w-8 h-8 mr-2 rounded border border-gray-300" 
                style={{ backgroundColor: colorCodeString }}
              />
              <input
                type="text"
                value={colorCodeString}
                onChange={(e) => setColorCodeString(e.target.value)}
                className="p-1 border border-gray-300 rounded"
              />
            </div>
          </div>
          <div className="flex justify-end">
            <button
              type="button"
              onClick={() => setShowColorPicker(false)}
              className="px-3 py-1 bg-blue-500 text-white rounded hover:bg-blue-600"
            >
              Done
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default CreateTagModal; 