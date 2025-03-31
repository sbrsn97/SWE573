package com.swe573.tests;

import com.swe573.models.Notification;
import com.swe573.models.NotificationPreference;
import com.swe573.models.User;
import com.swe573.models.enums.NotificationType;
import com.swe573.repositories.NotificationRepository;
import com.swe573.repositories.NotificationPreferenceRepository;
import com.swe573.repositories.UserRepository;
import com.swe573.services.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User testUser;
    private Notification testNotification;
    private NotificationPreference testPreference;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setUser(testUser);
        testNotification.setMessage("Test notification");
        testNotification.setType(NotificationType.THREAD_UPVOTE);
        testNotification.setReferenceId(1L);
        testNotification.setReferenceType("THREAD");
        testNotification.setRead(false);
        testNotification.setCreatedAt(LocalDateTime.now());

        testPreference = new NotificationPreference();
        testPreference.setId(1L);
        testPreference.setUser(testUser);
        testPreference.setType(NotificationType.THREAD_UPVOTE);
        testPreference.setEnabled(true);
        testPreference.setReferenceId(1L);
        testPreference.setReferenceType("THREAD");
        testPreference.setGlobal(false);
    }

    @Test
    void createNotification_ShouldCreateNotification() {
        when(userRepository.findById(testUser.getId())).thenReturn(java.util.Optional.of(testUser));
        when(preferenceRepository.findByUserIdAndTypeAndReferenceIdAndReferenceType(
            eq(testUser.getId()), eq(testNotification.getType()), any(), any()))
            .thenReturn(java.util.Optional.of(testPreference));

        notificationService.createNotification(
            testUser.getId(),
            testNotification.getMessage(),
            testNotification.getType(),
            testNotification.getReferenceId(),
            testNotification.getReferenceType()
        );

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void createNotification_WithActionUser_ShouldCreateNotification() {
        Long actionUserId = 2L;
        String actionUsername = "actionuser";
        when(userRepository.findById(testUser.getId())).thenReturn(java.util.Optional.of(testUser));
        when(preferenceRepository.findByUserIdAndTypeAndReferenceIdAndReferenceType(
            eq(testUser.getId()), eq(testNotification.getType()), any(), any()))
            .thenReturn(java.util.Optional.of(testPreference));

        notificationService.createNotification(
            testUser.getId(),
            testNotification.getMessage(),
            testNotification.getType(),
            testNotification.getReferenceId(),
            testNotification.getReferenceType(),
            actionUserId,
            actionUsername
        );

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void getUserNotifications_ShouldReturnAllNotifications() {
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId()))
            .thenReturn(notifications);

        List<Notification> result = notificationService.getUserNotifications(testUser.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testNotification.getId(), result.get(0).getId());
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(testUser.getId());
    }

    @Test
    void getUnreadNotifications_ShouldReturnUnreadNotifications() {
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(testUser.getId()))
            .thenReturn(notifications);

        List<Notification> result = notificationService.getUnreadNotifications(testUser.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).isRead());
        verify(notificationRepository).findByUserIdAndIsReadFalseOrderByCreatedAtDesc(testUser.getId());
    }

    @Test
    void markAsRead_ShouldUpdateNotificationStatus() {
        when(notificationRepository.findById(testNotification.getId())).thenReturn(java.util.Optional.of(testNotification));

        notificationService.markAsRead(testNotification.getId());

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void markAllAsRead_ShouldUpdateAllNotifications() {
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(testUser.getId()))
            .thenReturn(notifications);

        notificationService.markAllAsRead(testUser.getId());

        verify(notificationRepository).findByUserIdAndIsReadFalseOrderByCreatedAtDesc(testUser.getId());
        verify(notificationRepository).saveAll(notifications);
    }

    @Test
    void deleteNotification_ShouldDeleteNotification() {
        notificationService.deleteNotification(testNotification.getId());
        verify(notificationRepository).deleteById(testNotification.getId());
    }

    @Test
    void deleteAllNotifications_ShouldDeleteAllUserNotifications() {
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId()))
            .thenReturn(notifications);

        notificationService.deleteAllNotifications(testUser.getId());

        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(testUser.getId());
        verify(notificationRepository).deleteAll(notifications);
    }

    @Test
    void getUnreadNotificationCount_ShouldReturnCount() {
        when(notificationRepository.countByUserIdAndIsReadFalse(testUser.getId())).thenReturn(1L);

        int count = notificationService.getUnreadNotificationCount(testUser.getId());

        assertEquals(1, count);
        verify(notificationRepository).countByUserIdAndIsReadFalse(testUser.getId());
    }

    @Test
    void isNotificationEnabled_ShouldCheckGlobalPreference() {
        when(preferenceRepository.findByUserIdAndTypeAndReferenceIdAndReferenceType(
            eq(testUser.getId()), eq(testPreference.getType()), any(), any()))
            .thenReturn(java.util.Optional.empty());
        when(preferenceRepository.findByUserIdAndTypeAndIsGlobalTrue(
            testUser.getId(), testPreference.getType()))
            .thenReturn(java.util.Optional.of(testPreference));

        boolean isEnabled = notificationService.isNotificationEnabled(
            testUser.getId(), 
            testPreference.getType(),
            null,
            null
        );

        assertTrue(isEnabled);
    }

    @Test
    void enableNotification_ShouldCreateOrUpdatePreference() {
        when(userRepository.findById(testUser.getId())).thenReturn(java.util.Optional.of(testUser));

        notificationService.enableNotification(testUser.getId(), testPreference.getType());

        verify(preferenceRepository).save(any(NotificationPreference.class));
    }

    @Test
    void disableNotification_ShouldUpdatePreference() {
        when(userRepository.findById(testUser.getId())).thenReturn(java.util.Optional.of(testUser));
        when(preferenceRepository.findByUserIdAndTypeAndReferenceIdAndReferenceType(
            eq(testUser.getId()), eq(testPreference.getType()), isNull(), isNull()))
            .thenReturn(java.util.Optional.empty());

        notificationService.disableNotification(
            testUser.getId(),
            testPreference.getType(),
            null,
            null
        );

        verify(preferenceRepository).save(any(NotificationPreference.class));
    }

    @Test
    void isNotificationEnabled_WithReference_ShouldCheckSpecificPreference() {
        when(preferenceRepository.findByUserIdAndTypeAndReferenceIdAndReferenceType(
            testUser.getId(), testPreference.getType(), 1L, "THREAD"))
            .thenReturn(java.util.Optional.of(testPreference));

        boolean isEnabled = notificationService.isNotificationEnabled(
            testUser.getId(),
            testPreference.getType(),
            1L,
            "THREAD"
        );

        assertTrue(isEnabled);
        verify(preferenceRepository, never()).findByUserIdAndTypeAndIsGlobalTrue(any(), any());
    }

    @Test
    void enableNotification_WithReference_ShouldCreateOrUpdatePreference() {
        when(userRepository.findById(testUser.getId())).thenReturn(java.util.Optional.of(testUser));

        notificationService.enableNotification(
            testUser.getId(),
            testPreference.getType(),
            testPreference.getReferenceId(),
            testPreference.getReferenceType()
        );

        verify(preferenceRepository).save(any(NotificationPreference.class));
    }

    @Test
    void disableNotification_WithReference_ShouldUpdatePreference() {
        when(userRepository.findById(testUser.getId())).thenReturn(java.util.Optional.of(testUser));

        notificationService.disableNotification(
            testUser.getId(),
            testPreference.getType(),
            1L,
            "THREAD"
        );

        verify(preferenceRepository).save(any(NotificationPreference.class));
    }

    @Test
    void getUserPreferences_ShouldReturnAllPreferences() {
        List<NotificationPreference> preferences = Arrays.asList(testPreference);
        when(preferenceRepository.findByUserId(testUser.getId())).thenReturn(preferences);

        List<NotificationPreference> result = notificationService.getUserPreferences(testUser.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testPreference.getId(), result.get(0).getId());
        verify(preferenceRepository).findByUserId(testUser.getId());
    }

    @Test
    void getUserGlobalPreferences_ShouldReturnGlobalPreferences() {
        testPreference.setGlobal(true);
        List<NotificationPreference> preferences = Arrays.asList(testPreference);
        when(preferenceRepository.findByUserIdAndIsGlobalTrue(testUser.getId())).thenReturn(preferences);

        List<NotificationPreference> result = notificationService.getUserGlobalPreferences(testUser.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testPreference.getId(), result.get(0).getId());
        verify(preferenceRepository).findByUserIdAndIsGlobalTrue(testUser.getId());
    }

    @Test
    void getUserSpecificPreferences_ShouldReturnSpecificPreferences() {
        List<NotificationPreference> preferences = Arrays.asList(testPreference);
        when(preferenceRepository.findByUserIdAndIsGlobalFalse(testUser.getId())).thenReturn(preferences);

        List<NotificationPreference> result = notificationService.getUserSpecificPreferences(testUser.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testPreference.getId(), result.get(0).getId());
        verify(preferenceRepository).findByUserIdAndIsGlobalFalse(testUser.getId());
    }

    @Test
    void deletePreference_ShouldDeletePreference() {
        notificationService.deletePreference(testPreference.getId());
        verify(preferenceRepository).deleteById(testPreference.getId());
    }

    @Test
    void deleteAllPreferences_ShouldDeleteAllUserPreferences() {
        List<NotificationPreference> preferences = Arrays.asList(testPreference);
        when(preferenceRepository.findByUserId(testUser.getId())).thenReturn(preferences);

        notificationService.deleteAllPreferences(testUser.getId());

        verify(preferenceRepository).findByUserId(testUser.getId());
        verify(preferenceRepository).deleteAll(preferences);
    }
} 