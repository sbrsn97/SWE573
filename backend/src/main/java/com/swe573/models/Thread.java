package com.swe573.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import com.swe573.models.enums.VoteType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "threads")
@EqualsAndHashCode(callSuper = true, exclude = {"votes", "comments", "nodes", "edges", "threadFollowers"})
public class Thread extends BaseEntity {
    @NotBlank
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    @JsonIgnoreProperties({"threads", "hibernateLazyInitializer", "handler"})
    private User author;

    @ManyToMany
    @JoinTable(
        name = "thread_tags",
        joinColumns = @JoinColumn(name = "thread_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @JsonIgnoreProperties({"threads", "hibernateLazyInitializer", "handler"})
    private Set<Tag> tags = new HashSet<>();

    // Users following/subscribed to this thread
    @ManyToMany
    @JoinTable(
        name = "thread_followers",
        joinColumns = @JoinColumn(name = "thread_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnoreProperties({"followedThreads", "hibernateLazyInitializer", "handler"})
    private Set<User> threadFollowers = new HashSet<>();

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"thread", "hibernateLazyInitializer", "handler"})
    private Set<Comment> comments = new HashSet<>();

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"thread", "hibernateLazyInitializer", "handler"})
    private Set<Node> nodes = new HashSet<>();

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"thread", "hibernateLazyInitializer", "handler"})
    private Set<Edge> edges = new HashSet<>();

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"thread", "hibernateLazyInitializer", "handler"})
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
        // Clean up associations - clear votes first to avoid referential integrity issues
        votes.clear();
        comments.clear();
        nodes.clear();
        edges.clear();
        tags.clear();
        threadFollowers.clear();
    }

    @Override
    public String toString() {
        return "Thread{" +
                "id=" + getId() +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", authorId=" + (author != null ? author.getId() : null) +
                ", upvoteCount=" + upvoteCount +
                ", downvoteCount=" + downvoteCount +
                '}';
    }
} 