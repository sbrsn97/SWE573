package com.swe573.controllers;

import com.swe573.models.Notification;
import com.swe573.models.enums.NotificationType;
import com.swe573.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<List<Notification>> getNotificationsByType(
            @PathVariable Long userId,
            @PathVariable NotificationType type) {
        return ResponseEntity.ok(notificationService.getNotificationsByType(userId, type));
    }

    @GetMapping("/user/{userId}/reference/{referenceType}")
    public ResponseEntity<List<Notification>> getNotificationsByReferenceType(
            @PathVariable Long userId,
            @PathVariable String referenceType) {
        return ResponseEntity.ok(notificationService.getNotificationsByReferenceType(userId, referenceType));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllAsRead(@PathVariable Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/user/{userId}/type/{type}/read")
    public ResponseEntity<Void> markAsReadByType(
            @PathVariable Long userId,
            @PathVariable NotificationType type) {
        notificationService.markAsReadByType(userId, type);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/user/{userId}/all")
    public ResponseEntity<Void> deleteAllNotifications(@PathVariable Long userId) {
        notificationService.deleteAllNotifications(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/user/{userId}/type/{type}")
    public ResponseEntity<Void> deleteNotificationsByType(
            @PathVariable Long userId,
            @PathVariable NotificationType type) {
        notificationService.deleteNotificationsByType(userId, type);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<Integer> getUnreadNotificationCount(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUnreadNotificationCount(userId));
    }

    @GetMapping("/user/{userId}/type/{type}/unread/count")
    public ResponseEntity<Integer> getUnreadNotificationCountByType(
            @PathVariable Long userId,
            @PathVariable NotificationType type) {
        return ResponseEntity.ok(notificationService.getUnreadNotificationCountByType(userId, type));
    }

    @GetMapping("/user/{userId}/type/{type}/enabled")
    public ResponseEntity<Boolean> isNotificationEnabled(
            @PathVariable Long userId,
            @PathVariable NotificationType type) {
        return ResponseEntity.ok(notificationService.isNotificationEnabled(userId, type));
    }

    @PutMapping("/user/{userId}/type/{type}/enable")
    public ResponseEntity<Void> enableNotification(
            @PathVariable Long userId,
            @PathVariable NotificationType type) {
        notificationService.enableNotification(userId, type);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/user/{userId}/type/{type}/disable")
    public ResponseEntity<Void> disableNotification(
            @PathVariable Long userId,
            @PathVariable NotificationType type) {
        notificationService.disableNotification(userId, type);
        return ResponseEntity.ok().build();
    }
} 