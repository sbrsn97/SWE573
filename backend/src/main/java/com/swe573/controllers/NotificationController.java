package com.swe573.controllers;

import com.swe573.dto.ApiResponse;
import com.swe573.dto.NotificationDTO;
import com.swe573.models.Notification;
import com.swe573.models.enums.NotificationType;
import com.swe573.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getUserNotifications(@PathVariable Long userId) {
        List<Notification> notifications = notificationService.getUserNotifications(userId);
        List<NotificationDTO> dtos = notifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getUnreadNotifications(@PathVariable Long userId) {
        List<Notification> notifications = notificationService.getUnreadNotifications(userId);
        List<NotificationDTO> dtos = notifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getNotificationsByType(
            @PathVariable Long userId,
            @PathVariable NotificationType type) {
        List<Notification> notifications = notificationService.getNotificationsByType(userId, type);
        List<NotificationDTO> dtos = notifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/user/{userId}/reference/{referenceType}")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getNotificationsByReferenceType(
            @PathVariable Long userId,
            @PathVariable String referenceType) {
        List<Notification> notifications = notificationService.getNotificationsByReferenceType(userId, referenceType);
        List<NotificationDTO> dtos = notifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@PathVariable Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/user/{userId}/type/{type}/read")
    public ResponseEntity<ApiResponse<Void>> markAsReadByType(
            @PathVariable Long userId,
            @PathVariable NotificationType type) {
        notificationService.markAsReadByType(userId, type);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/user/{userId}/all")
    public ResponseEntity<ApiResponse<Void>> deleteAllNotifications(@PathVariable Long userId) {
        notificationService.deleteAllNotifications(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/user/{userId}/type/{type}")
    public ResponseEntity<ApiResponse<Void>> deleteNotificationsByType(
            @PathVariable Long userId,
            @PathVariable NotificationType type) {
        notificationService.deleteNotificationsByType(userId, type);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<ApiResponse<Integer>> getUnreadNotificationCount(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadNotificationCount(userId)));
    }

    @GetMapping("/user/{userId}/type/{type}/unread/count")
    public ResponseEntity<ApiResponse<Integer>> getUnreadNotificationCountByType(
            @PathVariable Long userId,
            @PathVariable NotificationType type) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadNotificationCountByType(userId, type)));
    }

    @GetMapping("/user/{userId}/type/{type}/enabled")
    public ResponseEntity<ApiResponse<Boolean>> isNotificationEnabled(
            @PathVariable Long userId,
            @PathVariable NotificationType type) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.isNotificationEnabled(userId, type)));
    }

    @PutMapping("/user/{userId}/type/{type}/enable")
    public ResponseEntity<ApiResponse<Void>> enableNotification(
            @PathVariable Long userId,
            @PathVariable NotificationType type) {
        notificationService.enableNotification(userId, type);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/user/{userId}/type/{type}/disable")
    public ResponseEntity<ApiResponse<Void>> disableNotification(
            @PathVariable Long userId,
            @PathVariable NotificationType type) {
        notificationService.disableNotification(userId, type);
        return ResponseEntity.ok(ApiResponse.success(null));
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