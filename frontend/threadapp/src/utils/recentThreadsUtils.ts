import { getCurrentUserId } from './authUtils';

interface RecentThread {
  id: number;
  title: string;
  timestamp: number; // For sorting by most recent
}

const BASE_STORAGE_KEY = 'recent_threads';
const MAX_RECENT_THREADS = 5;
const EXPIRATION_DAYS = 30; // Number of days to keep items in the recent list

/**
 * Gets the user-specific storage key
 */
const getUserStorageKey = (): string => {
  const userId = getCurrentUserId();
  return userId ? `${BASE_STORAGE_KEY}_${userId}` : BASE_STORAGE_KEY;
};

/**
 * Adds a thread to the recently viewed threads list in local storage
 */
export const addToRecentThreads = (threadId: number, threadTitle: string): void => {
  try {
    const storageKey = getUserStorageKey();
    
    // Get existing recent threads
    const existingThreadsJson = localStorage.getItem(storageKey);
    const recentThreads: RecentThread[] = existingThreadsJson 
      ? JSON.parse(existingThreadsJson) 
      : [];
    
    // Check if this thread is already in the list
    const existingIndex = recentThreads.findIndex(t => t.id === threadId);
    
    if (existingIndex !== -1) {
      // If thread already exists, remove it so we can add it to the top (most recent)
      recentThreads.splice(existingIndex, 1);
    }
    
    // Add the thread to the beginning of the array
    recentThreads.unshift({
      id: threadId,
      title: threadTitle,
      timestamp: Date.now()
    });
    
    // Limit to MAX_RECENT_THREADS
    const limitedThreads = recentThreads.slice(0, MAX_RECENT_THREADS);
    
    // Save back to local storage
    localStorage.setItem(storageKey, JSON.stringify(limitedThreads));
  } catch (error) {
    console.error('Error saving recent thread to local storage:', error);
  }
};

/**
 * Gets the list of recently viewed threads from local storage
 * Automatically filters out expired entries
 */
export const getRecentThreads = (): RecentThread[] => {
  try {
    const storageKey = getUserStorageKey();
    const threadsJson = localStorage.getItem(storageKey);
    if (!threadsJson) return [];
    
    const threads: RecentThread[] = JSON.parse(threadsJson);
    
    // Calculate expiration timestamp (current time - EXPIRATION_DAYS in milliseconds)
    const now = Date.now();
    const expirationTime = now - (EXPIRATION_DAYS * 24 * 60 * 60 * 1000);
    
    // Filter out expired threads
    const validThreads = threads.filter(thread => thread.timestamp > expirationTime);
    
    // If some threads were expired, update the storage
    if (validThreads.length !== threads.length) {
      localStorage.setItem(storageKey, JSON.stringify(validThreads));
    }
    
    return validThreads;
  } catch (error) {
    console.error('Error retrieving recent threads from local storage:', error);
    return [];
  }
};

/**
 * Clears the list of recently viewed threads
 */
export const clearRecentThreads = (): void => {
  const storageKey = getUserStorageKey();
  localStorage.removeItem(storageKey);
}; 