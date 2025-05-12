import { API_ENDPOINTS } from '../config/config';
import { fetchWithAuth } from '../utils/authUtils';

export interface WikidataEntity {
  id: string;
  label: string;
  description: string;
  url?: string;
  type?: string;
  properties?: Record<string, string>;
  propertyDescriptions?: Record<string, string>;
}

export interface WikidataProperty {
  id: string;
  label: string;
  description: string;
  url?: string;
  valueType?: string;
  exampleValue?: string;
}

export interface PaginatedResponse<T> {
  items: T[];
  currentPage: number;
  totalPages: number;
  totalItems: number;
  pageSize: number;
}

/**
 * Service for interacting with Wikidata API endpoints
 */
export const wikidataService = {
  /**
   * Search for topics in Wikidata
   * @param query Search query
   * @param page Page number (0-based)
   * @param size Number of results per page
   * @returns Promise with paginated list of Wikidata entities
   */
  async searchTopics(query: string, page = 0, size = 10): Promise<PaginatedResponse<WikidataEntity>> {
    try {
      const response = await fetchWithAuth(
        API_ENDPOINTS.wikidata.topics.search(query, page, size)
      );
      
      if (!response.ok) {
        throw new Error(`Failed to search Wikidata topics: ${response.status}`);
      }
      
      return await response.json();
    } catch (error) {
      console.error('Error searching Wikidata topics:', error);
      throw error;
    }
  },

  /**
   * Get details for a specific Wikidata topic
   * @param id Wikidata entity ID (e.g., Q123456)
   * @returns Promise with Wikidata entity details
   */
  async getTopicDetails(id: string): Promise<WikidataEntity> {
    try {
      const response = await fetchWithAuth(
        API_ENDPOINTS.wikidata.topics.getById(id)
      );
      
      if (!response.ok) {
        throw new Error(`Failed to get Wikidata topic details: ${response.status}`);
      }
      
      return await response.json();
    } catch (error) {
      console.error('Error fetching Wikidata topic details:', error);
      throw error;
    }
  },

  /**
   * Search for entities in Wikidata
   * @param query Search query
   * @param page Page number (0-based)
   * @param size Number of results per page
   * @returns Promise with paginated list of Wikidata entities
   */
  async searchEntities(query: string, page = 0, size = 10): Promise<PaginatedResponse<WikidataEntity>> {
    try {
      const response = await fetchWithAuth(
        API_ENDPOINTS.wikidata.entities.search(query, page, size)
      );
      
      if (!response.ok) {
        throw new Error(`Failed to search Wikidata entities: ${response.status}`);
      }
      
      return await response.json();
    } catch (error) {
      console.error('Error searching Wikidata entities:', error);
      throw error;
    }
  }
}; 