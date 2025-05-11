package com.swe573.models;

import com.swe573.models.enums.VoteType;
import jakarta.persistence.*;
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
@EqualsAndHashCode(callSuper = true, exclude = {"thread", "comment"})
public class Vote extends BaseEntity {
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

    /**
     * Checks if this vote is for a thread.
     * @return true if this vote is for a thread, false if it's for a comment
     */
    public boolean isThreadVote() {
        return thread != null;
    }

    /**
     * Checks if this vote is for a comment.
     * @return true if this vote is for a comment, false if it's for a thread
     */
    public boolean isCommentVote() {
        return comment != null;
    }

    @Override
    public String toString() {
        return "Vote{" +
                "id=" + getId() +
                ", type=" + type +
                ", userId=" + (user != null ? user.getId() : null) +
                ", threadId=" + (thread != null ? thread.getId() : null) +
                ", commentId=" + (comment != null ? comment.getId() : null) +
                '}';
    }
} 