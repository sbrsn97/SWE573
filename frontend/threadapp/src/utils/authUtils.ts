import { API_ENDPOINTS } from '../config/config';

// Function to handle API response errors related to authentication
export const handleAuthError = (response: Response, navigate: Function): boolean => {
  if (response.status === 401) {
    // Token is expired or invalid
    localStorage.removeItem('token');
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

// Create an interceptor for all API requests to handle token expiration
export const setupAuthInterceptor = (navigate: Function) => {
  const originalFetch = window.fetch;
  
  window.fetch = async function(input: RequestInfo | URL, init?: RequestInit) {
    const response = await originalFetch(input, init);
    
    if (response.status === 401) {
      try {
        const data = await response.clone().json();
        if (data.code === 'TOKEN_EXPIRED') {
          localStorage.removeItem('token');
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