import { API_ENDPOINTS } from '../config/config';
import { fetchWithAuth } from '../utils/authUtils';

/**
 * Service for interacting with NLP API endpoints
 */
export const nlpService = {
  /**
   * Extract keywords from text
   * @param text Text to analyze
   * @returns Promise with extracted keywords
   */
  async extractKeywords(text: string): Promise<string[]> {
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.nlp.keywords, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ text })
      });
      
      if (!response.ok) {
        throw new Error(`Failed to extract keywords: ${response.status}`);
      }
      
      const result = await response.json();
      return result.data || [];
    } catch (error) {
      console.error('Error extracting keywords:', error);
      throw error;
    }
  },

  /**
   * Extract named entities from text
   * @param text Text to analyze
   * @returns Promise with extracted named entities
   */
  async extractNamedEntities(text: string): Promise<string[]> {
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.nlp.entities, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ text })
      });
      
      if (!response.ok) {
        throw new Error(`Failed to extract named entities: ${response.status}`);
      }
      
      const result = await response.json();
      return result.data || [];
    } catch (error) {
      console.error('Error extracting named entities:', error);
      throw error;
    }
  },

  /**
   * Analyze text to extract relevant topics
   * @param text Text to analyze
   * @returns Promise with analyzed topics
   */
  async analyzeTopics(text: string): Promise<string[]> {
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.nlp.topics, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ text })
      });
      
      if (!response.ok) {
        throw new Error(`Failed to analyze topics: ${response.status}`);
      }
      
      const result = await response.json();
      return result.data || [];
    } catch (error) {
      console.error('Error analyzing topics:', error);
      throw error;
    }
  },

  /**
   * Comprehensive text analysis
   * @param text Text to analyze
   * @returns Promise with comprehensive analysis results
   */
  async analyze(text: string): Promise<{
    keywords: string[],
    entities: string[],
    topics: string[]
  }> {
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.nlp.analyze, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ text })
      });
      
      if (!response.ok) {
        throw new Error(`Failed to analyze text: ${response.status}`);
      }
      
      const result = await response.json();
      return result.data || { keywords: [], entities: [], topics: [] };
    } catch (error) {
      console.error('Error performing text analysis:', error);
      throw error;
    }
  }
}; 