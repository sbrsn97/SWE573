package com.swe573.services;

import com.swe573.models.Comment;
import com.swe573.dto.CreateCommentDTO;
import java.util.List;
import java.util.Optional;

public interface CommentService {
    Comment createComment(CreateCommentDTO createCommentDTO, Long authorId);
    Optional<Comment> findById(Long id);
    List<Comment> findByThreadId(Long threadId);
    List<Comment> findByAuthorId(Long authorId);
    Comment updateComment(Long id, String newContent);
    void softDeleteComment(Long id, Long userId);
    void hardDeleteComment(Long id);
    Comment reactivateComment(Long id);
    Comment save(Comment comment);
    void delete(Comment comment);
    // ... other methods will be added as needed
} 