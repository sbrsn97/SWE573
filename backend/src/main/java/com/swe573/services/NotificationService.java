package com.swe573.services;

import com.swe573.models.Notification;
import com.swe573.models.NotificationPreference;
import com.swe573.models.enums.NotificationType;
import java.util.List;

public interface NotificationService {
    // Create notifications
    void createNotification(Long userId, String message, NotificationType type, Long referenceId, String referenceType);
    void createNotification(Long userId, String message, NotificationType type, Long referenceId, String referenceType, Long actionUserId, String actionUsername);
    void createBulkNotifications(List<Long> userIds, String message, NotificationType type, Long referenceId, String referenceType);
    void createBulkNotifications(List<Long> userIds, String message, NotificationType type, Long referenceId, String referenceType, Long actionUserId, String actionUsername);
    
    // Get notifications
    List<Notification> getUserNotifications(Long userId);
    List<Notification> getUnreadNotifications(Long userId);
    List<Notification> getNotificationsByType(Long userId, NotificationType type);
    List<Notification> getNotificationsByReferenceType(Long userId, String referenceType);
    
    // Notification management
    void markAsRead(Long notificationId);
    void markAllAsRead(Long userId);
    void markAsReadByType(Long userId, NotificationType type);
    void deleteNotification(Long notificationId);
    void deleteAllNotifications(Long userId);
    void deleteNotificationsByType(Long userId, NotificationType type);
    
    // Notification counts
    int getUnreadNotificationCount(Long userId);
    int getUnreadNotificationCountByType(Long userId, NotificationType type);
    
    // Global notification preferences
    boolean isNotificationEnabled(Long userId, NotificationType type);
    void enableNotification(Long userId, NotificationType type);
    void disableNotification(Long userId, NotificationType type);
    
    // Specific notification preferences
    boolean isNotificationEnabled(Long userId, NotificationType type, Long referenceId, String referenceType);
    void enableNotification(Long userId, NotificationType type, Long referenceId, String referenceType);
    void disableNotification(Long userId, NotificationType type, Long referenceId, String referenceType);
    
    // Preference management
    List<NotificationPreference> getUserPreferences(Long userId);
    List<NotificationPreference> getUserGlobalPreferences(Long userId);
    List<NotificationPreference> getUserSpecificPreferences(Long userId);
    void deletePreference(Long preferenceId);
    void deleteAllPreferences(Long userId);
} 