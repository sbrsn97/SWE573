package com.swe573.repositories;

import com.swe573.models.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByThreadId(Long threadId);
    List<Comment> findByAuthorId(Long authorId);
} 