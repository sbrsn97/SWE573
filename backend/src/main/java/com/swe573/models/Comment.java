package com.swe573.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.util.HashSet;
import java.util.Set;

import com.swe573.models.enums.VoteType;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_comment_visibility", columnList = "isactive"),
    @Index(name = "idx_comment_thread", columnList = "thread_id"),
    @Index(name = "idx_comment_author", columnList = "author_id"),
    @Index(name = "idx_comment_parent", columnList = "parent_id")
})
@EqualsAndHashCode(callSuper = true, exclude = {"votes", "thread", "parent", "children"})
public class Comment extends BaseEntity {
    @Column(columnDefinition = "TEXT")
    @NotBlank
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Thread thread;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Comment parent;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Comment> children = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "comment_referenced_nodes",
        joinColumns = @JoinColumn(name = "comment_id"),
        inverseJoinColumns = @JoinColumn(name = "node_id")
    )
    private Set<Node> referencedNodes = new HashSet<>();

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<Vote> votes = new HashSet<>();

    // Cached vote counts to avoid counting every time
    @Column(name = "upvote_count")
    private int upvoteCount = 0;

    @Column(name = "downvote_count")
    private int downvoteCount = 0;

    public void addVote(Vote vote) {
        votes.add(vote);
        if (vote.getType() == VoteType.UPVOTE) {
            upvoteCount++;
        } else {
            downvoteCount++;
        }
    }

    public void removeVote(Vote vote) {
        if (votes.remove(vote)) {
            if (vote.getType() == VoteType.UPVOTE) {
                upvoteCount = Math.max(0, upvoteCount - 1);
            } else {
                downvoteCount = Math.max(0, downvoteCount - 1);
            }
        }
    }

    public int getVoteCount() {
        return upvoteCount - downvoteCount;
    }
    
    public boolean isParentComment() {
        return parent == null;
    }
    
    public boolean hasChildren() {
        return !children.isEmpty();
    }

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
        // Clean up associations
        if (votes != null) {
            for (Vote vote : new HashSet<>(votes)) {
                vote.setComment(null);
                votes.remove(vote);
            }
        }
        
        if (referencedNodes != null) {
            referencedNodes.clear();
        }
        
        if (children != null) {
            for (Comment child : new HashSet<>(children)) {
                child.setParent(null);
                children.remove(child);
            }
        }
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + getId() +
                ", content='" + content + '\'' +
                ", authorId=" + (author != null ? author.getId() : null) +
                ", threadId=" + (thread != null ? thread.getId() : null) +
                ", parentId=" + (parent != null ? parent.getId() : null) +
                ", childCount=" + children.size() +
                ", upvoteCount=" + upvoteCount +
                ", downvoteCount=" + downvoteCount +
                '}';
    }
} 