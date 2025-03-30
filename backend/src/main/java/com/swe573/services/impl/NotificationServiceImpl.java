package com.swe573.services.impl;

import com.swe573.models.Notification;
import com.swe573.models.NotificationPreference;
import com.swe573.models.User;
import com.swe573.models.enums.NotificationType;
import com.swe573.repositories.NotificationRepository;
import com.swe573.repositories.NotificationPreferenceRepository;
import com.swe573.repositories.UserRepository;
import com.swe573.services.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @Autowired
    private UserRepository userRepository;

    // In-memory cache for notification preferences
    private final Map<Long, Map<NotificationType, Boolean>> notificationPreferences = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void createNotification(Long userId, String message, NotificationType type, Long referenceId, String referenceType) {
        if (!isNotificationEnabled(userId, type, referenceId, referenceType)) {
            return;
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setType(type);
        notification.setReferenceId(referenceId);
        notification.setReferenceType(referenceType);
        notification.setRead(false);

        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void createNotification(Long userId, String message, NotificationType type, Long referenceId, String referenceType, Long actionUserId, String actionUsername) {
        if (!isNotificationEnabled(userId, type, referenceId, referenceType)) {
            return;
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setType(type);
        notification.setReferenceId(referenceId);
        notification.setReferenceType(referenceType);
        notification.setActionUserId(actionUserId);
        notification.setActionUsername(actionUsername);
        notification.setRead(false);

        notificationRepository.save(notification);
    }

    @Override
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Notification> getNotificationsByType(Long userId, NotificationType type) {
        return notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type);
    }

    @Override
    public List<Notification> getNotificationsByReferenceType(Long userId, String referenceType) {
        return notificationRepository.findByUserIdAndReferenceTypeOrderByCreatedAtDesc(userId, referenceType);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new EntityNotFoundException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = getUnreadNotifications(userId);
        unreadNotifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    @Override
    @Transactional
    public void markAsReadByType(Long userId, NotificationType type) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndTypeAndIsReadFalse(userId, type);
        unreadNotifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Override
    @Transactional
    public void deleteAllNotifications(Long userId) {
        List<Notification> notifications = getUserNotifications(userId);
        notificationRepository.deleteAll(notifications);
    }

    @Override
    @Transactional
    public void deleteNotificationsByType(Long userId, NotificationType type) {
        List<Notification> notifications = getNotificationsByType(userId, type);
        notificationRepository.deleteAll(notifications);
    }

    @Override
    public int getUnreadNotificationCount(Long userId) {
        return (int) notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    public int getUnreadNotificationCountByType(Long userId, NotificationType type) {
        return (int) notificationRepository.countByUserIdAndTypeAndIsReadFalse(userId, type);
    }

    @Override
    public boolean isNotificationEnabled(Long userId, NotificationType type, Long referenceId, String referenceType) {
        // First check for specific preference
        Optional<NotificationPreference> specificPreference = preferenceRepository
            .findByUserIdAndTypeAndReferenceIdAndReferenceType(userId, type, referenceId, referenceType);
        
        if (specificPreference.isPresent()) {
            return specificPreference.get().isEnabled();
        }

        // If no specific preference, check global preference
        Optional<NotificationPreference> globalPreference = preferenceRepository
            .findByUserIdAndTypeAndIsGlobalTrue(userId, type);
        
        if (globalPreference.isPresent()) {
            return globalPreference.get().isEnabled();
        }

        // If no preferences found, create default global preference (enabled)
        NotificationPreference defaultPreference = new NotificationPreference();
        defaultPreference.setUser(userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found")));
        defaultPreference.setType(type);
        defaultPreference.setEnabled(true);
        defaultPreference.setGlobal(true);
        preferenceRepository.save(defaultPreference);
        
        return true;
    }

    @Override
    @Transactional
    public void enableNotification(Long userId, NotificationType type, Long referenceId, String referenceType) {
        NotificationPreference preference = preferenceRepository
            .findByUserIdAndTypeAndReferenceIdAndReferenceType(userId, type, referenceId, referenceType)
            .orElseGet(() -> {
                NotificationPreference newPreference = new NotificationPreference();
                newPreference.setUser(userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found")));
                newPreference.setType(type);
                newPreference.setReferenceId(referenceId);
                newPreference.setReferenceType(referenceType);
                newPreference.setGlobal(false);
                return newPreference;
            });
        
        preference.setEnabled(true);
        preferenceRepository.save(preference);
    }

    @Override
    @Transactional
    public void disableNotification(Long userId, NotificationType type, Long referenceId, String referenceType) {
        NotificationPreference preference = preferenceRepository
            .findByUserIdAndTypeAndReferenceIdAndReferenceType(userId, type, referenceId, referenceType)
            .orElseGet(() -> {
                NotificationPreference newPreference = new NotificationPreference();
                newPreference.setUser(userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found")));
                newPreference.setType(type);
                newPreference.setReferenceId(referenceId);
                newPreference.setReferenceType(referenceType);
                newPreference.setGlobal(false);
                return newPreference;
            });
        
        preference.setEnabled(false);
        preferenceRepository.save(preference);
    }

    @Override
    @Transactional
    public void createBulkNotifications(List<Long> userIds, String message, NotificationType type, Long referenceId, String referenceType) {
        userIds.forEach(userId -> createNotification(userId, message, type, referenceId, referenceType));
    }

    @Override
    @Transactional
    public void createBulkNotifications(List<Long> userIds, String message, NotificationType type, Long referenceId, String referenceType, Long actionUserId, String actionUsername) {
        userIds.forEach(userId -> createNotification(userId, message, type, referenceId, referenceType, actionUserId, actionUsername));
    }

    @Override
    public List<NotificationPreference> getUserPreferences(Long userId) {
        return preferenceRepository.findByUserId(userId);
    }

    @Override
    public List<NotificationPreference> getUserGlobalPreferences(Long userId) {
        return preferenceRepository.findByUserIdAndIsGlobalTrue(userId);
    }

    @Override
    public List<NotificationPreference> getUserSpecificPreferences(Long userId) {
        return preferenceRepository.findByUserIdAndIsGlobalFalse(userId);
    }

    @Override
    @Transactional
    public void deletePreference(Long preferenceId) {
        preferenceRepository.deleteById(preferenceId);
    }

    @Override
    @Transactional
    public void deleteAllPreferences(Long userId) {
        List<NotificationPreference> preferences = preferenceRepository.findByUserId(userId);
        preferenceRepository.deleteAll(preferences);
    }

    @Override
    public boolean isNotificationEnabled(Long userId, NotificationType type) {
        // Check global preference
        Optional<NotificationPreference> globalPreference = preferenceRepository
            .findByUserIdAndTypeAndIsGlobalTrue(userId, type);
        
        if (globalPreference.isPresent()) {
            return globalPreference.get().isEnabled();
        }

        // If no preference found, create default global preference (enabled)
        NotificationPreference defaultPreference = new NotificationPreference();
        defaultPreference.setUser(userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found")));
        defaultPreference.setType(type);
        defaultPreference.setEnabled(true);
        defaultPreference.setGlobal(true);
        preferenceRepository.save(defaultPreference);
        
        return true;
    }

    @Override
    @Transactional
    public void enableNotification(Long userId, NotificationType type) {
        NotificationPreference preference = preferenceRepository
            .findByUserIdAndTypeAndIsGlobalTrue(userId, type)
            .orElseGet(() -> {
                NotificationPreference newPreference = new NotificationPreference();
                newPreference.setUser(userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found")));
                newPreference.setType(type);
                newPreference.setGlobal(true);
                return newPreference;
            });
        
        preference.setEnabled(true);
        preferenceRepository.save(preference);
    }

    @Override
    @Transactional
    public void disableNotification(Long userId, NotificationType type) {
        NotificationPreference preference = preferenceRepository
            .findByUserIdAndTypeAndIsGlobalTrue(userId, type)
            .orElseGet(() -> {
                NotificationPreference newPreference = new NotificationPreference();
                newPreference.setUser(userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found")));
                newPreference.setType(type);
                newPreference.setGlobal(true);
                return newPreference;
            });
        
        preference.setEnabled(false);
        preferenceRepository.save(preference);
    }
} 