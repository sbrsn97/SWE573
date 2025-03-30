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
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class VoteServiceImpl implements VoteService {

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ThreadRepository threadRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional
    public Vote createThreadVote(Long userId, Long threadId, VoteType type) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Thread thread = threadRepository.findById(threadId)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found"));

        // Remove existing vote if any
        deleteVoteByUserAndThread(userId, threadId);

        // Create new vote
        Vote vote = new Vote();
        vote.setUser(user);
        vote.setThread(thread);
        vote.setType(type);
        thread.getVotes().add(vote);

        // Update vote counts
        updateThreadVoteCounts(thread);

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

        return voteRepository.save(vote);
    }

    @Override
    @Transactional
    public Vote createCommentVote(Long userId, Long commentId, VoteType type) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new EntityNotFoundException("Comment not found"));

        // Remove existing vote if any
        deleteVoteByUserAndComment(userId, commentId);

        // Create new vote
        Vote vote = new Vote();
        vote.setUser(user);
        vote.setComment(comment);
        vote.setType(type);
        comment.getVotes().add(vote);

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

        return voteRepository.save(vote);
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
            vote.getThread().getVotes().remove(vote);
            updateThreadVoteCounts(vote.getThread());
            
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
            vote.getComment().getVotes().remove(vote);
            
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
        
        Optional<Vote> existingVote = voteRepository.findByUserIdAndThreadId(userId, threadId);
        if (existingVote.isPresent()) {
            Vote vote = existingVote.get();
            thread.getVotes().remove(vote);
            updateThreadVoteCounts(thread);
            
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
        }
    }

    @Override
    @Transactional
    public void deleteVoteByUserAndComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new EntityNotFoundException("Comment not found"));
        
        Optional<Vote> existingVote = voteRepository.findByUserIdAndCommentId(userId, commentId);
        if (existingVote.isPresent()) {
            Vote vote = existingVote.get();
            comment.getVotes().remove(vote);
            
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
    public VoteType getUserVoteTypeOnThread(Long userId, Long threadId) {
        return voteRepository.findByUserIdAndThreadId(userId, threadId)
            .map(Vote::getType)
            .orElse(null);
    }

    @Override
    public VoteType getUserVoteTypeOnComment(Long userId, Long commentId) {
        return voteRepository.findByUserIdAndCommentId(userId, commentId)
            .map(Vote::getType)
            .orElse(null);
    }

    @Override
    public int getThreadVoteCount(Long threadId) {
        Thread thread = threadRepository.findById(threadId)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found"));
        return thread.getUpvoteCount() - thread.getDownvoteCount();
    }

    @Override
    public int getCommentVoteCount(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new EntityNotFoundException("Comment not found"));
        return (int) comment.getVotes().stream()
            .filter(vote -> vote.getType() == VoteType.UPVOTE)
            .count() - (int) comment.getVotes().stream()
            .filter(vote -> vote.getType() == VoteType.DOWNVOTE)
            .count();
    }

    private void updateThreadVoteCounts(Thread thread) {
        int upvotes = 0;
        int downvotes = 0;
        for (Vote vote : thread.getVotes()) {
            if (vote.getType() == VoteType.UPVOTE) {
                upvotes++;
            } else {
                downvotes++;
            }
        }
        thread.setUpvoteCount(upvotes);
        thread.setDownvoteCount(downvotes);
    }
} 