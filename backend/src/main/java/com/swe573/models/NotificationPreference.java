package com.swe573.models;

import com.swe573.models.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@NoArgsConstructor
@Entity
@Table(
    name = "notification_preferences",
    indexes = {
        @Index(name = "idx_notification_prefs_user_id", columnList = "user_id"),
        @Index(name = "idx_notification_prefs_user_id_type", columnList = "user_id, notification_type"),
        @Index(name = "idx_notification_prefs_user_id_global", columnList = "user_id, is_global"),
        @Index(name = "idx_notification_prefs_user_id_ref", columnList = "user_id, reference_id, reference_type")
    }
)
@EqualsAndHashCode(callSuper = true)
public class NotificationPreference extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "notification_type")
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "is_global")
    private boolean isGlobal = false;

    public void softDeleteByUser() {
        softDelete(DeactivationRole.USER);
    }

    public void softDeleteByAdmin() {
        softDelete(DeactivationRole.ADMIN);
    }

    public void reactivate() {
        setActive(true);
        setDeactivatedByRole(null);
    }

    @Override
    public void hardDelete() {
        // No cleanup needed as this is a simple entity
    }
} 