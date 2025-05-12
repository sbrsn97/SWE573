import { API_ENDPOINTS } from '../config/config';

// Function to handle API response errors related to authentication
export const handleAuthError = (response: Response, navigate: Function): boolean => {
  if (response.status === 401) {
    // Token is expired or invalid - clear all local storage
    clearAllLocalStorage();
    navigate('/auth');
    return true;
  }
  return false;
};

// Function to check if token exists and is valid
export const isAuthenticated = (): boolean => {
  const token = localStorage.getItem('token');
  if (!token) return false;
  
  // You could add more validation here, like checking if token is expired
  // by decoding it (if it's a JWT) and checking exp claim
  
  return true;
};

// Function to add auth headers to fetch requests
export const fetchWithAuth = async (url: string, options: RequestInit = {}): Promise<Response> => {
  const token = localStorage.getItem('token');
  
  const headers = {
    ...options.headers,
    'Authorization': `Bearer ${token}`
  };
  
  const response = await fetch(url, {
    ...options,
    headers
  });
  
  return response;
};

// Function to clear all local storage data including recent threads
export const clearAllLocalStorage = () => {
  // Clear specific items we know about
  localStorage.removeItem('token');
  localStorage.removeItem('userId');
  
  // Clear any items that might contain thread data
  const keysToRemove = [];
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i);
    if (key && (
        key.includes('thread') || 
        key.includes('recent') || 
        key.includes('history') ||
        key.includes('viewed')
      )) {
      keysToRemove.push(key);
    }
  }
  
  // Remove the collected keys
  keysToRemove.forEach(key => localStorage.removeItem(key));
};

// Create an interceptor for all API requests to handle token expiration
export const setupAuthInterceptor = (navigate: Function) => {
  const originalFetch = window.fetch;
  
  window.fetch = async function(input: RequestInfo | URL, init?: RequestInit) {
    const response = await originalFetch(input, init);
    
    if (response.status === 401) {
      try {
        const data = await response.clone().json();
        if (data.code === 'TOKEN_EXPIRED') {
          clearAllLocalStorage();
          navigate('/auth');
        }
      } catch (e) {
        // If we can't parse JSON, just continue
      }
    }
    
    return response;
  };
};

// Function to get the current logged-in user's ID
export const getCurrentUserId = (): string | null => {
  // Try to get user ID from localStorage
  const userId = localStorage.getItem('userId');
  
  if (userId) {
    return userId;
  }
  
  return null;
};

// Function to set the current user ID in localStorage
export const setCurrentUserId = (userId: string | number): void => {
  localStorage.setItem('userId', userId.toString());
};

// Function to check if the current user is an admin
export const isAdmin = async (): Promise<boolean> => {
  try {
    const response = await fetchWithAuth(API_ENDPOINTS.users.me);
    if (!response.ok) return false;
    
    const { data } = await response.json();
    return data.role === 'ADMIN';
  } catch (error) {
    console.error('Error checking admin status:', error);
    return false;
  }
};

// Function to check if the current user is the owner of a specific thread
export const isThreadOwner = async (threadId: number): Promise<boolean> => {
  try {
    const currentUserId = localStorage.getItem('userId');
    if (!currentUserId) return false;
    
    const response = await fetchWithAuth(`${API_ENDPOINTS.threads.get(threadId)}`);
    if (!response.ok) return false;
    
    const { data } = await response.json();
    return data.authorId === parseInt(currentUserId);
  } catch (error) {
    console.error('Error checking thread ownership:', error);
    return false;
  }
};

// Function to check if user can edit a thread (is admin or owner)
export const canEditThread = async (threadId: number): Promise<boolean> => {
  const [admin, owner] = await Promise.all([
    isAdmin(),
    isThreadOwner(threadId)
  ]);
  
  return admin || owner;
};

// Function to check if the current user can interact with a thread based on visibility
export const canInteractWithThread = (
  threadStyle: string | undefined, 
  isFollowing: boolean, 
  isOwner: boolean,
  isAdmin: boolean
): boolean => {
  // Admins and owners can always interact
  if (isAdmin || isOwner) {
    return true;
  }
  
  // For PUBLIC threads, anyone can interact
  if (!threadStyle || threadStyle === 'PUBLIC') {
    return true;
  }
  
  // For FOLLOW_TO_INTERACT threads, only followers can interact
  if (threadStyle === 'FOLLOW_TO_INTERACT') {
    return isFollowing;
  }
  
  // For PRIVATE threads, only the owner and admin can interact (handled above)
  if (threadStyle === 'PRIVATE') {
    return false; // Other users can't interact
  }
  
  return true; // Default case
}; 