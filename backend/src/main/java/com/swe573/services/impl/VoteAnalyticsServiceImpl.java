package com.swe573.services.impl;

import com.swe573.models.Thread;
import com.swe573.models.Comment;
import com.swe573.models.Vote;
import com.swe573.models.enums.VoteType;
import com.swe573.repositories.ThreadRepository;
import com.swe573.repositories.CommentRepository;
import com.swe573.repositories.VoteRepository;
import com.swe573.services.VoteAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VoteAnalyticsServiceImpl implements VoteAnalyticsService {

    @Autowired
    private ThreadRepository threadRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Override
    public List<Thread> getMostVotedThreads(int limit) {
        return threadRepository.findAll().stream()
            .sorted((t1, t2) -> Integer.compare(t2.getUpvoteCount() - t2.getDownvoteCount(),
                                             t1.getUpvoteCount() - t1.getDownvoteCount()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public List<Thread> getMostVotedThreadsByTag(String tagLabel, int limit) {
        return threadRepository.findByTagLabel(tagLabel).stream()
            .sorted((t1, t2) -> Integer.compare(t2.getUpvoteCount() - t2.getDownvoteCount(),
                                             t1.getUpvoteCount() - t1.getDownvoteCount()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public List<Thread> getMostVotedThreadsByTimeRange(LocalDateTime start, LocalDateTime end, int limit) {
        return threadRepository.findAll().stream()
            .filter(thread -> thread.getCreatedAt().isAfter(start) && thread.getCreatedAt().isBefore(end))
            .sorted((t1, t2) -> Integer.compare(t2.getUpvoteCount() - t2.getDownvoteCount(),
                                             t1.getUpvoteCount() - t1.getDownvoteCount()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public List<Comment> getMostVotedComments(int limit) {
        return commentRepository.findAll().stream()
            .sorted((c1, c2) -> Integer.compare(
                (int) c2.getVotes().stream().filter(v -> v.getType() == VoteType.UPVOTE).count() -
                (int) c2.getVotes().stream().filter(v -> v.getType() == VoteType.DOWNVOTE).count(),
                (int) c1.getVotes().stream().filter(v -> v.getType() == VoteType.UPVOTE).count() -
                (int) c1.getVotes().stream().filter(v -> v.getType() == VoteType.DOWNVOTE).count()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public List<Comment> getMostVotedCommentsByThread(Long threadId, int limit) {
        Thread thread = threadRepository.findById(threadId)
            .orElseThrow(() -> new RuntimeException("Thread not found"));
        return thread.getComments().stream()
            .sorted((c1, c2) -> Integer.compare(
                (int) c2.getVotes().stream().filter(v -> v.getType() == VoteType.UPVOTE).count() -
                (int) c2.getVotes().stream().filter(v -> v.getType() == VoteType.DOWNVOTE).count(),
                (int) c1.getVotes().stream().filter(v -> v.getType() == VoteType.UPVOTE).count() -
                (int) c1.getVotes().stream().filter(v -> v.getType() == VoteType.DOWNVOTE).count()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public int getUserVoteCount(Long userId) {
        return (int) voteRepository.findAll().stream()
            .filter(vote -> vote.getUser().getId().equals(userId))
            .count();
    }

    @Override
    public int getUserUpvoteCount(Long userId) {
        return (int) voteRepository.findAll().stream()
            .filter(vote -> vote.getUser().getId().equals(userId) && vote.getType() == VoteType.UPVOTE)
            .count();
    }

    @Override
    public int getUserDownvoteCount(Long userId) {
        return (int) voteRepository.findAll().stream()
            .filter(vote -> vote.getUser().getId().equals(userId) && vote.getType() == VoteType.DOWNVOTE)
            .count();
    }

    @Override
    public List<Thread> getMostVotedThreadsByUser(Long userId, int limit) {
        return threadRepository.findAll().stream()
            .filter(thread -> thread.getAuthor().getId().equals(userId))
            .sorted((t1, t2) -> Integer.compare(t2.getUpvoteCount() - t2.getDownvoteCount(),
                                             t1.getUpvoteCount() - t1.getDownvoteCount()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public List<Comment> getMostVotedCommentsByUser(Long userId, int limit) {
        return commentRepository.findAll().stream()
            .filter(comment -> comment.getAuthor().getId().equals(userId))
            .sorted((c1, c2) -> Integer.compare(
                (int) c2.getVotes().stream().filter(v -> v.getType() == VoteType.UPVOTE).count() -
                (int) c2.getVotes().stream().filter(v -> v.getType() == VoteType.DOWNVOTE).count(),
                (int) c1.getVotes().stream().filter(v -> v.getType() == VoteType.UPVOTE).count() -
                (int) c1.getVotes().stream().filter(v -> v.getType() == VoteType.DOWNVOTE).count()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public int getVoteCountByTimeRange(LocalDateTime start, LocalDateTime end) {
        return (int) voteRepository.findAll().stream()
            .filter(vote -> vote.getCreatedAt().isAfter(start) && vote.getCreatedAt().isBefore(end))
            .count();
    }

    @Override
    public int getUpvoteCountByTimeRange(LocalDateTime start, LocalDateTime end) {
        return (int) voteRepository.findAll().stream()
            .filter(vote -> vote.getCreatedAt().isAfter(start) && 
                          vote.getCreatedAt().isBefore(end) && 
                          vote.getType() == VoteType.UPVOTE)
            .count();
    }

    @Override
    public int getDownvoteCountByTimeRange(LocalDateTime start, LocalDateTime end) {
        return (int) voteRepository.findAll().stream()
            .filter(vote -> vote.getCreatedAt().isAfter(start) && 
                          vote.getCreatedAt().isBefore(end) && 
                          vote.getType() == VoteType.DOWNVOTE)
            .count();
    }
} 