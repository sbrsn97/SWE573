package com.swe573.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "nodes", indexes = {
    @Index(name = "idx_node_thread", columnList = "thread_id"),
    @Index(name = "idx_node_visibility", columnList = "is_active")
})
@Getter
@Setter
public class Node extends BaseEntity {

    @NotBlank
    private String label;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id")
    private Thread thread;

    @Column(nullable = false)
    private Double xPosition;

    @Column(nullable = false)
    private Double yPosition;

    @Column(nullable = false)
    private String color = "#000000";

    @Column(nullable = false)
    private String shape = "circle";

    @Column(nullable = false)
    private Integer size = 30;

    @Column(nullable = false)
    private Integer version = 1;

    @PrePersist
    @PreUpdate
    public void validate() {
        if (thread == null) {
            throw new IllegalStateException("Node must be associated with a thread");
        }
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalStateException("Node must have a label");
        }
        if (xPosition == null || yPosition == null) {
            throw new IllegalStateException("Node must have position coordinates");
        }
    }
} 