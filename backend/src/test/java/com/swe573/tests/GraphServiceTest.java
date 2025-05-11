package com.swe573.tests;

import com.swe573.dto.BatchNodeDTO;
import com.swe573.models.Node;
import com.swe573.models.Thread;
import com.swe573.repositories.EdgeRepository;
import com.swe573.repositories.NodeRepository;
import com.swe573.repositories.ThreadRepository;
import com.swe573.services.GraphService;
import com.swe573.services.NlpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class GraphServiceTest {

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private EdgeRepository edgeRepository;

    @Mock
    private ThreadRepository threadRepository;

    @Mock
    private NlpService nlpService;

    @InjectMocks
    private GraphService graphService;

    private Thread testThread;
    private Node testNode;
    private BatchNodeDTO testNodeDTO;

    @BeforeEach
    void setUp() {
        testThread = new Thread();
        testThread.setId(1L);
        testThread.setTitle("Test Thread");

        testNode = new Node();
        testNode.setId(1L);
        testNode.setLabel("Test Node");
        testNode.setThread(testThread);

        testNodeDTO = new BatchNodeDTO();
        testNodeDTO.setLabel("Test Node");
        testNodeDTO.setXPosition(100.0);
        testNodeDTO.setYPosition(100.0);
        testNodeDTO.setDescription("Test Description");
    }

    @Test
    void createNode_Success() {
        // Arrange
        when(threadRepository.findById(1L)).thenReturn(Optional.of(testThread));
        when(nodeRepository.save(any(Node.class))).thenReturn(testNode);
        when(nlpService.containsProfanity("Test Node")).thenReturn(false);

        // Act
        Node result = graphService.createNode(1L, "Test Node", 100.0, 100.0, "#FFFFFF", "circle", 30);

        // Assert
        assertNotNull(result);
        assertEquals("Test Node", result.getLabel());
        verify(threadRepository).findById(1L);
        verify(nodeRepository).save(any(Node.class));
        verify(nlpService).containsProfanity("Test Node");
    }

    @Test
    void createNode_WithProfanity_ThrowsException() {
        // Arrange
        when(nlpService.containsProfanity("Bad Node")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            graphService.createNode(1L, "Bad Node", 100.0, 100.0, "#FFFFFF", "circle", 30);
        });

        assertEquals("Node label contains inappropriate language and cannot be created.", exception.getMessage());
        verify(nlpService).containsProfanity("Bad Node");
        verify(threadRepository, never()).findById(anyLong());
        verify(nodeRepository, never()).save(any(Node.class));
    }

    @Test
    void createNodesBatch_Success() {
        // Arrange
        when(threadRepository.findById(1L)).thenReturn(Optional.of(testThread));
        when(nodeRepository.save(any(Node.class))).thenReturn(testNode);
        when(nlpService.containsProfanity(anyString())).thenReturn(false);

        // Act
        List<Node> result = graphService.createNodesBatch(1L, Collections.singletonList(testNodeDTO));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(threadRepository).findById(1L);
        verify(nodeRepository).save(any(Node.class));
        verify(nlpService).containsProfanity("Test Node");
        verify(nlpService).containsProfanity("Test Description");
    }

    @Test
    void createNodesBatch_WithProfanityInLabel_ThrowsException() {
        // Arrange
        testNodeDTO.setLabel("Bad Node");
        when(nlpService.containsProfanity("Bad Node")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            graphService.createNodesBatch(1L, Collections.singletonList(testNodeDTO));
        });

        assertEquals("Node contains inappropriate language and cannot be created.", exception.getMessage());
        verify(nlpService).containsProfanity("Bad Node");
        verify(threadRepository, never()).findById(anyLong());
        verify(nodeRepository, never()).save(any(Node.class));
    }

    @Test
    void createNodesBatch_WithProfanityInDescription_ThrowsException() {
        // Arrange
        testNodeDTO.setDescription("Bad Description");
        when(nlpService.containsProfanity("Test Node")).thenReturn(false);
        when(nlpService.containsProfanity("Bad Description")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            graphService.createNodesBatch(1L, Collections.singletonList(testNodeDTO));
        });

        assertEquals("Node contains inappropriate language and cannot be created.", exception.getMessage());
        verify(nlpService).containsProfanity("Test Node");
        verify(nlpService).containsProfanity("Bad Description");
        verify(threadRepository, never()).findById(anyLong());
        verify(nodeRepository, never()).save(any(Node.class));
    }
} 