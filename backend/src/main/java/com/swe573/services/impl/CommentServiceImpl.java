package com.swe573.services.impl;

import com.swe573.models.Comment;
import com.swe573.models.Thread;
import com.swe573.models.User;
import com.swe573.models.Node;
import com.swe573.dto.CreateCommentDTO;
import com.swe573.repositories.CommentRepository;
import com.swe573.repositories.ThreadRepository;
import com.swe573.repositories.UserRepository;
import com.swe573.repositories.NodeRepository;
import com.swe573.services.CommentService;
import com.swe573.services.NotificationService;
import com.swe573.exceptions.ResourceNotFoundException;
import com.swe573.exceptions.UnauthorizedException;
import com.swe573.models.enums.NotificationType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ThreadRepository threadRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional
    public Comment createComment(CreateCommentDTO createCommentDTO, Long authorId) {
        User author = userRepository.findById(authorId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            
        Thread thread = threadRepository.findById(createCommentDTO.getThreadId())
            .orElseThrow(() -> new ResourceNotFoundException("Thread not found"));

        Comment comment = new Comment();
        comment.setContent(createCommentDTO.getContent());
        comment.setAuthor(author);
        comment.setThread(thread);
        
        // Handle parent-child relationship
        if (createCommentDTO.getParentId() != null) {
            Comment parentComment = commentRepository.findById(createCommentDTO.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));
                
            // Ensure parent is from the same thread
            if (!parentComment.getThread().getId().equals(thread.getId())) {
                throw new IllegalArgumentException("Parent comment must belong to the same thread");
            }
            
            // Ensure we're not creating a third-level comment
            if (parentComment.getParent() != null) {
                throw new IllegalArgumentException("Cannot reply to a child comment");
            }
            
            comment.setParent(parentComment);
        }

        if (createCommentDTO.getReferencedNodeIds() != null && !createCommentDTO.getReferencedNodeIds().isEmpty()) {
            List<Node> nodes = nodeRepository.findAllById(createCommentDTO.getReferencedNodeIds());
            comment.getReferencedNodes().addAll(nodes);
        }

        Comment savedComment = commentRepository.save(comment);

        // Create notification for thread author if it's a top-level comment
        if (!author.getId().equals(thread.getAuthor().getId()) && comment.getParent() == null) {
            notificationService.createNotification(
                thread.getAuthor().getId(),
                String.format("%s commented on your thread '%s'", author.getUsername(), thread.getTitle()),
                NotificationType.NEW_COMMENT,
                savedComment.getId(),
                "COMMENT",
                author.getId(),
                author.getUsername()
            );
        }
        
        // Create notification for parent comment author if it's a reply
        if (comment.getParent() != null && !author.getId().equals(comment.getParent().getAuthor().getId())) {
            notificationService.createNotification(
                comment.getParent().getAuthor().getId(),
                String.format("%s replied to your comment", author.getUsername()),
                NotificationType.COMMENT_REPLY,
                savedComment.getId(),
                "COMMENT",
                author.getId(),
                author.getUsername()
            );
        }

        return savedComment;
    }

    @Override
    public Optional<Comment> findById(Long id) {
        return commentRepository.findById(id);
    }

    @Override
    public List<Comment> findByThreadId(Long threadId) {
        return commentRepository.findByThreadIdOrderByCreatedAt(threadId);
    }
    
    @Override
    public List<Comment> findParentCommentsByThreadId(Long threadId) {
        return commentRepository.findByThreadIdAndParentIsNullOrderByCreatedAt(threadId);
    }
    
    @Override
    public List<Comment> findChildCommentsByParentId(Long parentId) {
        return commentRepository.findByParentIdOrderByCreatedAt(parentId);
    }

    @Override
    public List<Comment> findByAuthorId(Long authorId) {
        return commentRepository.findByAuthorIdOrderByCreatedAt(authorId);
    }

    @Override
    @Transactional
    public Comment updateComment(Long id, String newContent) {
        Comment comment = commentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
            
        comment.setContent(newContent);
        return commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void softDeleteComment(Long id, Long userId) {
        Comment comment = commentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
            
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new UnauthorizedException("You don't have permission to delete this comment");
        }

        comment.softDeleteByUser();
        commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void hardDeleteComment(Long id) {
        Comment comment = findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        
        comment.hardDelete();
        commentRepository.delete(comment);
    }

    @Override
    @Transactional
    public Comment reactivateComment(Long id) {
        Comment comment = commentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
            
        comment.reactivate();
        return commentRepository.save(comment);
    }

    @Override
    public Comment save(Comment comment) {
        return commentRepository.save(comment);
    }

    @Override
    public void delete(Comment comment) {
        commentRepository.delete(comment);
    }
} 