package com.swe573.controllers;

import com.swe573.models.Edge;
import com.swe573.models.Node;
import com.swe573.services.GraphService;
import com.swe573.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;

    // Node endpoints
    @PostMapping("/threads/{threadId}/nodes")
    public ResponseEntity<Node> createNode(
            @PathVariable Long threadId,
            @RequestParam String label,
            @RequestParam Double xPosition,
            @RequestParam Double yPosition,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String shape,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(graphService.createNode(threadId, label, xPosition, yPosition, color, shape, size));
    }

    @PostMapping("/threads/{threadId}/nodes/batch")
    public ResponseEntity<List<Node>> createNodesBatch(
            @PathVariable Long threadId,
            @RequestBody List<BatchNodeDTO> nodes) {
        return ResponseEntity.ok(graphService.createNodesBatch(threadId, nodes));
    }

    @PutMapping("/nodes/{nodeId}")
    public ResponseEntity<Node> updateNode(
            @PathVariable Long nodeId,
            @RequestBody NodeUpdateDTO updateDTO) {
        return ResponseEntity.ok(graphService.updateNode(nodeId, updateDTO));
    }

    @DeleteMapping("/nodes/{nodeId}")
    public ResponseEntity<Void> deleteNode(@PathVariable Long nodeId) {
        graphService.deleteNode(nodeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/nodes/{nodeId}")
    public ResponseEntity<Node> getNode(@PathVariable Long nodeId) {
        return ResponseEntity.ok(graphService.getNode(nodeId));
    }

    @GetMapping("/threads/{threadId}/nodes")
    public ResponseEntity<List<Node>> getNodesByThread(@PathVariable Long threadId) {
        return ResponseEntity.ok(graphService.getNodesByThread(threadId));
    }

    @GetMapping("/threads/{threadId}/nodes/search")
    public ResponseEntity<List<Node>> searchNodes(
            @PathVariable Long threadId,
            @RequestParam String query) {
        return ResponseEntity.ok(graphService.searchNodes(threadId, query));
    }

    // Edge endpoints
    @PostMapping("/threads/{threadId}/edges")
    public ResponseEntity<Edge> createEdge(
            @PathVariable Long threadId,
            @RequestParam Long sourceNodeId,
            @RequestParam Long targetNodeId,
            @RequestParam(required = false) String label,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer weight) {
        return ResponseEntity.ok(graphService.createEdge(threadId, sourceNodeId, targetNodeId, label, type, weight));
    }

    @PostMapping("/threads/{threadId}/edges/batch")
    public ResponseEntity<List<Edge>> createEdgesBatch(
            @PathVariable Long threadId,
            @RequestBody List<BatchEdgeDTO> edges) {
        return ResponseEntity.ok(graphService.createEdgesBatch(threadId, edges));
    }

    @PutMapping("/edges/{edgeId}")
    public ResponseEntity<Edge> updateEdge(
            @PathVariable Long edgeId,
            @RequestBody EdgeUpdateDTO updateDTO) {
        return ResponseEntity.ok(graphService.updateEdge(edgeId, updateDTO));
    }

    @DeleteMapping("/edges/{edgeId}")
    public ResponseEntity<Void> deleteEdge(@PathVariable Long edgeId) {
        graphService.deleteEdge(edgeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/threads/{threadId}/edges")
    public ResponseEntity<List<Edge>> getEdgesByThread(@PathVariable Long threadId) {
        return ResponseEntity.ok(graphService.getEdgesByThread(threadId));
    }

    @GetMapping("/nodes/{nodeId}/edges")
    public ResponseEntity<List<Edge>> getEdgesByNode(@PathVariable Long nodeId) {
        return ResponseEntity.ok(graphService.getEdgesByNode(nodeId));
    }

    @GetMapping("/threads/{threadId}/edges/search")
    public ResponseEntity<List<Edge>> searchEdges(
            @PathVariable Long threadId,
            @RequestParam String query) {
        return ResponseEntity.ok(graphService.searchEdges(threadId, query));
    }

    // Graph Analysis endpoints
    @GetMapping("/threads/{threadId}/analysis")
    public ResponseEntity<Map<String, Object>> getGraphAnalysis(@PathVariable Long threadId) {
        return ResponseEntity.ok(graphService.getGraphAnalysis(threadId));
    }

    @GetMapping("/nodes/{nodeId}/connections")
    public ResponseEntity<Set<Node>> getConnectedNodes(@PathVariable Long nodeId) {
        return ResponseEntity.ok(graphService.getConnectedNodes(nodeId));
    }
} 