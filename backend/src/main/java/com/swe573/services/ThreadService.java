package com.swe573.services;

import com.swe573.dto.ThreadDTO;
import com.swe573.models.Thread;
import java.util.List;
import java.util.Optional;

public interface ThreadService {
    Thread createThread(ThreadDTO threadDTO);
    Thread getThread(Long id);
    List<Thread> getAllThreads();
    List<Thread> getThreadsByAuthor(Long authorId);
    List<Thread> getThreadsByTag(String tagLabel);
    List<Thread> searchThreads(String keyword);
    List<Thread> getThreadsFollowedByUser(Long userId);
    Thread updateThread(Long id, ThreadDTO threadDTO);
    void deleteThread(Long id);
    Thread followThread(Long threadId, Long userId);
    Thread unfollowThread(Long threadId, Long userId);
    Thread voteThread(Long threadId, Long userId, boolean isUpvote);
    Thread removeVote(Long threadId, Long userId);
    Optional<Thread> findById(Long id);
    Thread save(Thread thread);
    void delete(Thread thread);
    
    /**
     * Check if a user can interact with a thread (comment, vote)
     * @param threadId the thread ID
     * @param userId the user ID
     * @return true if the user can interact with the thread
     */
    boolean canUserInteractWithThread(Long threadId, Long userId);
    
    /**
     * Check if a user can view a thread
     * @param threadId the thread ID
     * @param userId the user ID
     * @return true if the user can view the thread
     */
    boolean canUserViewThread(Long threadId, Long userId);
} 