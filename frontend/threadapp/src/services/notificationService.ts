import { API_ENDPOINTS } from '../config/config';
import { fetchWithAuth } from '../utils/authUtils';

// Define notification types enum
export enum NotificationType {
  // Vote related
  THREAD_UPVOTE = 'THREAD_UPVOTE',
  THREAD_DOWNVOTE = 'THREAD_DOWNVOTE',
  COMMENT_UPVOTE = 'COMMENT_UPVOTE',
  COMMENT_DOWNVOTE = 'COMMENT_DOWNVOTE',
  NEW_COMMENT = 'NEW_COMMENT',
  VOTE_MILESTONE = 'VOTE_MILESTONE',
  VOTE_REMOVED = 'VOTE_REMOVED',
  
  // Thread related
  NEW_THREAD_FOLLOWED = 'NEW_THREAD_FOLLOWED',
  THREAD_UPDATED = 'THREAD_UPDATED',
  THREAD_DELETED = 'THREAD_DELETED',
  
  // Comment related
  NEW_COMMENT_ON_THREAD = 'NEW_COMMENT_ON_THREAD',
  NEW_COMMENT_ON_FOLLOWED_THREAD = 'NEW_COMMENT_ON_FOLLOWED_THREAD',
  COMMENT_REPLY = 'COMMENT_REPLY',
  COMMENT_DELETED = 'COMMENT_DELETED',
  
  // User related
  USER_FOLLOWED = 'USER_FOLLOWED',
  USER_UNFOLLOWED = 'USER_UNFOLLOWED',
  USER_MENTIONED = 'USER_MENTIONED',
  
  // System related
  SYSTEM_NOTIFICATION = 'SYSTEM_NOTIFICATION'
}

export interface NotificationDTO {
  id: number;
  type: NotificationType;
  userId: number;
  threadId?: number;
  commentId?: number;
  createdAt: string;
  read: boolean;
  message?: string;
  actionUserId?: number;
  actionUsername?: string;
}

export interface NotificationPreference {
  id: number;
  type: NotificationType;
  enabled: boolean;
  referenceId?: number;
  referenceType?: string;
  isGlobal: boolean;
}

export interface NotificationPreferenceUpdateDTO {
  type: NotificationType;
  enabled: boolean;
  referenceId?: number;
  referenceType?: string;
  isGlobal: boolean;
}

/**
 * Service for interacting with notification API endpoints
 */
export const notificationService = {
  /**
   * Utility method to safely parse API responses
   */
  async safelyParseResponse(response: Response, errorContext: string): Promise<any> {
    try {
      const text = await response.text();
      
      // Check for common JSON syntax issues
      if (text.indexOf('"') === -1) {
        return { data: [] };
      }
      
      let result;
      try {
        result = JSON.parse(text);
      } catch (parseError: any) {
        // Try to identify specific issues
        if (parseError.message && parseError.message.includes('unterminated string')) {
          // Special handling for preferences - return empty data if it fails
          if (errorContext.includes('Preferences')) {
            return { data: [] };
          }
          
          // Attempt to fix common JSON issues
          try {
            // Try to fix common JSON problems by completing any unterminated strings
            const fixedText = text.replace(/([^\\])"([^"]*?)($|[^\\]")/g, '$1"$2"$3');
            return JSON.parse(fixedText);
          } catch (fixError) {
            // Failed to fix
          }
        }
        
        return { data: [] };
      }
      
      return result;
    } catch (error) {
      return { data: [] };
    }
  },

  /**
   * Get all notifications for a user
   */
  async getUserNotifications(userId: number): Promise<NotificationDTO[]> {
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.notifications.getUserNotifications(userId));
      
      if (!response.ok) {
        return [];
      }
      
      const result = await this.safelyParseResponse(response, 'getUserNotifications');
      
      if (!result.data || !Array.isArray(result.data)) {
        return [];
      }
      
      return result.data || [];
    } catch (error) {
      return []; // Return empty array instead of throwing to avoid crashing the UI
    }
  },

  /**
   * Get paginated notifications for a user (client-side implementation)
   * This will be much faster than retrieving all notifications at once
   */
  async getPaginatedNotifications(userId: number, page: number = 0, pageSize: number = 20): Promise<{ notifications: NotificationDTO[], hasMore: boolean }> {
    try {
      // Use the paginated backend endpoint
      const response = await fetchWithAuth(API_ENDPOINTS.notifications.getPaginatedNotifications(userId, page, pageSize));
      
      if (!response.ok) {
        return { notifications: [], hasMore: false };
      }
      
      const result = await this.safelyParseResponse(response, 'getPaginatedNotifications');
      
      if (!result.data || !Array.isArray(result.data)) {
        return { notifications: [], hasMore: false };
      }
      
      // If we got exactly the page size, assume there might be more (can be refined with a count endpoint)
      const hasMore = result.data.length >= pageSize;
      
      return { 
        notifications: result.data, 
        hasMore: hasMore
      };
    } catch (error) {
      return { notifications: [], hasMore: false };
    }
  },

  /**
   * Get unread notifications for a user
   */
  async getUnreadNotifications(userId: number): Promise<NotificationDTO[]> {
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.notifications.getUnreadNotifications(userId));
      
      if (!response.ok) {
        return [];
      }
      
      const result = await this.safelyParseResponse(response, 'getUnreadNotifications');
      
      if (!result.data || !Array.isArray(result.data)) {
        return [];
      }
      
      return result.data || [];
    } catch (error) {
      return []; // Return empty array instead of throwing to avoid crashing the UI
    }
  },

  /**
   * Get unread notification count
   */
  async getUnreadCount(userId: number): Promise<number> {
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.notifications.getUnreadCount(userId));
      
      if (!response.ok) {
        return 0; // Return 0 on error to avoid breaking UI
      }
      
      const result = await this.safelyParseResponse(response, 'getUnreadCount');
      
      if (typeof result.data !== 'number') {
        return 0;
      }
      
      return result.data || 0;
    } catch (error) {
      return 0; // Return 0 on error to avoid breaking UI
    }
  },

  /**
   * Mark a notification as read
   */
  async markAsRead(notificationId: number): Promise<void> {
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.notifications.markAsRead(notificationId), {
        method: 'PUT'
      });
      
      if (!response.ok) {
        return; // Return instead of throwing to avoid UI crashes
      }
      
      // For methods that don't return data, we still want to safely parse
      // the response to catch any JSON-related issues
      await this.safelyParseResponse(response, 'markAsRead');
    } catch (error) {
      // Don't throw to avoid UI crashes
    }
  },

  /**
   * Mark all notifications as read
   */
  async markAllAsRead(userId: number): Promise<void> {
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.notifications.markAllAsRead(userId), {
        method: 'PUT'
      });
      
      if (!response.ok) {
        return; // Return instead of throwing to avoid UI crashes
      }
      
      // For methods that don't return data, we still want to safely parse
      // the response to catch any JSON-related issues
      await this.safelyParseResponse(response, 'markAllAsRead');
    } catch (error) {
      // Don't throw to avoid UI crashes
    }
  },

  /**
   * Special method for user preferences that handles the large response issue
   */
  async getUserPreferencesWithFallback(userId: number): Promise<NotificationPreference[]> {
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.notifications.getPreferences(userId));
      
      if (!response.ok) {
        return this.getCompletePrefSet();
      }
      
      try {
        const result = await this.safelyParseResponse(response, 'getUserPreferences');
        
        if (!result.data || !Array.isArray(result.data)) {
          return this.getCompletePrefSet();
        }
        
        // Ensure all notification types are included by merging with complete set
        const existingPrefs = result.data || [];
        return this.ensureCompletePreferenceSet(existingPrefs);
      } catch (parseError) {
        return this.getCompletePrefSet();
      }
    } catch (error) {
      return this.getCompletePrefSet();
    }
  },
  
  /**
   * Provides fallback preferences when API call fails
   */
  getFallbackPreferences(): NotificationPreference[] {
    // Try to get from local storage first
    try {
      const savedPreferences = localStorage.getItem('notificationPreferences');
      if (savedPreferences) {
        const parsed = JSON.parse(savedPreferences);
        if (Array.isArray(parsed) && parsed.length > 0) {
          return parsed;
        }
      }
    } catch (error) {
      // Failed to read
    }
    
    return this.getCompletePrefSet();
  },

  /**
   * Get a complete set of all notification preferences with default values
   */
  getCompletePrefSet(): NotificationPreference[] {
    // Return complete notification preferences to allow app to function
    return [
      {
        id: 0,
        type: NotificationType.THREAD_UPVOTE,
        enabled: true,
        isGlobal: true
      },
      {
        id: 1, 
        type: NotificationType.THREAD_DOWNVOTE,
        enabled: true,
        isGlobal: true
      },
      {
        id: 2,
        type: NotificationType.COMMENT_UPVOTE,
        enabled: true,
        isGlobal: true
      },
      {
        id: 3,
        type: NotificationType.COMMENT_DOWNVOTE,
        enabled: true,
        isGlobal: true
      },
      {
        id: 4,
        type: NotificationType.NEW_COMMENT_ON_THREAD,
        enabled: true,
        isGlobal: true
      },
      {
        id: 5,
        type: NotificationType.NEW_COMMENT_ON_FOLLOWED_THREAD,
        enabled: true,
        isGlobal: true
      },
      {
        id: 6,
        type: NotificationType.COMMENT_REPLY,
        enabled: true,
        isGlobal: true
      },
      {
        id: 7,
        type: NotificationType.USER_FOLLOWED,
        enabled: true,
        isGlobal: true
      },
      {
        id: 8,
        type: NotificationType.USER_UNFOLLOWED,
        enabled: true,
        isGlobal: true
      },
      {
        id: 9,
        type: NotificationType.THREAD_UPDATED,
        enabled: true,
        isGlobal: true
      },
      {
        id: 10,
        type: NotificationType.NEW_THREAD_FOLLOWED,
        enabled: true,
        isGlobal: true
      }
    ];
  },

  /**
   * Ensure all possible notification types are included in preferences
   */
  ensureCompletePreferenceSet(existingPrefs: NotificationPreference[]): NotificationPreference[] {
    const completeSet = this.getCompletePrefSet();
    const result = [...existingPrefs];
    
    // Group existing prefs by type for global preferences
    const existingGlobalPrefsByType = existingPrefs
      .filter(p => p.isGlobal)
      .reduce((acc, pref) => {
        acc[pref.type] = pref;
        return acc;
      }, {} as Record<string, NotificationPreference>);
    
    // Add any missing notification types
    completeSet.forEach(defaultPref => {
      // Only add if global preference of this type doesn't already exist
      if (!existingGlobalPrefsByType[defaultPref.type]) {
        // Use a negative ID to indicate it's a client-side temp pref
        const newPref = {
          ...defaultPref,
          id: -Math.floor(Math.random() * 1000) // Use negative random ID to avoid conflicts
        };
        result.push(newPref);
      }
    });
    
    // Filter out disabled preferences if flagged to do so
    if (this.filterDisabledPrefs) {
      console.log('Filtering out disabled preferences, before:', result.length);
      const filtered = result.filter(pref => pref.enabled);
      console.log('After filtering:', filtered.length);
      return filtered;
    }
    
    return result;
  },
  
  /**
   * Flag to determine if disabled preferences should be filtered out
   */
  filterDisabledPrefs: true,
  
  /**
   * Set whether to filter out disabled preferences
   */
  setFilterDisabledPrefs(filter: boolean): void {
    this.filterDisabledPrefs = filter;
  },

  /**
   * Save preferences to local storage as a fallback
   */
  saveFallbackPreferences(preferences: NotificationPreference[]): void {
    try {
      localStorage.setItem('notificationPreferences', JSON.stringify(preferences));
    } catch (error) {
      // Failed to save
    }
  },

  /**
   * Get user notification preferences
   */
  async getUserPreferences(userId: number): Promise<NotificationPreference[]> {
    return this.getUserPreferencesWithFallback(userId);
  },

  /**
   * Toggle notification preference enabled status using direct SQL update
   * This method doesn't require passing the full preference object, just the ID and desired enabled state
   */
  async togglePreference(preferenceId: number, enabled: boolean): Promise<boolean> {
    try {
      // Use the new direct SQL endpoint that avoids JPA loading issues
      const response = await fetchWithAuth(API_ENDPOINTS.notifications.togglePreference(preferenceId, enabled), {
        method: 'PUT'
      });
      
      if (!response.ok) {
        console.error(`Failed to toggle notification preference: ${response.status}`);
        return false;
      }
      
      try {
        const result = await this.safelyParseResponse(response, 'togglePreference');
        return result.success === true;
      } catch (parseError) {
        console.warn('Failed to parse toggle preference response');
        return false;
      }
    } catch (error) {
      console.error('Error toggling preference:', error);
      return false;
    }
  },

  /**
   * Update notification preference
   */
  async updatePreference(preferenceId: number, updateDTO: NotificationPreferenceUpdateDTO): Promise<NotificationPreference> {
    try {
      // Use PUT method directly instead of PATCH
      const response = await fetchWithAuth(API_ENDPOINTS.notifications.updatePreference(preferenceId), {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(updateDTO)
      });
      
      if (!response.ok) {
        console.error(`Failed to update notification preference: ${response.status}`);
        // Fall back to local state update
        this.saveFallbackPreferences([{ 
          id: preferenceId, 
          ...updateDTO 
        } as NotificationPreference]);
        return { id: preferenceId, ...updateDTO } as NotificationPreference;
      }
      
      try {
        const result = await this.safelyParseResponse(response, 'updatePreference');
        
        if (!result.data) {
          throw new Error('Invalid update preference response format');
        }
        
        return result.data;
      } catch (parseError) {
        // If we can't parse the response, return the DTO as the response
        console.warn('Failed to parse preference update response, using input DTO');
        return { id: preferenceId, ...updateDTO } as NotificationPreference;
      }
    } catch (error) {
      console.error('Error updating preference:', error);
      // Instead of throwing, return the update DTO with the ID so UI doesn't break
      return { id: preferenceId, ...updateDTO } as NotificationPreference;
    }
  },

  /**
   * Enable notification type
   */
  async enableNotification(userId: number, type: NotificationType): Promise<void> {
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.notifications.enableNotification(userId, type), {
        method: 'PUT'
      });
      
      if (!response.ok) {
        throw new Error(`Failed to enable notification: ${response.status}`);
      }
      
      // For methods that don't return data, we still want to safely parse
      // the response to catch any JSON-related issues
      await this.safelyParseResponse(response, 'enableNotification');
    } catch (error) {
      // Don't throw to avoid UI crashes
    }
  },

  /**
   * Disable notification type
   */
  async disableNotification(userId: number, type: NotificationType): Promise<void> {
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.notifications.disableNotification(userId, type), {
        method: 'PUT'
      });
      
      if (!response.ok) {
        throw new Error(`Failed to disable notification: ${response.status}`);
      }
      
      // For methods that don't return data, we still want to safely parse
      // the response to catch any JSON-related issues
      await this.safelyParseResponse(response, 'disableNotification');
    } catch (error) {
      // Don't throw to avoid UI crashes
    }
  },

  /**
   * Delete a notification
   */
  async deleteNotification(notificationId: number): Promise<void> {
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.notifications.deleteNotification(notificationId), {
        method: 'DELETE'
      });
      
      if (!response.ok) {
        throw new Error(`Failed to delete notification: ${response.status}`);
      }
      
      // For methods that don't return data, we still want to safely parse
      // the response to catch any JSON-related issues
      await this.safelyParseResponse(response, 'deleteNotification');
    } catch (error) {
      // Don't throw to avoid UI crashes
    }
  }
}; 