package com.swe573.tests;

import com.swe573.models.Comment;
import com.swe573.models.Thread;
import com.swe573.models.User;
import com.swe573.models.Node;
import com.swe573.dto.CreateCommentDTO;
import com.swe573.repositories.CommentRepository;
import com.swe573.repositories.ThreadRepository;
import com.swe573.repositories.UserRepository;
import com.swe573.repositories.NodeRepository;
import com.swe573.services.NotificationService;
import com.swe573.services.impl.CommentServiceImpl;
import com.swe573.exceptions.ResourceNotFoundException;
import com.swe573.exceptions.UnauthorizedException;
import com.swe573.models.enums.Role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ThreadRepository threadRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User testUser;
    private Thread testThread;
    private Comment testComment;
    private CreateCommentDTO testCreateCommentDTO;
    private Node testNode;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(Role.USER);

        testThread = new Thread();
        testThread.setId(1L);
        testThread.setTitle("Test Thread");
        testThread.setAuthor(testUser);

        testNode = new Node();
        testNode.setId(1L);

        testComment = new Comment();
        testComment.setId(1L);
        testComment.setContent("Test comment");
        testComment.setAuthor(testUser);
        testComment.setThread(testThread);
        testComment.setActive(true);

        testCreateCommentDTO = new CreateCommentDTO();
        testCreateCommentDTO.setContent("Test comment");
        testCreateCommentDTO.setThreadId(1L);
        testCreateCommentDTO.setReferencedNodeIds(Set.of(1L));
    }

    @Test
    void createComment_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(threadRepository.findById(1L)).thenReturn(Optional.of(testThread));
        when(nodeRepository.findAllById(any())).thenReturn(List.of(testNode));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        // Act
        Comment result = commentService.createComment(testCreateCommentDTO, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(testComment.getId(), result.getId());
        assertEquals(testComment.getContent(), result.getContent());
        verify(commentRepository).save(any(Comment.class));
        verify(notificationService, never()).createNotification(anyLong(), anyString(), any(), anyLong(), anyString(), anyLong(), anyString());
    }

    @Test
    void createComment_Success_WithNotification() {
        // Arrange
        User threadAuthor = new User();
        threadAuthor.setId(2L);
        threadAuthor.setUsername("threadauthor");
        testThread.setAuthor(threadAuthor);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(threadRepository.findById(1L)).thenReturn(Optional.of(testThread));
        when(nodeRepository.findAllById(any())).thenReturn(List.of(testNode));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        // Act
        Comment result = commentService.createComment(testCreateCommentDTO, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(testComment.getId(), result.getId());
        assertEquals(testComment.getContent(), result.getContent());
        verify(commentRepository).save(any(Comment.class));
        verify(notificationService).createNotification(
            eq(threadAuthor.getId()),
            contains(testUser.getUsername()),
            any(),
            eq(testComment.getId()),
            eq("COMMENT"),
            eq(testUser.getId()),
            eq(testUser.getUsername())
        );
    }

    @Test
    void createComment_UserNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            commentService.createComment(testCreateCommentDTO, 1L));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void createComment_ThreadNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(threadRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            commentService.createComment(testCreateCommentDTO, 1L));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void findById_Success() {
        // Arrange
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));

        // Act
        Optional<Comment> result = commentService.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testComment.getId(), result.get().getId());
    }

    @Test
    void findByThreadId_Success() {
        // Arrange
        List<Comment> comments = Arrays.asList(testComment);
        when(commentRepository.findByThreadId(1L)).thenReturn(comments);

        // Act
        List<Comment> result = commentService.findByThreadId(1L);

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(testComment.getId(), result.get(0).getId());
    }

    @Test
    void updateComment_Success() {
        // Arrange
        String newContent = "Updated content";
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        // Act
        Comment result = commentService.updateComment(1L, newContent);

        // Assert
        assertNotNull(result);
        assertEquals(newContent, result.getContent());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void updateComment_NotFound() {
        // Arrange
        when(commentRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            commentService.updateComment(1L, "Updated content"));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void softDeleteComment_Success() {
        // Arrange
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        // Act
        commentService.softDeleteComment(1L, 1L);

        // Assert
        verify(commentRepository).save(any(Comment.class));
        assertFalse(testComment.isActive());
    }

    @Test
    void softDeleteComment_UnauthorizedUser() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setRole(Role.USER);

        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> 
            commentService.softDeleteComment(1L, 2L));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void hardDeleteComment_Success() {
        // Arrange
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));

        // Act
        commentService.hardDeleteComment(1L);

        // Assert
        verify(commentRepository).delete(testComment);
    }

    @Test
    void reactivateComment_Success() {
        // Arrange
        testComment.setActive(false);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        // Act
        Comment result = commentService.reactivateComment(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isActive());
        verify(commentRepository).save(any(Comment.class));
    }
} 