package com.swe573.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "tags")
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String wikidataEntityId;

    @NotBlank
    private String label;

    private String description;

    private String colorCodeString;

    @ManyToMany(mappedBy = "tags")
    private Set<Thread> threads = new HashSet<>();
} 