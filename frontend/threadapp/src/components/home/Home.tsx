import { useEffect } from 'react';
import MainLayout from '../layout/MainLayout';
import type { User } from '../layout/MainLayout';
import { API_ENDPOINTS } from '../../config/config';
import { fetchWithAuth } from '../../utils/authUtils';

// Session storage key for tracking if vote counts have been reset
const VOTE_COUNTS_RESET_KEY = 'vote_counts_reset';

const Home = () => {
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

  return (
    <MainLayout>
      {(user: User) => (
        <section className="bg-white rounded-xl shadow-sm p-8 mb-8">
          <h1 className="text-3xl font-semibold text-gray-900 mb-2">
            Welcome {user.username} to the Discussion Platform!
          </h1>
          <p className="text-lg text-gray-600">
            Start exploring and connecting with others.
          </p>
        </section>
      )}
    </MainLayout>
  );
};

export default Home; 