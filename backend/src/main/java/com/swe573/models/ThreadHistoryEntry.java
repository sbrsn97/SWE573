package com.swe573.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "thread_history")
@EqualsAndHashCode(callSuper = true)
public class ThreadHistoryEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private Thread thread;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EntityType entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(length = 2000)
    private String beforeState;

    @Column(length = 2000)
    private String afterState;

    @Column(length = 500)
    private String description;

    public enum ActionType {
        CREATE,
        UPDATE,
        DELETE,
        FOLLOW,
        UNFOLLOW,
        UPVOTE,
        DOWNVOTE,
        REMOVE_VOTE,
        ADD_TAG,
        REMOVE_TAG
    }

    public enum EntityType {
        THREAD,
        NODE,
        EDGE,
        COMMENT,
        TAG
    }
} 