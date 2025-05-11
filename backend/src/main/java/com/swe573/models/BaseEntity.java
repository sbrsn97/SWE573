package com.swe573.models;

import com.swe573.models.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "isactive", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deactivated_by_role")
    @Enumerated(EnumType.STRING)
    private DeactivationRole deactivatedByRole;

    public enum DeactivationRole {
        USER,
        ADMIN
    }

    public void softDelete(DeactivationRole role) {
        this.isActive = false;
        this.deactivatedByRole = role;
    }

    public void hardDelete() {
        // This method is meant to be overridden by entities that need custom hard delete logic
    }

    public boolean canBeReactivatedBy(User user) {
        if (isActive) return false;
        if (user.getRole() == Role.ADMIN) return true;
        return deactivatedByRole == DeactivationRole.USER;
    }
} 