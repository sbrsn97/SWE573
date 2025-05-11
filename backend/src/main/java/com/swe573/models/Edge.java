package com.swe573.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "edges", indexes = {
    @Index(name = "idx_edge_source", columnList = "source_node_id"),
    @Index(name = "idx_edge_target", columnList = "target_node_id"),
    @Index(name = "idx_edge_visibility", columnList = "is_active")
})
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, exclude = {"sourceNode", "targetNode", "thread"})
public class Edge extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_node_id")
    @JsonIgnoreProperties({"thread", "details", "hibernateLazyInitializer", "handler"})
    private Node sourceNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_node_id")
    @JsonIgnoreProperties({"thread", "details", "hibernateLazyInitializer", "handler"})
    private Node targetNode;

    @Column
    private String label;

    @Column(nullable = false)
    private String type = "default";

    @Column(nullable = false)
    private Integer weight = 1;

    @Column(nullable = false)
    private String color = "#555555";

    @Column
    private String wikidataPropertyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id")
    @JsonIgnoreProperties({"nodes", "edges", "hibernateLazyInitializer", "handler"})
    private Thread thread;

    @PrePersist
    @PreUpdate
    public void validate() {
        if (sourceNode == null || targetNode == null) {
            throw new IllegalStateException("Edge must connect two nodes");
        }
        if (sourceNode.equals(targetNode)) {
            throw new IllegalStateException("Edge cannot connect a node to itself");
        }
        if (!sourceNode.getThread().equals(targetNode.getThread())) {
            throw new IllegalStateException("Edge cannot connect nodes from different threads");
        }
        if (thread == null) {
            thread = sourceNode.getThread();
        }
    }
} 