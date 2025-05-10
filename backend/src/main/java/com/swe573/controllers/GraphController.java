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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;

    // Node endpoints
    @PostMapping("/threads/{threadId}/nodes")
    public ResponseEntity<ApiResponse<NodeDTO>> createNode(
            @PathVariable Long threadId,
            @RequestParam String label,
            @RequestParam Double xPosition,
            @RequestParam Double yPosition,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String shape,
            @RequestParam(required = false) Integer size) {
        Node node = graphService.createNode(threadId, label, xPosition, yPosition, color, shape, size);
        return ResponseEntity.ok(ApiResponse.success("Node created successfully", NodeDTO.fromEntity(node)));
    }

    @PostMapping("/threads/{threadId}/nodes/batch")
    public ResponseEntity<ApiResponse<List<NodeDTO>>> createNodesBatch(
            @PathVariable Long threadId,
            @RequestBody List<BatchNodeDTO> nodes) {
        List<Node> createdNodes = graphService.createNodesBatch(threadId, nodes);
        List<NodeDTO> nodeDTOs = createdNodes.stream()
                .map(NodeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Nodes created successfully", nodeDTOs));
    }

    @PutMapping("/nodes/{nodeId}")
    public ResponseEntity<ApiResponse<NodeDTO>> updateNode(
            @PathVariable Long nodeId,
            @RequestBody NodeUpdateDTO updateDTO) {
        Node node = graphService.updateNode(nodeId, updateDTO);
        return ResponseEntity.ok(ApiResponse.success("Node updated successfully", NodeDTO.fromEntity(node)));
    }

    @DeleteMapping("/nodes/{nodeId}")
    public ResponseEntity<ApiResponse<Void>> deleteNode(@PathVariable Long nodeId) {
        graphService.deleteNode(nodeId);
        return ResponseEntity.ok(ApiResponse.success("Node deleted successfully", null));
    }

    @GetMapping("/nodes/{nodeId}")
    public ResponseEntity<ApiResponse<NodeDTO>> getNode(@PathVariable Long nodeId) {
        Node node = graphService.getNode(nodeId);
        return ResponseEntity.ok(ApiResponse.success("Node retrieved successfully", NodeDTO.fromEntity(node)));
    }

    @GetMapping("/threads/{threadId}/nodes")
    public ResponseEntity<ApiResponse<List<NodeDTO>>> getNodesByThread(@PathVariable Long threadId) {
        List<Node> nodes = graphService.getNodesByThread(threadId);
        List<NodeDTO> nodeDTOs = nodes.stream()
                .map(NodeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Nodes retrieved successfully", nodeDTOs));
    }

    @GetMapping("/threads/{threadId}/nodes/search")
    public ResponseEntity<ApiResponse<List<NodeDTO>>> searchNodes(
            @PathVariable Long threadId,
            @RequestParam String query) {
        List<Node> nodes = graphService.searchNodes(threadId, query);
        List<NodeDTO> nodeDTOs = nodes.stream()
                .map(NodeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Nodes searched successfully", nodeDTOs));
    }

    // Edge endpoints
    @PostMapping("/threads/{threadId}/edges")
    public ResponseEntity<ApiResponse<EdgeDTO>> createEdge(
            @PathVariable Long threadId,
            @RequestParam Long sourceNodeId,
            @RequestParam Long targetNodeId,
            @RequestParam(required = false) String label,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer weight,
            @RequestParam(required = false) String color) {
        Edge edge = graphService.createEdge(threadId, sourceNodeId, targetNodeId, label, type, weight, color);
        return ResponseEntity.ok(ApiResponse.success("Edge created successfully", EdgeDTO.fromEntity(edge)));
    }

    @PostMapping("/threads/{threadId}/edges/batch")
    public ResponseEntity<ApiResponse<List<EdgeDTO>>> createEdgesBatch(
            @PathVariable Long threadId,
            @RequestBody List<BatchEdgeDTO> edges) {
        List<Edge> createdEdges = graphService.createEdgesBatch(threadId, edges);
        List<EdgeDTO> edgeDTOs = createdEdges.stream()
                .map(EdgeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Edges created successfully", edgeDTOs));
    }

    @PutMapping("/edges/{edgeId}")
    public ResponseEntity<ApiResponse<EdgeDTO>> updateEdge(
            @PathVariable Long edgeId,
            @RequestBody EdgeUpdateDTO updateDTO) {
        Edge edge = graphService.updateEdge(edgeId, updateDTO);
        return ResponseEntity.ok(ApiResponse.success("Edge updated successfully", EdgeDTO.fromEntity(edge)));
    }

    @DeleteMapping("/edges/{edgeId}")
    public ResponseEntity<ApiResponse<Void>> deleteEdge(@PathVariable Long edgeId) {
        graphService.deleteEdge(edgeId);
        return ResponseEntity.ok(ApiResponse.success("Edge deleted successfully", null));
    }

    @GetMapping("/threads/{threadId}/edges")
    public ResponseEntity<ApiResponse<List<EdgeDTO>>> getEdgesByThread(@PathVariable Long threadId) {
        List<Edge> edges = graphService.getEdgesByThread(threadId);
        List<EdgeDTO> edgeDTOs = edges.stream()
                .map(EdgeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Edges retrieved successfully", edgeDTOs));
    }

    @GetMapping("/nodes/{nodeId}/edges")
    public ResponseEntity<ApiResponse<List<EdgeDTO>>> getEdgesByNode(@PathVariable Long nodeId) {
        List<Edge> edges = graphService.getEdgesByNode(nodeId);
        List<EdgeDTO> edgeDTOs = edges.stream()
                .map(EdgeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Edges retrieved successfully", edgeDTOs));
    }

    @GetMapping("/threads/{threadId}/edges/search")
    public ResponseEntity<ApiResponse<List<EdgeDTO>>> searchEdges(
            @PathVariable Long threadId,
            @RequestParam String query) {
        List<Edge> edges = graphService.searchEdges(threadId, query);
        List<EdgeDTO> edgeDTOs = edges.stream()
                .map(EdgeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Edges searched successfully", edgeDTOs));
    }

    // Graph Analysis endpoints
    @GetMapping("/threads/{threadId}/analysis")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGraphAnalysis(@PathVariable Long threadId) {
        Map<String, Object> analysis = graphService.getGraphAnalysis(threadId);
        return ResponseEntity.ok(ApiResponse.success("Graph analysis retrieved successfully", analysis));
    }

    @GetMapping("/nodes/{nodeId}/connections")
    public ResponseEntity<ApiResponse<List<NodeDTO>>> getConnectedNodes(@PathVariable Long nodeId) {
        Set<Node> nodes = graphService.getConnectedNodes(nodeId);
        List<NodeDTO> nodeDTOs = nodes.stream()
                .map(NodeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Connected nodes retrieved successfully", nodeDTOs));
    }
} 