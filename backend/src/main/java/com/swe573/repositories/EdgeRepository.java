package com.swe573.repositories;

import com.swe573.models.Edge;
import com.swe573.models.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EdgeRepository extends JpaRepository<Edge, Long> {
    List<Edge> findByThread_Id(Long threadId);
    List<Edge> findBySourceNodeOrTargetNode(Node sourceNode, Node targetNode);
    void deleteBySourceNodeOrTargetNode(Node sourceNode, Node targetNode);
    boolean existsBySourceNodeAndTargetNode(Node sourceNode, Node targetNode);
    
    // Find edge by ID with eager loading of source node, target node, and their threads
    @Query("SELECT e FROM Edge e JOIN FETCH e.sourceNode sn JOIN FETCH e.targetNode tn JOIN FETCH sn.thread JOIN FETCH tn.thread WHERE e.id = :edgeId")
    Optional<Edge> findByIdWithNodesAndThreads(@Param("edgeId") Long edgeId);
} 