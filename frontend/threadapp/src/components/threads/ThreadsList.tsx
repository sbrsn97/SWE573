import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import MainLayout from '../layout/MainLayout';
import ThreadCard from './ThreadCard';
import { API_ENDPOINTS } from '../../config/config';
import { fetchWithAuth } from '../../utils/authUtils';
import { FaChevronLeft, FaChevronRight, FaTags, FaSearch, FaFilter, FaCalendarAlt, FaSort, FaTimesCircle } from 'react-icons/fa';
import Tag from '../tags/Tag';

interface Thread {
  id: number;
  title: string;
  description: string | null;
  authorId: number;
  upvoteCount: number;
  downvoteCount: number;
  createdAt: string;
  updatedAt: string;
  tags: Array<{
    id: number;
    label: string;
    description: string;
    colorCodeString: string;
    wikidataEntityId: string;
  }>;
  followerIds?: number[];
}

interface PaginatedResponse {
  content: Thread[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

interface Tag {
  id: number;
  label: string;
  description: string;
  colorCodeString: string;
  wikidataEntityId: string;
}

const ThreadsList = () => {
  const [threads, setThreads] = useState<Thread[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [totalPages, setTotalPages] = useState(1);
  const [searchParams, setSearchParams] = useSearchParams();
  const [availableTags, setAvailableTags] = useState<Tag[]>([]);
  const [loadingTags, setLoadingTags] = useState(false);
  
  // Filters
  const [selectedTags, setSelectedTags] = useState<number[]>([]);
  const [dateFilter, setDateFilter] = useState<string>('all');
  const [sortBy, setSortBy] = useState<string>('newest');
  
  // Tag search
  const [tagSearchQuery, setTagSearchQuery] = useState<string>('');
  
  // Get current page from URL or default to 1
  const currentPage = parseInt(searchParams.get('page') || '1', 10);
  const pageSize = 10; // Number of threads per page
  
  // Fetch available tags for filtering
  useEffect(() => {
    const fetchTags = async () => {
      setLoadingTags(true);
      try {
        const response = await fetchWithAuth(API_ENDPOINTS.tags.getAll);
        
        if (response.ok) {
          const result = await response.json();
          if (result && result.data) {
            setAvailableTags(result.data);
          }
        }
      } catch (err) {
        console.error('Error fetching tags:', err);
      } finally {
        setLoadingTags(false);
      }
    };
    
    fetchTags();
  }, []);
  
  // Build query parameters based on filters
  const buildQueryParams = () => {
    // If we are filtering client-side, we need the full dataset - remove pagination
    if (selectedTags.length > 0 || dateFilter !== 'all') {
      // Only apply sorting, not pagination, to get all threads
      let queryParams = '';
      
      // Add sorting
      switch(sortBy) {
        case 'newest':
          queryParams += 'sort=createdAt,desc';
          break;
        case 'oldest':
          queryParams += 'sort=createdAt,asc';
          break;
        case 'mostVoted':
          queryParams += 'sort=upvoteCount,desc';
          break;
        case 'leastVoted':
          queryParams += 'sort=upvoteCount,asc';
          break;
        default:
          queryParams += 'sort=createdAt,desc';
      }
      
      return queryParams;
    } else {
      // No client-side filtering, use server pagination
      let queryParams = `page=${currentPage - 1}&size=${pageSize}`;
      
      // Add sorting
      switch(sortBy) {
        case 'newest':
          queryParams += '&sort=createdAt,desc';
          break;
        case 'oldest':
          queryParams += '&sort=createdAt,asc';
          break;
        case 'mostVoted':
          queryParams += '&sort=upvoteCount,desc';
          break;
        case 'leastVoted':
          queryParams += '&sort=upvoteCount,asc';
          break;
        default:
          queryParams += '&sort=createdAt,desc';
      }
      
      return queryParams;
    }
  };
  
  // Track all threads for client-side filtering
  const [allThreads, setAllThreads] = useState<Thread[]>([]);
  
  // Apply client-side sorting based on sortBy value
  const applySorting = (threads: Thread[]): Thread[] => {
    const sortedThreads = [...threads];
    
    switch(sortBy) {
      case 'newest':
        sortedThreads.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        break;
      case 'oldest':
        sortedThreads.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
        break;
      case 'mostVoted':
        sortedThreads.sort((a, b) => b.upvoteCount - a.upvoteCount);
        break;
      case 'leastVoted':
        sortedThreads.sort((a, b) => a.upvoteCount - b.upvoteCount);
        break;
      default:
        sortedThreads.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    }
    
    return sortedThreads;
  };
  
  // Fetch threads with pagination and filters
  useEffect(() => {
    const fetchThreads = async () => {
      setLoading(true);
      setError(null);
      
      try {
        // Build query with all filters
        const queryParams = buildQueryParams();
        
        // Add pagination parameters to the API call
        const response = await fetchWithAuth(
          `${API_ENDPOINTS.threads.getAll}?${queryParams}`
        );
        
        if (response.ok) {
          const result = await response.json();
          
          // Debugging
          console.log('API Response:', result);
          
          // Check the structure of the response
          if (result && result.data) {
            
            // Are we doing client-side filtering?
            const isClientFiltering = selectedTags.length > 0 || dateFilter !== 'all';
            
            // Direct response format - array of threads
            if (Array.isArray(result.data)) {
              // Store all threads for filtering
              const sortedThreads = applySorting(result.data);
              setAllThreads(sortedThreads);
              
              if (isClientFiltering) {
                // Apply client-side filters
                let filteredThreads = sortedThreads;
                
                // Filter by tags
                if (selectedTags.length > 0) {
                  filteredThreads = filteredThreads.filter(thread => 
                    thread.tags.some(tag => selectedTags.includes(tag.id))
                  );
                }
                
                // Filter by date
                if (dateFilter !== 'all') {
                  const now = new Date();
                  let cutoffDate = new Date();
                  
                  switch(dateFilter) {
                    case 'today':
                      cutoffDate.setDate(now.getDate() - 1);
                      break;
                    case 'week':
                      cutoffDate.setDate(now.getDate() - 7);
                      break;
                    case 'month':
                      cutoffDate.setMonth(now.getMonth() - 1);
                      break;
                    case 'year':
                      cutoffDate.setFullYear(now.getFullYear() - 1);
                      break;
                  }
                  
                  filteredThreads = filteredThreads.filter(thread => 
                    new Date(thread.createdAt) >= cutoffDate
                  );
                }
                
                // Calculate total pages based on all filtered threads
                const totalFilteredThreads = filteredThreads.length;
                setTotalPages(Math.ceil(totalFilteredThreads / pageSize) || 1);
                
                // Apply client-side pagination after filtering - only get the right slice of threads
                const startIndex = (currentPage - 1) * pageSize;
                const paginatedThreads = filteredThreads.slice(startIndex, startIndex + pageSize);
                
                setThreads(paginatedThreads);
              } else {
                // No client filtering, use server pagination
                // Apply server-side pagination
                const startIndex = (currentPage - 1) * pageSize;
                const endIndex = startIndex + pageSize;
                const paginatedThreads = sortedThreads.slice(startIndex, endIndex);
                
                setThreads(paginatedThreads);
                setTotalPages(Math.ceil(sortedThreads.length / pageSize) || 1);
              }
            } 
            // Paginated response format
            else if (typeof result.data === 'object' && result.data.content) {
              const paginatedData = result.data as PaginatedResponse;
              
              if (isClientFiltering) {
                // If we already have all threads in our state, use those
                // Otherwise use what we got from the paginated response
                const threadsToFilter = allThreads.length > 0 
                  ? allThreads 
                  : applySorting(paginatedData.content);
                
                // Apply client-side filters
                let filteredThreads = threadsToFilter;
                
                // Filter by tags
                if (selectedTags.length > 0) {
                  filteredThreads = filteredThreads.filter(thread => 
                    thread.tags.some(tag => selectedTags.includes(tag.id))
                  );
                }
                
                // Filter by date
                if (dateFilter !== 'all') {
                  const now = new Date();
                  let cutoffDate = new Date();
                  
                  switch(dateFilter) {
                    case 'today':
                      cutoffDate.setDate(now.getDate() - 1);
                      break;
                    case 'week':
                      cutoffDate.setDate(now.getDate() - 7);
                      break;
                    case 'month':
                      cutoffDate.setMonth(now.getMonth() - 1);
                      break;
                    case 'year':
                      cutoffDate.setFullYear(now.getFullYear() - 1);
                      break;
                  }
                  
                  filteredThreads = filteredThreads.filter(thread => 
                    new Date(thread.createdAt) >= cutoffDate
                  );
                }
                
                // Calculate total pages based on filtered threads
                const totalFilteredThreads = filteredThreads.length;
                setTotalPages(Math.ceil(totalFilteredThreads / pageSize) || 1);
                
                // Apply client-side pagination
                const startIndex = (currentPage - 1) * pageSize;
                const paginatedThreads = filteredThreads.slice(startIndex, startIndex + pageSize);
                
                setThreads(paginatedThreads);
              } else {
                // No client filtering, use server pagination
                // Still sort client-side to ensure it's working
                const sortedContent = applySorting(paginatedData.content);
                setThreads(sortedContent);
                setTotalPages(paginatedData.totalPages || 1);
              }
            }
            // Unknown format, use as-is
            else {
              setThreads(result.data);
              setTotalPages(1);
            }
          } else {
            console.error('Unexpected response format:', result);
            setError('Invalid response format from server');
          }
        } else {
          setError('Failed to fetch threads');
        }
      } catch (err) {
        console.error('Error fetching threads:', err);
        setError('An error occurred while fetching threads');
      } finally {
        setLoading(false);
      }
    };

    fetchThreads();
  }, [currentPage, pageSize, selectedTags, dateFilter, sortBy, allThreads.length]);

  // Handle page change
  const handlePageChange = (newPage: number) => {
    if (newPage < 1 || newPage > totalPages) return;
    setSearchParams({ page: newPage.toString() });
    window.scrollTo(0, 0);
  };
  
  // Handle tag selection
  const handleTagSelection = (tagId: number) => {
    if (selectedTags.includes(tagId)) {
      setSelectedTags(selectedTags.filter(id => id !== tagId));
    } else {
      setSelectedTags([...selectedTags, tagId]);
    }
    // Reset to page 1 when changing filters
    setSearchParams({ page: '1' });
  };
  
  // Reset all filters
  const resetFilters = () => {
    setSelectedTags([]);
    setDateFilter('all');
    setSortBy('newest');
    setSearchParams({ page: '1' });
  };

  // Filter tags based on search query
  const getFilteredTags = () => {
    if (tagSearchQuery.trim() === '') {
      return availableTags;
    } else {
      return availableTags.filter(tag => 
        tag.label.toLowerCase().includes(tagSearchQuery.toLowerCase())
      );
    }
  };

  // Generate pagination controls
  const renderPagination = () => {
    if (totalPages <= 1) return null;
    
    return (
      <div className="flex justify-center mt-8">
        <nav className="flex items-center space-x-2">
          <button
            onClick={() => handlePageChange(currentPage - 1)}
            disabled={currentPage === 1}
            className={`p-2 rounded ${
              currentPage === 1 
                ? 'text-gray-400 cursor-not-allowed' 
                : 'text-blue-600 hover:bg-blue-50'
            }`}
            aria-label="Previous page"
          >
            <FaChevronLeft />
          </button>
          
          {/* Show page numbers with ellipsis for large page counts */}
          {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
            let pageNum;
            
            // Calculate which page numbers to show
            if (totalPages <= 5) {
              // Show all pages if 5 or fewer
              pageNum = i + 1;
            } else if (currentPage <= 3) {
              // At start, show first 5 pages
              pageNum = i + 1;
            } else if (currentPage >= totalPages - 2) {
              // At end, show last 5 pages
              pageNum = totalPages - 4 + i;
            } else {
              // In middle, show current and 2 on each side
              pageNum = currentPage - 2 + i;
            }
            
            return (
              <button
                key={pageNum}
                onClick={() => handlePageChange(pageNum)}
                className={`w-10 h-10 rounded ${
                  currentPage === pageNum 
                    ? 'bg-blue-600 text-white' 
                    : 'text-gray-700 hover:bg-blue-50'
                }`}
              >
                {pageNum}
              </button>
            );
          })}
          
          <button
            onClick={() => handlePageChange(currentPage + 1)}
            disabled={currentPage === totalPages}
            className={`p-2 rounded ${
              currentPage === totalPages 
                ? 'text-gray-400 cursor-not-allowed' 
                : 'text-blue-600 hover:bg-blue-50'
            }`}
            aria-label="Next page"
          >
            <FaChevronRight />
          </button>
        </nav>
      </div>
    );
  };

  // Render the thread filter sidebar
  const renderFilterSidebar = () => {
    // Get filtered tags based on search
    const filteredTags = getFilteredTags();
    
    return (
      <div className="w-full lg:w-64 bg-white rounded-xl shadow-sm p-4">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-gray-800">Filters</h2>
          {(selectedTags.length > 0 || dateFilter !== 'all' || sortBy !== 'newest') && (
            <button 
              onClick={resetFilters}
              className="text-sm text-blue-600 hover:text-blue-800 flex items-center"
            >
              <FaTimesCircle className="mr-1" />
              Reset All
            </button>
          )}
        </div>
        
        {/* Sort By Filter */}
        <div className="mb-6">
          <h3 className="flex items-center text-sm font-medium text-gray-700 mb-2">
            <FaSort className="mr-2 text-gray-500" />
            Sort By
          </h3>
          <div className="space-y-2">
            <label className="flex items-center">
              <input 
                type="radio" 
                name="sortBy" 
                value="newest" 
                checked={sortBy === 'newest'}
                onChange={(e) => {
                  setSortBy(e.target.value);
                  setSearchParams({ page: '1' });
                }}
                className="mr-2"
              />
              <span className="text-sm text-gray-700">Newest First</span>
            </label>
            <label className="flex items-center">
              <input 
                type="radio" 
                name="sortBy" 
                value="oldest" 
                checked={sortBy === 'oldest'}
                onChange={(e) => {
                  setSortBy(e.target.value);
                  setSearchParams({ page: '1' });
                }}
                className="mr-2"
              />
              <span className="text-sm text-gray-700">Oldest First</span>
            </label>
            <label className="flex items-center">
              <input 
                type="radio" 
                name="sortBy" 
                value="mostVoted" 
                checked={sortBy === 'mostVoted'}
                onChange={(e) => {
                  setSortBy(e.target.value);
                  setSearchParams({ page: '1' });
                }}
                className="mr-2"
              />
              <span className="text-sm text-gray-700">Most Voted</span>
            </label>
            <label className="flex items-center">
              <input 
                type="radio" 
                name="sortBy" 
                value="leastVoted" 
                checked={sortBy === 'leastVoted'}
                onChange={(e) => {
                  setSortBy(e.target.value);
                  setSearchParams({ page: '1' });
                }}
                className="mr-2"
              />
              <span className="text-sm text-gray-700">Least Voted</span>
            </label>
          </div>
        </div>
        
        {/* Date Filter */}
        <div className="mb-6">
          <h3 className="flex items-center text-sm font-medium text-gray-700 mb-2">
            <FaCalendarAlt className="mr-2 text-gray-500" />
            Created Date
          </h3>
          <div className="space-y-2">
            <label className="flex items-center">
              <input 
                type="radio" 
                name="dateFilter" 
                value="all" 
                checked={dateFilter === 'all'}
                onChange={(e) => {
                  setDateFilter(e.target.value);
                  setSearchParams({ page: '1' });
                }}
                className="mr-2"
              />
              <span className="text-sm text-gray-700">All Time</span>
            </label>
            <label className="flex items-center">
              <input 
                type="radio" 
                name="dateFilter" 
                value="today" 
                checked={dateFilter === 'today'}
                onChange={(e) => {
                  setDateFilter(e.target.value);
                  setSearchParams({ page: '1' });
                }}
                className="mr-2"
              />
              <span className="text-sm text-gray-700">Last 24 Hours</span>
            </label>
            <label className="flex items-center">
              <input 
                type="radio" 
                name="dateFilter" 
                value="week" 
                checked={dateFilter === 'week'}
                onChange={(e) => {
                  setDateFilter(e.target.value);
                  setSearchParams({ page: '1' });
                }}
                className="mr-2"
              />
              <span className="text-sm text-gray-700">Last Week</span>
            </label>
            <label className="flex items-center">
              <input 
                type="radio" 
                name="dateFilter" 
                value="month" 
                checked={dateFilter === 'month'}
                onChange={(e) => {
                  setDateFilter(e.target.value);
                  setSearchParams({ page: '1' });
                }}
                className="mr-2"
              />
              <span className="text-sm text-gray-700">Last Month</span>
            </label>
            <label className="flex items-center">
              <input 
                type="radio" 
                name="dateFilter" 
                value="year" 
                checked={dateFilter === 'year'}
                onChange={(e) => {
                  setDateFilter(e.target.value);
                  setSearchParams({ page: '1' });
                }}
                className="mr-2"
              />
              <span className="text-sm text-gray-700">Last Year</span>
            </label>
          </div>
        </div>
        
        {/* Tags Filter */}
        <div className="mb-4">
          <h3 className="flex items-center text-sm font-medium text-gray-700 mb-2">
            <FaTags className="mr-2 text-gray-500" />
            Tags
          </h3>
          
          {/* Tag search input */}
          <div className="relative mb-2">
            <input
              type="text"
              placeholder="Search tags..."
              value={tagSearchQuery}
              onChange={(e) => setTagSearchQuery(e.target.value)}
              className="w-full pl-8 pr-3 py-1.5 text-sm border rounded-md focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
            <FaSearch className="absolute left-2.5 top-2 text-gray-400 text-sm" />
            {tagSearchQuery && (
              <button
                onClick={() => setTagSearchQuery('')}
                className="absolute right-2 top-1.5 text-gray-400 hover:text-gray-600"
              >
                <FaTimesCircle className="text-sm" />
              </button>
            )}
          </div>
          
          {/* Selected tags section */}
          {selectedTags.length > 0 && (
            <div className="mb-2">
              <div className="text-xs text-gray-500 mb-1">Selected:</div>
              <div className="flex flex-wrap gap-1.5">
                {selectedTags.map(tagId => {
                  const tag = availableTags.find(t => t.id === tagId);
                  if (!tag) return null;
                  
                  return (
                    <div key={tag.id} className="flex items-center">
                      <Tag
                        tag={tag}
                        onRemove={() => handleTagSelection(tag.id)}
                      />
                    </div>
                  );
                })}
              </div>
            </div>
          )}
          
          {/* Tags list */}
          <div className="bg-gray-50 p-2 rounded max-h-48 overflow-y-auto border border-gray-100">
            {loadingTags ? (
              <div className="text-center py-4 text-gray-500 text-sm">Loading tags...</div>
            ) : filteredTags.length === 0 ? (
              <div className="text-center py-4 text-gray-500 text-sm">
                {availableTags.length === 0 ? "No tags available" : "No tags match your search"}
              </div>
            ) : (
              <div className="flex flex-wrap gap-1.5">
                {filteredTags
                  .filter(tag => !selectedTags.includes(tag.id)) // Don't show already selected tags
                  .map(tag => (
                    <div 
                      key={tag.id} 
                      onClick={() => handleTagSelection(tag.id)}
                      className="cursor-pointer"
                    >
                      <Tag tag={tag} />
                    </div>
                  ))
                }
              </div>
            )}
          </div>
        </div>
      </div>
    );
  };

  return (
    <MainLayout>
      {() => (
        <div className="flex flex-col lg:flex-row gap-6">
          {/* Main Content - Now first in order */}
          <div className="flex-1">
            <section className="bg-white rounded-xl shadow-sm p-6">
              <div className="flex items-center justify-between mb-6">
                <h1 className="text-2xl font-semibold text-gray-800">All Threads</h1>
                <div className="text-sm text-gray-500">
                  {threads.length > 0 && `Showing page ${currentPage} of ${totalPages}`}
                </div>
              </div>
              
              {/* Filter Sidebar - Shown on mobile */}
              <div className="lg:hidden mb-6">
                {renderFilterSidebar()}
              </div>
              
              {loading ? (
                <div className="text-center py-20 text-gray-500">Loading threads...</div>
              ) : error ? (
                <div className="bg-red-50 text-red-600 p-4 rounded-lg">{error}</div>
              ) : threads.length === 0 ? (
                <div className="text-center py-20 text-gray-500">No threads found matching your filters</div>
              ) : (
                <>
                  <div className="space-y-4">
                    {threads.map(thread => (
                      <ThreadCard key={thread.id} thread={thread} />
                    ))}
                  </div>
                  {renderPagination()}
                </>
              )}
            </section>
          </div>
          
          {/* Filter Sidebar - Now second in order and shown on desktop */}
          <div className="hidden lg:block lg:w-64 sticky top-24 self-start">
            {renderFilterSidebar()}
          </div>
        </div>
      )}
    </MainLayout>
  );
};

export default ThreadsList; 