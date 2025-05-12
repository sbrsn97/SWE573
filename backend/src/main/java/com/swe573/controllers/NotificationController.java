package com.swe573.controllers;

import com.swe573.dto.ApiResponse;
import com.swe573.dto.NotificationDTO;
import com.swe573.dto.NotificationPreferenceUpdateDTO;
import com.swe573.models.Notification;
import com.swe573.models.NotificationPreference;
import com.swe573.models.enums.NotificationType;
import com.swe573.services.NotificationService;
import com.swe573.services.impl.NotificationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Tag(name = "Notifications", description = "APIs for managing user notifications and preferences")
@RestController
@CrossOrigin(origins = "*", methods = {
    RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, 
    RequestMethod.DELETE, RequestMethod.OPTIONS, RequestMethod.PATCH
})
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Operation(summary = "Get user notifications", description = "Retrieves all notifications for a specific user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getUserNotifications(
            @Parameter(description = "ID of the user", required = true) @PathVariable Long userId) {
        List<Notification> notifications = notificationService.getUserNotifications(userId);
        List<NotificationDTO> dtos = notifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @Operation(summary = "Get paginated user notifications", description = "Retrieves notifications for a specific user with pagination")
    @GetMapping("/user/{userId}/paginated")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getPaginatedNotifications(
            @Parameter(description = "ID of the user", required = true) @PathVariable Long userId,
            @Parameter(description = "Page number (0-based)", required = false) @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", required = false) @RequestParam(defaultValue = "20") int size) {
        List<Notification> notifications = notificationService.getPaginatedNotifications(userId, page, size);
        List<NotificationDTO> dtos = notifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @Operation(summary = "Get unread notifications", description = "Retrieves all unread notifications for a specific user")
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getUnreadNotifications(
            @Parameter(description = "ID of the user", required = true) @PathVariable Long userId) {
        List<Notification> notifications = notificationService.getUnreadNotifications(userId);
        List<NotificationDTO> dtos = notifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @Operation(summary = "Get notifications by type", description = "Retrieves all notifications of a specific type for a user")
    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getNotificationsByType(
            @Parameter(description = "ID of the user", required = true) @PathVariable Long userId,
            @Parameter(description = "Type of notifications to retrieve", required = true) @PathVariable NotificationType type) {
        List<Notification> notifications = notificationService.getNotificationsByType(userId, type);
        List<NotificationDTO> dtos = notifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @Operation(summary = "Get notifications by reference type", description = "Retrieves all notifications for a specific reference type")
    @GetMapping("/user/{userId}/reference/{referenceType}")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getNotificationsByReferenceType(
            @Parameter(description = "ID of the user", required = true) @PathVariable Long userId,
            @Parameter(description = "Type of reference (e.g., THREAD, COMMENT)", required = true) @PathVariable String referenceType) {
        List<Notification> notifications = notificationService.getNotificationsByReferenceType(userId, referenceType);
        List<NotificationDTO> dtos = notifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @Operation(summary = "Mark notification as read", description = "Marks a specific notification as read")
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @Parameter(description = "ID of the notification to mark as read", required = true) @PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Mark all notifications as read", description = "Marks all notifications for a user as read")
    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @Parameter(description = "ID of the user", required = true) @PathVariable Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Mark notifications by type as read", description = "Marks all notifications of a specific type as read")
    @PutMapping("/user/{userId}/type/{type}/read")
    public ResponseEntity<ApiResponse<Void>> markAsReadByType(
            @Parameter(description = "ID of the user", required = true) @PathVariable Long userId,
            @Parameter(description = "Type of notifications to mark as read", required = true) @PathVariable NotificationType type) {
        notificationService.markAsReadByType(userId, type);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Delete notification", description = "Deletes a specific notification")
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @Parameter(description = "ID of the notification to delete", required = true) @PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Delete all notifications", description = "Deletes all notifications for a user")
    @DeleteMapping("/user/{userId}/all")
    public ResponseEntity<ApiResponse<Void>> deleteAllNotifications(
            @Parameter(description = "ID of the user", required = true) @PathVariable Long userId) {
        notificationService.deleteAllNotifications(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Delete notifications by type", description = "Deletes all notifications of a specific type for a user")
    @DeleteMapping("/user/{userId}/type/{type}")
    public ResponseEntity<ApiResponse<Void>> deleteNotificationsByType(
            @Parameter(description = "ID of the user", required = true) @PathVariable Long userId,
            @Parameter(description = "Type of notifications to delete", required = true) @PathVariable NotificationType type) {
        notificationService.deleteNotificationsByType(userId, type);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Get unread notification count", description = "Gets the count of unread notifications for a user")
    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<ApiResponse<Integer>> getUnreadNotificationCount(
            @Parameter(description = "ID of the user", required = true) @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadNotificationCount(userId)));
    }

    @Operation(summary = "Get unread notification count by type", description = "Gets the count of unread notifications of a specific type")
    @GetMapping("/user/{userId}/type/{type}/unread/count")
    public ResponseEntity<ApiResponse<Integer>> getUnreadNotificationCountByType(
            @Parameter(description = "ID of the user", required = true) @PathVariable Long userId,
            @Parameter(description = "Type of notifications to count", required = true) @PathVariable NotificationType type) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadNotificationCountByType(userId, type)));
    }

    @Operation(summary = "Check if notification type is enabled", description = "Checks if notifications of a specific type are enabled for a user")
    @GetMapping("/user/{userId}/type/{type}/enabled")
    public ResponseEntity<ApiResponse<Boolean>> isNotificationEnabled(
            @Parameter(description = "ID of the user", required = true) @PathVariable Long userId,
            @Parameter(description = "Type of notification to check", required = true) @PathVariable NotificationType type) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.isNotificationEnabled(userId, type)));
    }

    @Operation(summary = "Enable notification type", description = "Enables notifications of a specific type for a user")
    @PutMapping("/user/{userId}/type/{type}/enable")
    public ResponseEntity<ApiResponse<Void>> enableNotification(
            @Parameter(description = "ID of the user", required = true) @PathVariable Long userId,
            @Parameter(description = "Type of notification to enable", required = true) @PathVariable NotificationType type) {
        notificationService.enableNotification(userId, type);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Disable notification type", description = "Disables notifications of a specific type for a user")
    @PutMapping("/user/{userId}/type/{type}/disable")
    public ResponseEntity<ApiResponse<Void>> disableNotification(
            @Parameter(description = "ID of the user", required = true) @PathVariable Long userId,
            @Parameter(description = "Type of notification to disable", required = true) @PathVariable NotificationType type) {
        try {
            // Log the request for debugging
            System.out.println("Disabling notification type: " + type + " for user: " + userId);
            
            // Call the service with proper error handling
            notificationService.disableNotification(userId, type);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            System.err.println("Error disabling notification for user " + userId + ", type " + type + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to disable notification: " + e.getMessage()));
        }
    }

    @Operation(summary = "Update notification preference", description = "Updates a specific notification preference")
    @PutMapping("/preferences/{preferenceId}")
    public ResponseEntity<ApiResponse<NotificationPreference>> updatePreference(
            @Parameter(description = "ID of the preference to update", required = true) @PathVariable Long preferenceId,
            @Parameter(description = "Full preference object", required = true) @RequestBody NotificationPreference preference) {
        try {
            // Validate the preference
            if (preference == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Preference data cannot be null"));
            }
            
            // Log the request data for debugging
            System.out.println("Updating preference ID: " + preferenceId);
            
            // Convert the full preference to an update DTO
            NotificationPreferenceUpdateDTO updateDTO = new NotificationPreferenceUpdateDTO();
            updateDTO.setType(preference.getType());
            updateDTO.setEnabled(preference.isEnabled());
            updateDTO.setReferenceId(preference.getReferenceId());
            updateDTO.setReferenceType(preference.getReferenceType());
            updateDTO.setGlobal(preference.isGlobal());
            
            // Call the service with proper error handling
            NotificationPreference updatedPreference = notificationService.updatePreference(preferenceId, updateDTO);
            return ResponseEntity.ok(ApiResponse.success(updatedPreference));
        } catch (Exception e) {
            System.err.println("Error updating preference " + preferenceId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to update preference: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get all notification preferences", description = "Retrieves all notification preferences for a user")
    @GetMapping("/user/{userId}/preferences")
    public ResponseEntity<ApiResponse<List<NotificationPreference>>> getUserPreferences(
            @Parameter(description = "ID of the user", required = true) @PathVariable Long userId) {
        List<NotificationPreference> preferences = notificationService.getUserPreferences(userId);
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    @Operation(summary = "Get global notification preferences", description = "Retrieves global notification preferences for a user")
    @GetMapping("/user/{userId}/preferences/global")
    public ResponseEntity<ApiResponse<List<NotificationPreference>>> getUserGlobalPreferences(
            @Parameter(description = "ID of the user", required = true) @PathVariable Long userId) {
        List<NotificationPreference> preferences = notificationService.getUserGlobalPreferences(userId);
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    @Operation(summary = "Get specific notification preferences", description = "Retrieves specific notification preferences for a user")
    @GetMapping("/user/{userId}/preferences/specific")
    public ResponseEntity<ApiResponse<List<NotificationPreference>>> getUserSpecificPreferences(
            @Parameter(description = "ID of the user", required = true) @PathVariable Long userId) {
        List<NotificationPreference> preferences = notificationService.getUserSpecificPreferences(userId);
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    @Operation(summary = "Delete notification preference", description = "Deletes a specific notification preference")
    @DeleteMapping("/preferences/{preferenceId}")
    public ResponseEntity<ApiResponse<Void>> deletePreference(
            @Parameter(description = "ID of the preference to delete", required = true) @PathVariable Long preferenceId) {
        notificationService.deletePreference(preferenceId);
        return ResponseEntity.ok(ApiResponse.success("Preference deleted successfully", null));
    }

    @Operation(summary = "Delete all notification preferences", description = "Deletes all notification preferences for a user")
    @DeleteMapping("/user/{userId}/preferences/all")
    public ResponseEntity<ApiResponse<Void>> deleteAllPreferences(
            @Parameter(description = "ID of the user", required = true) @PathVariable Long userId) {
        notificationService.deleteAllPreferences(userId);
        return ResponseEntity.ok(ApiResponse.success("All preferences deleted successfully", null));
    }

    @Operation(summary = "Check specific notification preference", description = "Checks if notifications are enabled for a specific reference")
    @GetMapping("/user/{userId}/preferences/check")
    public ResponseEntity<ApiResponse<Boolean>> isNotificationEnabled(
            @Parameter(description = "ID of the user", required = true) @PathVariable Long userId,
            @Parameter(description = "Type of notification to check", required = true) @RequestParam NotificationType type,
            @Parameter(description = "ID of the reference", required = true) @RequestParam Long referenceId,
            @Parameter(description = "Type of reference (e.g., THREAD, COMMENT)", required = true) @RequestParam String referenceType) {
        return ResponseEntity.ok(ApiResponse.success(
            notificationService.isNotificationEnabled(userId, type, referenceId, referenceType)));
    }

    @Operation(summary = "Enable specific notification preference", description = "Enables notifications for a specific reference")
    @PutMapping("/user/{userId}/preferences/enable")
    public ResponseEntity<ApiResponse<Void>> enableNotification(
            @Parameter(description = "ID of the user", required = true) @PathVariable Long userId,
            @Parameter(description = "Type of notification to enable", required = true) @RequestParam NotificationType type,
            @Parameter(description = "ID of the reference", required = true) @RequestParam Long referenceId,
            @Parameter(description = "Type of reference (e.g., THREAD, COMMENT)", required = true) @RequestParam String referenceType) {
        notificationService.enableNotification(userId, type, referenceId, referenceType);
        return ResponseEntity.ok(ApiResponse.success("Notification preference enabled successfully", null));
    }

    @Operation(summary = "Disable specific notification preference", description = "Disables notifications for a specific reference")
    @PutMapping("/user/{userId}/preferences/disable")
    public ResponseEntity<ApiResponse<Void>> disableNotification(
            @Parameter(description = "ID of the user", required = true) @PathVariable Long userId,
            @Parameter(description = "Type of notification to disable", required = true) @RequestParam NotificationType type,
            @Parameter(description = "ID of the reference", required = true) @RequestParam Long referenceId,
            @Parameter(description = "Type of reference (e.g., THREAD, COMMENT)", required = true) @RequestParam String referenceType) {
        notificationService.disableNotification(userId, type, referenceId, referenceType);
        return ResponseEntity.ok(ApiResponse.success("Notification preference disabled successfully", null));
    }

    @Operation(summary = "Clean up duplicate notification preferences", description = "Admin endpoint to remove duplicate notification preferences")
    @PostMapping("/admin/cleanup-duplicates")
    public ResponseEntity<ApiResponse<Integer>> cleanupDuplicatePreferences() {
        // Get all preferences
        List<NotificationPreference> allPreferences = notificationService.getAllPreferences();
        
        // Create a map to track unique preferences by user, type, reference ID, and reference type
        Map<String, List<NotificationPreference>> duplicateMap = new HashMap<>();
        
        // Group preferences by their key characteristics
        for (NotificationPreference pref : allPreferences) {
            String key = pref.getUser().getId() + "_" + 
                         pref.getType() + "_" + 
                         (pref.getReferenceId() != null ? pref.getReferenceId() : "null") + "_" + 
                         (pref.getReferenceType() != null ? pref.getReferenceType() : "null") + "_" +
                         (pref.isGlobal() ? "global" : "specific");
            
            if (!duplicateMap.containsKey(key)) {
                duplicateMap.put(key, new ArrayList<>());
            }
            duplicateMap.get(key).add(pref);
        }
        
        // Count how many preferences will be deleted
        int deletedCount = 0;
        
        // For each group, keep the newest one and delete the rest
        for (List<NotificationPreference> duplicates : duplicateMap.values()) {
            if (duplicates.size() > 1) {
                // Sort by ID (descending) to keep the newest one
                duplicates.sort((a, b) -> Long.compare(b.getId(), a.getId()));
                
                // Keep the first one (highest ID) and delete the rest
                for (int i = 1; i < duplicates.size(); i++) {
                    notificationService.deletePreference(duplicates.get(i).getId());
                    deletedCount++;
                }
            }
        }
        
        return ResponseEntity.ok(ApiResponse.success("Removed " + deletedCount + " duplicate preferences", deletedCount));
    }

    @Operation(summary = "Toggle notification preference enabled status", description = "Updates only the enabled status of a notification preference using direct SQL")
    @PutMapping("/preferences/{preferenceId}/toggle")
    public ResponseEntity<ApiResponse<Boolean>> togglePreferenceStatus(
            @Parameter(description = "ID of the preference to update", required = true) @PathVariable Long preferenceId,
            @Parameter(description = "New enabled status", required = true) @RequestParam boolean enabled) {
        try {
            // Log the request
            System.out.println("Toggling preference " + preferenceId + " to " + enabled);
            
            // Use the direct SQL update method to bypass JPA entity management
            boolean success = notificationService.updatePreferenceEnabledStatusDirectly(preferenceId, enabled);
            
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("Preference updated successfully", enabled));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Preference not found or update failed"));
            }
        } catch (Exception e) {
            System.err.println("Error toggling preference " + preferenceId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to update preference: " + e.getMessage()));
        }
    }

    private NotificationDTO convertToDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setType(notification.getType());
        dto.setUserId(notification.getUser().getId());
        dto.setThreadId("THREAD".equals(notification.getReferenceType()) ? notification.getReferenceId() : null);
        dto.setCommentId("COMMENT".equals(notification.getReferenceType()) ? notification.getReferenceId() : null);
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setRead(notification.isRead());
        return dto;
    }
} 