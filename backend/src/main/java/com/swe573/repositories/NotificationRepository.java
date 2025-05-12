package com.swe573.repositories;

import com.swe573.models.Notification;
import com.swe573.models.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.type = :type ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(@Param("userId") Long userId, @Param("type") NotificationType type);
    
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.referenceType = :referenceType ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndReferenceTypeOrderByCreatedAtDesc(@Param("userId") Long userId, @Param("referenceType") String referenceType);
    
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.type = :type AND n.isRead = false")
    List<Notification> findByUserIdAndTypeAndIsReadFalse(@Param("userId") Long userId, @Param("type") NotificationType type);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    long countByUserIdAndIsReadFalse(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.type = :type AND n.isRead = false")
    long countByUserIdAndTypeAndIsReadFalse(@Param("userId") Long userId, @Param("type") NotificationType type);
    
    // Add a method to get paginated notifications for better performance
    @Query(value = "SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<Notification> findPaginatedByUserId(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);
} 