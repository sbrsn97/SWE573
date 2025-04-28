package com.swe573.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "node_details", indexes = {
    @Index(name = "idx_node_details_node", columnList = "node_id"),
    @Index(name = "idx_node_details_wikidata", columnList = "wikidata_entity_id")
})
@Getter
@Setter
public class NodeDetails extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", nullable = false)
    private Node node;

    @Column(name = "wikidata_entity_id", nullable = false)
    @NotBlank
    private String wikidataEntityId;

    @Column(nullable = false)
    @NotBlank
    private String label;

    @Column(columnDefinition = "TEXT")
    private String description;

    @PrePersist
    @PreUpdate
    public void validate() {
        if (node == null) {
            throw new IllegalStateException("NodeDetails must be associated with a node");
        }
        if (wikidataEntityId == null || wikidataEntityId.trim().isEmpty()) {
            throw new IllegalStateException("NodeDetails must have a Wikidata entity ID");
        }
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalStateException("NodeDetails must have a label");
        }
    }
} 