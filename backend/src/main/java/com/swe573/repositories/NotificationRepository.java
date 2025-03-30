package com.swe573.repositories;

import com.swe573.models.Notification;
import com.swe573.models.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, NotificationType type);
    List<Notification> findByUserIdAndReferenceTypeOrderByCreatedAtDesc(Long userId, String referenceType);
    List<Notification> findByUserIdAndTypeAndIsReadFalse(Long userId, NotificationType type);
    
    long countByUserIdAndIsReadFalse(Long userId);
    long countByUserIdAndTypeAndIsReadFalse(Long userId, NotificationType type);
} 