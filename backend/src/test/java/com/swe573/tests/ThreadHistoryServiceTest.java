package com.swe573.tests;

import com.swe573.dto.ThreadHistoryDTO;
import com.swe573.models.Thread;
import com.swe573.models.ThreadHistoryEntry;
import com.swe573.models.User;
import com.swe573.models.enums.Role;
import com.swe573.repositories.ThreadHistoryRepository;
import com.swe573.services.impl.ThreadHistoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ThreadHistoryServiceTest {

    @Mock
    private ThreadHistoryRepository threadHistoryRepository;

    @InjectMocks
    private ThreadHistoryServiceImpl threadHistoryService;

    private Thread testThread;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(Role.USER);

        // Create test thread
        testThread = new Thread();
        testThread.setId(1L);
        testThread.setTitle("Test Thread");
        testThread.setDescription("Test Description");
        testThread.setAuthor(testUser);
    }

    private ThreadHistoryEntry createTestEntry(ThreadHistoryEntry.ActionType actionType, 
                                               ThreadHistoryEntry.EntityType entityType,
                                               Long entityId) {
        ThreadHistoryEntry entry = new ThreadHistoryEntry();
        entry.setThread(testThread);
        entry.setUser(testUser);
        entry.setActionType(actionType);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAfterState("Test Thread");
        entry.setDescription("Test description");
        entry.setCreatedAt(LocalDateTime.now());
        return entry;
    }

    @Test
    void logThreadCreation_Success() {
        // Arrange
        ThreadHistoryEntry mockEntry = createTestEntry(
            ThreadHistoryEntry.ActionType.CREATE, 
            ThreadHistoryEntry.EntityType.THREAD, 
            testThread.getId());
        when(threadHistoryRepository.save(any(ThreadHistoryEntry.class))).thenReturn(mockEntry);
        
        // Act
        ThreadHistoryEntry result = threadHistoryService.logThreadCreation(testThread, testUser);
        
        // Assert
        assertNotNull(result);
        assertEquals(ThreadHistoryEntry.ActionType.CREATE, result.getActionType());
        assertEquals(ThreadHistoryEntry.EntityType.THREAD, result.getEntityType());
        assertEquals(testThread.getId(), result.getEntityId());
        verify(threadHistoryRepository).save(any(ThreadHistoryEntry.class));
    }

    @Test
    void logThreadUpdate_Success() {
        // Arrange
        ThreadHistoryEntry mockEntry = createTestEntry(
            ThreadHistoryEntry.ActionType.UPDATE,
            ThreadHistoryEntry.EntityType.THREAD, 
            testThread.getId());
        when(threadHistoryRepository.save(any(ThreadHistoryEntry.class))).thenReturn(mockEntry);
        
        String beforeState = "Old Title";
        String afterState = "New Title";
        
        // Act
        ThreadHistoryEntry result = threadHistoryService.logThreadUpdate(testThread, testUser, beforeState, afterState);
        
        // Assert
        assertNotNull(result);
        assertEquals(ThreadHistoryEntry.ActionType.UPDATE, result.getActionType());
        assertEquals(ThreadHistoryEntry.EntityType.THREAD, result.getEntityType());
        verify(threadHistoryRepository).save(any(ThreadHistoryEntry.class));
    }

    @Test
    void logThreadDeletion_Success() {
        // Arrange
        ThreadHistoryEntry mockEntry = createTestEntry(
            ThreadHistoryEntry.ActionType.DELETE,
            ThreadHistoryEntry.EntityType.THREAD, 
            testThread.getId());
        when(threadHistoryRepository.save(any(ThreadHistoryEntry.class))).thenReturn(mockEntry);
        
        // Act
        ThreadHistoryEntry result = threadHistoryService.logThreadDeletion(testThread, testUser, "Deleted Thread");
        
        // Assert
        assertNotNull(result);
        assertEquals(ThreadHistoryEntry.ActionType.DELETE, result.getActionType());
        assertEquals(ThreadHistoryEntry.EntityType.THREAD, result.getEntityType());
        verify(threadHistoryRepository).save(any(ThreadHistoryEntry.class));
    }

    @Test
    void logThreadVote_Upvote_Success() {
        // Arrange
        ThreadHistoryEntry mockEntry = createTestEntry(
            ThreadHistoryEntry.ActionType.UPVOTE,
            ThreadHistoryEntry.EntityType.THREAD, 
            testThread.getId());
        when(threadHistoryRepository.save(any(ThreadHistoryEntry.class))).thenReturn(mockEntry);
        
        // Act
        ThreadHistoryEntry result = threadHistoryService.logThreadVote(testThread, testUser, true);
        
        // Assert
        assertNotNull(result);
        assertEquals(ThreadHistoryEntry.ActionType.UPVOTE, result.getActionType());
        assertEquals(ThreadHistoryEntry.EntityType.THREAD, result.getEntityType());
        verify(threadHistoryRepository).save(any(ThreadHistoryEntry.class));
    }

    @Test
    void logThreadVote_Downvote_Success() {
        // Arrange
        ThreadHistoryEntry mockEntry = createTestEntry(
            ThreadHistoryEntry.ActionType.DOWNVOTE,
            ThreadHistoryEntry.EntityType.THREAD, 
            testThread.getId());
        when(threadHistoryRepository.save(any(ThreadHistoryEntry.class))).thenReturn(mockEntry);
        
        // Act
        ThreadHistoryEntry result = threadHistoryService.logThreadVote(testThread, testUser, false);
        
        // Assert
        assertNotNull(result);
        assertEquals(ThreadHistoryEntry.ActionType.DOWNVOTE, result.getActionType());
        assertEquals(ThreadHistoryEntry.EntityType.THREAD, result.getEntityType());
        verify(threadHistoryRepository).save(any(ThreadHistoryEntry.class));
    }

    @Test
    void logNodeCreation_Success() {
        // Arrange
        Long nodeId = 2L;
        ThreadHistoryEntry mockEntry = createTestEntry(
            ThreadHistoryEntry.ActionType.CREATE,
            ThreadHistoryEntry.EntityType.NODE, 
            nodeId);
        when(threadHistoryRepository.save(any(ThreadHistoryEntry.class))).thenReturn(mockEntry);
        
        String nodeDetails = "Node details";
        
        // Act
        ThreadHistoryEntry result = threadHistoryService.logNodeCreation(testThread, testUser, nodeId, nodeDetails);
        
        // Assert
        assertNotNull(result);
        assertEquals(ThreadHistoryEntry.ActionType.CREATE, result.getActionType());
        assertEquals(ThreadHistoryEntry.EntityType.NODE, result.getEntityType());
        assertEquals(nodeId, result.getEntityId());
        verify(threadHistoryRepository).save(any(ThreadHistoryEntry.class));
    }

    @Test
    void logEdgeCreation_Success() {
        // Arrange
        Long edgeId = 3L;
        ThreadHistoryEntry mockEntry = createTestEntry(
            ThreadHistoryEntry.ActionType.CREATE,
            ThreadHistoryEntry.EntityType.EDGE, 
            edgeId);
        when(threadHistoryRepository.save(any(ThreadHistoryEntry.class))).thenReturn(mockEntry);
        
        String edgeDetails = "Edge details";
        
        // Act
        ThreadHistoryEntry result = threadHistoryService.logEdgeCreation(testThread, testUser, edgeId, edgeDetails);
        
        // Assert
        assertNotNull(result);
        assertEquals(ThreadHistoryEntry.ActionType.CREATE, result.getActionType());
        assertEquals(ThreadHistoryEntry.EntityType.EDGE, result.getEntityType());
        assertEquals(edgeId, result.getEntityId());
        verify(threadHistoryRepository).save(any(ThreadHistoryEntry.class));
    }

    @Test
    void getThreadHistory_Success() {
        // Arrange
        ThreadHistoryEntry historyEntry = createTestEntry(
            ThreadHistoryEntry.ActionType.CREATE,
            ThreadHistoryEntry.EntityType.THREAD, 
            testThread.getId());
        List<ThreadHistoryEntry> historyEntries = new ArrayList<>();
        historyEntries.add(historyEntry);
        
        when(threadHistoryRepository.findByThreadIdOrderByCreatedAtDesc(1L)).thenReturn(historyEntries);
        
        // Act
        List<ThreadHistoryDTO> results = threadHistoryService.getThreadHistory(1L);
        
        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(threadHistoryRepository).findByThreadIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void getThreadHistoryPaginated_Success() {
        // Arrange
        ThreadHistoryEntry historyEntry = createTestEntry(
            ThreadHistoryEntry.ActionType.CREATE,
            ThreadHistoryEntry.EntityType.THREAD, 
            testThread.getId());
        List<ThreadHistoryEntry> historyEntries = new ArrayList<>();
        historyEntries.add(historyEntry);
        Page<ThreadHistoryEntry> page = new PageImpl<>(historyEntries);
        
        when(threadHistoryRepository.findByThreadIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class))).thenReturn(page);
        
        // Act
        Page<ThreadHistoryDTO> results = threadHistoryService.getThreadHistoryPaginated(1L, Pageable.unpaged());
        
        // Assert
        assertNotNull(results);
        assertEquals(1, results.getTotalElements());
        verify(threadHistoryRepository).findByThreadIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class));
    }

    @Test
    void getThreadEntityHistory_Success() {
        // Arrange
        ThreadHistoryEntry historyEntry = createTestEntry(
            ThreadHistoryEntry.ActionType.CREATE,
            ThreadHistoryEntry.EntityType.NODE, 
            2L);
        List<ThreadHistoryEntry> historyEntries = new ArrayList<>();
        historyEntries.add(historyEntry);
        
        when(threadHistoryRepository.findByThreadIdAndEntityIdAndEntityTypeOrderByCreatedAtDesc(
            anyLong(), anyLong(), any(ThreadHistoryEntry.EntityType.class)))
            .thenReturn(historyEntries);
        
        // Act
        List<ThreadHistoryDTO> results = threadHistoryService.getThreadEntityHistory(
            1L, ThreadHistoryEntry.EntityType.NODE, 2L);
        
        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(threadHistoryRepository).findByThreadIdAndEntityIdAndEntityTypeOrderByCreatedAtDesc(
            eq(1L), eq(2L), eq(ThreadHistoryEntry.EntityType.NODE));
    }

    @Test
    void getUserActionHistory_Success() {
        // Arrange
        ThreadHistoryEntry historyEntry = createTestEntry(
            ThreadHistoryEntry.ActionType.CREATE,
            ThreadHistoryEntry.EntityType.THREAD, 
            testThread.getId());
        List<ThreadHistoryEntry> historyEntries = new ArrayList<>();
        historyEntries.add(historyEntry);
        
        when(threadHistoryRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(historyEntries);
        
        // Act
        List<ThreadHistoryDTO> results = threadHistoryService.getUserActionHistory(1L);
        
        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(threadHistoryRepository).findByUserIdOrderByCreatedAtDesc(1L);
    }
} 