package com.swe573.tests;

import com.swe573.models.Thread;
import com.swe573.models.Tag;
import com.swe573.models.User;
import com.swe573.models.Vote;
import com.swe573.models.enums.VoteType;
import com.swe573.repositories.ThreadRepository;
import com.swe573.repositories.TagRepository;
import com.swe573.repositories.UserRepository;
import com.swe573.services.impl.ThreadServiceImpl;
import com.swe573.services.VoteService;
import com.swe573.services.NlpService;
import com.swe573.services.ThreadHistoryService;
import com.swe573.dto.ThreadDTO;
import com.swe573.dto.TagDTO;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ThreadServiceTest {

    @Mock
    private ThreadRepository threadRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private VoteService voteService;
    
    @Mock
    private NlpService nlpService;
    
    @Mock
    private ThreadHistoryService threadHistoryService;

    @InjectMocks
    private ThreadServiceImpl threadService;

    private Thread testThread;
    private User testUser;
    private Tag testTag;
    private ThreadDTO testThreadDTO;
    private Vote testVote;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testTag = new Tag();
        testTag.setId(1L);
        testTag.setLabel("testtag");

        testThread = new Thread();
        testThread.setId(1L);
        testThread.setTitle("Test Thread");
        testThread.setDescription("Test Description");
        testThread.setAuthor(testUser);
        testThread.setTags(new HashSet<>(Collections.singletonList(testTag)));
        testThread.setVotes(new HashSet<>());

        testVote = new Vote();
        testVote.setId(1L);
        testVote.setUser(testUser);
        testVote.setThread(testThread);
        testVote.setType(VoteType.UPVOTE);

        testThreadDTO = new ThreadDTO();
        testThreadDTO.setId(1L);
        testThreadDTO.setTitle("Test Thread");
        testThreadDTO.setDescription("Test Description");
        testThreadDTO.setAuthorId(1L);
        TagDTO testTagDTO = new TagDTO();
        testTagDTO.setLabel("testtag");
        testThreadDTO.setTags(new HashSet<>(Collections.singletonList(testTagDTO)));
        
        // Setup default behavior for mocks
        when(nlpService.containsProfanity(anyString())).thenReturn(false);
        when(threadHistoryService.logThreadCreation(any(), any())).thenReturn(null);
        when(threadHistoryService.logThreadUpdate(any(), any(), anyString(), anyString())).thenReturn(null);
        when(threadHistoryService.logThreadDeletion(any(), any(), anyString())).thenReturn(null);
        when(threadHistoryService.logThreadVote(any(), any(), anyBoolean())).thenReturn(null);
        when(threadHistoryService.logThreadFollow(any(), any())).thenReturn(null);
        when(threadHistoryService.logThreadUnfollow(any(), any())).thenReturn(null);
    }

    @Test
    void createThread_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tagRepository.findByLabel("testtag")).thenReturn(Optional.of(testTag));
        when(threadRepository.save(any(Thread.class))).thenReturn(testThread);

        // Act
        Thread result = threadService.createThread(testThreadDTO);

        // Assert
        assertNotNull(result);
        assertEquals(testThread.getTitle(), result.getTitle());
        assertEquals(testThread.getDescription(), result.getDescription());
        assertEquals(testThread.getAuthor(), result.getAuthor());
        assertEquals(1, result.getTags().size());
        verify(threadRepository).save(any(Thread.class));
        verify(nlpService).containsProfanity(testThreadDTO.getTitle());
        verify(nlpService).containsProfanity(testThreadDTO.getDescription());
        verify(threadHistoryService).logThreadCreation(any(Thread.class), eq(testUser));
    }
    
    @Test
    void createThread_WithProfanity() {
        // Arrange
        when(nlpService.containsProfanity(testThreadDTO.getTitle())).thenReturn(true);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> threadService.createThread(testThreadDTO));
        verify(threadRepository, never()).save(any(Thread.class));
        verify(threadHistoryService, never()).logThreadCreation(any(), any());
    }

    @Test
    void createThread_AuthorNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> threadService.createThread(testThreadDTO));
        verify(threadRepository, never()).save(any(Thread.class));
    }

    @Test
    void getThread_Success() {
        // Arrange
        when(threadRepository.findById(1L)).thenReturn(Optional.of(testThread));

        // Act
        Thread result = threadService.getThread(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testThread.getId(), result.getId());
        assertEquals(testThread.getTitle(), result.getTitle());
    }

    @Test
    void getThread_NotFound() {
        // Arrange
        when(threadRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> threadService.getThread(1L));
    }

    @Test
    void voteThread_Success() {
        // Arrange
        when(threadRepository.findById(1L)).thenReturn(Optional.of(testThread));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(voteService.createThreadVote(1L, 1L, VoteType.UPVOTE)).thenReturn(testVote);
        
        // The ThreadServiceImpl just returns the thread after creating the vote
        // without modifying it, so we don't actually need to mock the save here

        // Act
        Thread result = threadService.voteThread(1L, 1L, true);

        // Assert
        assertNotNull(result);
        verify(voteService).createThreadVote(1L, 1L, VoteType.UPVOTE);
        // Note: The ThreadServiceImpl doesn't call logThreadVote directly, it's called inside VoteService
        // so we don't verify it here
    }

    @Test
    void voteThread_ThreadNotFound() {
        // Arrange
        when(threadRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> threadService.voteThread(1L, 1L, true));
        verify(voteService, never()).createThreadVote(any(), any(), any());
    }

    @Test
    void voteThread_UserNotFound() {
        // Arrange
        when(threadRepository.findById(1L)).thenReturn(Optional.of(testThread));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> threadService.voteThread(1L, 1L, true));
        verify(voteService, never()).createThreadVote(any(), any(), any());
    }

    @Test
    void removeVote_Success() {
        // Arrange
        when(threadRepository.findById(1L)).thenReturn(Optional.of(testThread));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(voteService).deleteVoteByUserAndThread(1L, 1L);

        // Act
        Thread result = threadService.removeVote(1L, 1L);

        // Assert
        assertNotNull(result);
        verify(voteService).deleteVoteByUserAndThread(1L, 1L);
    }

    @Test
    void removeVote_ThreadNotFound() {
        // Arrange
        when(threadRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> threadService.removeVote(1L, 1L));
        verify(voteService, never()).deleteVoteByUserAndThread(any(), any());
    }

    @Test
    void followThread_Success() {
        // Arrange
        when(threadRepository.findById(1L)).thenReturn(Optional.of(testThread));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(threadRepository.save(any(Thread.class))).thenReturn(testThread);
        
        // Act
        Thread result = threadService.followThread(1L, 1L);
        
        // Assert
        assertNotNull(result);
        verify(threadRepository).save(any(Thread.class));
        verify(threadHistoryService).logThreadFollow(eq(testThread), eq(testUser));
    }

    @Test
    void unfollowThread_Success() {
        // Arrange
        Set<User> followers = new HashSet<>();
        followers.add(testUser);
        testThread.setThreadFollowers(followers);
        testUser.getFollowedThreads().add(testThread);
        
        when(threadRepository.findById(1L)).thenReturn(Optional.of(testThread));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(threadRepository.save(any(Thread.class))).thenReturn(testThread);
        
        // Act
        Thread result = threadService.unfollowThread(1L, 1L);
        
        // Assert
        assertNotNull(result);
        verify(threadRepository).save(any(Thread.class));
        verify(threadHistoryService).logThreadUnfollow(eq(testThread), eq(testUser));
    }

    @Test
    void searchThreads_Success() {
        // Arrange
        List<Thread> expectedThreads = Collections.singletonList(testThread);
        when(threadRepository.searchThreads("test")).thenReturn(expectedThreads);
        
        // Act
        List<Thread> result = threadService.searchThreads("test");
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(testThread.getId(), result.get(0).getId());
    }

    @Test
    void getThreadsByTag_Success() {
        // Arrange
        List<Thread> expectedThreads = Collections.singletonList(testThread);
        when(threadRepository.findByTagLabel("testtag")).thenReturn(expectedThreads);
        
        // Act
        List<Thread> result = threadService.getThreadsByTag("testtag");
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(testThread.getId(), result.get(0).getId());
    }

    @Test
    void getThreadsFollowedByUser_Success() {
        // Arrange
        List<Thread> expectedThreads = Collections.singletonList(testThread);
        when(threadRepository.findThreadsFollowedByUser(1L)).thenReturn(expectedThreads);
        
        // Act
        List<Thread> result = threadService.getThreadsFollowedByUser(1L);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(testThread.getId(), result.get(0).getId());
    }
} 