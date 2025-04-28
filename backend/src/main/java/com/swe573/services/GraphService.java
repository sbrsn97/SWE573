package com.swe573.services;

import com.swe573.models.*;
import com.swe573.models.Thread;
import com.swe573.repositories.NodeRepository;
import com.swe573.repositories.EdgeRepository;
import com.swe573.repositories.ThreadRepository;
import com.swe573.dto.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GraphService {

    private final NodeRepository nodeRepository;
    private final EdgeRepository edgeRepository;
    private final ThreadRepository threadRepository;

    // Node Operations
    @Transactional
    public Node createNode(Long threadId, String label, Double xPosition, Double yPosition, String color, String shape, Integer size) {
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new EntityNotFoundException("Thread not found"));

        Node node = new Node();
        node.setLabel(label);
        node.setThread(thread);
        node.setXPosition(xPosition);
        node.setYPosition(yPosition);
        node.setColor(color);
        node.setShape(shape);
        node.setSize(size);

        return nodeRepository.save(node);
    }

    @Transactional
    public List<Node> createNodesBatch(Long threadId, List<BatchNodeDTO> nodes) {
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new EntityNotFoundException("Thread not found"));

        List<Node> createdNodes = new ArrayList<>();
        for (BatchNodeDTO nodeDTO : nodes) {
            Node node = new Node();
            node.setLabel(nodeDTO.getLabel());
            node.setThread(thread);
            node.setXPosition(nodeDTO.getXPosition());
            node.setYPosition(nodeDTO.getYPosition());
            node.setColor(nodeDTO.getColor());
            node.setShape(nodeDTO.getShape());
            node.setSize(nodeDTO.getSize());

            // Create node details if wikidata entity or description is provided
            if (nodeDTO.getWikidataEntityId() != null || nodeDTO.getDescription() != null) {
                NodeDetails details = new NodeDetails();
                details.setNode(node);
                details.setWikidataEntityId(nodeDTO.getWikidataEntityId());
                details.setDescription(nodeDTO.getDescription());
                node.setDetails(details);
            }

            createdNodes.add(nodeRepository.save(node));
        }
        return createdNodes;
    }

    @Transactional
    public Node updateNode(Long nodeId, NodeUpdateDTO updateDTO) {
        Node node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("Node not found"));

        // Update basic node properties
        if (updateDTO.getLabel() != null) {
            node.setLabel(updateDTO.getLabel());
        }
        if (updateDTO.getXPosition() != null) {
            node.setXPosition(updateDTO.getXPosition());
        }
        if (updateDTO.getYPosition() != null) {
            node.setYPosition(updateDTO.getYPosition());
        }
        if (updateDTO.getColor() != null) {
            node.setColor(updateDTO.getColor());
        }
        if (updateDTO.getShape() != null) {
            node.setShape(updateDTO.getShape());
        }
        if (updateDTO.getSize() != null) {
            node.setSize(updateDTO.getSize());
        }

        // Update node details if they exist
        if (node.getDetails() == null) {
            NodeDetails details = new NodeDetails();
            details.setNode(node);
            node.setDetails(details);
        }

        // Update node details properties
        if (updateDTO.getWikidataEntityId() != null) {
            node.getDetails().setWikidataEntityId(updateDTO.getWikidataEntityId());
        }
        if (updateDTO.getDescription() != null) {
            node.getDetails().setDescription(updateDTO.getDescription());
        }

        node.setVersion(node.getVersion() + 1);
        return nodeRepository.save(node);
    }

    @Transactional
    public void deleteNode(Long nodeId) {
        Node node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("Node not found"));

        // Delete all edges connected to this node
        edgeRepository.deleteBySourceNodeOrTargetNode(node, node);
        
        // Delete the node
        nodeRepository.delete(node);
    }

    public Node getNode(Long nodeId) {
        return nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("Node not found"));
    }

    public List<Node> getNodesByThread(Long threadId) {
        return nodeRepository.findByThreadId(threadId);
    }

    public List<Node> searchNodes(Long threadId, String query) {
        List<Node> nodes = nodeRepository.findByThreadId(threadId);
        return nodes.stream()
                .filter(node -> 
                    node.getLabel().toLowerCase().contains(query.toLowerCase()) ||
                    (node.getDetails() != null && 
                     (node.getDetails().getWikidataEntityId().toLowerCase().contains(query.toLowerCase()) ||
                      (node.getDetails().getDescription() != null && 
                       node.getDetails().getDescription().toLowerCase().contains(query.toLowerCase()))))
                )
                .collect(Collectors.toList());
    }

    // Edge Operations
    @Transactional
    public Edge createEdge(Long threadId, Long sourceNodeId, Long targetNodeId, String label, String type, Integer weight) {
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new EntityNotFoundException("Thread not found"));

        Node sourceNode = nodeRepository.findById(sourceNodeId)
                .orElseThrow(() -> new EntityNotFoundException("Source node not found"));

        Node targetNode = nodeRepository.findById(targetNodeId)
                .orElseThrow(() -> new EntityNotFoundException("Target node not found"));

        // Validate that both nodes belong to the same thread
        if (!sourceNode.getThread().getId().equals(threadId) || !targetNode.getThread().getId().equals(threadId)) {
            throw new IllegalStateException("Both nodes must belong to the same thread");
        }

        // Check if edge already exists
        if (edgeRepository.existsBySourceNodeAndTargetNode(sourceNode, targetNode)) {
            throw new IllegalStateException("Edge already exists between these nodes");
        }

        Edge edge = new Edge();
        edge.setSourceNode(sourceNode);
        edge.setTargetNode(targetNode);
        edge.setLabel(label);
        edge.setType(type);
        edge.setWeight(weight);
        edge.setThread(thread);

        return edgeRepository.save(edge);
    }

    @Transactional
    public List<Edge> createEdgesBatch(Long threadId, List<BatchEdgeDTO> edges) {
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new EntityNotFoundException("Thread not found"));

        List<Edge> createdEdges = new ArrayList<>();
        for (BatchEdgeDTO edgeDTO : edges) {
            Node sourceNode = nodeRepository.findById(edgeDTO.getSourceNodeId())
                    .orElseThrow(() -> new EntityNotFoundException("Source node not found"));

            Node targetNode = nodeRepository.findById(edgeDTO.getTargetNodeId())
                    .orElseThrow(() -> new EntityNotFoundException("Target node not found"));

            // Validate that both nodes belong to the same thread
            if (!sourceNode.getThread().getId().equals(threadId) || !targetNode.getThread().getId().equals(threadId)) {
                throw new IllegalStateException("Both nodes must belong to the same thread");
            }

            // Check if edge already exists
            if (edgeRepository.existsBySourceNodeAndTargetNode(sourceNode, targetNode)) {
                continue; // Skip if edge already exists
            }

            Edge edge = new Edge();
            edge.setSourceNode(sourceNode);
            edge.setTargetNode(targetNode);
            edge.setLabel(edgeDTO.getLabel());
            edge.setType(edgeDTO.getType());
            edge.setWeight(edgeDTO.getWeight());
            edge.setThread(thread);

            createdEdges.add(edgeRepository.save(edge));
        }
        return createdEdges;
    }

    @Transactional
    public Edge updateEdge(Long edgeId, EdgeUpdateDTO updateDTO) {
        Edge edge = edgeRepository.findById(edgeId)
                .orElseThrow(() -> new EntityNotFoundException("Edge not found"));

        if (updateDTO.getLabel() != null) {
            edge.setLabel(updateDTO.getLabel());
        }
        if (updateDTO.getType() != null) {
            edge.setType(updateDTO.getType());
        }
        if (updateDTO.getWeight() != null) {
            edge.setWeight(updateDTO.getWeight());
        }

        return edgeRepository.save(edge);
    }

    @Transactional
    public void deleteEdge(Long edgeId) {
        Edge edge = edgeRepository.findById(edgeId)
                .orElseThrow(() -> new EntityNotFoundException("Edge not found"));

        edgeRepository.delete(edge);
    }

    public List<Edge> getEdgesByThread(Long threadId) {
        return edgeRepository.findByThreadId(threadId);
    }

    public List<Edge> getEdgesByNode(Long nodeId) {
        Node node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("Node not found"));

        return edgeRepository.findBySourceNodeOrTargetNode(node, node);
    }

    public List<Edge> searchEdges(Long threadId, String query) {
        List<Edge> edges = edgeRepository.findByThreadId(threadId);
        return edges.stream()
                .filter(edge -> 
                    (edge.getLabel() != null && edge.getLabel().toLowerCase().contains(query.toLowerCase())) ||
                    (edge.getType() != null && edge.getType().toLowerCase().contains(query.toLowerCase()))
                )
                .collect(Collectors.toList());
    }

    // Graph Analysis
    public Map<String, Object> getGraphAnalysis(Long threadId) {
        List<Node> nodes = nodeRepository.findByThreadId(threadId);
        List<Edge> edges = edgeRepository.findByThreadId(threadId);

        Map<String, Object> analysis = new HashMap<>();
        analysis.put("totalNodes", nodes.size());
        analysis.put("totalEdges", edges.size());

        // Calculate connected components
        Set<Set<Node>> components = new HashSet<>();
        Set<Node> visited = new HashSet<>();

        for (Node node : nodes) {
            if (!visited.contains(node)) {
                Set<Node> component = new HashSet<>();
                dfs(node, component, visited, edges);
                components.add(component);
            }
        }

        analysis.put("connectedComponents", components.size());

        // Calculate node degrees
        Map<Node, Integer> nodeDegrees = new HashMap<>();
        for (Edge edge : edges) {
            nodeDegrees.merge(edge.getSourceNode(), 1, Integer::sum);
            nodeDegrees.merge(edge.getTargetNode(), 1, Integer::sum);
        }

        analysis.put("averageDegree", nodeDegrees.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0));

        return analysis;
    }

    public Set<Node> getConnectedNodes(Long nodeId) {
        Node node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("Node not found"));

        Set<Node> connectedNodes = new HashSet<>();
        List<Edge> edges = edgeRepository.findBySourceNodeOrTargetNode(node, node);

        for (Edge edge : edges) {
            if (edge.getSourceNode().equals(node)) {
                connectedNodes.add(edge.getTargetNode());
            } else {
                connectedNodes.add(edge.getSourceNode());
            }
        }

        return connectedNodes;
    }

    private void dfs(Node node, Set<Node> component, Set<Node> visited, List<Edge> edges) {
        visited.add(node);
        component.add(node);

        for (Edge edge : edges) {
            Node neighbor = null;
            if (edge.getSourceNode().equals(node)) {
                neighbor = edge.getTargetNode();
            } else if (edge.getTargetNode().equals(node)) {
                neighbor = edge.getSourceNode();
            }

            if (neighbor != null && !visited.contains(neighbor)) {
                dfs(neighbor, component, visited, edges);
            }
        }
    }
} 