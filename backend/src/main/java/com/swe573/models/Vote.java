package com.swe573.models;

import com.swe573.models.enums.VoteType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "votes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "thread_id"}),
    @UniqueConstraint(columnNames = {"user_id", "comment_id"})
})
@EqualsAndHashCode(callSuper = true)
public class Vote extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id")
    private Thread thread;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @PrePersist
    @PreUpdate
    private void validateVoteTarget() {
        if ((thread == null && comment == null) || (thread != null && comment != null)) {
            throw new IllegalStateException("A vote must be associated with either a thread or a comment, but not both or neither");
        }
    }

    @Override
    public String toString() {
        return "Vote{" +
                "id=" + id +
                ", type=" + type +
                ", userId=" + (user != null ? user.getId() : null) +
                ", threadId=" + (thread != null ? thread.getId() : null) +
                ", commentId=" + (comment != null ? comment.getId() : null) +
                '}';
    }
} 