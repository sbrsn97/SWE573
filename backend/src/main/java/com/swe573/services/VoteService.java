package com.swe573.services;

import com.swe573.models.Vote;
import com.swe573.models.enums.VoteType;

public interface VoteService {
    Vote createThreadVote(Long userId, Long threadId, VoteType type);
    Vote createCommentVote(Long userId, Long commentId, VoteType type);
    Vote getVote(Long id);
    void deleteVote(Long id);
    void deleteVoteByUserAndThread(Long userId, Long threadId);
    void deleteVoteByUserAndComment(Long userId, Long commentId);
    boolean hasUserVotedOnThread(Long userId, Long threadId);
    boolean hasUserVotedOnComment(Long userId, Long commentId);
    VoteType getUserVoteTypeOnThread(Long userId, Long threadId);
    VoteType getUserVoteTypeOnComment(Long userId, Long commentId);
    int getThreadVoteCount(Long threadId);
    int getCommentVoteCount(Long commentId);
    void recalculateThreadVoteCounts(Long threadId);
    
    // Methods to reset vote counts based on actual votes
    int resetAllThreadVoteCounts();
    int resetAllCommentVoteCounts();
    
    // Methods to zero out all vote counts
    int zeroOutAllThreadVoteCounts();
    int zeroOutAllCommentVoteCounts();
} 