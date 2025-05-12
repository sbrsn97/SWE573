package com.swe573.tests;

import com.swe573.models.Thread;
import com.swe573.models.User;
import com.swe573.models.Vote;
import com.swe573.models.Comment;
import com.swe573.models.enums.VoteType;
import com.swe573.repositories.ThreadRepository;
import com.swe573.repositories.UserRepository;
import com.swe573.repositories.VoteRepository;
import com.swe573.repositories.CommentRepository;
import com.swe573.services.impl.VoteServiceImpl;
import com.swe573.services.NotificationService;
import com.swe573.services.ThreadHistoryService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VoteServiceTest {

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ThreadRepository threadRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private NotificationService notificationService;
    
    @Mock
    private ThreadHistoryService threadHistoryService;
    
    @Mock
    private EntityManager entityManager;
    
    @Mock
    private Query mockQuery;

    @InjectMocks
    private VoteServiceImpl voteService;

    private User user;
    private Thread thread;
    private Comment comment;
    private Vote vote;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testUser");

        thread = new Thread();
        thread.setId(1L);
        thread.setTitle("Test Thread");
        thread.setAuthor(user);
        thread.setVotes(new HashSet<>());
        thread.setUpvoteCount(0);
        thread.setDownvoteCount(0);

        comment = new Comment();
        comment.setId(1L);
        comment.setContent("Test Comment");
        comment.setThread(thread);
        comment.setAuthor(user);
        comment.setVotes(new HashSet<>());

        vote = new Vote();
        vote.setId(1L);
        vote.setUser(user);
        vote.setThread(thread);
        vote.setType(VoteType.UPVOTE);
        
        // Mock the entity manager and native query behavior
        when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.executeUpdate()).thenReturn(0);
        doNothing().when(entityManager).refresh(any());
        
        // Mock thread history service
        when(threadHistoryService.logThreadVote(any(), any(), anyBoolean())).thenReturn(null);
        when(threadHistoryService.logCommentVote(any(), any(), anyLong(), anyBoolean())).thenReturn(null);
    }

    @Test
    void createThreadVote_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(threadRepository.findById(1L)).thenReturn(Optional.of(thread));
        when(voteRepository.findByUserIdAndThreadId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(threadRepository.save(any(Thread.class))).thenReturn(thread);
        when(voteRepository.save(any(Vote.class))).thenReturn(vote);

        // Act
        Vote result = voteService.createThreadVote(1L, 1L, VoteType.UPVOTE);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(user, result.getUser());
        assertEquals(thread, result.getThread());
        assertEquals(VoteType.UPVOTE, result.getType());
        verify(voteRepository).save(any(Vote.class));
        verify(threadHistoryService).logThreadVote(eq(thread), eq(user), eq(true));
    }

    @Test
    void createThreadVote_UserNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> 
            voteService.createThreadVote(1L, 1L, VoteType.UPVOTE));
        verify(voteRepository, never()).save(any(Vote.class));
    }

    @Test
    void createThreadVote_ThreadNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(threadRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> 
            voteService.createThreadVote(1L, 1L, VoteType.UPVOTE));
        verify(voteRepository, never()).save(any(Vote.class));
    }

    @Test
    void createCommentVote_Success() {
        // Arrange
        Vote commentVote = new Vote();
        commentVote.setId(1L);
        commentVote.setUser(user);
        commentVote.setComment(comment);
        commentVote.setType(VoteType.UPVOTE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(voteRepository.findByUserIdAndCommentId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(voteRepository.save(any(Vote.class))).thenReturn(commentVote);

        // Act
        Vote result = voteService.createCommentVote(1L, 1L, VoteType.UPVOTE);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(user, result.getUser());
        assertEquals(comment, result.getComment());
        assertEquals(VoteType.UPVOTE, result.getType());
        verify(voteRepository).save(any(Vote.class));
        verify(threadHistoryService).logCommentVote(eq(thread), eq(user), eq(1L), eq(true));
    }

    @Test
    void deleteVote_Success() {
        // Arrange
        when(voteRepository.findById(1L)).thenReturn(Optional.of(vote));
        doNothing().when(voteRepository).delete(any(Vote.class));

        // Act
        voteService.deleteVote(1L);

        // Assert
        verify(voteRepository).delete(any(Vote.class));
    }

    @Test
    void deleteVote_NotFound() {
        // Arrange
        when(voteRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> voteService.deleteVote(1L));
        verify(voteRepository, never()).delete(any(Vote.class));
    }

    @Test
    void hasUserVotedOnThread_Success() {
        // Arrange
        when(voteRepository.existsByUserIdAndThreadId(1L, 1L)).thenReturn(true);

        // Act
        boolean result = voteService.hasUserVotedOnThread(1L, 1L);

        // Assert
        assertTrue(result);
    }

    @Test
    void hasUserVotedOnThread_NoVote() {
        // Arrange
        when(voteRepository.existsByUserIdAndThreadId(1L, 1L)).thenReturn(false);

        // Act
        boolean result = voteService.hasUserVotedOnThread(1L, 1L);

        // Assert
        assertFalse(result);
    }

    @Test
    void getUserVoteTypeOnThread_Success() {
        // Arrange
        when(voteRepository.findByUserIdAndThreadId(1L, 1L)).thenReturn(Optional.of(vote));

        // Act
        VoteType result = voteService.getUserVoteTypeOnThread(1L, 1L);

        // Assert
        assertEquals(VoteType.UPVOTE, result);
    }

    @Test
    void getUserVoteTypeOnThread_NoVote() {
        // Arrange
        when(voteRepository.findByUserIdAndThreadId(1L, 1L)).thenReturn(Optional.empty());

        // Act
        VoteType result = voteService.getUserVoteTypeOnThread(1L, 1L);

        // Assert
        assertNull(result);
    }

    @Test
    void getThreadVoteCount_Success() {
        // Arrange
        when(threadRepository.findById(1L)).thenReturn(Optional.of(thread));

        // Act
        int result = voteService.getThreadVoteCount(1L);

        // Assert
        assertEquals(0, result);
    }

    @Test
    void getThreadVoteCount_ThreadNotFound() {
        // Arrange
        when(threadRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> voteService.getThreadVoteCount(1L));
    }
} 