package com.swe573.services;

import com.swe573.models.Thread;
import com.swe573.models.Comment;
import java.time.LocalDateTime;
import java.util.List;

public interface VoteAnalyticsService {
    // Get most voted threads
    List<Thread> getMostVotedThreads(int limit);
    List<Thread> getMostVotedThreadsByTag(String tagLabel, int limit);
    List<Thread> getMostVotedThreadsByTimeRange(LocalDateTime start, LocalDateTime end, int limit);
    
    // Get most voted comments
    List<Comment> getMostVotedComments(int limit);
    List<Comment> getMostVotedCommentsByThread(Long threadId, int limit);
    
    // Get user voting statistics
    int getUserVoteCount(Long userId);
    int getUserUpvoteCount(Long userId);
    int getUserDownvoteCount(Long userId);
    List<Thread> getMostVotedThreadsByUser(Long userId, int limit);
    List<Comment> getMostVotedCommentsByUser(Long userId, int limit);
    
    // Get voting trends
    int getVoteCountByTimeRange(LocalDateTime start, LocalDateTime end);
    int getUpvoteCountByTimeRange(LocalDateTime start, LocalDateTime end);
    int getDownvoteCountByTimeRange(LocalDateTime start, LocalDateTime end);
} 