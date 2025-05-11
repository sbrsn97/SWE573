package com.swe573.services.impl;

import com.swe573.models.Thread;
import com.swe573.models.Comment;
import com.swe573.models.Tag;
import com.swe573.models.User;
import com.swe573.models.enums.VoteType;
import com.swe573.repositories.ThreadRepository;
import com.swe573.repositories.CommentRepository;
import com.swe573.repositories.VoteRepository;
import com.swe573.repositories.UserRepository;
import com.swe573.repositories.TagRepository;
import com.swe573.services.VoteAnalyticsService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

@Service
public class VoteAnalyticsServiceImpl implements VoteAnalyticsService {

    @Autowired
    private ThreadRepository threadRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private VoteRepository voteRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TagRepository tagRepository;

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

    @Override
    public List<Thread> getHotThreads(int daysBack, int limit) {
        // Define the time period for "hot" - activity within last X days
        LocalDateTime startDate = LocalDateTime.now().minusDays(daysBack);
        
        // Get all threads
        List<Thread> allThreads = threadRepository.findAll();
        
        // Score each thread based on activity: votes, comments, follows in the recent time period
        return allThreads.stream()
            .filter(thread -> thread.isActive()) // Only include active threads
            .map(thread -> {
                // Calculate activity score based on:
                // 1. Recent votes (last X days)
                long recentVotes = thread.getVotes().stream()
                    .filter(vote -> vote.getCreatedAt().isAfter(startDate))
                    .count();
                    
                // 2. Recent comments (last X days)
                long recentComments = thread.getComments().stream()
                    .filter(comment -> comment.getCreatedAt().isAfter(startDate))
                    .count();
                    
                // 3. Recent followers (challenging as follow timestamp isn't tracked directly)
                // This could be enhanced if you add timestamps to your join table
                
                // 4. Thread recency itself - newer threads get a boost
                long daysSinceCreation = java.time.Duration.between(
                    thread.getCreatedAt(), 
                    LocalDateTime.now()
                ).toDays();
                
                // Calculate a hotness score - you can adjust the weights
                double hotnessScore = (recentVotes * 1.0) + 
                                      (recentComments * 1.5) + 
                                      (thread.getUpvoteCount() * 0.5) -
                                      (daysSinceCreation * 0.1);
                                      
                // Return the thread with its score as a map entry for sorting
                return Map.entry(thread, hotnessScore);
            })
            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue())) // Sort by score descending
            .limit(limit)
            .map(Map.Entry::getKey) // Extract just the threads
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Thread> getRecommendedThreadsForUser(Long userId, int limit) {
        // Get the user and their tags
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        // Get tags the user is interested in (both explicit tags and tags from followed threads)
        Set<Tag> userTags = new HashSet<>(user.getTags());
        
        // Add tags from threads the user follows
        Set<Thread> followedThreads = user.getFollowedThreads();
        for (Thread thread : followedThreads) {
            userTags.addAll(thread.getTags());
        }
        
        // If user has no tags, return most voted threads
        if (userTags.isEmpty()) {
            return threadRepository.findAll().stream()
                .filter(t -> t.isActive() && !followedThreads.contains(t) && t.getAuthor().getId() != userId)
                .sorted((t1, t2) -> Integer.compare(t2.getUpvoteCount() - t2.getDownvoteCount(),
                                                    t1.getUpvoteCount() - t1.getDownvoteCount()))
                .limit(limit)
                .collect(Collectors.toList());
        }
        
        // Find all threads not created by this user and not already followed
        List<Thread> candidateThreads = threadRepository.findAll().stream()
            .filter(t -> t.isActive() && !followedThreads.contains(t) && t.getAuthor().getId() != userId)
            .collect(Collectors.toList());
        
        // Score each thread based on tag overlap with user interests
        return candidateThreads.stream()
            .map(thread -> {
                // Calculate tag overlap score
                Set<Tag> threadTags = thread.getTags();
                Set<Tag> commonTags = new HashSet<>(threadTags);
                commonTags.retainAll(userTags);
                
                double tagMatchScore = commonTags.size() / (double) Math.max(userTags.size(), 1);
                
                // Consider thread popularity too
                int voteScore = thread.getUpvoteCount() - thread.getDownvoteCount();
                
                // Final score is weighted combination
                double finalScore = (tagMatchScore * 0.7) + (voteScore * 0.01);
                
                return Map.entry(thread, finalScore);
            })
            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue())) // Sort by score descending
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    @Override
    public List<Thread> getSimilarThreads(Long threadId, int limit) {
        // Get the source thread
        Thread sourceThread = threadRepository.findById(threadId)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found"));
        
        // Get tags from the source thread
        Set<Tag> sourceTags = sourceThread.getTags();
        
        // If thread has no tags, return most voted threads
        if (sourceTags.isEmpty()) {
            return threadRepository.findAll().stream()
                .filter(t -> t.isActive() && !t.getId().equals(threadId))
                .sorted((t1, t2) -> Integer.compare(t2.getUpvoteCount() - t2.getDownvoteCount(),
                                                    t1.getUpvoteCount() - t1.getDownvoteCount()))
                .limit(limit)
                .collect(Collectors.toList());
        }
        
        // Find all other active threads
        List<Thread> candidateThreads = threadRepository.findAll().stream()
            .filter(t -> t.isActive() && !t.getId().equals(threadId))
            .collect(Collectors.toList());
        
        // Score each thread based on tag overlap
        return candidateThreads.stream()
            .map(thread -> {
                // Calculate tag overlap score (Jaccard similarity)
                Set<Tag> threadTags = thread.getTags();
                
                // If thread has no tags, score it low
                if (threadTags.isEmpty()) {
                    return Map.entry(thread, 0.0);
                }
                
                // Find common tags
                Set<Tag> commonTags = new HashSet<>(threadTags);
                commonTags.retainAll(sourceTags);
                
                // Union of tags
                Set<Tag> unionTags = new HashSet<>(threadTags);
                unionTags.addAll(sourceTags);
                
                // Jaccard similarity: |A ∩ B| / |A ∪ B|
                double tagSimilarity = commonTags.size() / (double) unionTags.size();
                
                // Consider thread popularity too
                int voteScore = thread.getUpvoteCount() - thread.getDownvoteCount();
                
                // Final score is weighted combination
                double finalScore = (tagSimilarity * 0.8) + (voteScore * 0.01);
                
                return Map.entry(thread, finalScore);
            })
            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue())) // Sort by score descending
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
} 