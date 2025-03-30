package com.swe573.repositories;

import com.swe573.models.NotificationPreference;
import com.swe573.models.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    List<NotificationPreference> findByUserId(Long userId);
    
    Optional<NotificationPreference> findByUserIdAndTypeAndReferenceIdIsNullAndReferenceTypeIsNull(Long userId, NotificationType type);
    
    Optional<NotificationPreference> findByUserIdAndTypeAndReferenceIdAndReferenceType(
        Long userId, NotificationType type, Long referenceId, String referenceType);
    
    List<NotificationPreference> findByUserIdAndReferenceIdIsNullAndReferenceTypeIsNull(Long userId);
    
    List<NotificationPreference> findByUserIdAndReferenceIdIsNotNullAndReferenceTypeIsNotNull(Long userId);

    Optional<NotificationPreference> findByUserIdAndTypeAndIsGlobalTrue(Long userId, NotificationType type);
    
    List<NotificationPreference> findByUserIdAndIsGlobalTrue(Long userId);
    
    List<NotificationPreference> findByUserIdAndIsGlobalFalse(Long userId);
} 