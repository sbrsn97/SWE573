package com.swe573.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "threads")
public class Thread {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToMany
    @JoinTable(
        name = "thread_tags",
        joinColumns = @JoinColumn(name = "thread_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    // Users following/subscribed to this thread
    @ManyToMany
    @JoinTable(
        name = "thread_followers",
        joinColumns = @JoinColumn(name = "thread_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> threadFollowers = new HashSet<>();

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL)
    private Set<Comment> comments = new HashSet<>();

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL)
    private Set<Node> nodes = new HashSet<>();

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL)
    private Set<Edge> edges = new HashSet<>();

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL)
    private Set<Vote> votes = new HashSet<>();

    // Cached vote counts to avoid counting every time
    @Column(name = "upvote_count")
    private int upvoteCount = 0;

    @Column(name = "downvote_count")
    private int downvoteCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 