package com.swe573.services;

import com.swe573.models.*;
import com.swe573.models.Thread;
import com.swe573.repositories.NodeRepository;
import com.swe573.repositories.EdgeRepository;
import com.swe573.repositories.ThreadRepository;
import com.swe573.repositories.UserRepository;
import com.swe573.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    private final UserRepository userRepository;
    private final NlpService nlpService;
    private final ThreadHistoryService threadHistoryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Node Operations
    @Transactional
    public Node createNode(Long threadId, String label, Double xPosition, Double yPosition, String color, String shape, Integer size) {
        return createNode(threadId, null, label, xPosition, yPosition, color, shape, size);
    }
    
    @Transactional
    public Node createNode(Long threadId, Long userId, String label, Double xPosition, Double yPosition, String color, String shape, Integer size) {
        return createNode(threadId, userId, label, xPosition, yPosition, color, shape, size, null, null);
    }
    
    @Transactional
    public Node createNode(Long threadId, Long userId, String label, Double xPosition, Double yPosition, 
                          String color, String shape, Integer size, String wikidataEntityId, String description) {
        // Check for profanity in node label
        if (nlpService.containsProfanity(label)) {
            throw new IllegalArgumentException("Node label contains inappropriate language and cannot be created.");
        }
        
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
        
        // Add node details if wikidataEntityId is provided
        if (wikidataEntityId != null) {
            NodeDetails details = new NodeDetails();
            details.setNode(node);
            details.setWikidataEntityId(wikidataEntityId);
            details.setLabel(label);
            details.setDescription(description);
            node.setDetails(details);
        }

        Node savedNode = nodeRepository.save(node);
        
        // Log node creation to history if user ID is provided
        if (userId != null) {
            try {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
                
                String nodeDetails = objectMapper.writeValueAsString(NodeDTO.fromEntity(savedNode));
                threadHistoryService.logNodeCreation(thread, user, savedNode.getId(), nodeDetails);
            } catch (JsonProcessingException e) {
                // Log error but don't fail the operation
                System.err.println("Error serializing node for history: " + e.getMessage());
            } catch (Exception e) {
                // Log error but don't fail the operation
                System.err.println("Error logging node creation: " + e.getMessage());
            }
        }

        return savedNode;
    }

    @Transactional
    public List<Node> createNodesBatch(Long threadId, List<BatchNodeDTO> nodes) {
        return createNodesBatch(threadId, null, nodes);
    }
    
    @Transactional
    public List<Node> createNodesBatch(Long threadId, Long userId, List<BatchNodeDTO> nodes) {
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new EntityNotFoundException("Thread not found"));

        // Get user if userId is provided
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        }

        List<Node> createdNodes = new ArrayList<>();
        for (BatchNodeDTO nodeDTO : nodes) {
            if (nlpService.containsProfanity(nodeDTO.getLabel())) {
                throw new IllegalArgumentException("Node label contains inappropriate language and cannot be created.");
            }
            
            Node node = new Node();
            node.setLabel(nodeDTO.getLabel());
            node.setThread(thread);
            node.setXPosition(nodeDTO.getXPosition());
            node.setYPosition(nodeDTO.getYPosition());
            node.setColor(nodeDTO.getColor());
            node.setShape(nodeDTO.getShape());
            node.setSize(nodeDTO.getSize());

            // Handle details if provided
            if (nodeDTO.getDescription() != null || nodeDTO.getWikidataEntityId() != null) {
                NodeDetails details = new NodeDetails();
                details.setNode(node);
                details.setLabel(nodeDTO.getLabel());
                details.setDescription(nodeDTO.getDescription());
                details.setWikidataEntityId(nodeDTO.getWikidataEntityId());
                node.setDetails(details);
            }

            Node savedNode = nodeRepository.save(node);
            createdNodes.add(savedNode);
            
            // Log node creation to history if user is available
            if (user != null) {
                try {
                    String nodeDetails = objectMapper.writeValueAsString(NodeDTO.fromEntity(savedNode));
                    threadHistoryService.logNodeCreation(thread, user, savedNode.getId(), nodeDetails);
                } catch (JsonProcessingException e) {
                    // Log error but don't fail the operation
                    System.err.println("Error serializing node for history: " + e.getMessage());
                } catch (Exception e) {
                    // Log error but don't fail the operation
                    System.err.println("Error logging node creation: " + e.getMessage());
                }
            }
        }
        return createdNodes;
    }

    @Transactional
    public Node updateNode(Long nodeId, NodeUpdateDTO updateDTO) {
        return updateNode(nodeId, null, updateDTO);
    }
    
    @Transactional
    public Node updateNode(Long nodeId, Long userId, NodeUpdateDTO updateDTO) {
        // Check for profanity in node label
        if (updateDTO.getLabel() != null && nlpService.containsProfanity(updateDTO.getLabel())) {
            throw new IllegalArgumentException("Node label contains inappropriate language and cannot be updated.");
        }
        
        Node node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("Node not found"));
        
        // Store the original state for history logging
        Node originalNode = new Node();
        originalNode.setId(node.getId());
        originalNode.setLabel(node.getLabel());
        originalNode.setXPosition(node.getXPosition());
        originalNode.setYPosition(node.getYPosition());
        originalNode.setColor(node.getColor());
        originalNode.setShape(node.getShape());
        originalNode.setSize(node.getSize());
        if (node.getDetails() != null) {
            NodeDetails originalDetails = new NodeDetails();
            originalDetails.setWikidataEntityId(node.getDetails().getWikidataEntityId());
            originalDetails.setDescription(node.getDetails().getDescription());
            originalNode.setDetails(originalDetails);
        }

        // Update node properties
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

        if (updateDTO.getWikidataEntityId() != null || updateDTO.getDescription() != null) {
            // Update node details if they exist or create if needed
            if (node.getDetails() == null) {
                NodeDetails details = new NodeDetails();
                details.setNode(node);
                details.setWikidataEntityId(updateDTO.getWikidataEntityId() != null ? 
                    updateDTO.getWikidataEntityId() : "default");
                details.setLabel(node.getLabel());
                node.setDetails(details);
            }

            // Update node details properties
            if (updateDTO.getWikidataEntityId() != null) {
                node.getDetails().setWikidataEntityId(updateDTO.getWikidataEntityId());
            }
            if (updateDTO.getDescription() != null) {
                node.getDetails().setDescription(updateDTO.getDescription());
            }
        }

        node.setVersion(node.getVersion() + 1);
        
        // Save and flush to ensure the entity is persisted immediately
        Node savedNode = nodeRepository.saveAndFlush(node);
        
        // Log node update to history if user ID is provided
        if (userId != null) {
            try {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
                
                String beforeState = objectMapper.writeValueAsString(NodeDTO.fromEntity(originalNode));
                String afterState = objectMapper.writeValueAsString(NodeDTO.fromEntity(savedNode));
                
                threadHistoryService.logNodeUpdate(
                    savedNode.getThread(),
                    user,
                    savedNode.getId(),
                    beforeState,
                    afterState
                );
            } catch (JsonProcessingException e) {
                // Log error but don't fail the operation
                System.err.println("Error serializing node for history: " + e.getMessage());
            } catch (Exception e) {
                // Log error but don't fail the operation
                System.err.println("Error logging node update: " + e.getMessage());
            }
        }
        
        // Return the updated node
        return savedNode;
    }

    @Transactional
    public void deleteNode(Long nodeId) {
        deleteNode(nodeId, null);
    }
    
    @Transactional
    public void deleteNode(Long nodeId, Long userId) {
        Node node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("Node not found"));
        
        // Store the original state for history logging
        String beforeState = null;
        User user = null;
        
        if (userId != null) {
            try {
                user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
                beforeState = objectMapper.writeValueAsString(NodeDTO.fromEntity(node));
            } catch (JsonProcessingException e) {
                // Log error but don't fail the operation
                System.err.println("Error serializing node for history: " + e.getMessage());
            } catch (Exception e) {
                // Log error but don't fail the operation
                System.err.println("Error preparing for node deletion log: " + e.getMessage());
            }
        }

        // Delete all edges connected to this node
        edgeRepository.deleteBySourceNodeOrTargetNode(node, node);
        
        // Get thread reference before deleting the node
        Thread thread = node.getThread();
        
        // Delete the node
        nodeRepository.delete(node);
        
        // Log node deletion to history if user ID is provided
        if (userId != null && user != null && beforeState != null) {
            try {
                threadHistoryService.logNodeDeletion(
                    thread,
                    user,
                    nodeId,
                    beforeState
                );
            } catch (Exception e) {
                // Log error but don't fail the operation
                System.err.println("Error logging node deletion: " + e.getMessage());
            }
        }
    }

    public Node getNode(Long nodeId) {
        return nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("Node not found"));
    }

    public List<Node> getNodesByThread(Long threadId) {
        return nodeRepository.findByThread_Id(threadId);
    }

    public List<Node> searchNodes(Long threadId, String query) {
        List<Node> nodes = nodeRepository.findByThread_Id(threadId);
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
    public Edge createEdge(Long threadId, Long sourceNodeId, Long targetNodeId, String label, String type, Integer weight, String color, String wikidataPropertyId) {
        return createEdge(threadId, null, sourceNodeId, targetNodeId, label, type, weight, color, wikidataPropertyId);
    }
    
    @Transactional
    public Edge createEdge(Long threadId, Long userId, Long sourceNodeId, Long targetNodeId, String label, String type, Integer weight, String color, String wikidataPropertyId) {
        // Check for profanity in edge label
        if (label != null && nlpService.containsProfanity(label)) {
            throw new IllegalArgumentException("Edge label contains inappropriate language and cannot be created.");
        }
        
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
        edge.setColor(color != null ? color : "#555555"); // Set color with default
        edge.setWikidataPropertyId(wikidataPropertyId);
        edge.setThread(thread);

        Edge savedEdge = edgeRepository.save(edge);
        
        // Log edge creation to history if user ID is provided
        if (userId != null) {
            try {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
                
                String edgeDetails = objectMapper.writeValueAsString(EdgeDTO.fromEntity(savedEdge));
                threadHistoryService.logEdgeCreation(thread, user, savedEdge.getId(), edgeDetails);
            } catch (JsonProcessingException e) {
                // Log error but don't fail the operation
                System.err.println("Error serializing edge for history: " + e.getMessage());
            } catch (Exception e) {
                // Log error but don't fail the operation
                System.err.println("Error logging edge creation: " + e.getMessage());
            }
        }

        return savedEdge;
    }

    @Transactional
    public List<Edge> createEdgesBatch(Long threadId, List<BatchEdgeDTO> edges) {
        return createEdgesBatch(threadId, null, edges);
    }
    
    @Transactional
    public List<Edge> createEdgesBatch(Long threadId, Long userId, List<BatchEdgeDTO> edges) {
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new EntityNotFoundException("Thread not found"));
        
        // Get user if userId is provided
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        }

        List<Edge> createdEdges = new ArrayList<>();
        for (BatchEdgeDTO edgeDTO : edges) {
            // Check for profanity in edge label
            if (edgeDTO.getLabel() != null && nlpService.containsProfanity(edgeDTO.getLabel())) {
                throw new IllegalArgumentException("Edge contains inappropriate language and cannot be created.");
            }
            
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
            edge.setColor(edgeDTO.getColor() != null ? edgeDTO.getColor() : "#555555"); // Set color with default
            edge.setWikidataPropertyId(edgeDTO.getWikidataPropertyId());
            edge.setThread(thread);

            Edge savedEdge = edgeRepository.save(edge);
            createdEdges.add(savedEdge);
            
            // Log edge creation to history if user is available
            if (user != null) {
                try {
                    String edgeDetails = objectMapper.writeValueAsString(EdgeDTO.fromEntity(savedEdge));
                    threadHistoryService.logEdgeCreation(thread, user, savedEdge.getId(), edgeDetails);
                } catch (JsonProcessingException e) {
                    // Log error but don't fail the operation
                    System.err.println("Error serializing edge for history: " + e.getMessage());
                } catch (Exception e) {
                    // Log error but don't fail the operation
                    System.err.println("Error logging edge creation: " + e.getMessage());
                }
            }
        }
        return createdEdges;
    }

    @Transactional
    public Edge updateEdge(Long edgeId, EdgeUpdateDTO updateDTO) {
        return updateEdge(edgeId, null, updateDTO);
    }
    
    @Transactional
    public Edge updateEdge(Long edgeId, Long userId, EdgeUpdateDTO updateDTO) {
        // Check for profanity in updated edge label
        if (updateDTO.getLabel() != null && nlpService.containsProfanity(updateDTO.getLabel())) {
            throw new IllegalArgumentException("Edge contains inappropriate language and cannot be updated.");
        }
        
        // Use the method that eagerly loads the related entities to prevent validation issues
        Edge edge = edgeRepository.findByIdWithNodesAndThreads(edgeId)
                .orElseThrow(() -> new EntityNotFoundException("Edge not found"));
        
        // Store the original state for history logging
        Edge originalEdge = new Edge();
        originalEdge.setId(edge.getId());
        originalEdge.setLabel(edge.getLabel());
        originalEdge.setType(edge.getType());
        originalEdge.setWeight(edge.getWeight());
        originalEdge.setColor(edge.getColor());
        originalEdge.setWikidataPropertyId(edge.getWikidataPropertyId());

        if (updateDTO.getLabel() != null) {
            edge.setLabel(updateDTO.getLabel());
        }
        if (updateDTO.getType() != null) {
            edge.setType(updateDTO.getType());
        }
        if (updateDTO.getWeight() != null) {
            edge.setWeight(updateDTO.getWeight());
        }
        if (updateDTO.getColor() != null) {
            edge.setColor(updateDTO.getColor());
        }
        if (updateDTO.getWikidataPropertyId() != null) {
            edge.setWikidataPropertyId(updateDTO.getWikidataPropertyId());
        }

        Edge savedEdge = edgeRepository.save(edge);
        
        // Log edge update to history if user ID is provided
        if (userId != null) {
            try {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
                
                String beforeState = objectMapper.writeValueAsString(EdgeDTO.fromEntity(originalEdge));
                String afterState = objectMapper.writeValueAsString(EdgeDTO.fromEntity(savedEdge));
                
                threadHistoryService.logEdgeUpdate(
                    savedEdge.getThread(),
                    user,
                    savedEdge.getId(),
                    beforeState,
                    afterState
                );
            } catch (JsonProcessingException e) {
                // Log error but don't fail the operation
                System.err.println("Error serializing edge for history: " + e.getMessage());
            } catch (Exception e) {
                // Log error but don't fail the operation
                System.err.println("Error logging edge update: " + e.getMessage());
            }
        }

        return savedEdge;
    }

    @Transactional
    public void deleteEdge(Long edgeId) {
        deleteEdge(edgeId, null);
    }
    
    @Transactional
    public void deleteEdge(Long edgeId, Long userId) {
        Edge edge = edgeRepository.findById(edgeId)
                .orElseThrow(() -> new EntityNotFoundException("Edge not found"));
        
        // Store the original state for history logging
        String beforeState = null;
        User user = null;
        
        if (userId != null) {
            try {
                user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
                beforeState = objectMapper.writeValueAsString(EdgeDTO.fromEntity(edge));
            } catch (JsonProcessingException e) {
                // Log error but don't fail the operation
                System.err.println("Error serializing edge for history: " + e.getMessage());
            } catch (Exception e) {
                // Log error but don't fail the operation
                System.err.println("Error preparing for edge deletion log: " + e.getMessage());
            }
        }
        
        // Get thread reference before deleting the edge
        Thread thread = edge.getThread();
        
        // Delete the edge
        edgeRepository.delete(edge);
        
        // Log edge deletion to history if user ID is provided
        if (userId != null && user != null && beforeState != null) {
            try {
                threadHistoryService.logEdgeDeletion(
                    thread,
                    user,
                    edgeId,
                    beforeState
                );
            } catch (Exception e) {
                // Log error but don't fail the operation
                System.err.println("Error logging edge deletion: " + e.getMessage());
            }
        }
    }

    public List<Edge> getEdgesByThread(Long threadId) {
        return edgeRepository.findByThread_Id(threadId);
    }

    public List<Edge> getEdgesByNode(Long nodeId) {
        Node node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("Node not found"));

        return edgeRepository.findBySourceNodeOrTargetNode(node, node);
    }
    
    public Set<Node> getConnectedNodes(Long nodeId) {
        Node node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("Node not found"));
        
        List<Edge> edges = edgeRepository.findBySourceNodeOrTargetNode(node, node);
        
        Set<Node> connectedNodes = new HashSet<>();
        for (Edge edge : edges) {
            if (edge.getSourceNode().getId().equals(nodeId)) {
                connectedNodes.add(edge.getTargetNode());
            } else {
                connectedNodes.add(edge.getSourceNode());
            }
        }
        
        return connectedNodes;
    }
} 