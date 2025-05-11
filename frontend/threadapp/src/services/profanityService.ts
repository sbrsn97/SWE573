import { API_ENDPOINTS } from '../config/config';
import { fetchWithAuth } from '../utils/authUtils';

/**
 * Service for interacting with profanity filter management endpoints
 */
export const profanityService = {
  /**
   * Get all profanity words from the server
   * @returns Promise with the list of profanity words
   */
  async getAllWords(): Promise<string[]> {
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.nlp.profanity.getAllWords);
      
      if (!response.ok) {
        throw new Error(`Failed to get profanity words: ${response.status}`);
      }
      
      const result = await response.json();
      return result.data || [];
    } catch (error) {
      console.error('Error fetching profanity words:', error);
      throw error;
    }
  },
  
  /**
   * Add a new word to the profanity filter
   * @param word The word to add
   * @param language The language code ('en' for English, 'tr' for Turkish)
   * @returns Promise with success status
   */
  async addWord(word: string, language: string = 'en'): Promise<boolean> {
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.nlp.profanity.addWord, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ word, language })
      });
      
      if (!response.ok) {
        throw new Error(`Failed to add profanity word: ${response.status}`);
      }
      
      return true;
    } catch (error) {
      console.error('Error adding profanity word:', error);
      throw error;
    }
  },
  
  /**
   * Remove a word from the profanity filter
   * @param word The word to remove
   * @returns Promise with success status
   */
  async removeWord(word: string): Promise<boolean> {
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.nlp.profanity.removeWord(word), {
        method: 'DELETE'
      });
      
      if (!response.ok) {
        throw new Error(`Failed to remove profanity word: ${response.status}`);
      }
      
      return true;
    } catch (error) {
      console.error('Error removing profanity word:', error);
      throw error;
    }
  },
  
  /**
   * Check if text contains profanity
   * @param text The text to check
   * @returns Promise with check result (true if profanity found)
   */
  async checkText(text: string): Promise<boolean> {
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.nlp.profanity.check, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ text })
      });
      
      if (!response.ok) {
        throw new Error(`Failed to check text: ${response.status}`);
      }
      
      // Parse the response to get the result from the data field
      const result = await response.json();
      return result.data === true;
    } catch (error) {
      console.error('Error checking text for profanity:', error);
      throw error;
    }
  },
  
  /**
   * Reload the profanity filter from disk
   * @returns Promise with success status
   */
  async reloadFilter(): Promise<boolean> {
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.nlp.profanity.reload, {
        method: 'POST'
      });
      
      if (!response.ok) {
        throw new Error(`Failed to reload profanity filter: ${response.status}`);
      }
      
      return true;
    } catch (error) {
      console.error('Error reloading profanity filter:', error);
      throw error;
    }
  }
}; 