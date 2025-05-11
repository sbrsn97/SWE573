import { useState, useEffect, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { FaPlus, FaBookmark, FaRegClock, FaChevronDown, FaChevronUp } from 'react-icons/fa';
import CreateThreadModal from '../threads/CreateThreadModal';
import { API_ENDPOINTS } from '../../config/config';
import { fetchWithAuth, handleAuthError } from '../../utils/authUtils';
import eventBus, { EVENTS } from '../../utils/eventBus';
import { getRecentThreads } from '../../utils/recentThreadsUtils';

interface Thread {
  id: number;
  title: string;
  description: string | null;
  authorId: number;
}

interface RecentThread {
  id: number;
  title: string;
  timestamp: number;
}

const Sidebar = () => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [followedThreads, setFollowedThreads] = useState<Thread[]>([]);
  const [recentThreads, setRecentThreads] = useState<RecentThread[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAllFollowed, setShowAllFollowed] = useState(false);
  const [showAllRecent, setShowAllRecent] = useState(false);
  const navigate = useNavigate();

  const fetchFollowedThreads = async () => {
    try {
      setLoading(true);
      const response = await fetchWithAuth(API_ENDPOINTS.threads.following);
      
      if (!response.ok) {
        if (handleAuthError(response, navigate)) return;
        console.error('Failed to fetch followed threads');
        return;
      }
      
      const { data } = await response.json();
      setFollowedThreads(data);
    } catch (err) {
      console.error('Error fetching followed threads:', err);
    } finally {
      setLoading(false);
    }
  };
  
  // Load recent threads from local storage
  const loadRecentThreads = useCallback(() => {
    const threads = getRecentThreads();
    setRecentThreads(threads);
  }, []);

  // Memoize the fetchFollowedThreads function with useCallback to use in event listeners
  const refreshFollowedThreads = useCallback(() => {
    fetchFollowedThreads();
  }, []);

  // Handle the thread viewed event
  const handleThreadViewed = useCallback(() => {
    // Reload recent threads from local storage
    loadRecentThreads();
  }, [loadRecentThreads]);

  useEffect(() => {
    fetchFollowedThreads();
    loadRecentThreads();
    
    // Add event listeners for thread follow/unfollow events
    eventBus.on(EVENTS.THREAD_FOLLOWED, refreshFollowedThreads);
    eventBus.on(EVENTS.THREAD_UNFOLLOWED, refreshFollowedThreads);
    
    // Add event listener for thread viewed events
    eventBus.on(EVENTS.THREAD_VIEWED, handleThreadViewed);
    
    // Listen for route changes to update recent threads
    window.addEventListener('popstate', loadRecentThreads);
    
    // Cleanup event listeners on component unmount
    return () => {
      eventBus.off(EVENTS.THREAD_FOLLOWED, refreshFollowedThreads);
      eventBus.off(EVENTS.THREAD_UNFOLLOWED, refreshFollowedThreads);
      eventBus.off(EVENTS.THREAD_VIEWED, handleThreadViewed);
      window.removeEventListener('popstate', loadRecentThreads);
    };
  }, [navigate, refreshFollowedThreads, loadRecentThreads, handleThreadViewed]);

  const handleThreadCreated = (threadId: number) => {
    // Refresh the followed threads list when a new thread is created
    fetchFollowedThreads();
    navigate(`/threads/${threadId}`);
  };

  // Display only 5 threads initially unless showing all
  const displayFollowedThreads = showAllFollowed ? followedThreads : followedThreads.slice(0, 5);
  const hasMoreFollowedThreads = followedThreads.length > 5;
  
  // Display only 5 recent threads initially unless showing all
  const displayRecentThreads = showAllRecent ? recentThreads : recentThreads.slice(0, 5);
  const hasMoreRecentThreads = recentThreads.length > 5;

  return (
    <div className="fixed left-0 top-[60px] h-[calc(100vh-60px)] w-64 bg-white shadow-sm border-r border-gray-200 overflow-y-auto">
      <div className="p-4">
        <button
          onClick={() => setIsModalOpen(true)}
          className="flex items-center justify-center gap-2 w-full bg-blue-600 text-white py-2 px-4 rounded-lg hover:bg-blue-700 transition-colors duration-200 shadow-sm hover:shadow-md"
        >
          <FaPlus className="text-lg text-white" />
          <span className="font-medium text-white">New Thread</span>
        </button>
      </div>
      
      {/* Followed Threads Section */}
      <div className="mt-4">
        <div className="px-4 py-2 flex items-center gap-2 border-b border-gray-100">
          <FaBookmark className="text-blue-500" />
          <h3 className="font-medium text-gray-700">Followed Threads</h3>
        </div>
        
        <div className="py-2">
          {loading ? (
            <div className="px-4 py-2 text-sm text-gray-500">Loading...</div>
          ) : followedThreads.length === 0 ? (
            <div className="px-4 py-2 text-sm text-gray-500">
              You're not following any threads yet.
            </div>
          ) : (
            <>
              <ul className="space-y-1">
                {displayFollowedThreads.map(thread => (
                  <li key={thread.id}>
                    <Link
                      to={`/threads/${thread.id}`}
                      className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 truncate"
                      title={thread.title}
                    >
                      {thread.title}
                    </Link>
                  </li>
                ))}
              </ul>
              
              {hasMoreFollowedThreads && (
                <button
                  onClick={() => setShowAllFollowed(!showAllFollowed)}
                  className="w-full flex items-center justify-center gap-1 text-sm text-blue-600 mt-2 py-1 hover:bg-gray-50"
                >
                  {showAllFollowed ? (
                    <>
                      <span>Show less</span>
                      <FaChevronUp size={12} />
                    </>
                  ) : (
                    <>
                      <span>Show all ({followedThreads.length})</span>
                      <FaChevronDown size={12} />
                    </>
                  )}
                </button>
              )}
            </>
          )}
        </div>
      </div>
      
      {/* Recent Threads Section */}
      <div className="mt-4">
        <div className="px-4 py-2 flex items-center gap-2 border-b border-gray-100">
          <FaRegClock className="text-gray-500" />
          <h3 className="font-medium text-gray-700">Recent</h3>
        </div>
        <div className="py-2">
          {recentThreads.length === 0 ? (
            <div className="px-4 py-2 text-sm text-gray-500">
              No recently viewed threads.
            </div>
          ) : (
            <>
              <ul className="space-y-1">
                {displayRecentThreads.map(thread => (
                  <li key={thread.id}>
                    <Link
                      to={`/threads/${thread.id}`}
                      className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 truncate"
                      title={thread.title}
                    >
                      {thread.title}
                    </Link>
                  </li>
                ))}
              </ul>
              
              {hasMoreRecentThreads && (
                <button
                  onClick={() => setShowAllRecent(!showAllRecent)}
                  className="w-full flex items-center justify-center gap-1 text-sm text-blue-600 mt-2 py-1 hover:bg-gray-50"
                >
                  {showAllRecent ? (
                    <>
                      <span>Show less</span>
                      <FaChevronUp size={12} />
                    </>
                  ) : (
                    <>
                      <span>Show all ({recentThreads.length})</span>
                      <FaChevronDown size={12} />
                    </>
                  )}
                </button>
              )}
            </>
          )}
        </div>
      </div>

      <CreateThreadModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onThreadCreated={handleThreadCreated}
      />
    </div>
  );
};

export default Sidebar;