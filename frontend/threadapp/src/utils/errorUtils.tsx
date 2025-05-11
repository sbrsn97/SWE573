import React from 'react';

/**
 * Checks if an API error is related to profanity
 * @param errorMessage The error message from the API
 * @returns boolean indicating if error is profanity-related
 */
export const isProfanityError = (errorMessage: string): boolean => {
  const profanityKeywords = [
    'inappropriate language',
    'profanity',
    'offensive content',
    'inappropriate content'
  ];
  
  return profanityKeywords.some(keyword => 
    errorMessage.toLowerCase().includes(keyword.toLowerCase())
  );
};

/**
 * Formats a profanity error message in a user-friendly way
 * @param originalMessage The original error message
 * @returns A more user-friendly message
 */
export const formatProfanityError = (originalMessage: string): string => {
  // Return a generic message to avoid repeating the offensive content
  return "Your submission contains inappropriate language and cannot be processed. Please revise your content.";
};

/**
 * Creates a styled error component for profanity errors
 * @param message The error message
 * @returns JSX element with appropriate styling
 */
export const ProfanityErrorMessage: React.FC<{ message: string }> = ({ message }) => {
  const isProfanity = isProfanityError(message);
  
  return (
    <div className={`px-4 py-3 mb-4 rounded-lg flex items-center ${isProfanity ? 'bg-red-50 border border-red-200' : 'bg-orange-50 border border-orange-200'}`}>
      <div className={`${isProfanity ? 'text-red-600' : 'text-orange-600'} mr-3`}>
        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
      </div>
      <p className={`text-sm font-medium ${isProfanity ? 'text-red-800' : 'text-orange-800'}`}>
        {isProfanity ? formatProfanityError(message) : message}
      </p>
    </div>
  );
};

/**
 * Process an API error and return a user-friendly message
 * @param error The error object or message
 * @returns A user-friendly error message
 */
export const processApiError = (error: any): string => {
  if (typeof error === 'string') {
    return isProfanityError(error) ? formatProfanityError(error) : error;
  }
  
  if (error?.message) {
    return isProfanityError(error.message) ? formatProfanityError(error.message) : error.message;
  }
  
  return "An unexpected error occurred. Please try again.";
}; 