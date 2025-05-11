import { useState, useEffect } from 'react';
import MainLayout from '../layout/MainLayout';
import type { User } from '../layout/MainLayout';
import { API_ENDPOINTS } from '../../config/config';
import { fetchWithAuth } from '../../utils/authUtils';
import ThreadCard from '../threads/ThreadCard';
import { FaFire, FaLightbulb } from 'react-icons/fa';

// Session storage key for tracking if vote counts have been reset
const VOTE_COUNTS_RESET_KEY = 'vote_counts_reset';

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

const Home = () => {
  const [hotThreads, setHotThreads] = useState<Thread[]>([]);
  const [recommendedThreads, setRecommendedThreads] = useState<Thread[]>([]);
  const [loadingHot, setLoadingHot] = useState(false);
  const [loadingRecommended, setLoadingRecommended] = useState(false);
  const [errorHot, setErrorHot] = useState<string | null>(null);
  const [errorRecommended, setErrorRecommended] = useState<string | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  // Reset vote counts once per session when the home page loads
  useEffect(() => {
    const resetVoteCounts = async () => {
      // Only reset once per browser session
      if (!sessionStorage.getItem(VOTE_COUNTS_RESET_KEY)) {
        try {
          console.log('Resetting vote counts...');
          const response = await fetchWithAuth(API_ENDPOINTS.votes.resetAllVoteCounts, {
            method: 'POST'
          });
          
          if (response.ok) {
            console.log('Vote counts successfully reset');
            // Mark that we've reset the counts in this session
            sessionStorage.setItem(VOTE_COUNTS_RESET_KEY, 'true');
          } else {
            console.error('Failed to reset vote counts');
          }
        } catch (err) {
          console.error('Error resetting vote counts:', err);
        }
      }
    };

    // Call reset function
    resetVoteCounts();
  }, []);

  // Check if user is authenticated
  useEffect(() => {
    const checkAuth = async () => {
      try {
        const response = await fetchWithAuth(API_ENDPOINTS.users.me);
        setIsAuthenticated(response.ok);
      } catch (err) {
        console.error('Error checking authentication:', err);
        setIsAuthenticated(false);
      }
    };

    checkAuth();
  }, []);

  // Fetch hot threads
  useEffect(() => {
    const fetchHotThreads = async () => {
      setLoadingHot(true);
      setErrorHot(null);
      
      try {
        const response = await fetchWithAuth(API_ENDPOINTS.analytics.hotThreads(7, 6));
        
        if (response.ok) {
          const { data } = await response.json();
          setHotThreads(data);
        } else {
          setErrorHot('Failed to fetch hot threads');
        }
      } catch (err) {
        console.error('Error fetching hot threads:', err);
        setErrorHot('An error occurred while fetching hot threads');
      } finally {
        setLoadingHot(false);
      }
    };

    fetchHotThreads();
  }, []);

  // Fetch recommended threads if authenticated
  useEffect(() => {
    if (!isAuthenticated) return;
    
    const fetchRecommendedThreads = async () => {
      setLoadingRecommended(true);
      setErrorRecommended(null);
      
      try {
        const response = await fetchWithAuth(API_ENDPOINTS.analytics.recommendedThreads(6));
        
        if (response.ok) {
          const { data } = await response.json();
          setRecommendedThreads(data);
        } else {
          setErrorRecommended('Failed to fetch recommended threads');
        }
      } catch (err) {
        console.error('Error fetching recommended threads:', err);
        setErrorRecommended('An error occurred while fetching recommended threads');
      } finally {
        setLoadingRecommended(false);
      }
    };

    fetchRecommendedThreads();
  }, [isAuthenticated]);

  return (
    <MainLayout>
      <div className="space-y-8">
        <section className="bg-white rounded-xl shadow-sm p-8 mb-8">
          <h1 className="text-3xl font-semibold text-gray-900 mb-2">
            Welcome to the Connect The Dots!
          </h1>
          <p className="text-lg text-gray-600">
            Start exploring and connecting with others.
          </p>
        </section>
        
        {/* Hot Threads Section */}
        <section className="bg-white rounded-xl shadow-sm p-6">
          <div className="flex items-center mb-6">
            <FaFire className="text-orange-500 mr-2" size={24} />
            <h2 className="text-2xl font-semibold text-gray-800">Hot Threads</h2>
          </div>
          
          {loadingHot ? (
            <div className="text-center py-8 text-gray-500">Loading hot threads...</div>
          ) : errorHot ? (
            <div className="bg-red-50 text-red-600 p-4 rounded-lg">{errorHot}</div>
          ) : hotThreads.length === 0 ? (
            <div className="text-center py-8 text-gray-500">No hot threads found</div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {hotThreads.map(thread => (
                <ThreadCard key={thread.id} thread={thread} />
              ))}
            </div>
          )}
        </section>
        
        {/* Recommended Threads Section (Only shows for authenticated users) */}
        {isAuthenticated && (
          <section className="bg-white rounded-xl shadow-sm p-6">
            <div className="flex items-center mb-6">
              <FaLightbulb className="text-yellow-500 mr-2" size={24} />
              <h2 className="text-2xl font-semibold text-gray-800">You Might Be Interested In</h2>
            </div>
            
            {loadingRecommended ? (
              <div className="text-center py-8 text-gray-500">Loading recommendations...</div>
            ) : errorRecommended ? (
              <div className="bg-red-50 text-red-600 p-4 rounded-lg">{errorRecommended}</div>
            ) : recommendedThreads.length === 0 ? (
              <div className="text-center py-8 text-gray-500">
                No recommendations found. Try following some threads or users to get personalized recommendations.
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {recommendedThreads.map(thread => (
                  <ThreadCard key={thread.id} thread={thread} />
                ))}
              </div>
            )}
          </section>
        )}
      </div>
    </MainLayout>
  );
};

export default Home; 