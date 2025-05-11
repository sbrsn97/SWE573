package com.swe573.services.impl;

import com.swe573.models.Thread;
import com.swe573.models.User;
import com.swe573.models.Vote;
import com.swe573.models.Comment;
import com.swe573.models.enums.VoteType;
import com.swe573.models.enums.NotificationType;
import com.swe573.repositories.ThreadRepository;
import com.swe573.repositories.UserRepository;
import com.swe573.repositories.VoteRepository;
import com.swe573.repositories.CommentRepository;
import com.swe573.services.VoteService;
import com.swe573.services.NotificationService;
import com.swe573.services.ThreadHistoryService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class VoteServiceImpl implements VoteService {

    private final VoteRepository voteRepository;
    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;
    private final ThreadHistoryService threadHistoryService;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public VoteServiceImpl(VoteRepository voteRepository, 
                          UserRepository userRepository, 
                          ThreadRepository threadRepository, 
                          CommentRepository commentRepository, 
                          NotificationService notificationService,
                          ThreadHistoryService threadHistoryService) {
        this.voteRepository = voteRepository;
        this.userRepository = userRepository;
        this.threadRepository = threadRepository;
        this.commentRepository = commentRepository;
        this.notificationService = notificationService;
        this.threadHistoryService = threadHistoryService;
    }

    @Override
    @Transactional
    public Vote createThreadVote(Long userId, Long threadId, VoteType type) {
        try {
            System.out.println("DEBUG: Creating vote for userId=" + userId + " threadId=" + threadId + " type=" + type);
            
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
            Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new EntityNotFoundException("Thread not found with ID: " + threadId));

            // First check if vote exists
            Optional<Vote> existingVote = voteRepository.findByUserIdAndThreadId(userId, threadId);
            
            if (existingVote.isPresent()) {
                System.out.println("DEBUG: Existing vote found, handling update");
                Vote vote = existingVote.get();
                
                // If same vote type, do nothing
                if (vote.getType() == type) {
                    return vote;
                }
                
                // If different vote type, update instead of delete and create
                VoteType oldType = vote.getType();
                vote.setType(type);
                
                // Update vote counts - when switching votes we need to +1 new type and -1 old type
                if (oldType == VoteType.UPVOTE && type == VoteType.DOWNVOTE) {
                    // Switching from upvote to downvote
                    thread.setUpvoteCount(Math.max(0, thread.getUpvoteCount() - 1));
                    thread.setDownvoteCount(thread.getDownvoteCount() + 1);
                    System.out.println("DEBUG: Switching from upvote to downvote. New counts: UP=" + 
                        thread.getUpvoteCount() + ", DOWN=" + thread.getDownvoteCount());
                } else if (oldType == VoteType.DOWNVOTE && type == VoteType.UPVOTE) {
                    // Switching from downvote to upvote
                    thread.setDownvoteCount(Math.max(0, thread.getDownvoteCount() - 1));
                    thread.setUpvoteCount(thread.getUpvoteCount() + 1);
                    System.out.println("DEBUG: Switching from downvote to upvote. New counts: UP=" + 
                        thread.getUpvoteCount() + ", DOWN=" + thread.getDownvoteCount());
                }
                
                // Save thread with updated counts
                Thread savedThread = threadRepository.save(thread);
                System.out.println("DEBUG: After save, thread counts: UP=" + 
                    savedThread.getUpvoteCount() + ", DOWN=" + savedThread.getDownvoteCount());
                
                // Create notification
                String message = String.format("%s %s your thread '%s'", 
                    user.getUsername(), 
                    type == VoteType.UPVOTE ? "upvoted" : "downvoted",
                    thread.getTitle());
                
                notificationService.createNotification(
                    thread.getAuthor().getId(),
                    message,
                    type == VoteType.UPVOTE ? NotificationType.THREAD_UPVOTE : NotificationType.THREAD_DOWNVOTE,
                    threadId,
                    "THREAD",
                    userId,
                    user.getUsername()
                );
                
                Vote savedVote = voteRepository.save(vote);
                
                // Log vote change to history
                threadHistoryService.logThreadVote(thread, user, type == VoteType.UPVOTE);
                
                System.out.println("DEBUG: Vote updated successfully");
                return savedVote;
            } else {
                System.out.println("DEBUG: No existing vote, creating new one");
                
                // To be extra careful, use a native query to delete any possible stray votes
                try {
                    Query query = entityManager.createNativeQuery(
                        "DELETE FROM votes WHERE user_id = :userId AND thread_id = :threadId");
                    query.setParameter("userId", userId);
                    query.setParameter("threadId", threadId);
                    int deletedCount = query.executeUpdate();
                    System.out.println("DEBUG: Deleted " + deletedCount + " votes directly from database (safety check)");
                    
                    // Force a refresh of the thread entity to ensure it's in sync
                    entityManager.refresh(thread);
                } catch (Exception e) {
                    System.err.println("DEBUG: Error attempting direct delete: " + e.getMessage());
                }
                
                // No existing vote, create new one
                Vote vote = new Vote();
                vote.setUser(user);
                vote.setThread(thread);
                vote.setType(type);
                
                // Update vote counts
                if (type == VoteType.UPVOTE) {
                    thread.setUpvoteCount(thread.getUpvoteCount() + 1);
                    System.out.println("DEBUG: New upvote. Counts now: UP=" + 
                        thread.getUpvoteCount() + ", DOWN=" + thread.getDownvoteCount());
                } else {
                    thread.setDownvoteCount(thread.getDownvoteCount() + 1);
                    System.out.println("DEBUG: New downvote. Counts now: UP=" + 
                        thread.getUpvoteCount() + ", DOWN=" + thread.getDownvoteCount());
                }
                
                // Save thread with updated counts
                Thread savedThread = threadRepository.save(thread);
                System.out.println("DEBUG: After save, thread counts: UP=" + 
                    savedThread.getUpvoteCount() + ", DOWN=" + savedThread.getDownvoteCount());
                
                // Create notification
                String message = String.format("%s %s your thread '%s'", 
                    user.getUsername(), 
                    type == VoteType.UPVOTE ? "upvoted" : "downvoted",
                    thread.getTitle());
                
                notificationService.createNotification(
                    thread.getAuthor().getId(),
                    message,
                    type == VoteType.UPVOTE ? NotificationType.THREAD_UPVOTE : NotificationType.THREAD_DOWNVOTE,
                    threadId,
                    "THREAD",
                    userId,
                    user.getUsername()
                );
                
                try {
                    Vote savedVote = voteRepository.save(vote);
                    System.out.println("DEBUG: Successfully saved vote with ID " + savedVote.getId());
                    
                    // Log vote to history
                    threadHistoryService.logThreadVote(thread, user, type == VoteType.UPVOTE);
                    
                    return savedVote;
                } catch (Exception e) {
                    System.err.println("DEBUG: Error saving vote: " + e.getMessage());
                    throw e;
                }
            }
        } catch (Exception e) {
            System.err.println("DEBUG ERROR in createThreadVote: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @Transactional
    public Vote createCommentVote(Long userId, Long commentId, VoteType type) {
        // Add emergency debugging
        System.out.println("EMERGENCY DEBUG: Creating vote for userId=" + userId + " commentId=" + commentId + " type=" + type);
        
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
            Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found with ID: " + commentId));
            
            // First check if vote exists
            Optional<Vote> existingVote = voteRepository.findByUserIdAndCommentId(userId, commentId);
            
            // Emergency: Try to delete any existing votes if found
            if (existingVote.isPresent() || true) { // Force execute for safety
                System.out.println("EMERGENCY: Attempting to delete any existing votes");
                // DANGEROUS but necessary for emergency fix - directly delete from DB
                try {
                    // Use a native query to delete any existing votes to ensure DB consistency
                    Query query = entityManager.createNativeQuery(
                        "DELETE FROM votes WHERE user_id = :userId AND comment_id = :commentId");
                    query.setParameter("userId", userId);
                    query.setParameter("commentId", commentId);
                    int deletedCount = query.executeUpdate();
                    System.out.println("EMERGENCY: Deleted " + deletedCount + " votes directly from database");
                    
                    // Force a refresh of the comment entity to ensure it's in sync
                    entityManager.refresh(comment);
                } catch (Exception e) {
                    System.err.println("EMERGENCY: Error attempting direct delete: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // Now create a new vote
            System.out.println("Creating fresh vote");
            Vote vote = new Vote();
            vote.setUser(user);
            vote.setComment(comment);
            vote.setType(type);
            
            // Update vote counts
            if (type == VoteType.UPVOTE) {
                comment.setUpvoteCount(comment.getUpvoteCount() + 1);
            } else {
                comment.setDownvoteCount(comment.getDownvoteCount() + 1);
            }
            
            commentRepository.save(comment);
            
            // Create notification
            String message = String.format("%s %s your comment on '%s'", 
                user.getUsername(), 
                type == VoteType.UPVOTE ? "upvoted" : "downvoted",
                comment.getThread().getTitle());
            
            notificationService.createNotification(
                comment.getAuthor().getId(),
                message,
                type == VoteType.UPVOTE ? NotificationType.COMMENT_UPVOTE : NotificationType.COMMENT_DOWNVOTE,
                commentId,
                "COMMENT",
                userId,
                user.getUsername()
            );
            
            try {
                Vote savedVote = voteRepository.save(vote);
                System.out.println("EMERGENCY: Successfully saved vote with ID " + savedVote.getId());
                
                // Log comment vote to history
                threadHistoryService.logCommentVote(comment.getThread(), user, comment.getId(), type == VoteType.UPVOTE);
                
                return savedVote;
            } catch (Exception e) {
                System.err.println("EMERGENCY: Error saving vote: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        } catch (Exception e) {
            System.err.println("EMERGENCY ERROR in createCommentVote: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public Vote getVote(Long id) {
        return voteRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Vote not found"));
    }

    @Override
    @Transactional
    public void deleteVote(Long id) {
        Vote vote = getVote(id);
        
        if (vote.getThread() != null) {
            Thread thread = vote.getThread();
            
            // Update vote counts
            if (vote.getType() == VoteType.UPVOTE) {
                thread.setUpvoteCount(Math.max(0, thread.getUpvoteCount() - 1));
            } else {
                thread.setDownvoteCount(Math.max(0, thread.getDownvoteCount() - 1));
            }
            
            // Remove vote from the thread's votes collection
            thread.getVotes().remove(vote);
            threadRepository.save(thread);
            
            // Create notification for vote removal
            notificationService.createNotification(
                vote.getThread().getAuthor().getId(),
                String.format("%s removed their %s from your thread '%s'", 
                    vote.getUser().getUsername(),
                    vote.getType() == VoteType.UPVOTE ? "upvote" : "downvote",
                    vote.getThread().getTitle()),
                NotificationType.VOTE_REMOVED,
                vote.getThread().getId(),
                "THREAD",
                vote.getUser().getId(),
                vote.getUser().getUsername()
            );
        }
        
        if (vote.getComment() != null) {
            Comment comment = vote.getComment();
            
            // Update vote counts
            if (vote.getType() == VoteType.UPVOTE) {
                comment.setUpvoteCount(Math.max(0, comment.getUpvoteCount() - 1));
            } else {
                comment.setDownvoteCount(Math.max(0, comment.getDownvoteCount() - 1));
            }
            
            // Remove vote from the comment's votes collection
            comment.getVotes().remove(vote);
            commentRepository.save(comment);
            
            // Create notification for vote removal
            notificationService.createNotification(
                vote.getComment().getAuthor().getId(),
                String.format("%s removed their %s from your comment on '%s'", 
                    vote.getUser().getUsername(),
                    vote.getType() == VoteType.UPVOTE ? "upvote" : "downvote",
                    vote.getComment().getThread().getTitle()),
                NotificationType.VOTE_REMOVED,
                vote.getComment().getId(),
                "COMMENT",
                vote.getUser().getId(),
                vote.getUser().getUsername()
            );
        }
        
        voteRepository.delete(vote);
    }

    @Override
    @Transactional
    public void deleteVoteByUserAndThread(Long userId, Long threadId) {
        Thread thread = threadRepository.findById(threadId)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
        
        Optional<Vote> existingVote = voteRepository.findByUserIdAndThreadId(userId, threadId);
        if (existingVote.isPresent()) {
            Vote vote = existingVote.get();
            
            // Update vote counts
            if (vote.getType() == VoteType.UPVOTE) {
                thread.setUpvoteCount(Math.max(0, thread.getUpvoteCount() - 1));
            } else {
                thread.setDownvoteCount(Math.max(0, thread.getDownvoteCount() - 1));
            }
            
            // Remove vote from the thread's votes collection
            thread.getVotes().remove(vote);
            threadRepository.save(thread);
            
            // Delete the vote from the repository
            voteRepository.delete(vote);
            
            // Create notification for vote removal
            notificationService.createNotification(
                thread.getAuthor().getId(),
                String.format("%s removed their %s from your thread '%s'", 
                    vote.getUser().getUsername(),
                    vote.getType() == VoteType.UPVOTE ? "upvote" : "downvote",
                    thread.getTitle()),
                NotificationType.VOTE_REMOVED,
                threadId,
                "THREAD",
                userId,
                vote.getUser().getUsername()
            );
            
            // Log vote removal to history
            threadHistoryService.logThreadRemoveVote(thread, user);
        }
    }

    @Override
    @Transactional
    public void deleteVoteByUserAndComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new EntityNotFoundException("Comment not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
        
        Optional<Vote> existingVote = voteRepository.findByUserIdAndCommentId(userId, commentId);
        if (existingVote.isPresent()) {
            Vote vote = existingVote.get();
            
            // Update vote counts
            if (vote.getType() == VoteType.UPVOTE) {
                comment.setUpvoteCount(Math.max(0, comment.getUpvoteCount() - 1));
            } else {
                comment.setDownvoteCount(Math.max(0, comment.getDownvoteCount() - 1));
            }
            
            // Remove vote from the comment's votes collection
            comment.getVotes().remove(vote);
            commentRepository.save(comment);
            
            // Delete the vote from the repository
            voteRepository.delete(vote);
            
            // Create notification for vote removal
            notificationService.createNotification(
                comment.getAuthor().getId(),
                String.format("%s removed their %s from your comment on '%s'", 
                    vote.getUser().getUsername(),
                    vote.getType() == VoteType.UPVOTE ? "upvote" : "downvote",
                    comment.getThread().getTitle()),
                NotificationType.VOTE_REMOVED,
                commentId,
                "COMMENT",
                userId,
                vote.getUser().getUsername()
            );
            
            // Log comment vote removal to history
            threadHistoryService.logCommentRemoveVote(comment.getThread(), user, comment.getId());
        }
    }

    @Override
    public boolean hasUserVotedOnThread(Long userId, Long threadId) {
        return voteRepository.existsByUserIdAndThreadId(userId, threadId);
    }

    @Override
    public boolean hasUserVotedOnComment(Long userId, Long commentId) {
        return voteRepository.existsByUserIdAndCommentId(userId, commentId);
    }

    @Override
    @Transactional
    public void recalculateThreadVoteCounts(Long threadId) {
        Thread thread = threadRepository.findById(threadId)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found with ID: " + threadId));
        
        // Count votes directly from database
        long upvoteCount = voteRepository.countByThreadIdAndType(threadId, VoteType.UPVOTE);
        long downvoteCount = voteRepository.countByThreadIdAndType(threadId, VoteType.DOWNVOTE);
        
        System.out.println("DEBUG: Recalculating vote counts for thread " + threadId);
        System.out.println("DEBUG: Current counts in thread - UP: " + thread.getUpvoteCount() + ", DOWN: " + thread.getDownvoteCount());
        System.out.println("DEBUG: Actual counts from votes table - UP: " + upvoteCount + ", DOWN: " + downvoteCount);
        
        // Update if counts are different
        if (thread.getUpvoteCount() != upvoteCount || thread.getDownvoteCount() != downvoteCount) {
            System.out.println("DEBUG: Correcting thread vote counts");
            thread.setUpvoteCount((int)upvoteCount);
            thread.setDownvoteCount((int)downvoteCount);
            threadRepository.save(thread);
        }
    }

    @Override
    public VoteType getUserVoteTypeOnThread(Long userId, Long threadId) {
        // Don't recalculate on every call
        return voteRepository.findByUserIdAndThreadId(userId, threadId)
            .map(Vote::getType)
            .orElse(null);
    }

    @Override
    public int getThreadVoteCount(Long threadId) {
        // Don't recalculate on every call
        Thread thread = threadRepository.findById(threadId)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found"));
        return thread.getUpvoteCount() - thread.getDownvoteCount();
    }

    @Override
    public int getCommentVoteCount(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new EntityNotFoundException("Comment not found"));
        return comment.getUpvoteCount() - comment.getDownvoteCount();
    }

    @Override
    public VoteType getUserVoteTypeOnComment(Long userId, Long commentId) {
        return voteRepository.findByUserIdAndCommentId(userId, commentId)
            .map(Vote::getType)
            .orElse(null);
    }

    @Override
    @Transactional
    public int resetAllThreadVoteCounts() {
        System.out.println("DEBUG: Resetting all thread vote counts");
        
        try {
            // First get all threads
            List<Thread> threads = threadRepository.findAll();
            int updatedCount = 0;
            
            for (Thread thread : threads) {
                // Count actual votes for this thread
                long upvoteCount = voteRepository.countByThreadIdAndType(thread.getId(), VoteType.UPVOTE);
                long downvoteCount = voteRepository.countByThreadIdAndType(thread.getId(), VoteType.DOWNVOTE);
                
                // Check if counts need updating
                if (thread.getUpvoteCount() != upvoteCount || thread.getDownvoteCount() != downvoteCount) {
                    System.out.println("DEBUG: Resetting vote counts for thread " + thread.getId());
                    System.out.println("DEBUG: Old counts - UP: " + thread.getUpvoteCount() + ", DOWN: " + thread.getDownvoteCount());
                    System.out.println("DEBUG: New counts - UP: " + upvoteCount + ", DOWN: " + downvoteCount);
                    
                    // Set new counts
                    thread.setUpvoteCount((int)upvoteCount);
                    thread.setDownvoteCount((int)downvoteCount);
                    threadRepository.save(thread);
                    updatedCount++;
                }
            }
            
            System.out.println("DEBUG: Updated " + updatedCount + " threads");
            return updatedCount;
        } catch (Exception e) {
            System.err.println("ERROR: Failed to reset thread vote counts: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @Transactional
    public int resetAllCommentVoteCounts() {
        System.out.println("DEBUG: Resetting all comment vote counts");
        
        try {
            // First get all comments
            List<Comment> comments = commentRepository.findAll();
            int updatedCount = 0;
            
            for (Comment comment : comments) {
                // Count actual votes for this comment
                long upvoteCount = voteRepository.countByCommentIdAndType(comment.getId(), VoteType.UPVOTE);
                long downvoteCount = voteRepository.countByCommentIdAndType(comment.getId(), VoteType.DOWNVOTE);
                
                // Check if counts need updating
                if (comment.getUpvoteCount() != upvoteCount || comment.getDownvoteCount() != downvoteCount) {
                    System.out.println("DEBUG: Resetting vote counts for comment " + comment.getId());
                    System.out.println("DEBUG: Old counts - UP: " + comment.getUpvoteCount() + ", DOWN: " + comment.getDownvoteCount());
                    System.out.println("DEBUG: New counts - UP: " + upvoteCount + ", DOWN: " + downvoteCount);
                    
                    // Set new counts
                    comment.setUpvoteCount((int)upvoteCount);
                    comment.setDownvoteCount((int)downvoteCount);
                    commentRepository.save(comment);
                    updatedCount++;
                }
            }
            
            System.out.println("DEBUG: Updated " + updatedCount + " comments");
            return updatedCount;
        } catch (Exception e) {
            System.err.println("ERROR: Failed to reset comment vote counts: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @Transactional
    public int zeroOutAllThreadVoteCounts() {
        System.out.println("DEBUG: Zeroing out all thread vote counts");
        
        try {
            // Use native SQL for a more efficient bulk update
            Query query = entityManager.createNativeQuery(
                "UPDATE threads SET upvote_count = 0, downvote_count = 0");
            
            int affected = query.executeUpdate();
            System.out.println("DEBUG: Zeroed out vote counts for " + affected + " threads");
            return affected;
        } catch (Exception e) {
            System.err.println("ERROR: Failed to zero out thread vote counts: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @Transactional
    public int zeroOutAllCommentVoteCounts() {
        System.out.println("DEBUG: Zeroing out all comment vote counts");
        
        try {
            // Use native SQL for a more efficient bulk update
            Query query = entityManager.createNativeQuery(
                "UPDATE comments SET upvote_count = 0, downvote_count = 0");
            
            int affected = query.executeUpdate();
            System.out.println("DEBUG: Zeroed out vote counts for " + affected + " comments");
            return affected;
        } catch (Exception e) {
            System.err.println("ERROR: Failed to zero out comment vote counts: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
} 