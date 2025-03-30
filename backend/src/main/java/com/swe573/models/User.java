package com.swe573.models;

import com.swe573.models.enums.NotificationType;
import com.swe573.models.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "users")
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String username;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    @Column(unique = true)
    private String email;

    @NotBlank
    private String password;

    private String bio;

    private String location;

    private String profession;

    @Past
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "reputation")
    private int reputation = 0;

    @ElementCollection
    @CollectionTable(name = "user_notification_preferences")
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "enabled")
    private Map<NotificationType, Boolean> notificationPreferences = new HashMap<>();

    // Users who follow this user
    @ManyToMany
    @JoinTable(
        name = "user_followers",
        joinColumns = @JoinColumn(name = "followed_id"),
        inverseJoinColumns = @JoinColumn(name = "follower_id")
    )
    private Set<User> followers = new HashSet<>();

    // Users that this user follows
    @ManyToMany(mappedBy = "followers")
    private Set<User> following = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "user_tags",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "author")
    private Set<Thread> authoredThreads = new HashSet<>();

    // Threads that this user follows
    @ManyToMany(mappedBy = "threadFollowers")
    private Set<Thread> followedThreads = new HashSet<>();
} 