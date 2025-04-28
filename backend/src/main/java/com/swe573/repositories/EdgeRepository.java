package com.swe573.repositories;

import com.swe573.models.Edge;
import com.swe573.models.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EdgeRepository extends JpaRepository<Edge, Long> {
    List<Edge> findByThreadId(Long threadId);
    List<Edge> findBySourceNodeOrTargetNode(Node sourceNode, Node targetNode);
    void deleteBySourceNodeOrTargetNode(Node sourceNode, Node targetNode);
    boolean existsBySourceNodeAndTargetNode(Node sourceNode, Node targetNode);
} 