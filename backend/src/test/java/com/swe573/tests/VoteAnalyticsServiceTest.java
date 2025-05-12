package com.swe573.tests;

import com.swe573.models.Comment;
import com.swe573.models.Thread;
import com.swe573.models.User;
import com.swe573.models.Vote;
import com.swe573.models.enums.Role;
import com.swe573.models.enums.VoteType;
import com.swe573.repositories.CommentRepository;
import com.swe573.repositories.ThreadRepository;
import com.swe573.repositories.UserRepository;
import com.swe573.repositories.VoteRepository;
import com.swe573.services.impl.VoteAnalyticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VoteAnalyticsServiceTest {

    @Mock
    private ThreadRepository threadRepository;
    
    @Mock
    private CommentRepository commentRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private VoteRepository voteRepository;
    
    @InjectMocks
    private VoteAnalyticsServiceImpl voteAnalyticsService;
    
    private Thread thread1, thread2;
    private Comment comment1, comment2;
    private User user;
    private List<Vote> votes;
    
    @BeforeEach
    void setUp() {
        // Create test user
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setRole(Role.USER);
        
        // Create test threads
        thread1 = new Thread();
        thread1.setId(1L);
        thread1.setTitle("Thread 1");
        thread1.setAuthor(user);
        thread1.setUpvoteCount(10);
        thread1.setDownvoteCount(2);
        thread1.setCreatedAt(LocalDateTime.now().minusDays(2));
        thread1.setActive(true);
        
        thread2 = new Thread();
        thread2.setId(2L);
        thread2.setTitle("Thread 2");
        thread2.setAuthor(user);
        thread2.setUpvoteCount(5);
        thread2.setDownvoteCount(1);
        thread2.setCreatedAt(LocalDateTime.now().minusDays(1));
        thread2.setActive(true);
        
        // Create test comments
        comment1 = new Comment();
        comment1.setId(1L);
        comment1.setThread(thread1);
        comment1.setAuthor(user);
        comment1.setUpvoteCount(8);
        comment1.setDownvoteCount(1);
        
        comment2 = new Comment();
        comment2.setId(2L);
        comment2.setThread(thread1);
        comment2.setAuthor(user);
        comment2.setUpvoteCount(3);
        comment2.setDownvoteCount(2);
        
        // Create votes list
        votes = new ArrayList<>();
        Vote vote1 = new Vote();
        vote1.setId(1L);
        vote1.setUser(user);
        vote1.setType(VoteType.UPVOTE);
        vote1.setCreatedAt(LocalDateTime.now().minusDays(1));
        votes.add(vote1);
    }
    
    @Test
    void getMostVotedThreads_Success() {
        // Arrange
        List<Thread> threads = Arrays.asList(thread1, thread2);
        when(threadRepository.findAll()).thenReturn(threads);
        
        // Act
        List<Thread> result = voteAnalyticsService.getMostVotedThreads(5);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(thread1.getId(), result.get(0).getId());
        assertEquals(thread2.getId(), result.get(1).getId());
    }
    
    @Test
    void getMostVotedComments_Success() {
        // Arrange
        List<Comment> comments = Arrays.asList(comment1, comment2);
        when(commentRepository.findAll()).thenReturn(comments);
        
        // Act
        List<Comment> result = voteAnalyticsService.getMostVotedComments(5);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }
    
    @Test
    void getMostVotedCommentsByThread_Success() {
        // Arrange
        List<Comment> comments = Arrays.asList(comment1, comment2);
        thread1.setComments(new java.util.HashSet<>(comments));
        when(threadRepository.findById(1L)).thenReturn(java.util.Optional.of(thread1));
        
        // Act
        List<Comment> result = voteAnalyticsService.getMostVotedCommentsByThread(1L, 5);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }
    
    @Test
    void getHotThreads_Success() {
        // Arrange
        List<Thread> threads = Arrays.asList(thread2, thread1); // Newer one first
        when(threadRepository.findAll()).thenReturn(threads);
        
        // Act
        List<Thread> result = voteAnalyticsService.getHotThreads(3, 5);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }
    
    @Test
    void getRecommendedThreadsForUser_Success() {
        // Arrange
        List<Thread> threads = Arrays.asList(thread1, thread2);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));
        when(threadRepository.findAll()).thenReturn(threads);
        
        // Act
        List<Thread> result = voteAnalyticsService.getRecommendedThreadsForUser(1L, 5);
        
        // Assert
        assertNotNull(result);
        // Size might be 0 as both threads have the same author as the user
        // and we filter those out in the service impl
    }
    
    @Test
    void getSimilarThreads_Success() {
        // Arrange
        List<Thread> threads = Arrays.asList(thread2);
        when(threadRepository.findById(1L)).thenReturn(java.util.Optional.of(thread1));
        when(threadRepository.findAll()).thenReturn(threads);
        
        // Act
        List<Thread> result = voteAnalyticsService.getSimilarThreads(1L, 5);
        
        // Assert
        assertNotNull(result);
    }
    
    @Test
    void getUserVoteCount_Success() {
        // Arrange
        when(voteRepository.findAll()).thenReturn(votes);
        
        // Act
        int result = voteAnalyticsService.getUserVoteCount(1L);
        
        // Assert
        assertEquals(1, result);
    }
    
    @Test
    void getUserUpvoteCount_Success() {
        // Arrange
        when(voteRepository.findAll()).thenReturn(votes);
        
        // Act
        int result = voteAnalyticsService.getUserUpvoteCount(1L);
        
        // Assert
        assertEquals(1, result);
    }
    
    @Test
    void getUserDownvoteCount_Success() {
        // Arrange
        when(voteRepository.findAll()).thenReturn(votes);
        
        // Act
        int result = voteAnalyticsService.getUserDownvoteCount(1L);
        
        // Assert
        assertEquals(0, result);
    }
    
    @Test
    void getVoteCountByTimeRange_Success() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        when(voteRepository.findAll()).thenReturn(votes);
        
        // Act
        int result = voteAnalyticsService.getVoteCountByTimeRange(start, end);
        
        // Assert
        assertEquals(1, result);
    }
} 