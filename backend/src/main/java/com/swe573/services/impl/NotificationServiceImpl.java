package com.swe573.services.impl;

import com.swe573.models.Notification;
import com.swe573.models.NotificationPreference;
import com.swe573.models.User;
import com.swe573.models.enums.NotificationType;
import com.swe573.repositories.NotificationRepository;
import com.swe573.repositories.NotificationPreferenceRepository;
import com.swe573.repositories.UserRepository;
import com.swe573.services.NotificationService;
import com.swe573.dto.NotificationPreferenceUpdateDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @Autowired
    private UserRepository userRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    // In-memory cache for notification preferences
    private final Map<Long, Map<NotificationType, Boolean>> notificationPreferences = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void createNotification(Long userId, String message, NotificationType type, Long referenceId, String referenceType) {
        if (userId == null) {
            return; // Skip for null user IDs
        }
        
        try {
            // First check if this notification type is disabled globally
            boolean enabled = true;
            
            // Check for specific preference first
            Optional<NotificationPreference> specificPreference = preferenceRepository
                .findByUserIdAndTypeAndReferenceIdAndReferenceType(userId, type, referenceId, referenceType);
            if (specificPreference.isPresent()) {
                enabled = specificPreference.get().isEnabled();
            } else {
                // If no specific preference, check global preference
                Optional<NotificationPreference> globalPreference = preferenceRepository
                    .findByUserIdAndTypeAndIsGlobalTrue(userId, type);
                if (globalPreference.isPresent()) {
                    enabled = globalPreference.get().isEnabled();
                }
                // If no preference found, it defaults to enabled=true (no need to create a preference here)
            }
            
            if (!enabled) {
                return; // Skip creating notification if disabled
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
        } catch (Exception e) {
            // Log error but don't crash
            System.err.println("Error creating notification: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void createNotification(Long userId, String message, NotificationType type, Long referenceId, String referenceType, Long actionUserId, String actionUsername) {
        if (userId == null) {
            return; // Skip for null user IDs
        }
        
        try {
            // First check if this notification type is disabled globally
            boolean enabled = true;
            
            // Check for specific preference first
            Optional<NotificationPreference> specificPreference = preferenceRepository
                .findByUserIdAndTypeAndReferenceIdAndReferenceType(userId, type, referenceId, referenceType);
            if (specificPreference.isPresent()) {
                enabled = specificPreference.get().isEnabled();
            } else {
                // If no specific preference, check global preference
                Optional<NotificationPreference> globalPreference = preferenceRepository
                    .findByUserIdAndTypeAndIsGlobalTrue(userId, type);
                if (globalPreference.isPresent()) {
                    enabled = globalPreference.get().isEnabled();
                }
                // If no preference found, it defaults to enabled=true (no need to create a preference here)
            }
            
            if (!enabled) {
                return; // Skip creating notification if disabled
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
        } catch (Exception e) {
            // Log error but don't crash
            System.err.println("Error creating notification: " + e.getMessage());
        }
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
    public List<Notification> getPaginatedNotifications(Long userId, int page, int size) {
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(page, size);
        return notificationRepository.findPaginatedByUserId(userId, pageRequest);
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
        if (userId == null) {
            return false; // If user is null, notifications are disabled
        }
        
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
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
                
        NotificationPreference defaultPreference = new NotificationPreference();
            defaultPreference.setUser(user);
        defaultPreference.setType(type);
        defaultPreference.setEnabled(true);
        defaultPreference.setGlobal(true);
        preferenceRepository.save(defaultPreference);
        
        return true;
        } catch (Exception e) {
            // Log the error
            System.err.println("Error creating default preference: " + e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public void enableNotification(Long userId, NotificationType type, Long referenceId, String referenceType) {
        if (userId == null) {
            return; // Do nothing if user ID is null
        }
        
        try {
            // First check if the preference already exists to avoid creating a new one
            Optional<NotificationPreference> existingPreference = preferenceRepository
                .findByUserIdAndTypeAndReferenceIdAndReferenceType(userId, type, referenceId, referenceType);
                
            NotificationPreference preference;
            if (existingPreference.isPresent()) {
                preference = existingPreference.get();
            } else {
                // Create a new preference if doesn't exist
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
                
                preference = new NotificationPreference();
                preference.setUser(user);
                preference.setType(type);
                preference.setReferenceId(referenceId);
                preference.setReferenceType(referenceType);
                preference.setGlobal(false);
            }
            
            // Set to enabled and save
        preference.setEnabled(true);
        preferenceRepository.save(preference);
        } catch (Exception e) {
            // Log error but don't propagate to prevent app from crashing
            System.err.println("Error enabling specific notification preference for user " + userId + 
                ", type " + type + ", ref " + referenceId + ": " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void disableNotification(Long userId, NotificationType type) {
        if (userId == null) {
            return; // Do nothing if user ID is null
        }
        
        try {
            // First check if the preference already exists to avoid creating a new one
            Optional<NotificationPreference> existingPreference = preferenceRepository
                .findByUserIdAndTypeAndIsGlobalTrue(userId, type);
                
            NotificationPreference preference;
            if (existingPreference.isPresent()) {
                preference = existingPreference.get();
            } else {
                // Create a new preference if doesn't exist
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
                
                preference = new NotificationPreference();
                preference.setUser(user);
                preference.setType(type);
                preference.setGlobal(true);
            }
            
            // Set to disabled and save
            preference.setEnabled(false);
            preferenceRepository.save(preference);
            
            // Update cache
            updateCache(userId, type, false);
        } catch (Exception e) {
            // Log error but don't propagate to prevent app from crashing
            System.err.println("Error disabling notification preference for user " + userId + ": " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void disableNotification(Long userId, NotificationType type, Long referenceId, String referenceType) {
        if (userId == null) {
            return; // Do nothing if user ID is null
        }
        
        try {
            // First check if the preference already exists to avoid creating a new one
            Optional<NotificationPreference> existingPreference = preferenceRepository
                .findByUserIdAndTypeAndReferenceIdAndReferenceType(userId, type, referenceId, referenceType);
                
            NotificationPreference preference;
            if (existingPreference.isPresent()) {
                preference = existingPreference.get();
            } else {
                // Create a new preference if doesn't exist
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
                
                preference = new NotificationPreference();
                preference.setUser(user);
                preference.setType(type);
                preference.setReferenceId(referenceId);
                preference.setReferenceType(referenceType);
                preference.setGlobal(false);
            }
            
            // Set to disabled and save
        preference.setEnabled(false);
        preferenceRepository.save(preference);
        } catch (Exception e) {
            // Log error but don't propagate to prevent app from crashing
            System.err.println("Error disabling specific notification preference for user " + userId + 
                ", type " + type + ", ref " + referenceId + ": " + e.getMessage());
        }
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
        if (userId == null) {
            return new ArrayList<>();
        }
        
        try {
            // Try to load existing preferences first
            List<NotificationPreference> existingPrefs = preferenceRepository.findByUserId(userId);
            
            // Only call ensure method if no preferences exist at all
            if (existingPrefs.isEmpty()) {
                try {
                    ensureAllDefaultPreferencesExist(userId);
                    existingPrefs = preferenceRepository.findByUserId(userId);
                } catch (Exception e) {
                    System.err.println("Error ensuring default preferences: " + e.getMessage());
                    // Continue with whatever preferences we have
                }
            }
            
            return existingPrefs;
        } catch (Exception e) {
            System.err.println("Error retrieving user preferences: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<NotificationPreference> getUserGlobalPreferences(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        
        try {
            // Try to load existing global preferences first
            List<NotificationPreference> existingPrefs = preferenceRepository.findByUserIdAndIsGlobalTrue(userId);
            
            // Only call ensure method if no global preferences exist at all
            if (existingPrefs.isEmpty()) {
                try {
                    ensureAllDefaultPreferencesExist(userId);
                    existingPrefs = preferenceRepository.findByUserIdAndIsGlobalTrue(userId);
                } catch (Exception e) {
                    System.err.println("Error ensuring default preferences: " + e.getMessage());
                    // Continue with whatever preferences we have
                }
            }
            
            return existingPrefs;
        } catch (Exception e) {
            System.err.println("Error retrieving user global preferences: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<NotificationPreference> getUserSpecificPreferences(Long userId) {
        List<NotificationPreference> preferences = preferenceRepository.findByUserIdAndIsGlobalFalse(userId);
        
        // Create a map to deduplicate preferences based on type, referenceId, and referenceType
        Map<String, NotificationPreference> uniquePreferences = new HashMap<>();
        
        for (NotificationPreference pref : preferences) {
            // Create a unique key for each preference based on its identifying properties
            String key = pref.getType() + "_" + 
                         (pref.getReferenceId() != null ? pref.getReferenceId() : "null") + "_" + 
                         (pref.getReferenceType() != null ? pref.getReferenceType() : "null");
            
            // If this is a new key or has a higher ID than an existing one with the same key, save it
            if (!uniquePreferences.containsKey(key) || 
                pref.getId() > uniquePreferences.get(key).getId()) {
                uniquePreferences.put(key, pref);
            }
        }
        
        // Return the deduplicated list
        return new ArrayList<>(uniquePreferences.values());
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
        if (userId == null) {
            return false; // If user is null, notifications are disabled
        }
        
        // First check cache
        if (notificationPreferences.containsKey(userId) && 
            notificationPreferences.get(userId).containsKey(type)) {
            return notificationPreferences.get(userId).get(type);
        }

        // Check if the user has a preference for this notification type
        Optional<NotificationPreference> preference = preferenceRepository
            .findByUserIdAndTypeAndIsGlobalTrue(userId, type);
        
        if (preference.isPresent()) {
            // Update cache
            updateCache(userId, type, preference.get().isEnabled());
            return preference.get().isEnabled();
        }

        // Create default preference (enabled by default)
        try {
            ensureDefaultPreferenceExists(userId, type);
            
            // Update cache
            updateCache(userId, type, true);
            
            return true;
        } catch (Exception e) {
            // Log the error
            System.err.println("Error creating default preference: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Ensures that a default preference exists for the given user and notification type
     */
    private void ensureDefaultPreferenceExists(Long userId, NotificationType type) {
        if (userId == null) {
            return; // Skip if user is null
        }
        
        // Check if a preference already exists to avoid duplicates
        if (preferenceRepository.findByUserIdAndTypeAndIsGlobalTrue(userId, type).isPresent()) {
            return;
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
            
        NotificationPreference defaultPreference = new NotificationPreference();
        defaultPreference.setUser(user);
        defaultPreference.setType(type);
        defaultPreference.setEnabled(true);
        defaultPreference.setGlobal(true);
        preferenceRepository.save(defaultPreference);
    }
    
    /**
     * Updates the in-memory cache for notification preferences
     */
    private void updateCache(Long userId, NotificationType type, boolean enabled) {
        if (!notificationPreferences.containsKey(userId)) {
            notificationPreferences.put(userId, new ConcurrentHashMap<>());
        }
        notificationPreferences.get(userId).put(type, enabled);
    }

    @Override
    @Transactional
    public void enableNotification(Long userId, NotificationType type) {
        if (userId == null) {
            return; // Do nothing if user ID is null
        }
        
        try {
            // First check if the preference already exists to avoid creating a new one
            Optional<NotificationPreference> existingPreference = preferenceRepository
                .findByUserIdAndTypeAndIsGlobalTrue(userId, type);
                
            NotificationPreference preference;
            if (existingPreference.isPresent()) {
                preference = existingPreference.get();
            } else {
                // Create a new preference if doesn't exist
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
                
                preference = new NotificationPreference();
                preference.setUser(user);
                preference.setType(type);
                preference.setGlobal(true);
            }
            
            // Set to enabled and save
        preference.setEnabled(true);
        preferenceRepository.save(preference);
            
            // Update cache
            updateCache(userId, type, true);
        } catch (Exception e) {
            // Log error but don't propagate to prevent app from crashing
            System.err.println("Error enabling notification preference for user " + userId + ": " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public NotificationPreference updatePreference(Long preferenceId, NotificationPreferenceUpdateDTO updateDTO) {
        if (preferenceId == null) {
            throw new IllegalArgumentException("Preference ID cannot be null");
        }
        
        try {
            System.out.println("DEBUG: Updating preference with ID: " + preferenceId);
            System.out.println("DEBUG: Update DTO: " + updateDTO);
            
            // First, find the preference directly by ID without eagerly loading the user relationship
            Optional<NotificationPreference> preferenceOpt = preferenceRepository.findById(preferenceId);
            if (!preferenceOpt.isPresent()) {
                throw new EntityNotFoundException("Notification preference not found with ID: " + preferenceId);
            }
            
            NotificationPreference preference = preferenceOpt.get();
            System.out.println("DEBUG: Found preference: " + preference);
            System.out.println("DEBUG: Current enabled state: " + preference.isEnabled());

            // Get the user ID without fully loading the user entity
            Long userId = null;
            if (preference.getUser() != null) {
                userId = preference.getUser().getId();
                System.out.println("DEBUG: User ID: " + userId);
            }
            
            // Update the detached preference object with the new values
            if (updateDTO.getType() != null) {
        preference.setType(updateDTO.getType());
            }
            
        preference.setEnabled(updateDTO.isEnabled());
            
            if (updateDTO.getReferenceId() != null) {
        preference.setReferenceId(updateDTO.getReferenceId());
            }
            
            if (updateDTO.getReferenceType() != null) {
        preference.setReferenceType(updateDTO.getReferenceType());
            }
            
            if (updateDTO.isGlobal() != preference.isGlobal()) {
        preference.setGlobal(updateDTO.isGlobal());
            }
            
            // Save using merge to avoid cascading operations
            NotificationPreference savedPreference = preferenceRepository.save(preference);
            System.out.println("DEBUG: Saved preference: " + savedPreference);
            System.out.println("DEBUG: Saved preference enabled: " + savedPreference.isEnabled());
            
            // Update the cache
            if (userId != null && savedPreference.isGlobal()) {
                if (notificationPreferences.containsKey(userId)) {
                    notificationPreferences.remove(userId);
                }
                updateCache(userId, savedPreference.getType(), savedPreference.isEnabled());
            }
            
            return savedPreference;
        } catch (EntityNotFoundException e) {
            throw e; // Re-throw EntityNotFoundExceptions
        } catch (Exception e) {
            // Log detailed error but don't recurse
            System.err.println("Error updating preference " + preferenceId + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error updating notification preference: " + e.getMessage());
        }
    }

    @Override
    public List<NotificationPreference> getAllPreferences() {
        return preferenceRepository.findAll();
    }

    /**
     * Ensures that default preferences exist for all notification types
     */
    private void ensureAllDefaultPreferencesExist(Long userId) {
        if (userId == null) {
            return; // Skip for null user IDs
        }
        
        try {
            // Get all possible notification types
            NotificationType[] allTypes = NotificationType.values();
            
            // Get existing global preferences for this user
            List<NotificationPreference> existingPrefs = preferenceRepository.findByUserIdAndIsGlobalTrue(userId);
            
            // Create a set of notification types that already have preferences
            Set<NotificationType> existingTypes = existingPrefs.stream()
                .map(NotificationPreference::getType)
                .collect(java.util.stream.Collectors.toSet());
            
            // Get the user once to avoid repeated DB calls
            User user = null;
            try {
                user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    return; // Skip if user not found
                }
            } catch (Exception e) {
                System.err.println("Error finding user " + userId + ": " + e.getMessage());
                return;
            }
            
            // Create default preferences for any missing types
            List<NotificationPreference> newPreferences = new ArrayList<>();
            for (NotificationType type : allTypes) {
                if (!existingTypes.contains(type)) {
                    try {
                        NotificationPreference defaultPreference = new NotificationPreference();
                        defaultPreference.setUser(user);
                        defaultPreference.setType(type);
                        defaultPreference.setEnabled(true);
                        defaultPreference.setGlobal(true);
                        newPreferences.add(defaultPreference);
                    } catch (Exception e) {
                        System.err.println("Error creating preference for type " + type + ": " + e.getMessage());
                    }
                }
            }
            
            // Batch save all new preferences at once if any
            if (!newPreferences.isEmpty()) {
                preferenceRepository.saveAll(newPreferences);
            }
        } catch (Exception e) {
            // Log but don't crash
            System.err.println("Error ensuring default preferences: " + e.getMessage());
        }
    }

    /**
     * Updates just the enabled status of a notification preference using direct SQL
     * to bypass any JPA entity management issues.
     * 
     * @param preferenceId The ID of the preference to update
     * @param enabled The new enabled status
     * @return true if the update was successful, false otherwise
     */
    @Transactional
    public boolean updatePreferenceEnabledStatusDirectly(Long preferenceId, boolean enabled) {
        try {
            // Note: This will now use a direct SQL update without loading the entity
            
            // Use a native query to update just the enabled field
            String sql = "UPDATE notification_preferences SET enabled = :enabled WHERE id = :id";
            int rowsUpdated = entityManager.createNativeQuery(sql)
                .setParameter("enabled", enabled)
                .setParameter("id", preferenceId)
                .executeUpdate();
            
            if (rowsUpdated > 0) {
                // Try to update the cache if this is a global preference
                try {
                    // Get the preference details without loading relationships
                    String fetchSql = "SELECT user_id, notification_type, is_global FROM notification_preferences WHERE id = :id";
                    Object[] result = (Object[]) entityManager.createNativeQuery(fetchSql)
                        .setParameter("id", preferenceId)
                        .getSingleResult();
                    
                    if (result != null && result.length == 3) {
                        Long userId = ((Number) result[0]).longValue();
                        String typeStr = (String) result[1];
                        boolean isGlobal = (boolean) result[2];
                        
                        if (isGlobal && typeStr != null) {
                            NotificationType type = NotificationType.valueOf(typeStr);
                            
                            // Update the in-memory cache
                            if (notificationPreferences.containsKey(userId)) {
                                notificationPreferences.remove(userId);
                            }
                            updateCache(userId, type, enabled);
                        }
                    }
                } catch (Exception e) {
                    // Log but ignore cache update errors
                    System.err.println("Error updating cache after direct update: " + e.getMessage());
                }
                
                return true;
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("Error in direct SQL update for preference " + preferenceId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
} 