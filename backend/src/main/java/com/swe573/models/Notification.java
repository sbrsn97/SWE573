package com.swe573.models;

import com.swe573.models.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "notifications")
@EqualsAndHashCode(callSuper = true)
public class Notification extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String message;

    @Column(name = "is_read")
    private boolean isRead = false;

    @Column(name = "notification_type")
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(name = "reference_id")
    private Long referenceId; // ID of the thread, comment, or user that triggered the notification

    @Column(name = "reference_type")
    private String referenceType; // Type of the reference (THREAD, COMMENT, USER)

    @Column(name = "action_user_id")
    private Long actionUserId; // ID of the user who performed the action

    @Column(name = "action_username")
    private String actionUsername; // Username of the user who performed the action
} 