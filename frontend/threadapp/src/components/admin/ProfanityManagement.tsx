import React, { useState, useEffect, useCallback } from 'react';
import { profanityService } from '../../services/profanityService';
import MainLayout from '../layout/MainLayout';
import { FaTimesCircle, FaPlus, FaSync, FaCheck, FaTrash, FaSearch, FaEye, FaEyeSlash } from 'react-icons/fa';
import { User } from '../layout/MainLayout';

const ProfanityManagement: React.FC = () => {
  const [words, setWords] = useState<string[]>([]);
  const [newWord, setNewWord] = useState('');
  const [testText, setTestText] = useState('');
  const [loading, setLoading] = useState(false);
  const [reloading, setReloading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [testResult, setTestResult] = useState<boolean | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [tab, setTab] = useState<'english' | 'turkish'>('english');
  const [wordsBlurred, setWordsBlurred] = useState<boolean>(true);
  const [showConfirmDialog, setShowConfirmDialog] = useState<boolean>(false);

  // Fetch all profanity words
  const fetchWords = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const wordList = await profanityService.getAllWords();
      setWords(wordList);
    } catch (err) {
      setError('Failed to load profanity words. Please try again.');
      console.error('Error fetching profanity words:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchWords();
  }, [fetchWords]);

  // Filter words based on search term and current tab
  const filteredWords = words.filter(word => {
    const matchesSearch = searchTerm === '' || word.toLowerCase().includes(searchTerm.toLowerCase());
    
    // Simple heuristic to determine if a word is likely Turkish
    // Turkish characters: ç, ş, ğ, ü, ö, ı, İ
    const turkishChars = /[çşğüöıİ]/i;
    const isTurkish = turkishChars.test(word);
    
    if (tab === 'turkish') {
      return matchesSearch && isTurkish;
    } else {
      return matchesSearch && !isTurkish;
    }
  });

  // Add a new word
  const handleAddWord = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newWord.trim()) return;
    
    setLoading(true);
    setError(null);
    setSuccess(null);
    
    try {
      // Use the current tab to determine the language
      const language = tab === 'turkish' ? 'tr' : 'en';
      
      await profanityService.addWord(newWord.trim(), language);
      setSuccess(`Word "${newWord}" added successfully to ${tab === 'turkish' ? 'Turkish' : 'English'} list`);
      setNewWord('');
      fetchWords(); // Reload the word list
    } catch (err) {
      setError('Failed to add word. Please try again.');
      console.error('Error adding profanity word:', err);
    } finally {
      setLoading(false);
    }
  };

  // Remove a word
  const handleRemoveWord = async (wordToRemove: string) => {
    if (!window.confirm(`Are you sure you want to remove "${wordToRemove}" from the profanity list?`)) {
      return;
    }
    
    setLoading(true);
    setError(null);
    setSuccess(null);
    
    try {
      // Step 1: Remove the word
      await profanityService.removeWord(wordToRemove);
      
      // Step 2: Force reload the filter to update server memory
      await profanityService.reloadFilter();
      
      // Step 3: Test the word to verify it's no longer detected as profanity
      const stillDetected = await profanityService.checkText(wordToRemove);
      
      // Update UI
      setWords(words.filter(word => word !== wordToRemove));
      
      if (stillDetected) {
        setError(`Warning: The word "${wordToRemove}" was removed but may still be detected by the filter. Please try restarting the server.`);
      } else {
        setSuccess(`Word "${wordToRemove}" successfully removed and verified.`);
      }
      
      // Refresh the word list from server
      fetchWords();
      
    } catch (err) {
      setError('Failed to remove word. Please try again.');
      console.error('Error removing profanity word:', err);
    } finally {
      setLoading(false);
    }
  };

  // Test text for profanity
  const handleTestText = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!testText.trim()) return;
    
    setLoading(true);
    setError(null);
    setTestResult(null);
    
    try {
      const result = await profanityService.checkText(testText);
      setTestResult(result);
    } catch (err) {
      setError('Failed to test text. Please try again.');
      console.error('Error testing text for profanity:', err);
    } finally {
      setLoading(false);
    }
  };

  // Reload the profanity filter
  const handleReloadFilter = async () => {
    if (!window.confirm('Are you sure you want to reload the profanity filter? This will reload words from disk and may discard unsaved changes.')) {
      return;
    }
    
    setReloading(true);
    setError(null);
    setSuccess(null);
    
    try {
      await profanityService.reloadFilter();
      setSuccess('Profanity filter reloaded successfully');
      fetchWords(); // Reload the word list
    } catch (err) {
      setError('Failed to reload profanity filter. Please try again.');
      console.error('Error reloading profanity filter:', err);
    } finally {
      setReloading(false);
    }
  };

  // Toggle blur effect on words
  const handleToggleBlur = () => {
    if (wordsBlurred) {
      setShowConfirmDialog(true);
    } else {
      setWordsBlurred(true);
    }
  };

  // Confirm viewing unblurred words
  const handleConfirmView = () => {
    setWordsBlurred(false);
    setShowConfirmDialog(false);
  };

  // Clear notifications after 5 seconds
  useEffect(() => {
    if (success) {
      const timer = setTimeout(() => {
        setSuccess(null);
      }, 5000);
      return () => clearTimeout(timer);
    }
    if (error) {
      const timer = setTimeout(() => {
        setError(null);
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [success, error]);

  // Render the content for MainLayout
  const renderContent = () => {
    
    return (
      <div className="container mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold mb-6">Profanity Filter Management</h1>
        <p className="text-gray-600 mb-8">
          Manage the profanity filter by adding or removing words. Changes take effect immediately.
        </p>

        {/* Notification area */}
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-800 px-4 py-3 rounded-lg mb-6 flex items-center">
            <FaTimesCircle className="text-red-600 mr-2" />
            <span>{error}</span>
          </div>
        )}
        {success && (
          <div className="bg-green-50 border border-green-200 text-green-800 px-4 py-3 rounded-lg mb-6 flex items-center">
            <FaCheck className="text-green-600 mr-2" />
            <span>{success}</span>
          </div>
        )}

        {/* Confirmation dialog */}
        {showConfirmDialog && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
              <h3 className="text-xl font-bold mb-4">Warning: Sensitive Content</h3>
              <p className="mb-6">
                You are about to view explicit and potentially offensive content. Are you sure you want to proceed?
              </p>
              <div className="flex justify-end space-x-3">
                <button
                  onClick={() => setShowConfirmDialog(false)}
                  className="px-4 py-2 bg-gray-200 text-gray-800 rounded hover:bg-gray-300"
                >
                  Cancel
                </button>
                <button
                  onClick={handleConfirmView}
                  className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
                >
                  Show Content
                </button>
              </div>
            </div>
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Left column */}
          <div className="bg-white rounded-lg shadow-md p-6">
            <div className="mb-6">
              <h2 className="text-xl font-semibold mb-4">Add New Word</h2>
              <form onSubmit={handleAddWord} className="flex">
                <input
                  type="text"
                  className="flex-grow px-4 py-2 border border-gray-300 rounded-l-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Enter a new profanity word"
                  value={newWord}
                  onChange={(e) => setNewWord(e.target.value)}
                />
                <button
                  type="submit"
                  className="bg-blue-600 text-white px-4 py-2 rounded-r-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 flex items-center"
                  disabled={loading || !newWord.trim()}
                >
                  <FaPlus className="mr-2" />
                  Add
                </button>
              </form>
            </div>

            <div className="mb-6">
              <h2 className="text-xl font-semibold mb-4">Test Text</h2>
              <form onSubmit={handleTestText}>
                <textarea
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 mb-3"
                  placeholder="Enter text to test for profanity"
                  value={testText}
                  onChange={(e) => setTestText(e.target.value)}
                  rows={4}
                />
                <button
                  type="submit"
                  className="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-green-500 flex items-center"
                  disabled={loading || !testText.trim()}
                >
                  <FaSearch className="mr-2" />
                  Test Text
                </button>
              </form>
              {testResult !== null && (
                <div className={`mt-4 p-3 rounded-lg ${testResult ? 'bg-red-50 text-red-800 border border-red-200' : 'bg-green-50 text-green-800 border border-green-200'}`}>
                  {testResult ? 'Profanity detected in text.' : 'No profanity detected in text.'}
                </div>
              )}
            </div>

            <div>
              <button
                onClick={handleReloadFilter}
                className="bg-purple-600 text-white px-4 py-2 rounded-lg hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-purple-500 flex items-center"
                disabled={reloading}
              >
                <FaSync className={`mr-2 ${reloading ? 'animate-spin' : ''}`} />
                Reload Filter from Disk
              </button>
              <p className="text-sm text-gray-500 mt-2">
                This will reload the profanity filter from disk and may discard unsaved changes.
              </p>
            </div>
          </div>

          {/* Right column */}
          <div className="bg-white rounded-lg shadow-md p-6">
            <div className="mb-4 flex justify-between items-center">
              <h2 className="text-xl font-semibold">Profanity Word List</h2>
              
              <div className="flex space-x-2">
                <button 
                  className={`px-3 py-1 rounded ${tab === 'english' ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-700'}`}
                  onClick={() => setTab('english')}
                >
                  English
                </button>
                <button 
                  className={`px-3 py-1 rounded ${tab === 'turkish' ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-700'}`}
                  onClick={() => setTab('turkish')}
                >
                  Turkish
                </button>
              </div>
            </div>
            
            <div className="mb-4 flex justify-between items-center">
              <div className="relative flex-grow mr-2">
                <input
                  type="text"
                  className="w-full px-4 py-2 pl-10 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Search words..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
                <FaSearch className="absolute left-3 top-3 text-gray-400" />
              </div>
              
              <button
                onClick={handleToggleBlur}
                className={`ml-2 p-2 rounded-lg ${wordsBlurred ? 'bg-yellow-100 text-yellow-700' : 'bg-red-100 text-red-700'} hover:opacity-80`}
                title={wordsBlurred ? 'Show words' : 'Blur words'}
              >
                {wordsBlurred ? <FaEye /> : <FaEyeSlash />}
              </button>
            </div>
            
            {loading ? (
              <div className="flex justify-center items-center h-64">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
              </div>
            ) : (
              <>
                {filteredWords.length === 0 ? (
                  <div className="text-center py-8 text-gray-500">
                    {searchTerm ? 'No matching profanity words found.' : 'No profanity words in the database.'}
                  </div>
                ) : (
                  <div className="mt-4 h-[450px] overflow-y-auto pr-4">
                    <ul className="divide-y divide-gray-200">
                      {filteredWords.map((word, index) => (
                        <li key={index} className="py-3 flex justify-between items-center">
                          <span 
                            className={`font-mono truncate mr-6 max-w-[80%] ${wordsBlurred ? 'filter blur-sm hover:blur-sm select-none' : ''}`}
                          >
                            {word}
                          </span>
                          <button
                            onClick={() => handleRemoveWord(word)}
                            className="text-red-600 hover:text-red-800 focus:outline-none flex-shrink-0 ml-auto"
                            title="Remove word"
                            aria-label="Remove word"
                          >
                            <FaTrash />
                          </button>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </div>
    );
  };

  return <MainLayout>{renderContent()}</MainLayout>;
};

export default ProfanityManagement; 