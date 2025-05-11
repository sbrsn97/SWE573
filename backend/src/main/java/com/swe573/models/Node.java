package com.swe573.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "nodes", indexes = {
    @Index(name = "idx_node_thread", columnList = "thread_id"),
    @Index(name = "idx_node_visibility", columnList = "isactive")
})
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, exclude = {"thread", "details"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Node extends BaseEntity {

    @NotBlank
    private String label;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id")
    @JsonIgnoreProperties({"nodes", "hibernateLazyInitializer", "handler"})
    private Thread thread;

    @Column(nullable = false)
    private Double xPosition;

    @Column(nullable = false)
    private Double yPosition;

    @Column(nullable = false)
    private String color = "#000000";

    @Column(nullable = false)
    private String shape = "circle";

    @Column(nullable = false)
    private Integer size = 30;

    @Column(nullable = false)
    private Integer version = 1;

    @OneToOne(mappedBy = "node", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnoreProperties({"node", "hibernateLazyInitializer", "handler"})
    private NodeDetails details;
    
    @OneToMany(mappedBy = "sourceNode", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"sourceNode", "hibernateLazyInitializer", "handler"})
    private Set<Edge> outgoingEdges = new HashSet<>();
    
    @OneToMany(mappedBy = "targetNode", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"targetNode", "hibernateLazyInitializer", "handler"})
    private Set<Edge> incomingEdges = new HashSet<>();

    @PrePersist
    @PreUpdate
    public void validate() {
        if (thread == null) {
            throw new IllegalStateException("Node must be associated with a thread");
        }
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalStateException("Node must have a label");
        }
        if (xPosition == null || yPosition == null) {
            throw new IllegalStateException("Node must have position coordinates");
        }
    }
    
    @Override
    public void hardDelete() {
        // Clear the details
        if (details != null) {
            details.setNode(null);
            details = null;
        }
        
        // Clear edges
        if (outgoingEdges != null) {
            outgoingEdges.clear();
        }
        
        if (incomingEdges != null) {
            incomingEdges.clear();
        }
    }
} 